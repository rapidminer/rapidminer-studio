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
package com.rapidminer.operator.postprocessing;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;


/**
 * This operator sets all predictions which do not have a higher confidence than the specified one
 * to &quot;unknown&quot; (missing value). This operator is a quite simple version of the
 * CostBasedThresholdLearner which might be useful in simple binominal classification settings
 * (although it does also work for polynominal classifications).
 *
 * @author Ingo Mierswa
 */
public class SimpleUncertainPredictionsTransformation extends AbstractDataProcessing {

	public static final String PARAMETER_CLASS_HANDLING = "class_handling";

	public static final String[] CLASS_HANDLING_MODES = { "balanced", "unbalanced" };

	public static final int CLASS_HANDLING_BALANCED = 0;

	public static final int CLASS_HANDLING_UNBALANCED = 1;

	public static final String PARAMETER_MIN_CONFIDENCE = "min_confidence";

	public static final String PARAMETER_MIN_CONFIDENCES = "min_confidences";

	public static final String PARAMETER_CLASS_VALUE = "class";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public SimpleUncertainPredictionsTransformation(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(new ExampleSetPrecondition(getExampleSetInputPort(), Ontology.VALUE_TYPE,
				Attributes.PREDICTION_NAME, Attributes.CONFIDENCE_NAME));
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// checks
		Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();
		if (predictedLabel == null) {
			throw new UserError(this, 107);
		}
		if (!predictedLabel.isNominal()) {
			throw new UserError(this, 119, predictedLabel, getName());
		}

		switch (getParameterAsInt(PARAMETER_CLASS_HANDLING)) {
			case CLASS_HANDLING_BALANCED:
				double minConfidence = getParameterAsDouble(PARAMETER_MIN_CONFIDENCE);
				for (Example example : exampleSet) {
					double predictionValue = example.getValue(predictedLabel);
					String predictionClass = predictedLabel.getMapping().mapIndex((int) predictionValue);
					double confidence = example.getConfidence(predictionClass);
					if (!Double.isNaN(confidence)) {
						if (confidence < minConfidence) {
							example.setValue(predictedLabel, Double.NaN);
						}
					}
				}
				break;
			case CLASS_HANDLING_UNBALANCED:
				HashMap<String, Double> thresholdMap = new HashMap<String, Double>();
				for (String[] threshold : getParameterList(PARAMETER_MIN_CONFIDENCES)) {
					thresholdMap.put(threshold[0], Double.valueOf(threshold[1]));
				}

				for (Example example : exampleSet) {
					double predictionValue = example.getValue(predictedLabel);
					String predictionClass = predictedLabel.getMapping().mapIndex((int) predictionValue);
					double confidence = example.getConfidence(predictionClass);
					Double threshold = thresholdMap.get(predictionClass);
					if (!Double.isNaN(confidence) && threshold != null) {
						if (confidence < threshold.doubleValue()) {
							example.setValue(predictedLabel, Double.NaN);
						}
					}
				}
				break;
		}

		return exampleSet;
	}

	@Override
	public boolean writesIntoExistingData() {
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			return true;
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected();
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> list = super.getParameterTypes();
		list.add(new ParameterTypeCategory(PARAMETER_CLASS_HANDLING,
				"The mode which defines if all classes are handled equally or if class individual thresholds are set.",
				CLASS_HANDLING_MODES, CLASS_HANDLING_BALANCED, false));
		ParameterType type = new ParameterTypeDouble(PARAMETER_MIN_CONFIDENCE,
				"The minimal confidence necessary for not setting the prediction to 'unknown'.", 0.0d, 1.0d, 0.5d);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_CLASS_HANDLING, CLASS_HANDLING_MODES, true, CLASS_HANDLING_BALANCED));
		type.setExpert(false);
		list.add(type);
		type = new ParameterTypeList(PARAMETER_MIN_CONFIDENCES, "A list which defines individual thresholds for classes.",
				new ParameterTypeString(PARAMETER_CLASS_VALUE,
						"The class for which the confidence threshold should be set."),
				new ParameterTypeDouble(PARAMETER_MIN_CONFIDENCE,
						"The minimal confidence necessary for not setting the prediction to 'unknown'.", 0.0d, 1.0d, 0.5d),
				false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_CLASS_HANDLING, CLASS_HANDLING_MODES, true,
				CLASS_HANDLING_UNBALANCED));
		list.add(type);
		return list;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}
}
