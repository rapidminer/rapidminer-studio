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
package com.rapidminer.studio.io.data.internal.file.csv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.OverlayLayout;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;

import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.CharTextField;
import com.rapidminer.gui.tools.ColoredTableCellRenderer;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.RowNumberTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.gui.tools.bubble.BubbleWindow;
import com.rapidminer.gui.tools.bubble.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tools.bubble.BubbleWindow.BubbleStyle;
import com.rapidminer.gui.tools.bubble.ComponentBubbleWindow;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.operator.nio.ErrorTableModel;
import com.rapidminer.operator.nio.ImportWizardUtils;
import com.rapidminer.operator.nio.LoadingContentPane;
import com.rapidminer.operator.nio.model.CSVResultSet;
import com.rapidminer.operator.nio.model.CSVResultSet.ColumnSplitter;
import com.rapidminer.operator.nio.model.CSVResultSetConfiguration;
import com.rapidminer.studio.io.gui.internal.DataImportWizardUtils;
import com.rapidminer.studio.io.gui.internal.DataWizardEventType;
import com.rapidminer.studio.io.gui.internal.steps.configuration.CollapsibleErrorTable;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.io.Encoding;


/**
 * Panel used to specify the {@link CSVResultSetConfiguration} in the
 * {@link CSVFormatSpecificationWizardStep}.
 *
 * @author Sebastian Loh, Simon Fischer, Gisa Schaefer
 * @since 7.0.0
 */
public class CSVFormatSpecificationPanel extends JPanel {

	private static final long serialVersionUID = -6249118015226310854L;

	/** the icon for the error message */
	private static final ImageIcon ERROR_MESSAGE_ICON = SwingTools
			.createIcon(I18N.getGUILabel("csv_format_specification.empty_table.icon"));

	/** the text for the error message when the table is empty */
	private static final String ERROR_MESSAGE_TEXT = I18N.getGUILabel("csv_format_specification.empty_table.label");

	/** normal font for the error message */
	private static final Font ERROR_MESSAGE_FONT = new JLabel().getFont();

	/** bigger font for the preview lettering */
	private static final Font PREVIW_LETTERING_FONT = ERROR_MESSAGE_FONT.deriveFont(Font.BOLD, 180);

	/** the preview lettering text */
	private static final String PREVIW_LETTERING = I18N.getGUILabel("csv_format_specification.preview_background");

	/** default decimal character for numbers */
	private static final char DEFAULT_DECIMAL_CHARACTER = '.';

	/**
	 * Enum for the entries of the separationComboBox which can be used to set the separator of the
	 * csv file.
	 */
	private enum ColumnSeparator {
		COMMA("csv_format_specification.character_comma", LineParser.SPLIT_BY_COMMA_EXPRESSION),
		SEMICOLON("csv_format_specification.character_semicolon", LineParser.SPLIT_BY_SEMICOLON_EXPRESSION),
		SPACE("csv_format_specification.character_space", LineParser.SPLIT_BY_SPACE_EXPRESSION),
		TAB("csv_format_specification.character_tab", LineParser.SPLIT_BY_TAB_EXPRESSION),
		REGULAR_EXPRESSION("csv_format_specification.regular_expression", LineParser.DEFAULT_SPLIT_EXPRESSION);

		private String text;
		private String separator;

		ColumnSeparator(String i18nForText, String separator) {
			text = I18N.getGUILabel(i18nForText);
			this.separator = separator;
		}

		public String getSeparator() {
			return separator;
		}

		@Override
		public String toString() {
			return text;
		}

	}

	private List<ChangeListener> changeListeners = new LinkedList<>();

