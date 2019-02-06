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
package com.rapidminer.example.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SimpleExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.att.AttributeSet;


/**
 * This class is the core data supplier for example sets. Several example sets can use the same data
 * and access the attribute values by reference.
 *
 * @author Ingo Mierswa
 */
public abstract class AbstractExampleTable implements ExampleTable {

	/**
	 *
	 */
	private static final long serialVersionUID = -6996954528182122684L;

	/**
	 * List of instances of {@link Attribute}. The <i>i</i>-th entry in the list belongs to the
	 * <i>i</i>-th data column. Whenever attributes are removed from the list of attributes (e.g.
	 * they were intermediate predicted labels used only within a validation chain), the succeeding
	 * entries do not move up, but the entry is replaced by a null entry and this index is added to
	 * {@link AbstractExampleTable#unusedColumnList} as an Integer.
	 */
	private List<Attribute> attributes = new ArrayList<>();

	/**
	 * List of Integers referencing indices of columns that were removed, e.g. predicted labels that
	 * were used within a validation chain but are not needed any longer. Any of the columns in this
	 * list may be used when a new attribute is created. The list is used as a queue.
	 */
	private List<Integer> unusedColumnList = new LinkedList<>();

	/**
	 * Creates a new ExampleTable.
	 *
	 * @param attributes
	 *            List of {@link Attribute}. The indices of the attibutes are set to values
	 *            reflecting their position in the list.
	 */
	public AbstractExampleTable(List<Attribute> attributes) {
		addAttributes(attributes);
	}

	/**
	 * Clone constructor.
	 * 
	 * @param other
	 *            the table to clone
	 */
	protected AbstractExampleTable(AbstractExampleTable other) {
		this.attributes = new ArrayList<>(other.attributes);
		this.unusedColumnList = new LinkedList<>(other.unusedColumnList);
	}

	// ------------------------------------------------------------

	/** Returns a new array containing all {@link Attribute}s. */
	@Override
	public Attribute[] getAttributes() {
		Attribute[] attribute = new Attribute[attributes.size()];
		attributes.toArray(attribute);
		return attribute;
	}

	/**
	 * Returns the attribute of the column number <i>i</i>. Attention: This value may return null if
	 * the column was marked unused.
	 */
	@Override
	public Attribute getAttribute(int i) {
		return attributes.get(i);
	}

	/** Returns the attribute with the given name. */
	@Override
	public Attribute findAttribute(String name) throws OperatorException {
		if (name == null) {
			return null;
		}
		for (Attribute att : attributes) {
			if (att != null) {
				if (att.getName().equals(name)) {
					return att;
				}
			}
		}
		throw new AttributeNotFoundError(null, null, name);
	}

	/**
	 * Adds all {@link Attribute}s in <code>newAttributes</code> to the end of the list of
	 * attributes, creating new data columns if necessary.
	 */
	@Override
	public void addAttributes(Collection<Attribute> newAttributes) {
		for (Attribute att : newAttributes) {
			addAttribute(att);
		}
	}

	/**
	 * Adds the attribute to the list of attributes assigning it a free column index. If the name is
	 * already in use, the attribute will be renamed.
	 */
	@Override
	public int addAttribute(Attribute a) {

		if (a == null) {
			throw new IllegalArgumentException("Attribute must not be null");
		} else {
			int index = -1;
			Attribute original = a;
			a = (Attribute) a.clone();

			if (unusedColumnList.size() > 0) {
				// if seems to be something free: Synchronize and check again
				synchronized (unusedColumnList) {
					if (unusedColumnList.size() > 0) {
						index = unusedColumnList.remove(0);
						attributes.set(index, a);
					} else {
						index = attributes.size();
						attributes.add(a);
					}
				}
			} else {
				index = attributes.size();
				attributes.add(a);
			}

			a.setTableIndex(index);
			original.setTableIndex(index);
			return index;
		}

	}

	/**
	 * Equivalent to calling <code>removeAttribute(attribute.getTableIndex())</code>.
	 */
	@Override
	public void removeAttribute(Attribute attribute) {
		removeAttribute(attribute.getTableIndex());
	}

	/**
	 * Sets the attribute with the given index to null. Afterwards, this column can be reused.
	 * Callers must make sure, that no other example set contains a reference to this column.
	 * Otherwise its data will be messed up. Usually this is only possible if an operator generates
	 * intermediate attributes, like a validation chain or a feature generator. If the attribute
	 * already was removed, this method returns silently.
	 */
	@Override
	public synchronized void removeAttribute(int index) {
		Attribute a = attributes.get(index);
		if (a == null) {
			return;
		}
		attributes.set(index, null);
		unusedColumnList.add(index);
	}

