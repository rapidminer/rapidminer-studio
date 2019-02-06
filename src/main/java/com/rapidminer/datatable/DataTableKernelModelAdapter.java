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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.learner.functions.kernel.KernelModel;


/**
 * This class can be used to use a kernel model as data table. The data is directly read from the
 * kernel model instead of building a copy. Please note that the method for adding new rows is not
 * supported by this type of data tables.
 *
 * @author Ingo Mierswa
 */
public class DataTableKernelModelAdapter extends AbstractDataTable {

	/** Helper class to iterated over the examples or support vectors of a {@link KernelModel}. */
	private static class KernelModelIterator implements Iterator<DataTableRow> {

		private int counter = 0;

		private DataTableKernelModelAdapter adapter;

		public KernelModelIterator(DataTableKernelModelAdapter adapter) {
			this.adapter = adapter;
		}

		@Override
		public boolean hasNext() {
			return counter < adapter.getNumberOfRows();
		}

		@Override
		public DataTableRow next() {
			DataTableRow row = adapter.getRow(counter);
			counter++;
			return row;
		}

		@Override
		public void remove() {
			throw new RuntimeException("DataTable.KernelModelIterator: remove not supported!");
		}
	}

	private KernelModel kernelModel;

	private String[] attributeNames;

	private static final String DEFAULT_REGULAR_ATTRIBUTE_PREFIX = "attribute";

	private int[] sampleMapping = null;

	private Map<Integer, String> index2LabelMap = new HashMap<>();
	private Map<String, Integer> label2IndexMap = new HashMap<>();

	public DataTableKernelModelAdapter(KernelModel kernelModel) {
		super("Kernel Model Support Vectors");
		this.kernelModel = kernelModel;
		int labelCounter = 0;
		if (this.kernelModel.isClassificationModel()) {
			for (int i = 0; i < this.kernelModel.getNumberOfSupportVectors(); i++) {
				String label = this.kernelModel.getClassificationLabel(i);
				if (label2IndexMap.get(label) == null) {
					this.label2IndexMap.put(label, labelCounter);
					this.index2LabelMap.put(labelCounter, label);
					labelCounter++;
				}
			}
		}

		// storing attribute names
		attributeNames = new String[kernelModel.getTrainingHeader().getAttributes().size()];
		int i = 0;
		for (Attribute attribute : kernelModel.getTrainingHeader().getAttributes()) {
			attributeNames[i] = attribute.getName();
			i++;
		}
	}

	public DataTableKernelModelAdapter(DataTableKernelModelAdapter dataTableKernelModelAdapter) {
		super(dataTableKernelModelAdapter.getName());
		this.kernelModel = dataTableKernelModelAdapter.kernelModel; // shallow clone
		this.index2LabelMap = dataTableKernelModelAdapter.index2LabelMap; // shallow clone
		this.label2IndexMap = dataTableKernelModelAdapter.label2IndexMap; // shallow clone
		this.attributeNames = dataTableKernelModelAdapter.attributeNames; // shallow clone
		this.sampleMapping = null;
	}

	@Override
	public int getNumberOfSpecialColumns() {
		return KernelModelRow2DataTableRowWrapper.NUMBER_OF_SPECIAL_COLUMNS;
	}

	@Override
	public boolean isSpecial(int index) {
		return index < KernelModelRow2DataTableRowWrapper.NUMBER_OF_SPECIAL_COLUMNS;
	}

