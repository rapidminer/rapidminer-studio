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

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
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
 * In some cases you might want to apply different parts of the process on the same input object.
 * You can use this operator to create <code>k</code> copies of the given input object.<br/>
 * This operator is deprecated and should be replaced by IOMultiplier2. This happens automatically
 * when old processes are imported.
 * 
 * @author Ingo Mierswa
 * 
 */
public class IOMultiplyOperator extends Operator {

	public static final String PARAMETER_NUMBER_OF_COPIES = "number_of_copies";

	public static final String PARAMETER_IO_OBJECT = "io_object";

	public static final String PARAMETER_MULTIPLY_TYPE = "multiply_type";

	public static final String PARAMETER_MULTIPLY_WHICH = "multiply_which";

	private static final String[] MULTIPLY_TYPES = new String[] { "multiply_one", "multiply_all" };

	private static final int MULTIPLY_ONE = 0;

	private static final int MULTIPLY_ALL = 1;

	private String[] objectArray = null;

	public IOMultiplyOperator(OperatorDescription description) {
		super(description);
	}

	private Class<? extends IOObject> getSelectedClass() throws UndefinedParameterError {
		String ioName = getParameterAsString(PARAMETER_IO_OBJECT);
		return OperatorService.getIOObjectClass(ioName);
	}

	/**
	 * For MULTIPLY_ONE, brings the given output port to the front of the stack. For MULTIPLAY_ALL,
	 * creates as many input ports as IOObjects of the correct type are present in the input.
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected LinkedList<OutputPort> preAutoWire(LinkedList<OutputPort> readyOutputs) throws OperatorException {
		getInputPorts().removeAll();
		getOutputPorts().removeAll();
		getTransformer().clearRules();

		Class<? extends IOObject> desiredClass = getSelectedClass();
		int hits = 0;
		if (desiredClass != null) {
			switch (getParameterAsInt(PARAMETER_MULTIPLY_TYPE)) {
				case MULTIPLY_ONE:
					OutputPort found = null;
					int number = getParameterAsInt(PARAMETER_MULTIPLY_WHICH);
					Iterator<OutputPort> i = readyOutputs.descendingIterator();
					while (i.hasNext()) {
						OutputPort port = i.next();
						MetaData md = port.getMetaData();
						if ((md != null) && desiredClass.isAssignableFrom(md.getObjectClass())) {
							hits++;
							if (hits == number) {
								getInputPorts().createPort("input_1", desiredClass);
								getLogger().info("IOMultiplier created temporary input: input_1");
								found = port;
								break;
							}
						}
					}
					if (found != null) {
						readyOutputs.remove(found);
						readyOutputs.addLast(found);
					}
					break;
				case MULTIPLY_ALL:
					i = readyOutputs.descendingIterator();
					while (i.hasNext()) {
						OutputPort port = i.next();
						MetaData md = port.getMetaData();
						if ((md != null) && desiredClass.isAssignableFrom(md.getObjectClass())) {
							hits++;
							InputPort inPort = getInputPorts().createPort("input_" + hits, false);
							getLogger().info("IOMultiplier created temporary input: input_" + hits);
							inPort.addPrecondition(new SimplePrecondition(inPort, new MetaData(desiredClass)));
							getInputPorts().addPort(inPort);
							// port.connectTo(inPort);
						}
					}
			}
		}
		int copies = getParameterAsInt(PARAMETER_MULTIPLY_WHICH) + 1; // +1 for original
		for (int i = 0; i < getInputPorts().getNumberOfPorts(); i++) {
			InputPort in = getInputPorts().getPortByIndex(i);
			for (int j = 0; j < copies; j++) {
				OutputPort out = getOutputPorts().createPort("output_" + (i + 1) + "_" + (j + 1));
				getLogger().info("IOMultiplier created temporary output: output_" + (i + 1) + "_" + (j + 1));
				getTransformer().addPassThroughRule(in, out);
			}
		}
		return readyOutputs;
	}

	@Override
	public void doWork() throws OperatorException {
		getLogger().warning("This operator is deprecated and should have been replaced during import by IOMultiplier2");
		/*
		 * List<IOObject> result = new LinkedList<IOObject>(); Class<? extends IOObject> clazz =
		 * getSelectedClass(); int numberOfCopies = getParameterAsInt(PARAMETER_NUMBER_OF_COPIES);
		 * if (clazz != null) { switch (getParameterAsInt(PARAMETER_MULTIPLY_TYPE)) { case
		 * MULTIPLY_ONE: int number = getParameterAsInt(PARAMETER_MULTIPLY_WHICH); IOObject ioObject
		 * = getInput(clazz, (number - 1)); addCopies(result, ioObject, numberOfCopies); break; case
		 * MULTIPLY_ALL: try { while (true) { ioObject = getInput(clazz); addCopies(result,
		 * ioObject, numberOfCopies); } } catch (MissingIOObjectException e) {} break; } }
		 * IOObject[] resultArray = new IOObject[result.size()]; result.toArray(resultArray);
		 */
	}

	/*
	 * private void addCopies(List<IOObject> result, IOObject ioObject, int numberOfCopies) {
	 * result.add(ioObject); for (int i = 0; i < numberOfCopies; i++) { result.add(ioObject.copy());
	 * } }
	 */

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
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_COPIES,
				"The number of copies which should be created.", 1, Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_IO_OBJECT, "The class of the object(s) which should be multiplied.",
				objectArray, 0);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_MULTIPLY_TYPE, "Defines the type of multiplying.", MULTIPLY_TYPES,
				MULTIPLY_ONE);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MULTIPLY_WHICH,
				"Defines which input object should be multiplied (only used for deletion type 'multiply_one').", 1,
				Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
