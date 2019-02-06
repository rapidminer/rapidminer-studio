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
package com.rapidminer.operator.preprocessing.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.ProcessTools;


/**
 * This operator removed duplicate examples from an example set by comparing all examples with each
 * other on basis of the specified attributes.
 * 
 * @author Ingo Mierswa, Sebastian Land, Zoltan Prekopcsak
 */
public class RemoveDuplicates extends AbstractDataProcessing {

	/** parameter to define the handling of missing values */
	private static final String PARAMETER_TREAT_MISSING_VALUES_AS_DUPLICATES = "treat_missing_values_as_duplicates";

	/** The first of their kind */
	private static final int NO_DUPLICATE = 0;

	/** Duplicate entries are marked with this */
	private static final int DUPLICATE = 1;

	/** The duplicates */
	private final OutputPort duplicateSetOutput = getOutputPorts().createPort("duplicates");

	private AttributeSubsetSelector subsetSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());

	public RemoveDuplicates(OperatorDescription description) {
		super(description);
		// add metadata to the duplicate output
		getTransformer().addRule(new PassThroughRule(getExampleSetInputPort(), duplicateSetOutput, false) {

			@Override
			public MetaData modifyMetaData(MetaData metaData) {
				if (metaData instanceof ExampleSetMetaData) {
					try {
						return RemoveDuplicates.this.modifyMetaData((ExampleSetMetaData) metaData);
					} catch (UndefinedParameterError e) {
						return metaData;
					}
				} else {
					return metaData;
				}
			}
		});
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		metaData.getNumberOfExamples().reduceByUnknownAmount();
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// partition: 0 select, 1 deselect
		int[] partition = new int[exampleSet.size()];
		Set<Attribute> compareAttributes = subsetSelector.getAttributeSubset(exampleSet, false);

		// if set is empty: Nothing can be done!
		if (compareAttributes.isEmpty()) {
			throw new UserError(this, 153, 1, 0);
		}

		// Creating hash buckets and check in case of collision if the example is equal to any other
		// in the bucket
		HashMap<Integer, List<Integer>> buckets = new HashMap<>();
		boolean missingsAsDuplicates = getParameterAsBoolean(PARAMETER_TREAT_MISSING_VALUES_AS_DUPLICATES);
		for (int i = 0; i < exampleSet.size(); i++) {
			this.checkForStop();
			Example example = exampleSet.getExample(i);
			int hash = 0;
			for (Attribute attribute : compareAttributes) {
				long bits = Double.doubleToLongBits(example.getValue(attribute));
				hash = hash * 31 + (int) (bits ^ bits >>> 32);
			}
			if (!buckets.containsKey(hash)) {
				buckets.put(hash, Collections.singletonList(Integer.valueOf(i)));
			} else {
				List<Integer> bucketExampleIndicesList = buckets.get(hash);
				for (Integer exampleIndex : bucketExampleIndicesList) {
					boolean equal = true;
					Example compExample = exampleSet.getExample(exampleIndex);
					for (Attribute attribute : compareAttributes) {
						if (missingsAsDuplicates) {
							if (Double.isNaN(example.getValue(attribute)) && Double.isNaN(compExample.getValue(attribute))) {
								continue;
							}
						}
						if (example.getValue(attribute) != compExample.getValue(attribute)) {
							equal = false;
							break;
						}
					}
					if (equal) {
						partition[i] = DUPLICATE;
					}
				}
				if (partition[i] == 0) { // then it is unequal with same hash value
					if (bucketExampleIndicesList.size() == 1) {
						// in case of a collision we have to replace the singeltonList by an
						// extendible collection.
						List<Integer> newList = new ArrayList<>(bucketExampleIndicesList);
						newList.add(Integer.valueOf(i));
						buckets.put(hash, newList);
					} else {
						// in this case we already have put in an ArrayList, because we had to store
						// a second example on a previous collision
						bucketExampleIndicesList.add(i);
					}
				}
			}
		}

		SplittedExampleSet result = new SplittedExampleSet(exampleSet, new Partition(partition, 2));

		// Create duplicates
		if (duplicateSetOutput.isConnected()) {
			SplittedExampleSet duplicates = (SplittedExampleSet) result.clone();
			duplicates.selectSingleSubset(DUPLICATE);
			duplicates.recalculateAllAttributeStatistics();
			duplicateSetOutput.deliver(duplicates);
		}

		result.selectSingleSubset(NO_DUPLICATE);
		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(subsetSelector.getParameterTypes(), true));

		ParameterType type = new ParameterTypeBoolean(PARAMETER_TREAT_MISSING_VALUES_AS_DUPLICATES,
				"If set to true, treats missing values as duplicates", false);
		type.setExpert(false);
		types.add(type);

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), RemoveDuplicates.class,
				null);
	}
}
