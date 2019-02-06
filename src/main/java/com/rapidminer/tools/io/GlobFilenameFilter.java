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
package com.rapidminer.tools.io;

import com.rapidminer.tools.GlobCompiler;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;


/**
 * This filename filter interprets a given glob expression, to determine if a file should be
 * accepted. Glob expressions are widely used in shells. See {@link GlobCompiler} for more details
 * about this language.
 * 
 * @author Sebastian Land
 */
public class GlobFilenameFilter implements FilenameFilter {

	private Pattern filenamePattern;
	private boolean includeDirectories = false;

	public GlobFilenameFilter(String glob) {
		filenamePattern = GlobCompiler.compileGlob(glob);
	}

	public GlobFilenameFilter(String glob, boolean includeDirectories) {
		filenamePattern = GlobCompiler.compileGlob(glob);
		this.includeDirectories = includeDirectories;
	}

	@Override
	public boolean accept(File dir, String name) {
		return filenamePattern.matcher(name.toLowerCase()).matches() && (includeDirectories || new File(dir, name).isFile());
	}

}
