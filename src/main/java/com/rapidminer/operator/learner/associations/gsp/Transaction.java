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
package com.rapidminer.operator.learner.associations.gsp;

import java.util.ArrayList;
import java.util.Collections;


/**
 * @author Sebastian Land
 * 
 */
public class Transaction extends ArrayList<Item> {

	private static final long serialVersionUID = 8725134393133916536L;

	private double time;

	public Transaction(double time, Item... items) {
		this.time = time;
		for (Item item : items) {
			super.add(item);
		}
		Collections.sort(this);
	}

	public Transaction(Transaction transaction) {
		this.addAll(transaction);
	}

	public Item getLastItem() {
		return get(size() - 1);
	}

	public double getTime() {
		return time;
	}

	@Override
	public boolean add(Item item) {
		if (!contains(item)) {
			super.add(item);
			Collections.sort(this);
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		String separator = "";
		for (Item item : this) {
			buffer.append(separator + item.toString());
			separator = ", ";
		}
		return buffer.toString();
	}
}
