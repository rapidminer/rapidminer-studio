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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;

import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ViewModel;


/**
 * This is a generic example set (view on the view stack of the data) which can be used to apply any
 * preprocessing model and create a view from it.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class ModelViewExampleSet extends AbstractExampleSet {

	private static final long serialVersionUID = -6443667708498013284L;

	private ExampleSet parent;

	private Attributes attributes;

	public ModelViewExampleSet(ExampleSet parent, ViewModel model) {
		this.parent = (ExampleSet) parent.clone();
		Attributes modelAttributes = model.getTargetAttributes(parent);
		// clear parent attributes and copy model attributes there
		Attributes parentAttributes = this.parent.getAttributes();
		parentAttributes.clearRegular();
		parentAttributes.clearSpecial();
		for (Iterator<AttributeRole> i = modelAttributes.allAttributeRoles(); i.hasNext();) {
			parentAttributes.add(i.next());
		}
	}

	/** Clone constructor. */
	public ModelViewExampleSet(ModelViewExampleSet other) {
		this.parent = (ExampleSet) other.parent.clone();
	}

	@Override
	public Attributes getAttributes() {
		return parent.getAttributes();
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
		DataRow dataRow = this.parent.getExample(index).getDataRow();
		if (dataRow == null) {
			return null;
		} else {
			return new Example(dataRow, this);
		}
	}

	@Override
	public ExampleTable getExampleTable() {
		return this.parent.getExampleTable();
	}

	@Override
	public int size() {
		return this.parent.size();
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
		// possible since {@link ViewAttribute}s have the same table index as the attributes that
		// they view
		parent.cleanup();
	}

	/**
	 * Adjust old version to new version where attributes are no longer stored in attribute field
	 * but in parent attributes.
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (attributes != null) {
			Attributes parentAttributes = parent.getAttributes();
			parentAttributes.clearRegular();
			parentAttributes.clearSpecial();
			for (Iterator<AttributeRole> i = attributes.allAttributeRoles(); i.hasNext();) {
				parentAttributes.add(i.next());
			}
			attributes = null;
		}
	}
}
