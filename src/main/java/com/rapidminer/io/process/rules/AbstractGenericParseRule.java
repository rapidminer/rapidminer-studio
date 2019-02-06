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
package com.rapidminer.io.process.rules;

import com.rapidminer.tools.OperatorService;

import java.util.List;


/**
 * This is an abstract class for all ParseRules that will apply to more than one operator key.
 * 
 * @author Sebastian Land
 */
public abstract class AbstractGenericParseRule implements ParseRule {

	/**
	 * This method returns the list of applicable operator keys. Subclasses might implement this by
	 * checking each operator registered on the {@link OperatorService}.
	 */
	public abstract List<String> getApplicableOperatorKeys();

}
