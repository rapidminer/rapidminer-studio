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
package com.rapidminer.tools.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.tools.LogService;


/**
 * A plugin dependency defines the name and version of a desired plugin.
 *
 * @author Ingo Mierswa
 */
public class Dependency {

	/** The name of the desired plugin. */
	private String extensionId;

	/** The version of the desired plugin. */
	private String version;

	/** Create a new plugin dependency. */
	public Dependency(String name, String version) {
		this.extensionId = name;
		this.version = version;
	}

	/**
	 * Returns true if the set contains a extension with the desired name and version.
	 */
	public boolean isFulfilled(Collection<Plugin> plugins) {
		Iterator<Plugin> i = plugins.iterator();
		while (i.hasNext()) {
			Plugin plugin = i.next();
			if (plugin.getExtensionId().equals(this.extensionId)) {
				try {
					return new VersionNumber(plugin.getVersion()).isAtLeast(new VersionNumber(this.version));
				} catch (VersionNumber.VersionNumberException vne) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.tools.plugin.Dependency.malformed_version",
							new Object[]{plugin.toString(), this.toString(), vne.getLocalizedMessage()});
					return false;
				}
			}
		}
		return false;
	}

	public String getPluginExtensionId() {
		return extensionId;
	}

	public String getPluginVersion() {
		return version;
	}

	@Override
	public String toString() {
		return extensionId + " (" + version + ")";
	}

	public static List<Dependency> parse(String dependencyString) {
		if (dependencyString == null || dependencyString.isEmpty()) {
			return Collections.emptyList();
		}
		List<Dependency> result = new LinkedList<Dependency>();
		String[] singleDependencies = dependencyString.trim().split(";");
		for (int i = 0; i < singleDependencies.length; i++) {
			if (singleDependencies[i].trim().length() > 0) {
				String dependencyName = singleDependencies[i].trim();
				String dependencyVersion = "0";
				if (singleDependencies[i].trim().indexOf("[") >= 0) {
					dependencyName = singleDependencies[i].trim().substring(0, singleDependencies[i].trim().indexOf("["))
							.trim();
					dependencyVersion = singleDependencies[i].trim().substring(singleDependencies[i].trim().indexOf("[") + 1,
							singleDependencies[i].trim().indexOf("]")).trim();
				}
				result.add(new Dependency(dependencyName, dependencyVersion));
			}
		}
		return result;
	}
}
