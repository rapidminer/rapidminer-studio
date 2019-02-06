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

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.Operator;


/**
 * Rule that defines how operators are imported from earlier RapidMiner versions.
 * 
 * @author Simon Fischer
 * 
 */
public interface ParseRule {

	/**
	 * Applies the rule and possibly returns a message describing what has been modified. This takes
	 * only place if the rule applies as well to the operator as to the version of the process we
	 * are importing or the given processVersion is null. Null is returned if the rule did not
	 * apply.
	 * 
	 * @param importer
	 */
	public String apply(Operator operator, VersionNumber processVersion, XMLImporter importer);
}
