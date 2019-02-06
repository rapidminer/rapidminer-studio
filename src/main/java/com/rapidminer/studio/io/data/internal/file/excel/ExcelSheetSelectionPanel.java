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
package com.rapidminer.studio.io.data.internal.file.excel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
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
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ColoredTableCellRenderer;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.RowNumberTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.bubble.BubbleWindow;
import com.rapidminer.gui.tools.bubble.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tools.bubble.BubbleWindow.BubbleStyle;
import com.rapidminer.gui.tools.bubble.ComponentBubbleWindow;
import com.rapidminer.operator.nio.model.xlsx.XlsxSheetMetaDataParser;
import com.rapidminer.operator.nio.model.xlsx.XlsxUtilities;
import com.rapidminer.operator.nio.model.xlsx.XlsxUtilities.XlsxCellCoordinates;
import com.rapidminer.studio.io.data.internal.ResultSetAdapter;
import com.rapidminer.studio.io.gui.internal.DataImportWizardUtils;
import com.rapidminer.studio.io.gui.internal.DataWizardEventType;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.container.Pair;


/**
 * This is a panel showing the contents of a complete excel workbook. The user is able to select the
 * current sheet via a drop down menu.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
final class ExcelSheetSelectionPanel extends JPanel {

	private static final long serialVersionUID = 9179757216097316344L;

	private static final Dimension CELL_TEXTFIELD_MIN_DIMENSION = new Dimension(50, 25);
	private static final Dimension CELL_TEXTFIELD_PREF_DIMENSION = new Dimension(150, 25);

	private static final Dimension CELL_CHECKBOX_MIN_DIMENSION = new Dimension(100, 25);

	/** re-use same log timer for all data import wizard dialogs */
	private static final Timer CELL_RANGE_TIMER = new Timer(500, null);

	/**
	 * Cell renderer for the content table.
	 */
	private final ColoredTableCellRenderer tableCellRenderer = new ColoredTableCellRenderer() {

		private final DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
		private final Font normalFont = defaultRenderer.getFont();
		private final Font boldFont = defaultRenderer.getFont().deriveFont(Font.BOLD);

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
					column);
			try {
				if (row == getHeaderRowIndex()) {
					rendererComponent.setFont(boldFont);
				} else {
					rendererComponent.setFont(normalFont);
				}
			} catch (NumberFormatException e) {
				rendererComponent.setFont(normalFont);
			}

			if (sheetSelectionModel.isShowingPreview()) {
				try {
					CellRangeSelection selection = getSelection();

					// highlight preview cells as if they were selected if included in the selection
					if (row >= selection.getRowIndexStart() && row <= selection.getRowIndexEnd()
							&& column >= selection.getColumnIndexStart() && column <= selection.getColumnIndexEnd()) {
						rendererComponent.setBackground(contentTable.getSelectionBackground());
						rendererComponent.setForeground(contentTable.getSelectionForeground());
					} else {
						rendererComponent.setBackground(contentTable.getBackground());
						rendererComponent.setForeground(contentTable.getForeground());
					}
				} catch (InvalidConfigurationException e) {
					rendererComponent.setBackground(contentTable.getBackground());
					rendererComponent.setForeground(contentTable.getForeground());
				}
			}
			return rendererComponent;
		}
	};

	/**
	 * Selection listener which updates the sheet selection model in case of list selection events.
	 */
	private final ListSelectionListener selectionListener = new ListSelectionListener() {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting() && !updatingUI) {
				sheetSelectionModel.updateCellRangeByTableSelection(contentTable);
			}
		}
	};

	/**
	 * Selection listener which updates the sheet selection model in case of column selection
	 * events.
	 */
	private final TableColumnModelListener columnSelectionListener = new TableColumnModelListener() {

		@Override
		public void columnAdded(TableColumnModelEvent e) {}

		@Override
		public void columnRemoved(TableColumnModelEvent e) {}

		@Override
		public void columnMoved(TableColumnModelEvent e) {}

		@Override
		public void columnMarginChanged(ChangeEvent e) {}

		@Override
		public void columnSelectionChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting() && !updatingUI) {
				sheetSelectionModel.updateCellRangeByTableSelection(contentTable);
			}
		}

	};

	/**
	 * Action which selects the whole sheet content.
	 */
	private final Action selectAllAction = new ResourceActionAdapter(false,
			"io.dataimport.step.excel.sheet_selection.select_all") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			killCurrentBubbleWindow(null);

			// create a new selection object
			CellRangeSelection selection = new CellRangeSelection(0, XlsxCellCoordinates.NO_ROW_NUMBER,
					contentTable.getModel().getColumnCount() - 1, XlsxSheetMetaDataParser.MAXIMUM_XLSX_ROW_INDEX);

			// update the model (will trigger an UI update)
			sheetSelectionModel.setCellRangeSelection(selection);
		}
	};

	/**
	 * Action to apply the selection entered into the cell range text field.
	 */
	private final Action applySelectionAction = new ResourceActionAdapter(false,
			"io.dataimport.step.excel.sheet_selection.apply_cell_selection") {

		private static final long serialVersionUID = 1L;

		@Override
		public synchronized void loggedActionPerformed(ActionEvent e) {
			applyTextFieldSelection(true);
		}

	};

	/**
	 * Item listener that acts in case the sheet has been changed.
	 */
	private final ItemListener comboBoxItemListener = new ItemListener() {

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				if (!updatingUI) {
					int newSheetIndex = sheetComboBox.getSelectedIndex();
					sheetSelectionModel.setSheetIndex(newSheetIndex);
				}
			}
		}
	};

	/**
	 * Observer that watches the {@link ExcelSheetSelectionPanelModel} and updates the UI on
	 * changes.
	 */
	private final ExcelSheetSelectionModelListener sheetSelectionModelListener = new ExcelSheetSelectionModelListener() {

		@Override
		public void loadingNewTableModel() {
			SwingTools.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					updatingUI = true;
					showNotificationLabel("io.dataimport.step.excel.sheet_selection.loading_excel_sheets");
					fireStateChanged();
					updatingUI = false;
				}
			});
		}

		@Override
		public void reportErrorLoadingTableModel(final Exception e) {
			SwingTools.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					showNotificationLabel("io.dataimport.step.excel.sheet_selection.error_loading_excel_sheet",
							e.getMessage());
					fireStateChanged();
				}
			});
		}

		@Override
		public void sheetIndexUpdated(final int newSheetIndex, final String[] sheetNames, final TableModel tableModel,
				final boolean isShowingPreview, final boolean wasModelLoaded) {

			updatingUI = true;

			SwingTools.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					fireStateChanged();
					updateTableModel(tableModel, isShowingPreview, wasModelLoaded, sheetNames, newSheetIndex);
				}
			});

			updatingUI = false;

			fireStateChanged();

		}

		@Override
		public void headerRowIndexUpdated(final int newHeaderRowIndex) {
			updatingUI = true;

			SwingTools.invokeAndWait(() -> {
				boolean hasHeaderRow = newHeaderRowIndex > ResultSetAdapter.NO_HEADER_ROW;
				int displayedHeaderRowIndex = hasHeaderRow ? newHeaderRowIndex + 1 : 1;
				headerRowSpinner.setModel(new SpinnerNumberModel(displayedHeaderRowIndex, 1, Integer.MAX_VALUE, 1));
				hasHeaderRowCheckBox.setSelected(hasHeaderRow);
				killCurrentBubbleWindow(headerRowSpinner);

				contentTable.revalidate();
				contentTable.repaint();
			});

			updatingUI = false;

			fireStateChanged();
		}

		@Override
		public void cellRangeSelectionUpdate(final CellRangeSelection newSelection) {
			updatingUI = true;

			SwingTools.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					if (newSelection == null) {
						contentTable.clearSelection();
						return;
					}

					// column indices
					int columnStartIndex = Math.max(newSelection.getColumnIndexStart(), 0);
					int columnEndIndex = Math.min(newSelection.getColumnIndexEnd(),
							isSheetEmpty() ? 0 : contentTable.getColumnCount() - 1);

					// row indices
					int firstSelectedRow = newSelection.getRowIndexStart();
					int lastSelectedRow = newSelection.getRowIndexEnd();

					if (sheetSelectionModel.isShowingPreview()) {
						// Only update the text field when showing a preview
						updateCellRangeTextFields(columnStartIndex, firstSelectedRow, columnEndIndex, lastSelectedRow);
					} else {

						if (!isSheetEmpty()) {
							contentTable.clearSelection();

							int tableRowCount = isSheetEmpty() ? 0 : contentTable.getRowCount() - 1;
							int lastSelectedTableRow = Math.min(newSelection.getRowIndexEnd(), tableRowCount);

							// no row number means we start with first row for table selection
							int firstSelectedTableRow = firstSelectedRow == XlsxCellCoordinates.NO_ROW_NUMBER ? 0
									: firstSelectedRow;

							// update the table selection
							contentTable.setColumnSelectionInterval(columnStartIndex, columnEndIndex);
							contentTable.setRowSelectionInterval(firstSelectedTableRow, lastSelectedTableRow);

							// update the text field
							updateCellRangeTextFields(columnStartIndex, firstSelectedRow, columnEndIndex, lastSelectedRow);
						}

					}

				}
			});

			updatingUI = false;

			fireStateChanged();

			contentTable.revalidate();
			contentTable.repaint();
		}

		private void updateTableModel(TableModel selectedModel, boolean isShowingPreview, boolean wasModelLoaded,
				String[] sheetNames, int sheetIndex) {

			final GridBagConstraints constraint = new GridBagConstraints();
			constraint.fill = GridBagConstraints.BOTH;
			constraint.weightx = 1.0;
			constraint.weighty = 1.0;

			// create a new table
			contentTable = new ExtendedJTable(false, false, false) {

				private static final long serialVersionUID = 1L;

				@Override
				public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
					/*
					 * Overwrite selection change event for the sheet table to disable partial
					 * selection changing with CTRL + click.
					 */
					if (toggle && !extend) {
						return;
					}
					super.changeSelection(rowIndex, columnIndex, toggle, extend);
				};
			};

			// ensure same background as JPanels in case of only few rows
			contentTable.setBackground(Colors.PANEL_BACKGROUND);

			contentTable.setGridColor(Colors.TAB_BORDER);
			contentTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			contentTable.setBorder(null);
			contentTable.getSelectionModel().addListSelectionListener(selectionListener);
			contentTable.getColumnModel().addColumnModelListener(columnSelectionListener);

			// only allow selection in case no preview is shown
			contentTable.setRowSelectionAllowed(!isShowingPreview);
			contentTable.setColumnSelectionAllowed(!isShowingPreview);
			contentTable.setCellSelectionEnabled(!isShowingPreview);

			contentTable.setShowPopupMenu(false);
			contentTable.setSortable(false);
			contentTable.getTableHeader().setReorderingAllowed(false);
			contentTable.setColoredTableCellRenderer(tableCellRenderer);
			contentTable.setEnabled(!isShowingPreview);

			// update table model
			contentTable.setModel(selectedModel);

			// update combo box content
			sheetComboBox.setModel(new DefaultComboBoxModel<String>(sheetNames));
			sheetComboBox.setSelectedIndex(sheetIndex);

			// add content table again
			centerPanel.removeAll();

			/*
			 * Hack to enlarge table columns in case of few columns. Add table to a full size JPanel
			 * and add the table header to the scroll pane.
			 */
			JPanel tablePanel = new JPanel(new BorderLayout());
			tablePanel.add(contentTable, BorderLayout.CENTER);

			JScrollPane scrollPane = new ExtendedJScrollPane(tablePanel);
			scrollPane.setColumnHeaderView(contentTable.getTableHeader());

			// Add scroll panel and table
			scrollPane.setRowHeaderView(new RowNumberTable(contentTable));
			scrollPane.setBorder(null);

			if (isShowingPreview) {
				// Create a layered pane to display both, the data table and a
				// "preview" overlay
				JLayeredPane layeredPane = new JLayeredPane();
				layeredPane.setLayout(new OverlayLayout(layeredPane));

				// add scroll pane
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

				centerPanel.add(layeredPane, constraint);
			} else {
				// display plain scroll pane
				centerPanel.add(scrollPane, constraint);
			}

			/*
			 * one last check whether the selected sheet is empty
			 */
			if (isSheetEmpty()) {
				showNotificationLabel("io.dataimport.step.excel.sheet_selection.empty_sheet");

				// even though the sheet is empty the user should still be able
				// to
				// select another sheet
				sheetComboBox.setEnabled(true);
			} else {
				// enable all header actions
				enableHeaderActions(true);
			}

			/*
			 * Show an information bubble in case the modal was loaded the first time and only a
			 * preview is shown.
			 */
			if (isShowingPreview && wasModelLoaded) {
				createBubbleWindow(cellRangeTextField, BubbleStyle.WARNING,
						"io.dataimport.step.excel.sheet_selection.showing_preview", XlsxUtilities.getSheetSelectionLength());
			}

			contentTable.revalidate();
			contentTable.repaint();
		}

		/**
		 * Uses the provided start and end ranges to configure the {@link ExcelSheetSelectionPanel#cellRangeTextField}.
		 *
		 * @param columnIndexStart
		 *            the 0-based column start index
		 * @param rowIndexStart
		 *            the 0-base row start index, {@link XlsxCellCoordinates#NO_ROW_NUMBER} means
		 *            the first start row is {@code 0}
		 * @param columnIndexEnd
		 *            the 0-based index that defines last column to import
		 * @param rowIndexEnd
		 *            the 0-based index that defines the last row to import
		 */
		private void updateCellRangeTextFields(int columnIndexStart, int rowIndexStart, int columnIndexEnd,
				int rowIndexEnd) {

			try {
				String startCell;
				if (rowIndexStart == XlsxCellCoordinates.NO_ROW_NUMBER
						&& rowIndexEnd == XlsxSheetMetaDataParser.MAXIMUM_XLSX_ROW_INDEX) {
					startCell = XlsxUtilities.convertToColumnName(columnIndexStart);
				} else if (rowIndexStart == XlsxCellCoordinates.NO_ROW_NUMBER) {
					startCell = XlsxUtilities.convertToColumnName(columnIndexStart) + "1";
				} else {
					startCell = XlsxUtilities.convertToColumnName(columnIndexStart) + (rowIndexStart + 1);
				}

				String endCell;
				if (rowIndexEnd == XlsxSheetMetaDataParser.MAXIMUM_XLSX_ROW_INDEX) {
					endCell = XlsxUtilities.convertToColumnName(columnIndexEnd);
				} else {
					endCell = XlsxUtilities.convertToColumnName(columnIndexEnd) + (rowIndexEnd + 1);
				}

				cellRangeTextField.setText(startCell + ":" + endCell);
			} catch (IllegalArgumentException e) {
				cellRangeTextField.setText("");
			}
		}

	};

	/** the model for the sheet selection defined in this step */
	private final ExcelSheetSelectionPanelModel sheetSelectionModel;

	/*
	 * UI elements
	 */
	private final JComboBox<String> sheetComboBox = new JComboBox<>();
	private final JPanel centerPanel = new JPanel(new GridBagLayout());

	private final JTextField cellRangeTextField = new JTextField();

	private final JCheckBox hasHeaderRowCheckBox = new JCheckBox(
			I18N.getGUILabel("io.dataimport.step.excel.sheet_selection.use_header_row"), true);
	private final JSpinner headerRowSpinner = new JSpinner();

	private ExtendedJTable contentTable = new ExtendedJTable();

	private BubbleWindow currentBubbleWindow;

	private JComponent bubbleOwner;

	/**
	 * Flag to avoid endless loop caused by e.g. the ComboBox listener and other listeners during UI
	 * update.
	 */
	private boolean updatingUI = false;

	private List<ChangeListener> changeListeners = new LinkedList<>();

	public ExcelSheetSelectionPanel(ExcelDataSource dataSource) {
		this.sheetSelectionModel = new ExcelSheetSelectionPanelModel(dataSource, sheetSelectionModelListener);

		setLayout(new BorderLayout());

		// create north panel
		{
			GridBagConstraints constraint = new GridBagConstraints();
			constraint.gridx = 0;
			constraint.insets = new Insets(0, 0, 0, 30);
			constraint.weightx = 0.0;
			constraint.fill = GridBagConstraints.NONE;

			JPanel northPanel = new JPanel(new GridBagLayout());
			northPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 10));

			// add sheet panel
			{
				GridBagConstraints innerGbc = new GridBagConstraints();
				innerGbc.insets = new Insets(5, 5, 5, 0);

				JPanel sheetPanel = new JPanel(new GridBagLayout());

				innerGbc.gridx = 0;
				sheetPanel.add(new ResourceLabel("io.dataimport.step.excel.sheet_selection.use_sheet"), innerGbc);

				innerGbc.gridx += 1;
				sheetPanel.add(sheetComboBox, innerGbc);
				sheetComboBox.addItemListener(comboBoxItemListener);

				northPanel.add(sheetPanel, constraint);
			}

			// cell range panel
			{
				GridBagConstraints innerGbc = new GridBagConstraints();
				innerGbc.insets = new Insets(5, 5, 5, 0);

				JPanel cellRangeColumnPanel = new JPanel(new GridBagLayout());

				innerGbc.gridx = 0;
				cellRangeColumnPanel.add(new ResourceLabel("io.dataimport.step.excel.sheet_selection.cells"), innerGbc);

				innerGbc.gridx += 1;
				cellRangeTextField.setMinimumSize(CELL_TEXTFIELD_MIN_DIMENSION);
				cellRangeTextField.setPreferredSize(CELL_TEXTFIELD_PREF_DIMENSION);
				cellRangeTextField.addActionListener(applySelectionAction);
				cellRangeTextField.getDocument().addUndoableEditListener(new UndoableEditListener() {

					@Override
					public void undoableEditHappened(UndoableEditEvent e) {
						if (!updatingUI) {
							// kill any possible error bubble window
							killCurrentBubbleWindow(null);

							// apply text field input
							applyTextFieldSelection(false);

							// remove all action listeners
							for (ActionListener l : CELL_RANGE_TIMER.getActionListeners()) {
								CELL_RANGE_TIMER.removeActionListener(l);
							}

							// add new action listener
							CELL_RANGE_TIMER.addActionListener(new ActionListener() {

								@Override
								public void actionPerformed(ActionEvent e) {
									applyTextFieldSelection(true);
								}
							});

							// restart timer (show an error after 1500 seconds if the input is
							// erroneous and the user does not enter any new text)
							CELL_RANGE_TIMER.setRepeats(false);
							CELL_RANGE_TIMER.restart();
						}
					}
				});
				cellRangeColumnPanel.add(cellRangeTextField, innerGbc);

				innerGbc.gridx += 1;
				cellRangeColumnPanel.add(new JButton(selectAllAction), innerGbc);

				constraint.gridx += 1;
				northPanel.add(cellRangeColumnPanel, constraint);
			}

			// header configuration
			{
				GridBagConstraints innerGbc = new GridBagConstraints();
				innerGbc.insets = new Insets(5, 5, 5, 0);

				JPanel headerConfigurationPanel = new JPanel(new GridBagLayout());

				hasHeaderRowCheckBox.setMinimumSize(CELL_CHECKBOX_MIN_DIMENSION);
				hasHeaderRowCheckBox.addActionListener(e -> {
					DataImportWizardUtils.logStats(DataWizardEventType.EXCEL_HEADER_ROW_STATE,
							Boolean.toString(hasHeaderRowCheckBox.isSelected()));
					headerRowSpinner.setEnabled(hasHeaderRowCheckBox.isSelected());

					if (!hasHeaderRowCheckBox.isSelected()) {
						sheetSelectionModel.setHeaderRowIndex(ResultSetAdapter.NO_END_ROW);
					} else {
						sheetSelectionModel.setHeaderRowIndex(getHeaderRowIndexFromSpinner());
					}
				});
				innerGbc.gridx = 0;
				headerConfigurationPanel.add(hasHeaderRowCheckBox, innerGbc);

				headerRowSpinner.setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
				headerRowSpinner.addChangeListener(e -> sheetSelectionModel.setHeaderRowIndex(getHeaderRowIndexFromSpinner()));
				innerGbc.gridx += 1;
				headerConfigurationPanel.add(headerRowSpinner, innerGbc);

				constraint.gridx += 1;
				northPanel.add(headerConfigurationPanel, constraint);
			}

			constraint.weightx = 1.0;
			constraint.fill = GridBagConstraints.HORIZONTAL;
			constraint.gridx += 1;
			northPanel.add(new JLabel(), constraint);

			add(northPanel, BorderLayout.NORTH);
		}

		// add center panel
		add(centerPanel, BorderLayout.CENTER);

		// initialize the model
		sheetSelectionModel.setSheetIndex(0);

	}

	private void applyTextFieldSelection(boolean showBubbleOnError) {
		try {

			Pair<XlsxCellCoordinates, XlsxCellCoordinates> coordinates = getCoordinates();
			XlsxCellCoordinates fromCoordinates = coordinates.getFirst();
			XlsxCellCoordinates toCoordinates = coordinates.getSecond();

			boolean errorDetected = false;
			if (fromCoordinates.columnNumber > toCoordinates.columnNumber) {
				sheetSelectionModel.setCellRangeSelection(null);
				if (showBubbleOnError) {
					createBubbleWindow(cellRangeTextField, BubbleStyle.ERROR,
							"io.dataimport.step.excel.sheet_selection.invalid_range");
				}
				errorDetected = true;
			} else if (fromCoordinates.rowNumber > toCoordinates.rowNumber) {
				sheetSelectionModel.setCellRangeSelection(null);
				if (showBubbleOnError) {
					createBubbleWindow(cellRangeTextField, BubbleStyle.ERROR,
							"io.dataimport.step.excel.sheet_selection.invalid_range");
				}
				errorDetected = true;
			} else if (fromCoordinates.columnNumber >= contentTable.getColumnCount()) {
				sheetSelectionModel.setCellRangeSelection(null);
				if (showBubbleOnError) {
					createBubbleWindow(cellRangeTextField, BubbleStyle.ERROR,
							"io.dataimport.step.excel.sheet_selection.invalid_range");
				}
				errorDetected = true;
			}

			if (!errorDetected) {
				// create new selection object
				CellRangeSelection newSelection = new CellRangeSelection(fromCoordinates.columnNumber,
						fromCoordinates.rowNumber, toCoordinates.columnNumber, toCoordinates.rowNumber);

				// update the model (will trigger an UI update)
				sheetSelectionModel.setCellRangeSelection(newSelection);
			} else {
				sheetSelectionModel.setCellRangeSelection(null);
			}

		} catch (InvalidConfigurationException e1) {
			sheetSelectionModel.setCellRangeSelection(null);
			if (showBubbleOnError) {
				createBubbleWindow(cellRangeTextField, BubbleStyle.ERROR,
						"io.dataimport.step.excel.sheet_selection.wrong_cell_format", cellRangeTextField.getText());
			}
		} finally {
			// stop timer
			CELL_RANGE_TIMER.stop();
		}
	}

	private Pair<XlsxCellCoordinates, XlsxCellCoordinates> getCoordinates() throws InvalidConfigurationException {
		XlsxCellCoordinates fromCoordinates = getFromCellCoordinates();
		XlsxCellCoordinates toCoordinates = getToCellCoordinates();

		return new Pair<>(fromCoordinates, toCoordinates);
	}

	private XlsxCellCoordinates getFromCellCoordinates() throws InvalidConfigurationException {
		try {
			return XlsxUtilities.convertCellRefToCoordinates(getCellRangeArguments()[0]);
		} catch (IllegalArgumentException e) {
			throw new InvalidConfigurationException();
		}
	}

	private XlsxCellCoordinates getToCellCoordinates() throws InvalidConfigurationException {
		try {
			XlsxCellCoordinates toCoordinates = XlsxUtilities.convertCellRefToCoordinates(getCellRangeArguments()[1]);

			// replace no row number flag by all rows flag
			if (toCoordinates.rowNumber == XlsxCellCoordinates.NO_ROW_NUMBER) {
				toCoordinates.rowNumber = XlsxSheetMetaDataParser.MAXIMUM_XLSX_ROW_INDEX;
			}
			return toCoordinates;
		} catch (IllegalArgumentException e) {
			throw new InvalidConfigurationException();
		}
	}

	private String[] getCellRangeArguments() throws InvalidConfigurationException {
		String[] cellRanges = cellRangeTextField.getText().split(":");
		if (cellRanges.length != 2) {
			throw new InvalidConfigurationException();
		}
		return cellRanges;
	}

	/**
	 * @return the header row index selected by the header row spinner. It is always one value below
	 *         the shown line number.
	 */
	private int getHeaderRowIndexFromSpinner() {
		return Integer.parseInt(headerRowSpinner.getValue().toString()) - 1;
	}

	/**
	 *
	 * Configures the UI according to the configuration of the provided {@link ExcelDataSource}.
	 *
	 * @param excelDataSource
	 *            the {@link ExcelDataSource} that should be used to configure the UI
	 */
	void configureSheetSelectionModel(final ExcelDataSource excelDataSource) {
		updateModel(excelDataSource.getResultSetConfiguration().getSheet(), excelDataSource.getHeaderRowIndex(),
				new CellRangeSelection(excelDataSource.getResultSetConfiguration()));
	}

	/**
	 * Kills the current error bubble.
	 *
	 * @param owner
	 *            the owner of the error, if the actual bubble owner is different this method won't
	 *            do anything. If the given owner is {@code null} the bubble will be forcefully
	 *            killed without respect to any owner.
	 */
	void killCurrentBubbleWindow(JComponent owner) {
		if (currentBubbleWindow != null && (owner == null || owner == bubbleOwner)) {
			currentBubbleWindow.killBubble(true);
			currentBubbleWindow = null;
			bubbleOwner = null;
		}
	}

	/**
	 * Creates a bubble for the component and kills other bubbles.
	 *
	 * @param component
	 *            the component for which to show the bubble
	 * @param style
	 *            the bubble style
	 * @param i18n
	 *            the i18n key
	 * @param arguments
	 *            arguments for the i18n
	 */
	private void createBubbleWindow(JComponent component, BubbleStyle style, String i18n, Object... arguments) {
		// BubbleWindow requires a visible owner
		if (!component.isShowing()) {
			return;
		}
		killCurrentBubbleWindow(null);
		bubbleOwner = component;
		JButton okayButton = new JButton(I18N.getGUILabel("io.dataimport.step.excel.sheet_selection.got_it"));
		final ComponentBubbleWindow errorWindow = new ComponentBubbleWindow(component, style,
				SwingUtilities.getWindowAncestor(ExcelSheetSelectionPanel.this), AlignedSide.TOP, i18n, null, null, false,
				true, new JButton[] { okayButton }, arguments);
		okayButton.addActionListener(e -> errorWindow.killBubble(false));

		// show and remember error window
		errorWindow.setVisible(true);
		currentBubbleWindow = errorWindow;
	};

	/**
	 * Shows a "header row behind start row"-bubble
	 */
	void notifyHeaderRowBehindStartRow() throws InvalidConfigurationException {
		createBubbleWindow(headerRowSpinner, BubbleStyle.ERROR,
				"io.dataimport.step.csv.format_specification.invalid_header_row", getHeaderRowIndex() + 1,
				getSelection().getRowIndexStart() + 1);
	}

	/**
	 * Shows a "header row not found"-bubble
	 */
	void notifyHeaderRowNotFound() {
		createBubbleWindow(headerRowSpinner, BubbleStyle.ERROR,
				"io.dataimport.step.csv.format_specification.header_row_not_found");
	}

	/**
	 * Shows a "no rows left"-bubble
	 */
	void notifyNoRowsLeft() {
		createBubbleWindow(headerRowSpinner, BubbleStyle.ERROR,
				"io.dataimport.step.excel.format_specification.no_rows_left");
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

		// disable all header actions
		enableHeaderActions(false);
	}

	/**
	 * Allows to change whether all header actions should be enabled or disabled.
	 *
	 * @param enabled
	 *            whether the header actions should be enabled
	 */
	private void enableHeaderActions(boolean enabled) {
		sheetComboBox.setEnabled(enabled);
		hasHeaderRowCheckBox.setEnabled(enabled);
		headerRowSpinner.setEnabled(enabled);
		applySelectionAction.setEnabled(enabled);
		cellRangeTextField.setEnabled(enabled);
		selectAllAction.setEnabled(enabled);
	}

	/**
	 * Updates the cell selection model with the provided new sheetIndex, headerRowIndex, and
	 * {@link CellRangeSelection}.
	 *
	 * @param sheetIndex
	 *            the new sheetIndex
	 * @param headerRowIndex
	 *            the new headerRowIndex
	 * @param selection
	 *            the new cell range selection
	 */
	private void updateModel(int sheetIndex, int headerRowIndex, CellRangeSelection selection) {
		sheetSelectionModel.updateModel(sheetIndex, headerRowIndex, selection);
	}

	/**
	 * @return the user selection of the range to be imported.
	 * @throws InvalidConfigurationException
	 *             in case the current selection is invalid
	 */
	CellRangeSelection getSelection() throws InvalidConfigurationException {
		CellRangeSelection selection = sheetSelectionModel.getCellRangeSelection();
		if (selection == null) {
			throw new InvalidConfigurationException();
		}
		return selection;
	}

	/**
	 * @return whether the current selected sheet is empty
	 */
	boolean isSheetEmpty() {
		return contentTable.getModel().getColumnCount() <= 0;
	}

	/**
	 * @return whether the current selection is empty
	 */
	boolean isSelectionEmpty() {
		return cellRangeTextField.getText().trim().isEmpty();
	}

	/**
	 * @return wether the UI of the sheet selection panel is currently updated
	 */
	boolean isUpdatingUI() {
		return updatingUI;
	}

	/**
	 * Returns the index of the header row
	 *
	 * @return the index of the header row or {@link ResultSetAdapter#NO_HEADER_ROW} in case the
	 *         user unchecks the {@link #hasHeaderRowCheckBox}.
	 */
	int getHeaderRowIndex() {
		return sheetSelectionModel.getHeaderRowIndex();
	}

	/**
	 * Returns the selected sheet index
	 *
	 * @return the index of the selected sheet
	 */
	public int getSheetIndex() {
		return sheetSelectionModel.getSheetIndex();
	}

	/**
	 * Clears the model cache.
	 */
	void clearCache() {
		sheetSelectionModel.clearTableModelCache();
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
	 * Cancels the loading and removes the bubble window
	 */
	void tearDown(){
		SwingTools.invokeLater(() -> enableHeaderActions(false));
		sheetSelectionModel.cancelLoading();
		// ensure error bubble has been killed when leaving the step
		killCurrentBubbleWindow(null);
	}

}
