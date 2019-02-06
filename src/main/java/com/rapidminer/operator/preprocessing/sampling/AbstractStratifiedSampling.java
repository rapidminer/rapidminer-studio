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
package com.rapidminer.operator.preprocessing.sampling;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SimpleExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.ListDataRowReader;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.RandomGenerator;


/**
 * Abstract superclass of stratified sampling operators. Provides functionality for sampling,
 * subclasses have to provide ratio via getRatio()
 *
 * @author Ingo Mierswa, Sebastian Land Exp $
 * @deprecated since 7.3, use {@link StratifiedSamplingOperator} instead
 */
@Deprecated
public abstract class AbstractStratifiedSampling extends AbstractSamplingOperator {

	public AbstractStratifiedSampling(OperatorDescription description) {
		super(description);
	}

	/**
	 * This method should return the ratio used for stratifiedSampling
	 */
	public abstract double getRatio(ExampleSet exampleSet) throws OperatorException;

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// perform stratified sampling
		SplittedExampleSet splittedExampleSet = new SplittedExampleSet(exampleSet, getRatio(exampleSet),
				SplittedExampleSet.STRATIFIED_SAMPLING,
				getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
				getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED));
		splittedExampleSet.selectSingleSubset(0);

		// fill new table
		List<DataRow> dataList = new LinkedList<DataRow>();
		Iterator<Example> reader = splittedExampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			dataList.add(example.getDataRow());
			checkForStop();
		}

		List<Attribute> attributes = Arrays.asList(splittedExampleSet.getExampleTable().getAttributes());
		ExampleTable exampleTable = new MemoryExampleTable(attributes, new ListDataRowReader(dataList.iterator()));

		// regular attributes
		List<Attribute> regularAttributes = new LinkedList<Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			regularAttributes.add(attribute);
		}

		// special attributes
		ExampleSet result = new SimpleExampleSet(exampleTable, regularAttributes);
		Iterator<AttributeRole> special = exampleSet.getAttributes().specialAttributes();
		while (special.hasNext()) {
			AttributeRole role = special.next();
			result.getAttributes().setSpecialAttribute(role.getAttribute(), role.getSpecialName());
		}

		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}
}
