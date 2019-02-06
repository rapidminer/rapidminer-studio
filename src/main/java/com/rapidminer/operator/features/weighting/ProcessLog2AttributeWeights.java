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
package com.rapidminer.operator.features.weighting;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.EqualTypeCondition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * <p>
 * This operator creates attribute weights from an attribute column in the statistics created by the
 * ProcessLog operator. In order to use this operator, one has first to add a ProcessLog operator
 * inside of a feature selection operator and log the currently selected attribute names. This
 * operator will then calculate attribute weights from such a statistics table by checking how often
 * each attribute was selected and use the relative frequencies as attribute weights.
 * </p>
 * 
 * <p>
 * If the performance is also logged with the ProcessLog operator, these values can also be used to
 * calculate the relative frequencies. In this case, the sorting type can be set to only use the top
 * k or the bottom k attribute sets for frequency calculation according to the specified performance
 * column.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class ProcessLog2AttributeWeights extends Operator {

	public static final String PARAMETER_LOG_NAME = "log_name";

	public static final String PARAMETER_ATTRIBUTE_NAMES_COLUMN = "attribute_names_column";

	public static final String PARAMETER_SORTING_TYPE = "sorting_type";

	public static final String PARAMETER_SORTING_DIMENSION = "sorting_dimension";

	public static final String PARAMETER_SORTING_K = "sorting_k";

	public static final String[] SORTING_TYPES = { "none", "top-k", "bottom-k" };

	public static final int SORTING_TYPE_NONE = 0;
	public static final int SORTING_TYPE_TOP_K = 1;
	public static final int SORTING_TYPE_BOTTOM_K = 2;

	private OutputPort weightsOutput = getOutputPorts().createPort("weights");

	public ProcessLog2AttributeWeights(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(weightsOutput, new MetaData(AttributeWeights.class)));
	}

	@Override
	public void doWork() throws OperatorException {
		Collection<DataTable> dataTables = getProcess().getDataTables();

		// first check if process log statistics are available at all
		if (dataTables.size() == 0) {
			throw new UserError(this, 937);
		}

		DataTable dataTable = null;
		if (isParameterSet(PARAMETER_LOG_NAME)) {
			String dataTableName = getParameterAsString(PARAMETER_LOG_NAME);
			dataTable = getProcess().getDataTable(dataTableName);
		} else {
			if (getProcess().getDataTables().size() > 0) {
				dataTable = getProcess().getDataTables().iterator().next();
				logNote("No log name was specified, using first data table found...");
			}
		}

		if (dataTable == null) {
			throw new UserError(this, 937);
		}

		// check if the statistics contain the desired attribute names column
		String attributeNamesColumnName = getParameterAsString(PARAMETER_ATTRIBUTE_NAMES_COLUMN);
		int attributeNamesIndex = -1;
		int index = 0;
		for (String name : dataTable.getColumnNames()) {
			if (name.equals(attributeNamesColumnName)) {
				attributeNamesIndex = index;
				break;
			}
			index++;
		}
		if (attributeNamesIndex < 0) {
			throw new UserError(this, 207, new Object[] { attributeNamesColumnName, PARAMETER_ATTRIBUTE_NAMES_COLUMN,
					"no column with this name is part of the first found statistics data table" });
		}

		// check sorting type and set sorting parameters
		String sortingDimensionName = null;
		int sortingK = 0;
		int sortingType = getParameterAsInt(PARAMETER_SORTING_TYPE);
		switch (sortingType) {
			case SORTING_TYPE_TOP_K:
				sortingDimensionName = getParameterAsString(PARAMETER_SORTING_DIMENSION);
				sortingK = getParameterAsInt(PARAMETER_SORTING_K);
				break;
			case SORTING_TYPE_BOTTOM_K:
				sortingDimensionName = getParameterAsString(PARAMETER_SORTING_DIMENSION);
				sortingK = getParameterAsInt(PARAMETER_SORTING_K);
				break;
			default:
				break;
		}

		// check if sorting dimension is part of the statistics
		int sortingDimensionIndex = -1;
		if (sortingDimensionName != null) {
			index = 0;
			for (String name : dataTable.getColumnNames()) {
				if (name.equals(sortingDimensionName)) {
					sortingDimensionIndex = index;
					break;
				}
				index++;
			}
			if (sortingDimensionIndex < 0) {
				throw new UserError(this, 207, new Object[] { sortingDimensionName, PARAMETER_SORTING_DIMENSION,
						"no column with this name is part of the first found statistics data table" });
			} else {
				if (!dataTable.isNumerical(sortingDimensionIndex)) {
					throw new UserError(this, 207, new Object[] { sortingDimensionName, PARAMETER_SORTING_DIMENSION,
							"only numerical columns are allowed for the sorting dimension" });
				}
			}
		}

		// Everything OK? Then start the data retrieval...
		AttributeWeights weights = calculateWeights(dataTable, attributeNamesIndex, sortingType, sortingDimensionIndex,
				sortingK);

		// normalize?
		if (getParameterAsBoolean(AbstractWeighting.PARAMETER_NORMALIZE_WEIGHTS)) {
			weights.normalize();
		}

		weightsOutput.deliver(weights);
	}

	private AttributeWeights calculateWeights(DataTable dataTable, int attributeNamesIndex, final int sortingType,
			int sortingDimensionIndex, int sortingK) {

		class NamesAndPerformance implements Comparable<NamesAndPerformance> {

			String names;
			double performance;

			public NamesAndPerformance(String names, double performance) {
				this.names = names;
				this.performance = performance;
			}

			@Override
			public int compareTo(NamesAndPerformance o) {
				if (sortingType == SORTING_TYPE_NONE) {
					return 0;
				} else if (sortingType == SORTING_TYPE_TOP_K) {
					return Double.compare(o.performance, performance);
				} else {
					return Double.compare(performance, o.performance);
				}
			}
		}

		// collect all names and performances
		List<NamesAndPerformance> namesAndPerformances = new LinkedList<NamesAndPerformance>();
		for (int i = 0; i < dataTable.getNumberOfRows(); i++) {
			DataTableRow row = dataTable.getRow(i);
			String names = dataTable.getValueAsString(row, attributeNamesIndex);
			double performance = 1.0d;
			if (sortingDimensionIndex >= 0) {
				performance = row.getValue(sortingDimensionIndex);
			}
			namesAndPerformances.add(new NamesAndPerformance(names, performance));
		}

		// sorting
		Collections.sort(namesAndPerformances);

		// create counters
		Map<String, AtomicInteger> counters = new HashMap<String, AtomicInteger>();
		int number = 0;
		for (NamesAndPerformance namesAndPerformance : namesAndPerformances) {
			if ((sortingType != SORTING_TYPE_NONE) && (number >= sortingK)) {
				break;
			}
			String[] names = namesAndPerformance.names.split(",");
			for (String name : names) {
				name = name.trim();
				AtomicInteger counter = counters.get(name);
				if (counter == null) {
					counter = new AtomicInteger(1);
					counters.put(name, counter);
				} else {
					counter.incrementAndGet();
				}
			}
			number++;
		}

		// calculate weights
		AttributeWeights weights = new AttributeWeights();
		for (Map.Entry<String, AtomicInteger> entry : counters.entrySet()) {
			String name = entry.getKey();
			int currentCount = entry.getValue().intValue();
			weights.setWeight(name, (double) currentCount / (double) number);
		}

		// return result
		return weights;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(AbstractWeighting.PARAMETER_NORMALIZE_WEIGHTS,
				"Activates the normalization of all weights.", true, false));

		types.add(new ParameterTypeString(
				PARAMETER_LOG_NAME,
				"The name of the ProcessLog operator which generated the log data which should be transformed (empty: use first found data table).",
				true, false));

		types.add(new ParameterTypeString(PARAMETER_ATTRIBUTE_NAMES_COLUMN,
				"The column of the statistics (Process Log) containing the attribute names.", false, false));

		types.add(new ParameterTypeCategory(PARAMETER_SORTING_TYPE,
				"Indicates if the logged values should be sorted according to the specified dimension.", SORTING_TYPES,
				SORTING_TYPE_NONE));

		ParameterType type = new ParameterTypeString(PARAMETER_SORTING_DIMENSION,
				"If the sorting type is set to top-k or bottom-k, this dimension is used for sorting.", true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SORTING_TYPE, SORTING_TYPES, true,
				SORTING_TYPE_TOP_K, SORTING_TYPE_BOTTOM_K));
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_SORTING_K,
				"If the sorting type is set to top-k or bottom-k, this number of results will be kept.", 1,
				Integer.MAX_VALUE, 100);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SORTING_TYPE, SORTING_TYPES, false,
				SORTING_TYPE_TOP_K, SORTING_TYPE_BOTTOM_K));
		types.add(type);

		return types;
	}
}
