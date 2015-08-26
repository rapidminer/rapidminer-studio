/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTabbedPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.io.ExcelExampleSource;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import jxl.Workbook;
import jxl.read.biff.BiffException;


/**
 * 
 * @author Tobias Malbrecht
 */
public class ExcelWorkbookPane extends JPanel {

	public class ExcelWorkbookSelection {

		private int sheetIndex;
		private int columnIndexStart;
		private int rowIndexStart;
		private int columnIndexEnd;
		private int rowIndexEnd;

		public ExcelWorkbookSelection(int sheetIndex, int columnIndexStart, int rowIndexStart, int columnIndexEnd,
				int rowIndexEnd) {
			this.sheetIndex = sheetIndex;
			this.columnIndexStart = columnIndexStart;
			this.rowIndexStart = rowIndexStart;
			this.columnIndexEnd = columnIndexEnd;
			this.rowIndexEnd = rowIndexEnd;
		}

		@Override
		public String toString() {
			return sheetIndex + ": " + columnIndexStart + ":" + rowIndexStart + " - " + columnIndexEnd + ":" + rowIndexEnd;
		}

		public int getSheetIndex() {
			return sheetIndex;
		}

		public int getColumnIndexEnd() {
			return columnIndexEnd;
		}

		public int getColumnIndexStart() {
			return columnIndexStart;
		}

		public int getRowIndexEnd() {
			return rowIndexEnd;
		}

		public int getRowIndexStart() {
			return rowIndexStart;
		}

		public int getSelectionWidth() {
			return columnIndexEnd - columnIndexStart + 1;
		}

		public int getSelectionHeight() {
			return rowIndexEnd - rowIndexStart + 1;
		}

		public Map<Integer, String> getAnnotationMap() {
			return ((ExcelSheetModel) tables[sheetIndex].getModel()).getAnnotationMap();
		}
	}

	private static final long serialVersionUID = 9179757216097316344L;

	private Workbook excelWorkbook;
	private ExtendedJTabbedPane sheetsPane;
	private ExtendedJTable[] tables;
	private ExcelWorkbookSelection selectedView;

	private WizardStep wizardStep;

	private ExcelExampleSource reader;

	public ExcelWorkbookPane(WizardStep wizardStep, ExcelExampleSource reader) {
		super();
		this.wizardStep = wizardStep;
		this.reader = reader;
		sheetsPane = new ExtendedJTabbedPane();
		sheetsPane.setBorder(null);
		this.setLayout(new BorderLayout());
		this.add(sheetsPane);
	}

	// public ExcelWorkbookPane(String fileName, ExcelExampleSource reader) {
	// this((WizardStep)null, reader);
	// loadWorkbook(fileName);
	// }
	//
	// public ExcelWorkbookPane(File file, ExcelExampleSource reader) {
	// this((WizardStep)null, reader);
	// loadWorkbook(file);
	// }
	//
	// public void loadWorkbook(String fileName) {
	// loadWorkbook(new File(fileName));
	// }

	// public void loadWorkbook(File file) {
	// Workbook workbook = null;
	// try {
	// workbook = Workbook.getWorkbook(file);
	// } catch (Exception e) {
	// // TODO correct error handling
	// LogService.getRoot().log(Level.WARNING, "Error loading workbook: "+e, e);
	// }
	// excelWorkbook = workbook;
	// loadWorkbook();
	// }

	public ExcelWorkbookSelection getSelection() {
		if (selectedView == null) {
			int sheetIndex = sheetsPane.getSelectedIndex();
			int columnIndexStart = tables[sheetIndex].getSelectedColumn();
			int rowIndexStart = tables[sheetIndex].getSelectedRow();
			int columnIndexEnd = columnIndexStart + tables[sheetIndex].getSelectedColumnCount() - 1;
			int rowIndexEnd = rowIndexStart + tables[sheetIndex].getSelectedRowCount() - 1;
			if (columnIndexStart == -1) {
				// then use complete selected table
				return new ExcelWorkbookSelection(sheetIndex, 0, 0, tables[sheetIndex].getColumnCount() - 1,
						tables[sheetIndex].getRowCount() - 1);
			} else {
				return new ExcelWorkbookSelection(sheetIndex, columnIndexStart, rowIndexStart, columnIndexEnd, rowIndexEnd);
			}
		} else {
			int sheetIndex = selectedView.getSheetIndex();
			int columnIndexStart = tables[0].getSelectedColumn() + selectedView.getColumnIndexStart();
			int rowIndexStart = tables[0].getSelectedRow() + selectedView.getRowIndexStart();
			int columnIndexEnd = columnIndexStart + tables[0].getSelectedColumnCount() - 1
					+ selectedView.getColumnIndexStart();
			int rowIndexEnd = rowIndexStart + tables[0].getSelectedRowCount() - 1 + selectedView.getRowIndexStart();
			if (columnIndexStart == -1) {
				// then use complete selected table
				return new ExcelWorkbookSelection(sheetIndex, selectedView.getColumnIndexStart(),
						selectedView.getRowIndexStart(), selectedView.getColumnIndexEnd(), selectedView.getRowIndexEnd());
			} else {
				return new ExcelWorkbookSelection(sheetIndex, columnIndexStart, rowIndexStart, columnIndexEnd, rowIndexEnd);
			}
		}
	}

