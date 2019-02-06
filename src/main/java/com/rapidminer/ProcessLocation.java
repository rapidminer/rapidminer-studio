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
package com.rapidminer;

import java.io.IOException;

import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.XMLException;


/**
 * A place where a process can be saved. Basically, this is either a file or a location in a
 * repository.
 * 
 * @author Simon Fischer
 * */
public interface ProcessLocation {

	/** Reads the process and returns it. */
	Process load(ProgressListener listener) throws IOException, XMLException;

	/** Stores the process at the referenced location. */
	void store(Process process, ProgressListener listener) throws IOException;

	/** The toString representation is used, e.g. in the welcome screen dialog, */
	@Override
	String toString();

	/**
	 * Reads the contents of the referenced resource and returns the XML without parsing it. Used if
	 * process file is broken.
	 */
	String getRawXML() throws IOException;

	/** Returns a string saved to the history file. */
	String toHistoryFileString();

	/** Returns a string as it is displayed in the recent files menu. */
	String toMenuString();

	/** Returns a short name, e.g. the last component of the path. */
	String getShortName();

	/**
	 * The icon name without size modifier that visualizes where this process is stored, e.g. RM Server repo, local repo, Cloud, ...
	 *
	 * @return the name of the icon to visualize the origin of this recent process
	 * @since 8.2
	 */
	String getIconName();
}
