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
package com.rapidminer.operator.similarity;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.gui.ExampleVisualizer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.DistanceMeasurePrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.ObjectVisualizerService;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;
import com.rapidminer.tools.metadata.MetaDataTools;

import java.util.List;


/**
 * This class represents an operator that creates a similarity measure based on an ExampleSet.
 * 
 * @author Michael Wurst, Ingo Mierswa
 */
public class ExampleSet2Similarity extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort similarityOutput = getOutputPorts().createPort("similarity");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	private DistanceMeasureHelper measureHelper = new DistanceMeasureHelper(this);

	public ExampleSet2Similarity(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new DistanceMeasurePrecondition(exampleSetInput, this));

		getTransformer().addRule(new GenerateNewMDRule(similarityOutput, new MetaData(SimilarityMeasureObject.class)));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				MetaDataTools.checkAndCreateIds(metaData);
				return metaData;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		// needed for some measures
		Tools.checkAndCreateIds(exampleSet);

		DistanceMeasure measure = measureHelper.getInitializedMeasure(exampleSet);
		SimilarityMeasureObject measureObject = new SimilarityMeasureObject(measure, exampleSet);

		ObjectVisualizerService.addObjectVisualizer(measureObject, new ExampleVisualizer(exampleSet));

		similarityOutput.deliver(measureObject);
		exampleSetOutput.deliver(exampleSet);
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == exampleSetOutput) {
			return getParameterAsBoolean("keep_example_set");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(DistanceMeasures.getParameterTypes(this));
		return types;
	}
}
