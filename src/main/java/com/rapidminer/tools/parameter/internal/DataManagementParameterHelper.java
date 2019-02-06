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
package com.rapidminer.tools.parameter.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.utils.ExampleSetBuilder.DataManagement;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.generator.ExampleSetGenerator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.ParameterService;


/**
 * Helper class for data management parameters. Can translate operator parameters into
 * {@link DataManagement} options and add data management parameters to a parameter list.
 *
 * @author Gisa Schaefer
 * @since 7.4
 */
public final class DataManagementParameterHelper {

	private DataManagementParameterHelper() {
		// utility class constructor
	}

	/**
	 * the options for {@link #PARAMETER_NEW_DATA_MANAGEMENT} coming from {@link DataManagement}
	 */
	public static final String[] NEW_DATA_MANAGMENT_OPTIONS = new String[] { "auto", "memory-optimized", "speed-optimized" };

	/**
	 * Mapping from displayed data management options to {@link DataManagement} options. Must always
	 * be adjusted to {@link #NEW_DATA_MANAGMENT_OPTIONS}.
	 */
	private static final Map<String, DataManagement> DATA_MANAGEMENT_LOOKUP = new HashMap<>();

	static {
		DATA_MANAGEMENT_LOOKUP.put(NEW_DATA_MANAGMENT_OPTIONS[0], DataManagement.AUTO);
		DATA_MANAGEMENT_LOOKUP.put(NEW_DATA_MANAGMENT_OPTIONS[1], DataManagement.MEMORY_OPTIMIZED);
		DATA_MANAGEMENT_LOOKUP.put(NEW_DATA_MANAGMENT_OPTIONS[2], DataManagement.SPEED_OPTIMIZED);
	}

	/**
	 * key for the new data management parameter
	 */
	public static final String PARAMETER_NEW_DATA_MANAGEMENT = "data_management";

	/**
	 * Adds the data management parameters for beta and standard mode to the given list of parameter
	 * types.
	 *
	 * @param types
	 *            the list of parameter types to which the data management parameters should be
	 *            added
	 * @param operator
	 *            the operator for which the parameters are meant
	 */
	public static void addParameterTypes(List<ParameterType> types, Operator operator) {
		ParameterType standard = new ParameterTypeCategory(ExampleSetGenerator.PARAMETER_DATAMANAGEMENT,
				"Determines, how the data is represented internally.", DataRowFactory.TYPE_NAMES,
				DataRowFactory.TYPE_DOUBLE_ARRAY, true);
		standard.registerDependencyCondition(new ParameterCondition(operator, false) {

			@Override
			public boolean isConditionFullfilled() {
				return Boolean.parseBoolean(
						ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT));
			}
		});
		types.add(standard);
		ParameterType beta = new ParameterTypeCategory(PARAMETER_NEW_DATA_MANAGEMENT,
				"The data management optimization to use. Determines, how the data is represented internally. The auto option (default) only compresses data if it is very sparse and otherwise optimizes for speed. Choose speed-optimized if you have enough memory and want to speed up your process. Choose memory-optimized if you have a lot of sparse data that has trouble fitting into memory with auto mode.",
				NEW_DATA_MANAGMENT_OPTIONS, 0, true);
		beta.registerDependencyCondition(new ParameterCondition(operator, false) {

			@Override
			public boolean isConditionFullfilled() {
				return !Boolean.parseBoolean(
						ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT));
			}
		});
		types.add(beta);
	}

	/**
	 * Retrieves the {@link DataManagement} associated to the selected option for the parameter
	 * {@link #PARAMETER_NEW_DATA_MANAGEMENT} of the given operator.
	 *
	 * @param operator
	 *            the operator to which the data management parameter belongs
	 * @return the {@link DataManagement} associated to the selection
	 * @throws UndefinedParameterError
	 *             if the parameter could not be retrieved
	 */
	public static DataManagement getSelectedDataManagement(Operator operator) throws UndefinedParameterError {
		String selected = operator.getParameterAsString(PARAMETER_NEW_DATA_MANAGEMENT);
		return DATA_MANAGEMENT_LOOKUP.get(selected);
	}

}
