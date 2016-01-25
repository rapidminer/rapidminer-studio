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
package com.rapidminer.operator.features.transformation;

import java.util.Iterator;

import Jama.Matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.AbstractModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * This is the transformation model of the <code>FastICA</code>. The number of independent
 * components is initially specified by the <code>FastICA</code>. Additionally you can specify
 * parameters in the <code>ModelApplier</code>.
 * <ul>
 * <li><b>keep_attributes</b> <i>true|false</i> If true, the original features are not removed.
 * </ul>
 *
 * @author Daniel Hakenjos, Ingo Mierswa
 * @see FastICA
 */
public class FastICAModel extends AbstractModel implements ComponentWeightsCreatable {

	private static final long serialVersionUID = -6380202686511014212L;

	private double[] means;

	private boolean rowNorm;

	private int numberOfComponents;

	private Matrix kMatrix;
	private Matrix wMatrix;
	private Matrix aMatrix;

	private String[] attributeNames;

	// -------------------------------------------------------------------------------------

	private int numberOfSamples, numberOfAttributes;

	private boolean keepAttributes = false;

	public FastICAModel(ExampleSet exampleSet, int numberOfComponents, double[] means, boolean rowNorm, Matrix kMatrix,
			Matrix wMatrix, Matrix aMatrix) {
		super(exampleSet);
		this.attributeNames = com.rapidminer.example.Tools.getRegularAttributeNames(exampleSet);
		this.numberOfComponents = numberOfComponents;
		this.means = means;
		this.rowNorm = rowNorm;
		this.kMatrix = kMatrix;
		this.wMatrix = wMatrix;
		this.aMatrix = aMatrix;
	}

	@Override
	public ExampleSet apply(ExampleSet inputTestSet) throws OperatorException {
		ExampleSet testSet = (ExampleSet) inputTestSet.clone();
		testSet.recalculateAllAttributeStatistics();
		numberOfSamples = testSet.size();
		numberOfAttributes = testSet.getAttributes().size();
		if (numberOfAttributes != means.length) {
			throw new UserError(null, 133, means.length, numberOfAttributes);
		}

		// all attributes numerical
		for (Attribute attribute : testSet.getAttributes()) {
			if (!attribute.isNumerical()) {
				throw new UserError(null, 104, new Object[] { "FastICA", attribute.getName() });
			}
		}

		// get the centered data
		double[][] data = new double[numberOfSamples][numberOfAttributes];
		Iterator<Example> reader = testSet.iterator();
		Example example;

		for (int sample = 0; sample < numberOfSamples; sample++) {
			example = reader.next();
			int d = 0;
			for (Attribute attribute : testSet.getAttributes()) {
				data[sample][d] = example.getValue(attribute) - means[d];
				d++;
			}
		}

		// row normalization
		// Scaling is done by dividing the rows of the data
		// by their root-mean-square. The root-mean-square for a row
		// is obtained by computing the
		// square-root of the sum-of-squares of the values in the
		// row divided by the number of values minus one.
		if (rowNorm) {
			log("Scaling the data now.");
			double rmsRow;
			for (int row = 0; row < numberOfSamples; row++) {
				// compute root mean square for the row
				rmsRow = 0.0d;
				for (int d = 0; d < numberOfAttributes; d++) {
					rmsRow += data[row][d] * data[row][d];
				}
				rmsRow = Math.sqrt(rmsRow) / Math.max(1, numberOfAttributes - 1);

				for (int d = 0; d < numberOfAttributes; d++) {
					data[row][d] = data[row][d] / rmsRow;
				}
			}
		}

		Matrix X = new Matrix(data);

		Matrix S = X.times(kMatrix).times(wMatrix);

		if (!keepAttributes) {
			testSet.getAttributes().clearRegular();
		}

		Attribute[] icAttributes = new Attribute[numberOfComponents];
		for (int i = 0; i < numberOfComponents; i++) {
			icAttributes[i] = AttributeFactory.createAttribute("ic_" + (i + 1), Ontology.REAL);
			testSet.getExampleTable().addAttribute(icAttributes[i]);
			testSet.getAttributes().addRegular(icAttributes[i]);
		}

		double[][] finaldata = S.getArray();
		reader = testSet.iterator();
		for (int sample = 0; sample < testSet.size(); sample++) {
			example = reader.next();
			for (int d = 0; d < numberOfComponents; d++) {
				example.setValue(icAttributes[d], finaldata[sample][d]);
			}

		}

		return testSet;
	}

	public void setNumberOfComponents(int number) {
		this.numberOfComponents = number;
	}

	@Override
	public void setParameter(String name, Object object) throws OperatorException {
		if (name.equals("number_of_components")) {
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
		if (component > numberOfComponents) {
			logWarning("Creating weights of component " + numberOfComponents + "!");
			component = numberOfComponents;
		}
		AttributeWeights attweights = new AttributeWeights();
		for (int i = 0; i < attributeNames.length; i++) {
			attweights.setWeight(attributeNames[i], aMatrix.get(component - 1, i));
		}

		return attweights;
	}

	@Override
	public String toResultString() {
		StringBuffer result = new StringBuffer();
		result.append("Number of Components: " + numberOfComponents + Tools.getLineSeparator());
		if (aMatrix != null) {
			result.append("Resulting attribute weights (from first component):" + Tools.getLineSeparator());
			for (int i = 0; i < attributeNames.length; i++) {
				result.append(attributeNames[i] + ": " + Tools.formatNumber(aMatrix.get(0, i)) + Tools.getLineSeparator());
			}
		}
		return result.toString();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("ICA Model" + Tools.getLineSeparator());
		result.append("Number of Components: " + numberOfComponents + Tools.getLineSeparator());
		return result.toString();
	}
}