	private final JCheckBox trimLinesBox = new JCheckBox(I18N.getGUILabel("csv_format_specification.trim_lines"), true);
	private final JComboBox<String> encodingComboBox = new JComboBox<>(Encoding.CHARSETS);
	private final JCheckBox skipCommentsBox = new JCheckBox(I18N.getGUILabel("csv_format_specification.skip_comments"),
			true);
	private final JCheckBox headerRow = new JCheckBox(I18N.getGUILabel("csv_format_specification.header_row"), true);
	private final JComboBox<ColumnSeparator> separationComboBox = new JComboBox<>(ColumnSeparator.values());
	private final JCheckBox useQuotesBox = new JCheckBox(I18N.getGUILabel("csv_format_specification.use_quotes"), true);
	private final JTextField commentCharacterTextField = new JTextField(LineParser.DEFAULT_COMMENT_CHARACTER_STRING);
	private final CharTextField quoteCharacterTextField = new CharTextField(LineParser.DEFAULT_QUOTE_CHARACTER);
	private final CharTextField escapeCharacterTextField = new CharTextField(LineParser.DEFAULT_QUOTE_ESCAPE_CHARACTER);
	private final JTextField regexTextField = new JTextField(LineParser.DEFAULT_SPLIT_EXPRESSION);
	private final JButton regexEvalButton = new JButton();
	private final JSpinner startRowSpinner = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
	private final JSpinner headerRowSpinner = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
	private final CharTextField decimalCharacterTextField = new CharTextField();
	private LoadingContentPane loadingContentPane;
	private JLabel overlayLabel;

	private final CSVResultSetConfiguration configuration;

	private ExtendedJTable previewTable;
	private JScrollPane tablePane;

	private final ErrorTableModel errorTableModel = new ErrorTableModel();
	private final CollapsibleErrorTable collapsibleErrorTable = new CollapsibleErrorTable(errorTableModel);

	private BubbleWindow currentErrorWindow;
	private boolean keepBubble = false;

	public CSVFormatSpecificationPanel(CSVResultSetConfiguration csvConfiguration) {

		this.configuration = csvConfiguration;

		// configuration -> UI components
		skipCommentsBox.setSelected(configuration.isSkipComments());
		useQuotesBox.setSelected(configuration.isUseQuotes());
		headerRow.setSelected(configuration.hasHeaderRow());
		trimLinesBox.setSelected(configuration.isTrimLines());
		commentCharacterTextField.setText(configuration.getCommentCharacters());
		escapeCharacterTextField.setText(String.valueOf(configuration.getEscapeCharacter()));
		quoteCharacterTextField.setText(String.valueOf(configuration.getQuoteCharacter()));
		encodingComboBox.setSelectedItem(configuration.getEncoding().name());
		// do not fire action event when using keyboard to move up and down
		encodingComboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
		startRowSpinner.setValue(configuration.getStartingRow() + 1);
		headerRowSpinner.setValue(configuration.getHeaderRow() + 1);
		decimalCharacterTextField.setText(String.valueOf(configuration.getDecimalCharacter()));

		String sep = configuration.getColumnSeparators();
		switch (sep) {
			case LineParser.SPLIT_BY_COMMA_EXPRESSION:
				separationComboBox.setSelectedItem(ColumnSeparator.COMMA);
				break;
			case LineParser.SPLIT_BY_SEMICOLON_EXPRESSION:
				separationComboBox.setSelectedItem(ColumnSeparator.SEMICOLON);
				break;
			case LineParser.SPLIT_BY_TAB_EXPRESSION:
				separationComboBox.setSelectedItem(ColumnSeparator.TAB);
				break;
			case LineParser.SPLIT_BY_SPACE_EXPRESSION:
				separationComboBox.setSelectedItem(ColumnSeparator.SPACE);
				break;
			default:
				separationComboBox.setSelectedItem(ColumnSeparator.REGULAR_EXPRESSION);
		}
		// do not fire action event when using keyboard to move up and down
		separationComboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);

