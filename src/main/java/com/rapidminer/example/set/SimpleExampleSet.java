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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.table.internal.ColumnarExampleTable;


/**
 * A simple implementation of ExampleSet containing a list of attributes and a special attribute
 * map. The data is queried from an example table which contains the data (example sets actually are
 * only views on this table and does not keep any data). This simple example set implementation
 * usually is the basic example set of the multi-layered data view.
 *
 * @author Ingo Mierswa, Simon Fischer Exp $
 */
public class SimpleExampleSet extends AbstractExampleSet {

	private static final long serialVersionUID = 9163340881176421801L;

	/** The table used for reading the examples from. */
	private ExampleTable exampleTable;

	/** Holds all information about the attributes. */
	private Attributes attributes = new SimpleAttributes();

	/**
	 * Constructs a new SimpleExampleSet backed by the given example table. The example set
	 * initially does not have any special attributes but all attributes from the given table will
	 * be used as regular attributes.
	 *
	 * If you are constructing the example set from a {@link MemoryExampleTable}, you should use the
	 * method {@link MemoryExampleTable#createExampleSet()} instead unless you are absolutely sure
	 * what you are doing.
	 */
	public SimpleExampleSet(ExampleTable exampleTable) {
		this(exampleTable, null, null);
	}

	/**
	 * Constructs a new SimpleExampleSet backed by the given example table. The example set
	 * initially does not have any special attributes but all attributes from the given table will
	 * be used as regular attributes.
	 *
	 * If you are constructing the example set from a {@link MemoryExampleTable}, you should use the
	 * method {@link MemoryExampleTable#createExampleSet()} instead unless you are absolutely sure
	 * what you are doing.
	 */
	public SimpleExampleSet(ExampleTable exampleTable, List<Attribute> regularAttributes) {
		this(exampleTable, regularAttributes, null);
	}

	/**
	 * Constructs a new SimpleExampleSet backed by the given example table. All attributes in the
	 * table apart from the special attributes become normal (regular) attributes. The special
	 * attributes are specified by the given map. The ordering of the attributes is defined by the
	 * iteration order of the map.
	 *
	 * If you are constructing the example set from a {@link MemoryExampleTable}, you should use the
	 * method {@link MemoryExampleTable#createExampleSet(Map)} instead unless you are absolutely
	 * sure what you are doing.
	 */
	public SimpleExampleSet(ExampleTable exampleTable, Map<Attribute, String> specialAttributes) {
		this(exampleTable, null, specialAttributes);
	}

	/**
	 * Constructs a new SimpleExampleSet backed by the given example table. All attributes in the
	 * table defined in the regular attribute list apart from those (also) defined the special
	 * attributes become normal (regular) attributes. The special attributes are specified by the
	 * given map. The ordering of the attributes is defined by the iteration order of the map.
	 *
	 * If you are constructing the example set from a {@link MemoryExampleTable}, you should use the
	 * method {@link MemoryExampleTable#createExampleSet(Map)} instead unless you are absolutely
	 * sure what you are doing.
	 */
	public SimpleExampleSet(ExampleTable exampleTable, List<Attribute> regularAttributes,
			Map<Attribute, String> specialAttributes) {
		this.exampleTable = exampleTable;
		List<Attribute> regularList = regularAttributes;
		if (regularList == null) {
			regularList = new LinkedList<Attribute>();
			for (int a = 0; a < exampleTable.getNumberOfAttributes(); a++) {
				Attribute attribute = exampleTable.getAttribute(a);
				if (attribute != null) {
					regularList.add(attribute);
				}
			}
		}

		for (Attribute attribute : regularList) {
			if ((specialAttributes == null) || (specialAttributes.get(attribute) == null)) {
				getAttributes().add(new AttributeRole((Attribute) attribute.clone()));
			}
		}

		if (specialAttributes != null) {
			Iterator<Map.Entry<Attribute, String>> s = specialAttributes.entrySet().iterator();
			while (s.hasNext()) {
				Map.Entry<Attribute, String> entry = s.next();
				getAttributes().setSpecialAttribute((Attribute) entry.getKey().clone(), entry.getValue());
			}
		}
	}

	/**
	 * Clone constructor. The example table is copied by reference, the attributes are copied by a
	 * deep clone.
	 *
	 * Don't use this method directly but use the clone method instead.
	 */
	public SimpleExampleSet(SimpleExampleSet exampleSet) {
		cloneAnnotationsFrom(exampleSet);
		this.exampleTable = exampleSet.exampleTable;
		this.attributes = (Attributes) exampleSet.getAttributes().clone();
	}

	// --- attributes ---

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	// --- examples ---

	@Override
	public ExampleTable getExampleTable() {
		return exampleTable;
	}

	@Override
	public int size() {
		return exampleTable.size();
	}

	@Override
	public Example getExample(int index) {
		DataRow dataRow = getExampleTable().getDataRow(index);
		if (dataRow == null) {
			return null;
		} else {
			return new Example(dataRow, this);
		}
	}

	@Override
	public Iterator<Example> iterator() {
		return new SimpleExampleReader(getExampleTable().getDataRowReader(), this);
	}

	@Override
	public void cleanup() {
		if (exampleTable instanceof ColumnarExampleTable) {
			ColumnarExampleTable table = (ColumnarExampleTable) exampleTable;
			this.exampleTable = table.columnCleanupClone(attributes);
		}
	}

	@Override
	public boolean isThreadSafeView() {
		return true;
	}
}
