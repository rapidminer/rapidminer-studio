/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.util.LinkedList;


/**
 * A frequency stack based on a list implementation.
 * 
 * @author Sebastian Land
 */
public class ListFrequencyStack implements FrequencyStack {

	private LinkedList<Integer> list;

	public ListFrequencyStack() {
		list = new LinkedList<Integer>();
	}

	@Override
	public int getFrequency(int height) {
		if (height >= list.size()) {
			return 0;
		} else if (height == list.size() - 1) {
			return list.getLast();
		} else {
			return list.get(height);
		}
	}

	@Override
	public void increaseFrequency(int stackHeight, int value) {
		if (stackHeight == list.size() - 1) {
			// int newValue = value + list.pollLast(); // IM: pollLast only
			// available in JDK 6
			int newValue = value + list.removeLast();
			list.addLast(newValue);
		} else if (stackHeight == list.size()) {
			list.addLast(value);
		}
	}

	@Override
	public void popFrequency(int height) {
		if (height == list.size() - 1) {
			// list.pollLast(); // IM: pollLast only available in JDK 6
			list.removeLast();
		} else if (height < list.size() - 1) {
			list.remove(height);
		}
	}
}
