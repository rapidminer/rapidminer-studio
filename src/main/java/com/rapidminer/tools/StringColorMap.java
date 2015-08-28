/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.awt.Color;


/**
 * @author Tobias Malbrecht
 */
public class StringColorMap extends ParentResolvingMap<String, Color> {

	@Override
	public String getParent(String child) {
		if (child == null) {
			return null;
		}
		int dot = child.lastIndexOf('.');
		if (dot != -1) {
			return child.substring(0, dot);
		} else {
			return null;
		}
	}

	@Override
	public String parseKey(String key, ClassLoader classLoader) {
		return key;
	}

	@Override
	public Color parseValue(String value, ClassLoader classLoader) {
		return Color.decode(value.trim());
	}

	@Override
	public Color getDefault() {
		return Color.WHITE;
	}

}
