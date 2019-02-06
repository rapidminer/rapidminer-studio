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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.InputMissingMetaDataError;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorService;


/**
 * This operator can be used to store the given IOObject into the process under the specified name
 * (the IOObject will be &quot;hidden&quot; and can not be directly accessed by following operators.
 * In order to retrieve the stored object and make it again accessible, you can use the operator
 * {@link IORetrievalOperator}. The combination of those two operators can be used to build complex
 * processes where an input object is used in completely different parts or loops of processes.
 * 
 * @author Ingo Mierswa
 */
public class IOStorageOperator extends Operator {

	private final InputPort storeInput = getInputPorts().createPort("store");
	private final OutputPort storedOutput = getOutputPorts().createPort("stored");

	public static final String PARAMETER_NAME = "name";

	public static final String PARAMETER_IO_OBJECT = "io_object";

	public static final String PARAMETER_STORE_WHICH = "store_which";

	public static final String PARAMETER_REMOVE_FROM_PROCESS = "remove_from_process";

	public IOStorageOperator(OperatorDescription description) {
		super(description);
		storeInput.addPrecondition(new Precondition() {

			@Override
			public void assumeSatisfied() {
				storeInput.receiveMD(new MetaData(getSelectedClass()));
			}

			@Override
			public void check(MetaData metaData) {
				Class<? extends IOObject> selected = getSelectedClass();
				if (metaData == null) {
					storeInput.addError(new InputMissingMetaDataError(storeInput, selected));
				} else {
					if (!selected.isAssignableFrom(metaData.getObjectClass())) {
						storeInput.addError(new InputMissingMetaDataError(storeInput, selected, metaData.getObjectClass()));
					}
				}
			}

			@Override
			public String getDescription() {
				return "expect: " + getSelectedClass();
			}

			@Override
			public boolean isCompatible(MetaData input, CompatibilityLevel level) {
				boolean result = input != null && getSelectedClass().isAssignableFrom(input.getObjectClass());
				return result;
			}

			@Override
			public MetaData getExpectedMetaData() {
				return new MetaData(IOObject.class);
			}

		});
		getTransformer().addRule(new PassThroughRule(storeInput, storedOutput, false));
	}

	@Override
	public void doWork() throws OperatorException {
		IOObject object = storeInput.getData(IOObject.class);
		if (getParameterAsBoolean(PARAMETER_REMOVE_FROM_PROCESS)) {
			getProcess().store(getParameterAsString(PARAMETER_NAME), object);
		} else {
			getProcess().store(getParameterAsString(PARAMETER_NAME), object.copy());
		}
		storedOutput.deliver(object);
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == storedOutput) {
			return !getParameterAsBoolean(PARAMETER_REMOVE_FROM_PROCESS);
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public LinkedList<OutputPort> preAutoWire(LinkedList<OutputPort> ports) throws UndefinedParameterError {
		Class<? extends IOObject> clazz = getSelectedClass();
		if (clazz != null) {
			OutputPort found = null;
			Iterator<OutputPort> i = ports.descendingIterator();
			int which = isParameterSet(PARAMETER_STORE_WHICH) ? getParameterAsInt(PARAMETER_STORE_WHICH) : 0;
			int hits = 0;
			while (i.hasNext()) {
				OutputPort port = i.next();
				if (port.getMetaData() != null && clazz.isAssignableFrom(port.getMetaData().getObjectClass())) {
					hits++;
					if (hits == which) {
						found = port;
						i.remove();
						break;
					}
				}
			}
			if (found != null) {
				ports.addLast(found);
			}
		}
		return ports;
	}

	private Class<? extends IOObject> getSelectedClass() {
		String ioType;
		try {
			ioType = getParameterAsString(PARAMETER_IO_OBJECT);
		} catch (UndefinedParameterError e) {
			return IOObject.class;
		}
		Class<? extends IOObject> selected = OperatorService.getIOObjectClass(ioType);
		if (selected == null) {
			return IOObject.class;
		} else {
			return selected;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeString(PARAMETER_NAME,
				"The name under which the specified object is stored and can later be retrieved.", false, false));

		Set<String> ioObjects = OperatorService.getIOObjectsNames();
		String[] objectArray = new String[ioObjects.size()];
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

		type = new ParameterTypeInt(PARAMETER_STORE_WHICH, "Defines which input object should be stored.", 1,
				Integer.MAX_VALUE, 1);
		type.setDeprecated();
		types.add(type);

		type = new ParameterTypeBoolean(
				PARAMETER_REMOVE_FROM_PROCESS,
				"Indicates if the stored object should be removed from the process so that following operators can only access this object after retrieving it.",
				true);
		type.setDeprecated();
		types.add(type);

		return types;
	}
}
