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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.rapidminer.operator.UserError;


/**
 * Uses long class names and maps them to the final (short) part. These can be used for example for
 * GUI purposes.
 * 
 * @author Michael Wurst, Ingo Mierswa
 */
public class ClassNameMapper {

	Map<String, String> classMap = new LinkedHashMap<>();

	public ClassNameMapper(String[] classNames) {
		for (String completeClassName : classNames) {
			// if possible, strip package information
			String simpleClassName = completeClassName.substring(completeClassName.lastIndexOf('.') + 1);

			// if no class with the same short name is found in the map, add it
			// else use the complete class name
			if (classMap.putIfAbsent(simpleClassName, completeClassName) != null) {
				classMap.put(completeClassName, completeClassName);
			}
		}
	}

	public String getCompleteClassName(String shortName) {
		return classMap.get(shortName);
	}

	public Class<?> getClassByShortName(String shortName) throws UserError {
		// if the name is not found in the map, try to use the one provided as parameter.
		String completeClassName = classMap.getOrDefault(shortName, shortName);
		try {
			Objects.requireNonNull(completeClassName, "No such class");
			return Class.forName(completeClassName);
		} catch (ClassNotFoundException | NullPointerException e) {
			throw new UserError(null, 904, shortName, e.getMessage());
		}
	}

	public String[] getShortClassNames() {
		return classMap.keySet().toArray(new String[0]);
	}

	public Object getInstantiation(String shortName) throws UserError {
		try {
			return getClassByShortName(shortName).newInstance();
		} catch (InstantiationException | IllegalAccessException | NullPointerException e) {
			throw new UserError(null, 904, shortName, e.getMessage());
		}
	}
}
