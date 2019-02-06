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
package com.rapidminer.datatable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.rapidminer.tools.Tools;


/**
 * A simple data table implementation which stores the data itself.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class SimpleDataTable extends AbstractDataTable implements Serializable {

	private static final long serialVersionUID = 4459570725439894361L;

	private List<DataTableRow> data = new ArrayList<>();

	private String[] columns;

	private double[] weights;

	private boolean[] specialColumns;

	private Map<Integer, Map<Integer, String>> index2StringMap = new HashMap<>();
	private Map<Integer, Map<String, Integer>> string2IndexMap = new HashMap<>();

	private int[] currentIndices;

	public SimpleDataTable(String name, String[] columns) {
		this(name, columns, null);
	}

	public SimpleDataTable(String name, String[] columns, double[] weights) {
		super(name);
		this.columns = columns;
		this.weights = weights;
		this.specialColumns = new boolean[columns.length];
		for (int i = 0; i < this.specialColumns.length; i++) {
			this.specialColumns[i] = false;
		}
		this.currentIndices = new int[columns.length];
		for (int i = 0; i < currentIndices.length; i++) {
			currentIndices[i] = 0;
		}
	}

	private SimpleDataTable(SimpleDataTable simpleDataTable) {
		super(simpleDataTable.getName());

		this.columns = null;
		if (simpleDataTable.columns != null) {
			this.columns = new String[simpleDataTable.columns.length];
			for (int i = 0; i < simpleDataTable.columns.length; i++) {
				this.columns[i] = simpleDataTable.columns[i];
			}
		}

		this.weights = null;
		if (simpleDataTable.weights != null) {
			this.weights = new double[simpleDataTable.weights.length];
			for (int i = 0; i < simpleDataTable.weights.length; i++) {
				this.weights[i] = simpleDataTable.weights[i];
			}
		}

		this.specialColumns = null;
		if (simpleDataTable.specialColumns != null) {
			this.specialColumns = new boolean[simpleDataTable.specialColumns.length];
			for (int i = 0; i < simpleDataTable.specialColumns.length; i++) {
				this.specialColumns[i] = simpleDataTable.specialColumns[i];
			}
		}

		this.currentIndices = new int[simpleDataTable.currentIndices.length];
		for (int i = 0; i < this.currentIndices.length; i++) {
			this.currentIndices[i] = simpleDataTable.currentIndices[i];
		}

		this.index2StringMap = new HashMap<>();
		for (Map.Entry<Integer, Map<Integer, String>> entry : simpleDataTable.index2StringMap.entrySet()) {
			Integer key = entry.getKey();
			Map<Integer, String> indexMap = entry.getValue();
			Map<Integer, String> newIndexMap = new HashMap<>();
			for (Map.Entry<Integer, String> innerEntry : indexMap.entrySet()) {
				newIndexMap.put(innerEntry.getKey(), innerEntry.getValue());
			}
			this.index2StringMap.put(key, newIndexMap);
		}

		this.string2IndexMap = new HashMap<>();
		for (Map.Entry<Integer, Map<String, Integer>> entry : simpleDataTable.string2IndexMap.entrySet()) {
			Integer key = entry.getKey();
			Map<String, Integer> indexMap = entry.getValue();
			Map<String, Integer> newIndexMap = new HashMap<>();
			for (Map.Entry<String, Integer> innerEntry : indexMap.entrySet()) {
				newIndexMap.put(innerEntry.getKey(), innerEntry.getValue());
			}
			this.string2IndexMap.put(key, newIndexMap);
		}
	}

	@Override
	public int getNumberOfSpecialColumns() {
		int counter = 0;
		for (boolean b : specialColumns) {
			if (b) {
				counter++;
			}
		}
		return counter;
	}

	@Override
	public boolean isSpecial(int index) {
		return specialColumns[index];
	}

	public void setSpecial(int index, boolean special) {
		this.specialColumns[index] = special;
	}

	@Override
	public boolean isNominal(int column) {
		return index2StringMap.get(column) != null;
	}

	@Override
	public boolean isDate(int index) {
		return false;
	}

	@Override
	public boolean isTime(int index) {
		return false;
	}

	@Override
	public boolean isDateTime(int index) {
		return false;
	}

	@Override
	public boolean isNumerical(int index) {
		return !isNominal(index);
	}

	@Override
	public String mapIndex(int column, int index) {
		Map<Integer, String> columnIndexMap = index2StringMap.get(column);
		return columnIndexMap.get(index);
	}

	@Override
	public int mapString(int column, String value) {
		Map<String, Integer> columnValueMap = string2IndexMap.get(column);
		if (columnValueMap == null) {
			columnValueMap = new HashMap<>();
			columnValueMap.put(value, currentIndices[column]);
			string2IndexMap.put(column, columnValueMap);
			Map<Integer, String> columnIndexMap = new HashMap<>();
			columnIndexMap.put(currentIndices[column], value);
			index2StringMap.put(column, columnIndexMap);
			int returnValue = currentIndices[column];
			currentIndices[column]++;
			return returnValue;
		} else {
			Integer result = columnValueMap.get(value);
			if (result != null) {
				return result.intValue();
			} else {
				int newIndex = currentIndices[column];
				columnValueMap.put(value, newIndex);
				Map<Integer, String> columnIndexMap = index2StringMap.get(column);
				columnIndexMap.put(newIndex, value);
				currentIndices[column]++;
				return newIndex;
			}
		}
	}

	@Override
	public int getNumberOfValues(int column) {
		return index2StringMap.get(column).size();
	}

	public void cleanMappingTables() {
		Map<Integer, Set<String>> allValues = new HashMap<>();
		for (Map.Entry<Integer, Map<String, Integer>> entry : this.string2IndexMap.entrySet()) {
			Integer key = entry.getKey();
			Set<String> columnValues = new HashSet<>();
			for (String current : entry.getValue().keySet()) {
				columnValues.add(current);
			}
			allValues.put(key, columnValues);
		}

		for (DataTableRow row : this) {
			for (int i = 0; i < getNumberOfColumns(); i++) {
				if (isNominal(i)) {
					String currentValue = getValueAsString(row, i);
					allValues.get(i).remove(currentValue);
				}
			}
		}

		for (int i = 0; i < getNumberOfColumns(); i++) {
			Set<String> toDelete = allValues.get(i);
			if (toDelete != null) {
				Map<String, Integer> string2Index = this.string2IndexMap.get(i);
				Map<Integer, String> index2String = this.index2StringMap.get(i);
				for (String current : toDelete) {
					int oldIndex = string2Index.get(current);
					index2String.remove(oldIndex);
					string2Index.remove(current);
				}
			}
		}
	}

	@Override
	public boolean isSupportingColumnWeights() {
		return weights != null;
	}

	@Override
	public double getColumnWeight(int column) {
		if (weights == null) {
			return Double.NaN;
		} else {
			return weights[column];
		}
	}

	@Override
	public String getColumnName(int i) {
		return columns[i];
	}

	@Override
	public int getColumnIndex(String name) {
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].equals(name)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public int getNumberOfColumns() {
		return columns.length;
	}

	@Override
	public String[] getColumnNames() {
		return columns;
	}

	@Override
	public synchronized void add(DataTableRow row) {
		synchronized (data) {
			data.add(row);
			fireEvent();
		}
	}

	public synchronized void remove(DataTableRow row) {
		synchronized (data) {
			data.remove(row);
			fireEvent();
		}
	}

	@Override
	public DataTableRow getRow(int index) {
		return data.get(index);
	}

	@Override
	public synchronized Iterator<DataTableRow> iterator() {
		Iterator<DataTableRow> i = null;
		synchronized (data) {
			i = data.iterator();
		}
		return i;
	}

	@Override
	public int getNumberOfRows() {
		int result = 0;
		synchronized (data) {
			result = data.size();
		}
		return result;
	}

	public void clear() {
		data.clear();
		fireEvent();
	}

	@Override
	public synchronized DataTable sample(int newSize) {
		if (getNumberOfRows() <= newSize) {
			return this;
		} else {
			SimpleDataTable result = new SimpleDataTable(this);

			// must be a usual random since otherwise plotting would change the rest of
			// the process during a breakpoint result viewing
			Random random = new Random();

			List<Integer> indices = new ArrayList<>(getNumberOfRows());
			for (int i = 0; i < getNumberOfRows(); i++) {
				indices.add(i);
			}

			while (result.getNumberOfRows() < newSize) {
				int index = random.nextInt(indices.size());
				result.add(data.get(indices.remove(index)));
			}

			return result;
		}
	}

	/** Dumps the complete table into a string (complete data!). */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (DataTableRow row : this) {
			for (int i = 0; i < getNumberOfColumns(); i++) {
				if (i != 0) {
					result.append(", ");
				}
				result.append(row.getValue(i));
			}
			result.append(Tools.getLineSeparator());
		}
		return result.toString();
	}
}
