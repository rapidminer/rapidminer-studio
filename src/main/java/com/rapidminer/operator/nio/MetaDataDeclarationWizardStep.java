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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.CellColorProviderAlternating;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.tools.table.EditableTableHeader;
import com.rapidminer.gui.tools.table.EditableTableHeaderColumn;
import com.rapidminer.gui.viewer.DataTableViewerTableModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.ColumnMetaData;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.ParsingError;
import com.rapidminer.operator.nio.model.WizardState;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.tools.I18N;


/**
 * This Wizard Step might be used to defined the meta data of each attribute.
 *
 * @author Sebastian Land, Simon Fischer
 */
public class MetaDataDeclarationWizardStep extends WizardStep {

	private static final Icon OK_ICON = SwingTools.createIcon("16/ok.png");
	private static final Icon ERROR_ICON = SwingTools.createIcon("16/error.png");

	/** The original column names extracted from the file */
	private String[] columnNames;
	private boolean isGuessing = false;
	private boolean isReloading = false;
	/** First boolean indicates loading, second canceling */
	private AtomicMarkableReference<Boolean> isLoadingOrCanceling = new AtomicMarkableReference<>(false, false);
	private ExtendedJTable previewTable;
	private MetaDataTableHeaderCellEditor headerRenderer;
	private MetaDataTableHeaderCellEditor headerEditor;

