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

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.math.Averagable;
import com.rapidminer.tools.math.AverageVector;
import com.rapidminer.tools.math.RunVector;

import java.util.List;


/**
 * Collects all average vectors (e.g. PerformanceVectors) from the input and averages them if they
 * are of the same type.
 * 
 * @author Ingo Mierswa
 */
public class AverageBuilder extends Operator {

	InputPortExtender inExtender = new InputPortExtender("averagable", getInputPorts(), new MetaData(AverageVector.class), 2);

	private final OutputPort runOutput = getOutputPorts().createPort("average");

	public AverageBuilder(OperatorDescription description) {
		super(description);
		inExtender.start();
		getTransformer().addRule(inExtender.makeFlatteningPassThroughRule(runOutput));
	}

	@Override
	public void doWork() throws OperatorException {
		RunVector runVector = new RunVector();
		List<AverageVector> averageVectors = inExtender.getData(AverageVector.class, true);
		Class<? extends AverageVector> clazz = null;
		for (AverageVector av : averageVectors) {
			if (clazz == null) {
				clazz = av.getClass();
			} else {
				if (!av.getClass().equals(clazz)) {
					getLogger().warning("Received inputs of different types (" + clazz.getName() + " and " + av.getName()
							+ "). Ignoring the latter.");
					continue;
				}
			}
			runVector.addVector(av);
		}
		if (runVector.size() == 0) {
			throw new UserError(this, "averagable_input_missing", Averagable.class);
		}

		runOutput.deliver(runVector.average());

		// collect AverageVectors
		// Create RunVector for each on a per-class basis
		// Map<Class, RunVector> classMap = new HashMap<Class, RunVector>();
		// while (true) {
		// AverageVector vector = null;
		// try {
		// vector = getInput(AverageVector.class);
		// } catch (MissingIOObjectException e) {
		// break;
		// }
		// addVector(vector, classMap);
		// }
		// //
		// List<AverageVector> averages = new LinkedList<AverageVector>();
		// for (RunVector runVector : classMap.values()) {
		// averages.add(runVector.average());
		// }
		//
		// IOObject[] result = new IOObject[averages.size()];
		// averages.toArray(result);
		// return result;
	}

	// private static void addVector(AverageVector averageVector, Map<Class, RunVector> classMap) {
	// RunVector runVector = classMap.get(averageVector.getClass());
	// if (runVector == null) {
	// runVector = new RunVector();
	// classMap.put(averageVector.getClass(), runVector);
	// }
	// runVector.addVector(averageVector);
	// }

}
