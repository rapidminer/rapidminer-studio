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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * A node in the FPTree.
 * 
 * @author Sebastian Land
 */
public class FPTreeNode {

	protected FrequencyStack frequencies;

	protected Item nodeItem;

	protected FPTreeNode sibling;

	protected FPTreeNode father;

	protected Map<Item, FPTreeNode> children;

	public FPTreeNode() {
		frequencies = new ListFrequencyStack();
		children = new LinkedHashMap<Item, FPTreeNode>();
	}

	public FPTreeNode(FPTreeNode father, Item nodeItem) {
		frequencies = new ListFrequencyStack();
		this.father = father;
		children = new HashMap<Item, FPTreeNode>();
		this.nodeItem = nodeItem;
	}

	/**
	 * This method only works at recursiondepth 0, therefore may only be used for tree constructing.
	 * This method adds a set of Items to the tree of this node. This set of items has to be sorted
	 * after the frequency of the contained items. This method is recursivly used to expand the tree
	 * for the given set, by adding a node for the first item and then call this method with the
	 * remaining set on the new node. The frequency of the set is represented of weight.
	 * siblingChain is the headerTable, giving this method a startingpoint for finding the other
	 * nodes of the item to append new nodes
	 * 
	 * @param itemSet
	 *            the sorted set of items
	 * @param headerTable
	 *            gives the headertable for finding other nodes of an item
	 */
	public void addItemSet(Collection<Item> itemSet, Map<Item, Header> headerTable, int weight) {
		Iterator<Item> iterator = itemSet.iterator();
		if (iterator.hasNext()) {
			Item firstItem = iterator.next();
			FPTreeNode childNode;
			if (!children.containsKey(firstItem)) {
				// if this node has no child for this item, create it
				childNode = createChildNode(firstItem);
				// and add it to childs of this node
				children.put(firstItem, childNode);
				// update header table:
				if (!headerTable.containsKey(firstItem)) {
					// if item unknown in headerTable, create new entry
					headerTable.put(firstItem, new Header());
				}
				// append new node to sibling chain of this item
				headerTable.get(firstItem).addSibling(childNode);
			} else {
				// select children for this item if allready existing
				childNode = children.get(firstItem);
			}
			// updating frequency in headerTable
			headerTable.get(firstItem).frequencies.increaseFrequency(0, weight);
			// updating frequency in this node
			childNode.increaseFrequency(0, weight);
			// remove added item and make recursiv call on child note
			itemSet.remove(firstItem);
			childNode.addItemSet(itemSet, headerTable, weight);
		}
	}

	/**
	 * Returns the father of this node or null if node is root
	 */
	public FPTreeNode getFather() {
		return father;
	}

	/**
	 * Returns true if node has father. If node is root, false is returned
	 */
	public boolean hasFather() {
		return (this.father != null);
	}

	/**
	 * Returns the next node representing the same item as this node.
	 */
	public FPTreeNode getSibling() {
		return sibling;
	}

	/**
	 * Returns the last node of the chain of nodes representing the same item as this node
	 */
	public FPTreeNode getLastSibling() {
		FPTreeNode currentNode = this;
		while (currentNode.hasSibling()) {
			currentNode = currentNode.getSibling();
		}
		return currentNode;
	}

	/**
	 * This method sets the next node in the chain of node representing the same item as this node
	 * 
	 * @param sibling
	 *            is the next node in the chain
	 */
	public void setSibling(FPTreeNode sibling) {
		this.sibling = sibling;
	}

	/**
	 * Returns true if this node is not the last one in the chain of nodes representing the same
	 * item as this node. Otherwise false is returned.
	 */
	public boolean hasSibling() {
		return (this.sibling != null);
	}

	/**
	 * This method increases the frequency of this current node by the given weight in given
	 * recusionDepth
	 * 
	 * @param value
	 *            the frequency is increased by this value
	 */
	public void increaseFrequency(int recursionDepth, int value) {
		frequencies.increaseFrequency(recursionDepth, value);
	}

	/**
	 * This method clears the frequency stack on top
	 */
	public void popFrequency(int height) {
		frequencies.popFrequency(height);
	}

	/**
	 * this returns the frequency of the node in current recursion
	 */
	public int getFrequency(int height) {
		return frequencies.getFrequency(height);
	}

	/**
	 * this returns the item, this node represents
	 */
	public Item getNodeItem() {
		return this.nodeItem;
	}

	/**
	 * This returns the map, which maps the child nodes on items. It may be used to get a set of all
	 * childNodes or all represented items.
	 */
	public Map<Item, FPTreeNode> getChildren() {
		return this.children;
	}

	/**
	 * This method returns the first child. If no child exists, null is returned
	 */
	public FPTreeNode getChild() {
		if (children.size() != 1) {
			return null;
		} else {
			return children.get(children.keySet().iterator().next());
		}
	}

	/**
	 * this method creates a new childnode of this node, representing the node item
	 * 
	 * @param nodeItem
	 *            the item, represented by the new node
	 */
	public FPTreeNode createChildNode(Item nodeItem) {
		return new FPTreeNode(this, nodeItem);
	}

	public String toString(int recursionDepth) {
		return toString("", recursionDepth);
	}

	public String toString(String abs, int recursionDepth) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(abs);
		buffer.append("+ ");
		buffer.append(nodeItem.toString());
		buffer.append(" (");
		buffer.append(frequencies.getFrequency(recursionDepth));
		buffer.append(")");
		buffer.append(Tools.getLineSeparator());
		for (FPTreeNode node : children.values()) {
			buffer.append(node.toString(abs + "  ", recursionDepth));
		}
		return buffer.toString();
	}
}
