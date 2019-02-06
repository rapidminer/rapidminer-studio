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
package com.rapidminer.example;

import java.util.Iterator;


/**
 * Iterates over the regular attributes. This does the same as {@link AttributeIterator} with type
 * {@link Attributes#REGULAR} but was intentionally moved to an extra class for performance reasons.
 *
 * @author Ingo Mierswa, Gisa Schaefer
 * @since 7.4.0
 */
public class RegularAttributeIterator implements Iterator<Attribute> {

	private Iterator<AttributeRole> parent;

	private Attribute current = null;

	public RegularAttributeIterator(Iterator<AttributeRole> parent) {
		this.parent = parent;
	}

	@Override
	public boolean hasNext() {
		while (current == null && parent.hasNext()) {
			AttributeRole candidate = parent.next();
			if (!candidate.isSpecial()) {
				current = candidate.getAttribute();
				break;
			}
		}
		return current != null;
	}

	@Override
	public Attribute next() {
		if (current == null) {
			hasNext();
		}
		Attribute returnValue = current;
		this.current = null;
		return returnValue;
	}

	@Override
	public void remove() {
		parent.remove();
	}
}
