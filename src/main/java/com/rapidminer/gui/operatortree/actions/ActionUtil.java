/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
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
			List<OutputPort> sources = collectAndLockPorts(op.getInputPorts(), toUnlock);
			sources.forEach(Port::disconnect);

			List<InputPort> destinations = collectAndLockPorts(op.getOutputPorts(), toUnlock);
			destinations.forEach(Port::disconnect);

			reconnectPorts(sources, destinations);
		} finally {
			for (Port port : toUnlock) {
				port.unlock();
			}
		}
	}

	/**
	 * Collect and lock connected ports
	 * @since 9.7
	 */
	private static <P extends Port<P, ?>> List<P> collectAndLockPorts(Ports<? extends Port<?, P>> ports, List<Port> toUnlock) {
		// get opposites of connected ports
		return ports.getAllPorts().stream().filter(Port::isConnected).map(Port::getOpposite)
				// filter out connections to disabled operators and lock opposites
				.filter(p -> p.getPorts().getOwner().getOperator().isEnabled()).peek(toUnlock::add).peek(Port::lock)
				.collect(Collectors.toList());
	}

	/**
	 * Reconnect ports where possible, cutting out the disabled operators in the middle
	 */
	private static void reconnectPorts(List<OutputPort> sources, List<InputPort> destinations) {
		for (OutputPort source : sources) {
			for (InputPort destination : destinations) {
				MetaData metaData = null;
				try {
					metaData = source.getMetaData(MetaData.class);
				} catch (IncompatibleMDClassException e) {
					// so it is null and ignored
				}
				if (metaData != null && destination.isInputCompatible(metaData, CompatibilityLevel.PRE_VERSION_5)) {
					source.connectTo(destination);
					destinations.remove(destination);
					break;
				}
			}
		}
	}
}