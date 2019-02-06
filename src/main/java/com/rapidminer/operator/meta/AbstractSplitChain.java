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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * <p>
 * An operator chain that split an {@link ExampleSet} into two disjoint parts and applies the first
 * child operator on the first part and applies the second child on the second part and the result
 * of the first child. The total result is the result of the second operator.
 * </p>
 * 
 * <p>
 * Subclasses must define how the example set is divided.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public abstract class AbstractSplitChain extends OperatorChain {

	private final InputPort exampleSetInput = getInputPorts().createPort("example set");

	/** The input example set's first part is sent here. */
	private final OutputPort firstNestedExampleSetOutput = getSubprocess(0).getInnerSources().createPort("example set");
	private final PortPairExtender firstToSecondExtender = new PortPairExtender("through", getSubprocess(0).getInnerSinks(),
			getSubprocess(1).getInnerSources());

	private final OutputPort secondNestedExampleSetOutput = getSubprocess(1).getInnerSources().createPort("example set");

	private final PortPairExtender secondToOutputExtender = new PortPairExtender("through",
			getSubprocess(1).getInnerSinks(), getOutputPorts());

	public AbstractSplitChain(OperatorDescription description) {
		super(description, "First Part", "Second Part");
		firstToSecondExtender.start();
		secondToOutputExtender.start();

		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, new ExampleSetMetaData()));

		getTransformer().addRule(
				new ExampleSetPassThroughRule(exampleSetInput, firstNestedExampleSetOutput, SetRelation.EQUAL) {

					@Override
					public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
						metaData.setNumberOfExamples(getNumberOfExamplesFirst(metaData.getNumberOfExamples()));
						return super.modifyExampleSet(metaData);
					}
				});
		getTransformer().addRule(
				new ExampleSetPassThroughRule(exampleSetInput, secondNestedExampleSetOutput, SetRelation.EQUAL) {

					@Override
					public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
						metaData.setNumberOfExamples(getNumberOfExamplesSecond(metaData.getNumberOfExamples()));
						return super.modifyExampleSet(metaData);
					}
				});

		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(firstToSecondExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(1)));
		getTransformer().addRule(secondToOutputExtender.makePassThroughRule());
	}

	protected abstract MDInteger getNumberOfExamplesFirst(MDInteger numberOfExamples) throws UndefinedParameterError;

	protected abstract MDInteger getNumberOfExamplesSecond(MDInteger numberOfExamples) throws UndefinedParameterError;

	/**
	 * Creates the splitted example set for this operator. Please note that the results must contain
	 * two parts.
	 */
	protected abstract SplittedExampleSet createSplittedExampleSet(ExampleSet exampleSet) throws OperatorException;

	@Override
	public void doWork() throws OperatorException {
		ExampleSet inputSet = exampleSetInput.getData(ExampleSet.class);
		SplittedExampleSet exampleSet = createSplittedExampleSet(inputSet);
		// TODO: Simon: Clone?
		exampleSet.selectSingleSubset(0);
		firstNestedExampleSetOutput.deliver(exampleSet);
		getSubprocess(0).execute();

		exampleSet.selectSingleSubset(1);
		secondNestedExampleSetOutput.deliver(exampleSet);
		firstToSecondExtender.passDataThrough();
		getSubprocess(1).execute();

		secondToOutputExtender.passDataThrough();
	}

	protected InputPort getExampleSetInputPort() {
		return exampleSetInput;
	}
}