		registerListeners();
		makePanel();
	}

	private static UpdateQueue updateQueue;

	void startDataFetching() {
		updateQueue = new UpdateQueue("CSV-Preview-Fetcher");
		updateQueue.start();
		settingsChanged();
	}

	void stopDataFetching() {
		if (updateQueue != null) {
			updateQueue.shutdown();
			updateQueue = null;
		}
	}

	private void registerListeners() {
		headerRow.addActionListener(e -> {
			DataImportWizardUtils.logStats(DataWizardEventType.CSV_HEADER_ROW_STATE,
					Boolean.toString(headerRow.isSelected()));
			configuration.setHasHeaderRow(headerRow.isSelected());
			headerRowSpinner.setEnabled(headerRow.isSelected());
			settingsChanged();
		});

		encodingComboBox.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED && encodingComboBox.getSelectedItem() != null) {
				configuration.setEncoding(Encoding.getEncoding(encodingComboBox.getSelectedItem().toString()));
				settingsChanged();
			}
		});

		separationComboBox.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				DataImportWizardUtils.logStats(DataWizardEventType.CSV_SEPARATOR_CHANGED,
						configuration.getColumnSeparators() + "->" + getSplitExpression());
				enableFields();
				configuration.setColumnSeparators(getSplitExpression());
				settingsChanged();
			}

		});

		trimLinesBox.addActionListener(e -> {
			configuration.setTrimLines(trimLinesBox.isSelected());
			settingsChanged();
		});
		skipCommentsBox.addActionListener(e -> {
			commentCharacterTextField.setEnabled(skipCommentsBox.isSelected());
			configuration.setSkipComments(skipCommentsBox.isSelected());
			settingsChanged();
		});
		useQuotesBox.addActionListener(e -> {
			quoteCharacterTextField.setEnabled(useQuotesBox.isSelected());
			escapeCharacterTextField.setEnabled(useQuotesBox.isSelected());
			configuration.setUseQuotes(useQuotesBox.isSelected());
			settingsChanged();
		});
		quoteCharacterTextField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}

			private void update() {
				if (!quoteCharacterTextField.getText().isEmpty()) {
					configuration.setQuoteCharacter(quoteCharacterTextField.getText().charAt(0));
					settingsChanged();
				}
			}
		});
		quoteCharacterTextField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				if (quoteCharacterTextField.getText().isEmpty()) {
					quoteCharacterTextField.setCharacter(LineParser.DEFAULT_QUOTE_CHARACTER);
				}
			}
		});
		escapeCharacterTextField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}

			private void update() {
				if (!escapeCharacterTextField.getText().isEmpty()) {
					configuration.setEscapeCharacter(escapeCharacterTextField.getText().charAt(0));
					settingsChanged();
				}
			}
		});
		escapeCharacterTextField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				if (escapeCharacterTextField.getText().isEmpty()) {
					escapeCharacterTextField.setCharacter(LineParser.DEFAULT_QUOTE_ESCAPE_CHARACTER);
				}
			}
		});
		commentCharacterTextField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}

			private void update() {
				configuration.setCommentCharacters(commentCharacterTextField.getText());
				settingsChanged();
			}
		});
		commentCharacterTextField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				if (commentCharacterTextField.getText().isEmpty()) {
					commentCharacterTextField.setText(LineParser.DEFAULT_COMMENT_CHARACTER_STRING);
				}
			}
		});
		decimalCharacterTextField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (!decimalCharacterTextField.getText().isEmpty()) {
					configuration.setDecimalCharacter(decimalCharacterTextField.getText().charAt(0));
				}
			}
		});
		decimalCharacterTextField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}

			private void update() {
				if (!decimalCharacterTextField.getText().isEmpty()) {
					configuration.setDecimalCharacter(decimalCharacterTextField.getText().charAt(0));
				}
			}
		});
		decimalCharacterTextField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				if (decimalCharacterTextField.getText().isEmpty()) {
					decimalCharacterTextField.setCharacter(DEFAULT_DECIMAL_CHARACTER);
				}
			}

		});
		startRowSpinner.addChangeListener(e -> {
			updateStartingRow();
		});
		headerRowSpinner.addChangeListener(e -> {
			updateHeaderRow();
		});
	}

	/**
	 * Creates a panel containing a settings panel at the top, a preview table in the middle and an
	 * error panel at the bottom.
	 */
	private void makePanel() {
		this.setLayout(new BorderLayout(0, ButtonDialog.GAP));
		this.add(makeSettingsPanel(), BorderLayout.NORTH);

		this.add(makePreviewTable(), BorderLayout.CENTER);

		this.add(collapsibleErrorTable, BorderLayout.SOUTH);
	}

	/**
	 * @return the panel containing all the file encoding options
	 */
	private JPanel makeSettingsPanel() {
		// the left settings panel
		JPanel leftPanel = new JPanel(ButtonDialog.createGridLayout(4, 2));
		leftPanel.add(headerRow);
		leftPanel.add(headerRowSpinner);

		leftPanel.add(new JLabel(I18N.getGUILabel("csv_format_specification.start_row")));
		leftPanel.add(startRowSpinner);

		leftPanel.add(new JLabel(I18N.getGUILabel("csv_format_specification.column_separation")));
		leftPanel.add(separationComboBox);

		leftPanel.add(new JLabel());
		leftPanel.add(makeRegexPanel());
		leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

		// the middle settings panel
		JPanel middlePanel = new JPanel(ButtonDialog.createGridLayout(4, 2));
		middlePanel.add(new JLabel(I18N.getGUILabel("csv_format_specification.file_encoding")));
		middlePanel.add(encodingComboBox);

		middlePanel.add(new JLabel(I18N.getGUILabel("csv_format_specification.escape_character")));
		middlePanel.add(escapeCharacterTextField);

		middlePanel.add(new JLabel(I18N.getGUILabel("csv_format_specification.decimal_character")));
		middlePanel.add(decimalCharacterTextField);

		middlePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 1, 0, 1, SwingTools.brightenColor(Color.GRAY)),
				BorderFactory.createEmptyBorder(0, 15, 0, 15)));

		// the right settings panel
		JPanel rightPanel = new JPanel(ButtonDialog.createGridLayout(4, 2));
		rightPanel.add(useQuotesBox);
		rightPanel.add(quoteCharacterTextField);

		rightPanel.add(trimLinesBox);
		rightPanel.add(new JLabel());

		rightPanel.add(skipCommentsBox);
		rightPanel.add(commentCharacterTextField);
		rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0));

		// add the three settings panel together
		JPanel settingsPanel = new JPanel(ButtonDialog.createGridLayout(1, 3));
		settingsPanel.add(leftPanel);
		settingsPanel.add(middlePanel);
		settingsPanel.add(rightPanel);
		return settingsPanel;
	}

	/**
	 * Fills the tablePane with content.
	 */
	private JComponent makePreviewTable() {
		previewTable = new ExtendedJTable(false, false, false);
		// ensure same background as JPanels in case of only few rows
		previewTable.setBackground(Colors.PANEL_BACKGROUND);
		previewTable.setColoredTableCellRenderer(new ColoredTableCellRenderer() {

			private final Font boldFont = getFont().deriveFont(Font.BOLD);

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
														   int row, int column) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				adjustCell(row, label, boldFont);
				return label;
			}

		});

		loadingContentPane = new LoadingContentPane("loading_data", previewTable);

		tablePane = new ExtendedJScrollPane(loadingContentPane);
		tablePane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		tablePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		tablePane.setBorder(null);

		// add PREVIEW label in front of scrollpane
		JLayeredPane layeredPane = new JLayeredPane();
		layeredPane.setLayout(new OverlayLayout(layeredPane));
		layeredPane.add(tablePane, JLayeredPane.DEFAULT_LAYER);

		JPanel overlayPanel = new JPanel(new BorderLayout());
		overlayPanel.setOpaque(false);
		overlayLabel = new JLabel("", SwingConstants.CENTER);
		showPreviewLettering();
		overlayPanel.add(overlayLabel, BorderLayout.CENTER);

		layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
		return layeredPane;
	}

	/**
	 * Shows the preview lettering.
	 */
	private void showPreviewLettering() {
		overlayLabel.setText(PREVIW_LETTERING);
		overlayLabel.setFont(PREVIW_LETTERING_FONT);
		overlayLabel.setForeground(DataImportWizardUtils.getPreviewFontColor());
		overlayLabel.setIcon(null);
	}

	/**
	 * Shows an error message about empty data.
	 */
	private void showEmptyMessage() {
		overlayLabel.setText(ERROR_MESSAGE_TEXT);
		overlayLabel.setFont(ERROR_MESSAGE_FONT);
		overlayLabel.setForeground(Color.BLACK);
		overlayLabel.setIcon(ERROR_MESSAGE_ICON);
	}

	/**
	 * @return a panel containing the regexTextField and the regexEvalButton
	 */
	private JPanel makeRegexPanel() {
		JPanel regexPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridx = GridBagConstraints.RELATIVE;
		regexPanel.add(regexTextField, gbc);
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.gridx = 1;
		Action evalRegexAction = new ResourceAction(true, "import_regex_refresh") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				configuration.setColumnSeparators(getSplitExpression());
				settingsChanged();
			}
		};
		regexEvalButton.setAction(evalRegexAction);
		regexEvalButton.setVisible(false);
		regexTextField.setVisible(false);
		regexTextField.addActionListener(e -> {
			// on Enter, apply changes
			configuration.setColumnSeparators(getSplitExpression());
			settingsChanged();
		});
		regexTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				if (!e.isTemporary()) {
					configuration.setColumnSeparators(getSplitExpression());
					settingsChanged();
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				// nothing to do
			}
		});
		regexPanel.add(regexEvalButton, gbc);
		return regexPanel;
	}

	/**
	 * @return the split expression determined by the separationComboBox
	 */
	private String getSplitExpression() {
		String splitExpression;
		if (separationComboBox.getSelectedItem() != ColumnSeparator.REGULAR_EXPRESSION) {
			splitExpression = ((ColumnSeparator) separationComboBox.getSelectedItem()).getSeparator();
		} else {
			splitExpression = regexTextField.getText();
			if (splitExpression.isEmpty()) {
				splitExpression = null;
			} else {
				try {
					Pattern.compile(splitExpression);
				} catch (PatternSyntaxException pse) {
					splitExpression = null;
				}
			}
		}
		return splitExpression;
	}

	/**
	 * Reloads the data with the new settings and adjusts the display accordingly.
	 */
	private void settingsChanged() {
		ProgressThread loadingDataThread = new ProgressThread("loading_data") {

			@Override
			public void run() {
				try {
					final TableModel model = configuration.makePreviewTableModel(getProgressListener());
					SwingTools.invokeAndWait(() -> {
						previewTable.setModel(model);
						tablePane.setRowHeaderView(new RowNumberTable(previewTable));
						if (model.getRowCount() > 0) {
							showPreviewLettering();
						} else {
							showEmptyMessage();
						}
						updateErrorTable();
						fireStateChanged();
					});
				} catch (Exception e) {
					updateErrorTable();
					ImportWizardUtils.showErrorMessage(configuration.getResourceName(), e.toString(), e);
				}
			}
		};

		loadingContentPane.init(loadingDataThread);
		updateQueue.executeBackgroundJob(loadingDataThread);
	}

	/**
	 * Updates the content of the errorTable and the label that is used to show it.
	 */
	private void updateErrorTable() {
		errorTableModel.setErrors(configuration.getErrors());

		// make the first row smaller
		collapsibleErrorTable.getTable().getColumnModel().getColumn(0).setMaxWidth(150);
		collapsibleErrorTable.getTable().getColumnModel().getColumn(0).setPreferredWidth(100);

		// make the last row wider
		collapsibleErrorTable.getTable().getColumnModel().getColumn(3).setMaxWidth(800);
		collapsibleErrorTable.getTable().getColumnModel().getColumn(3).setPreferredWidth(400);

		SwingTools.invokeLater(collapsibleErrorTable::update);
	}

	/**
	 * Enables the text field and button associated to the Column Separator combobox item
	 * "Regular Expression".
	 */
	private void enableFields() {
		regexTextField.setVisible(separationComboBox.getSelectedItem() == ColumnSeparator.REGULAR_EXPRESSION);
		regexEvalButton.setVisible(separationComboBox.getSelectedItem() == ColumnSeparator.REGULAR_EXPRESSION);
	}

	/**
	 * Adjust how the cell is displayed. If it is a header cell it should be opaque with a dark
	 * background, otherwise it should be transparent to show the lettering in the background. If
	 * the row is the header the font is bold, if the row is smaller than the start row it is gray,
	 * otherwise black.
	 */
	private void adjustCell(int row, JLabel cell, Font boldFont) {
		if (configuration.hasHeaderRow() && row == configuration.getHeaderRow()) {
			cell.setBackground(Color.LIGHT_GRAY);
			cell.setFont(boldFont);
			cell.setForeground(Color.BLACK);
		} else if (row < configuration.getStartingRow()) {
			cell.setForeground(Color.LIGHT_GRAY);
		} else {
			cell.setForeground(Color.BLACK);
		}
	}

	/**
	 * Sets the starting row as defined in the startingRowTextField and repaints the table.
	 */
	private void updateStartingRow() {
		int startingRow = (int) startRowSpinner.getValue();
		configuration.setStartingRow(startingRow - 1);
		settingsChanged();
	}

	/**
	 * Sets the header row as defined in the headerRowSpinner and repaints the table.
	 */
	private void updateHeaderRow() {
		int headerRowNumber = (int) headerRowSpinner.getValue();
		configuration.setHeaderRow(headerRowNumber - 1);
		if (headerRowNumber > configuration.getStartingRow()) {
			startRowSpinner.getModel().setValue(headerRowNumber);
			configuration.setStartingRow(headerRowNumber - 1);
		}
		settingsChanged();
	}

	/**
	 * Kills the current error bubble.
	 */
	void killCurrentErrorBubbleWindow() {
		if (currentErrorWindow != null) {
			currentErrorWindow.killBubble(true);
			currentErrorWindow = null;
		}
	}

	/**
	 * Creates an error bubble for the component and kills other error bubbles.
	 *
	 * @param component
	 * 		the component for which to show the bubble
	 * @param i18n
	 * 		the i18n key
	 * @param arguments
	 * 		arguments for the i18n
	 */
	private void createErrorBubbleWindow(JComponent component, String i18n, Object... arguments) {
		killCurrentErrorBubbleWindow();
		JButton okayButton = new JButton(I18N.getGUILabel("io.dataimport.step.excel.sheet_selection.got_it"));
		final ComponentBubbleWindow errorWindow = new ComponentBubbleWindow(component, BubbleStyle.ERROR,
				SwingUtilities.getWindowAncestor(this), AlignedSide.BOTTOM, i18n, null, null, false, true,
				new JButton[]{okayButton}, arguments);
		okayButton.addActionListener(e -> errorWindow.killBubble(false));

		// show and remember error window
		errorWindow.setVisible(true);
		currentErrorWindow = errorWindow;
	}

	/**
	 * Shows a "header row not found"-bubble
	 */
	void notifyHeaderRowNotFound() {
		keepBubble = true;
		createErrorBubbleWindow(headerRowSpinner, "io.dataimport.step.csv.format_specification.header_row_not_found");
	}

	/**
	 * Shows a "start row not found"-bubble
	 */
	void notifyStartRowNotFound() {
		keepBubble = true;
		createErrorBubbleWindow(startRowSpinner, "io.dataimport.step.csv.format_specification.start_row_not_found");
	}

	/**
	 * Shows a "header row behind start row"-bubble
	 */
	void notifyHeaderRowBehindStartRow() {
		keepBubble = false;
		createErrorBubbleWindow(headerRowSpinner, "io.dataimport.step.csv.format_specification.invalid_header_row",
				configuration.getHeaderRow() + 1, configuration.getStartingRow() + 1);
	}

	/**
	 * Sets the column separator associated to the splitter.
	 *
	 * @param splitter
	 * 		a {@link ColumnSplitter}
	 */
	void setColumnSeparator(ColumnSplitter splitter) {
		SwingTools.invokeLater(() -> {
			switch (splitter) {
				case COMMA:
					separationComboBox.setSelectedItem(ColumnSeparator.COMMA);
					break;
				case TAB:
					separationComboBox.setSelectedItem(ColumnSeparator.TAB);
					break;
				case PIPE:
				case TILDE:
					regexTextField.setText(splitter.getPattern().pattern());
					separationComboBox.setSelectedItem(ColumnSeparator.REGULAR_EXPRESSION);
					break;
				default:
				case SEMI_COLON:
					separationComboBox.setSelectedItem(ColumnSeparator.SEMICOLON);
					break;
			}
		});
	}


	/**
	 * Sets the text qualifier
	 *
	 * @param qualifier
	 * 		a {@link com.rapidminer.operator.nio.model.CSVResultSet.TextQualifier}
	 */
	void setTextQualifier(CSVResultSet.TextQualifier qualifier) {
		SwingTools.invokeLater(() -> {
			quoteCharacterTextField.setText(qualifier.getString());
			configuration.setQuoteCharacter(quoteCharacterTextField.getText().charAt(0));
		});
	}

	/**
	 * Sets the Decimal Separator
	 *
	 * @param character
	 * 		a {@link com.rapidminer.operator.nio.model.CSVResultSet.DecimalCharacter}
	 */
	void setDecimalCharacter(CSVResultSet.DecimalCharacter character) {
		SwingTools.invokeLater(() -> {
			decimalCharacterTextField.setText(character.getString());
			configuration.setDecimalCharacter(decimalCharacterTextField.getText().charAt(0));
		});
	}

	/**
	 * Checks that the table has content and the header row is not behind the start row.
	 *
	 * @throws InvalidConfigurationException
	 * 		if the conditions are not fulfilled
	 */
	void validateConfiguration() throws InvalidConfigurationException {
		if (previewTable.getModel().getRowCount() == 0) {
			throw new InvalidConfigurationException();
		}
		if (configuration.hasHeaderRow() && configuration.getHeaderRow() > configuration.getStartingRow()) {
			notifyHeaderRowBehindStartRow();
			throw new InvalidConfigurationException();
		} else if (keepBubble) {
			// ensure that row-not-found-bubbles are not killed until something changed
			throw new InvalidConfigurationException();
		} else {
			killCurrentErrorBubbleWindow();
		}
	}

	/**
	 * Registers a new change listener.
	 *
	 * @param changeListener
	 * 		the listener to register
	 */
	void addChangeListener(ChangeListener changeListener) {
		this.changeListeners.add(changeListener);
	}

	/**
	 * Fires a {@link ChangeEvent} that informs the listeners of a changed state.
	 */
	private void fireStateChanged() {
		keepBubble = false;
		ChangeEvent event = new ChangeEvent(this);
		for (ChangeListener listener : changeListeners) {
			try {
				listener.stateChanged(event);
			} catch (RuntimeException rte) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.io.dataimport.AbstractWizardStep.changelistener_failed", rte);
			}
		}
	}

}
