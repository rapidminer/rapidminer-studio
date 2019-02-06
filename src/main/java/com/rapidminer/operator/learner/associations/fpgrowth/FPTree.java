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

import com.rapidminer.operator.learner.associations.Item;
import com.rapidminer.tools.Tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * This is the basic data structure for {@link FPGrowth}.
 * 
 * @author Sebastian Land
 */
public class FPTree extends FPTreeNode {

	private Map<Item, Header> headerTable;

	public FPTree() {
		super();
		headerTable = new HashMap<Item, Header>();
		children = new HashMap<Item, FPTreeNode>();
	}

	/**
	 * This method adds a set of Items to the tree. This set of items has to be sorted after the
	 * frequency of the contained items. This method should be used to add Items of a transaction or
	 * a treepath to the tree. The frequency of the set is represented of weight, which should be 1
	 * if items are gathered from transaction
	 * 
	 * @param itemSet
	 *            the sorted set of items
	 * @param weight
	 *            the frequency of the set of items
	 */
	public void addItemSet(Collection<Item> itemSet, int weight) {
		super.addItemSet(itemSet, headerTable, weight);
	}

	public Map<Item, Header> getHeaderTable() {
		return headerTable;
	}

	@Override
	public String toString(String abs, int recursionDepth) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(abs);
		buffer.append("+ ROOT");
		buffer.append(Tools.getLineSeparator());
		for (FPTreeNode node : children.values()) {
			buffer.append(node.toString(abs + "  ", recursionDepth));
		}
		return buffer.toString();
	}

	public String printHeaderTable(int recursionDepth) {
		StringBuffer buffer = new StringBuffer();
		for (Item item : headerTable.keySet()) {
			buffer.append(item.toString());
			buffer.append(" : ");
			buffer.append(headerTable.get(item).getFrequencies().getFrequency(recursionDepth));
			buffer.append(Tools.getLineSeparator());
		}
		return buffer.toString();
	}
}
