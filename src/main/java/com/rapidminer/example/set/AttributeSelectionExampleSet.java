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

import java.util.Iterator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.ExampleTable;


/**
 * An implementation of ExampleSet that is only a fixed view on a selection of attributes of the
 * parent example set.
 *
 * @author Ingo Mierswa
 */
public class AttributeSelectionExampleSet extends AbstractExampleSet {

	private static final long serialVersionUID = 7946137859300860625L;

	private ExampleSet parent;

	/**
	 * Constructs a new AttributeSelectionExampleSet. Only those attributes with a true value in the
	 * selection mask will be used. If the given mask is null, all regular attributes of the parent
	 * example set will be used.
	 *
	 * @deprecated use static creation method {@link #create(ExampleSet, boolean[])} instead
	 */
	@Deprecated
	public AttributeSelectionExampleSet(ExampleSet exampleSet, boolean[] selectionMask) {
		this.parent = (ExampleSet) exampleSet.clone();
		selectAttributes(selectionMask, parent);
	}

	/**
	 * Creates a new example set with only the regular attributes selected by the selection mask.
	 * Only those attributes with a true value in the selection mask will be used. If the given mask
	 * is null, all regular attributes of the parent example set will be used. The special
	 * attributes stay the same.
	 *
	 * @param exampleSet
	 *            the example set to start from
	 * @param selectionMask
	 *            the mask defining which attributes to use
	 * @return an example set which the specified attributes selected
	 * @throws IllegalArgumentException
	 *             if the selectionMask has not the same number of entries as regular attributes in
	 *             the example set
	 * @since 7.5.1
	 */
	public static ExampleSet create(ExampleSet exampleSet, boolean[] selectionMask) {
		ExampleSet newSet = (ExampleSet) exampleSet.clone();
		selectAttributes(selectionMask, newSet);
		return newSet;
	}

	/**
	 * Removes all attributes at which index the selectinMask is {@code false}.
	 *
	 * @throws IllegalArgumentException
	 *             if the selectionMask has not the same number of entries as regular attributes in
	 *             the example set
	 */
	private static void selectAttributes(boolean[] selectionMask, ExampleSet exampleSet) {
		if (selectionMask != null) {
			if (selectionMask.length != exampleSet.getAttributes().size()) {
				throw new IllegalArgumentException(
						"Length of the selection mask must be equal to the parent's number of attributes.");
			}

			int counter = 0;
			Iterator<Attribute> i = exampleSet.getAttributes().iterator();
			while (i.hasNext()) {
				i.next();
				if (!selectionMask[counter]) {
					i.remove();
				}
				counter++;
			}
		}
	}

	/** Clone constructor. */
	public AttributeSelectionExampleSet(AttributeSelectionExampleSet exampleSet) {
		cloneAnnotationsFrom(exampleSet);
		this.parent = (ExampleSet) exampleSet.parent.clone();
	}

	@Override
	public boolean equals(Object o) {
		if (!super.equals(o)) {
			return false;
		}
		if (!(o instanceof AttributeSelectionExampleSet)) {
			return false;
		}
		return this.parent.equals(o);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ this.parent.hashCode();
	}

	// -------------------- overridden methods --------------------

	/** Returns the attribute container. */
	@Override
	public Attributes getAttributes() {
		return this.parent.getAttributes();
	}

	/**
	 * Creates a new example set reader.
	 */
	@Override
	public Iterator<Example> iterator() {
		return new AttributesExampleReader(parent.iterator(), this);
	}

	@Override
	public Example getExample(int index) {
		return this.parent.getExample(index);
	}

	@Override
	public ExampleTable getExampleTable() {
		return parent.getExampleTable();
	}

	@Override
	public int size() {
		return parent.size();
	}

	@Override
	public void cleanup() {
		parent.cleanup();
	}
}
