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

import java.util.ArrayList;
import java.util.Iterator;


/**
 * This class is an adapter to obtain an ArrayList given an Iterator.
 * 
 * @author Michael Wurst, Ingo Mierswa
 */
public class IterationArrayList<E> extends ArrayList<E> {

	private static final long serialVersionUID = 8054453172552877145L;

	private static final int MAX_ENTRIES = 10;

	public IterationArrayList(Iterator<E> it) {
		while (it.hasNext()) {
			add(it.next());
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (int i = 0; (i < size()) && (i < MAX_ENTRIES); i++) {
			result.append(get(i).toString());
			result.append(" ");
		}
		if (size() > MAX_ENTRIES) {
			result.append("... additional ");
			result.append(size() - MAX_ENTRIES);
			result.append(" items");
		}
		return result.toString();
	}
}
