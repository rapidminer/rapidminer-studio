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
package com.rapidminer.operator.visualization;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueString;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;

import java.util.List;


/**
 * <p>
 * This operator can be used to log the current value of the specified macro. Some operators provide
 * the macro they define themselves as loggable values and in these cases this value can directly be
 * logged. But in all other cases where the operator does not provide a loggable value for the
 * defined macro, this operator may be used to define such a value from the macro.
 * </p>
 * 
 * <p>
 * Please note that the value will be logged as nominal value even if it is actually numerical. This
 * can be later be changed by transforming the logged statistics into a data set.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class Macro2Log extends Operator {

	public static final String PARAMETER_MACRO_NAME = "macro_name";

	private String currentValue = null;

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public Macro2Log(OperatorDescription description) {
		super(description);

		dummyPorts.start();

		getTransformer().addRule(dummyPorts.makePassThroughRule());

		addValue(new ValueString("macro_value", "The value from the macro which should be logged.") {

			@Override
			public String getStringValue() {
				return currentValue;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		this.currentValue = getProcess().getMacroHandler().getMacro(getParameterAsString(PARAMETER_MACRO_NAME));
		dummyPorts.passDataThrough();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_MACRO_NAME, "The value of this macro should be provided for logging.",
				false));
		return types;
	}
}
