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
 * An iterator for attribute roles which is able to iterate over all attributes or skip either
 * regular or special attributes.
 * 
 * @author Ingo Mierswa
 */
public class AttributeRoleIterator implements Iterator<AttributeRole> {

	private Iterator<AttributeRole> parent;

	private int type = Attributes.REGULAR;

	private AttributeRole current = null;

	public AttributeRoleIterator(Iterator<AttributeRole> parent, int type) {
		this.parent = parent;
		this.type = type;
	}

	@Override
	public boolean hasNext() {
		while (current == null && parent.hasNext()) {
			AttributeRole candidate = parent.next();
			switch (type) {
				case Attributes.REGULAR:
					if (!candidate.isSpecial()) {
						current = candidate;
					}
					break;
				case Attributes.SPECIAL:
					if (candidate.isSpecial()) {
						current = candidate;
					}
					break;
				case Attributes.ALL:
					current = candidate;
					break;
				default:
					break;
			}
		}
		return current != null;
	}

	@Override
	public AttributeRole next() {
		if (current == null) {
			hasNext();
		}
		AttributeRole returnValue = current;
		current = null;
		return returnValue;
	}

	@Override
	public void remove() {
		parent.remove();
		this.current = null;
	}
}
