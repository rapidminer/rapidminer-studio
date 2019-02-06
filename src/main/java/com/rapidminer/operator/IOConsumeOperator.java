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

import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorService;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Most RapidMiner operators should define their desired input and delivered output in a senseful
 * way. In some cases operators can produce additional output which is indicated with a boolean
 * parameter. Other operators are able to deliver their input as output instead of consuming it
 * (parameter keep_...). However, in some cases it might be usefull to delete unwanted output to
 * ensure that following operators use the correct input object. Furthermore, some operators produce
 * additional unneeded and therefore unconsumed output. In an iterating operator chain this unneeded
 * output will grow with each iteration. Therefore, the IOConsumeOperator can be used to delete one
 * (the n-th) object of a given type (indicated by delete_one), all input objects of a given type
 * (indicated by delete_all), all input objects but those of a given type (indicated by
 * delete_all_but), or all input objects of the given type except for the n-th object of the type.
 * 
 * @author Ingo Mierswa
 */
public class IOConsumeOperator extends Operator {

	public static final String PARAMETER_IO_OBJECT = "io_object";

	public static final String PARAMETER_DELETION_TYPE = "deletion_type";

	public static final String PARAMETER_DELETE_WHICH = "delete_which";

	public static final String PARAMETER_EXCEPT = "except";

	private static final String[] DELETION_TYPES = new String[] { "delete_one", "delete_all", "delete_all_but",
			"delete_all_but_number" };

	public static final int DELETE_ONE = 0;

	public static final int DELETE_ALL = 1;

	public static final int DELETE_ALL_BUT = 2;

	public static final int DELETE_ALL_BUT_NUMBER = 3;

	private String[] objectArray = null;

	public IOConsumeOperator(OperatorDescription description) {
		super(description);
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
	public void doWork() {
		getLogger().info(
				"IOConsumer is deprecated and does nothing. It is only used while importing processes from earlier versions. After that, IOConsumers can be deleted.");
	}

	@SuppressWarnings("deprecation")
	@Override
	protected LinkedList<OutputPort> preAutoWire(LinkedList<OutputPort> readyOutputs) throws OperatorException {
		getLogger().info("Simulating IOConsumeOperator with old stack: " + readyOutputs);
		Class<? extends IOObject> clazz = getSelectedClass();
		Iterator<OutputPort> i = readyOutputs.descendingIterator();
		if (clazz != null) {
			switch (getParameterAsInt(PARAMETER_DELETION_TYPE)) {
				case DELETE_ONE:
					int number = getParameterAsInt(PARAMETER_DELETE_WHICH);
					int hits = 0;
					while (i.hasNext()) {
						OutputPort port = i.next();
						if (port.shouldAutoConnect() && (port.getMetaData() != null)
								&& (clazz.isAssignableFrom(port.getMetaData().getObjectClass()))) {
							hits++;
							if (hits == number) {
								i.remove();
								getLogger().info("Deleted " + number + ". " + clazz.getName() + ".");
								break;
							}
						}
					}
					break;
				case DELETE_ALL:
					int counter = 0;
					while (i.hasNext()) {
						OutputPort port = i.next();
						if (port.shouldAutoConnect() && (port.getMetaData() != null)
								&& (clazz.isAssignableFrom(port.getMetaData().getObjectClass()))) {
							counter++;
							i.remove();
						}
					}
					getLogger().info("Deleted " + counter + " " + clazz.getName() + ".");
					break;
				case DELETE_ALL_BUT:
					counter = 0;
					while (i.hasNext()) {
						OutputPort port = i.next();
						if (port.shouldAutoConnect() && (port.getMetaData() != null)
								&& (!clazz.isAssignableFrom(port.getMetaData().getObjectClass()))) {
							counter++;
							i.remove();
						}
					}
					getLogger().info("Deleted " + counter + " input objects.");
					break;
				case DELETE_ALL_BUT_NUMBER:
					counter = 0;
					hits = 0;
					number = getParameterAsInt(PARAMETER_EXCEPT);
					while (i.hasNext()) {
						OutputPort port = i.next();
						if (port.shouldAutoConnect() && (port.getMetaData() != null)
								&& (clazz.isAssignableFrom(port.getMetaData().getObjectClass()))) {
							hits++;
							if (hits != number) {
								i.remove();
								counter++;
								getLogger().info("Deleted " + number + ". " + clazz.getName() + ".");
								break;
							}
						}
					}
					getLogger().info("Deleted " + counter + " " + clazz.getName() + ".");
					// IOContainer input = getInput();
					// number = getParameterAsInt(PARAMETER_EXCEPT);
					// IOObject temp = getInput(clazz, number - 1);
					// while (input.contains(clazz)) {
					// input.remove(clazz);
					// counter++;
					// }
					// setInput(input.prepend(temp));

					break;
			}
		}
		getLogger().info("New stack: " + readyOutputs);
		return readyOutputs;
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
		type = new ParameterTypeCategory(PARAMETER_DELETION_TYPE, "Defines the type of deletion.", DELETION_TYPES,
				DELETE_ALL);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_DELETE_WHICH,
				"Defines which input object should be deleted (only used for deletion type 'delete_one').", 1,
				Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_EXCEPT,
				"Defines which input object should not be deleted (only used for deletion type 'delete_one_but_number').",
				1, Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
