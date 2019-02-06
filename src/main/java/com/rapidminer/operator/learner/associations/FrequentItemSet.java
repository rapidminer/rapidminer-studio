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
package com.rapidminer.operator.learner.associations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.rapidminer.tools.Tools;


/**
 * A frequent item set contains a set of frequent {@link Item}s.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class FrequentItemSet implements Comparable<FrequentItemSet>, Cloneable, Serializable {

	private static final long serialVersionUID = 180867073198503510L;

	private ArrayList<Item> items;

	private int frequency;

	public FrequentItemSet() {
		this.items = new ArrayList<>(1);
	}

	public FrequentItemSet(ArrayList<Item> items, int frequency) {
		this.items = items;
		Collections.sort(this.items);
		this.frequency = frequency;
	}

	/**
	 * Clone constructor.
	 */
	private FrequentItemSet(FrequentItemSet other) {
		this.items = new ArrayList<>(other.items);
		this.frequency = other.frequency;
	}

	public void addItem(Item item, int frequency) {
		items.add(item);
		Collections.sort(this.items);
		this.frequency = frequency;
	}

	public Collection<Item> getItems() {
		return items;
	}

	public Item getItem(int index) {
		return items.get(index);
	}

	public int getNumberOfItems() {
		return items.size();
	}

	public int getFrequency() {
		return frequency;
	}

	/**
	 * This method compares FrequentItemSets. It first compares the length of items sets, then the
	 * items itself. If they are the same, the Sets are equal.
	 */
	@Override
	public int compareTo(FrequentItemSet o) {
		// compare size
		Collection<Item> hisItems = o.getItems();
		if (items.size() < hisItems.size()) {
			return -1;
		} else if (items.size() > hisItems.size()) {
			return 1;
		} else {
			// compare items
			Iterator<Item> iterator = hisItems.iterator();
			for (Item myCurrentItem : this.items) {
				int relation = myCurrentItem.compareTo(iterator.next());
				if (relation != 0) {
					return relation;
				}
			}
			// equal sets
			return 0;
		}
	}

	/**
	 * this method returns true if the frequent Items set are equal in size and items.
	 */
	@Override
	public boolean equals(Object o) {
		return o instanceof FrequentItemSet && (this.compareTo((FrequentItemSet) o) == 0);
	}

	@Override
	public int hashCode() {
		return items.hashCode();
	}

	/**
	 * This method returns a representation of the items
	 */
	public String getItemsAsString() {
		StringBuilder buffer = new StringBuilder();
		Iterator<Item> iterator = items.iterator();
		while (iterator.hasNext()) {
			buffer.append(iterator.next().toString());
			if (iterator.hasNext()) {
				buffer.append(", ");
			}
		}
		return buffer.toString();
	}

	/**
	 * This method should return a proper String representation of this frequent Item Set
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		Iterator<Item> iterator = items.iterator();
		while (iterator.hasNext()) {
			buffer.append(iterator.next().toString());
			if (iterator.hasNext()) {
				buffer.append(", ");
			}
		}
		buffer.append(", frequency: ");
		buffer.append(Tools.formatNumber(frequency));
		return buffer.toString();
	}

	@Override
	public Object clone() {
		return new FrequentItemSet(this);
	}
}
