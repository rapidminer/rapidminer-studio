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

import java.util.Iterator;
import java.util.List;

import com.rapidminer.operator.meta.FeatureIterator;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * <p>
 * (Re-)Define macros for the current process. Macros will be replaced in the value strings of
 * parameters by the macro values defined in the parameter list of this operator.
 * </p>
 * 
 * <p>
 * In the parameter list of this operator, you have to define the macro name (without the enclosing
 * brackets) and the macro value. The defined macro can then be used in all succeeding operators as
 * parameter value for string type parameters. A macro must then be enclosed by
 * &quot;MACRO_START&quot; and &quot;MACRO_END&quot;.
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
 * arbitrary string during the process run. Please note also that several other short macros exist,
 * e.g. MACRO_STARTaMACRO_END for the number of times the current operator was applied. Please refer
 * to the section about macros in the RapidMiner tutorial. Please note also that other operators
 * like the {@link FeatureIterator} also add specific macros.
 * </p>
 * 
 * @author Ingo Mierswa
 */

public class MacroDefinitionOperator extends Operator {

	/** The parameter name for &quot;The values of the user defined macros.&quot; */
	public static final String PARAMETER_VALUES = "values";
	public static final String PARAMETER_MACROS = "macros";

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public MacroDefinitionOperator(OperatorDescription description) {
		super(description);

		dummyPorts.start();

		getTransformer().addRule(dummyPorts.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		List<String[]> macros = getParameterList(PARAMETER_MACROS);
		Iterator<String[]> i = macros.iterator();
		while (i.hasNext()) {
			String[] macroDefinition = i.next();
			String macro = macroDefinition[0];
			String value = macroDefinition[1];
			getProcess().getMacroHandler().addMacro(macro, value);
		}

		dummyPorts.passDataThrough();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeList(PARAMETER_MACROS, "The list of macros defined by the user.",
				new ParameterTypeString("macro_name", "The macro name."), new ParameterTypeString(PARAMETER_VALUES,
				"The value of this macro.", false), false);
		type.setPrimary(true);
		types.add(type);
		return types;
	}
}
