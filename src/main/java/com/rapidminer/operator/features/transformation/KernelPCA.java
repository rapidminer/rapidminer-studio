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
import java.util.List;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.kernels.Kernel;


/**
 * This operator performs a kernel-based principal components analysis (PCA). Hence, the result will
 * be the set of data points in a non-linearly transformed space. Please note that in contrast to
 * the usual linear PCA the kernel variant does also works for large numbers of attributes but will
 * become slow for large number of examples.
 *
 * @author Sebastian Land
 */
public class KernelPCA extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set input");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set output");
	private OutputPort originalOutput = getOutputPorts().createPort("original");
	private OutputPort modelOutput = getOutputPorts().createPort("preprocessing model");

	public KernelPCA(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.NUMERICAL));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				switch (metaData.getNumberOfExamples().getRelation()) {
					case EQUAL:
						metaData.attributesAreKnown();
						break;
					case AT_LEAST:
						metaData.attributesAreSubset();
						break;
					case AT_MOST:
					case UNKNOWN:
						metaData.attributesAreSuperset();
						break;
				}
				if (metaData.getNumberOfExamples().getNumber() != null) {
					int numberOfExamples = metaData.getNumberOfExamples().getNumber();
					metaData.clearRegular();
					for (int i = 1; i <= numberOfExamples; i++) {
						metaData.addAttribute(new AttributeMetaData("kpc_" + i, Ontology.REAL));
					}
				}
				return super.modifyExampleSet(metaData);
			}
		});
		getTransformer().addRule(new PassThroughRule(exampleSetInput, originalOutput, false));
		getTransformer().addGenerationRule(modelOutput, Model.class);

	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		// only use numeric attributes
		Tools.onlyNumericalAttributes(exampleSet, "KernelPCA");
		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this);

		Attributes attributes = exampleSet.getAttributes();
		int numberOfExamples = exampleSet.size();

		// calculating means for later zero centering
		exampleSet.recalculateAllAttributeStatistics();
		double[] means = new double[exampleSet.getAttributes().size()];
		int i = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			means[i] = exampleSet.getStatistics(attribute, Statistics.AVERAGE);
			i++;
		}

		// kernel
		Kernel kernel = Kernel.createKernel(this);

		// copying zero centered exampleValues
		ArrayList<double[]> exampleValues = new ArrayList<double[]>(numberOfExamples);
		i = 0;
		for (Example columnExample : exampleSet) {
			double[] columnValues = getAttributeValues(columnExample, attributes, means);
			exampleValues.add(columnValues);
			i++;
		}

		// filling kernel matrix
		Matrix kernelMatrix = new Matrix(numberOfExamples, numberOfExamples);
		for (i = 0; i < numberOfExamples; i++) {
			for (int j = 0; j < numberOfExamples; j++) {
				kernelMatrix.set(i, j, kernel.calculateDistance(exampleValues.get(i), exampleValues.get(j)));
			}
		}

		// calculating eigenVectors
		EigenvalueDecomposition eig = kernelMatrix.eig();
		Model model = new KernelPCAModel(exampleSet, means, eig.getV(), exampleValues, kernel);

		if (exampleSetOutput.isConnected()) {
			exampleSetOutput.deliver(model.apply(exampleSet));
		}
		originalOutput.deliver(exampleSet);
		modelOutput.deliver(model);
	}

	private double[] getAttributeValues(Example example, Attributes attributes, double[] means) {
		double[] values = new double[attributes.size()];
		int x = 0;
		for (Attribute attribute : attributes) {
			values[x] = example.getValue(attribute) - means[x];
			x++;
		}
		return values;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(Kernel.getParameters(this));
		return types;
	}
}
