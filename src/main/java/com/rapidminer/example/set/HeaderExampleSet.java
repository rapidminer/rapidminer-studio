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
package com.rapidminer.example.set;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.ExampleTable;

import java.util.Iterator;


/**
 * This example set is a clone of the attributes without reference to any data. Therefore it can be
 * used as a data header description. Since no data reference exist, all example based methods will
 * throw an {@link UnsupportedOperationException}.
 * 
 * @author Ingo Mierswa
 */
public class HeaderExampleSet extends AbstractExampleSet {

	private static final long serialVersionUID = -255270841843010670L;

	/** The parent example set. */
	private Attributes attributes;

	public HeaderExampleSet(ExampleSet parent) {
		cloneAnnotationsFrom(parent);
		this.attributes = (Attributes) parent.getAttributes().clone();
	}


	/**
	 * Creates a new header example set with the given attribute. The attributes are not cloned automatically and must
	 * be cloned before calling this constructor if they are shared with another {@link ExampleSet}.
	 *
	 * @param attributes
	 *            the attributes for the header example set
	 * @since 8.1.0
	 */
	public HeaderExampleSet(Attributes attributes) {
		this.attributes = attributes;
	}

	/** Header example set clone constructor. */
	public HeaderExampleSet(HeaderExampleSet other) {
		cloneAnnotationsFrom(other);
		this.attributes = (Attributes) other.attributes.clone();
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public Example getExample(int index) {
		return null;
	}

	@Override
	public Example getExampleFromId(double value) {
		throw new UnsupportedOperationException(
				"The method getExampleFromId(double) is not supported by the header example set.");
	}

	@Override
	public ExampleTable getExampleTable() {
		throw new UnsupportedOperationException("The method getExampleTable() is not supported by the header example set.");
	}

	@Override
	public void remapIds() {
		throw new UnsupportedOperationException("The method remapIds() is not supported by the header example set.");
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Iterator<Example> iterator() {
		throw new UnsupportedOperationException("The method iterator() is not supported by the header example set.");
	}
}
