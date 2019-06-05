/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTaskPane;

import com.rapidminer.Process;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.FilterListener;
import com.rapidminer.gui.tools.FilterTextField;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ScrollableJPopupMenu;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.TextFieldWithAction;
import com.rapidminer.gui.tools.syntax.ExpressionTokenMaker;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.parameter.ParameterTypeExpression;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParser;
import com.rapidminer.tools.expression.ExpressionParserBuilder;
import com.rapidminer.tools.expression.ExpressionRegistry;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInput;
import com.rapidminer.tools.expression.FunctionInput.Category;
import com.rapidminer.tools.expression.MacroResolver;


/**
 *
 * The {@link ExpressionPropertyDialog} enables to enter an expression out of functions, attribute
 * values, constants and macro values and validates the expression's syntax.
 *
 * @author Ingo Mierswa, Marco Boeck, Sabrina Kirstein
 *
 */
public class ExpressionPropertyDialog extends PropertyDialog {

	private static final long serialVersionUID = 5567661137372752202L;

	/**
	 * The maximum number of characters allowed in the syntax error message (without the TRUNCATED_SYMBOL to mark the
	 * truncated parts).
	 */
	private static final int MAX_ERROR_MESSAGE_LENGTH = 124;

	/**
	 * This string will be used to mark places in the syntax error message that have been truncated.
	 */
	private static final String TRUNCATED_SYMBOL = "[...]";

	/**
	 * An input panel owns an {@link Observer}, which is updated about model changes.
	 *
	 * @author Sabrina Kirstein
	 */
	private class PrivateInputObserver implements Observer<FunctionInputPanel> {

