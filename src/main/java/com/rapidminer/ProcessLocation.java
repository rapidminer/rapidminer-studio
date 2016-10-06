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
package com.rapidminer;

import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.XMLException;

import java.io.IOException;


/**
 * A place where a process can be saved. Basically, this is either a file or a location in a
 * repository.
 * 
 * @author Simon Fischer
 * */
public interface ProcessLocation {

	/** Reads the process and returns it. */
	public Process load(ProgressListener listener) throws IOException, XMLException;

	/** Stores the process at the referenced location. */
	public void store(Process process, ProgressListener listener) throws IOException;

	/** The toString representation is used, e.g. in the welcome screen dialog, */
	@Override
	public String toString();

	/**
	 * Reads the contents of the referenced resource and returns the XML without parsing it. Used if
	 * process file is broken.
	 */
	public String getRawXML() throws IOException;

	/** Returns a string saved to the history file. */
	public String toHistoryFileString();

	/** Returns a string as it is displayed in the recent files menu. */
	public String toMenuString();

	/** Returns a short name, e.g. the last component of the path. */
	public String getShortName();
}
