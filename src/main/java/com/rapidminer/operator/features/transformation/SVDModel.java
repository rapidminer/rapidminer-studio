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
package com.rapidminer.operator.features.transformation;

import java.util.Arrays;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.AbstractModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;

import Jama.Matrix;


/**
 * This is the transformation model of the principal components analysis. The number of components
 * is initially specified by the <code>PCA</code>. Additionally you can specify the number of
 * components in the <code>ModelApplier</code>. You can add two prediction parameter:
 * <ul>
 * <li><b>variance_threshold</b> <i>double</i> Specify a new threshold for the cumulative variance
 * of the principal components.
 * <li><b>number_of_components</b> <i>integer</i> Specify a lower number of components
 * <li><b>keep_attributes</b> <i>true|false</i> If true, the original features are not removed.
 * </ul>
 *
 * @author Sebastian Land, Daniel Hakenjos, Ingo Mierswa
 * @see PCA
 */
public class SVDModel extends AbstractModel implements ComponentWeightsCreatable {

	private static final long serialVersionUID = 5424591594470376525L;

	private static final int OPERATOR_PROGRESS_STEPS = 10_000;

	private Matrix vMatrix;

	private double[] singularValues;

	private double[] cumulativeSingularValueProportion;

	private double singularValuesSum;

	private String[] attributeNames;

	private boolean manualNumber;

	private int numberOfComponents = -1;

	private double proportionThreshold;

	private boolean useLegacyNames = false;

	// -----------------------------------

	private boolean keepAttributes = false;

	public SVDModel(ExampleSet exampleSet, double[] singularValues, Matrix vMatrix) {
		super(exampleSet);

		this.vMatrix = vMatrix;
		this.singularValues = singularValues;

		this.keepAttributes = false;
		this.attributeNames = new String[exampleSet.getAttributes().size()];
		int counter = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributeNames[counter] = attribute.getName();
			counter++;
		}

		// compute cumulative values
		cumulativeSingularValueProportion = new double[singularValues.length];
		// insert cumulative sum of singular values
		singularValuesSum = 0.0d;
		for (int i = 0; i < singularValues.length; i++) {
			singularValuesSum += singularValues[i];
			cumulativeSingularValueProportion[i] = singularValuesSum;
		}

