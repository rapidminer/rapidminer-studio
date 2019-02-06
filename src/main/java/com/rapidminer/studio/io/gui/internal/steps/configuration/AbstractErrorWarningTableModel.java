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
package com.rapidminer.studio.io.gui.internal.steps.configuration;

import javax.swing.table.AbstractTableModel;


/**
 * Table model that knows how many warnings and errors it contains. Used in the
 * {@link CollapsibleErrorTable} to display the number of errors and warnings.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
public abstract class AbstractErrorWarningTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	/**
	 * @return the number of errors this table model contains
	 */
	public abstract int getErrorCount();

	/**
	 * @return the number of warnings this table model contains
	 */
	public abstract int getWarningCount();
}
