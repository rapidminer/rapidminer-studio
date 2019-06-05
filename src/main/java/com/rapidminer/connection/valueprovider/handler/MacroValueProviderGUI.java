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
package com.rapidminer.connection.valueprovider.handler;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.DefaultValueProviderGUI;
import com.rapidminer.connection.valueprovider.ValueProvider;


/**
 * GUI for the macro value provider
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class MacroValueProviderGUI extends DefaultValueProviderGUI {

	@Override
	public String getCustomLabel(CustomLabel key, ValueProvider provider, ConnectionInformation connection, String group, String parameterKey, Object... args) {
		return super.getCustomLabel(key, provider, connection, group, parameterKey, MacroValueProviderHandler.getPrefix(provider) + parameterKey);
	}
}
