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
package com.rapidminer.operator.io;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.io.Encoding;

import java.util.LinkedList;
import java.util.List;


/**
 * Superclass of all operators that take a single object as input, save it to disk and return the
 * same object as output. This class is mainly a tribute to the e-LICO DMO.
 * 
 * It defines precondition and a pass through rule for its output port.
 * 
 * @author Simon Fischer
 */
public abstract class AbstractWriter<T extends IOObject> extends Operator {

	private InputPort inputPort = getInputPorts().createPort("input");
	private OutputPort outputPort = getOutputPorts().createPort("through");
	private Class<T> savedClass;

	public AbstractWriter(OperatorDescription description, Class<T> savedClass) {
		super(description);
		this.savedClass = savedClass;
		inputPort.addPrecondition(new SimplePrecondition(inputPort, new MetaData(savedClass)));
		getTransformer().addRule(new PassThroughRule(inputPort, outputPort, false));
	}

	/**
	 * Creates (or reads) the ExampleSet that will be returned by {@link #apply()}.
	 * 
	 * @return the written IOObject itself
	 */
	public abstract T write(T ioobject) throws OperatorException;

	@Override
	public final void doWork() throws OperatorException {
		T ioobject = inputPort.getData(savedClass);
		ioobject = write(ioobject);
		outputPort.deliver(ioobject);
	}

	protected boolean supportsEncoding() {
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.addAll(super.getParameterTypes());
		if (supportsEncoding()) {
			types.addAll(Encoding.getParameterTypes(this));
		}
		return types;
	}
}
