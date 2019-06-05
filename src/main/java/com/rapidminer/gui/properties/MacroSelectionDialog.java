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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * Dialog which shows a choice between two buttons how a {@link FunctionInputType#MACRO} should be
 * inserted into the {@link ExpressionPropertyDialog}, as a value or as an interpreted expression.
 *
 * @author Sabrina Kirstein
 *
 */
public class MacroSelectionDialog extends JDialog {

	private static final long serialVersionUID = 6827927054583795240L;

	/**
	 * macro expression that should be inserted into the {@link ExpressionPropertyDialog}'s
	 * expression
	 */
	private String expression = "";

	/** default background color */
	private Color defaultBackground;

	/** panel showing the 'Insert as value' part */
	private JPanel valuePanel = new JPanel();

	/** indicator if the value part is highlighted */
	private boolean valueHighlighted = false;

	/** panel showing the 'Insert as evaluated expression' part */
	private JPanel expressionPanel = new JPanel();

	/** indicator if the expression part is highlighted */
	private boolean expressionHighlighted = false;

	/** panel showing the 'Insert as evaluated attribute' part */
	private JPanel attributePanel = new JPanel();

	/** indicator if the attribute part is highlighted */
	private boolean attributeHighlighted = false;

	/** indicates if the old macro handling is used */
	private boolean deprecated = false;

	/** title of the entire dialog */
	private static final String DIALOG_TITLE = I18N.getGUILabel("macro_selection_dialog.title");

	/** title of the value part */
	private static final String VALUE_TITLE = I18N.getGUILabel("macro_selection_dialog.value.title");

	/** description of the value part */
	private static final String VALUE_DESCRIPTION = I18N.getGUILabel("macro_selection_dialog.value.description");

	/** expression, when the user selects the expression part */
	private static final String VALUE_CALL = "%{macro_name}";

	/** start of the expression, when the user selects the expression part */
	private static final String VALUE_CALL_START = "%{";

	/** end of the expression, when the user selects the expression part */
	private static final String VALUE_CALL_END = "}";

	/** expression, when the user selects the expression part */
	private static final String VALUE_CALL_DEPRECATED = "macro(\"macro_name\")";

	/** start of the expression, when the user selects the expression part */
	private static final String VALUE_CALL_START_DEPRECATED = "macro(\"";

	/** end of the expression, when the user selects the expression part */
	private static final String VALUE_CALL_END_DEPRECATED = "\")";

	/** title of the expression part */
	private static final String EXPRESSION_TITLE = I18N.getGUILabel("macro_selection_dialog.expression.title");

	/** description of the expression part 1 */
	private static final String EXPRESSION_DESCRIPTION = I18N.getGUILabel("macro_selection_dialog.expression.description");

	/** expression, when the user selects the expression part */
	private static final String EXPRESSION_CALL = "eval(%{macro_name})";

	/** start of the expression, when the user selects the expression part */
	private static final String EXPRESSION_CALL_START = "eval(%{";

	/** end of the expression, when the user selects the expression part */
	private static final String EXPRESSION_CALL_END = "})";

	/** expression, when the user selects the expression part */
	private static final String EXPRESSION_CALL_DEPRECATED = "%{macro_name}";

	/** start of the expression, when the user selects the expression part */
	private static final String EXPRESSION_CALL_START_DEPRECATED = "%{";

	/** end of the expression, when the user selects the expression part */
	private static final String EXPRESSION_CALL_END_DEPRECATED = "}";

	/** title of the attribute part */
	private static final String ATTRIBUTE_TITLE = I18N.getGUILabel("macro_selection_dialog.attribute.title");

	/** description of the attribute part 1 */
	private static final String ATTRIBUTE_DESCRIPTION = I18N.getGUILabel("macro_selection_dialog.attribute" +
			".description");

	/** expression, when the user selects the attribute part */
	private static final String ATTRIBUTE_CALL = "#{macro_name}";

	/** start of the expression, when the user selects the attribute part */
	private static final String ATTRIBUTE_CALL_START = "#{";

	/** end of the expression, when the user selects the attribute part */
	private static final String ATTRIBUTE_CALL_END = "}";

	/**
	 * Creates a dialog for a given {@link FunctionInputType#MACRO} to choose between inserting the
	 * macro as a pure value or as an interpreted expression.
	 *
	 * @param macroPanel
	 * @param deprecated
	 *            if deprecated, use old macro handling
	 */
	public MacroSelectionDialog(FunctionInputPanel macroPanel, boolean deprecated) {
		this.deprecated = deprecated;
		initGui(macroPanel);
		expressionPanel.requestFocusInWindow();
	}

	/**
	 * Creates a dialog for a given {@link FunctionInputType#MACRO} to choose between inserting the
	 * macro as a pure value or as an interpreted expression.
	 *
	 * @param macroPanel
	 */
	public MacroSelectionDialog(FunctionInputPanel macroPanel) {
		this(macroPanel, false);
	}

	/**
	 * @return the selected expression which will be added to the {@link ExpressionPropertyDialog}
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * Initializes the User Interface
	 *
	 * @param macroPanel
	 *            {@link FunctionInputPanel} showing the macro that the user clicked on
	 */
	private void initGui(final FunctionInputPanel macroPanel) {

		defaultBackground = getBackground();
		setResizable(false);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle(DIALOG_TITLE);
		setIconImage(SwingTools.createIcon("16/rapidminer_studio.png").getImage());
		setSize(new Dimension(300, 375));

		JPanel main = new JPanel();
		GridBagLayout mainLayout = new GridBagLayout();
		main.setLayout(mainLayout);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.NORTHWEST;

		// VALUE PART
		valuePanel.setLayout(new GridBagLayout());
		valuePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		valuePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY));
		gbc.insets = new Insets(5, 3, 2, 0);
		// add text
		if (deprecated) {
			valuePanel.add(new JLabel("<html><p align=\"left\"><b><font size=4>" + VALUE_TITLE
					+ "</font></b><br><font size=3 color=\"gray\">" + VALUE_CALL_DEPRECATED + "</font><br><br>"
					+ VALUE_DESCRIPTION + "</p></html>"), gbc);
		} else {
			valuePanel.add(new JLabel("<html><p align=\"left\"><b><font size=4>" + VALUE_TITLE
					+ "</font></b><br><font size=3 color=\"gray\">" + VALUE_CALL + "</font><br><br>" + VALUE_DESCRIPTION
					+ "</p></html>"), gbc);
		}

		// add highlighting behavior and store the expression if the user selects a type of
		// expression
		valuePanel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {

				if (deprecated) {
					expression = VALUE_CALL_START_DEPRECATED + macroPanel.getInputName().replace("\\", "\\\\")
							+ VALUE_CALL_END_DEPRECATED;
				} else {
					expression = VALUE_CALL_START + escape(macroPanel.getInputName()) + VALUE_CALL_END;
				}
				MacroSelectionDialog.this.setVisible(false);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				highlightValue(true);
				highlightExpression(false);
				highlightAttribute(false);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				highlightValue(false);
			}
		});
		gbc.insets = new Insets(0, 0, 0, 0);
		main.add(valuePanel, gbc);

		// EVALUATED EXPRESSION PART
		expressionPanel.setLayout(new GridBagLayout());
		expressionPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		expressionPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY));
		// add text
		gbc.insets = new Insets(5, 3, 2, 0);
		if (deprecated) {
			expressionPanel.add(new JLabel("<html><p align=\"left\"><b><font size=4>" + EXPRESSION_TITLE
					+ "</font></b><br><font size=3 color=\"gray\">" + EXPRESSION_CALL_DEPRECATED + "</font><br><br>"
					+ EXPRESSION_DESCRIPTION + "</p></html>"), gbc);
		} else {
			expressionPanel.add(new JLabel("<html><p align=\"left\"><b><font size=4>" + EXPRESSION_TITLE
					+ "</font></b><br><font size=3 color=\"gray\">" + EXPRESSION_CALL + "</font><br><br>"
					+ EXPRESSION_DESCRIPTION + "</p></html>"), gbc);
		}

		// add highlighting behavior and store the expression if the user selects a type of
		// expression
		expressionPanel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (deprecated) {
					expression = EXPRESSION_CALL_START_DEPRECATED + macroPanel.getInputName()
							+ EXPRESSION_CALL_END_DEPRECATED;
				} else {
					expression = EXPRESSION_CALL_START + escape(macroPanel.getInputName()) + EXPRESSION_CALL_END;
				}
				MacroSelectionDialog.this.setVisible(false);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				highlightExpression(true);
				highlightValue(false);
				highlightAttribute(false);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				highlightExpression(false);
			}
		});
		gbc.gridy += 1;
		gbc.insets = new Insets(10, 0, 0, 0);
		main.add(expressionPanel, gbc);

		// EVALUATED ATTRIBUTE PART

		attributePanel.setLayout(new GridBagLayout());
		attributePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		attributePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY));
		// add text
		gbc.insets = new Insets(5, 3, 2, 0);
		attributePanel.add(new JLabel("<html><p align=\"left\"><b><font size=4>" + ATTRIBUTE_TITLE
				+ "</font></b><br><font size=3 color=\"gray\">" + ATTRIBUTE_CALL + "</font><br><br>"
				+ ATTRIBUTE_DESCRIPTION + "</p></html>"), gbc);


		// add highlighting behavior and store the expression if the user selects a type of
		// expression
		attributePanel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				expression = ATTRIBUTE_CALL_START + escape(macroPanel.getInputName()) + ATTRIBUTE_CALL_END;
				MacroSelectionDialog.this.setVisible(false);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				highlightAttribute(true);
				highlightExpression(false);
				highlightValue(false);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				highlightAttribute(false);
			}
		});
		gbc.gridy += 1;
		gbc.insets = new Insets(10, 0, 0, 0);
		main.add(attributePanel, gbc);


		// add highlighting behavior for keys and store the expression if the user selects a type of
		// expression
		addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_UP) {
					if (expressionHighlighted) {
						highlightExpression(false);
						highlightValue(true);
					} else if (attributeHighlighted) {
						highlightExpression(true);
						highlightAttribute(false);
					} else if (!valueHighlighted) {
						highlightAttribute(true);
						highlightValue(false);
					}
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					if (valueHighlighted) {
						highlightExpression(true);
						highlightValue(false);
					} else if (expressionHighlighted) {
						highlightExpression(false);
						highlightAttribute(true);
					} else if (!attributeHighlighted) {
						highlightAttribute(false);
						highlightValue(true);
					}
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (valueHighlighted) {
						if (deprecated) {
							expression = VALUE_CALL_START_DEPRECATED + macroPanel.getInputName().replace("\\", "\\\\")
									+ VALUE_CALL_END_DEPRECATED;
						} else {
							expression = VALUE_CALL_START + escape(macroPanel.getInputName()) + VALUE_CALL_END;
						}
						MacroSelectionDialog.this.setVisible(false);
					} else if (expressionHighlighted) {
						if (deprecated) {
							expression = EXPRESSION_CALL_START_DEPRECATED + macroPanel.getInputName()
									+ EXPRESSION_CALL_END_DEPRECATED;
						} else {
							expression = EXPRESSION_CALL_START + escape(macroPanel.getInputName()) + EXPRESSION_CALL_END;
						}
						MacroSelectionDialog.this.setVisible(false);
					} else if (attributeHighlighted) {
						expression = ATTRIBUTE_CALL_START + escape(macroPanel.getInputName()) + ATTRIBUTE_CALL_END;
						MacroSelectionDialog.this.setVisible(false);
					}
				} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					setVisible(false);
				}
			}
		});

		setLayout(new GridBagLayout());

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(10, 10, 10, 10);
		add(main, gbc);
	}

	/**
	 * Escapes curly brackets and backslashes inside inputName.
	 */
	private String escape(String inputName) {
		return inputName.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}");
	}

	/**
	 * Highlights the part to select, if the user wants to insert the value
	 *
	 * @param highlight
	 *            whether it should be highlighted
	 */
	private void highlightValue(boolean highlight) {

		valueHighlighted = highlight;
		if (highlight) {
			valuePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, SwingTools.RAPIDMINER_ORANGE));
			valuePanel.setBackground(Color.LIGHT_GRAY);
		} else {

			valuePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY));
			valuePanel.setBackground(defaultBackground);
		}
	}

	/**
	 * Highlights the part to select, if the user wants to insert the evaluated expression of the
	 * macro
	 *
	 * @param highlight
	 *            whether it should be highlighted
	 */
	private void highlightExpression(boolean highlight) {

		expressionHighlighted = highlight;

		if (highlight) {
			expressionPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, SwingTools.RAPIDMINER_ORANGE));
			expressionPanel.setBackground(Color.LIGHT_GRAY);
		} else {

			expressionPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY));
			expressionPanel.setBackground(defaultBackground);
		}
	}

	/**
	 * Highlights the part to select, if the user wants to insert the attribute associated to the macro content
	 *
	 * @param highlight
	 * 		whether it should be highlighted
	 */
	private void highlightAttribute(boolean highlight) {

		attributeHighlighted = highlight;

		if (highlight) {
			attributePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, SwingTools.RAPIDMINER_ORANGE));
			attributePanel.setBackground(Color.LIGHT_GRAY);
		} else {

			attributePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY));
			attributePanel.setBackground(defaultBackground);
		}
	}
}