	private Action reloadAction = new ResourceAction("wizard.validate_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			if (isLoadingOrCanceling.compareAndSet(false, true, false, false)) {
				reload();
			}
		}
	};
	private Action cancelReloadAction = new ResourceAction("wizard.abort_validate_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			if (!isGuessing && isLoadingOrCanceling.compareAndSet(true, true, false, true)) {
				cancelReload();
			}
		}
	};
	private JButton reloadButton = new JButton(reloadAction);

	private Action guessValueTypes = new ResourceAction("wizard.guess_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			if (isLoadingOrCanceling.compareAndSet(false, true, false, false)) {
				guessValueTypes();
			}
		}
	};
	private Action cancelGuessValueTypes = new ResourceAction("wizard.abort_guess_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			if (!isReloading && isLoadingOrCanceling.compareAndSet(true, true, false, true)) {
				cancelGuessing();
			}
		}
	};
	private JButton guessButton = new JButton(guessValueTypes);

	private JCheckBox errorsAsMissingBox = new JCheckBox(new ResourceAction("wizard.error_tolerant") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			state.getTranslationConfiguration().setFaultTolerant(errorsAsMissingBox.isSelected());
		}
	});
	private JCheckBox filterErrorsBox = new JCheckBox(new ResourceAction("wizard.show_error_rows") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			if (filteredModel != null) {
				filteredModel.setFilterEnabled(filterErrorsBox.isSelected());
			}
		}
	});

	private JComboBox<String> dateFormatField = new JComboBox<>(ParameterTypeDateFormat.PREDEFINED_DATE_FORMATS);

	private JCheckBox limitedPreviewBox = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(),
			"gui.action.importwizard.limited_preview.label", ImportWizardUtils.getPreviewLength()));

	private WizardState state;

	private JPanel panel = new JPanel(new BorderLayout());
	private JScrollPane tableScrollPane;

	private ErrorTableModel errorTableModel = new ErrorTableModel();
	private RowFilteringTableModel filteredModel;
	private JLabel errorLabel = new JLabel();

	private boolean canProceed = true;
	private MetaDataValidator headerValidator;

	private final LoadingContentPane loadingContentPane;

	public MetaDataDeclarationWizardStep(WizardState state) {
		super("importwizard.metadata");
		limitedPreviewBox.setSelected(true);

		this.state = state;
		dateFormatField.setEditable(true);
		dateFormatField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				MetaDataDeclarationWizardStep.this.state.getTranslationConfiguration().setDatePattern(
						(String) dateFormatField.getSelectedItem());
			}
		});

		JPanel buttonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(0, 0, 5, 10);
		gbc.anchor = GridBagConstraints.WEST;
		buttonPanel.add(reloadButton, gbc);

		gbc.gridx += 1;
		buttonPanel.add(guessButton, gbc);

		JLabel label = new ResourceLabel("date_format");
		label.setLabelFor(dateFormatField);
		gbc.gridx += 1;
		buttonPanel.add(label, gbc);

		gbc.gridx += 1;
		buttonPanel.add(dateFormatField, gbc);

		gbc.gridx += 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		buttonPanel.add(new JLabel(), gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		buttonPanel.add(limitedPreviewBox, gbc);

		panel.add(buttonPanel, BorderLayout.NORTH);

		JPanel errorPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.ipadx = c.ipady = 4;
		c.weighty = 0;
		c.weightx = 1;

		c.gridwidth = 1;
		c.weightx = 1;
		errorPanel.add(errorLabel, c);

		c.weightx = 0;
		c.gridwidth = GridBagConstraints.RELATIVE;
		errorPanel.add(errorsAsMissingBox, c);
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		errorPanel.add(filterErrorsBox, c);

		final JTable errorTable = new JTable(errorTableModel);
		errorTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					final int selected = errorTable.getSelectedRow();
					if (selected >= 0) {
						ParsingError error = errorTableModel.getErrorInRow(selected);
						int row = error.getExampleIndex();
						row = filteredModel.inverseTranslateRow(row);
						if (row == -1) {
							return;
						}
						int col = error.getColumn();
						previewTable.setRowSelectionInterval(row, row);
						previewTable.setColumnSelectionInterval(col, col);
					}
				}
			}
		});
		final JScrollPane errorScrollPane = new ExtendedJScrollPane(errorTable);
		errorScrollPane.setPreferredSize(new Dimension(500, 80));
		c.weighty = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		errorPanel.add(errorScrollPane, c);

		panel.add(errorPanel, BorderLayout.SOUTH);

		final JLabel dummy = new JLabel("-");
		dummy.setPreferredSize(new Dimension(500, 500));
		dummy.setMinimumSize(new Dimension(500, 500));
		tableScrollPane = new JScrollPane(dummy, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		loadingContentPane = new LoadingContentPane("loading_data", tableScrollPane);
		panel.add(loadingContentPane, BorderLayout.CENTER);
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		loadingContentPane.init();
		dateFormatField.setSelectedItem(state.getTranslationConfiguration().getDatePattern());
		errorsAsMissingBox.setSelected(state.getTranslationConfiguration().isFaultTolerant());
		reloadButton.setEnabled(false);
		guessButton.setEnabled(false);
		new ProgressThread("loading_data") {

			@Override
			public void run() {
				try (DataResultSet previewResultSet = state.getDataResultSetFactory().makeDataResultSet(state.getOperator())) {
					state.getTranslationConfiguration().reconfigure(previewResultSet);
				} catch (OperatorException e1) {
					ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e1.toString(), e1);
					return;
				}

				try {
					TableModel dataPreview = state.getDataResultSetFactory().makePreviewTableModel(getProgressListener());
					// Copy name annotations to name
					int nameIndex = state.getTranslationConfiguration().getNameRow();
					if (nameIndex != -1 && dataPreview != null) {
						for (int i = 0; i < dataPreview.getColumnCount(); i++) {
							ColumnMetaData columnMetaData = state.getTranslationConfiguration().getColumnMetaData(i);
							String foundName = String.valueOf(dataPreview.getValueAt(nameIndex, i));
							if (state.getOperator() == null || state.getOperator().shouldTrimAttributeNames()) {
								foundName = foundName == null ? null : foundName.trim();
							}
							if (foundName != null && !foundName.isEmpty()) {
								columnMetaData.setUserDefinedAttributeName(foundName);
							}
						}
					}
				} catch (Exception e) {
					ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(), e);
					return;
				}
				SwingTools.invokeLater(MetaDataDeclarationWizardStep.this::guessValueTypes);
			}

		}.start();
		return true;
	}

	public void updateErrors(final Set<?> updateColumns) {
		updateErrors();
		((UpdatableHeaderRowFilteringTableModel) previewTable.getModel()).updateHeader(updateColumns);
	}

	public void updateErrors() {
		final List<ParsingError> errorList = new ArrayList<>();
		canProceed = headerValidator.getErrors().isEmpty();
		errorList.addAll(headerValidator.getErrors());
		errorList.addAll(state.getTranslator().getErrors());
		errorLabel.setText(errorList.size() + " errors.");
		errorLabel.setIcon(errorList.isEmpty() ? OK_ICON : ERROR_ICON);
		errorTableModel.setErrors(errorList);
		fireStateChanged();
	}

	private class UpdatableHeaderRowFilteringTableModel extends RowFilteringTableModel {

		private static final long serialVersionUID = 1L;

		public UpdatableHeaderRowFilteringTableModel(TableModel wrappedModel, int[] rowMap, boolean enabled) {
			super(wrappedModel, rowMap, enabled);
		}

		public void updateHeader(Set<?> columns) {
			fireTableCellUpdated(TableModelEvent.HEADER_ROW, TableModelEvent.ALL_COLUMNS);
		}

	}

	private void updateTableModel(ExampleSet exampleSet) {
		if (previewTable == null) {
			previewTable = new ExtendedJTable(false, false, false) {

				private static final long serialVersionUID = 1L;

				@Override
				public void createDefaultColumnsFromModel() {
					TableModel m = getModel();
					if (m != null) {
						// Remove any current columns
						TableColumnModel cm = getColumnModel();
						while (cm.getColumnCount() > 0) {
							cm.removeColumn(cm.getColumn(0));
						}

						// Create new columns from the data model info
						for (int i = 0; i < m.getColumnCount(); i++) {
							EditableTableHeaderColumn col = new EditableTableHeaderColumn(i);
							col.setHeaderValue(state.getTranslationConfiguration().getColumnMetaData()[i]);
							col.setHeaderRenderer(headerRenderer);
							col.setHeaderEditor(headerEditor);
							addColumn(col);
						}
					}
				}
			};
		}
		previewTable.setAutoCreateColumnsFromModel(true);

		// data model
		DataTableViewerTableModel model = new DataTableViewerTableModel(new DataTableExampleSetAdapter(exampleSet, null));
		int[] rowMap = state.getTranslator().getErrors().stream().mapToInt(ParsingError::getExampleIndex).distinct().toArray();
		filteredModel = new UpdatableHeaderRowFilteringTableModel(model, rowMap, filterErrorsBox.isSelected());
		previewTable.setModel(filteredModel);

		// Header validator
		this.headerValidator = new MetaDataValidator();
		headerValidator.addObserver((o, arg) -> {
			if (arg instanceof Set<?>) {
				updateErrors((Set<?>) arg);
			}
		});

		// Header model
		TableColumnModel columnModel = previewTable.getColumnModel();
		previewTable.setTableHeader(new EditableTableHeader(columnModel));
		headerRenderer = new MetaDataTableHeaderCellEditor(headerValidator);
		headerEditor = new MetaDataTableHeaderCellEditor(headerValidator);
		for (int i = 0; i < previewTable.getColumnCount(); i++) {
			EditableTableHeaderColumn col = (EditableTableHeaderColumn) previewTable.getColumnModel().getColumn(i);
			ColumnMetaData cmd = state.getTranslationConfiguration().getColumnMetaData()[i];
			headerValidator.addColumnMetaData(cmd, i);
			col.setHeaderValue(cmd);
			col.setHeaderRenderer(headerRenderer);
			col.setHeaderEditor(headerEditor);
		}
		headerValidator.checkForDuplicates();
		previewTable.getTableHeader().setReorderingAllowed(false);

		previewTable.setCellColorProvider(new CellColorProviderAlternating() {

			@Override
			public Color getCellColor(int row, int column) {
				row = filteredModel.translateRow(row);
				ParsingError error = state.getTranslator().getErrorByExampleIndexAndColumn(row, column);
				if (error != null) {
					return SwingTools.DARK_YELLOW;
				} else {
					return super.getCellColor(row, column);
				}
			}
		});
		tableScrollPane.setViewportView(previewTable);
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.FINISH) {
			try {
				if (state.getTranslator() != null) {
					state.getTranslator().close();
				}
			} catch (OperatorException e) {
				ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(), e);
			}

			// use settings edited by user even if he never pressed Enter or otherwise confirmed his
			// changes
			for (int i = 0; i < previewTable.getColumnCount(); i++) {
				EditableTableHeaderColumn col = (EditableTableHeaderColumn) previewTable.getColumnModel().getColumn(i);
				if (col.getHeaderEditor() instanceof MetaDataTableHeaderCellEditor) {
					MetaDataTableHeaderCellEditor editor = (MetaDataTableHeaderCellEditor) col.getHeaderEditor();
					editor.updateColumnMetaData();
				}
			}
		}
		return true;
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}

	@Override
	protected boolean canProceed() {
		return canProceed;
	}

	@Override
	protected JComponent getComponent() {
		return panel;
	}

	private void reload() {
		guessButton.setEnabled(false);
		reloadButton.setAction(cancelReloadAction);
		isReloading = true;
		reloadData("loading_data");
	}

	private void cancelReload() {
		reloadButton.setEnabled(false);
		state.getTranslator().cancelLoading();
	}

	private void guessValueTypes() {
		reloadButton.setEnabled(false);
		guessButton.setAction(cancelGuessValueTypes);
		isGuessing = true;
		reloadData("guessing_value_types");
	}

	private void cancelGuessing() {
		guessButton.setEnabled(false);
		state.getTranslator().cancelGuessing();
		state.getTranslator().cancelLoading();
	}

	/**
	 * Reloads the data
	 *
	 * @param progressThreadLabel label key used for the progress thread
	 */
	private void reloadData(String progressThreadLabel) {
		SwingTools.invokeLater(loadingContentPane::init);
		new ProgressThread(progressThreadLabel) {

			@Override
			public void run() {
				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(1);
				final ColumnMetaData[] metaData = state.getTranslationConfiguration().getColumnMetaData().clone();
				try (DataResultSet resultSet = state.getDataResultSetFactory().makeDataResultSet(state.getOperator())) {
					state.getTranslator().close();
					state.getTranslator().clearErrors();
					// Clear configuration to avoid invalid result from readNow
					state.getTranslationConfiguration().reconfigure(resultSet);

					if (isGuessing) {
						state.getTranslator().guessValueTypes(state.getTranslationConfiguration(), resultSet,
								state.getNumberOfPreviewRows(), getProgressListener());
						checkForGuessingCanceled();
					} else {
						//Set previous value types, so that readNow will add the errors
						IntStream.range(0, metaData.length).forEach(i -> state.getTranslationConfiguration().getColumnMetaData()[i].setAttributeValueType(metaData[i].getAttributeValueType()));
					}
					setDisplayLabel("generate_preview");
					final ExampleSet exampleSet = state.readNow(resultSet, limitedPreviewBox.isSelected(),
							getProgressListener());
					checkForLoadingCanceled();

					final String[] newColumnNames = Stream.of(state.getTranslationConfiguration().getColumnMetaData()).filter(Objects::nonNull).map(ColumnMetaData::getUserDefinedAttributeName).toArray(String[]::new);
					// Only restore columnMetaData if the column names are the same as before
					if (Arrays.deepEquals(newColumnNames, columnNames)) {
						// Set the guessed value type for each column
						for (int i = 0; i < metaData.length && isGuessing; i++) {
							metaData[i].setAttributeValueType(state.getTranslationConfiguration().getColumnMetaData()[i].getAttributeValueType());
						}
						// Restore user defined column meta data
						state.getTranslationConfiguration().setColumnMetaData(metaData);
					}
					// Store the new column names, since they are modifiable by the user
					columnNames = newColumnNames;

					SwingTools.invokeLater(() -> updateTableModel(exampleSet));

				} catch (OperatorException e) {
					ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(), e);
				} catch (LoadingCanceledException e) {
					state.getTranslator().clearErrors();
					state.getTranslationConfiguration().setColumnMetaData(metaData);
				} finally {
					getProgressListener().complete();
					SwingTools.invokeLater(() -> {
						updateErrors();
						loadingContentPane.loadingFinished();
						guessButton.setAction(guessValueTypes);
						reloadButton.setAction(reloadAction);
						isLoadingOrCanceling.set(false, false);
						isGuessing = false;
						isReloading = false;
						reloadButton.setEnabled(true);
						guessButton.setEnabled(true);
					});
				}
			}

			private void checkForLoadingCanceled() throws LoadingCanceledException {
				if (state.getTranslator().isLoadingCancelled()) {
					throw new LoadingCanceledException();
				}
			}

			private void checkForGuessingCanceled() throws LoadingCanceledException {
				if (isGuessing && state.getTranslator().isGuessingCancelled()) {
					throw new LoadingCanceledException();
				}
			}

			/** marker for loading canceled */
			class LoadingCanceledException extends Exception {
			}
		}.start();
	}
}
