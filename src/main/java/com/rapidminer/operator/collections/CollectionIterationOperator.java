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
package com.rapidminer.operator.collections;

import java.util.List;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.CollectingPortPairExtender;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * Iterates over a collection and executes the subprocess on each element. The outputs of the
 * subprocesses are collected and returned as collections.
 *
 * @author Simon Fischer
 *
 */
public class CollectionIterationOperator extends OperatorChain {

	protected static final String PARAMETER_SET_MACRO = "set_iteration_macro";
	protected static final String PARAMETER_MACRO_NAME = "macro_name";
	protected static final String PARAMETER_MACRO_START_VALUE = "macro_start_value";

	protected static final String PARAMETER_UNFOLD = "unfold";

	private final InputPort collectionInput = getInputPorts().createPort("collection",
			new CollectionMetaData(new MetaData()));
	private final OutputPort singleInnerSource = getSubprocess(0).getInnerSources().createPort("single");
	private final CollectingPortPairExtender outExtender = new CollectingPortPairExtender("output", getSubprocess(0)
			.getInnerSinks(), getOutputPorts());

	private int currentIteration = 0;

	public CollectionIterationOperator(OperatorDescription description) {
		super(description, "Iteration");
		outExtender.start();
		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				MetaData md = collectionInput.getMetaData();
				if (md != null && md instanceof CollectionMetaData) {
					if (getParameterAsBoolean(PARAMETER_UNFOLD)) {
						singleInnerSource.deliverMD(((CollectionMetaData) md).getElementMetaDataRecursive());
					} else {
						singleInnerSource.deliverMD(((CollectionMetaData) md).getElementMetaData());
					}
				} else {
					singleInnerSource.deliverMD(null);
				}
			}
		});
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(outExtender.makePassThroughRule());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void doWork() throws OperatorException {
		IOObjectCollection<IOObject> data = collectionInput.getData(IOObjectCollection.class);
		List<IOObject> list;
		if (getParameterAsBoolean(PARAMETER_UNFOLD)) {
			list = data.getObjectsRecursive();
		} else {
			list = data.getObjects();
		}

		// init Operator progress
		getProgress().setTotal(list.size());
		getProgress().setCheckForStop(false);

		outExtender.reset();
		String iterationMacroName = null;
		int macroIterationOffset = 0;
		boolean setIterationMacro = getParameterAsBoolean(PARAMETER_SET_MACRO);
		if (setIterationMacro) {
			iterationMacroName = getParameterAsString(PARAMETER_MACRO_NAME);
			macroIterationOffset = getParameterAsInt(PARAMETER_MACRO_START_VALUE);
		}
		this.currentIteration = 0;
		for (IOObject o : list) {
			if (setIterationMacro) {
				String iterationString = Integer.toString(currentIteration + macroIterationOffset);
				getProcess().getMacroHandler().addMacro(iterationMacroName, iterationString);
			}
			singleInnerSource.deliver(o);
			getSubprocess(0).execute();
			outExtender.collect();
			currentIteration++;
			inApplyLoop();
			getProgress().step();
		}
		getProgress().complete();
		// outExtender.passDataThrough();
	}

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

		types.add(new ParameterTypeBoolean(PARAMETER_UNFOLD, "Determines if the input collection is unfolded.", false));
		return types;
	}
}
