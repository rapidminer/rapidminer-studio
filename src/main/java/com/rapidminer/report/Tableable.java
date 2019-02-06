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
 * This interface provides methods for exporting tables.
 * 
 * @author Sebastian Land
 */
public interface Tableable extends Reportable {

	public boolean isFirstLineHeader();

	public boolean isFirstColumnHeader();

	public void prepareReporting();

	public void finishReporting();

	public String getColumnName(int index);

	/**
	 * @return The number of rows in this {@link Tableable}
	 */
	public int getRowNumber();

	/**
	 * @return The number of columns in this {@link Tableable}
	 */
	public int getColumnNumber();

	public String getCell(int row, int column);

}
