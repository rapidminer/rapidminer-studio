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
package com.rapidminer.operator.ports;

import java.util.Collection;
import java.util.List;

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.tools.Observable;


/**
 * Operators in a process are connected via input and output ports. Whenever an operator generates
 * output (or meta data about output), it is delivered to the connected input port. <br/>
 * This interface defines all behavior and properies common to input and output ports. This is
 * basically names, description etc., as well as adding messages about problems in the process setup
 * and quick fixes.
 * <p>
 * Implementations should extend {@link com.rapidminer.operator.ports.impl.AbstractPort}.
 * 
 * @see Ports
 * 
 * @author Simon Fischer
 */
public interface Port extends Observable<Port> {

	public static final int CLEAR_META_DATA_ERRORS = 1 << 0;
	public static final int CLEAR_METADATA = 1 << 1;
	public static final int CLEAR_DATA = 1 << 2;
	public static final int CLEAR_SIMPLE_ERRORS = 1 << 3;
	public static final int CLEAR_REAL_METADATA = 1 << 4;
	public static final int CLEAR_ALL = CLEAR_META_DATA_ERRORS | CLEAR_METADATA | CLEAR_DATA | CLEAR_SIMPLE_ERRORS
			| CLEAR_REAL_METADATA;

	/** Clears all error types. */
	public static final int CLEAR_ALL_ERRORS = CLEAR_META_DATA_ERRORS | CLEAR_SIMPLE_ERRORS;

	/** Clears all meta data, real and inferred. */
	public static final int CLEAR_ALL_METADATA = CLEAR_METADATA | CLEAR_REAL_METADATA;

	/** A human readable, unique (operator scope) name for the port. */
	public String getName();

	/**
	 * Returns the meta data currently assigned to this port.
	 * 
	 * @deprecated use {@link #getMetaData(Class)} instead
	 */
	@Deprecated
	public MetaData getMetaData();

	/**
	 * Returns the meta data of the desired class or throws an UserError if available meta data
	 * cannot be cast to the desired class. If no meta data is present at all, <code>null</code> is
	 * returned.
	 * 
	 */
	public <T extends MetaData> T getMetaData(Class<T> desiredClass) throws IncompatibleMDClassException;

	/**
	 * This method returns the object of the desired class or throws an UserError if no object is
	 * present or cannot be casted to the desiredClass.
	 *
	 * @throws UserError
	 * 		if data is missing or of wrong class.
	 */
	public <T extends IOObject> T getData(Class<T> desiredClass) throws UserError;

	/**
	 * Returns the last object delivered to the connected {@link InputPort} or received from the
	 * connected {@link OutputPort}
	 * 
	 * @throws UserError
	 *             If data is not of the requested type.
	 */
	public <T extends IOObject> T getDataOrNull(Class<T> desiredClass) throws UserError;

	/**
	 * Same as {@link #getData(true)}.
	 * 
	 * @throws UserError
	 *             if data is missing.
	 * @deprecated use {@link #getData(Class)}
	 */
	@Deprecated
	public <T extends IOObject> T getData() throws OperatorException;

	/**
	 * Returns the last object delivered to the connected {@link InputPort} or received from the
	 * connected {@link OutputPort}.
	 * 
	 * @throws UserError
	 *             If data is not of the requested type.
	 * @deprecated call {@link #getDataOrNull(Class)}
	 */
	@Deprecated
	public <T extends IOObject> T getDataOrNull() throws UserError;

	/**
	 * Returns the last object delivered to the connected {@link InputPort} or received from the connected {@link
	 * OutputPort}. Never throws an exception but instead returns {@code null} if there is no data.
	 *
	 * @return the data at the port or {@code null}
	 * @since 9.4
	 */
	public default IOObject getRawData(){
		// default method for compatibility, overwritten by {@link AbstractPort}
		return getAnyDataOrNull();
	}


	/**
	 * This method returns the object of the desired class or {@link null} if no object is
	 * present or it cannot be cast or converted to the desiredClass. Never throws an exception.
	 *
	 * @return the data cast or converted to the desired class or {@code null}
	 * @since 9.4
	 */
	public default <T extends IOObject> T getDataAsOrNull(Class<T> desiredClass){
		// default method for compatibility, overwritten by {@link AbstractPort}
		try {
			return getDataOrNull(desiredClass);
		} catch (UserError userError) {
			return null;
		}
	}

	/**
	 * Returns the last object delivered to the connected {@link InputPort} or received from the
	 * connected {@link OutputPort}. Never throws an exception. Converts {@link com.rapidminer.adaption.belt.IOTable}s
	 * to {@link com.rapidminer.example.ExampleSet}s.
	 *
	 * @deprecated since 9.4, use {@link #getRawData()} or {@link #getDataAsOrNull(Class)} instead.
	 */
	@Deprecated
	public IOObject getAnyDataOrNull();

	/** Returns the set of ports to which this port belongs. */
	public Ports<? extends Port> getPorts();

	/** Gets a three letter abbreviation of the port's name. */
	public String getShortName();

	/** Returns a human readable description of the ports pre/ and postconditions. */
	public String getDescription();

	/** Returns true if connected to another Port. */
	public boolean isConnected();

	/** Report an error in the current process setup. */
	public void addError(MetaDataError metaDataError);

	/** Returns the set of errors added since the last clear errors. */
	public Collection<MetaDataError> getErrors();

	/**
	 * Clears data, meta data and errors at this port.
	 * 
	 * @param clearFlags
	 *            disjunction of the CLEAR_XX constants.
	 */
	public void clear(int clearFlags);

	/** Returns a sorted list of all quick fixes applicable for this port. */
	public List<QuickFix> collectQuickFixes();

	/** Returns the string "OperatorName.PortName". */
	public String getSpec();

	/**
	 * Indicates whether {@link ExecutionUnit#autoWire()} should simulate the pre RM 5.0 stack
	 * behaviour of {@link IOContainer}. Normally, ports should return true here. However, ports
	 * created by {@link PortPairExtender}s should return false here, since (most of the time) they
	 * only pass data through rather adding new IOObjects to the IOContainer. TODO: delete
	 * 
	 * @deprecated The above reasoning turned out to be unnecessary if implementations in other
	 *             places are correct. We always simulate the stack now. This method is only called
	 *             from within {@link ExecutionUnit#autoWire(CompatibilityLevel, InputPorts,
	 *             LinkedList<OutputPort>)} to keep a reference, but it has no effect on the
	 *             auto-wiring process. We keep this method until the end of the alpha test phase of
	 *             Vega.
	 */
	@Deprecated
	public boolean simulatesStack();

	/**
	 * Locks the port so port extenders do not remove the port if disconnected. unlocks it.
	 */
	public void lock();

	/**
	 * @see #lock()
	 */
	public void unlock();

	/**
	 * @see #lock()
	 */
	public boolean isLocked();

	/** Releases of any hard reference to IOObjects held by this class. */
	void freeMemory();

}
