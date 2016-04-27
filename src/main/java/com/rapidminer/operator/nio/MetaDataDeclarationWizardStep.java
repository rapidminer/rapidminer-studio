/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.CellColorProviderAlternating;
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

	/** Publicly exposes the method {@link #configurePropertiesFromAction(Action)} public. */
	private static class ReconfigurableButton extends JButton {

		private static final long serialVersionUID = 1L;

		private ReconfigurableButton(Action action) {
			super(action);
		}

		@Override
		protected void configurePropertiesFromAction(Action a) {
			super.configurePropertiesFromAction(a);
		}
	}

	private Action reloadAction = new ResourceAction("wizard.validate_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			toggleReload();
		}
	};
	private Action cancelReloadAction = new ResourceAction("wizard.abort_validate_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			toggleReload();
		}
	};
	private ReconfigurableButton reloadButton = new ReconfigurableButton(reloadAction);

	private Action guessValueTypes = new ResourceAction("wizard.guess_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			toggleGuessValueTypes();
		}
	};
	private Action cancelGuessValueTypes = new ResourceAction("wizard.abort_guess_value_types") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			toggleGuessValueTypes();
		}
	};
	private ReconfigurableButton guessButton = new ReconfigurableButton(guessValueTypes);

	private JCheckBox errorsAsMissingBox = new JCheckBox(new ResourceAction("wizard.error_tolerant") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			state.getTranslationConfiguration().setFaultTolerant(errorsAsMissingBox.isSelected());
		}
	});
	private JCheckBox filterErrorsBox = new JCheckBox(new ResourceAction("wizard.show_error_rows") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (filteredModel != null) {
				filteredModel.setFilterEnabled(filterErrorsBox.isSelected());
			}
		}
	});

	private JComboBox dateFormatField = new JComboBox(ParameterTypeDateFormat.PREDEFINED_DATE_FORMATS);

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
		final JScrollPane errorScrollPane = new JScrollPane(errorTable);
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
							final String foundName = String.valueOf(dataPreview.getValueAt(nameIndex, i));
							if (foundName != null && !foundName.isEmpty()) {
								columnMetaData.setUserDefinedAttributeName(foundName);
							}
						}
					}
				} catch (Exception e) {
					ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(), e);
					return;
				}
				guessValueTypes();
			}

		}.start();
		return true;
	}

	public void updateErrors(final Set<?> updateColumns) {
		updateErrors();
		((UpdatableHeaderRowFilteringTableModel) previewTable.getModel()).updateHeader(updateColumns);
	}

	public void updateErrors() {
		final List<ParsingError> errorList = new ArrayList<ParsingError>();

		canProceed = true;
		if (headerValidator.getErrors().size() > 0) {
			List<ParsingError> headerErrors = headerValidator.getErrors();
			errorList.addAll(headerErrors);
			canProceed = false;
		}
		errorList.addAll(state.getTranslator().getErrors());

		final int size = errorList.size();
		errorLabel.setText(size + " errors.");
		if (size == 0) {
			errorLabel.setIcon(SwingTools.createIcon("16/ok.png"));
		} else {
			errorLabel.setIcon(SwingTools.createIcon("16/error.png"));
		}

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
		List<Integer> rowsList = new LinkedList<Integer>();
		int lastHit = -1;
		for (ParsingError error : state.getTranslator().getErrors()) {
			if (error.getExampleIndex() != lastHit) {
				rowsList.add(error.getExampleIndex());
				lastHit = error.getExampleIndex();
			}
		}
		int[] rowMap = new int[rowsList.size()];
		int j = 0;
		for (Integer row : rowsList) {
			rowMap[j++] = row;
		}
		filteredModel = new UpdatableHeaderRowFilteringTableModel(model, rowMap, filterErrorsBox.isSelected());
		previewTable.setModel(filteredModel);

		// Header validator
		this.headerValidator = new MetaDataValidator();
		headerValidator.addObserver(new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				if (arg instanceof Set<?>) {
					updateErrors((Set<?>) arg);
				}
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
		reloadButton.configurePropertiesFromAction(cancelReloadAction);
		new ProgressThread("loading_data") {

			@Override
			public void run() {
				try (DataResultSet resultSet = state.getDataResultSetFactory().makeDataResultSet(null)) {
					if (state.getTranslator() != null) {
						state.getTranslator().close();
					}
					state.getTranslator().clearErrors();
					final ExampleSet exampleSet = state.readNow(resultSet, limitedPreviewBox.isSelected(),
							getProgressListener());

					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							updateTableModel(exampleSet);
							updateErrors();
						}
					});
				} catch (OperatorException e) {
					ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(), e);
				} finally {
					getProgressListener().complete();
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							reloadButton.configurePropertiesFromAction(reloadAction);
							reloadButton.setEnabled(true);
							isReloading = false;
						}
					});
				}
			}
		}.start();
	}

	private void cancelReload() {
		state.getTranslator().cancelLoading();
		reloadButton.setEnabled(false);
	}

	private void guessValueTypes() {
		loadingContentPane.init();
		guessButton.configurePropertiesFromAction(cancelGuessValueTypes);
		isGuessing = true;
		new ProgressThread("guessing_value_types") {

			@Override
			public void run() {
				Thread.yield();
				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(1);
				try (DataResultSet resultSet = state.getDataResultSetFactory().makeDataResultSet(null)) {
					if (state.getTranslator() != null) {
						state.getTranslator().close();
					}

					state.getTranslator().clearErrors();
					state.getTranslationConfiguration().resetValueTypes();
					state.getTranslator().guessValueTypes(state.getTranslationConfiguration(), resultSet,
							state.getNumberOfPreviewRows(), getProgressListener());
					if (!state.getTranslator().isGuessingCancelled()) {
						setDisplayLabel("generate_preview");
						final ExampleSet exampleSet = state.readNow(resultSet, limitedPreviewBox.isSelected(),
								getProgressListener());
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								updateTableModel(exampleSet);
								updateErrors();
								loadingContentPane.loadingFinished();
							}
						});
					} else {
						loadingContentPane.loadingFinished();
					}
				} catch (OperatorException e) {
					ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(), e);
				} finally {
					getProgressListener().complete();
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							guessButton.configurePropertiesFromAction(guessValueTypes);
							guessButton.setEnabled(true);
							isGuessing = false;
						}
					});
				}
			}
		}.start();
	}

	private boolean isGuessing = false;
	private boolean isReloading = false;
	private ExtendedJTable previewTable;
	private MetaDataTableHeaderCellEditor headerRenderer;
	private MetaDataTableHeaderCellEditor headerEditor;

	private void cancelGuessing() {
		state.getTranslator().cancelGuessing();
		state.getTranslator().cancelLoading();
		guessButton.setEnabled(false);
	}

	private void toggleGuessValueTypes() {
		isGuessing = !isGuessing;
		if (isGuessing) {
			guessValueTypes();
		} else {
			cancelGuessing();
		}
	}

	private void toggleReload() {
		isReloading = !isReloading;
		if (isReloading) {
			reload();
		} else {
			cancelReload();
		}
	}

}
