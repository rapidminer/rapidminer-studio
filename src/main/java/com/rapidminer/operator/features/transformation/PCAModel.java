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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.example.set.ExampleSetUtilities.SetsCompareOption;
import com.rapidminer.example.set.ExampleSetUtilities.TypesCompareOption;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.gui.renderer.models.EigenvectorModelEigenvalueRenderer.EigenvalueTableModel;
import com.rapidminer.gui.renderer.models.EigenvectorModelEigenvectorRenderer.EigenvectorTableModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


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
public class PCAModel extends AbstractEigenvectorModel implements ComponentWeightsCreatable {

	private static final long serialVersionUID = 5424591594470376525L;

	private static final int OPERATOR_PROGRESS_STEPS = 5_000;

	private List<Eigenvector> eigenVectors;

	private double[] means;

	private String[] attributeNames;

	private boolean manualNumber;

	private int numberOfComponents = -1;

	private double varianceThreshold;

	// -----------------------------------

	private double[] variances;

	private double[] cumulativeVariance;

	private boolean keepAttributes = false;

	public PCAModel(ExampleSet eSet, double[] eigenvalues, double[][] eigenvectors) {
		super(eSet);

		this.keepAttributes = false;
		this.attributeNames = new String[eSet.getAttributes().size()];
		this.means = new double[eSet.getAttributes().size()];
		int counter = 0;
		eSet.recalculateAllAttributeStatistics(); // ensures that the statistics were created
		for (Attribute attribute : eSet.getAttributes()) {
			attributeNames[counter] = attribute.getName();
			means[counter] = eSet.getStatistics(attribute, Statistics.AVERAGE);
			counter++;
		}
		this.eigenVectors = new ArrayList<>(eigenvalues.length);
		for (int i = 0; i < eigenvalues.length; i++) {
			double[] currentEigenVector = new double[eSet.getAttributes().size()];
			for (int j = 0; j < currentEigenVector.length; j++) {
				currentEigenVector[j] = eigenvectors[j][i];
			}
			this.eigenVectors.add(new Eigenvector(currentEigenVector, eigenvalues[i]));
		}

		// order the eigenvectors by the eigenvalues
		Collections.sort(this.eigenVectors);

		calculateCumulativeVariance();
	}

	public String[] getAttributeNames() {
		return attributeNames;
	}

	public double[] getMeans() {
		return means;
	}

	public double getMean(int index) {
		return means[index];
	}

	public double getVariance(int index) {
		return this.variances[index];
	}

	public double getCumulativeVariance(int index) {
		return this.cumulativeVariance[index];
	}

	public double getEigenvalue(int index) {
		return this.eigenVectors.get(index).getEigenvalue();
	}

	public double[] getEigenvector(int index) {
		return this.eigenVectors.get(index).getEigenvector();
	}

	public double getVarianceThreshold() {
		return this.varianceThreshold;
	}

	public int getMaximumNumberOfComponents() {
		return attributeNames.length;
	}

	public int getNumberOfComponents() {
		return numberOfComponents;
	}

	public void setVarianceThreshold(double threshold) {
		this.manualNumber = false;
		this.varianceThreshold = threshold;
		this.numberOfComponents = -1;
	}

	public void setNumberOfComponents(int numberOfComponents) {
		this.varianceThreshold = 0.95;
		this.manualNumber = true;
		this.numberOfComponents = numberOfComponents;
	}