	// public void createView(ExcelWorkbookSelection selection) {
	// createView(selection, null);
	// }

	// public void createView(ExcelWorkbookSelection selection, List<String>
	// columnNames) {
	// sheetsPane.removeAll();
	// tables = new ExtendedJTable[1];
	// int sheetIndex = selection.getSheetIndex();
	// Sheet sheet = excelWorkbook.getSheet(sheetIndex);
	// ExcelSheetModel sheetModel = new ExcelSheetModel();
	// sheetModel.createView(selection);
	// sheetModel.setNames(columnNames);
	// tables[0] = new ExtendedJTable(sheetModel, false, false);
	// tables[0].setBorder(null);
	// tables[0].setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	// tables[0].getColumnModel().getColumn(0).setCellEditor(new
	// AnnotationCellEditor());
	// /*
	// * tables[0].setMaximumSize(new Dimension(5000, 50000));
	// * TableColumnModel columnModel = tables[0].getColumnModel(); for (int
	// * columnIndex = 0; columnIndex < tables[0].getColumnCount();
	// * columnIndex++) {
	// * columnModel.getColumn(columnIndex).setPreferredWidth(
	// * sheet.getColumnView(columnIndex).getSize() / 36); }
	// */
	// // tables[sheetIndex].doLayout();
	//
	// ExtendedJScrollPane pane = new ExtendedJScrollPane(tables[0]);
	// pane.setBorder(null);
	// sheetsPane.addTab(sheet.getName(), pane);
	// selectedView = selection;
	// }
	//
	// public void resetView() {
	// selectedView = null;
	// loadWorkbook();
	// }

	// public Workbook getWorkbook() {
	// return excelWorkbook;
	// }

	public void loadWorkbook() {
		File file = null;
		try {
			file = reader.getParameterAsFile(ExcelExampleSource.PARAMETER_EXCEL_FILE);
		} catch (UndefinedParameterError e1) {
			throw new RuntimeException("Error during loading workbook: ", e1);
		} catch (UserError e1) {
			throw new RuntimeException("Error during loading workbook: ", e1);
		}

		try {
			excelWorkbook = Workbook.getWorkbook(file);
		} catch (BiffException e1) {
			// LogService.getRoot().log(Level.WARNING, "Error loading workbook: " + e1, e1);
			LogService
					.getRoot()
					.log(Level.WARNING,
							I18N.getMessage(
									LogService.getRoot().getResourceBundle(),
									"com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel.ExcelWorkbookPane.loading_workbook_error",
									e1), e1);

			return;
		} catch (IOException e1) {
			// LogService.getRoot().log(Level.WARNING, "Error loading workbook: " + e1, e1);
			LogService
					.getRoot()
					.log(Level.WARNING,
							I18N.getMessage(
									LogService.getRoot().getResourceBundle(),
									"com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel.ExcelWorkbookPane.loading_workbook_error",
									e1), e1);
			return;
		}
		sheetsPane.removeAll();
		// add dummy
		JPanel dummy = new JPanel();
		dummy.add(new JLabel("Loading Excel Sheets"));
		sheetsPane.addTab("  ", dummy);
		tables = new ExtendedJTable[excelWorkbook.getNumberOfSheets()];

		new ProgressThread("load_workbook") {

			@Override
			public void run() {
				String[] sheetNames = excelWorkbook.getSheetNames();
				for (int sheetIndex = 0; sheetIndex < excelWorkbook.getNumberOfSheets(); sheetIndex++) {
					// set up the reader to read the right sheet.
					synchronized (reader) {
						reader.setParameter(ExcelExampleSource.PARAMETER_SHEET_NUMBER, Integer.toString(sheetIndex + 1));
						reader.clearReaderSettings();
						List<Object[]> data = null;
						try {
							data = reader.getShortPreviewAsList(getProgressListener(), true);
						} catch (OperatorException e1) {
							// if the loaded sheet is empty an exception is thrown here.
							// create a empty data object
							data = new LinkedList<Object[]>();
							data.add(new Object[] {});
						}
						ExcelSheetModel sheetModel = new ExcelSheetModel(data);

						// Sheet sheet = excelWorkbook.getSheet(sheetIndex);
						// TableModel sheetModel = new ExcelTableModel(sheet); // OLD

						sheetModel.addTableModelListener(new TableModelListener() {

							@Override
							public void tableChanged(TableModelEvent e) {
								if (wizardStep != null) {
									wizardStep.fireStateChanged();
								}
							}
						});
						tables[sheetIndex] = new ExtendedJTable(sheetModel, false, false) {

							private static final long serialVersionUID = 1L;

							@Override
							public TableCellRenderer getCellRenderer(int row, int col) {
								if (col > 0) {
									return super.getCellRenderer(row, col);
								} else {
									return new DefaultTableCellRenderer() {

										private static final long serialVersionUID = 2791054497317720420L;

										@Override
										public Component getTableCellRendererComponent(JTable table, Object value,
												boolean isSelected, boolean hasFocus, int row, int column) {
											Component component = super.getTableCellRendererComponent(table, value,
													isSelected, hasFocus, row, column);
											component.setBackground(ExcelWorkbookPane.this.getBackground());
											component.setForeground(Color.BLACK);
											return component;
										};
									};
								}
							};
						};
						tables[sheetIndex].setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
						tables[sheetIndex].setBorder(null);

						tables[sheetIndex].getColumnModel().getColumn(0)
								.setCellEditor(new AnnotationCellEditor(ExcelWorkbookPane.this.getBackground()));

						// momentary disable selection in tables
						tables[sheetIndex].setRowSelectionAllowed(false);
						tables[sheetIndex].setColumnSelectionAllowed(false);
						tables[sheetIndex].setCellSelectionEnabled(false);

						// tables[sheetIndex].pack();
						ExtendedJScrollPane pane = new ExtendedJScrollPane(tables[sheetIndex]);
						pane.setBorder(null);
						if (sheetIndex == 0) {
							sheetsPane.removeAll();
						}
						sheetsPane.addTab(sheetNames[sheetIndex], pane);
					}
				}

			}
		}.start();

	}

