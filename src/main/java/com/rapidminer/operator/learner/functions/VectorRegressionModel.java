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
package com.rapidminer.operator.learner.functions;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;

import Jama.Matrix;


/**
 * The model for vector linear regression.
 *
 * @author Tobias Malbrecht, Sebastian Land
 */
public class VectorRegressionModel extends PredictionModel {

	private static final long serialVersionUID = 8381268071090932037L;

	private static final int OPERATOR_PROGRESS_STEPS = 1000;

	private String[] labelNames;

	private String[] attributeNames;

	Matrix coefficients;

	private boolean useIntercept = true;

	public VectorRegressionModel(ExampleSet exampleSet, String[] labelNames, Matrix coefficients, boolean useIntercept) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.labelNames = labelNames;
		this.attributeNames = com.rapidminer.example.Tools.getRegularAttributeNames(exampleSet);
		this.coefficients = coefficients;
		this.useIntercept = useIntercept;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		checkCompatibility(exampleSet);
		exampleSet = (ExampleSet) exampleSet.clone();

		// creating labels
		Attribute[] predictedLabels = new Attribute[labelNames.length];
		for (int i = 0; i < labelNames.length; i++) {
			predictedLabels[i] = AttributeFactory.createAttribute("prediction(" + labelNames[i] + ")", Ontology.NUMERICAL);
			exampleSet.getExampleTable().addAttribute(predictedLabels[i]);
			exampleSet.getAttributes().addRegular(predictedLabels[i]);
			exampleSet.getAttributes().setSpecialAttribute(predictedLabels[i], "prediction_" + labelNames[i]);
		}

		// retrieving attributes
		Attributes attributes = exampleSet.getAttributes();
		Attribute[] usedAttributes = new Attribute[attributeNames.length];
		for (int i = 0; i < attributeNames.length; i++) {
			usedAttributes[i] = attributes.get(attributeNames[i]);
		}

		// initialize progress
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;

		// now calculate predicted value
		for (Example example : exampleSet) {
			for (int i = 0; i < predictedLabels.length; i++) {
				double predictedLabel = useIntercept ? coefficients.get(0, i) : 0;
				if (useIntercept) {
					for (int j = 1; j <= attributeNames.length; j++) {
						predictedLabel += example.getValue(usedAttributes[j - 1]) * coefficients.get(j, i);
					}
				} else {
					for (int j = 0; j < attributeNames.length; j++) {
						predictedLabel += example.getValue(usedAttributes[j]) * coefficients.get(j, i);
					}
				}
				example.setValue(predictedLabels[i], predictedLabel);
			}

			// trigger progress
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
		return exampleSet;
	}

	public String[] getLabelNames() {
		return labelNames;
	}

	public String[] getAttributeNames() {
		return attributeNames;
	}

	public double[] getCoefficients(String labelName) {
		double[] coefficients = new double[this.coefficients.getRowDimension()];
		int i = 0;
		for (String label : labelNames) {
			if (label.equals(labelName)) {
				for (int j = 0; j < coefficients.length - 1; j++) {
					coefficients[j] = this.coefficients.get(j + 1, i);
				}
				coefficients[coefficients.length - 1] = this.coefficients.get(0, i);
				return coefficients;
			}
			i++;
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();

		for (int i = 0; i < labelNames.length; i++) {
			result.append(labelNames[i] + " = ");
			boolean first = true;
			for (int j = useIntercept ? 1 : 0; j < attributeNames.length + (useIntercept ? 1 : 0); j++) {
				result.append(getCoefficientString(coefficients.get(j, i), first) + " * "
						+ attributeNames[j - (useIntercept ? 1 : 0)] + "  ");
				first = false;
			}
			if (useIntercept) {
				result.append(getCoefficientString(coefficients.get(0, i), false));
			}
			result.append("\n");
		}

		return result.toString();
	}

	private String getCoefficientString(double coefficient, boolean first) {
		if (!first) {
			if (coefficient >= 0) {
				return "+ " + Tools.formatNumber(Math.abs(coefficient));
			} else {
				return "- " + Tools.formatNumber(Math.abs(coefficient));
			}
		} else {
			if (coefficient >= 0) {
				return "  " + Tools.formatNumber(Math.abs(coefficient));
			} else {
				return "- " + Tools.formatNumber(Math.abs(coefficient));
			}
		}
	}

	@Override
	/**
	 * This method won't be called at all, because we overwrite the calling method.
	 */
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		return null;
	}
}
