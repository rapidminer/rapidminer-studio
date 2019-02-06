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
package com.rapidminer.gui.wizards;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.gui.EditorCellRenderer;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.att.AttributeDataSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


/**
 * This table shows only the attribute names and the attribute value types.
 * 
 * @author Ingo Mierswa
 */
public class ExampleSourceConfigurationWizardValueTypeTable extends ExtendedJTable {

	private static final long serialVersionUID = -6402806364622312588L;

	private static class ExampleSourceConfigurationWizardValueTypeTableModel extends AbstractTableModel {

		private static final long serialVersionUID = -8459288119418286682L;

		private List<AttributeDataSource> sources;

		public ExampleSourceConfigurationWizardValueTypeTableModel(List<AttributeDataSource> sources) {
			this.sources = sources;
		}

		public void guessValueTypes(File originalDataFile, String commentString, String columnSeparators,
				char decimalPointCharacter, boolean useQuotes, boolean firstLineAsNames) {
			Pattern separatorPattern = Pattern.compile(columnSeparators);
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(originalDataFile));
				String line = null;
				boolean first = true;
				boolean[] hasToCheck = null;
				boolean[] onlyMissing = null;
				int rowCounter = 1;
				while ((line = in.readLine()) != null) {
					if ((commentString != null) && (commentString.trim().length() > 0) && (line.startsWith(commentString))) {
						continue;
					}
					if (line.trim().length() == 0) {
						continue;
					}

					String[] row = null;
					if (useQuotes) {
						row = Tools.quotedSplit(line, separatorPattern);
					} else {
						row = line.trim().split(columnSeparators);
					}

					if (first) {
						hasToCheck = new boolean[row.length];
						onlyMissing = new boolean[row.length];
						for (int i = 0; i < hasToCheck.length; i++) {
							onlyMissing[i] = hasToCheck[i] = true;
						}

						if (!firstLineAsNames) {
							updateValueTypes(row, hasToCheck, onlyMissing, decimalPointCharacter);
						}

						first = false;
					} else {
						if (row.length != hasToCheck.length) {
							throw new IOException("Line " + rowCounter + " has a number of columns (" + row.length
									+ ") different from preceding lines (" + hasToCheck.length + ").");
						}
						updateValueTypes(row, hasToCheck, onlyMissing, decimalPointCharacter);
					}

					rowCounter++;
				}

				// set value types of columns with only missing values to nominal
				for (int m = 0; m < onlyMissing.length; m++) {
					if (onlyMissing[m]) {
						setValueAt(Ontology.VALUE_TYPE_NAMES[Ontology.NOMINAL], 0, m);
					}
				}

			} catch (IOException e) {
				SwingTools.showSimpleErrorMessage("cannot_guess_value_types", e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						SwingTools.showSimpleErrorMessage("cannot_close_stream_to_data_file", e);
					}
				}
			}
		}

		private void updateValueTypes(String[] row, boolean[] hasToCheck, boolean[] onlyMissing, char decimalPointCharacter) {
			for (int c = 0; c < row.length; c++) {
				if (hasToCheck[c]) {
					int valueType = Ontology.INTEGER;
					String value = row[c];

					if ((value != null) && (value.length() > 0) && (!value.equals("?"))) {
						onlyMissing[c] = false;
						try {
							String decimalValue = value.replace(decimalPointCharacter, '.');
							double d = Double.parseDouble(decimalValue);
							if ((valueType == Ontology.INTEGER) && (!Tools.isEqual(Math.round(d), d))) {
								valueType = Ontology.REAL;
								hasToCheck[c] = false;
							}
						} catch (NumberFormatException e) {
							valueType = Ontology.NOMINAL;
							hasToCheck[c] = false;
						}
					}
					setValueAt(Ontology.VALUE_TYPE_NAMES[valueType], 0, c);
				}
			}
		}

		@Override
		public int getColumnCount() {
			return sources.size();
		}

		@Override
		public int getRowCount() {
			return 1;
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			String valueTypeName = (String) value;
			int valueType = Ontology.NOMINAL;
			for (int i = 0; i < Ontology.VALUE_TYPE_NAMES.length; i++) {
				if (Ontology.VALUE_TYPE_NAMES[i].equals(valueTypeName)) {
					valueType = i;
					break;
				}
			}
			AttributeDataSource source = sources.get(columnIndex);
			Attribute oldAttribute = source.getAttribute();
			source.setAttribute(AttributeFactory.changeValueType(oldAttribute, valueType));
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return Ontology.VALUE_TYPE_NAMES[sources.get(columnIndex).getAttribute().getValueType()];
		}

		@Override
		public String getColumnName(int column) {
			return sources.get(column).getAttribute().getName();
		}
	}

	private ExampleSourceConfigurationWizardValueTypeTableModel model;

	public ExampleSourceConfigurationWizardValueTypeTable(List<AttributeDataSource> sources) {
		super(false);
		setAutoResizeMode(AUTO_RESIZE_OFF);
		this.model = new ExampleSourceConfigurationWizardValueTypeTableModel(sources);
		setModel(model);
		update();
	}

	public void guessValueTypes(File data, String commentString, String columnSeparators, char decimalPointCharacter,
			boolean useQuotes, boolean firstLineAsNames) {
		this.model
				.guessValueTypes(data, commentString, columnSeparators, decimalPointCharacter, useQuotes, firstLineAsNames);
	}

	public void update() {
		((AbstractTableModel) getModel()).fireTableStructureChanged();
		TableColumnModel columnModel = getColumnModel();
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			TableColumn tableColumn = columnModel.getColumn(i);
			tableColumn.setPreferredWidth(120);
		}
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return true;
	}

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		List<String> usedTypes = new LinkedList<String>();
		for (int i = 0; i < Ontology.VALUE_TYPE_NAMES.length; i++) {
			if ((i != Ontology.ATTRIBUTE_VALUE) && (i != Ontology.FILE_PATH)
					&& (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(i, Ontology.DATE_TIME))) {
				usedTypes.add(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(i));
			}
		}
		String[] valueTypes = new String[usedTypes.size()];
		int vCounter = 0;
		for (String type : usedTypes) {
			valueTypes[vCounter++] = type;
		}
		JComboBox<String> typeBox = new JComboBox<>(valueTypes);
		typeBox.setBackground(javax.swing.UIManager.getColor("Table.cellBackground"));
		return new DefaultCellEditor(typeBox);
	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		return new EditorCellRenderer(getCellEditor(row, column));
	}
}
