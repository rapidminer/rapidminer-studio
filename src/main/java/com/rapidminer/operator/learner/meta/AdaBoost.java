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
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 * This AdaBoost implementation can be used with all learners available in RapidMiner, not only the
 * ones which originally are part of the Weka package.
 * 
 * @author Martin Scholz
 */
public class AdaBoost extends AbstractMetaLearner {

	/**
	 * Name of the variable specifying the maximal number of iterations of the learner.
	 */
	public static final String PARAMETER_ITERATIONS = "iterations";

	/** Discard models with an advantage of less than the specified value. */
	public static final double MIN_ADVANTAGE = 0.001;

	// field for visualizing performance
	protected int currentIteration;

	// The total weight as a performance measure to be visualized.
	private double performance = 0;

	// A backup of the original weights of the training set to restore them
	// after learning.
	private double[] oldWeights;

	/** Constructor. */
	public AdaBoost(OperatorDescription description) {
		super(description);
		addValue(new ValueDouble("performance", "The performance.") {

			@Override
			public double getDoubleValue() {
				return performance;
			}
		});
		addValue(new ValueDouble("iteration", "The current iteration.") {

			@Override
			public double getDoubleValue() {
				return currentIteration;
			}
		});
	}

	@Override
	/**
	 * Adding weight attributes
	 */
	protected MetaData modifyExampleSetMetaData(ExampleSetMetaData unmodifiedMetaData) {
		AttributeMetaData weightAttribute = new AttributeMetaData("weight", Ontology.REAL, Attributes.WEIGHT_NAME);
		unmodifiedMetaData.addAttribute(weightAttribute);
		return super.modifyExampleSetMetaData(unmodifiedMetaData);
	}

	/**
	 * Overrides the method of the super class. Returns true for polynominal class.
	 */
	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		switch (lc) {
			case NUMERICAL_LABEL:
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
				return false;
			default:
				return true;
		}
	}

	/**
	 * Constructs a <code>Model</code> repeatedly running a weak learner, re-weighting the training
	 * example set accordingly, and combining the hypothesis using the available weighted
	 * performance values.
	 */
	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		if (!exampleSet.getAttributes().getLabel().isNominal()) {
			throw new UserError(this, 119, exampleSet.getAttributes().getLabel().getName(), getName());
		}

		this.performance = this.prepareWeights(exampleSet);
		Model model = this.trainBoostingModel(exampleSet);

		Attribute weightAttribute = exampleSet.getAttributes().getWeight();
		if (this.oldWeights != null) { // need to reset weights
			Iterator<Example> reader = exampleSet.iterator();
			int i = 0;
			while (reader.hasNext() && i < this.oldWeights.length) {
				reader.next().setValue(weightAttribute, this.oldWeights[i++]);
			}
		} else { // need to delete the weights attribute
			exampleSet.getAttributes().remove(weightAttribute);
			exampleSet.getExampleTable().removeAttribute(weightAttribute);
		}

		return model;
	}

	/**
	 * Creates a weight attribute if not yet done. It either backs up the old weights for restoring
	 * them later, or it fills the newly created attribute with the initial value of 1.
	 * 
	 * @param exampleSet
	 *            the example set to be prepared
	 * @return the total weight
	 */
	protected double prepareWeights(ExampleSet exampleSet) {
		Attribute weightAttr = exampleSet.getAttributes().getWeight();
		double totalWeight = 0;
		if (weightAttr == null) {
			this.oldWeights = null;
			weightAttr = Tools.createWeightAttribute(exampleSet);
			Iterator<Example> exRead = exampleSet.iterator();
			while (exRead.hasNext()) {
				exRead.next().setValue(weightAttr, 1);
				totalWeight++;
			}
		} else { // Back up old weights:
			this.oldWeights = new double[exampleSet.size()];
			Iterator<Example> reader = exampleSet.iterator();

			for (int i = 0; (reader.hasNext() && i < oldWeights.length); i++) {
				this.oldWeights[i] = reader.next().getWeight();
				totalWeight += this.oldWeights[i];
			}
		}
		return totalWeight;
	}

	/** Main method for training the ensemble classifier */
	private AdaBoostModel trainBoostingModel(ExampleSet trainingSet) throws OperatorException {
		log("Total weight of example set at the beginning: " + this.performance);

		// Containers for models and weights:
		Vector<Model> ensembleModels = new Vector<Model>();
		Vector<Double> ensembleWeights = new Vector<Double>();

		// maximum number of iterations
		final int iterations = this.getParameterAsInt(PARAMETER_ITERATIONS);
		for (int i = 0; (i < iterations && this.performance > 0); i++) {
			this.currentIteration = i;

			// train one model per iteration
			ExampleSet iterationSet = (ExampleSet) trainingSet.clone();
			Model model = applyInnerLearner(iterationSet);
			iterationSet = model.apply(iterationSet);
			// get the weighted performance value of the example set with
			// respect to the model
			AdaBoostPerformanceMeasures wp = new AdaBoostPerformanceMeasures(iterationSet);

			// Reweight the example set with respect to the weighted performance
			// values:
			this.performance = wp.reweightExamples(iterationSet);
			PredictionModel.removePredictedLabel(iterationSet);

			log("Total weight of example set after iteration " + (this.currentIteration + 1) + " is " + this.performance);

			if (this.isModelUseful(wp) == false) {
				// If the model is not considered to be useful (low advantage)
				// then discard it and stop.
				log("Discard model because of low advantage on training data.");
				return new AdaBoostModel(trainingSet, ensembleModels, ensembleWeights);
			}

			// Add the new model and its weights to the collection of models:
			ensembleModels.add(model);
			double errorRate = wp.getErrorRate();
			double weight;
			if (errorRate == 0) {
				weight = Double.POSITIVE_INFINITY;
			} else {
				weight = Math.log((1.0d - errorRate) / errorRate);
			}
			ensembleWeights.add(weight);
		}

		// Build a Model object. Last parameter is "crispPredictions", nowadays
		// always true.
		AdaBoostModel resultModel = new AdaBoostModel(trainingSet, ensembleModels, ensembleWeights);

		return resultModel;
	}

	/**
	 * Helper method to decide whether a model improves the training error enough to be considered.
	 * 
	 * @param wp
	 *            the advantage over the default classifier / random guessing
	 * @return <code>true</code> iff the advantage is high enough to consider the model to be useful
	 */
	private boolean isModelUseful(AdaBoostPerformanceMeasures wp) {
		return (wp.getErrorRate() < 0.5);
	}

	/**
	 * Adds the parameters &quot;number of iterations&quot; and &quot;model file&quot;.
	 */
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_ITERATIONS, "The maximum number of iterations.", 1,
				Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
