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

import com.rapidminer.tools.Tools;

import java.io.Serializable;


/**
 * A number which is not known exactly, but maybe only in terms of upper or lower bounds. E.g. after
 * applying an attribute filter, the number of examples is at most the number it was before, but the
 * exact value is unknown.
 * 
 * @author Simon Fischer
 * 
 */
public abstract class MDNumber<T extends Number> implements Serializable, Comparable<MDNumber<T>> {

	private static final long serialVersionUID = 1L;

	public enum Relation {
		AT_LEAST, EQUAL, AT_MOST, UNKNOWN
	}

	private Relation relation = Relation.UNKNOWN;
	private T number;

	public MDNumber() {
		relation = Relation.UNKNOWN;
		setNumber(null);
	}

	protected MDNumber(MDNumber<T> other) {
		this.setNumber(other.getNumber());
		this.relation = other.relation;
	}

	public MDNumber(T number) {
		this.setNumber(number);
		this.relation = Relation.EQUAL;
	}

	public MetaDataInfo equals(T value) {
		if ((relation == Relation.EQUAL) && this.getNumber().equals(value)) {
			return MetaDataInfo.YES;
		} else {
			return MetaDataInfo.UNKNOWN;
		}
	}

	public MetaDataInfo isAtMost(T value) {
		switch (relation) {
			case AT_LEAST:
				return MetaDataInfo.UNKNOWN;
			case EQUAL:
				if (this.getNumber().doubleValue() <= value.doubleValue()) {
					return MetaDataInfo.YES;
				} else {
					return MetaDataInfo.NO;
				}
			case AT_MOST:
				if (this.getNumber().doubleValue() <= value.doubleValue()) {
					return MetaDataInfo.YES;
				} else {
					return MetaDataInfo.UNKNOWN;
				}
			case UNKNOWN:
			default:
				return MetaDataInfo.UNKNOWN;
		}
	}

	public MetaDataInfo isAtLeast(T value) {
		switch (relation) {
			case AT_MOST:
				return MetaDataInfo.UNKNOWN;
			case EQUAL:
				if (this.getNumber().doubleValue() >= value.doubleValue()) {
					return MetaDataInfo.YES;
				} else {
					return MetaDataInfo.NO;
				}
			case AT_LEAST:
				if (this.getNumber().doubleValue() >= value.doubleValue()) {
					return MetaDataInfo.YES;
				} else {
					return MetaDataInfo.UNKNOWN;
				}
			case UNKNOWN:
			default:
				return MetaDataInfo.UNKNOWN;
		}
	}

	public void increaseByUnknownAmount() {
		switch (relation) {
			case AT_MOST:
				relation = Relation.UNKNOWN;
				break;
			case EQUAL:
				relation = Relation.AT_LEAST;
				break;
			case AT_LEAST:
			case UNKNOWN:
				// stays like it is
				break;
		}
	}

	public void reduceByUnknownAmount() {
		switch (relation) {
			case AT_LEAST:
				relation = Relation.UNKNOWN;
				break;
			case EQUAL:
				relation = Relation.AT_MOST;
				break;
			case AT_MOST:
			case UNKNOWN:
				// stays like it is
				break;
		}
	}

	public boolean isKnown() {
		return relation == Relation.EQUAL;
	}

	public void setUnkown() {
		this.relation = Relation.UNKNOWN;
	}

	/**
	 * Returns the value. Call this method only if {@link #isKnown()} returns true. Otherwise, a
	 * runtime exception will be thrown.
	 */
	public T getValue() {
		if (isKnown()) {
			return getNumber();
		} else {
			throw new IllegalStateException("Value is currently not exactly known.");
		}
	}

	public Relation getRelation() {
		return relation;
	}

	@Override
	public String toString() {
		switch (relation) {
			case EQUAL:
				return "=" + Tools.formatNumber(getNumber().doubleValue(), 3);
			case AT_LEAST:
				return "\u2265" + Tools.formatNumber(getNumber().doubleValue(), 3);
			case AT_MOST:
				return "\u2264" + Tools.formatNumber(getNumber().doubleValue(), 3);
			case UNKNOWN:
			default:
				return "?";
		}
	}

	public abstract MDNumber<T> copy();

	protected void setNumber(T number) {
		this.number = number;
	}

	/**
	 * This returns the estimated number regardless of relation. Please take care to handle the
	 * relation correctly.
	 */
	public T getNumber() {
		return number;
	}

	public abstract MDNumber<T> add(T add);

	public abstract MDNumber<T> multiply(double factor);

	@Override
	public int compareTo(MDNumber<T> other) {
		return (int) Math.signum(this.number.doubleValue() - other.number.doubleValue());
	}
}
