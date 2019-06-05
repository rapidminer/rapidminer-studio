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
package com.rapidminer.gui.tools;

/**
 * This enumeration hold the information about all available icon sizes (mainly used for actions).
 * Currently, only icon sizes 16, 24, and 48 are supported. 16 should be used for small entries (e.g. lists) and 24 in
 * menus, while 48 can be used for large panels.
 * 
 * @author Ingo Mierswa
 */
public enum IconSize {

	/** 16 pixel icons */
	SMALL(16),

	/** 24 pixel icons */
	LARGE(24),

	/** 48 pixel icons */
	HUGE(48);

	private int size;

	IconSize(int size) {
		this.size = size;
	}

	public int getSize() {
		return size;
	}
}
