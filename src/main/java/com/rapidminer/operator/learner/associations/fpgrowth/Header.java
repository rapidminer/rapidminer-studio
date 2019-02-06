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
package com.rapidminer.operator.learner.associations.fpgrowth;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * An entry in the header table.
 * 
 * @author Sebastian Land
 */
public class Header {

	FrequencyStack frequencies;

	List<FPTreeNode> siblingChain;

	public Header() {
		frequencies = new ListFrequencyStack();
		siblingChain = new LinkedList<FPTreeNode>();
	}

	public void addSibling(FPTreeNode node) {
		siblingChain.add(node);
	}

	public Collection<FPTreeNode> getSiblingChain() {
		return siblingChain;
	}

	public FrequencyStack getFrequencies() {
		return frequencies;
	}
}
