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
package com.rapidminer.operator;

import java.util.List;


/**
 * This helper class contains information about the XML definition of a list.
 * 
 * @author Ingo Mierswa
 */
public class ListDescription {

	private String key;
	private List<String[]> list;

	public ListDescription(String key, List<String[]> list) {
		this.key = key;
		this.list = list;
	}

	public String getKey() {
		return key;
	}

	public List<String[]> getList() {
		return list;
	}

}
