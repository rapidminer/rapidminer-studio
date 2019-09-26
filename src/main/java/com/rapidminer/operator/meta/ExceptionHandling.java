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

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.Value;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.MultiInputPortPairExtender;
import com.rapidminer.operator.ports.MultiOutputPortPairExtender;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.LogService;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


/**
 * <p>
 * This operator performs the inner operators and delivers the result of the inner operators. If any
 * error occurs during this subprocess, this error will be neglected and this operator simply will
 * return no additional input.
 * </p>
 *
 * <p>
 * Please use this operator with care since it will also cover errors which are not expected by the
 * analyst. In combination with a process branch, however, it can be used to handle exceptions in
 * the analysis process (i.e. expected errors).
 * </p>
 *
 * @author Ingo Mierswa, Marius Helf
 */
public class ExceptionHandling extends OperatorChain {

	public static final String PARAMETER_EXCEPTION_MACRO = "exception_macro";

	public static final String PARAMETER_ADD_DETAILS_TO_LOG = "add_details_to_log";

	private boolean withoutError = true;
	private Throwable throwable;

	private static final int TRY_SUBPROCESS = 0;
	private static final int CATCH_SUBPROCESS = 1;

	private final MultiOutputPortPairExtender inputExtender = new MultiOutputPortPairExtender("in", getInputPorts(),
			new OutputPorts[] { getSubprocess(0).getInnerSources(), getSubprocess(1).getInnerSources() });
	private final MultiInputPortPairExtender outputExtender = new MultiInputPortPairExtender("out", getOutputPorts(),
			new InputPorts[] { getSubprocess(0).getInnerSinks(), getSubprocess(1).getInnerSinks() });

	public ExceptionHandling(OperatorDescription description) {
		super(description, "Try", "Catch");

		inputExtender.start();
		getTransformer().addRule(inputExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(1)));
		getTransformer().addRule(outputExtender.makePassThroughRule());
		outputExtender.start();

		addValue(new Value("success", "Indicates whether the execution was successful") {

			@Override
			public Object getValue() {
				return withoutError;
			}

			@Override
			public boolean isNominal() {
				return true;
			}
		});
		addValue(new Value("exception", "The exception that occured during execution.") {

			@Override
			public Object getValue() {
				return throwable;
			}

			@Override
			public boolean isNominal() {
				return true;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		withoutError = true;
		throwable = null;

		ExecutionUnit tryProcess = getSubprocess(TRY_SUBPROCESS);
		ExecutionUnit catchProcess = getSubprocess(CATCH_SUBPROCESS);
		tryProcess.getInnerSinks().clear(Port.CLEAR_DATA);
		catchProcess.getInnerSinks().clear(Port.CLEAR_DATA);

		try {
			inputExtender.passDataThrough(TRY_SUBPROCESS);
			tryProcess.execute();
			outputExtender.passDataThrough(TRY_SUBPROCESS);
		} catch (Throwable e) {
			if (getParameterAsBoolean(PARAMETER_ADD_DETAILS_TO_LOG)) {
				LogService.getRoot().log(Level.WARNING,
						"Error occurred and will be neglected by " + getName() + ": " + e.getMessage(), e);
			} else {
				LogService.getRoot().log(Level.WARNING,
						"Error occurred and will be neglected by " + getName() + ": " + e.getMessage());
			}
			if (isParameterSet(PARAMETER_EXCEPTION_MACRO)) {
				getProcess().getMacroHandler().addMacro(getParameterAsString(PARAMETER_EXCEPTION_MACRO), e.getMessage());
			}
			withoutError = false;
			this.throwable = e;

			inputExtender.passDataThrough(CATCH_SUBPROCESS);
			catchProcess.execute();
			outputExtender.passDataThrough(CATCH_SUBPROCESS);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeString(PARAMETER_EXCEPTION_MACRO,
				"The name of the macro a potentially occuring exception message will be stored in.", true));
		types.add(new ParameterTypeBoolean(PARAMETER_ADD_DETAILS_TO_LOG,
				"Indicates if the stack trace and details of the handled exception should be also added to the log files in addition to a simple warning message.",
				false));
		types.addAll(super.getParameterTypes());
		return types;
	}
}
