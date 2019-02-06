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
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;


/**
 * For each example set the ExampleSetIterator finds in its input, the inner operators are applied
 * as if it was an OperatorChain. This operator can be used to conduct a process consecutively on a
 * number of different data sets.
 *
 * @author Ingo Mierswa
 */
public class ExampleSetIterator extends OperatorChain {

	private final InputPortExtender exampleSetExtender = new InputPortExtender("example set", getInputPorts()) {

		@Override
		protected Precondition makePrecondition(InputPort port) {
			return new SimplePrecondition(port, new ExampleSetMetaData(), false);
		}
	};

	private final PortPairExtender outputExtender = new PortPairExtender("output", getSubprocess(0).getInnerSinks(),
			getOutputPorts());
	private final OutputPort exampleSetInnerSource = getSubprocess(0).getInnerSources().createPort("example set");
	private final InputPort performanceInnerSink = getSubprocess(0).getInnerSinks().createPort("performance");

	/**
	 * The parameter name for &quot;Return only best result? (Requires a PerformanceVector in the
	 * inner result).&quot;
	 */
	public static final String PARAMETER_ONLY_BEST = "only_best";

	public ExampleSetIterator(OperatorDescription description) {
		super(description, "Subprocess");

		exampleSetExtender.start();
		outputExtender.start();

		performanceInnerSink.addPrecondition(new SimplePrecondition(performanceInnerSink, new MetaData(
				PerformanceVector.class), false) {

			@Override
			public boolean isMandatory() {
				return ExampleSetIterator.this.getParameterAsBoolean(PARAMETER_ONLY_BEST);
			}
		});
		getTransformer().addRule(exampleSetExtender.makeFlatteningPassThroughRule(exampleSetInnerSource));
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(outputExtender.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		List<ExampleSet> eSetList = exampleSetExtender.getData(ExampleSet.class, true);

		// init Operator progress
		getProgress().setTotal(eSetList.size());

		// disable call to checkForStop as inApplyLoop will call it anyway
		getProgress().setCheckForStop(false);

		boolean onlyBest = getParameterAsBoolean(PARAMETER_ONLY_BEST);
		double bestFitness = Double.NEGATIVE_INFINITY;
		for (ExampleSet exampleSet : eSetList) {
			exampleSetInnerSource.deliver(exampleSet);
			getSubprocess(0).execute();
			if (onlyBest) {
				PerformanceVector pv = performanceInnerSink.getData(PerformanceVector.class);
				double fitness = pv.getMainCriterion().getFitness();
				if (fitness > bestFitness) {
					bestFitness = fitness;
					outputExtender.passDataThrough();
				}
			}
			inApplyLoop();
			getProgress().step();
		}
		if (!onlyBest) {
			outputExtender.passDataThrough();
		}
		getProgress().complete();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_ONLY_BEST,
				"Return only best result? (Requires a the performance port to be connected.)", false);
		type.setExpert(false);
		types.add(type);

		return types;
	}
}
