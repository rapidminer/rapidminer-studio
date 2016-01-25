/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


/**
 * This operator removed duplicate examples from an example set by comparing all examples with each
 * other on basis of the specified attributes.
 * 
 * @author Ingo Mierswa, Sebastian Land, Zoltan Prekopcsak
 */
public class RemoveDuplicates extends AbstractDataProcessing {

	/** parameter to define the handling of missing values */
	private static final String PARAMETER_TREAT_MISSING_VALUES_AS_DUPLICATES = "treat_missing_values_as_duplicates";

	private AttributeSubsetSelector subsetSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());

	public RemoveDuplicates(OperatorDescription description) {
		super(description);
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
		for (int i = 0; i < exampleSet.size(); i++) {
			this.checkForStop();
			Example example = exampleSet.getExample(i);
			int hash = 0;
			for (Attribute attribute : compareAttributes) {
				long bits = Double.doubleToLongBits(example.getValue(attribute));
				hash = hash * 31 + (int) (bits ^ (bits >>> 32));
			}
			if (!buckets.containsKey(hash)) {
				buckets.put(hash, Collections.singletonList(Integer.valueOf(i)));
			} else {
				List<Integer> bucketExampleIndicesList = buckets.get(hash);
				for (Integer exampleIndex : bucketExampleIndicesList) {
					boolean equal = true;
					Example compExample = exampleSet.getExample(exampleIndex);
					for (Attribute attribute : compareAttributes) {
						if (getParameterAsBoolean(PARAMETER_TREAT_MISSING_VALUES_AS_DUPLICATES)) {
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
						partition[i] = 1;
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
		result.selectSingleSubset(0);

		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(subsetSelector.getParameterTypes());

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
