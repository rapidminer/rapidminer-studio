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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.att.AttributeSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
 * <p>
 * This class is the core data supplier for example sets. Several example sets can use the same data
 * and access the attribute values by reference. Thats means that ExampleTable contains all data
 * like in a database management systems and all {@link ExampleSet}s are only views on the data. The
 * ExampleSets themself do hence not contain any data rows and can be cloned without copying the
 * data.
 * </p>
 * 
 * <p>
 * Changing the data in the ExampleTable will change the data for all views (ExampleSets). On the
 * other hand, the changes for one view (ExampleSet) like adding or removing {@link Attribute}s will
 * not change the ExampleTable and will also not change other views (ExampleSets).
 * </p>
 * 
 * @author Ingo Mierswa
 */
public interface ExampleTable extends Serializable {

	/** Returns the number of examples. */
	public int size();

	/**
	 * Returns an Iterator for example data given as <code>DataRow</code> objects. This should be
	 * used in all cases where iteration is desired. Since {@link #getDataRow(int)} does not ensure
	 * to work in an efficient way the usage of this method is preferred (instead using for-loops).
	 */
	public DataRowReader getDataRowReader();

	/**
	 * Returns the i-th data row. Calling methods cannot rely on the efficiency of this method.
	 * Memory based example tables should return the data row in O(1).
	 */
	public DataRow getDataRow(int index);

	/** Returns a new array containing all {@link Attribute}s. */
	public Attribute[] getAttributes();

	/**
	 * Returns the attribute of the column number <i>i</i>. Attention: This value may return null if
	 * the column was marked unused.
	 */
	public Attribute getAttribute(int i);

	/**
	 * Returns the attribute with the given name. CAUTION: This does only return the first attribute
	 * found with this name. Since there is no guarantee that names are unique, this might not
	 * deliver the desired result.
	 */
	public Attribute findAttribute(String name) throws OperatorException;

	/**
	 * Adds all {@link Attribute}s in <code>newAttributes</code> to the end of the list of
	 * attributes, creating new data columns if necessary.
	 */
	public void addAttributes(Collection<Attribute> newAttributes);

	/**
	 * Adds a clone of the attribute <code>a</code> to the list of attributes assigning it a free
	 * column index. The column index is also set on <code>a</code>.
	 */
	public int addAttribute(Attribute a);

	/**
	 * Equivalent to calling <code>removeAttribute(attribute.getTableIndex())</code>.
	 */
	public void removeAttribute(Attribute attribute);

	/**
	 * Sets the attribute with the given index to null. Afterwards, this column can be reused.
	 * Callers must make sure, that no other example set contains a reference to this column.
	 * Otherwise its data will be messed up. Usually this is only possible if an operator generates
	 * intermediate attributes, like a validation chain or a feature generator. If the attribute
	 * already was removed, this method returns silently.
	 */
	public void removeAttribute(int index);

	/**
	 * Returns the number of attributes. Attention: Callers that use a for-loop and retrieving
	 * {@link Attribute}s by calling {@link AbstractExampleTable#getAttribute(int)} must keep in
	 * mind, that some of these attributes may be null.
	 */
	public int getNumberOfAttributes();

	/**
	 * Returns the number of non null attributes. <b>Attention</b>: Since there might be null
	 * attributes in the table, the return value of this method must not be used in a for-loop!
	 * 
	 * @see ExampleTable#getNumberOfAttributes().
	 */
	public int getAttributeCount();

	/**
	 * Returns a new example set with all attributes switched on. The given attribute will be used
	 * as a special label attribute for learning.
	 */
	public ExampleSet createExampleSet(Attribute labelAttribute);

	/*
	 * Returns a new example set with all attributes switched on. The iterator over the attribute
	 * roles will define the special attributes.
	 */
	public ExampleSet createExampleSet(Iterator<AttributeRole> newSpecialAttributes);

	/**
	 * Returns a new example set with all attributes switched on. The given attributes will be used
	 * as a special label attribute for learning, as (example) weight attribute, and as id
	 * attribute.
	 */
	public ExampleSet createExampleSet(Attribute labelAttribute, Attribute weightAttribute, Attribute idAttribute);

	/**
	 * Returns a new example set with all attributes of the {@link ExampleTable} and with the
	 * special roles defined by the given attribute set.
	 */
	public ExampleSet createExampleSet(AttributeSet attributeSet);

	/**
	 * Returns a new example set with all attributes switched on. The attributes in the given map
	 * will be used as special attributes, all other attributes given at creation time will be
	 * regular.
	 */
	public ExampleSet createExampleSet(Map<Attribute, String> specialAttributes);

	/**
	 * Returns a new example set with all attributes switched on. All attributes will be used as
	 * regular attributes.
	 */
	public ExampleSet createExampleSet();

	/** Returns a string representation of this example table. */
	@Override
	public String toString();

	/** Dumps the complete data as string. */
	public String toDataString();

}
