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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.ObjectVisualizerService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;


/**
 * This class can be used to use an example set as data table. The data is directly read from the
 * example set instead of building a copy. Please note that the method for adding new rows is not
 * supported by this type of data tables.
 *
 * @author Ingo Mierswa
 */
public class DataTableExampleSetAdapter extends AbstractDataTable {

	private static final int DEFAULT_MAX_SIZE_FOR_SHUFFLED_SAMPLING = 100000;

	private ExampleSet exampleSet;

	private List<Attribute> allAttributes = new ArrayList<Attribute>();

	private int numberOfRegularAttributes = 0;

	private AttributeWeights weights = null;

	private Attribute idAttribute;

	public DataTableExampleSetAdapter(ExampleSet exampleSet, AttributeWeights weights) {
		this(exampleSet, weights, true);
	}

	/**
	 * @param ignoreId
	 *            If this variable is true, the id will not be visible in the data table.
	 */
	public DataTableExampleSetAdapter(ExampleSet exampleSet, AttributeWeights weights, boolean ignoreId) {
		super("Data Table");
		this.exampleSet = exampleSet;
		this.weights = weights;

		for (Attribute attribute : exampleSet.getAttributes()) {
			allAttributes.add(attribute);
		}

		this.idAttribute = exampleSet.getAttributes().getId();
		Iterator<AttributeRole> s = exampleSet.getAttributes().specialAttributes();
		while (s.hasNext()) {
			Attribute specialAttribute = s.next().getAttribute();
			if (!ignoreId || idAttribute == null || !idAttribute.getName().equals(specialAttribute.getName())) {
				allAttributes.add(specialAttribute);
			}
		}

		this.numberOfRegularAttributes = exampleSet.getAttributes().size();

		// TODO: Find another solution for this hack
		registerVisualizerForMe(exampleSet);
	}

	public DataTableExampleSetAdapter(DataTableExampleSetAdapter dataTableExampleSetAdapter) {
		super(dataTableExampleSetAdapter.getName());
		this.exampleSet = dataTableExampleSetAdapter.exampleSet; // shallow clone
		this.allAttributes = dataTableExampleSetAdapter.allAttributes; // shallow clone
		this.numberOfRegularAttributes = dataTableExampleSetAdapter.numberOfRegularAttributes;
		this.weights = dataTableExampleSetAdapter.weights; // shallow clone
		this.idAttribute = dataTableExampleSetAdapter.idAttribute; // shallow clone

		// TODO: Find another solution for this hack
		registerVisualizerForMe(dataTableExampleSetAdapter);
	}

	@Override
	public int getNumberOfSpecialColumns() {
		return allAttributes.size() - numberOfRegularAttributes;
	}

	@Override
	public boolean isSpecial(int index) {
		return index >= numberOfRegularAttributes;
	}

	@Override
	public boolean isNominal(int index) {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(allAttributes.get(index).getValueType(), Ontology.NOMINAL);
	}

	@Override
	public boolean isDate(int index) {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(allAttributes.get(index).getValueType(), Ontology.DATE);
	}

	@Override
	public boolean isTime(int index) {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(allAttributes.get(index).getValueType(), Ontology.TIME);
	}

	@Override
	public boolean isDateTime(int index) {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(allAttributes.get(index).getValueType(), Ontology.DATE_TIME);
	}

	@Override
	public boolean isNumerical(int index) {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(allAttributes.get(index).getValueType(), Ontology.NUMERICAL);
	}

	public String getLabelName() {
		if (this.exampleSet.getAttributes().getLabel() != null) {
			return this.exampleSet.getAttributes().getLabel().getName();
		} else {
			return null;
		}
	}

	public String getClusterName() {
		if (this.exampleSet.getAttributes().getCluster() != null) {
			return this.exampleSet.getAttributes().getCluster().getName();
		} else {
			return null;
		}
	}

	public boolean isLabelNominal() {
		if (this.exampleSet.getAttributes().getLabel() != null) {
			return this.exampleSet.getAttributes().getLabel().getValueType() == Ontology.NOMINAL;
		} else {
			return false; // if there is no label, it should not be nominal
		}
	}

