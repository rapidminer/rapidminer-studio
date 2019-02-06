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
package com.rapidminer.operator;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;


/**
 * An unmodifyable view of a collection of Operators that hides disabled operators.
 * 
 * @author Simon Fischer
 * 
 */
public class EnabledOperatorView extends AbstractList<Operator> {

	private Collection<Operator> base;

	public EnabledOperatorView(Collection<Operator> base) {
		super();
		this.base = base;
	}

	@Override
	public Iterator<Operator> iterator() {
		return new Iterator<Operator>() {

			private Operator next;
			private Iterator<Operator> baseIterator = base.iterator();

			@Override
			public boolean hasNext() {
				if (next != null) {
					return true;
				}
				while (baseIterator.hasNext()) {
					next = baseIterator.next();
					if (next.isEnabled()) {
						return true;
					}
				}
				next = null;
				return false;
			}

			@Override
			public Operator next() {
				hasNext();
				Operator result = next;
				next = null;
				return result;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Unmodifyable view!");
			}

		};
	}

	@Override
	public int size() {
		int size = 0;
		Iterator<Operator> i = iterator();
		while (i.hasNext()) {
			i.next();
			size++;
		}
		return size;
	}

	@Override
	public Operator get(int index) {
		int n = 0;
		Iterator<Operator> i = iterator();
		while (i.hasNext()) {
			Operator next = i.next();
			if (n == index) {
				return next;
			}
			n++;
		}
		return null;
	}
}
