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
package com.rapidminer.operator.io;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;


/**
 * Dummy operator doing nothing.
 * 
 * TODO: Implement two quick fixes:- - Take old IOContainerReader, read, split, and replace by
 * several IOObjectReaders - Check whether corresponding IOContainerWriter was already imported and
 * executed once, so we have several generated files, and create one IOObjectReader for each
 * 
 * @author Simon Fischer
 * 
 */
public class IOContainerReader extends Operator {

	public IOContainerReader(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() {
		getLogger().warning(
				"This operator is deprecated and does nothing. It should be replaced by several IOObjectReaders.");
	}

}
