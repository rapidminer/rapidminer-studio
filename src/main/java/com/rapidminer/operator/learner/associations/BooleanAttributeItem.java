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

import com.rapidminer.example.Attribute;


/**
 * This is an {@link Item} based on boolean attributes.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class BooleanAttributeItem implements Item {

	private static final long serialVersionUID = -7963677912091349984L;

	private int frequency = 0;

	private String name;

	public BooleanAttributeItem(Attribute item) {
		this.name = item.getName();
	}

	@Override
	public int getFrequency() {
		return this.frequency;
	}

	@Override
	public void increaseFrequency() {
		this.frequency++;
	}

	public void increaseFrequency(double value) {
		this.frequency += value;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof BooleanAttributeItem)) {
			return false;
		}
		BooleanAttributeItem o = (BooleanAttributeItem) other;
		return (this.name.equals(o.name)) && (this.frequency == o.frequency);
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() ^ Double.valueOf(this.frequency).hashCode();
	}

	@Override
	public int compareTo(Item arg0) {
		Item comparer = arg0;
		// Collections.sort generates ascending order. Descending needed,
		// therefore invert return values!
		if (comparer.getFrequency() == this.getFrequency()) {
			return (-1 * this.name.compareTo(arg0.toString()));
		} else if (comparer.getFrequency() < this.getFrequency()) {
			return -1;
		} else {
			return 1;
		}
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public void increaseFrequency(int value) {
		frequency += value;
	}
}
