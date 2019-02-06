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
package com.rapidminer.gui.viewer.collection;

import java.util.UUID;

import com.rapidminer.operator.IOObject;


/**
 * Wrapper class for an IOObject as an element of the CollectionTree. This class prevents two or
 * more IOObjects without Attributes from being treated as the same Object. A JTree with duplicate
 * Objects would cause errors.
 *
 * @author Marcel Seifert
 *
 */
public class CollectionTreeElement {

	private final UUID uniqueID = UUID.randomUUID();
	private final IOObject ioobject;

	public CollectionTreeElement(IOObject ioobject) {
		this.ioobject = ioobject;

	}

	public IOObject getIOObject() {
		return ioobject;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (ioobject == null ? 0 : ioobject.hashCode());
		result = prime * result + (uniqueID == null ? 0 : uniqueID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		CollectionTreeElement other = (CollectionTreeElement) obj;
		if (ioobject == null) {
			if (other.ioobject != null) {
				return false;
			}
		} else if (!ioobject.equals(other.ioobject)) {
			return false;
		}
		if (uniqueID == null) {
			if (other.uniqueID != null) {
				return false;
			}
		} else if (!uniqueID.equals(other.uniqueID)) {
			return false;
		}
		return true;
	}

}
