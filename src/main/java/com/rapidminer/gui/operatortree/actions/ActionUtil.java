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
package com.rapidminer.gui.operatortree.actions;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * This util capsulates functionality to pass ports through for disabled/removed Operators
 *
 * @author Andreas Timm
 * @since 8.0
 */
public final class ActionUtil {

	private ActionUtil() {
		throw new UnsupportedOperationException("Initialization not available");
	}

	/**
	 * Pass input to output ports where possible, where meta info matches
	 *
	 * @param op the operator that holds input and output connections
	 */
	public static void doPassthroughPorts(Operator op) {
		List<Port> toUnlock = new LinkedList<>();
		try {
			// disconnect and pass through
			List<OutputPort> sources = new LinkedList<>();
			for (InputPort in : op.getInputPorts().getAllPorts()) {
				if (in.isConnected() && in.getSource().getPorts().getOwner().getOperator().isEnabled()) {
					sources.add(in.getSource());
					toUnlock.add(in.getSource());
					in.getSource().lock();
				}
			}
			for (OutputPort in : sources) {
				in.disconnect();
			}

			List<InputPort> destinations = new LinkedList<>();
			for (OutputPort out : op.getOutputPorts().getAllPorts()) {
				if (out.isConnected() && out.getDestination().getPorts().getOwner().getOperator().isEnabled()) {
					destinations.add(out.getDestination());
					toUnlock.add(out.getDestination());
					out.getDestination().lock();
				}
			}
			for (InputPort in : destinations) {
				in.getSource().disconnect();
			}

			reconnectPorts(sources, destinations);
		} finally {
			for (Port port : toUnlock) {
				port.unlock();
			}
		}
	}

	private static void reconnectPorts(List<OutputPort> sources, List<InputPort> destinations) {
		for (OutputPort source : sources) {
			Iterator<InputPort> i = destinations.iterator();
			while (i.hasNext()) {
				InputPort dest = i.next();
				MetaData metaData = null;
				try {
					metaData = source.getMetaData(MetaData.class);
				} catch (IncompatibleMDClassException e) {
					// so it is null and ignored
				}
				if (metaData != null && dest.isInputCompatible(metaData, CompatibilityLevel.PRE_VERSION_5)) {
					source.connectTo(dest);
					i.remove();
					break;
				}
			}
		}
	}
}