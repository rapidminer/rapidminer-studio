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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.functions.kernel.AbstractMySVMLearner;
import com.rapidminer.operator.learner.functions.kernel.JMySVMLearner;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;


/**
 * Uses the coefficients of the normal vector of a linear SVM as feature weights. In contrast to
 * most of the SVM based operators available in RapidMiner, this one works for multiple classes,
 * too. Please note that the attribute values, however, still have to be numerical. Please use
 * appropriate preprocessing operators in order to ensure this.
 * 
 * @author Ingo Mierswa
 * 
 */
public class SVMWeighting extends AbstractWeighting {

	public SVMWeighting(OperatorDescription description) {
		super(description, true);
	}

	@Override
	protected AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {

		// checks
		Attribute label = exampleSet.getAttributes().getLabel();

		// create and init SVM operator
		JMySVMLearner svmOperator = null;
		try {
			svmOperator = OperatorService.createOperator(JMySVMLearner.class);
		} catch (OperatorCreationException e) {
			throw new UserError(this, 904, "inner SVM operator", e.getMessage());
		}
		svmOperator.setParameter(AbstractMySVMLearner.PARAMETER_KERNEL_TYPE, AbstractMySVMLearner.KERNEL_DOT + "");
		svmOperator.setParameter(AbstractMySVMLearner.PARAMETER_C, getParameterAsDouble(AbstractMySVMLearner.PARAMETER_C)
				+ "");
		svmOperator.setParameter(AbstractMySVMLearner.PARAMETER_CALCULATE_WEIGHTS, "true");

		// calculate weights
		AttributeWeights result = null;

		// regression or binomial case
		if (label.isNumerical() || label.isNominal() && label.getMapping().size() == 2) {
			result = calculateAttributeWeights(svmOperator, exampleSet);
		} else if (label.isNominal()) {
			// polynomial case
			exampleSet.recalculateAttributeStatistics(label);
			int totalClassSizeSum = 0;
			int[] classFrequencies = new int[label.getMapping().size()];
			int counter = 0;
			List<AttributeWeights> allWeights = new LinkedList<>();
			for (String value : label.getMapping().getValues()) {
				int frequency = (int) exampleSet.getStatistics(label, Statistics.COUNT, value);
				classFrequencies[counter++] = frequency;
				totalClassSizeSum += frequency;

				// create temp label (one vs. all)
				Attribute tempLabel = AttributeFactory.createAttribute("temp_label", Ontology.BINOMINAL);
				int positiveIndex = tempLabel.getMapping().mapString("positive");
				int negativeIndex = tempLabel.getMapping().mapString("negative");
				exampleSet.getExampleTable().addAttribute(tempLabel);
				exampleSet.getAttributes().addRegular(tempLabel);
				int currentLabelIndex = label.getMapping().mapString(value);
				for (Example e : exampleSet) {
					int oldLabelValue = (int) e.getValue(label);
					if (oldLabelValue == currentLabelIndex) {
						e.setValue(tempLabel, positiveIndex);
					} else {
						e.setValue(tempLabel, negativeIndex);
					}
				}
				exampleSet.getAttributes().remove(tempLabel);
				exampleSet.getAttributes().setLabel(tempLabel);

				// actual calculating weights for this class
				AttributeWeights currentWeights = calculateAttributeWeights(svmOperator, exampleSet);
				allWeights.add(currentWeights);

				// clean up
				exampleSet.getAttributes().setLabel(label);
				exampleSet.getExampleTable().removeAttribute(tempLabel);
			}

			// build the weighted average for all weights
			result = new AttributeWeights();
			Iterator<String> nameIterator = allWeights.get(0).getAttributeNames().iterator();
			while (nameIterator.hasNext()) {
				String attributeName = nameIterator.next();
				double currentWeightSum = 0.0d;
				counter = 0;
				for (AttributeWeights weights : allWeights) {
					double weight = weights.getWeight(attributeName);
					currentWeightSum += Math.abs(weight) * classFrequencies[counter++];
				}
				result.setWeight(attributeName, currentWeightSum / totalClassSizeSum);
			}
		} else {
			logError("Calculation of SVM weights only possible for numerical or nominal labels.");
		}

		result.setSource(this.getName());
		return result;
	}

	private AttributeWeights calculateAttributeWeights(JMySVMLearner svmOperator, ExampleSet exampleSet)
			throws OperatorException {
		svmOperator.doWork(exampleSet);
		return svmOperator.getWeights(exampleSet);
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_LABEL:
			case POLYNOMINAL_LABEL:
			case NUMERICAL_LABEL:
			case NUMERICAL_ATTRIBUTES:
			case WEIGHTED_EXAMPLES:
				return true;
			default:
				return false;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeDouble(AbstractMySVMLearner.PARAMETER_C, "The SVM complexity weighting factor.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0d));
		return types;
	}
}