		@Override
		public void update(Observable<FunctionInputPanel> observable, FunctionInputPanel arg) {

			if (arg != null) {
				// add the function name to the expression
				if (arg.getCategory() == Category.SCOPE) {
					boolean predefined = false;
					for (String predefinedMacro : controllingProcess.getMacroHandler()
							.getAllGraphicallySupportedPredefinedMacros()) {
						// if this is a predefined macro
						if (predefinedMacro.equals(arg.getInputName())) {
							// if the old expression parser is supported
							if (parser.getExpressionContext().getFunction("macro") != null) {
								// if the predefined macro is the number applied times, evaluate it
								if (predefinedMacro
										.equals(Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_USER_FRIENDLY)
										|| predefinedMacro.equals(Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES)) {
									addToExpression("%{" + arg.getInputName() + "}");
									// otherwise show the string
								} else {
									addToExpression("macro(\"" + arg.getInputName() + "\")");
								}
							} else {
								// if the predefined macro is the number applied times, evaluate it
								if (predefinedMacro
										.equals(Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_USER_FRIENDLY)
										|| predefinedMacro.equals(Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES)) {
									addToExpression("eval(%{" + arg.getInputName() + "})");
									// otherwise show the string
								} else {
									addToExpression("%{" + arg.getInputName() + "}");
								}
							}
							predefined = true;
							break;
						}
					}
					// if the macro is a custom macro, give the user the choice between adding the
					// value or an evaluated expression (only if "macro" and/or "eval" functions
					// available in the dialog context)
					if (!predefined) {
						if (parser.getExpressionContext().getFunction("macro") != null
								|| parser.getExpressionContext().getFunction("eval") != null) {
							MacroSelectionDialog macroSelectionDialog = new MacroSelectionDialog(arg,
									parser.getExpressionContext().getFunction("macro") != null);
							macroSelectionDialog.setLocation(arg.getLocationOnScreen().x, arg.getLocationOnScreen().y + 40);
							macroSelectionDialog.setVisible(true);
							addToExpression(macroSelectionDialog.getExpression());
						} else {
							addToExpression("%{" + arg.getInputName() + "}");
						}
					}
				} else if (arg.getCategory() == Category.DYNAMIC) {
					if (parser.getExpressionContext().getConstant(arg.getInputName()) != null) {
						// if the attribute has the same name as a constant, add it with the
						// brackets
						addToExpression("[" + arg.getInputName() + "]");

					} else if (arg.getInputName().matches("(^[A-Za-z])([A-Z_a-z\\d]*)")) {
						// check whether the attribute is alphanumerical without a number at the
						// front
						addToExpression(arg.getInputName());
					} else {
						// if the attribute is not alphanumeric, add it with the brackets,
						// escape [ , ] and \
						String inputName = arg.getInputName().replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]");
						addToExpression("[" + inputName + "]");
					}
				} else {
					addToExpression(arg.getInputName());
				}
			} else {
				// the filtered model changed:
				// update the panels in regard to the filtered model
				updateInputs();
			}
		}
	}

	/**
	 * {@link FunctionDescriptionPanel} owns an {@link Observer}, which is updated on model changes.
	 *
	 * @author Sabrina Kirstein
	 */
	private class PrivateModelObserver implements Observer<FunctionDescription> {

		@Override
		public void update(Observable<FunctionDescription> observable, FunctionDescription arg) {

			if (arg != null) {
				// add the function name to the expression
				addToExpression(arg.getDisplayName(), arg);
			} else {
				// the filtered model changed:
				// update the panels in regard to the filtered model
				updateFunctions();
			}
		}
	}

	/**
	 *
	 * Mouse listener to react on hover events of the filter menu button. Highlights the button,
	 * when hovered.
	 *
	 * @author Sabrina Kirstein
	 *
	 */
	private final class HoverBorderMouseListener extends MouseAdapter {

		private final JButton button;

		public HoverBorderMouseListener(final JButton pageButton) {
			this.button = pageButton;

		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			if (!button.isEnabled()) {
				button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			}
			super.mouseReleased(e);
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			super.mouseExited(e);
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			if (button.isEnabled()) {
				button.setBorder(BorderFactory.createLineBorder(SwingTools.RAPIDMINER_ORANGE, 1));
			}
			super.mouseEntered(e);
		}
	}

	// EXPRESSION
	/** text area with the highlighted current expression */
	private RSyntaxTextArea currentExpression = new RSyntaxTextArea();

	/** scroll pane of the text area with the highlighted current expression */
	private RTextScrollPane scrollPaneExpression = new RTextScrollPane();

	// PARSER
	private final ExpressionParser parser;
	private final com.rapidminer.Process controllingProcess;

	// INPUTS
	/** search text filter of the inputs field */
	private final FilterTextField inputsFilterField = new FilterTextField(12);

	/** QuickFilter: Nominal */
	private JCheckBox chbNominal;

	/** QuickFilter: Numeric */
	private JCheckBox chbNumeric;

	/** QuickFilter: Date_time */
	private JCheckBox chbDateTime;

	/** the input categorization panes */
	private Map<String, JXTaskPane> inputCategoryTaskPanes;

	/** model of the inputs */
	private FunctionInputsModel inputsModel = new FunctionInputsModel();

	/** observer to update the GUI, if the model changed or an input value was clicked */
	private PrivateInputObserver inputObserver = new PrivateInputObserver();

	/** listener which checks if the value of the filter string changed */
	private FilterListener filterInputsListener = new FilterListener() {

		@Override
		public void valueChanged(String value) {
			// gives the filter string to the model
			inputsModel.setFilterNameString(value);
		}
	};

	/** action which is executed when the filter search field is cleared */
	private transient final ResourceAction clearInputsFilterAction = new ResourceAction(true, "clear_filter") {

		private static final long serialVersionUID = 3236281211064051583L;

		@Override
		public void loggedActionPerformed(final ActionEvent e) {
			inputsFilterField.clearFilter();
			inputsModel.setFilterNameString("");
			inputsFilterField.requestFocusInWindow();
		}
	};

	/** panel containing this {@link FunctionInput}s part */
	private JPanel inputsPanel = new JPanel();

	/** layout of the inputs part */
	private GridBagLayout inputsLayout;

	// FUNCTIONS
	/** search text field in the {@link FunctionDescription}s part */
	private final FilterTextField functionsFilterField = new FilterTextField(12);

	/** the function categorization panes */
	private Map<String, JXTaskPane> functionCategoryTaskPanes;

	/** model of the {@link FunctionDescription}s */
	private FunctionDescriptionModel functionModel = new FunctionDescriptionModel();

	/** observer to update the GUI, if the model changed or a function was clicked */
	private PrivateModelObserver functionObserver = new PrivateModelObserver();

	/** listener which checks if the value of the filter string changed */
	private FilterListener filterFunctionsListener = new FilterListener() {

		@Override
		public void valueChanged(String value) {
			functionModel.setFilterNameString(value);
		}
	};

	/** action which is executed when the filter search field is cleared */
	private transient final ResourceAction clearFunctionsFilterAction = new ResourceAction(true, "clear_filter") {

		private static final long serialVersionUID = 3236281211064051583L;

		@Override
		public void loggedActionPerformed(final ActionEvent e) {
			functionsFilterField.clearFilter();
			functionModel.setFilterNameString("");
			functionsFilterField.requestFocusInWindow();
		}
	};

	/** scroll pane containing the different {@link FunctionDescriptionPanel}s */
	private JScrollPane functionButtonScrollPane;

	/** panel containing the {@link FunctionDescription}s part */
	private JPanel functionsPanel = new JPanel();

	/** layout of the functions part */
	private GridBagLayout functionButtonsLayout = new GridBagLayout();

	/**
	 * hack to prevent filter popup (inputs part) from opening itself again when you click the
	 * button to actually close it while it is open
	 */
	private long lastPopupCloseTime;

	/** syntax highlighting color */
	private static final Color DARK_PURPLE = new Color(139, 0, 139);

	/** syntax highlighting color */
	private static final Color DARK_CYAN = new Color(0, 139, 139);

	/** color of the validation label, when no error occured */
	private static final Color DARK_GREEN = new Color(45, 136, 45);

	/** the background color of the lists with functions */
	private static final Color LIGHTER_GRAY = Colors.WINDOW_BACKGROUND;

	/** Color of the expression border */
	private static final Color COLOR_BORDER_EXPRESSION = Colors.TEXTFIELD_BORDER;

	/** Color of the category title {@link JXTaskPane}s */
	private static final Color COLOR_TITLE_TASKPANE_BACKGROUND = new Color(230, 230, 230);

	/** icon to of the search text field, which is shown when the clear filter icon is hovered */
	private static final ImageIcon CLEAR_FILTER_HOVERED_ICON = SwingTools.createIcon("16/x-mark_orange.png");

	/** maximal number of {@link FunctionInput}s that is shown in open {@link JXTaskPane}(s) */
	private static final int MAX_NMBR_INPUTS_SHOWN = 7;

	/**
	 * maximal number of {@link FunctionDescription}s that is shown in open {@link JXTaskPane}(s)
	 */
	private static final int MAX_NMBR_FUNCTIONS_SHOWN = 15;

	/** size of the expression */
	private static final int NUMBER_OF_EXPRESSION_ROWS = 6;

	/** message when the search in the model resulted in an empty set */
	private static final String MESSAGE_NO_RESULTS = " No search results found.";

	/** Icon name of the error icon (parser) */
	private static final String ERROR_ICON_NAME = "error.png";

	/** The font size of the categories({@link JXTaskPane}s) titles */
	private static final String FONT_SIZE_HEADER = "5";

	/** maximal width of the inputs panel part */
	private static final int WIDTH_ATTRIBUTE_PANEL = 350;

	/** width of the search field in the inputs part */
	private static final int WIDTH_INPUTS_SEARCH_FIELD = 200;

	/** width of the search field in the functions part */
	private static final int WIDTH_FUNCTION_SEARCH_FIELD = 300;

	/** height of the expression field */
	private static final int HEIGHT_EXPRESSION_SCROLL_PANE = 120;

	/** height of the search fields */
	private static final int HEIGHT_SEARCH_FIELD = 24;

	/** standard padding size */
	private static final int STD_INSET_GBC = 7;

	/** error icon (parser) */
	private static Icon ERROR_ICON = null;

	static {
		ERROR_ICON = SwingTools.createIcon("13/" + ERROR_ICON_NAME);
	}

	/** label showing a validation result of the expression */
	private JLabel validationLabel = new JLabel();

	private JTextArea validationTextArea = new JTextArea();

	/** typical filter icon */
	private static final ImageIcon ICON_FILTER = SwingTools.createIcon("16/" + "funnel.png");

	/**
	 * Creates an {@link ExpressionPropertyDialog} with the given initial value.
	 *
	 * @param type
	 * @param initialValue
	 */
	public ExpressionPropertyDialog(final ParameterTypeExpression type, String initialValue) {
		this(type, null, initialValue);
	}

	/**
	 * Creates an {@link ExpressionPropertyDialog} with the given initial value, controlling
	 * process, expression parser, input model and function model
	 *
	 * @param type
	 * @param process
	 * @param inputs
	 * @param functions
	 * @param parser
	 * @param initialValue
	 */
	public ExpressionPropertyDialog(ParameterTypeExpression type, Process process, List<FunctionInput> inputs,
			List<FunctionDescription> functions, ExpressionParser parser, String initialValue) {
		super(type, "expression");

		this.inputsModel.addObserver(inputObserver, false);
		this.functionModel.addObserver(functionObserver, false);

		this.controllingProcess = getControllingProcessOrNull(type, process);

		this.inputsModel.addContent(inputs);
		this.functionModel.addContent(functions);

		this.parser = parser;

		ExpressionTokenMaker.removeFunctionInputs();
		ExpressionTokenMaker.addFunctionInputs(inputs);
		ExpressionTokenMaker.addFunctions(functions);

		initGui(initialValue);

		FunctionDescriptionPanel.updateMaximalWidth(functionsPanel.getSize().width);
		updateFunctions();
	}

	/**
	 * Creates an {@link ExpressionPropertyDialog} with the given initial value and a controlling
	 * process
	 *
	 * @param type
	 * @param process
	 * @param initialValue
	 */
	public ExpressionPropertyDialog(ParameterTypeExpression type, Process process, String initialValue) {
		super(type, "expression");

		// add observers to receive model updates
		inputsModel.addObserver(inputObserver, false);
		functionModel.addObserver(functionObserver, false);

		// create ExpressionParser with Process to enable Process functions
		controllingProcess = getControllingProcessOrNull(type, process);

		// use the ExpressionParserBuilder to create the parser
		ExpressionParserBuilder builder = new ExpressionParserBuilder();

		// use a compatibility level to only show functions that are available
		if (type.getInputPort() != null) {
			builder = builder
					.withCompatibility(type.getInputPort().getPorts().getOwner().getOperator().getCompatibilityLevel());
		}
		if (controllingProcess != null) {
			builder = builder.withProcess(controllingProcess);
			// make macros available to the parser
			builder = builder.withScope(new MacroResolver(controllingProcess.getMacroHandler()));
		}

		// make attributes available to the parser
		InputPort inPort = ((ParameterTypeExpression) getParameterType()).getInputPort();
		if (inPort != null) {
			if (inPort.getMetaData() instanceof ExampleSetMetaData) {
				ExampleSetMetaData emd = (ExampleSetMetaData) inPort.getMetaData();
				if (emd != null) {
					builder = builder.withDynamics(new ExampleResolver(emd));
				}
			} else if (inPort.getMetaData() instanceof ModelMetaData) {
				ModelMetaData mmd = (ModelMetaData) inPort.getMetaData();
				if (mmd != null) {
					ExampleSetMetaData emd = mmd.getTrainingSetMetaData();
					if (emd != null) {
						builder = builder.withDynamics(new ExampleResolver(emd));
					}
				}
			}
		}

		// show all registered modules
		builder = builder.withModules(ExpressionRegistry.INSTANCE.getAll());

		// finally create the parser
		parser = builder.build();

		// fetch the expression context from the parser
		// add the current function inputs to the functions input model
		inputsModel.addContent(parser.getExpressionContext().getFunctionInputs());

		// add the existing functions to the functions model
		functionModel.addContent(parser.getExpressionContext().getFunctionDescriptions());

		// remove deprecated expression context from the syntax highlighting
		ExpressionTokenMaker.removeFunctionInputs();
		// add the current expression context to the syntax highlighting
		ExpressionTokenMaker.addFunctionInputs(parser.getExpressionContext().getFunctionInputs());
		ExpressionTokenMaker.addFunctions(parser.getExpressionContext().getFunctionDescriptions());

		// initialize the UI
		initGui(initialValue);

		FunctionDescriptionPanel.updateMaximalWidth(functionsPanel.getSize().width);
		updateFunctions();
	}

	private Process getControllingProcessOrNull(ParameterTypeExpression type, Process process) {
		if (process != null) {
			return process;
		} else if (type.getInputPort() != null) {
			return type.getInputPort().getPorts().getOwner().getOperator().getProcess();
		} else {
			return null;
		}
	}

	/**
	 * Initializes the UI of the dialog.
	 *
	 * @param initialValue
	 *            of the expression
	 */
	public void initGui(String initialValue) {

		// this is the only way to set colors for the JXTaskPane component
		/* background color */
		UIManager.put("TaskPane.background", LIGHTER_GRAY);
		/* title hover color */
		UIManager.put("TaskPane.titleOver", SwingTools.RAPIDMINER_ORANGE);
		UIManager.put("TaskPane.specialTitleOver", SwingTools.RAPIDMINER_ORANGE);
		/* border color */
		UIManager.put("TaskPane.borderColor", LIGHTER_GRAY);
		/* foreground */
		UIManager.put("TaskPane.foreground", Color.black);
		UIManager.put("TaskPane.titleForeground", Color.black);
		UIManager.put("TaskPane.specialTitleForeground", Color.black);
		/* title background */
		UIManager.put("TaskPane.specialTitleBackground", COLOR_TITLE_TASKPANE_BACKGROUND);
		UIManager.put("TaskPane.titleBackgroundGradientStart", COLOR_TITLE_TASKPANE_BACKGROUND);
		UIManager.put("TaskPane.titleBackgroundGradientEnd", COLOR_TITLE_TASKPANE_BACKGROUND);

		// add OK and cancel button
		Collection<AbstractButton> buttons = new LinkedList<AbstractButton>();
		final JButton okButton = makeOkButton("expression_property_dialog_apply");
		buttons.add(okButton);
		buttons.add(makeCancelButton());

		// Create the main panel
		JPanel mainPanel = new JPanel();
		GridBagLayout mainLayout = new GridBagLayout();
		mainPanel.setLayout(mainLayout);
		GridBagConstraints mainC = new GridBagConstraints();
		mainC.fill = GridBagConstraints.BOTH;
		mainC.weightx = 1;
		mainC.weighty = 0;
		mainC.gridwidth = 2;
		mainC.gridx = 0;
		mainC.gridy = 0;
		mainC.insets = new Insets(0, STD_INSET_GBC, 0, STD_INSET_GBC);

		// EXPRESSION
		JPanel expressionPanel = new JPanel();
		GridBagLayout expressionLayout = new GridBagLayout();
		expressionPanel.setLayout(expressionLayout);
		GridBagConstraints expressionC = new GridBagConstraints();
		expressionC.fill = GridBagConstraints.BOTH;
		expressionC.insets = new Insets(STD_INSET_GBC, STD_INSET_GBC, STD_INSET_GBC, STD_INSET_GBC);
		expressionC.gridx = 0;
		expressionC.gridy = 0;
		expressionC.gridwidth = 2;
		expressionC.weightx = 0;
		expressionC.weighty = 0;

		// expression title
		JLabel label = new JLabel("<html><b><font size=" + FONT_SIZE_HEADER + ">Expression</font></b></html>");
		expressionC.gridy += 1;
		expressionPanel.add(label, expressionC);

		// current expression
		// validate the expression when it was changed
		currentExpression.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getModifiersEx() == KeyEvent.CTRL_DOWN_MASK) {
					if (e.getExtendedKeyCode() == KeyEvent.VK_ENTER) {
						okButton.doClick();
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				validateExpression();
			}
		});

		// Use the custom token maker to highlight RapidMiner expressions
		AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
		atmf.putMapping("text/expression", "com.rapidminer.gui.tools.syntax.ExpressionTokenMaker");
		currentExpression.setSyntaxEditingStyle("text/expression");
		// the current line should not be highlighted
		currentExpression.setHighlightCurrentLine(false);
		// enable bracket matching (just works if brackets have the token type Token.SEPARATOR)
		currentExpression.setBracketMatchingEnabled(true);
		currentExpression.setAnimateBracketMatching(true);
		currentExpression.setPaintMatchedBracketPair(true);

		// set initial size
		currentExpression.setRows(NUMBER_OF_EXPRESSION_ROWS);
		// set custom colors for syntax highlighting
		currentExpression.setSyntaxScheme(getExpressionColorScheme(currentExpression.getSyntaxScheme()));
		currentExpression.setBorder(BorderFactory.createEmptyBorder());
		scrollPaneExpression = new RTextScrollPane(currentExpression, true);
		scrollPaneExpression.setMinimumSize(new Dimension(getMinimumSize().width, HEIGHT_EXPRESSION_SCROLL_PANE));
		scrollPaneExpression.setPreferredSize(new Dimension(getPreferredSize().width, HEIGHT_EXPRESSION_SCROLL_PANE));
		scrollPaneExpression.setMaximumSize(new Dimension(getMaximumSize().width, HEIGHT_EXPRESSION_SCROLL_PANE));
		scrollPaneExpression.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 0, COLOR_BORDER_EXPRESSION));
		scrollPaneExpression.getVerticalScrollBar()
				.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Colors.TEXTFIELD_BORDER));

		// use the gutter to display an error icon in the line with an error
		Gutter gutter = scrollPaneExpression.getGutter();
		gutter.setBookmarkingEnabled(true);

		expressionC.gridy += 1;
		expressionC.weightx = 1;
		expressionC.insets = new Insets(0, STD_INSET_GBC, STD_INSET_GBC, STD_INSET_GBC);
		expressionC.fill = GridBagConstraints.BOTH;
		expressionC.gridwidth = 6;
		expressionPanel.add(scrollPaneExpression, expressionC);

		JPanel validationPanel = new JPanel();
		validationPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		// insert validation label
		validationLabel.setAlignmentX(SwingConstants.LEFT);
		validationPanel.add(validationLabel, gbc);

		gbc.gridy += 1;
		validationTextArea.setAlignmentX(SwingConstants.LEFT);
		validationTextArea.setEditable(false);
		validationTextArea.setRows(2);
		validationTextArea.setOpaque(false);
		validationTextArea.setBorder(BorderFactory.createEmptyBorder());
		validationPanel.add(validationTextArea, gbc);

		expressionC.fill = GridBagConstraints.BOTH;
		expressionC.insets = new Insets(STD_INSET_GBC, STD_INSET_GBC, 0, STD_INSET_GBC);
		expressionC.gridx = 0;
		expressionC.weightx = 1;
		expressionC.gridy += 1;
		expressionC.gridwidth = 6;
		expressionC.anchor = GridBagConstraints.NORTHWEST;
		expressionPanel.add(validationPanel, expressionC);

		// add expression part to the main panel
		mainPanel.add(expressionPanel, mainC);

		// FUNCTIONS
		JPanel functionPanel = new JPanel();
		GridBagLayout functionsLayout = new GridBagLayout();
		functionPanel.setLayout(functionsLayout);
		GridBagConstraints functionsC = new GridBagConstraints();

		// add functions title
		functionsC.insets = new Insets(0, STD_INSET_GBC, STD_INSET_GBC, STD_INSET_GBC);
		functionsC.gridy = 0;
		functionsC.gridx = 0;
		functionsC.anchor = GridBagConstraints.NORTHWEST;
		functionPanel.add(new JLabel("<html><b><font size=" + FONT_SIZE_HEADER + ">Functions</font></b></html>"),
				functionsC);

		functionsC.insets = new Insets(0, 0, STD_INSET_GBC, STD_INSET_GBC);
		functionsC.gridx += 1;
		functionsC.anchor = GridBagConstraints.SOUTHEAST;

		// add search field for functions
		functionsFilterField.addFilterListener(filterFunctionsListener);
		TextFieldWithAction textField = new TextFieldWithAction(functionsFilterField, clearFunctionsFilterAction,
				CLEAR_FILTER_HOVERED_ICON);
		textField.setMinimumSize(new Dimension(WIDTH_FUNCTION_SEARCH_FIELD, HEIGHT_SEARCH_FIELD));
		textField.setPreferredSize(new Dimension(WIDTH_FUNCTION_SEARCH_FIELD, HEIGHT_SEARCH_FIELD));
		textField.setMaximumSize(new Dimension(WIDTH_FUNCTION_SEARCH_FIELD, HEIGHT_SEARCH_FIELD));
		functionPanel.add(textField, functionsC);

		// create the functions panel and display the existing functions
		updateFunctions();

		JPanel outerFunctionPanel = new JPanel();
		outerFunctionPanel.setLayout(new GridBagLayout());
		GridBagConstraints outerFunctionC = new GridBagConstraints();
		outerFunctionC.gridwidth = GridBagConstraints.REMAINDER;
		outerFunctionC.fill = GridBagConstraints.HORIZONTAL;
		outerFunctionC.weightx = 1;
		outerFunctionC.weighty = 1;
		outerFunctionC.anchor = GridBagConstraints.NORTHWEST;
		functionsPanel.setBackground(LIGHTER_GRAY);
		outerFunctionPanel.add(functionsPanel, outerFunctionC);

		outerFunctionC.fill = GridBagConstraints.BOTH;
		JPanel gapPanel2 = new JPanel();
		gapPanel2.setBackground(LIGHTER_GRAY);
		outerFunctionPanel.add(gapPanel2, outerFunctionC);
		outerFunctionPanel.setBackground(LIGHTER_GRAY);

		// add the functions panel to the scroll bar
		functionButtonScrollPane = new ExtendedJScrollPane(outerFunctionPanel);
		functionButtonScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Colors.TEXTFIELD_BORDER));
		// the scroll bar should always be visible
		functionButtonScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		// add scroll pane to function panel
		functionsC.gridx = 0;
		functionsC.gridy += 1;
		functionsC.fill = GridBagConstraints.BOTH;
		functionsC.weightx = 1;
		functionsC.weighty = 1;
		functionsC.insets = new Insets(0, STD_INSET_GBC, STD_INSET_GBC, STD_INSET_GBC);
		functionsC.gridwidth = 2;
		functionsC.anchor = GridBagConstraints.NORTH;
		functionPanel.add(functionButtonScrollPane, functionsC);

		// add function panel to the main panel
		mainC.weighty = 1;
		mainC.gridwidth = 1;
		mainC.weightx = 0.9;
		mainC.gridy += 1;
		mainPanel.add(functionPanel, mainC);

		// INPUTS
		inputsLayout = new GridBagLayout();
		inputsPanel.setLayout(inputsLayout);

		// add inputs panel to the outer panel
		JPanel outerInputPanel = new JPanel();
		outerInputPanel.setLayout(new GridBagLayout());
		GridBagConstraints outerInputC = new GridBagConstraints();
		outerInputC.gridwidth = GridBagConstraints.REMAINDER;
		outerInputC.fill = GridBagConstraints.HORIZONTAL;
		outerInputC.weightx = 1;
		outerInputC.weighty = 1;
		outerInputC.anchor = GridBagConstraints.NORTHWEST;
		inputsPanel.setBackground(LIGHTER_GRAY);
		outerInputPanel.add(inputsPanel, outerInputC);

		outerInputC.weighty = 1;
		outerInputC.fill = GridBagConstraints.BOTH;
		JPanel gapPanel = new JPanel();
		gapPanel.setBackground(LIGHTER_GRAY);
		outerInputPanel.add(gapPanel, outerInputC);

		// and update the view of the inputs
		if (inputsModel.getFilteredModel().size() > 0) {
			updateInputs();

			// add inputs title
			JPanel outerInputsPanel = new JPanel();
			outerInputsPanel.setLayout(new GridBagLayout());
			outerInputsPanel.setMinimumSize(new Dimension(WIDTH_ATTRIBUTE_PANEL, getMinimumSize().height));
			outerInputsPanel.setPreferredSize(new Dimension(WIDTH_ATTRIBUTE_PANEL, getPreferredSize().height));
			outerInputsPanel.setMaximumSize(new Dimension(WIDTH_ATTRIBUTE_PANEL, getMaximumSize().height));
			GridBagConstraints outerGBC = new GridBagConstraints();

			outerGBC.gridx = 0;
			outerGBC.gridy = 0;
			outerGBC.insets = new Insets(0, STD_INSET_GBC, STD_INSET_GBC, STD_INSET_GBC);
			outerGBC.anchor = GridBagConstraints.NORTHWEST;
			outerInputsPanel.add(new JLabel("<html><b><font size=" + FONT_SIZE_HEADER + ">Inputs</font></b></html>"),
					outerGBC);

			outerGBC.gridx += 1;
			outerGBC.weightx = 1;
			outerInputsPanel.add(new JLabel(" "), outerGBC);

			// add search text field for FunctionInputs
			outerGBC.gridx += 1;
			outerGBC.weightx = 0.1;
			outerGBC.anchor = GridBagConstraints.SOUTHEAST;
			outerGBC.insets = new Insets(0, 0, STD_INSET_GBC, 0);

			inputsFilterField.addFilterListener(filterInputsListener);
			TextFieldWithAction inputTextField = new TextFieldWithAction(inputsFilterField, clearInputsFilterAction,
					CLEAR_FILTER_HOVERED_ICON);
			inputTextField.setMaximumSize(new Dimension(WIDTH_INPUTS_SEARCH_FIELD, HEIGHT_SEARCH_FIELD));
			inputTextField.setPreferredSize(new Dimension(WIDTH_INPUTS_SEARCH_FIELD, HEIGHT_SEARCH_FIELD));
			inputTextField.setMinimumSize(new Dimension(WIDTH_INPUTS_SEARCH_FIELD, HEIGHT_SEARCH_FIELD));
			outerInputsPanel.add(inputTextField, outerGBC);

			// Add type filter for nominal input values
			chbNominal = new JCheckBox(new ResourceAction(true, "expression_property_dialog.quick_filter.nominal") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent arg0) {
					inputsModel.setNominalFilter(chbNominal.isSelected());
				}
			});
			chbNominal.setSelected(inputsModel.isNominalFilterToggled());

			// Add type filter for numerical input values
			chbNumeric = new JCheckBox(new ResourceAction(true, "expression_property_dialog.quick_filter.numerical") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent arg0) {
					inputsModel.setNumericFilter(chbNumeric.isSelected());
				}
			});
			chbNumeric.setSelected(inputsModel.isNumericFilterToggled());

			// Add type filter for date time input values
			chbDateTime = new JCheckBox(new ResourceAction(true, "expression_property_dialog.quick_filter.date_time") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent arg0) {
					inputsModel.setDateTimeFilter(chbDateTime.isSelected());
				}
			});
			chbDateTime.setSelected(inputsModel.isDateTimeFilterToggled());

			// create the menu with the type filters
			final ScrollableJPopupMenu filterMenu = new ScrollableJPopupMenu();
			// small hack to prevent the popup from opening itself when you click the button to
			// actually
			// close it
			filterMenu.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {}

				@Override
				public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
					lastPopupCloseTime = System.currentTimeMillis();
				}

				@Override
				public void popupMenuCanceled(final PopupMenuEvent e) {}
			});

			filterMenu.add(chbNominal);
			filterMenu.add(chbNumeric);
			filterMenu.add(chbDateTime);

			// create button to open the type filter menu
			final JButton filterDropdownButton = new JButton(ICON_FILTER);

			filterDropdownButton.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			filterDropdownButton.setContentAreaFilled(false);
			filterDropdownButton.addMouseListener(new HoverBorderMouseListener(filterDropdownButton));
			filterDropdownButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
					"gui.label.expression_property_dialog.quick_filter.filter_select.tip"));

			// show the menu when the button is clicked
			filterDropdownButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {
					if (!filterMenu.isVisible()) {
						// hack to prevent filter popup from opening itself again when you click the
						// button to actually close it while it is open
						if (System.currentTimeMillis() - lastPopupCloseTime < 250) {
							return;
						}

						int menuWidth = filterMenu.getSize().width;
						if (menuWidth == 0) {
							// guess the correct width for the first opening
							menuWidth = 108;
						}

						filterMenu.show(filterDropdownButton, -menuWidth + filterDropdownButton.getSize().width,
								filterDropdownButton.getHeight());

						filterMenu.requestFocusInWindow();
					}
				}
			});

			outerGBC.gridx += 1;
			outerGBC.insets = new Insets(0, 0, STD_INSET_GBC, STD_INSET_GBC);
			outerInputsPanel.add(filterDropdownButton, outerGBC);

			// create scroll bar for inputs
			outerInputPanel.setBackground(LIGHTER_GRAY);
			ExtendedJScrollPane inputsScrollPane = new ExtendedJScrollPane(outerInputPanel);
			inputsScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Colors.TEXTFIELD_BORDER));
			inputsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

			outerGBC.gridx = 0;
			outerGBC.insets = new Insets(0, STD_INSET_GBC, STD_INSET_GBC, STD_INSET_GBC);
			outerGBC.gridwidth = 4;
			outerGBC.gridy += 1;
			outerGBC.fill = GridBagConstraints.BOTH;
			outerGBC.anchor = GridBagConstraints.NORTH;
			outerGBC.weightx = 1;
			outerGBC.weighty = 1;
			outerInputsPanel.add(inputsScrollPane, outerGBC);

			// add inputs part to the main panel
			mainC.weightx = 0.1;
			mainC.gridx += 1;
			mainPanel.add(outerInputsPanel, mainC);
		}

		setIconImage(SwingTools.createIcon("16/rapidminer_studio.png").getImage());
		layoutDefault(mainPanel, HUGE, buttons.toArray(new AbstractButton[buttons.size()]));

		// if an initial value is given, set it
		if (initialValue != null) {
			currentExpression.setText(initialValue);
		}

		// validate the expression
		validateExpression();
		setResizable(false);
	}

	/**
	 * @return current expression
	 */
	public String getExpression() {
		return currentExpression.getText();
	}

	@Override
	protected Icon getInfoIcon() {
		return null;
	}

	@Override
	protected String getInfoText() {
		return "";
	}

	/**
	 * Adds the given value to the expression text
	 *
	 * @param value
	 *            that is added to the expression text
	 */
	private void addToExpression(String value) {
		if (value == null) {
			return;
		}

		String selectedText = currentExpression.getSelectedText();
		if (selectedText != null && selectedText.length() > 0) {
			// replace selected text by function including the selection as argument (if the string
			// to be added actually IS a function...)
			if (value.endsWith("()")) {
				int selectionStart = currentExpression.getSelectionStart();
				int selectionEnd = currentExpression.getSelectionEnd();
				String text = currentExpression.getText();
				String firstPart = text.substring(0, selectionStart);
				String lastPart = text.substring(selectionEnd);

				currentExpression.setText(firstPart + value + lastPart);
				int lengthForCaretPosition = value.length();
				if (value.endsWith("()")) {
					lengthForCaretPosition--;
				}
				currentExpression.setCaretPosition(selectionStart + lengthForCaretPosition);

				addToExpression(selectedText);
				currentExpression.setCaretPosition(currentExpression.getCaretPosition() + 1);

				validateExpression();
				requestExpressionFocus();
			} else {
				int selectionStart = currentExpression.getSelectionStart();
				int selectionEnd = currentExpression.getSelectionEnd();
				String text = currentExpression.getText();
				String firstPart = text.substring(0, selectionStart);
				String lastPart = text.substring(selectionEnd);

				currentExpression.setText(firstPart + value + lastPart);
				int lengthForCaretPosition = value.length();
				if (value.endsWith("()")) {
					lengthForCaretPosition--;
				}
				currentExpression.setCaretPosition(selectionStart + lengthForCaretPosition);

				validateExpression();
				requestExpressionFocus();
			}
		} else {
			// just add the text at the current caret position
			int caretPosition = currentExpression.getCaretPosition();
			String text = currentExpression.getText();
			if (text != null && text.length() > 0) {
				String firstPart = text.substring(0, caretPosition);
				String lastPart = text.substring(caretPosition);
				currentExpression.setText(firstPart + value + lastPart);

				int lengthForCaretPosition = value.length();
				if (value.endsWith("()")) {
					lengthForCaretPosition--;
				}
				currentExpression.setCaretPosition(caretPosition + lengthForCaretPosition);
			} else {
				currentExpression.setText(value);
				int lengthForCaretPosition = value.length();
				if (value.endsWith("()")) {
					lengthForCaretPosition--;
				}
				currentExpression.setCaretPosition(caretPosition + lengthForCaretPosition);
				currentExpression.setCaretPosition(lengthForCaretPosition);
			}

			validateExpression();
			requestExpressionFocus();
		}
	}

	/**
	 * Adds the given value (function name) to the expression text. If the function has no
	 * arguments, the caret is placed after the function's right parenthesis.
	 *
	 * @param value
	 * @param function
	 */
	private void addToExpression(String value, FunctionDescription function) {
		addToExpression(value);
		if (function.getNumberOfArguments() == 0) {
			currentExpression.setCaretPosition(currentExpression.getCaretPosition() + 1);
		}
	}

	/**
	 * Requests focus on the expression text
	 */
	private void requestExpressionFocus() {
		currentExpression.requestFocusInWindow();
	}

	/**
	 * Updates the function panel
	 */
	private void updateFunctions() {

		// remove all content
		functionsPanel.removeAll();
		functionsPanel.setLayout(functionButtonsLayout);
		functionCategoryTaskPanes = new HashMap<>();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		int totalFunctionCount = 0;

		// get the filtered model (the FunctionDescriptions we want to display)
		Map<String, List<FunctionDescription>> filteredModel = functionModel.getFilteredModel();
		String filterName = functionModel.getFilterNameString();
		boolean searchStringGiven = !filterName.isEmpty();

		for (String functionGroup : filteredModel.keySet()) {

			boolean perfectMatch = false;
			JXTaskPane functionCategoryTaskPane = new JXTaskPane();
			functionCategoryTaskPane.setName(functionGroup);
			List<FunctionDescription> list = functionModel.getFilteredModel(functionGroup);
			totalFunctionCount += list.size();

			for (FunctionDescription function : list) {
				// create the panels for the functions, register the observer and add them to the
				// related category
				final FunctionDescriptionPanel funcDescPanel = new FunctionDescriptionPanel(function);
				funcDescPanel.registerObserver(functionObserver);
				functionCategoryTaskPane.add(funcDescPanel);
				if (!perfectMatch && searchStringGiven) {
					// check for function name equality without brackets and with brackets
					String functionName = function.getDisplayName().split("\\(")[0];
					if (filterName.toLowerCase(Locale.ENGLISH).equals(functionName.toLowerCase(Locale.ENGLISH)) || filterName
							.toLowerCase(Locale.ENGLISH).equals(function.getDisplayName().toLowerCase(Locale.ENGLISH))) {
						perfectMatch = true;
					}
				}
			}

			functionCategoryTaskPane.setTitle(functionGroup);
			functionCategoryTaskPane.setAnimated(false);

			// if there is only one category in the filtered model, open the task pane
			if (filteredModel.keySet().size() == 1) {
				functionCategoryTaskPane.setCollapsed(false);
			} else {
				functionCategoryTaskPane.setCollapsed(true);
			}

			if (perfectMatch) {
				functionCategoryTaskPane.setCollapsed(false);
			}

			functionCategoryTaskPanes.put(functionGroup, functionCategoryTaskPane);

			gbc.ipady = 10;
			functionsPanel.add(functionCategoryTaskPane, gbc);
			gbc.gridy += 1;
		}

		// if the number of result functions is clear, open the task panes
		// (if you can see all categories even if they are opened)
		if (totalFunctionCount <= MAX_NMBR_FUNCTIONS_SHOWN) {
			for (JXTaskPane taskPane : functionCategoryTaskPanes.values()) {
				taskPane.setCollapsed(false);
			}
		}

		// if there are no results, show a simple message
		if (filteredModel.isEmpty()) {
			gbc.ipady = 10;
			functionsPanel.add(new JLabel(MESSAGE_NO_RESULTS), gbc);
		}

		functionsPanel.revalidate();
	}

	/**
	 * updates the inputs panel
	 */
	private void updateInputs() {

		// remove all content
		inputsPanel.removeAll();
		inputsPanel.setLayout(inputsLayout);
		inputCategoryTaskPanes = new HashMap<>();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		int totalEntryCount = 0;

		// get the filtered model (the FunctionInputs we want to display)
		Map<String, List<FunctionInput>> filteredModel = inputsModel.getFilteredModel();
		String filterName = inputsModel.getFilterNameString();
		boolean searchStringGiven = !filterName.isEmpty();
		List<String> keySet = new LinkedList<>(filteredModel.keySet());
		boolean anyPerfectMatch = false;

		for (String type : keySet) {
			boolean perfectMatch = false;
			JXTaskPane inputCategoryTaskPane = new JXTaskPane();
			inputCategoryTaskPane.setName(type);
			List<FunctionInput> list = inputsModel.getFilteredModel(type);

			for (FunctionInput entry : list) {

				// set a value where we want to see a role, value or description in a second line
				String value = null;
				if (entry.getCategory() == Category.DYNAMIC || entry.getCategory() == Category.CONSTANT) {
					totalEntryCount += 1;
					value = entry.getAdditionalInformation();

				} else if (entry.getCategory() == Category.SCOPE) {
					totalEntryCount += 1;
					// use the current scope value
					value = parser.getExpressionContext().getScopeString(entry.getName());
				}

				FunctionInputPanel inputPanel = null;
				// create the panels for the inputs, register the observer and add them to the
				// related category
				if (value == null) {
					inputPanel = new FunctionInputPanel(entry);
				} else {
					inputPanel = new FunctionInputPanel(entry, value);
				}
				inputPanel.registerObserver(inputObserver);
				inputCategoryTaskPane.add(inputPanel);

				if (!perfectMatch && searchStringGiven) {
					// check if the input name is equal to search term
					if (filterName.toLowerCase(Locale.ENGLISH).equals(entry.getName().toLowerCase(Locale.ENGLISH))) {
						perfectMatch = true;
						anyPerfectMatch = true;
					}
				}
			}

			inputCategoryTaskPane.setTitle(type);
			inputCategoryTaskPane.setAnimated(false);

			// if there is only one category in the filtered model, open the task pane
			if (filteredModel.keySet().size() == 1) {
				inputCategoryTaskPane.setCollapsed(false);
			} else {
				inputCategoryTaskPane.setCollapsed(true);
			}

			if (perfectMatch) {
				inputCategoryTaskPane.setCollapsed(false);
			}

			inputCategoryTaskPanes.put(type, inputCategoryTaskPane);

			gbc.ipady = 10;
			inputsPanel.add(inputCategoryTaskPane, gbc);
			gbc.gridy += 1;
		}

		// if the number of result inputs is clear, open the task panes
		// (if you can see all categories even if they are opened)
		if (totalEntryCount <= MAX_NMBR_INPUTS_SHOWN) {
			for (JXTaskPane taskPane : inputCategoryTaskPanes.values()) {
				taskPane.setCollapsed(false);
			}
		} else {
			// if there was no perfect match open attributes if there are not too much entries
			if (!anyPerfectMatch) {
				// if attributes can be opened such that you can see that there are more categories,
				// open the attributes categories
				if (filteredModel.get(ExampleResolver.KEY_ATTRIBUTES) != null
						&& filteredModel.get(ExampleResolver.KEY_SPECIAL_ATTRIBUTES) != null
						&& filteredModel.get(ExampleResolver.KEY_ATTRIBUTES).size() + filteredModel
								.get(ExampleResolver.KEY_SPECIAL_ATTRIBUTES).size() <= MAX_NMBR_INPUTS_SHOWN) {

					inputCategoryTaskPanes.get(ExampleResolver.KEY_ATTRIBUTES).setCollapsed(false);
					inputCategoryTaskPanes.get(ExampleResolver.KEY_SPECIAL_ATTRIBUTES).setCollapsed(false);

				} else if (filteredModel.get(ExampleResolver.KEY_ATTRIBUTES) != null
						&& filteredModel.get(ExampleResolver.KEY_ATTRIBUTES).size() <= MAX_NMBR_INPUTS_SHOWN) {

					inputCategoryTaskPanes.get(ExampleResolver.KEY_ATTRIBUTES).setCollapsed(false);
				}
			}
		}

		// if there are no results, show a simple message
		if (filteredModel.isEmpty()) {
			gbc.ipady = 10;
			inputsPanel.add(new JLabel(MESSAGE_NO_RESULTS), gbc);
		}

		inputsPanel.revalidate();
	}

	/**
	 * Validates the syntax of the current text in the expression field
	 */
	private void validateExpression() {
		// remove error buttons
		removeLineSignals();
		String expression = currentExpression.getText();
		if (expression != null) {
			if (expression.trim().length() > 0) {
				try {
					// make a syntax check
					parser.checkSyntax(expression);
					// show status of the expression
					showError(false, "<b>Info: </b>", "Expression is syntactically correct.");
				} catch (ExpressionException e) {
					// show status of the expression
					showError(true, "<b>Error: </b>", e.getMessage());
					// if the line of the error is given, show an error icon in this line
					int line = e.getErrorLine();
					if (line > 0) {
						signalLine(line);
					}
					return;
				}
			} else {
				// show status of the expression
				showError(false, "", "Please specify a valid expression.");
			}
		} else {
			// show status of the expression
			showError(false, "", "Please specify a valid expression.");
		}
	}

	/**
	 * Changes the {@link SyntaxScheme} of a {@link RSyntaxTextArea} to use custom colors
	 *
	 * @param textAreaSyntaxScheme
	 *            the {@link SyntaxScheme} which should be changed
	 * @return the changed {@link SyntaxScheme} with custom colors
	 */
	private SyntaxScheme getExpressionColorScheme(SyntaxScheme textAreaSyntaxScheme) {
		SyntaxScheme ss = textAreaSyntaxScheme;
		// show brackets in dark purple
		ss.setStyle(Token.SEPARATOR, new Style(DARK_PURPLE));
		// show double quotes / strings in dark cyan
		ss.setStyle(Token.LITERAL_STRING_DOUBLE_QUOTE, new Style(DARK_CYAN));
		// show attributes in RapidMiner orange
		ss.setStyle(Token.VARIABLE, new Style(SwingTools.RAPIDMINER_ORANGE));
		// show unknown attributes that are placed in brackets in [] in black
		ss.setStyle(Token.COMMENT_KEYWORD, new Style(Color.black));
		// show operators that are not defined in the functions in black (like other unknown words)
		ss.setStyle(Token.OPERATOR, new Style(Color.black));
		return ss;
	}

	/**
	 * Removes all line signals that show error occurrences
	 */
	private void removeLineSignals() {
		scrollPaneExpression.getGutter().removeAllTrackingIcons();
	}

	/**
	 * Shows an error icon in the given line
	 *
	 * @param line
	 *            that should show an error icon
	 */
	private void signalLine(int line) {
		// use the gutter of the expression scroll pane to display icons
		try {
			scrollPaneExpression.getGutter().addLineTrackingIcon(line - 1, ERROR_ICON);
		} catch (BadLocationException e) {
			// in this case don't show an error icon
		}
	}

	/**
	 * Show a title and a message (error or information) about the status of the expression
	 *
	 * @param error
	 *            if the message is an error message
	 * @param title
	 *            title of the message
	 * @param message
	 *            message, which shows in case of error the place of the error
	 */
	private void showError(boolean error, String title, String message) {

		// set colors accordingly
		if (error) {
			validationLabel.setForeground(Color.RED);
			validationTextArea.setForeground(Color.RED);
		} else {
			validationLabel.setForeground(DARK_GREEN);
			validationTextArea.setForeground(DARK_GREEN);
		}

		// add the explanation line to the label to use a different font and the same indentation as
		// the title
		String[] splittedMessage = message.split("\n");
		String explanation = splittedMessage.length > 0 ? splittedMessage[0] : "";
		explanation = (explanation.charAt(0) + "").toUpperCase() + explanation.substring(1);
		validationLabel.setText("<html>" + title + explanation + "</html>");
		// show the error message with the place of the error in monospaced
		// DO NOT CHANGE THIS, AS THE INDENTATION IS WRONG OTHERWISE
		validationTextArea.setFont(FontTools.getFont(Font.MONOSPACED, Font.PLAIN, 12));

		if (splittedMessage.length > 1) {
			// truncate error message to maxMessageLength where necessary to avoid overflow in the validationTextArea
			truncateMessage(splittedMessage, MAX_ERROR_MESSAGE_LENGTH);
		}
		// set the error message
		String errorMessage = splittedMessage.length > 1 ? splittedMessage[1] : "\n";
		if (splittedMessage.length > 2) {
			errorMessage += "\n" + splittedMessage[2];
		}
		validationTextArea.setText(errorMessage);
	}

	/**
	 * Helper method that truncates the given error message if it is longer than maxChar characters. It does not return
	 * a new string array but modifies the given one instead. In this case the
	 * error message will contain as much of the error as possible plus a symmetrical window around it. Parts of the
	 * original message that have been truncated are marked by the TRUNCATED_SYMBOL constant string.
	 *
	 * @param splittedMessage
	 * 		the original error message
	 * @param maxChars
	 * 		the maximum number of characters the error message is allowed to have
	 */
	static void truncateMessage(String[] splittedMessage, int maxChars) {
		if (splittedMessage.length > 2) {
			// that means the error has been marked. we dont want to cut the error away
			if (maxChars < splittedMessage[1].length()) {
				// every character of the error is marked by a '^' in splittedMessage[2]
				int errorStart = splittedMessage[2].indexOf('^');
				int errorEnd = splittedMessage[2].lastIndexOf('^');
				int errorLength = errorEnd - errorStart + 1;
				int originalMsgLength = splittedMessage[1].length();
				if (errorLength > maxChars) {
					// the end of the error has to be cut because there is no space for the whole error
					splittedMessage[1] = splittedMessage[1].substring(errorStart, errorStart + maxChars);
					splittedMessage[2] = splittedMessage[2].substring(errorStart, errorStart + maxChars);
					addTruncatedSymbols(splittedMessage, errorStart, errorEnd, originalMsgLength);
				} else {
					// we can preserve the whole error. we will print the error and a symmetrical window around it
					int rest = maxChars - errorLength;
					int windowStart = errorStart - (rest / 2);
					int windowEnd = errorEnd + (rest / 2);
					// now we need to make sure that the window is inside the strings bounds
					if (windowStart < 0) {
						// pushes the window into the strings bounds from the left side
						windowEnd -= windowStart;
						windowStart = 0;
					}
					if (windowEnd >= originalMsgLength) {
						// pushes the window into the strings bounds from the right side
						windowStart -= windowEnd - originalMsgLength + 1;
						windowEnd = originalMsgLength - 1;
					}
					// applies the windows to the error message
					splittedMessage[1] = splittedMessage[1].substring(windowStart, windowEnd + 1);
					splittedMessage[2] = splittedMessage[2].substring(windowStart, Math.min(windowEnd + 1,
							splittedMessage[2].length()));
					// adds the TRUNCATED_SYMBOL where necessary
					addTruncatedSymbols(splittedMessage, windowStart, windowEnd, originalMsgLength);
				}
			}

		} else {
			// that means the error has not been marked. therefore we simply cut the end if necessary
			if (maxChars < splittedMessage[1].length()) {
				splittedMessage[1] = splittedMessage[1].substring(0, maxChars) + TRUNCATED_SYMBOL;
			}
		}
	}

	/**
	 * Helper method that adds the TRUNCATED_SYMBOL constant string to the error message where needed
	 */
	private static void addTruncatedSymbols(String[] splittedMessage, int windowStart, int windowEnd, int originalMsgLength) {
		if (windowStart > 0) {
			splittedMessage[1] = TRUNCATED_SYMBOL + splittedMessage[1];
			splittedMessage[2] = "     " + splittedMessage[2];
		}
		if (windowEnd < (originalMsgLength - 1)) {
			splittedMessage[1] = splittedMessage[1] + TRUNCATED_SYMBOL;
		}
	}
}
