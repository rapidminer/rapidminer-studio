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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import com.rapidminer.example.set.CustomFilter.CustomFilters;
import com.rapidminer.gui.properties.tablepanel.TablePanel;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellType;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeComboBox;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeDate;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeDateTime;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeLabel;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeRegex;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldDefault;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldInteger;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldNumerical;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldTime;
import com.rapidminer.gui.properties.tablepanel.model.TablePanelModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * This is the backing model for the {@link TablePanel} used by the example set filters.
 *
 * @author Marco Boeck
 *
 */
public class FilterTableModel extends AbstractTableModel implements TablePanelModel {

	private static final long serialVersionUID = 7919277746196444952L;

	private static final int COLUMN_COUNT = 3;
	private static final int COLUMN_ATTRIBUTES_INDEX = 0;
	private static final int COLUMN_CUSTOM_FILTER_INDEX = 1;
	private static final int COLUMN_FILTER_VALUE_INDEX = 2;

	private static final String[] COLUMN_NAMES = new String[] {
		I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.filter_table_model.column_name_attribute.title"),
		I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.filter_table_model.column_name_compare.title"),
		I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.filter_table_model.column_name_field.title") };

	/** syntax help for date fields */
	private static final String SYNTAX_DATE = "01/31/2014";

	/** syntax help for date_time fields */
	private static final String SYNTAX_DATE_TIME = "01/31/2014 9:00:00 AM";

	/** syntax help for time fields */
	private static final String SYNTAX_TIME = "9:00:00 AM";

	/** the maximal number of nominal values returned as possible values from {@link #getPossibleValuesForCellOrNull} */
	private static final int MAX_NOMINAL_VALUES = 100;

	/** the metadata for the example set at the input port */
	private ExampleSetMetaData md;

	/** flag which indicates if possible values for filter comparators should depend on meta data */
	private boolean checkMetaDataForComparators;

	/** the number of rows this table model currently has */
	private int rowsCount;

	/** the list of AttributeMetaData for this model */
	private List<AttributeMetaData> listOfAttributeMetaData;

	/** the list of selected values for the attribute column */
	private List<String> rowListOfSelectedAttributes;

	/** the list of selected values for the comparator column */
	private List<String> rowListOfSelectedComparators;

	/** the list of selected values for the value column */
	private List<String> rowListOfSelectedValues;

	/** used to prevent events while we are creating the model */
	private AtomicBoolean creatingModel;

	/** the format for date_time */
	private final DateFormat FORMAT_DATE_TIME = new SimpleDateFormat(CustomFilters.DATE_TIME_FORMAT_STRING, Locale.ENGLISH);

	/** the format for date */
	private final DateFormat FORMAT_DATE = new SimpleDateFormat(CustomFilters.DATE_FORMAT_STRING, Locale.ENGLISH);

	/** the format for time */
	private final DateFormat FORMAT_TIME = new SimpleDateFormat(CustomFilters.TIME_FORMAT_STRING, Locale.ENGLISH);

	/**
	 * Creates a new {@link FilterTableModel} instance.
	 *
	 * @param inputPort
	 * @throws IllegalArgumentException
	 *             if the input port has no example set
	 */
	public FilterTableModel(InputPort inputPort) throws IllegalArgumentException {
		if (inputPort == null) {
			throw new IllegalArgumentException("InputPort must not be null!");
		}

		this.listOfAttributeMetaData = new LinkedList<>();
		if (inputPort.getMetaData() instanceof ExampleSetMetaData) {
			this.md = (ExampleSetMetaData) inputPort.getMetaData();
			for (AttributeMetaData metadata : md.getAllAttributes()) {
				listOfAttributeMetaData.add(metadata);
			}
		}
		this.rowsCount = 0;
		this.rowListOfSelectedAttributes = new LinkedList<>();
		this.rowListOfSelectedComparators = new LinkedList<>();
		this.rowListOfSelectedValues = new LinkedList<>();
		this.creatingModel = new AtomicBoolean(false);
		this.checkMetaDataForComparators = true;
	}

	@Override
	public int getRowCount() {
		return rowsCount;
	}

	@Override
	public int getColumnCount() {
		return COLUMN_COUNT;
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex < 0 || columnIndex >= getColumnCount()) {
			throw new IllegalArgumentException("Invalid columnIndex: " + columnIndex);
		}
		return COLUMN_NAMES[columnIndex];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return getColumnClass(-1, columnIndex);
	}

