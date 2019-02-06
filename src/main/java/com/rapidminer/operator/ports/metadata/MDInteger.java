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
package com.rapidminer.operator.ports.metadata;

/**
 * @author Simon Fischer
 */
public class MDInteger extends MDNumber<Integer> {

	private static final long serialVersionUID = 1L;

	/**
	 * This constructor will build a unkown number
	 */
	public MDInteger() {
		super(0);
	}

	public MDInteger(int i) {
		super(i);
	}

	public MDInteger(MDInteger integer) {
		super(integer);
	}

	@Override
	public MDInteger add(Integer add) {
		setNumber(getNumber() + add);
		return this;
	}

	public void add(MDInteger add) {
		setNumber(getNumber() + add.getNumber());
		switch (add.getRelation()) {
			case AT_LEAST:
				increaseByUnknownAmount();
				break;
			case AT_MOST:
				reduceByUnknownAmount();
				break;
			case UNKNOWN:
				setUnkown();
			default:
		}
	}

	@Override
	public MDInteger multiply(double factor) {
		Integer current = getNumber();
		if (current != null) {
			setNumber((int) Math.round(current * factor));
		}
		return this;
	}

	public MDInteger subtract(int subtrahend) {
		Integer current = getNumber();
		if (current != null) {
			setNumber(current - subtrahend);
		}
		return this;
	}

	@Override
	public MDInteger copy() {
		return new MDInteger(this);
	}

	@Override
	public String toString() {
		switch (getRelation()) {
			case EQUAL:
				return "= " + getValue();
			case AT_LEAST:
				return "\u2265 " + getNumber();
			case AT_MOST:
				return "\u2264 " + getNumber();
			case UNKNOWN:
			default:
				return "?";
		}
	}

}