	public String getColumnName(int sheet, int index) {
		return tables[sheet].getColumnName(index);
	}

	// public boolean displayErrorStatus(JLabel label) {
	// if (tables == null) {
	// return true;
	// }
	// Set<String> usedAnnotations = new HashSet<String>();
	// for (String an : ((ExcelSheetModel)
	// tables[getSelection().getSheetIndex()].getModel()).getAnnotationMap().values()) {
	// if (AnnotationCellEditor.NONE.equals(an)) {
	// continue;
	// }
	// if (usedAnnotations.contains(an)) {
	// label.setText("Duplicate annotation: " + an);
	// return false;
	// } else {
	// usedAnnotations.add(an);
	// }
	// }
	// label.setText("");
	// return true;
	// }

	private class ExcelSheetModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private final Map<Integer, String> annotationTypes = new HashMap<Integer, String>();

		// private ExcelWorkbookSelection reductionSelection;

		private ArrayList<Object[]> data = null;

		public ExcelSheetModel(List<Object[]> data) {
			// read former annotations if exists
			try {
				for (String[] pair : reader.getParameterList(ExcelExampleSource.PARAMETER_ANNOTATIONS)) {
					try {
						final int row = Integer.parseInt(pair[0]);
						annotationTypes.put(row, pair[1]);
					} catch (NumberFormatException e) {
						throw new OperatorException("row_number entries in parameter list "
								+ ExcelExampleSource.PARAMETER_ANNOTATIONS + " must be integers.", e);
					}
				}
			} catch (UndefinedParameterError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OperatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			this.data = new ArrayList<Object[]>(data);
			;
		}

		// private void setData(List<Object[]> data) {
		// this.data = new ArrayList<Object[]>(data);
		// }

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				if (AnnotationCellEditor.NONE.equals(aValue)) {
					getAnnotationMap().remove(rowIndex);
				} else {
					getAnnotationMap().put(rowIndex, (String) aValue);
				}
				fireTableCellUpdated(rowIndex, columnIndex);
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				String value = getAnnotationMap().get(rowIndex);
				if (value == null) {
					return AnnotationCellEditor.NONE;
				} else {
					return value;
				}
			}
			columnIndex--;

			if (rowIndex >= data.size()) {
				return "";
			}
			if (columnIndex >= data.get(rowIndex).length) {
				return "";
			}
			return data.get(rowIndex)[columnIndex];
		}

		@Override
		public int getRowCount() {
			return data.size();
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) {
				return "Use as";
			}
			if (columnIndex == 1) {
				return "Row No.";
			}
			return Tools.getExcelColumnName(columnIndex - 2);
		}

		@Override
		public int getColumnCount() {
			if (data.isEmpty()) {
				return 1;
			}
			return data.get(0).length + 1;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 0;
		}

		public Map<Integer, String> getAnnotationMap() {
			return annotationTypes;
		}

	}
}