		// now reduce to proportion
		for (int i = 0; i < singularValues.length; i++) {
			cumulativeSingularValueProportion[i] /= singularValuesSum;
		}

	}

	public String[] getAttributeNames() {
		return attributeNames;
	}

	public double getSingularValue(int index) {
		return this.singularValues[index];
	}

	public double getSingularValueProportion(int index) {
		return this.singularValues[index] / singularValuesSum;
	}

	public double getCumulativeSingularValue(int index) {
		return this.cumulativeSingularValueProportion[index] * singularValuesSum;
	}

	public double getCumulativeSingularValueProportion(int index) {
		return this.cumulativeSingularValueProportion[index];
	}

	public double getSingularVectorValue(int vectorIndex, int component) {
		return this.vMatrix.get(component, vectorIndex);
	}

	public double getProportionThreshold() {
		return this.proportionThreshold;
	}

	public int getMaximumNumberOfComponents() {
		return attributeNames.length;
	}

	/**
	 * This returns the total number of possible components.
	 */
	public int getNumberOfComponents() {
		return singularValues.length;
	}

	public void setVarianceThreshold(double threshold) {
		this.manualNumber = false;
		this.proportionThreshold = threshold;
		this.numberOfComponents = -1;
	}

	public void setNumberOfComponents(int numberOfComponents) {
		this.proportionThreshold = 0.95;
		this.manualNumber = true;
		this.numberOfComponents = numberOfComponents;
	}

	@Override
	public ExampleSet apply(ExampleSet inputExampleSet) throws OperatorException {
		ExampleSet exampleSet = (ExampleSet) inputExampleSet.clone();
		Attributes attributes = exampleSet.getAttributes();
		if (attributeNames.length != attributes.size()) {
			throw new UserError(null, 133, numberOfComponents, attributes.size());
		}

		// remember attributes that have been removed during training. These will be removed lateron
		Attribute[] inputAttributes = new Attribute[getTrainingHeader().getAttributes().size()];
		int d = 0;
		for (Attribute oldAttribute : getTrainingHeader().getAttributes()) {
			inputAttributes[d] = attributes.get(oldAttribute.getName());
			d++;
		}

		// determining number of used components
		int numberOfUsedComponents = -1;
		if (manualNumber) {
			numberOfUsedComponents = numberOfComponents;
		} else {
			if (proportionThreshold == 0.0d) {
				numberOfUsedComponents = -1;
			} else {
				numberOfUsedComponents = 0;
				while (cumulativeSingularValueProportion[numberOfUsedComponents] < proportionThreshold) {
					numberOfUsedComponents++;
				}
				numberOfUsedComponents++;
			}
		}
		// if nothing defined or number exceeds maximal number of possible components
		if (numberOfUsedComponents == -1 || numberOfUsedComponents > getNumberOfComponents()) {
			// keep all components
			numberOfUsedComponents = getNumberOfComponents();
		}

		// retrieve factors inside singularValueVectors
		double[][] singularValueFactors = new double[numberOfUsedComponents][attributeNames.length];
		double[][] vMatrixData = vMatrix.getArray();
		for (int i = 0; i < numberOfUsedComponents; i++) {
			double invertedSingularValue = 1d / singularValues[i];
			for (int j = 0; j < attributeNames.length; j++) {
				singularValueFactors[i][j] = vMatrixData[j][i] * invertedSingularValue;
			}
		}

		// now build new attributes
		Attribute[] derivedAttributes = new Attribute[numberOfUsedComponents];
		for (int i = 0; i < numberOfUsedComponents; i++) {
			if (useLegacyNames) {
				derivedAttributes[i] = AttributeFactory.createAttribute("d" + i, Ontology.REAL);
			} else {
				derivedAttributes[i] = AttributeFactory.createAttribute("svd_" + (i + 1), Ontology.REAL);
			}
			exampleSet.getExampleTable().addAttribute(derivedAttributes[i]);
			attributes.addRegular(derivedAttributes[i]);
		}

		// initialize progress
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}

		// now iterator through all examples and derive value of new features
		double[] derivedValues = new double[numberOfUsedComponents];
		int progressCounter = 0;
		for (Example example : exampleSet) {
			// calculate values of new attributes with single scan over attributes
			d = 0;
			for (Attribute attribute : inputAttributes) {
				double attributeValue = example.getValue(attribute);
				for (int i = 0; i < numberOfUsedComponents; i++) {
					derivedValues[i] += singularValueFactors[i][d] * attributeValue;
				}
				d++;
			}

			// set values
			for (int i = 0; i < numberOfUsedComponents; i++) {
				example.setValue(derivedAttributes[i], derivedValues[i]);
			}

			// set values back
			Arrays.fill(derivedValues, 0);

			// trigger progress
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}

		// now remove attributes if needed
		if (!keepAttributes) {
			for (Attribute attribute : inputAttributes) {
				attributes.remove(attribute);
			}
		}

		return exampleSet;
	}

	public void enableLegacyMode() {
		this.useLegacyNames = true;
	}

	@Override
	public void setParameter(String name, Object object) throws OperatorException {
		if (name.equals("proportion_threshold")) {
			String value = (String) object;

			try {
				this.setVarianceThreshold(Double.parseDouble(value));
			} catch (NumberFormatException error) {
				super.setParameter(name, value);
			}

		} else if (name.equals("number_of_components")) {
			String value = (String) object;

			try {
				this.setNumberOfComponents(Integer.parseInt(value));
			} catch (NumberFormatException error) {
				super.setParameter(name, value);
			}

		} else if (name.equals("keep_attributes")) {
			String value = (String) object;
			keepAttributes = false;
			if (value.equals("true")) {
				keepAttributes = true;
			}
		} else {
			super.setParameter(name, object);
		}
	}

	@Override
	public AttributeWeights getWeightsOfComponent(int component) throws OperatorException {
		if (component < 1) {
			component = 1;
		}
		if (component > attributeNames.length) {
			logWarning("Creating weights of component " + attributeNames.length + "!");
			component = attributeNames.length;
		}
		AttributeWeights weights = new AttributeWeights();

		double[] singularVector = vMatrix.getArray()[component];
		for (int i = 0; i < attributeNames.length; i++) {
			weights.setWeight(attributeNames[i], singularVector[i]);
		}

		return weights;
	}

	@Override
	public String toResultString() {
		StringBuilder result = new StringBuilder(
				Tools.getLineSeparator() + "Principal Components:" + Tools.getLineSeparator());
		if (manualNumber) {
			result.append("Number of Components: " + numberOfComponents + Tools.getLineSeparator());
		} else {
			result.append("Proportion Threshold: " + proportionThreshold + Tools.getLineSeparator());
		}
		for (int i = 0; i < vMatrix.getColumnDimension(); i++) {
			result.append("PC " + (i + 1) + ": ");
			for (int j = 0; j < attributeNames.length; j++) {
				double value = vMatrix.get(i, j);
				if (value > 0) {
					result.append(" + ");
				} else {
					result.append(" - ");
				}
				result.append(Tools.formatNumber(Math.abs(value)) + " * " + attributeNames[j]);
			}
			result.append(Tools.getLineSeparator());
		}
		return result.toString();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(
				Tools.getLineSeparator() + "Principal Components:" + Tools.getLineSeparator());
		if (manualNumber) {
			result.append("Number of Components: " + numberOfComponents + Tools.getLineSeparator());
		} else {
			result.append("Variance Threshold: " + proportionThreshold + Tools.getLineSeparator());
		}
		return result.toString();
	}
}
