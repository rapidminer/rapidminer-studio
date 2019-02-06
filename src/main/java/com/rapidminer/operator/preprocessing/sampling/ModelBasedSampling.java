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
package com.rapidminer.operator.preprocessing.sampling;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.meta.WeightedPerformanceMeasures;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PredictionModelMetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;


// TODO Verify results, add capability to specify sample size, move sample size parameters to
// superclass
/**
 * Sampling based on a model. Examples which are correctly predicted will removed with a higher
 * probability.
 *
 * @author Martin Scholz, Ingo Mierswa, Sebastian Land
 */
public class ModelBasedSampling extends AbstractSamplingOperator {

	private InputPort modelInput = getInputPorts().createPort("model", PredictionModel.class);

	public ModelBasedSampling(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		// adding model's prediction attributes
		MetaData modelMetaData = modelInput.getMetaData();
		if (modelMetaData instanceof PredictionModelMetaData) {
			List<AttributeMetaData> predictionAttributes = ((PredictionModelMetaData) modelMetaData)
					.getPredictionAttributeMetaData();
			if (predictionAttributes != null) {
				metaData.addAllAttributes(predictionAttributes);
				metaData.mergeSetRelation(((PredictionModelMetaData) modelMetaData).getPredictionAttributeSetRelation());
			}
		}

		// adding weight attribute
		metaData.addAttribute(Tools.createWeightAttributeMetaData(metaData));

		// setting number of examples
		metaData.setNumberOfExamples(getSampledSize(metaData));

		return metaData;
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) {
		return new MDInteger();
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// retrieving and applying model
		PredictionModel model = modelInput.getData(PredictionModel.class);
		exampleSet = model.apply(exampleSet);

		Attribute weightAttr = Tools.createWeightAttribute(exampleSet);

		WeightedPerformanceMeasures wp = new WeightedPerformanceMeasures(exampleSet);
		WeightedPerformanceMeasures.reweightExamples(exampleSet, wp.getContingencyMatrix(), true);

		// recalculate weight attribute statistics
		exampleSet.recalculateAttributeStatistics(exampleSet.getAttributes().getWeight());
		double maxWeight = exampleSet.getStatistics(exampleSet.getAttributes().getWeight(), Statistics.MAXIMUM);

		// fill new table
		RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this);

		int[] remappingIndices = new int[exampleSet.size()];
		int i = 0;
		for (Example example : exampleSet) {
			if (randomGenerator.nextDouble() > example.getValue(weightAttr) / maxWeight) {
				example.setValue(weightAttr, 1.0d);
				remappingIndices[i] = 1;
			}
			i++;
		}
		checkForStop();
		SplittedExampleSet splittedExampleSet = new SplittedExampleSet(exampleSet, new Partition(remappingIndices, 2));
		splittedExampleSet.selectSingleSubset(1);
		return splittedExampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), ModelBasedSampling.class,
				null);
	}
}
