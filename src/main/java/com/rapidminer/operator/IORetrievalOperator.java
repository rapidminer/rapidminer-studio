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
import java.util.Set;

import com.rapidminer.adaption.belt.AtPortConverter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorService;


/**
 * This operator can be used to retrieve the IOObject which was previously stored under the
 * specified name. In order to store an object to make it again accessible, you can use the operator
 * {@link IOStorageOperator}. The combination of those two operator can be used to build complex
 * processes where an input object is used in completely different parts or loops of processes.
 * 
 * @author Ingo Mierswa
 */
public class IORetrievalOperator extends Operator {

	public static final String PARAMETER_NAME = "name";

	public static final String PARAMETER_IO_OBJECT = "io_object";

	public static final String PARAMETER_REMOVE_FROM_STORE = "remove_from_store";

	private String[] objectArray = null;

	private final OutputPort resultOutput = getOutputPorts().createPort("result");

	public IORetrievalOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				Class<? extends IOObject> clazz;
				try {
					clazz = getSelectedClass();
					if (clazz != null) {
						resultOutput.deliverMD(new MetaData(clazz));
					} else {
						resultOutput.deliverMD(new MetaData(IOObject.class));
					}
				} catch (UndefinedParameterError e) {
					getLogger().fine("Cannot transform meta data: " + e);
				}
			}
		});
	}

	private Class<? extends IOObject> getSelectedClass() throws UndefinedParameterError {
		String ioType = getParameterAsString(PARAMETER_IO_OBJECT);
		Class<? extends IOObject> selected = OperatorService.getIOObjectClass(ioType);
		if (selected != null) {
			return selected;
		} else {
			return IOObject.class;
		}
	}

	@Override
	public void doWork() throws OperatorException {
		Class<? extends IOObject> clazz = getSelectedClass();
		IOObject object = null;
		if (clazz != null) {
			String name = getParameterAsString(PARAMETER_NAME);
			object = getProcess().retrieve(name, getParameterAsBoolean(PARAMETER_REMOVE_FROM_STORE));

			if (object == null) {
				throw new UserError(this, 941, name);
			}

			if (!clazz.isInstance(object) && !AtPortConverter.isConvertible(object.getClass(), clazz)) {
				throw new UserError(this, 940, name, objectArray[getParameterAsInt(PARAMETER_IO_OBJECT)]);
			}

			if (getParameterAsBoolean(PARAMETER_REMOVE_FROM_STORE)) {
				resultOutput.deliver(object);
			} else {
				resultOutput.deliver(object.copy());
			}
		} else {
			resultOutput.deliver(null);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeString(PARAMETER_NAME,
				"The name under which the specified object is stored and can later be retrieved.", false, false));

		Set<String> ioObjects = OperatorService.getIOObjectsNames();
		this.objectArray = new String[ioObjects.size()];
		Iterator<String> i = ioObjects.iterator();
		int exampleSetClassIndex = 0;
		int index = 0;
		while (i.hasNext()) {
			String name = i.next();
			if (ExampleSet.class.getSimpleName().equals(name)) {
				exampleSetClassIndex = index;
			}
			objectArray[index++] = name;
		}

		ParameterType type = new ParameterTypeCategory(PARAMETER_IO_OBJECT,
				"The class of the object which should be stored.", objectArray, exampleSetClassIndex);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeBoolean(
				PARAMETER_REMOVE_FROM_STORE,
				"Indicates if the stored object should be removed from the process store so that following operators can retrieve it again from the store.",
				true, false));

		return types;
	}
}
