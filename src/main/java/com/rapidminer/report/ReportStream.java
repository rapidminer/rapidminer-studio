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
package com.rapidminer.report;

/**
 * This is the basic interface for all streams for reportable items. Subclasses might implement
 * streams for different file formats.
 * 
 * @author Sebastian Land, Ingo Mierswa, Helge Homburg
 */
public interface ReportStream {

	/**
	 * Returns the string name of this stream.
	 */
	public String getName();

	/**
	 * This method will cause the stream to add a page break to the stream.
	 */
	public void addPageBreak();

	/**
	 * This method will cause the stream to start a new Section at the specified level.
	 * 
	 * @param sectionName
	 * @param sectionLevel
	 * @throws ReportException
	 */
	public void startSection(String sectionName, int sectionLevel) throws ReportException;

	/**
	 * This method will cause the stream to append a readable to the stream.
	 */
	public void append(String name, Reportable reportable, int width, int height) throws ReportException;

	/**
	 * This method is called to free all resourcess and finish writing if needed.
	 * 
	 * @throws ReportException
	 */
	public void close() throws ReportException;

}
