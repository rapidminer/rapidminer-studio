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
package com.rapidminer.operator.nio;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import com.rapidminer.gui.tools.CharTextField;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.operator.nio.model.CSVResultSetConfiguration;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.io.Encoding;


/**
 *
 * @author Sebastian Loh, Simon Fischer
 *
 */
public class CSVSyntaxConfigurationWizardStep extends WizardStep {

	private JPanel panel;

	private final JCheckBox trimLinesBox = new JCheckBox("Trim Lines", true);
	private final JComboBox<String> encodingComboBox = new JComboBox<>(Encoding.CHARSETS);
	private final JCheckBox skipCommentsBox = new JCheckBox("Skip Comments", true); // just temp
																					 // preselection,
																					 // real value
																					 // is
																					 // defined in
																					 // the
																					 // constructor
	private final JCheckBox useQuotesBox = new JCheckBox("Use Quotes", true); // just temp
																				 // preselection,
																				 // real value is
																				 // defined in the
																				 // constructor
	private final JTextField commentCharacterTextField = new JTextField(LineParser.DEFAULT_COMMENT_CHARACTER_STRING);
	private final CharTextField quoteCharacterTextField = new CharTextField(LineParser.DEFAULT_QUOTE_CHARACTER);
	private final JLabel escapeCharacterLabel = new JLabel("Escape Character:");
	private final CharTextField escapeCharacterTextField = new CharTextField(LineParser.DEFAULT_QUOTE_ESCAPE_CHARACTER);
	private final JRadioButton commaButton = new JRadioButton("Comma \",\" ");
	private final JRadioButton semicolonButton = new JRadioButton("Semicolon \";\"");
	private final JRadioButton tabButton = new JRadioButton("Tab");
	private final JRadioButton spaceButton = new JRadioButton("Space");
	private final JRadioButton regexButton = new JRadioButton("Regular Expression");
	private final JTextField regexTextField = new JTextField(LineParser.DEFAULT_SPLIT_EXPRESSION);
	private final JButton regexEvalButton = new JButton();
	private LoadingContentPane loadingContentPane;

	private CSVResultSetConfiguration configuration;

	private JTable previewTable;

	private JScrollPane tablePane;

	private ErrorTableModel errorTableModel = new ErrorTableModel();
	private JTable errorTable = new JTable(errorTableModel);

	public CSVSyntaxConfigurationWizardStep(CSVImportWizard csvImportWizard, CSVResultSetConfiguration csvConfiguration) {
		super("specify_csv_parsing_options");
		this.configuration = csvConfiguration;

		// configuration -> UI components
		skipCommentsBox.setSelected(configuration.isSkipComments());
		useQuotesBox.setSelected(configuration.isUseQuotes());
		trimLinesBox.setSelected(configuration.isTrimLines());
		commentCharacterTextField.setText(configuration.getCommentCharacters());
		escapeCharacterTextField.setText(String.valueOf(configuration.getEscapeCharacter()));
		quoteCharacterTextField.setText(String.valueOf(configuration.getQuoteCharacter()));
		encodingComboBox.setSelectedItem(configuration.getEncoding().name());

		String sep = configuration.getColumnSeparators();
		regexButton.setSelected(true);
		if (sep.equals(LineParser.SPLIT_BY_COMMA_EXPRESSION)) {
			commaButton.setSelected(true);
		}
		if (sep.equals(LineParser.SPLIT_BY_SEMICOLON_EXPRESSION)) {
			semicolonButton.setSelected(true);
		}
		if (sep.equals(LineParser.SPLIT_BY_TAB_EXPRESSION)) {
			tabButton.setSelected(true);
		}
		if (sep.equals(LineParser.SPLIT_BY_SPACE_EXPRESSION)) {
			spaceButton.setSelected(true);
		}

		registerListeners();
		makePanel();
	}

