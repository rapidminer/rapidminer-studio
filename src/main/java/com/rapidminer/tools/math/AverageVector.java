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
package com.rapidminer.tools.math;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.tools.Tools;


/**
 * Handles several averagables.
 * 
 * @author Ingo Mierswa
 */
public abstract class AverageVector extends ResultObjectAdapter implements Comparable<Object>, Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6207859713603581755L;
	private List<Averagable> averagesList = new ArrayList<Averagable>();

	@Override
	public abstract Object clone() throws CloneNotSupportedException;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AverageVector)) {
			return false;
		}
		AverageVector v = (AverageVector) o;
		return averagesList.equals(v.averagesList);
	}

	@Override
	public int hashCode() {
		return this.averagesList.hashCode();
	}

	/** Returns the number of averages in the list. */
	public int size() {
		return averagesList.size();
	}

	/** Adds an {@link Averagable} to the list of criteria. */
	public void addAveragable(Averagable avg) {
		averagesList.add(avg);
	}

	/** Removes an {@link Averagable} from the list of criteria. */
	public void removeAveragable(Averagable avg) {
		averagesList.remove(avg);
	}

	/** Returns the Averagable by index. */
	public Averagable getAveragable(int index) {
		return averagesList.get(index);
	}

	/** Returns the Averagable by name. */
	public Averagable getAveragable(String name) {
		Iterator<Averagable> i = averagesList.iterator();
		while (i.hasNext()) {
			Averagable a = i.next();
			if (a.getName().equals(name)) {
				return a;
			}
		}
		return null;
	}

	/** Returns the number of averagables in this vector. */
	public int getSize() {
		return averagesList.size();
	}

	@Override
	public String toResultString() {
		StringBuffer result = new StringBuffer(getName());
		result.append(":");
		result.append(Tools.getLineSeparator());
		Iterator<Averagable> i = averagesList.iterator();
		while (i.hasNext()) {
			result.append(i.next().toResultString());
			result.append(Tools.getLineSeparator());
		}
		return result.toString();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("AverageVector [");
		for (int i = 0; i < size(); i++) {
			Averagable avg = getAveragable(i);
			if (i > 0) {
				result.append(", ");
			}
			result.append(avg);
		}
		result.append("]");
		return result.toString();
	}

	public void buildAverages(AverageVector av) {
		if (this.size() != av.size()) {
			throw new IllegalArgumentException("Performance vectors have different size!");
		}
		for (int i = 0; i < size(); i++) {
			this.getAveragable(i).buildAverage(av.getAveragable(i));
		}
	}
}
