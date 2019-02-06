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
package com.rapidminer.tools.math.container;

import java.util.Comparator;
import java.util.PriorityQueue;


/**
 * This class implements a bounded priority queue which contains always the n-th smalles values. If
 * a new value is added, it is added only if it is smaller than the greatest value already in queue.
 * The greatest value is then removed. The iterator does not iterate in any particular order. This
 * queue will implement a reverse order in compare with the java PriorityQueue, so peek and poll
 * will retrieve the greatest elements
 * 
 * @author Sebastian Land
 * 
 * @param <E>
 */
public class BoundedPriorityQueue<E> extends PriorityQueue<E> {

	private static final long serialVersionUID = 6020635755912950637L;

	private final int bound;
	private final Comparator<E> comparator;

	public BoundedPriorityQueue(int bound) {
		super(bound, new ReverseComparableComparator<E>());
		this.comparator = new ReverseComparableComparator<E>();
		this.bound = bound;
	}

	public BoundedPriorityQueue(int bound, Comparator<? super E> comp) {
		super(bound, new ReverseComparator<E>(comp));
		this.bound = bound;
		this.comparator = new ReverseComparator<E>(comp);
	}

	@Override
	public boolean offer(E e) {
		if (size() == bound) {
			E head = peek();
			// test with Reverse(!) comparator if e is smaller: Test if greater
			if (comparator.compare(e, head) > 0) {
				// if smaller: remove biggest and add e
				poll();
				return super.offer(e);
			}
		} else {
			return super.offer(e);
		}
		return false;
	}

	@Override
	public boolean add(E e) {
		return offer(e);
	}

	public boolean isFilled() {
		return (size() == bound);
	}
}
