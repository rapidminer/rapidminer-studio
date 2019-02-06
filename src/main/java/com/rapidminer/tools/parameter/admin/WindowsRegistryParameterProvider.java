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
package com.rapidminer.tools.parameter.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.SystemInfoUtilities;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;


/**
 * Windows Registry based {@link ParameterProvider Parameter Provider}
 * <p>
 * Loads the config from {@link #CONFIG_KEY HKEY_CURRENT_USER\Software\RapidMiner\RapidMiner Studio\Config}.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0.0
 */
public class WindowsRegistryParameterProvider implements ParameterProvider {

	/**
	 * Indicates that the OS might be Windows
	 */
	private static final boolean IS_WINDOWS = SystemInfoUtilities.getOperatingSystem() == SystemInfoUtilities.OperatingSystem.WINDOWS;

	/**
	 * Path to the config in the HKEY_CURRENT_USER Registry Hive
	 */
	public static final String CONFIG_KEY = "Software\\RapidMiner\\RapidMiner Studio\\Config";

	@Override
	public Map<String, String> readProperties() {
		if (!IS_WINDOWS) {
			return null;
		}
		try {
			// Check if key exists
			if (!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, CONFIG_KEY)) {
				return null;
			}
			// Read values from registry
			TreeMap<String, Object> map = Advapi32Util.registryGetValues(WinReg.HKEY_CURRENT_USER, CONFIG_KEY);
			Map<String, String> values = new HashMap<>();
			map.forEach((String k, Object oV) -> {
						String v = Objects.toString(oV, null);
						values.put(k.trim(), v != null ? v.trim() : null);
					}
			);
			LogService.getRoot().fine(() -> String.format("Successfully enforced %d settings from the Windows registry.", values.size()));
			return values;
		} catch (Throwable e) {
			LogService.getRoot().log(Level.WARNING, "Failed to access the Windows registry.", e);
			return null;
		}
	}
}
