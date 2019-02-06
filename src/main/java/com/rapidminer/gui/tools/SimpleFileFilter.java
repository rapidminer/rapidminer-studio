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

import java.io.File;
import java.util.Arrays;

import javax.swing.filechooser.FileFilter;


/**
 * A file filter for a given set of extensions. This filter matches all files which has one of the
 * given extensions.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class SimpleFileFilter extends FileFilter {

	private String[] extensions;

	private String description;

	private int id;

	public SimpleFileFilter(String description, String extension) {
		this(description, extension == null ? null : new String[] { extension }, -1);
	}

	public SimpleFileFilter(String description, String extension, int id) {
		this(description, extension == null ? null : new String[] { extension }, id);
	}

	/**
	 * Creates a FileFilter that filters based on a list of extensions.
	 * 
	 * @param id
	 *            Can be used to identify the filter
	 */
	public SimpleFileFilter(String description, String[] extensions, int id) {
		this.description = description;
		this.extensions = extensions;
		this.id = id;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}
		if (extensions == null) {
			return true;
		}
		for (int i = 0; i < extensions.length; i++) {
			if (f.getName().toLowerCase().endsWith(extensions[i].toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	public int getId() {
		return id;
	}

	public String getExtension() {
		if ((extensions != null) && (extensions.length == 1)) {
			return extensions[0];
		} else {
			return null;
		}
	}

	@Override
	public String toString() {
		return "File filter for " + Arrays.asList(extensions) + " (" + getDescription() + ")";
	}
}
