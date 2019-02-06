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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;


/**
 * This class represents properties accessed by a String value.
 * 
 * @author Michael Wurst, Ingo Mierswa
 * 
 */
public class StringProperties implements Serializable {

	private static final long serialVersionUID = 1926744586167372203L;

	private final Map<Object, Object> properties;

	public StringProperties() {
		super();
		properties = new HashMap<Object, Object>();
	}

	public StringProperties(StringProperties props) {
		this();
		this.properties.putAll(props.properties);
	}

	public void set(String key, Object val) {
		properties.put(key, val);
	}

	public Object get(String key) {
		return properties.get(key);
	}

	public Iterator<Object> getKeys() {
		return properties.keySet().iterator();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (Entry<Object, Object> entry : properties.entrySet()) {
			result.append(entry.getKey());
			result.append(":");
			result.append(entry.getValue());
			result.append(Tools.getLineSeparator());
		}
		return result.toString();
	}
}
