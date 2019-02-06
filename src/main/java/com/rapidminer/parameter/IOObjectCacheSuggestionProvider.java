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
package com.rapidminer.parameter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.ProgressListener;


/**
 * Suggests names from {@link com.rapidminer.Process#getIOObjectCache()}.
 *
 * @author Simon Fischer
 *
 */
public class IOObjectCacheSuggestionProvider implements SuggestionProvider<String> {

	@Override
	public List<String> getSuggestions(Operator op, ProgressListener pl) {

		if (op == null || op.getProcess() == null) {
			return Collections.emptyList();
		}
		// return all names of IOObjects that are already stored in the cache
		Set<String> resultSet = op.getProcess().getIOObjectCache().getAllKeys();
		List<String> result = new LinkedList<String>(resultSet);
		Collections.sort(result);
		return result;
	}

	@Override
	public ResourceAction getAction() {
		// no action should be displayed
		return null;
	}
}
