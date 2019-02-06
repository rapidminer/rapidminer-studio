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
package com.rapidminer.operator.execution;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.Port;


/**
 * A simple container for {@link ProcessFlowFilter}s which contain the data and the port from which
 * the data came.
 *
 * @author Marco Boeck
 * @since 7.2
 *
 */
public class FlowData {

	private final IOObject data;

	private final Port port;

	/**
	 * Create a new flow object with data and the port from which the data came.
	 *
	 * @param data
	 *            the data, must not be {@code null}
	 * @param port
	 *            the port, must not be {@code null}
	 */
	public FlowData(IOObject data, Port port) {
		if (data == null) {
			throw new IllegalArgumentException("data must not be null!");
		}
		if (port == null) {
			throw new IllegalArgumentException("port must not be null!");
		}

		this.data = data;
		this.port = port;
	}

	/**
	 * Gets the {@link IOObject} for this flow object
	 *
	 * @return the data, never {@code null}
	 */
	public IOObject getData() {
		return data;
	}

	/**
	 * Gets the {@link Port} for this flow object where the data came from
	 *
	 * @return the port, never {@code null}
	 */
	public Port getPort() {
		return port;
	}
}