	@Override
	public boolean isNominal(int index) {
		if (index == KernelModelRow2DataTableRowWrapper.LABEL) {
			return this.kernelModel.isClassificationModel();
		} else {
			return index == KernelModelRow2DataTableRowWrapper.SUPPORT_VECTOR;
		}
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
	public String mapIndex(int column, int value) {
		if (column == KernelModelRow2DataTableRowWrapper.LABEL && this.kernelModel.isClassificationModel()) {
			return index2LabelMap.get(value);
		} else if (column == KernelModelRow2DataTableRowWrapper.SUPPORT_VECTOR) {
			if (value == 0) {
				return "no support vector";
			} else {
				return "support vector";
			}
		} else {
			return null;
		}
	}

	@Override
	public int mapString(int column, String value) {
		if (column == KernelModelRow2DataTableRowWrapper.LABEL && this.kernelModel.isClassificationModel()) {
			return label2IndexMap.get(value);
		} else if (column == KernelModelRow2DataTableRowWrapper.SUPPORT_VECTOR) {
			if ("no support vector".equals(value)) {
				return 0;
			} else {
				return 1;
			}
		} else {
			return -1;
		}
	}

	@Override
	public int getNumberOfValues(int column) {
		if (column == KernelModelRow2DataTableRowWrapper.LABEL && this.kernelModel.isClassificationModel()) {
			return index2LabelMap.size();
		} else if (column == KernelModelRow2DataTableRowWrapper.SUPPORT_VECTOR) {
			return 2;
		} else {
			return -1;
		}
	}

	@Override
	public String getColumnName(int i) {
		if (i < KernelModelRow2DataTableRowWrapper.NUMBER_OF_SPECIAL_COLUMNS) {
			return KernelModelRow2DataTableRowWrapper.SPECIAL_COLUMN_NAMES[i];
		} else {
			int attributeIndex = i - KernelModelRow2DataTableRowWrapper.NUMBER_OF_SPECIAL_COLUMNS;
			if (attributeIndex >= 0 && attributeIndex <= attributeNames.length) {
				return attributeNames[attributeIndex];
			}
			return DEFAULT_REGULAR_ATTRIBUTE_PREFIX + (attributeIndex + 1);
		}
	}

	@Override
	public int getColumnIndex(String name) {
		for (int i = 0; i < KernelModelRow2DataTableRowWrapper.NUMBER_OF_SPECIAL_COLUMNS; i++) {
			if (KernelModelRow2DataTableRowWrapper.SPECIAL_COLUMN_NAMES[i].equals(name)) {
				return i;
			}
		}
		for (int i = 0; i < attributeNames.length; i++) {
			if (attributeNames[i].equals(name)) {
				return i + KernelModelRow2DataTableRowWrapper.NUMBER_OF_SPECIAL_COLUMNS;
			}
		}
		if (name.startsWith(DEFAULT_REGULAR_ATTRIBUTE_PREFIX)) {
			return Integer.parseInt(name.substring(DEFAULT_REGULAR_ATTRIBUTE_PREFIX.length()))
					+ KernelModelRow2DataTableRowWrapper.NUMBER_OF_SPECIAL_COLUMNS - 1;
		}
		return -1;
	}

	@Override
	public boolean isSupportingColumnWeights() {
		return false;
	}

	@Override
	public double getColumnWeight(int column) {
		return Double.NaN;
	}

	@Override
	public int getNumberOfColumns() {
		return kernelModel.getNumberOfAttributes() + KernelModelRow2DataTableRowWrapper.NUMBER_OF_SPECIAL_COLUMNS;
	}

	@Override
	public int getNumberOfRows() {
		if (this.sampleMapping == null) {
			return this.kernelModel.getNumberOfSupportVectors();
		} else {
			return this.sampleMapping.length;
		}
	}

	@Override
	public void add(DataTableRow row) {
		throw new RuntimeException("DataTableKernelModelAdapter: adding new rows is not supported!");
	}

	@Override
	public DataTableRow getRow(int index) {
		if (this.sampleMapping == null) {
			return new KernelModelRow2DataTableRowWrapper(this.kernelModel, this, index);
		} else {
			return new KernelModelRow2DataTableRowWrapper(this.kernelModel, this, this.sampleMapping[index]);
		}
	}

	@Override
	public Iterator<DataTableRow> iterator() {
		return new KernelModelIterator(this);
	}

	@Override
	public DataTable sample(int newSize) {
		DataTableKernelModelAdapter result = new DataTableKernelModelAdapter(this);

		double ratio = (double) newSize / (double) getNumberOfRows();
		Random random = new Random(2001);
		List<Integer> usedRows = new LinkedList<>();
		for (int i = 0; i < getNumberOfRows(); i++) {
			if (random.nextDouble() <= ratio) {
				int index = i;
				if (this.sampleMapping != null) {
					index = this.sampleMapping[index];
				}
				usedRows.add(index);
			}
		}
		int[] sampleMapping = new int[usedRows.size()];
		int counter = 0;
		Iterator<Integer> i = usedRows.iterator();
		while (i.hasNext()) {
			sampleMapping[counter++] = i.next();
		}

		result.sampleMapping = sampleMapping;
		return result;
	}

}
