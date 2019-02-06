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

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;

import java.util.Collection;


/**
 * A port that receives data from a connected output port. Operators query their input ports for
 * data.
 * 
 * @author Simon Fischer
 */
public interface InputPort extends Port {

	/**
	 * Receives data from the output port. Throws an {@link PortException} if data has already been
	 * set. (Called by {@link OuptutPort#deliver(Object)} . *
	 */
	public void receive(IOObject object);

	/** Does the same as {@link #receive(Object)} but only with meta data. */
	public void receiveMD(MetaData metaData);

	/** Returns the meta data received by {@link #receiveMD(MetaData)}. */
	@Override
	public MetaData getMetaData();

	// /** Called by the OutputPort when it connects to this InputPort. */
	// public void connect(OutputPort outputPort);

	/** Returns the output port to which this input port is connected. */
	public OutputPort getSource();

	/** Adds a precondition to this input port. */
	public void addPrecondition(Precondition precondition);

	/** Returns a collection (view) of all preconditions assigned to this InputPort. */
	public Collection<Precondition> getAllPreconditions();

	/** Checks all registered preconditions. */
	public void checkPreconditions();

	/** Returns true if the given input is compatible with the preconditions. */
	public boolean isInputCompatible(MetaData input, CompatibilityLevel level);

	/** Returns a human readable representation of the preconditions. */
	public String getPreconditionDescription();

	/**
	 * This will add the given listener to this port. It is informed whenever the method
	 * {@link #receiveMD(MetaData)} is called.
	 */
	public void registerMetaDataChangeListener(MetaDataChangeListener listener);

	/**
	 * Removes the given listener again.
	 */
	public void removeMetaDataChangeListener(MetaDataChangeListener listener);

}
