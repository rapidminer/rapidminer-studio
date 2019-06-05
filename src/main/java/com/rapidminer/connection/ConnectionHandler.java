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
package com.rapidminer.connection;

import java.util.Collections;
import java.util.Map;

import com.rapidminer.connection.util.GenericHandler;


/**
 * An interface for handler/factory for {@link ConnectionInformation ConnectionInformations}. Implementations provide
 * the possibility to create new {@link ConnectionInformation ConnectionInformations}. They can be registered using
 * {@link ConnectionHandlerRegistry#registerHandler(GenericHandler) ConnectionHandlerRegistry.registerHandler}.
 * Additionally implementations can provide a set of additional actions for the connections.
 *
 * @author Jan Czogalla
 * @since 9.3
 * @see com.rapidminer.tools.config.ConfigurableConnectionHandler ConfigurableConnectionHandler
 */
public interface ConnectionHandler extends GenericHandler<ConnectionInformation> {

	/**
	 * Creates a new instance of {@link ConnectionInformation} with the given name, this handler's type
	 * and an implementation dependent id.
	 *
	 * @param name
	 * 		the name of the new connection; must not be {@code null}
	 * @see ConnectionInformationBuilder
	 */
	ConnectionInformation createNewConnectionInformation(String name);

	/**
	 * A map of name or key/runnable pairs, representing additional actions
	 *
	 * @return an empty map by default
	 */
	default Map<String, Runnable> getAdditionalActions() {
		return Collections.emptyMap();
	}
}