	public boolean hasId() {
		if (exampleSet.getAttributes().getId() == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public String mapIndex(int column, int value) {
		try {
			return allAttributes.get(column).getMapping().mapIndex(value);
		} catch (AttributeTypeException e) {
			// Can throw AttributeTypeException in case mapping is empty
			return "?";
		}
	}

	@Override
	public int mapString(int column, String value) {
		return allAttributes.get(column).getMapping().mapString(value);
	}

	@Override
	public int getNumberOfValues(int column) {
		return allAttributes.get(column).getMapping().size();
	}

	@Override
	public String getColumnName(int i) {
		return allAttributes.get(i).getName();
	}

	@Override
	public int getColumnIndex(String name) {
		for (int i = 0; i < allAttributes.size(); i++) {
			if (allAttributes.get(i).getName().equals(name)) {
				return i;
			}
		}
		return -1;
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
			return weights.getWeight(getColumnName(column));
		}
	}

	@Override
	public int getNumberOfColumns() {
		return this.allAttributes.size();
	}

	@Override
	public void add(DataTableRow row) {
		throw new RuntimeException("DataTableExampleSetAdapter: adding new rows is not supported!");
	}

	@Override
	public DataTableRow getRow(int index) {
		return new Example2DataTableRowWrapper(exampleSet.getExample(index), allAttributes, idAttribute);
	}

	@Override
	public Iterator<DataTableRow> iterator() {
		return new Example2DataTableRowIterator(exampleSet.iterator(), allAttributes, idAttribute);
	}

	@Override
	public int getNumberOfRows() {
		return this.exampleSet.size();
	}

	@Override
	public DataTable sample(int newSize) {
		DataTableExampleSetAdapter result = new DataTableExampleSetAdapter(this);

		double ratio = (double) newSize / (double) getNumberOfRows();

		int maxNumberBeforeSampling = DEFAULT_MAX_SIZE_FOR_SHUFFLED_SAMPLING;
		String maxString = ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_MAX_STATISTICS_ROWS);
		if (maxString != null) {
			try {
				maxNumberBeforeSampling = Integer.parseInt(maxString);
			} catch (NumberFormatException e) {
				// do nothing
			}
		}

		ExampleSet exampleSet = null;
		if (getNumberOfRows() < maxNumberBeforeSampling) {
			try {
				exampleSet = new SplittedExampleSet(this.exampleSet, ratio, SplittedExampleSet.SHUFFLED_SAMPLING, false, 0);
			} catch (UserError e) {
				// this exception is only thrown for Stratified Sampling
			}
			if (exampleSet == null) {
				return null; // cannot happen
			}
			((SplittedExampleSet) exampleSet).selectSingleSubset(0);
		} else {
			exampleSet = Tools.getLinearSubsetCopy(this.exampleSet, newSize, 0);
		}

		result.exampleSet = exampleSet;
		return result;
	}

	public static ExampleSet createExampleSetFromDataTable(DataTable table) {
		List<Attribute> attributes = new ArrayList<Attribute>();

		for (int i = 0; i < table.getNumberOfColumns(); i++) {
			if (table.isDate(i)) {
				Attribute attribute = AttributeFactory.createAttribute(table.getColumnName(i), Ontology.DATE);
				attributes.add(attribute);
			} else if (table.isTime(i)) {
				Attribute attribute = AttributeFactory.createAttribute(table.getColumnName(i), Ontology.TIME);
				attributes.add(attribute);
			} else if (table.isDateTime(i)) {
				Attribute attribute = AttributeFactory.createAttribute(table.getColumnName(i), Ontology.DATE_TIME);
				attributes.add(attribute);
			} else if (table.isNominal(i)) {
				Attribute attribute = AttributeFactory.createAttribute(table.getColumnName(i), Ontology.NOMINAL);
				attributes.add(attribute);
			} else {
				Attribute attribute = AttributeFactory.createAttribute(table.getColumnName(i), Ontology.REAL);
				attributes.add(attribute);
			}
		}

		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(table.getNumberOfRows());

		for (int i = 0; i < table.getNumberOfRows(); i++) {
			DataTableRow row = table.getRow(i);
			double[] values = new double[attributes.size()];
			for (int a = 0; a < values.length; a++) {
				Attribute attribute = attributes.get(a);
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
					values[a] = row.getValue(a);
				} else if (attribute.isNominal()) {
					values[a] = attribute.getMapping().mapString(table.getValueAsString(row, a));
				} else {
					values[a] = row.getValue(a);
				}
			}
			builder.addRow(values);
		}

		return builder.build();
	}

	private void registerVisualizerForMe(Object father) {
		ObjectVisualizer visualizer = ObjectVisualizerService.getVisualizerForObject(father);
		ObjectVisualizerService.addObjectVisualizer(this, visualizer);
	}

}
