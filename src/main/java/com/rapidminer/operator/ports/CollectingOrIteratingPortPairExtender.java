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

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * In addition to {@link CollectingPortPairExtender}, this extender can toggle between returning a
 * collection and a single {@link IOObject}. See {@link CollectingPortPairExtender} for details.
 *
 * @author Marco Boeck
 * @since 7.4
 */
public class CollectingOrIteratingPortPairExtender extends CollectingPortPairExtender {

	/**
	 * The mode for the {@link CollectingOrIteratingPortPairExtender} which defines if the ports
	 * should pass on collections or single data entries.
	 *
	 */
	public static enum PortOutputMode {
		/** ports pass on a collection of {@link IOObject}s */
		COLLECTING,

		/** ports pass on single {@link IOObject}s */
		ITERATING;
	}

	private PortOutputMode outputMode = PortOutputMode.COLLECTING;

	public CollectingOrIteratingPortPairExtender(String name, InputPorts inPorts, OutputPorts outPorts) {
		super(name, inPorts, outPorts);
	}

	@Override
	protected MetaData transformMetaData(MetaData md) {
		return outputMode == PortOutputMode.COLLECTING ? new CollectionMetaData(md) : md;
	}

	/**
	 * Changes the output mode of the port pair extender. See {@link PortOutputMode} for details.
	 *
	 * @param outputMode
	 *            one of {@link PortOutputMode}, must not be {@code null}
	 */
	public void setOutputMode(PortOutputMode outputMode) {
		if (outputMode == null) {
			throw new IllegalArgumentException("outputMode must not be null!");
		}

		this.outputMode = outputMode;
	}

	/**
	 * Returns the output mode of the port pair extender. See {@link PortOutputMode} for details.
	 *
	 * @return the mode, never {@code null}
	 */
	public PortOutputMode getOutputMode() {
		return outputMode;
	}

	/**
	 * Resets all output ports by clearing them.
	 */
	@Override
	public void reset() {
		for (PortPair pair : getManagedPairs()) {
			pair.getOutputPort().clear(Port.CLEAR_DATA);
		}
	}

	/**
	 * Same as {@link CollectingPortPairExtender#getData(Class)} but does not omit {@code null}
	 * results. This ensures the correct result order in the process view on the operator output
	 * ports.
	 */
	@Override
	public <T extends IOObject> List<T> getData(Class<T> desiredClass) throws UserError {
		List<PortPair> managedPairs = getManagedPairs();
		List<T> results = new ArrayList<T>(managedPairs.size());
		for (PortPair pair : managedPairs) {
			T data = pair.getInputPort().<T> getDataOrNull(desiredClass);
			results.add(data);
		}
		return results;
	}

	@Override
	public void collect() {
		// only collect if mode is set correctly
		if (outputMode != PortOutputMode.COLLECTING) {
			return;
		}

		super.collect();
		// now clear all input ports to avoid multiple entries if multiple iterations are collected
		synchronized (this) {
			for (PortPair pair : getManagedPairs()) {
				pair.getInputPort().clear(InputPort.CLEAR_DATA);
			}
		}
	}
}
