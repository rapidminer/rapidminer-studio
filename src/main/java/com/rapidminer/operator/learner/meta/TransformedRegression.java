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
package com.rapidminer.operator.learner.meta;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDReal;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;

import java.util.Iterator;
import java.util.List;


/**
 * This meta learner applies a transformation on the label before the inner regression learner is
 * applied. Please note, that the logistic link function will only work for probabilities or other
 * labels with a range from 0 to 1 exclusive. If a value exceeds this range, it is set to the
 * nearest possible element within this range.
 * 
 * @author Stefan Rueping, Ingo Mierswa
 * @author Stefan Rueping, Ingo Mierswa, Sebastian Land
 */
public class TransformedRegression extends AbstractMetaLearner {

	/**
	 * The parameter name for &quot;Type of transformation to use on the labels (log, exp, transform
	 * to mean 0 and variance 1, rank, or none).&quot;
	 */
	public static final String PARAMETER_TRANSFORMATION_METHOD = "transformation_method";

	/**
	 * The parameter name for &quot;Scale transformed values to mean 0 and standard deviation
	 * 1?&quot;
	 */
	public static final String PARAMETER_Z_SCALE = "z_scale";

	/**
	 * The parameter name for &quot;Interpolate prediction if predicted rank is not an
	 * integer?&quot;
	 */
	public static final String PARAMETER_INTERPOLATE_RANK = "interpolate_rank";

	private final PortPairExtender through = new PortPairExtender("through", getSubprocess(0).getInnerSinks(),
			getOutputPorts());

	public TransformedRegression(OperatorDescription description) {
		super(description);

		through.start();

		getTransformer().addRule(through.makePassThroughRule());
	}

	@Override
	protected MetaData modifyExampleSetMetaData(ExampleSetMetaData unmodifiedMetaData) {
		switch (unmodifiedMetaData.hasSpecial(Attributes.LABEL_NAME)) {
			case NO:
				getTrainingSetInputPort().addError(
						new SimpleMetaDataError(Severity.ERROR, getTrainingSetInputPort(), "special_missing", "label"));
				return unmodifiedMetaData;
			case UNKNOWN:
				getTrainingSetInputPort().addError(
						new SimpleMetaDataError(Severity.WARNING, getTrainingSetInputPort(), "special_unknown", "label"));
				return unmodifiedMetaData;
			case YES:
				AttributeMetaData labelMD = unmodifiedMetaData.getLabelMetaData();
				unmodifiedMetaData.removeAttribute(labelMD);
				AttributeMetaData transformedMD = labelMD.copy();
				transformedMD.setName("transformation(" + labelMD.getName() + ")");
				// TODO: Transform values instead of setting unknown
				transformedMD.setValueSetRelation(SetRelation.UNKNOWN);
				transformedMD.setMean(new MDReal());

				unmodifiedMetaData.addAttribute(transformedMD);
				return unmodifiedMetaData;
			default:
				return unmodifiedMetaData;
		}
	}

	@Override
	public Model learn(ExampleSet inputSet) throws OperatorException {
		int method = getParameterAsInt(PARAMETER_TRANSFORMATION_METHOD);
		double[] rank = null;
		double mean = 0.0d;
		double stddev = 1.0d;

		Attribute label = inputSet.getAttributes().getLabel();
		inputSet.recalculateAttributeStatistics(label);

		ExampleSet eSet = (ExampleSet) inputSet.clone();
		Attribute tempLabel = AttributeFactory.createAttribute("transformation(" + label.getName() + ")", Ontology.REAL);
		eSet.getExampleTable().addAttribute(tempLabel);

		// 1. Set new regression labels
		Iterator<Example> r = eSet.iterator();
		switch (method) {
			case TransformedRegressionModel.LOG:
				double offset = 1.0d - inputSet.getStatistics(label, Statistics.MINIMUM);
				rank = new double[1];
				rank[0] = offset;
				while (r.hasNext()) {
					Example e = r.next();
					e.setValue(tempLabel, Math.log(offset + e.getValue(label)));
				}
				break;
			case TransformedRegressionModel.LOG_LINK:
				while (r.hasNext()) {
					Example e = r.next();
					double value = e.getValue(label);
					if (value >= 1d) {
						value = 0.99999999999d;
					}
					if (value <= 0d) {
						value = 0.00000000001d;
					}
					e.setValue(tempLabel, Math.log(value / (1 - value)));
				}
				break;
			case TransformedRegressionModel.EXP:
				while (r.hasNext()) {
					Example e = r.next();
					e.setValue(tempLabel, Math.exp(e.getValue(label)));
				}
				break;
			case TransformedRegressionModel.RANK:
				double[] dummy = new double[eSet.size()];
				int i = 0;
				while (r.hasNext()) {
					Example e = r.next();
					dummy[i] = e.getValue(label);
					i++;
				}
				java.util.Arrays.sort(dummy);
				// remove double entries
				i = 0;
				for (int j = 0; j < dummy.length; j++) {
					if (dummy[i] != dummy[j]) {
						i++;
						dummy[i] = dummy[j];
					}
				}
				rank = new double[i + 1];
				for (int j = 0; j < i + 1; j++) {
					rank[j] = dummy[j];
				}

				r = eSet.iterator();
				while (r.hasNext()) {
					Example e = r.next();
					e.setValue(tempLabel, java.util.Arrays.binarySearch(rank, e.getValue(label)));
				}
				// }
				break;
			case TransformedRegressionModel.NONE:
				// just for convenience...
				while (r.hasNext()) {
					Example e = r.next();
					e.setValue(tempLabel, e.getValue(label));
				}
				break;
			default:
				// cannot happen
				break;
		}

		if (getParameterAsBoolean(PARAMETER_Z_SCALE)) {
			eSet.recalculateAttributeStatistics(tempLabel);
			mean = eSet.getStatistics(tempLabel, Statistics.AVERAGE);
			stddev = eSet.getStatistics(tempLabel, Statistics.VARIANCE);
			if (stddev <= 0.0d) {
				// catch numerical errors
				stddev = 1.0d;
			}
			;
			r = eSet.iterator();
			while (r.hasNext()) {
				Example e = r.next();
				e.setValue(tempLabel, (e.getValue(tempLabel) - mean) / stddev);
			}
		}
		;

		// 2. Apply learner
		eSet.getAttributes().remove(label);
		eSet.getAttributes().addRegular(tempLabel);
		eSet.getAttributes().setLabel(tempLabel);
		Model model = applyInnerLearner(eSet);
		TransformedRegressionModel resultModel = new TransformedRegressionModel(inputSet, method, rank, model,
				getParameterAsBoolean(PARAMETER_Z_SCALE), mean, stddev, getParameterAsBoolean(PARAMETER_INTERPOLATE_RANK));

		// passing inner data
		through.passDataThrough();

		return resultModel;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(
				PARAMETER_TRANSFORMATION_METHOD,
				"Type of transformation to use on the labels (log, exp, transform to mean 0 and variance 1, rank, or none).",
				TransformedRegressionModel.METHODS, TransformedRegressionModel.LOG);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_Z_SCALE,
				"If checked the values will be normalized to mean 0 and standard deviation 1.", false);
		type.setExpert(true);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_INTERPOLATE_RANK,
				"If checked and predicted rank is not an integer, it will be interpolated.", true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_TRANSFORMATION_METHOD,
				TransformedRegressionModel.METHODS, false, TransformedRegressionModel.RANK));
		type.setExpert(true);
		types.add(type);
		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case POLYNOMINAL_LABEL:
			case BINOMINAL_LABEL:
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
				return false;
			default:
				return true;
		}
	}
}
