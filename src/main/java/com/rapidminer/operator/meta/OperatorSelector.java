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

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.MultiInputPortPairExtender;
import com.rapidminer.operator.ports.MultiOutputPortPairExtender;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;


/**
 * This operator can be used to employ a single inner operator or operator chain. Which operator
 * should be used can be defined by the parameter &quot;select_which&quot;. Together with one of the
 * parameter optimizing or iterating operators this operator can be used to dynamically change the
 * process setup which might be useful in order to test different layouts, e.g. the gain by using
 * different preprocessing steps or chains or the quality of a certain learner.
 *
 * @author Ingo Mierswa
 */
public class OperatorSelector extends OperatorChain {

	/**
	 * The parameter name for &quot;Indicates if the operator which inner operator should be
	 * used&quot;.
	 */
	public static final String PARAMETER_SELECT_WHICH = "select_which";

	private final MultiOutputPortPairExtender inputExtender = new MultiOutputPortPairExtender("input", getInputPorts(),
			new OutputPorts[] { getSubprocess(0).getInnerSources(), getSubprocess(1).getInnerSources() });
	private final MultiInputPortPairExtender outputExtender = new MultiInputPortPairExtender("output", getOutputPorts(),
			new InputPorts[] { getSubprocess(0).getInnerSinks(), getSubprocess(1).getInnerSinks() });

	public OperatorSelector(OperatorDescription description) {
		super(description, "Selection 1", "Selection 2");
		inputExtender.start();
		outputExtender.start();
		getTransformer().addRule(inputExtender.makePassThroughRule());
		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				int operatorIndex = -1;
				try {
					operatorIndex = getParameterAsInt(PARAMETER_SELECT_WHICH) - 1;
					for (int i = 0; i < getNumberOfSubprocesses(); i++) {
						if (i != operatorIndex) { // skip selected and transform last, so it
													 // overrides everything
							getSubprocess(i).transformMetaData();
						}
					}
					if ((operatorIndex >= 0) && (operatorIndex < getNumberOfSubprocesses())) {
						getSubprocess(operatorIndex).transformMetaData();
					}
				} catch (Exception e) {

				}

			}
		});
		getTransformer().addRule(outputExtender.makePassThroughRule());
	}

	@Override
	public boolean areSubprocessesExtendable() {
		return true;
	}

	@Override
	protected ExecutionUnit createSubprocess(int index) {
		return new ExecutionUnit(this, "Selection");
	}

	@Override
	public ExecutionUnit addSubprocess(int index) {
		ExecutionUnit newProcess = super.addSubprocess(index);
		inputExtender.addMultiPorts(newProcess.getInnerSources(), index);
		outputExtender.addMultiPorts(newProcess.getInnerSinks(), index);
		normalizeSubprocessNames();
		return newProcess;
	}

	@Override
	public ExecutionUnit removeSubprocess(int index) {
		ExecutionUnit oldProcess = super.removeSubprocess(index);
		inputExtender.removeMultiPorts(index);
		outputExtender.removeMultiPorts(index);
		normalizeSubprocessNames();
		return oldProcess;
	}

	private void normalizeSubprocessNames() {
		for (int i = 0; i < getNumberOfSubprocesses(); i++) {
			getSubprocess(i).setName("Selection " + (i + 1));
		}
	}

	@Override
	public void doWork() throws OperatorException {
		int operatorIndex = getParameterAsInt(PARAMETER_SELECT_WHICH);
		if (operatorIndex < 1 || operatorIndex > getNumberOfSubprocesses()) {
			throw new UserError(this, 207, new Object[] { operatorIndex, PARAMETER_SELECT_WHICH,
					"must be between 1 and the number of inner operators." });
		}

		inputExtender.passDataThrough(operatorIndex - 1);
		getSubprocess(operatorIndex - 1).execute();
		outputExtender.passDataThrough(operatorIndex - 1);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_SELECT_WHICH,
				"Indicates which inner operator should be currently employed by this operator on the input objects.", 1,
				Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
