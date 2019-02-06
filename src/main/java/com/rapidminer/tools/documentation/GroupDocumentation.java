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
package com.rapidminer.tools.documentation;

import org.w3c.dom.Element;

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.GroupTree;


/**
 * Documentation for a {@link GroupTree}.
 *
 * @author Simon Fischer
 */
public class GroupDocumentation {

	private final String key;
	private final String name;
	private final String help;

	public GroupDocumentation(String key) {
		this.key = key;
		this.name = keyToUpperCase(key);
		this.help = "The group '" + name + "'.";
	}

	public GroupDocumentation(String key, String name, String help) {
		this.key = key;
		this.name = name;
		this.help = help;
	}

	GroupDocumentation(Element element) {
		this.key = XMLTools.getTagContents(element, "key");
		this.name = XMLTools.getTagContents(element, "name");
		this.help = XMLTools.getTagContents(element, "help");
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public String getHelp() {
		return help != null ? help : "";
	}

	@Override
	public String toString() {
		return key + ": " + name;
	}

	public static String keyToUpperCase(String key) {
		String name = key;
		if (name.indexOf('.') >= 0) {
			name = name.substring(name.lastIndexOf('.') + 1);
		}
		name = name.replace('_', ' ');
		char[] chars = name.toCharArray();
		boolean makeUppercase = true;
		for (int i = 0; i < chars.length; i++) {
			if (makeUppercase) {
				chars[i] = Character.toUpperCase(chars[i]);
			}
			makeUppercase = Character.isWhitespace(chars[i]);
		}
		return new String(chars);
	}
}
