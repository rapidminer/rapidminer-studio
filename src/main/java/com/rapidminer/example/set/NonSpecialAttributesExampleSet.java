/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.operator.Annotations;

import java.util.Iterator;


/**
 * This example set treats all special attributes as regular attributes.
 * 
 * @author Ingo Mierswa
 */
public class NonSpecialAttributesExampleSet extends AbstractExampleSet {

	private static final long serialVersionUID = -4782316585512718459L;

	/** The parent example set. */
	private ExampleSet parent;

	public NonSpecialAttributesExampleSet(ExampleSet exampleSet) {
		this.parent = (ExampleSet) exampleSet.clone();
		Iterator<AttributeRole> s = this.parent.getAttributes().specialAttributes();
		while (s.hasNext()) {
			AttributeRole attributeRole = s.next();
			if (attributeRole.isSpecial()) {
				attributeRole.changeToRegular();
			}
		}
	}

	/** Clone constructor. */
	public NonSpecialAttributesExampleSet(NonSpecialAttributesExampleSet exampleSet) {
		this.parent = (ExampleSet) exampleSet.parent.clone();
	}

	@Override
	public Attributes getAttributes() {
		return this.parent.getAttributes();
	}

	/**
	 * Creates an iterator over all examples.
	 */
	@Override
	public Iterator<Example> iterator() {
		return new AttributesExampleReader(parent.iterator(), this);
	}

	@Override
	public ExampleTable getExampleTable() {
		return parent.getExampleTable();
	}

	@Override
	public Example getExample(int index) {
		return this.parent.getExample(index);
	}

	@Override
	public int size() {
		return parent.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.operator.ResultObjectAdapter#getAnnotations()
	 */
	@Override
	public Annotations getAnnotations() {
		return parent.getAnnotations();
	}

	@Override
	public void cleanup() {
		parent.cleanup();
	}
}
