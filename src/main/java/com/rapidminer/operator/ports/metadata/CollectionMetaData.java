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

import com.rapidminer.operator.IOObjectCollection;


/**
 * MetaData for a {@link IOObjectCollection}. The collection's elements are represented by
 * {@link #elementMetaData} which represents the "union" of all elements.
 * 
 * @author Simon Fischer
 * 
 */
public class CollectionMetaData extends MetaData {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MetaData elementMetaData;

	/** Keep for clone! */
	public CollectionMetaData() {}

	public CollectionMetaData(MetaData elementMetaData) {
		super(IOObjectCollection.class);
		this.elementMetaData = elementMetaData;
	}

	/** Factory constructor for {@link MetaDataFactory}. */
	public CollectionMetaData(IOObjectCollection<?> col, boolean shortened) {
		super(IOObjectCollection.class);
		if (col.size() > 0) {
			this.elementMetaData = MetaData.forIOObject(col.getElement(0, false), shortened);
		} else {
			this.elementMetaData = new MetaData();
		}
	}

	@Override
	public String getDescription() {
		if (elementMetaData != null) {
			return "Collection of " + elementMetaData.getDescription();
		} else {
			return "Collection";
		}
	}

	public MetaData getElementMetaData() {
		return elementMetaData;
	}

	@Override
	public CollectionMetaData clone() {
		CollectionMetaData clone = (CollectionMetaData) super.clone();
		clone.elementMetaData = this.elementMetaData != null ? this.elementMetaData.clone() : null;
		return clone;
	}

	public MetaData getElementMetaDataRecursive() {
		if (elementMetaData instanceof CollectionMetaData) {
			return ((CollectionMetaData) elementMetaData).getElementMetaData();
		} else {
			return elementMetaData;
		}
	}

}
