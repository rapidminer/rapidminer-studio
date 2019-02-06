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

import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ports.CollectingPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 *
 * @author Sebastian Land
 */
public abstract class AbstractIteratingOperatorChain extends OperatorChain {

	public static final String PARAMETER_SET_MACRO = "set_iteration_macro";
	public static final String PARAMETER_MACRO_NAME = "macro_name";
	public static final String PARAMETER_MACRO_START_VALUE = "macro_start_value";

	private final PortPairExtender inputPortPairExtender = new PortPairExtender("input", getInputPorts(), getSubprocess(0)
			.getInnerSources());
	private final CollectingPortPairExtender outExtender = new CollectingPortPairExtender("output", getSubprocess(0)
			.getInnerSinks(), getOutputPorts());

	private int currentIteration = 0;

	public AbstractIteratingOperatorChain(OperatorDescription description) {
		super(description, "Iteration");

		inputPortPairExtender.start();
		outExtender.start();

		getTransformer().addRule(inputPortPairExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(outExtender.makePassThroughRule());

		addValue(new ValueDouble("iteration", "The iteration currently performed by this looping operator.") {

			@Override
			public double getDoubleValue() {
				return currentIteration;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		outExtender.reset();
		String iterationMacroName = null;
		int macroIterationOffset = 0;

		// disable call to checkForStop as inApplyLoop will call it anyway
		getProgress().setCheckForStop(false);

		boolean setIterationMacro = getParameterAsBoolean(PARAMETER_SET_MACRO);
		if (setIterationMacro) {
			iterationMacroName = getParameterAsString(PARAMETER_MACRO_NAME);
			macroIterationOffset = getParameterAsInt(PARAMETER_MACRO_START_VALUE);
		}
		this.currentIteration = 0;
		while (!shouldStop(getSubprocess(0).getInnerSinks().createIOContainer(false))) {
			if (setIterationMacro) {
				String iterationString = Integer.toString(currentIteration + macroIterationOffset);
				getProcess().getMacroHandler().addMacro(iterationMacroName, iterationString);
			}
			getLogger().fine("Starting iteration " + (currentIteration + 1));
			inputPortPairExtender.passDataThrough();
			getSubprocess(0).execute();
			outExtender.collect();
			getLogger().fine("Completed iteration " + (++currentIteration));
			inApplyLoop();
			getProgress().step();
		}
		getProgress().complete();
	}

	protected int getIteration() {
		return currentIteration;
	}

	abstract boolean shouldStop(IOContainer iterationResults) throws OperatorException;

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type;
		type = new ParameterTypeBoolean(PARAMETER_SET_MACRO,
				"Selects if in each iteration a macro with the current iteration number is set.", false, true);
		types.add(type);
		type = new ParameterTypeString(PARAMETER_MACRO_NAME, "The name of the iteration macro.", "iteration", true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_SET_MACRO, true, true));
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MACRO_START_VALUE,
				"The number which is set for the macro in the first iteration.", Integer.MIN_VALUE, Integer.MAX_VALUE, 1,
				true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_SET_MACRO, true, true));
		types.add(type);

		return types;
	}
}
