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
package com.rapidminer.operator.libraries;

import com.rapidminer.tools.documentation.GroupDocumentation;
import com.rapidminer.tools.documentation.OperatorDocBundle;

import java.util.Enumeration;
import java.util.Vector;


/**
 * This class wraps a given {@link OperatorLibrary} into a {@link OperatorDocBundle} to provide the
 * necessary information to have a nice GUI integration.
 * 
 * @author Sebastian Land
 */
public class OperatorLibraryDocBundle extends OperatorDocBundle {

	private OperatorLibrary library;
	private GroupDocumentation libraryGroupDoc;
	private String namespace;

	public OperatorLibraryDocBundle(OperatorLibrary library) {
		this.library = library;
		this.namespace = library.getNamespace() + ":" + library.getName();
		this.libraryGroupDoc = new GroupDocumentation(namespace, library.getName(), library.getSynopsis());
	}

	@Override
	public Enumeration<String> getKeys() {
		Vector<String> keys = new Vector<String>();

		String namespace = library.getNamespace();
		for (String operatorKey : library.getOperatorKeys()) {
			keys.add(OperatorDocBundle.OPERATOR_PREFIX + namespace + operatorKey);
		}
		keys.add(OperatorDocBundle.GROUP_PREFIX + namespace);

		return keys.elements();
	}

	@Override
	protected Object handleGetObject(String key) {
		if (key.startsWith(OPERATOR_PREFIX)) {
			String suffix = key.substring(OperatorDocBundle.OPERATOR_PREFIX.length());
			return library.getOperatorDocumentation(suffix);
		} else if (key.startsWith(GROUP_PREFIX)) {
			String suffix = key.substring(OperatorDocBundle.GROUP_PREFIX.length());
			if (suffix.equals(namespace)) {
				return libraryGroupDoc;
			}
		}
		return null;
	}
}
