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
package com.rapidminer.operator;

import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;


/**
 * A simple operator chain which can have an arbitrary number of inner operators. The operators are
 * subsequently applied and their output is used as input for the succeeding operator. The input of
 * the operator chain is used as input for the first inner operator and the output of the last
 * operator is used as the output of the operator chain.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class SimpleOperatorChain extends OperatorChain {

	protected PortPairExtender inputExtender = new PortPairExtender("in", getInputPorts(), getSubprocess(0)
			.getInnerSources());
	protected PortPairExtender outputExtender = new PortPairExtender("out", getSubprocess(0).getInnerSinks(),
			getOutputPorts());

	/** Creates an empty operator chain. */
	public SimpleOperatorChain(OperatorDescription description) {
		this(description, "Nested Process");
	}

	/** This constructor allows subclasses to change the subprocess' name. */
	protected SimpleOperatorChain(OperatorDescription description, String subProcessName) {
		super(description, subProcessName);
		inputExtender.start();
		outputExtender.start();
		getTransformer().addRule(inputExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(outputExtender.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		final int numOperators = this.getSubprocess(0).getEnabledOperators().size();
		getProgress().setTotal(numOperators);
		getProcess().getRootOperator().addProcessListener(new ProcessListener() {

			int counter = 0;

			@Override
			public void processStarts(Process process) {
				// dont care

			}

			@Override
			public void processStartedOperator(Process process, Operator op) {
				// dont care

			}

			@Override
			public void processFinishedOperator(Process process, Operator op) {
				try {
					if (op.getParent() != null && op.getParent().equals(SimpleOperatorChain.this)) {
						SimpleOperatorChain.this.getProgress().setCompleted(++counter);
					}
				} catch (ProcessStoppedException e) {
					SimpleOperatorChain.this.getProcess().getRootOperator().removeProcessListener(this);
				}
				if (counter == numOperators) {
					SimpleOperatorChain.this.getProcess().getRootOperator().removeProcessListener(this);
				}

			}

			@Override
			public void processEnded(Process process) {
				SimpleOperatorChain.this.getProcess().getRootOperator().removeProcessListener(this);
			}
		});
		clearAllInnerSinks();
		inputExtender.passDataThrough();
		super.doWork();
		outputExtender.passDataThrough();
	}

	/**
	 * @return the input extender
	 */
	public PortPairExtender getInputExtender() {
		return inputExtender;
	}

	/**
	 * @return the output extender
	 */
	public PortPairExtender getOutputExtender() {
		return outputExtender;
	}
}