	@Override
	public Class<? extends CellType> getColumnClass(int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case COLUMN_ATTRIBUTES_INDEX:
				return CellTypeComboBox.class;
			case COLUMN_CUSTOM_FILTER_INDEX:
				return CellTypeComboBox.class;
			case COLUMN_FILTER_VALUE_INDEX:
				if (rowIndex == -1) {
					return CellTypeLabel.class;
				}
				CustomFilters filter = CustomFilters.getByLabel(String.valueOf(getValueAt(rowIndex,
						COLUMN_CUSTOM_FILTER_INDEX)));
				if (filter == CustomFilters.REGEX) {
					return CellTypeRegex.class;
				} else if (filter == CustomFilters.MISSING || filter == CustomFilters.NOT_MISSING) {
					// missing/not missing filter -> no date cell type
					return CellTypeTextFieldDefault.class;
				} else if (filter == null || filter.isNominalFilter()) {
					return CellTypeTextFieldDefault.class;
				} else {
					// distinguish between numerical and date attributes
					String selectedAttributeName = String.valueOf(getValueAt(rowIndex, COLUMN_ATTRIBUTES_INDEX));
					int valueType = Ontology.ATTRIBUTE_VALUE;
					// see if the attribute has been set to an existing one so the value type can be
					// obtained
					for (AttributeMetaData md : listOfAttributeMetaData) {
						if (md.getName().equals(selectedAttributeName)) {
							valueType = md.getValueType();
							break;
						}
					}
					// this happens when the meta data is unknown - use String.class for generic
					// textfield
					if (valueType == Ontology.ATTRIBUTE_VALUE) {
						return CellTypeTextFieldDefault.class;
					}
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE)) {
						return CellTypeDate.class;
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.TIME)) {
						return CellTypeTextFieldTime.class;
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME)) {
						return CellTypeDateTime.class;
					}

					if (valueType == Ontology.INTEGER) {
						return CellTypeTextFieldInteger.class;
					}
					return CellTypeTextFieldNumerical.class;
				}
			default:
				throw new IllegalArgumentException("Invalid columnIndex: " + columnIndex);
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		// only first and last column are editable
		if (columnIndex == COLUMN_ATTRIBUTES_INDEX) {
			return true;
		}
		if (columnIndex == COLUMN_FILTER_VALUE_INDEX) {
			// all filter value cells are editable; except for special filters as they do not need a
			// filter value
			CustomFilters filter = CustomFilters
					.getByLabel(String.valueOf(getValueAt(rowIndex, COLUMN_CUSTOM_FILTER_INDEX)));
			return !filter.isSpecialFilter();
		}
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex < 0 || rowIndex >= getRowCount()) {
			throw new IllegalArgumentException("Invalid rowIndex: " + columnIndex);
		}
		switch (columnIndex) {
			case COLUMN_ATTRIBUTES_INDEX:
				return rowListOfSelectedAttributes.get(rowIndex);
			case COLUMN_CUSTOM_FILTER_INDEX:
				// return the filter label (I18N), internally the symbol is used (no I18N)
				return CustomFilters.getBySymbol(rowListOfSelectedComparators.get(rowIndex)).getLabel();
			case COLUMN_FILTER_VALUE_INDEX:
				return rowListOfSelectedValues.get(rowIndex);
			default:
				throw new IllegalArgumentException("Invalid columnIndex: " + columnIndex);
		}
	}

	@Override
	public String getHelptextAt(int rowIndex, int columnIndex) {
		if (rowIndex < 0 || rowIndex >= getRowCount()) {
			throw new IllegalArgumentException("Invalid rowIndex: " + columnIndex);
		}
		switch (columnIndex) {
			case COLUMN_ATTRIBUTES_INDEX:
				return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.filter_table_model.column_name_attribute.tip");
			case COLUMN_CUSTOM_FILTER_INDEX:
				CustomFilters filter = CustomFilters.getByLabel(String.valueOf(getValueAt(rowIndex,
						COLUMN_CUSTOM_FILTER_INDEX)));
				return filter == null ? null : filter.getHelptext();
			case COLUMN_FILTER_VALUE_INDEX:
				String genericHelptext = I18N.getMessage(I18N.getGUIBundle(),
						"gui.dialog.filter_table_model.column_name_field.tip");
				CustomFilters filter1 = CustomFilters.getByLabel(String.valueOf(getValueAt(rowIndex,
						COLUMN_CUSTOM_FILTER_INDEX)));
				return filter1 == null ? genericHelptext : filter1.getHelptext();
			default:
				throw new IllegalArgumentException("Invalid columnIndex: " + columnIndex);
		}
	}

	@Override
	public String getSyntaxHelpAt(int rowIndex, int columnIndex) {
		if (rowIndex < 0 || rowIndex >= getRowCount()) {
			throw new IllegalArgumentException("Invalid rowIndex: " + columnIndex);
		}
		switch (columnIndex) {
			case COLUMN_ATTRIBUTES_INDEX:
				return null;
			case COLUMN_CUSTOM_FILTER_INDEX:
				return null;
			case COLUMN_FILTER_VALUE_INDEX:
				// only show syntax help for time class
				if (CellTypeTextFieldTime.class.isAssignableFrom(getColumnClass(rowIndex, columnIndex))) {
					return SYNTAX_TIME;
				} else if (CellTypeDateTime.class.isAssignableFrom(getColumnClass(rowIndex, columnIndex))) {
					return SYNTAX_DATE_TIME;
				} else if (CellTypeDate.class.isAssignableFrom(getColumnClass(rowIndex, columnIndex))) {
					return SYNTAX_DATE;
				} else {
					return null;
				}
			default:
				throw new IllegalArgumentException("Invalid columnIndex: " + columnIndex);
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (rowIndex < 0 || rowIndex >= getRowCount()) {
			throw new IllegalArgumentException("Invalid rowIndex: " + columnIndex);
		}
		switch (columnIndex) {
			case COLUMN_ATTRIBUTES_INDEX:
				if (!(aValue instanceof String)) {
					throw new IllegalArgumentException("aValue must be of class String!");
				}
				rowListOfSelectedAttributes.set(rowIndex, String.valueOf(aValue));
				// attribute selection has consequences for the comparator column GUI, so fire a
				// change there
				fireTableChanged(new TableModelEvent(this, rowIndex, rowIndex, COLUMN_CUSTOM_FILTER_INDEX,
						TableModelEvent.UPDATE));
				// attribute selection has consequences for the filter value column GUI, so fire a
				// change there
				fireTableChanged(new TableModelEvent(this, rowIndex, rowIndex, COLUMN_FILTER_VALUE_INDEX,
						TableModelEvent.UPDATE));
				break;
			case COLUMN_CUSTOM_FILTER_INDEX:
				if (!(aValue instanceof String)) {
					throw new IllegalArgumentException("aValue must be of class String!");
				}
				// set the filter symbol (no I18N), GUI uses the label (I18N)
				CustomFilters filter = CustomFilters.getByLabel(String.valueOf(aValue));
				if (filter != null) {
					rowListOfSelectedComparators.set(rowIndex, filter.getSymbol());
					// special case switch to missing filter: clear value column as well
					if (filter == CustomFilters.MISSING || filter == CustomFilters.NOT_MISSING) {
						rowListOfSelectedValues.set(rowIndex, "");
					}
					// filter selection may have consequences for the filter value column GUI, so
					// fire a change there
					fireTableChanged(new TableModelEvent(this, rowIndex, rowIndex, COLUMN_FILTER_VALUE_INDEX,
							TableModelEvent.UPDATE));
				}
				break;
			case COLUMN_FILTER_VALUE_INDEX:
				if (!(aValue instanceof String)) {
					throw new IllegalArgumentException("aValue must be of class String!");
				}
				rowListOfSelectedValues.set(rowIndex, String.valueOf(aValue));
				break;
			default:
				throw new IllegalArgumentException("Invalid columnIndex: " + columnIndex);
		}
	}

	@Override
	public void appendRow() {
		rowsCount++;

		rowListOfSelectedAttributes.add(listOfAttributeMetaData.size() <= 0 ? null : listOfAttributeMetaData.get(0)
				.getName());

		// see if we have a valueType and set comparator accordingly
		int valueType = listOfAttributeMetaData.size() <= 0 ? Ontology.ATTRIBUTE_VALUE : listOfAttributeMetaData.get(0)
				.getValueType();
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL)) {
			rowListOfSelectedComparators.add(CustomFilters.EQUALS_NOMINAL.getSymbol());
		} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NUMERICAL)
				|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME)) {
			rowListOfSelectedComparators.add(CustomFilters.EQUALS_NUMERICAL.getSymbol());
		} else {
			rowListOfSelectedComparators.add(CustomFilters.CONTAINS.getSymbol());
		}

		rowListOfSelectedValues.add("");

		fireTableStructureChanged();
	}

	@Override
	public void removeRow(int rowIndex) {
		if (rowIndex < 0 || rowIndex >= rowsCount) {
			throw new IllegalArgumentException("Invalid rowIndex: " + rowIndex);
		}
		if (rowsCount <= 0) {
			return;
		}
		rowsCount--;
		rowListOfSelectedAttributes.remove(rowIndex);
		rowListOfSelectedComparators.remove(rowIndex);
		rowListOfSelectedValues.remove(rowIndex);

		fireTableStructureChanged();
	}

	@Override
	public List<String> getPossibleValuesForCellOrNull(int rowIndex, int columnIndex) throws IllegalArgumentException {
		if (rowIndex < 0 || rowIndex >= getRowCount()) {
			throw new IllegalArgumentException("Invalid rowIndex: " + rowIndex);
		}
		if (columnIndex < 0 || columnIndex >= getColumnCount()) {
			throw new IllegalArgumentException("Invalid columnIndex: " + columnIndex);
		}
		List<String> possibleValues = null;

		// attribute column
		if (columnIndex == COLUMN_ATTRIBUTES_INDEX) {
			possibleValues = new LinkedList<>();
			for (AttributeMetaData md : listOfAttributeMetaData) {
				possibleValues.add(md.getName());
			}
		}

		// custom filter column
		if (columnIndex == COLUMN_CUSTOM_FILTER_INDEX) {
			possibleValues = new LinkedList<>();
			String selectedAttributeName = String.valueOf(getValueAt(rowIndex, COLUMN_ATTRIBUTES_INDEX));
			int valueType = Ontology.ATTRIBUTE_VALUE;
			if (checkMetaDataForComparators) {
				// only when possible values should depend on meta data
				// see if the attribute has been set to an existing one so the value type can be
				// obtained
				for (AttributeMetaData md : listOfAttributeMetaData) {
					if (md.getName().equals(selectedAttributeName)) {
						valueType = md.getValueType();
						break;
					}
				}
			}

			for (CustomFilters filter : CustomFilters.getFiltersForValueType(valueType)) {
				possibleValues.add(filter.getLabel());
			}
		}

		// filter value column
		if (columnIndex == COLUMN_FILTER_VALUE_INDEX) {
			String selectedAttributeName = String.valueOf(getValueAt(rowIndex, COLUMN_ATTRIBUTES_INDEX));
			AttributeMetaData attMD = null;
			// see if the attribute has been set to an existing one so the meta data can be obtained
			for (AttributeMetaData md : listOfAttributeMetaData) {
				if (md.getName().equals(selectedAttributeName)) {
					// content assist for value column is always possible except for unknown meta
					// data
					attMD = md;
					break;
				}
			}
			if (attMD != null) {
				possibleValues = new LinkedList<>();
				if (attMD.isNominal()) {
					int count = 0;
					for (String value : attMD.getValueSet()) {
						possibleValues.add(value);
						count++;
						if (count > MAX_NOMINAL_VALUES) {
							break;
						}
					}
				} else {
					// we only have a range so we can only add lower and upper bounds for numerical
					// values
					double upperBound = attMD.getValueRange().getUpper();
					double lowerBound = attMD.getValueRange().getLower();
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attMD.getValueType(), Ontology.DATE)) {
						possibleValues.add(FORMAT_DATE.format(new Date((long) lowerBound)));
						possibleValues.add(FORMAT_DATE.format(new Date((long) upperBound)));
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attMD.getValueType(), Ontology.TIME)) {
						possibleValues.add(FORMAT_TIME.format(new Date((long) lowerBound)));
						possibleValues.add(FORMAT_TIME.format(new Date((long) upperBound)));
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attMD.getValueType(), Ontology.DATE_TIME)) {
						possibleValues.add(FORMAT_DATE_TIME.format(new Date((long) lowerBound)));
						possibleValues.add(FORMAT_DATE_TIME.format(new Date((long) upperBound)));
					} else {
						possibleValues.add(String.valueOf(lowerBound) + " "
								+ I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.filter_table_model.lower_bound.title"));
						possibleValues.add(String.valueOf(upperBound) + " "
								+ I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.filter_table_model.upper_bound.title"));
					}
				}
			}
		}

		return possibleValues;
	}

	@Override
	public boolean isContentAssistPossibleForCell(int rowIndex, int columnIndex) {
		if (rowIndex < 0 || rowIndex >= getRowCount()) {
			throw new IllegalArgumentException("Invalid rowIndex: " + rowIndex);
		}
		if (columnIndex < 0 || columnIndex >= getColumnCount()) {
			throw new IllegalArgumentException("Invalid columnIndex: " + columnIndex);
		}

		if (columnIndex == COLUMN_FILTER_VALUE_INDEX) {
			String selectedAttributeName = String.valueOf(getValueAt(rowIndex, COLUMN_ATTRIBUTES_INDEX));
			// see if the attribute has been set to an existing one so the meta data can be obtained
			for (AttributeMetaData md : listOfAttributeMetaData) {
				if (md.getName().equals(selectedAttributeName)) {
					// content assist for value column is always possible except for unknown meta
					// data
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean canCellHaveMultipleValues(int rowIndex, int columnIndex) {
		if (rowIndex < 0 || rowIndex >= getRowCount()) {
			throw new IllegalArgumentException("Invalid rowIndex: " + rowIndex);
		}
		if (columnIndex < 0 || columnIndex >= getColumnCount()) {
			throw new IllegalArgumentException("Invalid columnIndex: " + columnIndex);
		}
		if (!isContentAssistPossibleForCell(rowIndex, columnIndex)) {
			return false;
		}

		if (columnIndex == COLUMN_FILTER_VALUE_INDEX) {
			CustomFilters filter = CustomFilters
					.getByLabel(String.valueOf(getValueAt(rowIndex, COLUMN_CUSTOM_FILTER_INDEX)));
			// condition with == is intended, it's an enum
			if (filter == CustomFilters.IS_IN_NOMINAL || filter == CustomFilters.IS_NOT_IN_NOMINAL) {
				// multiple values are only allowed for these filters
				return true;
			}
		}

		return false;
	}

	@Override
	public List<String> convertEncodedStringValueToList(String encodedValue) {
		List<String> result;
		try {
			result = Tools.unescape(encodedValue, CustomFilters.ESCAPE_CHAR, new char[] { CustomFilters.SEPERATOR_CHAR },
					CustomFilters.SEPERATOR_CHAR);
		} catch (IllegalArgumentException e) {
			// happens when there is a char escaped via one backslash which is no special char,
			// ignore it
			return Collections.<String> emptyList();
		}
		return result;
	}

	@Override
	public String encodeListOfStringsToValue(List<String> listOfStrings) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < listOfStrings.size(); i++) {
			if (i > 0) {
				builder.append(CustomFilters.SEPERATOR_CHAR);
			}
			if (listOfStrings.get(i) != null) {
				builder.append(Tools.escape(listOfStrings.get(i), CustomFilters.ESCAPE_CHAR,
						new char[] { CustomFilters.SEPERATOR_CHAR }));
			}
		}
		return builder.toString();
	}

	@Override
	public List<String[]> getRowTupels() {
		List<String[]> list = new LinkedList<>();
		for (int row = 0; row < getRowCount(); row++) {
			// conversion because getValue() returns the filter label (used by GUI which displays
			// the I18N label instead of the symbol)
			// but getTupels() expects the filter symbols
			CustomFilters filter = CustomFilters.getByLabel(String.valueOf(getValueAt(row, COLUMN_CUSTOM_FILTER_INDEX)));
			String filterSymbol = filter != null ? filter.getSymbol() : "eq";
			list.add(new String[] { String.valueOf(getValueAt(row, COLUMN_ATTRIBUTES_INDEX)), filterSymbol,
					String.valueOf(getValueAt(row, COLUMN_FILTER_VALUE_INDEX)) });
		}

		return list;
	}

	@Override
	public void setRowTupels(List<String[]> tupelList) {
		creatingModel.set(true);
		// remove existing rows (if there are any)
		for (int row = getRowCount() - 1; row >= 0; row--) {
			removeRow(row);
		}

		// add new rows
		for (int row = 0; row < tupelList.size(); row++) {
			appendRow();

			String[] tupel = tupelList.get(row);
			// conversion because setValue() takes the filter label (used by GUI which displays the
			// I18N label instead of the symbol)
			// but setTupels() gets called with the filter symbols
			CustomFilters filter = CustomFilters.getBySymbol(tupel[1]);
			String filterLabel = filter != null ? filter.getLabel() : "=";
			setValueAt(tupel[0], row, COLUMN_ATTRIBUTES_INDEX);
			setValueAt(filterLabel, row, COLUMN_CUSTOM_FILTER_INDEX);
			setValueAt(tupel[2], row, COLUMN_FILTER_VALUE_INDEX);
		}
		creatingModel.set(false);

		fireTableStructureChanged();
	}

	@Override
	public void fireTableChanged(TableModelEvent e) {
		// don't fire while we are creating the model
		if (creatingModel.get()) {
			return;
		}

		super.fireTableChanged(e);
	}

	@Override
	public void fireTableStructureChanged() {
		// don't fire while we are creating the model
		if (creatingModel.get()) {
			return;
		}

		super.fireTableStructureChanged();
	}

	/**
	 * Sets checkMetaDataForComparators and fires an update if it has been changed.
	 *
	 * @param checkMetaDataForComparators
	 *            the flag which decides whether the meta data should be checked for filter
	 *            comparator preselection
	 */
	public void setCheckMetaDataForComparators(boolean checkMetaDataForComparators) {
		if (this.checkMetaDataForComparators != checkMetaDataForComparators) {
			this.checkMetaDataForComparators = checkMetaDataForComparators;
			for (int row = 0; row < rowsCount; row++) {
				fireTableCellUpdated(row, FilterTableModel.COLUMN_CUSTOM_FILTER_INDEX);
			}
		}
	}

}
