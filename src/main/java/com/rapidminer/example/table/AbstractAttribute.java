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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeDescription;
import com.rapidminer.example.AttributeTransformation;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.Annotations;
import com.rapidminer.tools.Ontology;


/**
 * This is a possible abstract superclass for all attribute implementations. Most methods of
 * {@link Attribute} are already implemented here.
 *
 * @author Ingo Mierswa
 */
public abstract class AbstractAttribute implements Attribute {

	private static final long serialVersionUID = -9167755945651618227L;

	private transient List<Attributes> owners = new LinkedList<Attributes>();

	/** The basic information about the attribute. Will only be shallowly cloned. */
	private AttributeDescription attributeDescription;

	private final List<AttributeTransformation> transformations = new ArrayList<AttributeTransformation>();

	/** Contains all attribute statistics calculation algorithms. */
	private List<Statistics> statistics = new LinkedList<Statistics>();

	/** The current attribute construction description object. */
	private String constructionDescription = null;

	private Annotations annotations = new Annotations();

	// --------------------------------------------------------------------------------

	/**
	 * Creates a simple attribute which is not part of a series and does not provide a unit string.
	 * This constructor should only be used for attributes which were not generated with help of a
	 * generator, i.e. this attribute has no function arguments. Only the last transformation is
	 * cloned, the other transformations are cloned by reference.
	 */
	protected AbstractAttribute(AbstractAttribute attribute) {
		this.attributeDescription = attribute.attributeDescription;

		// copy statistics
		this.statistics = new LinkedList<Statistics>();
		for (Statistics statistics : attribute.statistics) {
			this.statistics.add((Statistics) statistics.clone());
		}

		// copy transformations if necessary (only the transformation on top of the view stack!)
		int counter = 0;
		for (AttributeTransformation transformation : attribute.transformations) {
			if (counter < attribute.transformations.size() - 1) {
				addTransformation(transformation);
			} else {
				addTransformation((AttributeTransformation) transformation.clone());
			}
			counter++;
		}

		// copy construction description
		this.constructionDescription = attribute.constructionDescription;

		// copy annotations
		annotations.putAll(attribute.getAnnotations());
	}

	/**
	 * Creates a simple attribute which is not part of a series and does not provide a unit string.
	 * This constructor should only be used for attributes which were not generated with help of a
	 * generator, i.e. this attribute has no function arguments.
	 */
	protected AbstractAttribute(String name, int valueType) {
		this.attributeDescription = new AttributeDescription(this, name, valueType, Ontology.SINGLE_VALUE, 0.0d,
				UNDEFINED_ATTRIBUTE_INDEX);
		this.constructionDescription = name;
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (owners == null) {
			owners = new LinkedList<Attributes>();
		}
		if (annotations == null) {
			annotations = new Annotations();
		}
	}

	@Override
	public void addOwner(Attributes attributes) {
		this.owners.add(attributes);
	}

	@Override
	public void removeOwner(Attributes attributes) {
		this.owners.remove(attributes);
	}

	/** Clones this attribute. */
	@Override
	public abstract Object clone();

	/**
	 * Returns true if the given attribute has the same name and the same table index.
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AbstractAttribute)) {
			return false;
		}
		AbstractAttribute a = (AbstractAttribute) o;
		return this.attributeDescription.equals(a.attributeDescription);
	}

	@Override
	public int hashCode() {
		return attributeDescription.hashCode();
	}

	@Override
	public void addTransformation(AttributeTransformation transformation) {
		this.transformations.add(transformation);
	}

	@Override
	public void clearTransformations() {
		this.transformations.clear();
	}

	@Override
	public AttributeTransformation getLastTransformation() {
		int size = this.transformations.size();
		if (size > 0) {
			return this.transformations.get(size - 1);
		} else {
			return null;
		}
	}

	@Override
	public double getValue(DataRow row) {
		double result = row.get(getTableIndex(), getDefault());
		if (!transformations.isEmpty()) {
			for (AttributeTransformation transformation : transformations) {
				result = transformation.transform(this, result);
			}
		}
		return result;
	}

	@Override
	public void setValue(DataRow row, double value) {
		double newValue = value;
		for (AttributeTransformation transformation : transformations) {
			if (transformation.isReversable()) {
				newValue = transformation.inverseTransform(this, newValue);
			} else {
				throw new RuntimeException(
						"Cannot set value for attribute using irreversible transformations. This process will probably work if you deactivate create_view in preprocessing operators.");
			}
		}
		row.set(getTableIndex(), newValue, getDefault());
	}

	/** Returns the name of the attribute. */
	@Override
	public String getName() {
		return this.attributeDescription.getName();
	}