	@Override
	public ExampleSet apply(ExampleSet inputExampleSet) throws OperatorException {
		ExampleSet exampleSet = (ExampleSet) inputExampleSet.clone();
		exampleSet.recalculateAllAttributeStatistics();

		Attributes attributes = exampleSet.getAttributes();
		if (attributeNames.length != attributes.size()) {
			throw new UserError(null, 133, attributeNames.length, attributes.size());
		}

		// remember attributes that have been removed during training. These will be removed lateron
		Attributes trainingHeaderAttributes = getTrainingHeader().getAttributes();
		ExampleSetUtilities.checkAttributesMatching(getOperator(), trainingHeaderAttributes, attributes,
				SetsCompareOption.EQUAL, TypesCompareOption.EQUAL);
		Attribute[] inputAttributes = new Attribute[trainingHeaderAttributes.size()];
		int d = 0;
		for (Attribute oldAttribute : trainingHeaderAttributes) {
			inputAttributes[d] = attributes.get(oldAttribute.getName());
			d++;
		}

		// determining number of used components
		int numberOfUsedComponents = -1;
		if (manualNumber) {
			numberOfUsedComponents = numberOfComponents;
		} else {
			if (varianceThreshold == 0.0d) {
				numberOfUsedComponents = -1;
			} else {
				numberOfUsedComponents = 0;
				while (cumulativeVariance[numberOfUsedComponents] < varianceThreshold) {
					numberOfUsedComponents++;
				}
				numberOfUsedComponents++;
				if (numberOfUsedComponents == eigenVectors.size()) {
					numberOfUsedComponents--;
				}
			}
		}
		if (numberOfUsedComponents == -1) {
			// keep all components
			numberOfUsedComponents = attributes.size();
		}

		// retrieve factors inside eigenVectors
		double[][] eigenValueFactors = new double[numberOfUsedComponents][attributeNames.length];
		for (int i = 0; i < numberOfUsedComponents; i++) {
			eigenValueFactors[i] = this.eigenVectors.get(i).getEigenvector();
		}

		// now build new attributes
		Attribute[] derivedAttributes = new Attribute[numberOfUsedComponents];
		for (int i = 0; i < numberOfUsedComponents; i++) {
			derivedAttributes[i] = AttributeFactory.createAttribute("pc_" + (i + 1), Ontology.REAL);
			exampleSet.getExampleTable().addAttribute(derivedAttributes[i]);
			attributes.addRegular(derivedAttributes[i]);
		}

		// now iterator through all examples and derive value of new features
		double[] derivedValues = new double[numberOfUsedComponents];
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;
		for (Example example : exampleSet) {
			// calculate values of new attributes with single scan over attributes
			d = 0;
			for (Attribute attribute : inputAttributes) {
				double attributeValue = example.getValue(attribute) - means[d];
				for (int i = 0; i < numberOfUsedComponents; i++) {
					derivedValues[i] += eigenValueFactors[i][d] * attributeValue;
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

	/** Calculates the cumulative variance. */
	private void calculateCumulativeVariance() {
		double sumvariance = 0.0d;
		for (Eigenvector ev : this.eigenVectors) {
			sumvariance += ev.getEigenvalue();
		}
		this.variances = new double[this.eigenVectors.size()];
		this.cumulativeVariance = new double[variances.length];
		double cumulative = 0.0d;
		int counter = 0;
		for (Eigenvector ev : this.eigenVectors) {
			double proportion = ev.getEigenvalue() / sumvariance;
			this.variances[counter] = proportion;
			cumulative += proportion;
			this.cumulativeVariance[counter] = cumulative;
			counter++;
		}
	}

	@Override
	public void setParameter(String name, Object object) throws OperatorException {
		if (name.equals("variance_threshold")) {
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

		double[] eigenvector = eigenVectors.get(component - 1).getEigenvector();
		for (int i = 0; i < attributeNames.length; i++) {
			weights.setWeight(attributeNames[i], eigenvector[i]);
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
			result.append("Variance Threshold: " + varianceThreshold + Tools.getLineSeparator());
		}
		for (int i = 0; i < eigenVectors.size(); i++) {
			result.append("PC " + (i + 1) + ": ");
			for (int j = 0; j < attributeNames.length; j++) {
				double value = eigenVectors.get(i).getEigenvector()[j];
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
			result.append("Variance Threshold: " + varianceThreshold + Tools.getLineSeparator());
		}
		return result.toString();
	}

	@Override
	public double[] getCumulativeVariance() {
		return this.cumulativeVariance;
	}

	@Override
	public EigenvalueTableModel getEigenvalueTableModel() {
		double varianceSum = 0.0d;
		for (Eigenvector wv : eigenVectors) {
			varianceSum += wv.getEigenvalue();
		}
		return new EigenvalueTableModel(eigenVectors, cumulativeVariance, varianceSum);
	}

	@Override
	public EigenvectorTableModel getEigenvectorTableModel() {
		return new EigenvectorTableModel(eigenVectors, attributeNames, attributeNames.length);
	}
}
