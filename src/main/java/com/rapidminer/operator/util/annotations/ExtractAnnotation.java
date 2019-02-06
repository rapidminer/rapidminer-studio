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
package com.rapidminer.operator.util.annotations;

import com.rapidminer.MacroHandler;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;

import java.util.List;


/**
 * @author Marius Helf
 * 
 */
public class ExtractAnnotation extends Operator {

	private InputPort inputPort = getInputPorts().createPort("object", IOObject.class);
	private OutputPort outputPort = getOutputPorts().createPort("object");

	public static final String PARAMETER_MACRO_NAME = "macro";
	public static final String PARAMETER_ANNOTATION = "annotation";
	public static final String PARAMETER_EXTRACT_ALL = "extract_all";
	public static final String PARAMETER_NAME_PREFIX = "name_prefix";
	private static final String PARAMETER_FAIL_ON_MISSING = "fail_on_missing";

	/**
	 * @param description
	 */
	public ExtractAnnotation(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(inputPort, outputPort);
	}

	@Override
	public void doWork() throws OperatorException {
		IOObject data = inputPort.getData(IOObject.class);

		Annotations annotations = data.getAnnotations();
		MacroHandler macroHandler = getProcess().getMacroHandler();

		if (getParameterAsBoolean(PARAMETER_EXTRACT_ALL)) {
			String prefix = getParameterAsString(PARAMETER_NAME_PREFIX);
			if (prefix == null) {
				prefix = "";
			}
			for (String annotation : annotations.getDefinedAnnotationNames()) {
				String macroName = prefix + annotation;
				String value = annotations.getAnnotation(annotation);
				macroHandler.addMacro(macroName, value);
			}
		} else {
			String macroName = getParameterAsString(PARAMETER_MACRO_NAME);
			String annotation = getParameterAsString(PARAMETER_ANNOTATION);
			String value = annotations.getAnnotation(annotation);
			if (value == null) {
				if (getParameterAsBoolean(PARAMETER_FAIL_ON_MISSING)) {
					throw new UserError(this, "annotations.annotation_not_exist", annotation);
				} else {
					value = "";
				}
			}
			macroHandler.addMacro(macroName, value);
		}

		outputPort.deliver(inputPort.getData(IOObject.class));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType extractAll = new ParameterTypeBoolean(
				PARAMETER_EXTRACT_ALL,
				"If checked, all annotations are extracted to macros named the same as the annotations. Optionally, you can define a name prefix which is prepended to the macro names",
				false, true);
		types.add(extractAll);

		ParameterType type;

		type = new ParameterTypeString(PARAMETER_MACRO_NAME, "Defines the name of the created macro", true, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_EXTRACT_ALL, true, false));
		types.add(type);

		type = new ParameterTypeString(PARAMETER_ANNOTATION, "The name of the annotation to be extracted", true, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_EXTRACT_ALL, true, false));
		types.add(type);

		type = new ParameterTypeBoolean(
				PARAMETER_FAIL_ON_MISSING,
				"If checked, the operator breaks if the specified annotation can't be found; if unchecked, in that case an empty macro will be created.",
				true, true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_EXTRACT_ALL, false, false));
		types.add(type);

		type = new ParameterTypeString(PARAMETER_NAME_PREFIX, "A prefix which is prepended to all macro names", true, true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_EXTRACT_ALL, false, true));
		types.add(type);

		return types;
	}
}
