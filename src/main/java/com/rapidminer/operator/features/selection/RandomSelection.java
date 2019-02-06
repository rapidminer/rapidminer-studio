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
package com.rapidminer.operator.features.selection;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.RandomGenerator;

import java.util.List;


/**
 * This operator selects a randomly chosen number of attributes randomly from the input example set.
 * This can be useful in combination with a ParameterIteration operator or can be used as a baseline
 * for significance test comparisons for feature selection techniques.
 * 
 * @author Ingo Mierswa
 */
public class RandomSelection extends AbstractFeatureSelection {

	public static final String PARAMETER_USE_FIXED_NUMBER_OF_ATTRIBUTES = "use_fixed_number_of_attributes";
	public static final String PARAMETER_NUMBER_OF_ATTRIBUTES = "number_of_attributes";

	public RandomSelection(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		ExampleSet result = (ExampleSet) exampleSet.clone();

		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		int number = 0;
		if (getParameterAsBoolean(PARAMETER_USE_FIXED_NUMBER_OF_ATTRIBUTES)) {
			number = getParameterAsInt(PARAMETER_NUMBER_OF_ATTRIBUTES);
		} else {
			number = random.nextIntInRange(1, result.getAttributes().size() + 1);
			if (number > result.getAttributes().size()) {
				throw new UserError(this, 125, number, result.getAttributes().size());
			}
		}

		while (result.getAttributes().size() > number) {
			int toDeleteIndex = random.nextIntInRange(0, result.getAttributes().size());
			Attribute toDeleteAttribute = null;
			int counter = 0;
			for (Attribute attribute : result.getAttributes()) {
				if (counter >= toDeleteIndex) {
					toDeleteAttribute = attribute;
					break;
				}
				counter++;
			}
			if (toDeleteAttribute != null) {
				result.getAttributes().remove(toDeleteAttribute);
			}
		}

		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_USE_FIXED_NUMBER_OF_ATTRIBUTES,
				"Indicates if a fixed number of attributes should be selected.", false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_ATTRIBUTES,
				"The number of attributes which should be randomly selected.", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_FIXED_NUMBER_OF_ATTRIBUTES, true,
				true));
		type.setExpert(false);
		types.add(type);
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}
}
