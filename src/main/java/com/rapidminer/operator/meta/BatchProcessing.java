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
package com.rapidminer.operator.meta;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * This operator groups the input examples into batches of the specified size and performs the inner
 * operators on all batches subsequently. This might be useful for very large data sets which cannot
 * be load into memory but must be handled in a database. In these cases, preprocessing methods or
 * model applications and other tasks can be performed on each batch and the result might be again
 * written into a database table (by using the DatabaseExampleSetWriter in its append mode). <br/>
 * Note that the output of this operator is not composed of the results of the nested subprocess. In
 * fact the subprocess does not need to deliver any output since it operates on a subset view of the
 * input example set.
 *
 * @author Ingo Mierswa
 */
public class BatchProcessing extends OperatorChain {

	public static final String PARAMETER_BATCH_SIZE = "batch_size";

	private final InputPort exampleSetInput = getInputPorts().createPort("example set", new ExampleSetMetaData());
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private final OutputPort exampleSetInnerSource = getSubprocess(0).getInnerSources().createPort("exampleSet");

	public BatchProcessing(OperatorDescription description) {
		super(description, "Batch Process");
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetInnerSource, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				metaData.setNumberOfExamples(getParameterAsInt(PARAMETER_BATCH_SIZE));
				return super.modifyExampleSet(metaData);
			}
		});
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new PassThroughRule(exampleSetInput, exampleSetOutput, false));
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		int batchSize = getParameterAsInt(PARAMETER_BATCH_SIZE);
		int size = exampleSet.size();
		int currentStart = 0;

		// init Operator progress
		getProgress().setTotal(size);

		// disable call to checkForStop as inApplyLoop will call it anyway
		getProgress().setCheckForStop(false);

		while (currentStart < size) {
			ExampleSet materializedSet = Tools.getLinearSubsetCopy(exampleSet, batchSize, currentStart);
			exampleSetInnerSource.deliver(materializedSet);

			getSubprocess(0).execute();

			currentStart += batchSize;
			inApplyLoop();
			getProgress().step();
		}

		exampleSetOutput.deliver(exampleSet);
		getProgress().complete();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_BATCH_SIZE,
				"This number of examples is processed batch-wise by the inner operators of this operator.", 1,
				Integer.MAX_VALUE, 1000, false));
		return types;
	}
}
