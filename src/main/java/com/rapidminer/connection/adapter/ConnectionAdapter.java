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
package com.rapidminer.connection.adapter;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.legacy.ConversionException;
import com.rapidminer.tools.config.AbstractConfigurable;

/**
 * An adapter helper class for {@link com.rapidminer.tools.config.Configurable Configurables} and
 * {@link ConnectionInformation ConnectionInformations}. Indicates that this configurable supports the
 * connection information mechanism and provides a
 * {@link ConnectionAdapterHandler#convert(ConnectionAdapter) conversion method}.
 * <p>
 * <strong>Note:</strong> Should always be used in conjunction with a {@link ConnectionAdapterHandler}
 *
 * @author Jan Czogalla, Gisa Meier
 * @see ConnectionAdapterHandler
 * @since 9.3
 */
public abstract class ConnectionAdapter extends AbstractConfigurable {

	/** @return always {@code true} */
	@Override
	public final boolean supportsNewConnectionManagement() {
		return true;
	}

	/** @see ConnectionAdapterHandler#convert(ConnectionAdapter) */
	@Override
	public final ConnectionInformation convert() throws ConversionException {
		ConnectionAdapterHandler<ConnectionAdapter> handler = ConnectionAdapterHandler.getHandler(getTypeId());
		if (handler == null) {
			throw new ConversionException("No handler for " + getTypeId());
		}
		return handler.convert(this);
	}
}
