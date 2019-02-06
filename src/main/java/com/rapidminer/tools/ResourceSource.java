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

import java.net.URL;


/**
 * ResourceSources can be added to the {@link Tools} class in order to allow resource loading for
 * both the RapidMiner core and the plugin. Each plugin might add a new new resource source
 * indicating where the sources of the plugin can be found.
 * 
 * Please note that a resource path is only allowed to contain '/' instead of using File.separator.
 * This must be considered if a new prefix should be defined.
 * 
 * @author Ingo Mierswa
 */
public class ResourceSource {

	private ClassLoader loader;

	private String prefix;

	public ResourceSource(ClassLoader loader) {
		this(loader, Tools.RESOURCE_PREFIX);
	}

	public ResourceSource(ClassLoader loader, String prefix) {
		this.loader = loader;
		this.prefix = prefix;
		if (!prefix.endsWith("/")) {
			this.prefix = prefix + "/";
		}
	}

	public URL getResource(String name) {
		return loader.getResource((prefix + name).trim());
	}

	@Override
	public String toString() {
		return loader + "(" + prefix + ")";
	}
}