	/** Sets the name of the attribute. */
	@Override
	public void setName(String v) {
		if (v.equals(this.attributeDescription.getName())) {
			return;
		}
		for (Attributes attributes : owners) {
			attributes.rename(this, v);
		}
		this.attributeDescription = (AttributeDescription) this.attributeDescription.clone();
		this.attributeDescription.setName(v);
	}

	/** Returns the index in the example table. */
	@Override
	public int getTableIndex() {
		return this.attributeDescription.getTableIndex();
	}

	/** Sets the index in the example table. */
	@Override
	public void setTableIndex(int i) {
		this.attributeDescription = (AttributeDescription) this.attributeDescription.clone();
		this.attributeDescription.setTableIndex(i);
	}

	// --- meta data of data ---

	/**
	 * Returns the block type of this attribute.
	 *
	 * @see com.rapidminer.tools.Ontology#ATTRIBUTE_BLOCK_TYPE
	 */
	@Override
	public int getBlockType() {
		return this.attributeDescription.getBlockType();
	}

	/**
	 * Sets the block type of this attribute.
	 *
	 * @see com.rapidminer.tools.Ontology#ATTRIBUTE_BLOCK_TYPE
	 */
	@Override
	public void setBlockType(int b) {
		this.attributeDescription = (AttributeDescription) this.attributeDescription.clone();
		this.attributeDescription.setBlockType(b);
	}

	/**
	 * Returns the value type of this attribute.
	 *
	 * @see com.rapidminer.tools.Ontology#ATTRIBUTE_VALUE_TYPE
	 */
	@Override
	public int getValueType() {
		return this.attributeDescription.getValueType();
	}

	/** Returns the attribute statistics. */
	@Override
	public Iterator<Statistics> getAllStatistics() {
		return this.statistics.iterator();
	}

	@Override
	public void registerStatistics(Statistics statistics) {
		this.statistics.add(statistics);
	}

	/**
	 * Returns the attribute statistics.
	 *
	 * @deprecated Please use the method {@link ExampleSet#getStatistics(Attribute, String)}
	 *             instead.
	 */
	@Override
	@Deprecated
	public double getStatistics(String name) {
		return getStatistics(name, null);
	}

	/**
	 * Returns the attribute statistics.
	 *
	 * @deprecated Please use the method {@link ExampleSet#getStatistics(Attribute, String)}
	 *             instead.
	 */
	@Override
	@Deprecated
	public double getStatistics(String name, String parameter) {
		for (Statistics statistics : this.statistics) {
			if (statistics.handleStatistics(name)) {
				return statistics.getStatistics(this, name, parameter);
			}
		}
		throw new RuntimeException("No statistics object was available for attribute statistics '" + name + "'!");
	}

	/** Returns the construction description. */
	@Override
	public String getConstruction() {
		return this.constructionDescription;
	}

	/** Returns the construction description. */
	@Override
	public void setConstruction(String description) {
		this.constructionDescription = description;
	}

	// ================================================================================
	// default value
	// ================================================================================

	@Override
	public void setDefault(double value) {
		this.attributeDescription = (AttributeDescription) this.attributeDescription.clone();
		this.attributeDescription.setDefault(value);
	}

	@Override
	public double getDefault() {
		return this.attributeDescription.getDefault();
	}

	// ================================================================================
	// string and result methods
	// ================================================================================

	/** Returns a human readable string that describes this attribute. */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("#");
		result.append(this.attributeDescription.getTableIndex());
		result.append(": ");
		result.append(this.attributeDescription.getName());
		result.append(" (");
		result.append(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(this.attributeDescription.getValueType()));
		result.append("/");
		result.append(Ontology.ATTRIBUTE_BLOCK_TYPE.mapIndex(this.attributeDescription.getBlockType()));
		result.append(")");
		return result.toString();
	}

	@Override
	public Annotations getAnnotations() {
		return annotations;
	}
}
