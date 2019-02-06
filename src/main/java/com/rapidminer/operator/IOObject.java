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
import com.rapidminer.operator.ports.ProcessingStep;
import com.rapidminer.tools.LoggingHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;


/**
 * This interface must be implemented by all objects that can be input/output objects for Operators.
 * The copy method is necessary in cases where meta operator chains want to copy the input
 * IOContainer before it is given to the children operators. Please note that the method only need
 * to be implemented like a usual <code>clone</code> method for IO objects which can be altered
 * after creation. In all other cases the implementation can simply return the same object. Hence,
 * we use the name <code>copy</code> instead of <code>clone</code>.
 * 
 * @author Ingo Mierswa
 */
public interface IOObject extends Serializable {

	/** Sets the source of this IOObject. */
	public void setSource(String sourceName);

	/** Returns the source of this IOObject (might return null if the source is unknown). */
	public String getSource();

	/**
	 * This method is called if this IOObject is put out of the specified port after being created
	 * or processed by the given operator. This method has to keep track of the processing stations
	 * of this object, so that they are remembered and might be returned by the method
	 * getProcessingHistory.
	 */
	public void appendOperatorToHistory(Operator operator, OutputPort port);

	/**
	 * This method must return a list of each step of processing steps this IOObject has been made
	 * including it's creating operator.
	 */
	public List<ProcessingStep> getProcessingHistory();

	/**
	 * Should return a copy of this IOObject. Please note that the method can usually be implemented
	 * by simply returning the same object (i.e. return this;). The object needs only to be cloned
	 * in cases the IOObject can be altered after creation. This is for example the case for
	 * ExampleSets.
	 */
	public IOObject copy();

	/** Writes the object data into a stream. */
	public void write(OutputStream out) throws IOException;

	/**
	 * Gets the logging associated with the operator currently working on this IOObject or the
	 * global log service if no operator was set.
	 */
	public LoggingHandler getLog();

	/**
	 * Sets the current working operator, i.e. the operator which is currently working on this
	 * IOObject. This might be used for example for logging.
	 */
	public void setLoggingHandler(LoggingHandler loggingHandler);

	public Annotations getAnnotations();

	/**
	 * Returns user specified data attached to the IOObject. The key has to be a fully qualified
	 * name
	 */
	public Object getUserData(String key);

	/**
	 * Specify user data attached to the IOObject. The key has to be a fully qualified name
	 * 
	 * @return the previous value associated with key, or null if there was no mapping for key. (A
	 *         null return can also indicate that the user data previously associated null with
	 *         key.)
	 */
	public Object setUserData(String key, Object value);
}
