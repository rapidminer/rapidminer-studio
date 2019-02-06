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
package com.rapidminer.studio.io.data;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.tools.I18N;


/**
 *
 * Exception that is thrown when a given start row in a data set cannot be found.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
public class StartRowNotFoundException extends DataSetException {

	private static final long serialVersionUID = 1L;

	private static final String HEADER_ROW_NOT_FOUND = I18N.getGUILabel("import_wizard.start_row_not_found");

	/**
	 * Creates an exception indicating that the desired start row was not found in the data set.
	 */
	public StartRowNotFoundException() {
		super(HEADER_ROW_NOT_FOUND);
	}

}
