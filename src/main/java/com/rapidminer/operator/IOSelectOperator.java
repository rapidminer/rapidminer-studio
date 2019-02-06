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

import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorService;


/**
 * <p>
 * This operator allows to choose special IOObjects from the given input. Bringing an IOObject to
 * the front of the input queue allows the next operator to directly perform its action on the
 * selected object. Please note that counting for the parameter value starts with one, but usually
 * the IOObject which was added at last gets the number one, the object added directly before get
 * number two and so on.
 * </p>
 *
 * <p>
 * The user can specify with the parameter <code>delete_others</code> what will happen to the
 * non-selected input objects of the specified type: if this parameter is set to true, all other
 * IOObjects of the specified type will be removed by this operator. Otherwise (default), the
 * objects will all be kept and the selected objects will just be brought into front.
 * </p>
 *
 * @author Thomas Harzer, Ingo Mierswa
 */
public class IOSelectOperator extends Operator {

	public static final String PARAMETER_IO_OBJECT = "io_object";

	public static final String PARAMETER_SELECT_WHICH = "select_which";

	public static final String PARAMETER_DELETE_OTHERS = "delete_others";

	private String[] objectArray = null;

	public IOSelectOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() {
		getLogger().info(
				"IOSelector is deprecated and does nothing. It is only used while importing processes from earlier versions. After that, IOSelectors can be deleted.");
	}

	@SuppressWarnings("deprecation")
	@Override
	protected LinkedList<OutputPort> preAutoWire(LinkedList<OutputPort> readyOutputs) throws OperatorException {
		getLogger().info("Simulating IOSelectOperator with old stack: " + readyOutputs);
		Class<? extends IOObject> clazz = getSelectedClass();
		boolean deleteOthers = getParameterAsBoolean(PARAMETER_DELETE_OTHERS);
		int number = getParameterAsInt(PARAMETER_SELECT_WHICH);
		int hits = 0;
		OutputPort myPort = null;
		Iterator<OutputPort> i = readyOutputs.descendingIterator();
		int count = 0;
		while (i.hasNext()) {
			OutputPort port = i.next();
			if (!port.shouldAutoConnect()) {
				continue;
			}
			if (port.getMetaData() != null && clazz.isAssignableFrom(port.getMetaData().getObjectClass())) {
				hits++;
				if (hits == number) {
					myPort = port;
					i.remove();
				} else if (deleteOthers) {
					count++;
					i.remove();
				}
			}
		}
		if (myPort != null) {
			readyOutputs.addLast(myPort);
			getLogger().info("Bringing output port to front: " + myPort.getSpec());
		}
		if (count > 0) {
			getLogger().info("Deleted " + myPort.getSpec() + " output ports.");
		}
		getLogger().info("New stack is: " + readyOutputs);
		return readyOutputs;
	}

	private Class<? extends IOObject> getSelectedClass() throws UndefinedParameterError {
		int ioType = getParameterAsInt(PARAMETER_IO_OBJECT);
		if (objectArray != null) {
			return OperatorService.getIOObjectClass(objectArray[ioType]);
		} else {
			return null;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		Set<String> ioObjects = OperatorService.getIOObjectsNames();
		this.objectArray = new String[ioObjects.size()];
		Iterator<String> i = ioObjects.iterator();
		int index = 0;
		while (i.hasNext()) {
			objectArray[index++] = i.next();
		}
		ParameterType type = new ParameterTypeCategory(PARAMETER_IO_OBJECT,
				"The class of the object(s) which should be removed.", objectArray, 0);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_SELECT_WHICH, "Defines which input object should be selected.", 1,
				Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_DELETE_OTHERS,
				"Indicates if the other non-selected objects should be deleted.", false));
		return types;
	}
}
