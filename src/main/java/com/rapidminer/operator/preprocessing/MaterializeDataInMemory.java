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
package com.rapidminer.operator.preprocessing;

import java.util.Iterator;
import java.util.List;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSetBuilder.DataManagement;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.MemoryCleanUp;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.generator.ExampleSetGenerator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.parameter.internal.DataManagementParameterHelper;


/**
 * Creates a fresh and clean copy of the data in memory. Might be very useful in combination with
 * the {@link MemoryCleanUp} operator after large preprocessing trees using lot of views or data
 * copies.
 *
 * @author Ingo Mierswa
 */
public class MaterializeDataInMemory extends AbstractDataProcessing {

	public MaterializeDataInMemory(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		int dataManagement;
		DataManagement newDataManagement = DataManagement.AUTO;
		if (Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))) {
			dataManagement = getParameterAsInt(ExampleSetGenerator.PARAMETER_DATAMANAGEMENT);
		} else {
			dataManagement = DataRowFactory.TYPE_COLUMN_VIEW;
			newDataManagement = DataManagementParameterHelper.getSelectedDataManagement(this);
		}
		ExampleSet createdSet = materialize(exampleSet, dataManagement, newDataManagement);
		return createdSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		DataManagementParameterHelper.addParameterTypes(types, this);
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				MaterializeDataInMemory.class, null);
	}

	/**
	 * Creates a materialized copy of the given example set, i.e., a hard copy with all unnecessary
	 * abstraction layers being removed. The data management strategy will be the same as in the
	 * current example set. If you want to use a different strategy call
	 * {@link #materializeExampleSet(ExampleSet, int)} instead.
	 *
	 * @param exampleSet
	 *            the example set to materialize
	 * @return the materialized example set
	 * @throws UndefinedParameterError
	 */
	public static ExampleSet materializeExampleSet(ExampleSet exampleSet) {
		return materialize(exampleSet, findDataRowType(exampleSet));
	}

	/**
	 * Creates a materialized copy of the given example set, i.e., a hard copy with all unnecessary
	 * abstraction layers being removed.
	 *
	 * @param exampleSet
	 *            the example set to materialize
	 * @param dataManagement
	 *            the data management strategy (see {@link DataRowFactory} for available types)
	 * @return the materialized example set
	 * @throws UndefinedParameterError
	 */
	public static ExampleSet materializeExampleSet(ExampleSet exampleSet, int dataManagement)
			throws UndefinedParameterError {
		return materialize(exampleSet, dataManagement);
	}

	/**
	 * Creates a materialized copy of the given example set, i.e., a hard copy with all unnecessary
	 * abstraction layers being removed.
	 *
	 * @param exampleSet
	 *            the example set to materialize
	 * @param dataManagement
	 *            the data management strategy
	 * @return the materialized example set
	 */
	private static ExampleSet materialize(ExampleSet exampleSet, int dataManagement) {
		return materialize(exampleSet, dataManagement, DataManagement.AUTO);
	}

	/**
	 * Creates a materialized copy of the given example set, i.e., a hard copy with all unnecessary
	 * abstraction layers being removed.
	 *
	 * @param exampleSet
	 *            the example set to materialize
	 * @param dataManagement
	 *            the data management strategy
	 * @param newDataManagement
	 *            the new data management strategy
	 * @return the materialized example set
	 */
	private static ExampleSet materialize(ExampleSet exampleSet, int dataManagement, DataManagement newDataManagement) {
		// create new attributes
		Attribute[] sourceAttributes = new Attribute[exampleSet.getAttributes().allSize()];
		Attribute[] targetAttributes = new Attribute[exampleSet.getAttributes().allSize()];
		String[] targetRoles = new String[targetAttributes.length];

		Iterator<AttributeRole> iterator = exampleSet.getAttributes().allAttributeRoles();
		for (int i = 0; i < sourceAttributes.length; i++) {
			AttributeRole sourceRole = iterator.next();
			sourceAttributes[i] = sourceRole.getAttribute();
			targetAttributes[i] = AttributeFactory.createAttribute(sourceAttributes[i].getName(),
					sourceAttributes[i].getValueType());

			if (sourceAttributes[i].isNominal()) {
				targetAttributes[i].setMapping((NominalMapping) sourceAttributes[i].getMapping().clone());
			}

			if (sourceRole.isSpecial()) {
				targetRoles[i] = sourceRole.getSpecialName();
			}

			targetAttributes[i].getAnnotations().addAll(sourceAttributes[i].getAnnotations());
		}

		// size table by setting number of rows and add attributes
		ExampleSetBuilder builder = ExampleSets.from(targetAttributes);

		// copy columnwise if beta features are activated and dataManagment is double array or
		// column view
		// if datamanagement is not one of the two then there can be value changes when copying to a
		// "smaller" row which we need to keep
		if (Boolean.valueOf(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))
				|| (dataManagement != DataRowFactory.TYPE_DOUBLE_ARRAY
						&& dataManagement != DataRowFactory.TYPE_COLUMN_VIEW)) {
			builder.withExpectedSize(exampleSet.size());
			DataRowFactory rowFactory = new DataRowFactory(dataManagement, '.');
			
			// copying data differently for sparse and non sparse for speed reasons
			if (isSparseType(dataManagement)) {
				for (Example example : exampleSet) {
					DataRow targetRow = rowFactory.create(targetAttributes.length);
					for (int i = 0; i < sourceAttributes.length; i++) {
						double value = example.getValue(sourceAttributes[i]);
						// we have a fresh sparse row, so everything is currently empty and we only
						// need to set non default value attributes to avoid unnecessary binary
						// searchs
						if (value != 0) {
							targetRow.set(targetAttributes[i], value);
						}
					}
					builder.addDataRow(targetRow);
				}
			} else {
				// dense data we copy entirely without condition
				for (Example example : exampleSet) {
					DataRow targetRow = rowFactory.create(targetAttributes.length);
					for (int i = 0; i < sourceAttributes.length; i++) {
						targetRow.set(targetAttributes[i], example.getValue(sourceAttributes[i]));
					}
					builder.addDataRow(targetRow);
				}
			}
		} else {
			builder.withBlankSize(exampleSet.size());
			builder.withOptimizationHint(newDataManagement);
			for (int i = 0; i < sourceAttributes.length; i++) {
				final int index = i;
				builder.withColumnFiller(targetAttributes[i],
						j -> exampleSet.getExample(j).getValue(sourceAttributes[index]));
			}
		}

		// create and return result
		for (int i = 0; i < targetAttributes.length; i++) {
			builder.withRole(targetAttributes[i], targetRoles[i]);
		}
		ExampleSet createdSet = builder.build();
		createdSet.getAnnotations().addAll(exampleSet.getAnnotations());
		return createdSet;
	}

	/**
	 * Returns whether the given type is sparse.
	 */
	private static boolean isSparseType(int dataRowType) {
		switch (dataRowType) {
			case DataRowFactory.TYPE_BOOLEAN_SPARSE_ARRAY:
			case DataRowFactory.TYPE_BYTE_SPARSE_ARRAY:
			case DataRowFactory.TYPE_DOUBLE_SPARSE_ARRAY:
			case DataRowFactory.TYPE_FLOAT_SPARSE_ARRAY:
			case DataRowFactory.TYPE_INT_SPARSE_ARRAY:
			case DataRowFactory.TYPE_LONG_SPARSE_ARRAY:
			case DataRowFactory.TYPE_SHORT_SPARSE_ARRAY:
			case DataRowFactory.TYPE_SPARSE_MAP:
				return true;
			default:
				return false;
		}
	}

	/**
	 * This method determines the current used data row implementation in RapidMiner's backend.
	 */
	private static int findDataRowType(ExampleSet exampleSet) {
		if (exampleSet.size() > 0) {
			// then determine current representation: get first row
			DataRow usedRow = exampleSet.getExample(0).getDataRow();
			if (usedRow != null) {
				return usedRow.getType();
			}
		}
		// default type
		return DataRowFactory.TYPE_DOUBLE_ARRAY;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}
}
