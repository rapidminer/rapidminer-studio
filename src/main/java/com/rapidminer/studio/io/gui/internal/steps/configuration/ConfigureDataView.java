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
package com.rapidminer.studio.io.gui.internal.steps.configuration;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.tools.ColoredTableCellRenderer;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.RowNumberTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.studio.io.gui.internal.DataImportWizardUtils;
import com.rapidminer.studio.io.gui.internal.DataWizardEventType;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * The view shown during the data configuration step. It contains a table showing the loaded data
 * preview and allows the user to configure the data via the table header.
 *
 * @author Nils Woehler, Gisa Schaefer
 * @since 7.0.0
 */
final class ConfigureDataView extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final String PROGRESS_THREAD_ID = "io.dataimport.step.data_column_configuration.prepare_data_preview";

	public static final Color BACKGROUND_COLUMN_DISABLED = new Color(232, 232, 232);
	public static final Color FOREGROUND_COLUMN_DISABLED = new Color(189, 189, 189);
	private static final String ERROR_TOOLTIP_CONTENT = "<p style=\"padding-bottom:4px\">"
			+ I18N.getGUILabel("io.dataimport.step.data_column_configuration.replace_errors_checkbox.tip") + "</p>";

	/** cell renderer that displays icons */
	private static final DefaultTableCellRenderer ICON_CELL_RENDERER = new DefaultTableCellRenderer() {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			label.setText(null);
			label.setIcon((ImageIcon) value);
			return label;
		}

	};

	/**
	 * Colored border for error cells. The empty border part together with the colored border gives
	 * a border of the same size as {@link ColoredTableCellRenderer#CELL_BORDER}
	 */
	private static final Border ERROR_BORDER = BorderFactory
			.createCompoundBorder(BorderFactory.createLineBorder(Color.RED, 2), BorderFactory.createEmptyBorder(0, 8, 0, 3));
	private static final Border WARNING_BORDER = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.ORANGE, 2), BorderFactory.createEmptyBorder(0, 8, 0, 3));

	/** Cell renderer that marks error cells with a colored border */
	private final ColoredTableCellRenderer ERROR_MARKING_CELL_RENDERER = new ColoredTableCellRenderer() {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (tableModel.hasError(row, column)) {
				if (errorHandlingCheckBox.isSelected()) {
					label.setBorder(WARNING_BORDER);
				} else {
					label.setBorder(ERROR_BORDER);
				}
			}
			return label;
		}

	};

	private List<ChangeListener> changeListeners = new LinkedList<>();

	private DataSetMetaData dataSetMetaData;
	private ConfigureDataTableModel tableModel;
	private JPanel centerPanel;
	private JPanel upperPanel;
	private final JCheckBox errorHandlingCheckBox;
	private final ConfigureDataValidator validator;
	private final ErrorWarningTableModel errorTableModel;
	private final CollapsibleErrorTable collapsibleErrorTable;
	private final JComboBox<String> dateFormatField;

	private boolean fatalError;

	/**
	 * The constructor that creates a new {@link ConfigureDataView} instance.
	 */
	public ConfigureDataView(JDialog owner) {
		validator = new ConfigureDataValidator();
		errorTableModel = new ErrorWarningTableModel(validator);
		errorTableModel.addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				fireStateChanged();
			}
		});
		collapsibleErrorTable = new CollapsibleErrorTable(errorTableModel);

		setLayout(new BorderLayout());
		upperPanel = new JPanel(new GridBagLayout());
		upperPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
		upperPanel.setVisible(false);

		JPanel errorHandlingPanel = new JPanel(new BorderLayout());
		errorHandlingCheckBox = new JCheckBox(
				I18N.getGUILabel("io.dataimport.step.data_column_configuration.replace_errors_checkbox.label"));
		errorHandlingCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				DataImportWizardUtils.logStats(DataWizardEventType.ERROR_HANDLING_CHANGED,
						Boolean.toString(errorHandlingCheckBox.isSelected()));
				errorTableModel.setFaultTolerant(errorHandlingCheckBox.isSelected());
				tableModel.fireTableDataChanged();
			}
		});
		errorHandlingPanel.add(errorHandlingCheckBox, BorderLayout.CENTER);
		SwingTools.addTooltipHelpIconToLabel(ERROR_TOOLTIP_CONTENT, errorHandlingPanel, owner);

		dateFormatField = new JComboBox<String>(ParameterTypeDateFormat.PREDEFINED_DATE_FORMATS);

		dateFormatField.setEditable(true);
		// do not fire action event when using keyboard to move up and down
		dateFormatField.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
		dateFormatField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// prevent two updates on enter key
				if (!"comboBoxChanged".equals(e.getActionCommand())) {
					return;
				}
				String datePattern = (String) dateFormatField.getSelectedItem();
				if (datePattern != null && !datePattern.isEmpty()) {
					updateDateFormat(new SimpleDateFormat(datePattern));
				}
			}

		});

		JLabel datelabel = new ResourceLabel("date_format");
		datelabel.setLabelFor(dateFormatField);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 30, 0, 5);
		gbc.weightx = 0.0;
		gbc.anchor = GridBagConstraints.WEST;

		upperPanel.add(datelabel, gbc);

		gbc.insets = new Insets(0, 0, 0, 70);
		upperPanel.add(dateFormatField, gbc);

		gbc.insets = new Insets(0, 0, 0, 0);
		upperPanel.add(errorHandlingPanel, gbc);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		upperPanel.add(new JLabel(), gbc);

		add(upperPanel, BorderLayout.NORTH);
		centerPanel = new JPanel(new GridBagLayout());
		add(centerPanel, BorderLayout.CENTER);
		add(collapsibleErrorTable, BorderLayout.SOUTH);
		collapsibleErrorTable.setVisible(false);
	}

	/**
	 * Takes the current data source, copies the meta data and configures the view according to the
	 * data and meta data within a progress thread.
	 *
	 * @param dataSource
	 *            the data source to retrieve the meta data and preview data from
	 * @throws InvalidConfigurationException
	 *             in case the meta data could not be retrieved
	 */
	void updatePreviewContent(final DataSource dataSource) throws InvalidConfigurationException {
		fatalError = false;

		// copy meta data to work on copy instead of real instance
		try {
			dataSetMetaData = dataSource.getMetadata().copy();
		} catch (DataSetException e) {
			SwingTools.showSimpleErrorMessage("io.dataimport.step.data_column_configuration.error_configuring_metadata",
					e.getMessage());
			throw new InvalidConfigurationException();
		}

		errorHandlingCheckBox.setSelected(dataSetMetaData.isFaultTolerant());
		DateFormat dateFormat = dataSetMetaData.getDateFormat();
		if (dateFormat instanceof SimpleDateFormat) {
			// remove action listeners before setting the date format pattern to prevent
			// unneccessary update
			ActionListener[] listeners = dateFormatField.getActionListeners();
			dateFormatField.removeActionListener(listeners[0]);
			dateFormatField.setSelectedItem(((SimpleDateFormat) dateFormat).toPattern());
			dateFormatField.addActionListener(listeners[0]);
		}
		errorTableModel.setColumnMetaData(dataSetMetaData.getColumnMetaData());
		validator.init(dataSetMetaData.getColumnMetaData());

		ProgressThread loadDataPG = new ProgressThread(PROGRESS_THREAD_ID) {

			@Override
			public void run() {
				getProgressListener().setTotal(100);
				SwingTools.invokeLater(new Runnable() {

					@Override
					public void run() {
						showNotificationLabel("io.dataimport.step.data_column_configuration.loading_preview");
					}
				});

				// load table model
				try {
					tableModel = new ConfigureDataTableModel(dataSource, dataSetMetaData, getProgressListener());
					validator.setParsingErrors(tableModel.getParsingErrors());

					// adapt view after table has been loaded
					SwingTools.invokeLater(new Runnable() {

						@Override
						public void run() {

							// show error message in case preview is empty
							if (tableModel.getRowCount() == 0) {
								showErrorNotification("io.dataimport.step.data_column_configuration.no_data_available");
								return;
							}

							// remove all components
							centerPanel.removeAll();

							// add preview table
							ExtendedJTable previewTable = new ExtendedJTable(tableModel, false, false, false) {

								private static final long serialVersionUID = 1L;

								@Override
								public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
									Component c = super.prepareRenderer(renderer, row, column);
									ColumnMetaData metaData = dataSetMetaData.getColumnMetaData().get(column);
									if (metaData.isRemoved()) {
										c.setBackground(BACKGROUND_COLUMN_DISABLED);
										c.setForeground(FOREGROUND_COLUMN_DISABLED);
									} else {
										String role = metaData.getRole();
										if (role != null) {
											c.setBackground(AttributeGuiTools.getColorForAttributeRole(role));
										} else {
											c.setBackground(Color.WHITE);
										}
										c.setForeground(Color.BLACK);
									}
									return c;
								};
							};
							previewTable.setColumnSelectionAllowed(false);
							previewTable.setCellSelectionEnabled(false);
							previewTable.setRowSelectionAllowed(false);
							previewTable.setColoredTableCellRenderer(ERROR_MARKING_CELL_RENDERER);
							previewTable.setShowPopupMenu(false);

							// ensure same background as JPanels in case of only few rows
							previewTable.setBackground(Colors.PANEL_BACKGROUND);

							TableColumnModel columnModel = previewTable.getColumnModel();

							// set cell renderer for column headers
							previewTable.setTableHeader(new JTableHeader(columnModel));
							for (int columnIndex = 0; columnIndex < columnModel.getColumnCount(); columnIndex++) {
								TableColumn column = columnModel.getColumn(columnIndex);
								ConfigureDataTableHeader headerRenderer = new ConfigureDataTableHeader(previewTable,
										columnIndex, dataSetMetaData, validator, ConfigureDataView.this);
								column.setHeaderRenderer(headerRenderer);
								column.setMinWidth(120);
							}

							previewTable.getTableHeader().setReorderingAllowed(false);

							// Create a layered pane to display both, the data table and a
							// "preview" overlay
							JLayeredPane layeredPane = new JLayeredPane();
							layeredPane.setLayout(new OverlayLayout(layeredPane));

							/*
							 * Hack to enlarge table columns in case of few columns. Add table to a
							 * full size JPanel and add the table header to the scroll pane.
							 */
							JPanel tablePanel = new JPanel(new BorderLayout());
							tablePanel.add(previewTable, BorderLayout.CENTER);

							JScrollPane scrollPane = new ExtendedJScrollPane(tablePanel);
							scrollPane.setColumnHeaderView(previewTable.getTableHeader());

							scrollPane.setBorder(null);

							// show row numbers
							scrollPane.setRowHeaderView(new RowNumberTable(previewTable));
							layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);

							// Add "Preview" overlay
							JPanel previewPanel = new JPanel(new BorderLayout());
							previewPanel.setOpaque(false);
							JLabel previewLabel = new JLabel(I18N.getGUILabel("csv_format_specification.preview_background"),
									SwingConstants.CENTER);
							previewLabel.setFont(previewLabel.getFont().deriveFont(Font.BOLD, 180));
							previewLabel.setForeground(DataImportWizardUtils.getPreviewFontColor());
							previewPanel.add(previewLabel, BorderLayout.CENTER);
							layeredPane.add(previewPanel, JLayeredPane.PALETTE_LAYER);

							GridBagConstraints constraint = new GridBagConstraints();
							constraint.fill = GridBagConstraints.BOTH;
							constraint.weightx = 1.0;
							constraint.weighty = 1.0;
							centerPanel.add(layeredPane, constraint);

							centerPanel.revalidate();
							centerPanel.repaint();

							upperPanel.setVisible(true);
							setupErrorTable();

							fireStateChanged();
						}

					});
				} catch (final DataSetException e) {
					SwingTools.invokeLater(new Runnable() {

						@Override
						public void run() {
							showErrorNotification("io.dataimport.step.data_column_configuration.error_loading_data",
									e.getMessage());
						}
					});
				} finally {
					getProgressListener().complete();
				}
			}
		};
		loadDataPG.addDependency(PROGRESS_THREAD_ID);
		loadDataPG.start();
	}

	/**
	 * Sets up the error table.
	 */
	private void setupErrorTable() {
		// increase row height
		collapsibleErrorTable.getTable().setRowHeight(20);

		// make first column smaller and add special cell renderer
		collapsibleErrorTable.getTable().getColumnModel().getColumn(0).setPreferredWidth(22);
		collapsibleErrorTable.getTable().getColumnModel().getColumn(0).setMaxWidth(22);
		collapsibleErrorTable.getTable().getColumnModel().getColumn(0).setCellRenderer(ICON_CELL_RENDERER);

		// make second column smaller
		collapsibleErrorTable.getTable().getColumnModel().getColumn(1).setPreferredWidth(50);
		collapsibleErrorTable.getTable().getColumnModel().getColumn(1).setMaxWidth(100);

		// make third column small
		collapsibleErrorTable.getTable().getColumnModel().getColumn(2).setPreferredWidth(100);
		collapsibleErrorTable.getTable().getColumnModel().getColumn(2).setMaxWidth(800);

		// make last column bigger
		collapsibleErrorTable.getTable().getColumnModel().getColumn(5).setMaxWidth(800);
		collapsibleErrorTable.getTable().getColumnModel().getColumn(5).setPreferredWidth(400);

		collapsibleErrorTable.update();
		collapsibleErrorTable.setVisible(true);
	}

	void showErrorNotification(String i18nKey, Object... arguments) {
		showNotificationLabel(i18nKey, arguments);
		fatalError = true;
		fireStateChanged();
	}

	/**
	 * Shows a central label displaying a notification to the user (e.g. for errors or during
	 * loading).
	 *
	 * @param i18nKey
	 *            the notification I18N key to lookup the label text and icon
	 * @param arguments
	 *            the I18N arguments
	 */
	private void showNotificationLabel(String i18nKey, Object... arguments) {
		upperPanel.setVisible(false);
		collapsibleErrorTable.setVisible(false);

		GridBagConstraints constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.BOTH;
		constraint.weightx = 1.0;
		constraint.weighty = 1.0;

		centerPanel.removeAll();

		centerPanel.add(new JPanel(), constraint);

		constraint.weightx = 0.0;
		constraint.weighty = 0.0;
		constraint.fill = GridBagConstraints.NONE;
		constraint.anchor = GridBagConstraints.CENTER;
		centerPanel.add(new ResourceLabel(i18nKey, arguments), constraint);

		constraint.weightx = 1.0;
		constraint.weighty = 1.0;
		constraint.fill = GridBagConstraints.BOTH;
		centerPanel.add(new JPanel(), constraint);

		centerPanel.revalidate();
		centerPanel.repaint();
	}

	/**
	 * @return the meta data for the current view.
	 */
	DataSetMetaData getMetaData() {
		dataSetMetaData.setFaultTolerant(errorHandlingCheckBox.isSelected());
		return dataSetMetaData;
	}

	/**
	 * Checks whether the current view configuration is valid.
	 *
	 * @throws InvalidConfigurationException
	 *             in case the configuration is invalid
	 *
	 */
	public void validateConfiguration() throws InvalidConfigurationException {
		if (fatalError || tableModel != null && (tableModel.getRowCount() == 0 || errorTableModel.getErrorCount() > 0)) {
			throw new InvalidConfigurationException();
		}
	}

	/**
	 * Registers a new change listener.
	 *
	 * @param changeListener
	 *            the listener to register
	 */
	void addChangeListener(ChangeListener changeListener) {
		this.changeListeners.add(changeListener);
	}

	/**
	 * Fires a {@link ChangeEvent} that informs the listeners of a changed state.
	 */
	private void fireStateChanged() {
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

	/**
	 * Updates date format for the date and reloads it.
	 *
	 * @param format
	 *            the new date format
	 */
	private void updateDateFormat(SimpleDateFormat format) {
		DataImportWizardUtils.logStats(DataWizardEventType.DATE_FORMAT_CHANGED, format.toPattern());
		dataSetMetaData.setDateFormat(format);
		ProgressThread rereadThread = new ProgressThread("io.dataimport.step.data_column_configuration.update_date_format") {

			@Override
			public void run() {
				try {
					tableModel.reread(getProgressListener());
				} catch (final DataSetException e) {
					SwingTools.invokeLater(new Runnable() {

						@Override
						public void run() {
							showErrorNotification("io.dataimport.step.data_column_configuration.error_loading_data",
									e.getMessage());
						}
					});
					return;
				}
				SwingTools.invokeLater(new Runnable() {

					@Override
					public void run() {
						validator.setParsingErrors(tableModel.getParsingErrors());
						tableModel.fireTableDataChanged();
					}
				});

			}
		};

		rereadThread.start();
	}

}
