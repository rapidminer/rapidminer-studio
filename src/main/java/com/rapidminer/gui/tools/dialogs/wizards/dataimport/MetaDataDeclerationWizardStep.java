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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractDataReader;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 * 
 * @author Tobias Malbrecht
 * @author Sebastian Loh (22.04.2010)
 * @deprecated use {@link com.rapidminer.studio.io.gui.internal.steps.configuration.ConfigureDataStep} instead
 */
@Deprecated
public abstract class MetaDataDeclerationWizardStep extends WizardStep {

	protected MetaDataDeclarationEditor editor = null;

	protected AbstractDataReader reader;

	protected final JCheckBox tolerateErrorCheckBox = new JCheckBox("Read non matching values as missings.", true);
	{
		tolerateErrorCheckBox
				.setToolTipText("Values which does not match to the specified value typed are considered as missings. A binomial attribute is changed to a nominal, if more than two different values are read.");
		tolerateErrorCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				reader.setErrorTolerant(tolerateErrorCheckBox.isSelected());
			}
		});
	}

	private JButton abortValueTypeValidationButton = new JButton(new ResourceAction("wizard.abort_validate_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			reader.stopReading();
		}
	});
	private JButton validateValueTypesButton = new JButton(new ResourceAction("wizard.validate_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			new ProgressThread("validate_value_types") {

				@Override
				public void run() {
					abortValueTypeValidationButton.setEnabled(true);
					validateValueTypesButton.setEnabled(false);
					reader.setDetectErrorsInPreview(true);

					final List<Object[]> previewAsList;
					try {
						reader.stopReading();
						synchronized (reader) {
							previewAsList = reader.getPreviewAsList(getProgressListener(), false);
						}
					} catch (OperatorException e1) {
						// TODO fix this
						SwingTools.showVerySimpleErrorMessage(e1.getMessage(), e1);
						return;
					}
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							setData(previewAsList);
							reader.setDetectErrorsInPreview(false);
							validateValueTypesButton.setEnabled(true);
							abortValueTypeValidationButton.setEnabled(false);
						};
					});
				}
			}.start();
		}
	});
	protected JPanel validationButtonsPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
	{
		validationButtonsPanel.add(validateValueTypesButton, 0);
		validationButtonsPanel.add(abortValueTypeValidationButton, 1);
		abortValueTypeValidationButton.setEnabled(false);
	}

	private JButton abortShowErrorRowsButton = new JButton(new ResourceAction("wizard.abort_show_error_rows") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			reader.stopReading();
		}
	});
	private JButton showErrorRowsButton = new JButton(new ResourceAction("wizard.show_error_rows") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			new ProgressThread("show_error_rows") {

				@Override
				public void run() {
					abortShowErrorRowsButton.setEnabled(true);
					showErrorRowsButton.setEnabled(false);
					final List<Object[]> previewAsList;
					try {
						reader.stopReading();
						synchronized (reader) {
							previewAsList = reader.getErrorPreviewAsList(getProgressListener());
						}
					} catch (OperatorException e1) {
						// TODO fix this
						SwingTools.showVerySimpleErrorMessage(e1.getMessage(), e1);
						return;
					}
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							setData(previewAsList);
							showErrorRowsButton.setEnabled(true);
							abortShowErrorRowsButton.setEnabled(false);
						};
					});
				}
			}.start();
		}
	});
	protected JPanel errorPreviewButtonsPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
	{
		errorPreviewButtonsPanel.add(showErrorRowsButton, 0);
		errorPreviewButtonsPanel.add(abortShowErrorRowsButton, 1);
		abortShowErrorRowsButton.setEnabled(false);
	}

	private JButton abortGuessingButton = new JButton(new ResourceAction("wizard.abort_guess_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			reader.stopReading();
		}
	});

	private JButton guessValueTypesButton = new JButton(new ResourceAction("wizard.guess_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			new ProgressThread("guessing_value_types") {

				@Override
				public void run() {
					final List<Object[]> previewAsList;
					try {
						abortGuessingButton.setEnabled(true);
						guessValueTypesButton.setEnabled(false);
						reader.stopReading();
						synchronized (reader) {
							reader.guessValueTypes(getProgressListener());
							previewAsList = reader.getPreviewAsList(getProgressListener(), true, false,
									AbstractDataReader.PREVIEW_LINES);
						}
					} catch (OperatorException e1) {
						// TODO fix this
						SwingTools.showVerySimpleErrorMessage(e1.getMessage(), e1);
						return;
					}
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							setData(previewAsList);
							guessValueTypesButton.setEnabled(true);
							abortGuessingButton.setEnabled(false);
						};
					});
				}
			}.start();

		}
	});

	protected JPanel guessingButtonsPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
	{
		guessingButtonsPanel.add(guessValueTypesButton, 0);
		guessingButtonsPanel.add(abortGuessingButton, 1);
		abortGuessingButton.setEnabled(false);
	}

	public MetaDataDeclerationWizardStep(String key, AbstractDataReader reader) {
		super(key);
		editor = new MetaDataDeclarationEditor(reader, true);
		this.reader = reader;
	}

	protected void setData(List<Object[]> data) {
		editor.setData(data);
	}

	@Override
	protected JComponent getComponent() {
		JPanel errorTolerancePanel = new JPanel(ButtonDialog.createGridLayout(2, 2));
		errorTolerancePanel.setBorder(ButtonDialog.createTitledBorder("Error Handling"));
		errorTolerancePanel.add(tolerateErrorCheckBox, 0);
		errorTolerancePanel.add(validationButtonsPanel, 1);

		errorTolerancePanel.add(new JPanel(), 2);
		errorTolerancePanel.add(errorPreviewButtonsPanel, 3);
		editor.setBorder(ButtonDialog.createTitledBorder("Data Preview"));

		JPanel panel = new JPanel(new BorderLayout(0, ButtonDialog.GAP));
		panel.add(errorTolerancePanel, BorderLayout.NORTH);
		panel.add(editor, BorderLayout.CENTER);
		return panel;
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		// dummy list
		setData(new LinkedList<Object[]>());
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				new ProgressThread("guessing_value_types") {

					@Override
					public void run() {
						final List<Object[]> previewAsList;
						try {
							reader.stopReading();
							synchronized (reader) {
								previewAsList = reader.getPreviewAsList(getProgressListener(), true);
							}
							SwingUtilities.invokeLater(new Runnable() {

								@Override
								public void run() {
									setData(previewAsList);
									doAfterEnteringAction();
								};
							});
						} catch (OperatorException e1) {
							// TODO fix this
							SwingTools.showVerySimpleErrorMessage(e1.getMessage(), e1);
							return;
						}
					}
				}.start();
			}
		});

		return true;
	}

	protected void doAfterEnteringAction() {
		// do nothing
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}

	@Override
	protected boolean canProceed() {
		return true;
	}

}
