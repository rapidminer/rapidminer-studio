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

import com.rapidminer.operator.meta.FeatureIterator;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;

import java.util.List;


/**
 * <p>
 * (Re-)Define macros for the current process. Macros will be replaced in the value strings of
 * parameters by the macro values defined as a parameter of this operator. In contrast to the usual
 * MacroDefinitionOperator, this operator only supports the definition of a single macro and can
 * hence be used inside of parameter iterations.
 * </p>
 * 
 * <p>
 * You have to define the macro name (without the enclosing brackets) and the macro value. The
 * defined macro can then be used in all succeeding operators as parameter value. A macro must then
 * be enclosed by &quot;MACRO_START&quot; and &quot;MACRO_END&quot;.
 * </p>
 * 
 * <p>
 * There are several predefined macros:
 * </p>
 * <ul>
 * <li>MACRO_STARTprocess_nameMACRO_END: will be replaced by the name of the process (without path
 * and extension)</li>
 * <li>MACRO_STARTprocess_fileMACRO_END: will be replaced by the file name of the process (with
 * extension)</li>
 * <li>MACRO_STARTprocess_pathMACRO_END: will be replaced by the complete absolute path of the
 * process file</li>
 * </ul>
 * 
 * <p>
 * In addition to those the user might define arbitrary other macros which will be replaced by
 * arbitrary strings during the process run. Please note also that several other short macros exist,
 * e.g. MACRO_STARTaMACRO_END for the number of times the current operator was applied. Please refer
 * to the section about macros in the RapidMiner tutorial. Please note also that other operators
 * like the {@link FeatureIterator} also add specific macros.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class SingleMacroDefinitionOperator extends Operator {

	/** The parameter name for &quot;The values of the user defined macros.&quot; */
	public static final String PARAMETER_MACRO = "macro";
	public static final String PARAMETER_VALUE = "value";

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public SingleMacroDefinitionOperator(OperatorDescription description) {
		super(description);

		dummyPorts.start();

		getTransformer().addRule(dummyPorts.makePassThroughRule());

		addValue(new ValueString("macro_name", "The name of the macro.") {

			@Override
			public String getStringValue() {
				try {
					return getParameterAsString(PARAMETER_MACRO);
				} catch (UndefinedParameterError e) {
					return null;
				}
			}
		});

		addValue(new ValueString("macro_value", "The value of the macro.") {

			@Override
			public String getStringValue() {
				try {
					return getParameterAsString(PARAMETER_VALUE);
				} catch (UndefinedParameterError e) {
					return null;
				}
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		String macro = getParameterAsString(PARAMETER_MACRO);
		String value = getParameterAsString(PARAMETER_VALUE);
		if (value == null) {
			value = "";
		}
		getProcess().getMacroHandler().addMacro(macro, value);

		dummyPorts.passDataThrough();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_MACRO, "The macro name defined by the user.", false, false));
		types.add(new ParameterTypeString(PARAMETER_VALUE, "The macro value defined by the user.", true, false));
		return types;
	}
}
