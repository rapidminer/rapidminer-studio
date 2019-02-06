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
package com.rapidminer.tools;

import java.awt.Color;

import com.rapidminer.tools.plugin.Plugin;


/**
 * @author Tobias Malbrecht
 */
public class StringColorMap extends ParentResolvingMap<String, Color> {

	public static final Color DEFAULT_COLOR = Color.WHITE;

	@Override
	public String getParent(String child, Plugin provider) {
		if (child == null) {
			return null;
		}
		int dot = child.lastIndexOf('.');
		if (dot != -1) {
			return child.substring(0, dot);
		}
		if (provider != null && provider.useExtensionTreeRoot() && !child.equals(provider.getName())) {
			return provider.getName();
		} else {
			return null;
		}
	}

	@Override
	public String parseKey(String key, ClassLoader classLoader, Plugin provider) {
		if (provider != null && provider.useExtensionTreeRoot()) {
			if (key.trim().isEmpty()) {
				// use extension name as top-level group in case group..color was defined in
				// groups.properties file
				return provider.getName();
			} else {
				// otherwise add the extension name to the key to prevent duplicate keys
				return provider.getName() + "." + key;
			}
		}
		return key;
	}

	@Override
	public Color parseValue(String value, ClassLoader classLoader, Plugin provider) {
		return Color.decode(value.trim());
	}

	@Override
	public Color getDefault() {
		return DEFAULT_COLOR;
	}

}