	private void registerListeners() {
		encodingComboBox.addActionListener(e -> {
			configuration.setEncoding(Encoding.getEncoding(encodingComboBox.getSelectedItem().toString()));
			settingsChanged();
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
		quoteCharacterTextField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setQuoteCharacter(quoteCharacterTextField.getText().charAt(0));
				settingsChanged();
			}
		});
		escapeCharacterTextField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setEscapeCharacter(escapeCharacterTextField.getText().charAt(0));
				settingsChanged();
			}
		});
		commentCharacterTextField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(KeyEvent e) {
				configuration.setCommentCharacters(commentCharacterTextField.getText());
				settingsChanged();
			}
		});

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(commaButton);
		buttonGroup.add(semicolonButton);
		buttonGroup.add(spaceButton);
		buttonGroup.add(tabButton);
		buttonGroup.add(regexButton);
		ActionListener separatorListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				enableFields();
				configuration.setColumnSeparators(getSplitExpression());
				settingsChanged();
			}
		};
		commaButton.addActionListener(separatorListener);
		semicolonButton.addActionListener(separatorListener);
		spaceButton.addActionListener(separatorListener);
		tabButton.addActionListener(separatorListener);
		regexButton.addActionListener(separatorListener);
	}

	private void makePanel() {
		JPanel optionPanel = new JPanel(ButtonDialog.createGridLayout(4, 1));

		JPanel tmpPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
		tmpPanel.add(new JLabel("File Encoding"));
		tmpPanel.add(encodingComboBox);
		optionPanel.add(tmpPanel);

		optionPanel.add(trimLinesBox);

		tmpPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
		tmpPanel.add(skipCommentsBox);
		tmpPanel.add(commentCharacterTextField);
		optionPanel.add(tmpPanel);

		optionPanel.setBorder(ButtonDialog.createTitledBorder("File Reading"));

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
		regexEvalButton.setEnabled(false);
		regexTextField.setEnabled(false);
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

		JPanel separationPanel = new JPanel(ButtonDialog.createGridLayout(5, 2));
		separationPanel.add(commaButton);
		separationPanel.add(spaceButton);
		separationPanel.add(semicolonButton);
		separationPanel.add(tabButton);
		separationPanel.add(regexButton);
		separationPanel.add(regexPanel);
		separationPanel.add(escapeCharacterLabel);
		separationPanel.add(escapeCharacterTextField);
		separationPanel.add(useQuotesBox);
		separationPanel.add(quoteCharacterTextField);

		separationPanel.setBorder(ButtonDialog.createTitledBorder("Column Separation"));

		JPanel parsingPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
		parsingPanel.add(optionPanel);
		parsingPanel.add(separationPanel);

		previewTable = new ExtendedJTable(false, false, false);
		loadingContentPane = new LoadingContentPane("loading_data", previewTable);
		// previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tablePane = new JScrollPane(loadingContentPane);

		tablePane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		tablePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		tablePane.setBorder(ButtonDialog.createBorder());

		panel = new JPanel(new BorderLayout(0, ButtonDialog.GAP));
		panel.add(parsingPanel, BorderLayout.NORTH);

		// panel.add(new JScrollPane(previewTable), BorderLayout.CENTER);
		panel.add(tablePane, BorderLayout.CENTER);

		JScrollPane errorScrollPane = new JScrollPane(errorTable);
		errorScrollPane.setPreferredSize(new Dimension(500, 80));
		panel.add(errorScrollPane, BorderLayout.SOUTH);
	}

	private String getSplitExpression() {
		String splitExpression = null;
		if (regexButton.isSelected()) {
			splitExpression = regexTextField.getText();
			if ("".equals(splitExpression)) {
				splitExpression = null;
			} else {
				try {
					Pattern.compile(splitExpression);
				} catch (PatternSyntaxException pse) {
					splitExpression = null;
				}
			}
		} else if (commaButton.isSelected()) {
			splitExpression = LineParser.SPLIT_BY_COMMA_EXPRESSION;
		} else if (semicolonButton.isSelected()) {
			splitExpression = LineParser.SPLIT_BY_SEMICOLON_EXPRESSION;
		} else if (tabButton.isSelected()) {
			splitExpression = LineParser.SPLIT_BY_TAB_EXPRESSION;
		} else if (spaceButton.isSelected()) {
			splitExpression = LineParser.SPLIT_BY_SPACE_EXPRESSION;
		}
		return splitExpression;
	}

	private static UpdateQueue updateQueue;

	private void settingsChanged() {
		ProgressThread loadingDataThread = new ProgressThread("loading_data") {

			@Override
			public void run() {
				try {
					final TableModel model = configuration.makePreviewTableModel(getProgressListener());
					SwingUtilities.invokeAndWait(new Runnable() {

						@Override
						public void run() {
							previewTable.setModel(model);
							errorTableModel.setErrors(configuration.getErrors());
						}
					});
				} catch (Exception e) {
					errorTableModel.setErrors(configuration.getErrors());
					ImportWizardUtils.showErrorMessage(configuration.getResourceName(), e.toString(), e);
				}
			}
		};

		loadingContentPane.init(loadingDataThread);
		updateQueue.executeBackgroundJob(loadingDataThread);
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		updateQueue = new UpdateQueue("CSV-Preview-Fetcher");
		updateQueue.start();
		settingsChanged();
		return true;
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		if (updateQueue != null) {
			updateQueue.shutdown();
			updateQueue = null;
		}
		return super.performLeavingAction(direction);
	}

	@Override
	protected JComponent getComponent() {
		return panel;
	}

	@Override
	protected boolean canProceed() {
		return true;
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}

	private void enableFields() {
		regexTextField.setEnabled(regexButton.isSelected());
		regexEvalButton.setEnabled(regexButton.isSelected());
	}
}
