/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.operator.UserError;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Uses long class names and maps them to the final (short) part. These can be used for example for
 * GUI purposes.
 * 
 * @author Michael Wurst, Ingo Mierswa
 */
public class ClassNameMapper {

	Map<String, String> classMap = new LinkedHashMap<String, String>();

	public ClassNameMapper(String[] classNames) {
		for (int i = 0; i < classNames.length; i++) {
			String completeClassName = classNames[i];
			String simpleClassName = completeClassName;

			// if possible, strip package information
			int index = completeClassName.lastIndexOf('.');
			if (index > -1) {
				simpleClassName = completeClassName.substring(index + 1);
			}

			// if no class with the same short name is found in the map, add it
			// else use the complete class name
			if (classMap.get(simpleClassName) == null) {
				classMap.put(simpleClassName, completeClassName);
			} else {
				classMap.put(completeClassName, completeClassName);
			}
		}
	}

	public String getCompleteClassName(String shortName) {
		return classMap.get(shortName);
	}

	public Class getClassByShortName(String shortName) throws UserError {
		String completeClassName = getCompleteClassName(shortName);

		// if the name is not found in the map, try to use the one provided as parameter.
		if (completeClassName == null) {
			completeClassName = shortName;
		}

		if (completeClassName == null) {
			throw new UserError(null, 904, shortName, "No such class.");
		}

		try {
			return Class.forName(completeClassName);
		} catch (ClassNotFoundException e) {
			throw new UserError(null, 904, shortName, e.getMessage());
		}
	}

	public String[] getShortClassNames() {
		String[] result = new String[classMap.size()];
		Iterator<String> it = classMap.keySet().iterator();

		for (int i = 0; i < classMap.size(); i++) {
			result[i] = it.next();
		}

		return result;
	}

	public Object getInstantiation(String shortName) throws UserError {
		Object result = null;
		try {
			result = getClassByShortName(shortName).newInstance();
			if (result == null) {
				throw new UserError(null, 904, shortName, "No such class");
			}
		} catch (InstantiationException e) {
			throw new UserError(null, 904, shortName, e.getMessage());
		} catch (IllegalAccessException e) {
			throw new UserError(null, 904, shortName, e.getMessage());
		}
		return result;
	}
}