	/**
	 * Returns the number of attributes. Attention: Callers that use a for-loop and retrieving
	 * {@link Attribute}s by calling {@link AbstractExampleTable#getAttribute(int)} must keep in
	 * mind, that some of these attributes may be null.
	 */
	@Override
	public int getNumberOfAttributes() {
		return attributes.size();
	}

	/**
	 * Returns the number of non null attributes. <b>Attention</b>: Since there are null attributes
	 * in the list, the return value of this method must not be used in a for-loop!
	 *
	 * @see ExampleTable#getNumberOfAttributes().
	 */
	@Override
	public int getAttributeCount() {
		return attributes.size() - unusedColumnList.size();
	}

	// ------------------------------------------------------------

	/**
	 * Returns a new example set with all attributes switched on. The given attribute will be used
	 * as a special label attribute for learning.
	 */
	@Override
	public ExampleSet createExampleSet(Attribute labelAttribute) {
		return createExampleSet(labelAttribute, null, null);
	}

	/**
	 * Returns a new example set with all attributes switched on. The given attributes will be used
	 * as a special label attribute for learning, as (example) weight attribute, and as id
	 * attribute.
	 */
	@Override
	public ExampleSet createExampleSet(Attribute labelAttribute, Attribute weightAttribute, Attribute idAttribute) {
		Map<Attribute, String> specialAttributes = new LinkedHashMap<>();
		if (labelAttribute != null) {
			specialAttributes.put(labelAttribute, Attributes.LABEL_NAME);
		}
		if (weightAttribute != null) {
			specialAttributes.put(weightAttribute, Attributes.WEIGHT_NAME);
		}
		if (idAttribute != null) {
			specialAttributes.put(idAttribute, Attributes.ID_NAME);
		}
		return new SimpleExampleSet(this, specialAttributes);
	}

	/**
	 * Returns a new example set with all attributes switched on. The iterator over the attribute
	 * roles will define the special attributes.
	 */
	@Override
	public ExampleSet createExampleSet(Iterator<AttributeRole> newSpecialAttributes) {
		Map<Attribute, String> specialAttributes = new LinkedHashMap<>();
		while (newSpecialAttributes.hasNext()) {
			AttributeRole role = newSpecialAttributes.next();
			specialAttributes.put(role.getAttribute(), role.getSpecialName());
		}
		return new SimpleExampleSet(this, specialAttributes);
	}

	/**
	 * Returns a new example set with all attributes of the {@link ExampleTable} and with the
	 * special roles defined by the given attribute set.
	 */
	@Override
	public ExampleSet createExampleSet(AttributeSet attributeSet) {
		Map<Attribute, String> specialAttributes = new LinkedHashMap<>();
		Iterator<String> i = attributeSet.getSpecialNames().iterator();
		while (i.hasNext()) {
			String name = i.next();
			specialAttributes.put(attributeSet.getSpecialAttribute(name), name);
		}
		return createExampleSet(specialAttributes);
	}

	/**
	 * Returns a new example set with all attributes switched on. All attributes given at creation
	 * time will be regular.
	 */
	@Override
	public ExampleSet createExampleSet() {
		return createExampleSet(Collections.<Attribute, String> emptyMap());
	}

	/**
	 * Returns a new example set with all attributes switched on. The attributes in the given map
	 * will be used as special attributes with the specified names, all other attributes given at
	 * creation time will be regular. The ordering of the attributes is defined by the iteration
	 * order of the map.
	 */
	@Override
	public ExampleSet createExampleSet(Map<Attribute, String> specialAttributes) {
		return new SimpleExampleSet(this, specialAttributes);
	}

	// ------------------------------------------------------------

	@Override
	public String toString() {
		return "ExampleTable, " + attributes.size() + " attributes, " + size() + " data rows," + Tools.getLineSeparator()
				+ "attributes: " + attributes;
	}

	@Override
	public String toDataString() {
		StringBuffer result = new StringBuffer(toString() + Tools.getLineSeparator());
		DataRowReader reader = getDataRowReader();
		while (reader.hasNext()) {
			result.append(reader.next().toString() + Tools.getLineSeparator());
		}
		return result.toString();
	}
}
