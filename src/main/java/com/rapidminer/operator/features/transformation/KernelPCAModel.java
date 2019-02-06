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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.AbstractModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.kernels.Kernel;

import Jama.Matrix;


/**
 * The model for the Kernel-PCA.
 *
 * @author Sebastian Land
 */
public class KernelPCAModel extends AbstractModel {

	private static final long serialVersionUID = -6699248775014738833L;

	private static final int OPERATOR_PROGRESS_STEPS_1 = 100;

	private static final int OPERATOR_PROGRESS_STEPS_2 = 10;

	private static final int INTERMEDIATE_PROGRESS = 20;

	private Matrix eigenVectors;

	private ArrayList<double[]> exampleValues;

	private Kernel kernel;

	private ArrayList<String> attributeNames;

	private double[] means;

	protected KernelPCAModel(ExampleSet exampleSet) {
		super(exampleSet);
	}

	public KernelPCAModel(ExampleSet exampleSet, double[] means, Matrix eigenVectors, ArrayList<double[]> exampleValues,
			Kernel kernel) {
		super(exampleSet);
		this.eigenVectors = eigenVectors;
		this.exampleValues = exampleValues;
		this.kernel = kernel;
		this.means = means;

		this.attributeNames = new ArrayList<String>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributeNames.add(attribute.getName());
		}
	}

	@Override
	public ExampleSet apply(ExampleSet inputExampleSet) throws OperatorException {
		ExampleSet exampleSet = (ExampleSet) inputExampleSet.clone();
		Attributes attributes = (Attributes) exampleSet.getAttributes().clone();

		checkNames(attributes);

		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(100);
		}

		log("Adding new the derived features...");
		Attribute[] pcatts = new Attribute[exampleValues.size()];
		for (int i = 0; i < exampleValues.size(); i++) {
			pcatts[i] = AttributeFactory.createAttribute("kpc_" + (i + 1), Ontology.REAL);
			exampleSet.getExampleTable().addAttribute(pcatts[i]);
			exampleSet.getAttributes().addRegular(pcatts[i]);
			if (progress != null && i % OPERATOR_PROGRESS_STEPS_1 == 0) {
				progress.setCompleted((int) ((double) INTERMEDIATE_PROGRESS * i / exampleValues.size()));
			}
		}
		log("Calculating new features");

		Matrix distanceValues = new Matrix(1, exampleValues.size());

		int progressCounter = 0;
		for (Example example : exampleSet) {
			int i = 0;
			for (double[] trainValue : exampleValues) {
				distanceValues.set(0, i++, kernel.calculateDistance(trainValue, getAttributeValues(example, attributes)));
			}
			Matrix resultValues = eigenVectors.times(distanceValues.transpose());
			for (int j = 0; j < exampleValues.size(); j++) {
				example.setValue(pcatts[j], resultValues.get(j, 0));
			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS_2 == 0) {
				progress.setCompleted((int) ((100.0 - INTERMEDIATE_PROGRESS) * progressCounter / exampleSet.size()
						+ INTERMEDIATE_PROGRESS));
			}
		}

		// removing old attributes
		exampleSet.getAttributes().clearRegular();
		for (Attribute attribute : pcatts) {
			exampleSet.getAttributes().addRegular(attribute);
		}
		return exampleSet;
	}

	private void checkNames(Attributes attributes) throws UserError {
		int i = 0;
		for (Attribute attribute : attributes) {
			if (!attribute.getName().equals(attributeNames.get(i++))) {
				throw new UserError(null, 141);
			}
		}

	}

	private double[] getAttributeValues(Example example, Attributes attributes) {
		double[] values = new double[attributes.size()];
		int x = 0;
		for (Attribute attribute : attributes) {
			values[x] = example.getValue(attribute) - means[x];
			x++;
		}
		return values;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Model uses " + exampleValues.size() + " Examples for calculating transformation\n");
		buffer.append("Kernel used for distance calculation:\n " + kernel.toString());
		return buffer.toString();
	}
}
