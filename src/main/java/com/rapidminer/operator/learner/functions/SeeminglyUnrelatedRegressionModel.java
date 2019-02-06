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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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


/**
 * This is the model of a SUR regression. It contains the used attributes, the names of the target
 * labels and the coefficients.
 *
 * @author Sebastian Land
 *
 */
public class SeeminglyUnrelatedRegressionModel extends PredictionModel {

	private static final long serialVersionUID = 4843759046775802520L;

	private static final int OPERATOR_PROGRESS_STEPS = 20_000;

	private ArrayList<String[]> usedAttributeNames;
	private ArrayList<String> labelNames;
	private double[] coefficients;
	
	protected SeeminglyUnrelatedRegressionModel(ExampleSet trainingExampleSet, ArrayList<String[]> usedAttributeNames,
			ArrayList<String> labelNames, double[] coefficients) {
		super(trainingExampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.usedAttributeNames = usedAttributeNames;
		this.labelNames = labelNames;
		this.coefficients = coefficients;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		checkCompatibility(exampleSet);
		exampleSet = (ExampleSet) exampleSet.clone();

		Set<String> usedLabelNames = new HashSet<>();

		// creating labels
		Attribute[] predictedLabels = new Attribute[labelNames.size()];
		for (int i = 0; i < labelNames.size(); i++) {
			String labelName = generateLabelName(usedLabelNames, labelNames.get(i), i + 1);

			predictedLabels[i] = AttributeFactory.createAttribute("prediction(" + labelName + ")", Ontology.REAL);
			exampleSet.getExampleTable().addAttribute(predictedLabels[i]);
			exampleSet.getAttributes().addRegular(predictedLabels[i]);
			exampleSet.getAttributes().setSpecialAttribute(predictedLabels[i], "prediction_" + labelName);
		}

		// retrieving used attributes
		Attribute[][] usedAttributes = new Attribute[usedAttributeNames.size()][];
		Attributes attributes = exampleSet.getAttributes();
		for (int i = 0; i < usedAttributeNames.size(); i++) {
			String[] currentAttributeNames = usedAttributeNames.get(i);
			Attribute[] regressionAttributes = new Attribute[currentAttributeNames.length];
			for (int j = 0; j < currentAttributeNames.length; j++) {
				regressionAttributes[j] = attributes.get(currentAttributeNames[j]);
			}
			usedAttributes[i] = regressionAttributes;
		}

		// initialize progress
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;

		// perform prediction
		for (Example example : exampleSet) {
			int coefficientOffset = 0;
			for (int i = 0; i < predictedLabels.length; i++) {
				// adding bias
				double predictedValue = coefficients[coefficientOffset];
				coefficientOffset++;

				// calculating regression
				for (int j = 0; j < usedAttributes[i].length; j++) {
					predictedValue += example.getValue(usedAttributes[i][j]) * coefficients[coefficientOffset + j];
				}
				coefficientOffset += usedAttributes[i].length;
				example.setValue(predictedLabels[i], predictedValue);
			}
			
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
		return exampleSet;
	}

	@Override
	/**
	 * This method isn't called at all, since we have overridden the calling method.
	 */
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		return null;
	}

	public double[] getCoefficients(String labelName) {
		int offset = 0;
		int length = 0;
		int i = 0;
		for (String label : labelNames) {
			String[] selectedAttributes = usedAttributeNames.get(i);
			length = selectedAttributes.length;
			if (label.equals(labelName)) {
				break;
			}
			offset += 1 + length;
			i++;
		}
		if (offset < this.coefficients.length) {
			double[] coefficients = new double[length + 1];
			for (int j = 0; j < coefficients.length - 1; j++) {
				coefficients[j] = this.coefficients[offset + j + 1];
			}
			coefficients[coefficients.length - 1] = this.coefficients[offset];
			return coefficients;
		}
		return null;
	}

	public String[] getSelectedAttributeNames(String labelName) {
		int i = 0;
		for (String label : labelNames) {
			if (label.equals(labelName)) {
				return usedAttributeNames.get(i);
			}
			i++;
		}
		return null;
	}

	public List<String> getLabelNames() {
		return labelNames;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		int j = 0;
		int coeffIndex = 0;
		for (String labelName : labelNames) {
			result.append(Tools.getLineSeparators(2) + labelName + Tools.getLineSeparators(2));
			String[] selectedAttributes = usedAttributeNames.get(j);

			// bias
			result.append(getCoefficientString(coefficients[coeffIndex], true) + Tools.getLineSeparator());
			coeffIndex++;

			// coefficients
			for (int i = 0; i < selectedAttributes.length; i++) {
				result.append(getCoefficientString(coefficients[coeffIndex], false) + " * " + selectedAttributes[i]
						+ Tools.getLineSeparator());
				coeffIndex++;
			}
			j++;
		}
		return result.toString();
	}
	
	/**
	 * Generates label names that will be used in the prediction attributes.
	 * Resolves collisions between label names by generating a unique postfix if necessary.
	 * If all label names are different, this method does not change the names.
	 * 
	 * @param usedLabelNames
	 *             Already accepted unique label names
	 * @param originalLabelName 
	 *             The label name to be inserted
	 * @param portIndex
	 *             The input port index of the unrelated ExampleSet holding {@code originalLabelName} as label
	 * @return
	 */
	private static String generateLabelName(Set<String> usedLabelNames, String originalLabelName, int portIndex) {
		String labelName = originalLabelName;
		
		if (!usedLabelNames.contains(labelName)) {
			usedLabelNames.add(labelName);
			return labelName;
		}
		
		originalLabelName = originalLabelName + "_port" + portIndex;
		labelName = originalLabelName;
		
		int i = 1;
		while (usedLabelNames.contains(labelName)) {
			labelName = originalLabelName + "_" + i ++;
		}
		usedLabelNames.add(labelName);
		return labelName;
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
}
