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
package com.rapidminer.example;

import java.io.Serializable;
import java.util.Iterator;


/**
 * This container holds all information about the example set attributes. Implementors might want to
 * override {@link com.rapidminer.example.AbstractAttributes} for which only one of the iterating
 * methods must be overridden.
 *
 * @author Ingo Mierswa
 */
public interface Attributes extends Iterable<Attribute>, Cloneable, Serializable {

	/** Indicates regular attributes. */
	public static final int REGULAR = 0;

	/** Indicates special attributes. */
	public static final int SPECIAL = 1;

	/** Indicates all attributes. */
	public static final int ALL = 2;

	/** The name of the confidence special attributes. */
	public static final String CONFIDENCE_NAME = "confidence";

	/** The name of regular attributes. */
	public static final String ATTRIBUTE_NAME = "attribute";

	/** The name of the special attribute id. */
	public static final String ID_NAME = "id";

	/** The name of the special attribute label. */
	public static final String LABEL_NAME = "label";

	/** The name of the special attribute prediction. */
	public static final String PREDICTION_NAME = "prediction";

	/** The name of the special attribute cluster. */
	public static final String CLUSTER_NAME = "cluster";

	/** The name of the special attribute weight (example weights). */
	public static final String WEIGHT_NAME = "weight";

	/** The name of the special attribute batch. */
	public static final String BATCH_NAME = "batch";

	/** The name of the special attribute outlier. */
	public static final String OUTLIER_NAME = "outlier";

	/** The name of the classification cost special attribute. */
	public static final String CLASSIFICATION_COST = "cost";

	/** The name of the classification cost special attribute. */
	public static final String BASE_VALUE = "base_value";

	/** All known names of regular and special attribute types as an array. */
	public static final String[] KNOWN_ATTRIBUTE_TYPES = new String[] { ATTRIBUTE_NAME, LABEL_NAME, ID_NAME, WEIGHT_NAME,
		BATCH_NAME, CLUSTER_NAME, PREDICTION_NAME, OUTLIER_NAME, CLASSIFICATION_COST, BASE_VALUE };

	/** Indicates a regular attribute type. */
	public static final int TYPE_ATTRIBUTE = 0;

	/** Indicates the special attribute type label. */
	public static final int TYPE_LABEL = 1;

	/** Indicates the special attribute type id. */
	public static final int TYPE_ID = 2;

	/** Indicates the special attribute type weight (example weights). */
	public static final int TYPE_WEIGHT = 3;

	/** Indicates the special attribute type batch (example batches). */
	public static final int TYPE_BATCH = 4;

	/** Indicates the special attribute type cluster. */
	public static final int TYPE_CLUSTER = 5;

	/** Indicates the special attribute type prediction. */
	public static final int TYPE_PREDICTION = 6;

	/** Indicates the special attribute type outlier. */
	public static final int TYPE_OUTLIER = 7;

	/** Indicates the special attribute type cost. */
	public static final int TYPE_COST = 8;

	/** Indicates the special attribute type base_value. */
	public static final int TYPE_BASE_VALUE = 9;

	/** Returns a clone of this attribute set. */
	public Object clone();

	/** Returns true if the given object is equal to this attribute set. */
	@Override
	public boolean equals(Object o);

	/** Returns the hash code of this attribute set. */
	@Override
	public int hashCode();

	/** Iterates over all regular attributes. */
	@Override
	public Iterator<Attribute> iterator();

	/** Returns an iterator over all attributes, including the special attributes. */
	public Iterator<Attribute> allAttributes();

	/** Returns an iterator over all attribute roles, including the special attribute roles. */
	public Iterator<AttributeRole> allAttributeRoles();

	/** Returns an iterator over the attribute roles of the special attributes. */
	public Iterator<AttributeRole> specialAttributes();

	/** Returns an iterator over the attribute roles of the regular attributes. */
	public Iterator<AttributeRole> regularAttributes();

	/** Returns true if this attribute set contains the given attribute. */
	public boolean contains(Attribute attribute);

	/** Returns the number of regular attributes. */
	public int size();

	/** Returns the number of special attributes. */
	public int specialSize();

	/** Returns the number of all attributes, i.e. of the regular and the special attributes. */
	public int allSize();

	/** Adds a new attribute role. */
	public void add(AttributeRole attributeRole);

	/** Adds the given attribute as regular attribute. */
	public void addRegular(Attribute attribute);

	/** Removes the given attribute role. */
	public boolean remove(AttributeRole attributeRole);

	/** Removes the given attribute. */
	public boolean remove(Attribute attribute);

	/** Removes all regular attributes. */
	public void clearRegular();

	/** Removes all special attributes. */
	public void clearSpecial();

	/** Replaces the first attribute by the second. Returns the second attribute. */
	public Attribute replace(Attribute first, Attribute second);

	/** Returns the attribute for the given name. The search is case sensitive. */
	public Attribute get(String name);

	/**
	 * Returns the attribute for the given name. If the search is performed case sensitive depends
	 * on the boolean parameter. Please keep in mind that case insensitive search is not optimized
	 * and will take linear time to number of attributes.
	 * */
	public Attribute get(String name, boolean caseSensitive);

	/** Returns the regular attribute for the given name. */
	public Attribute getRegular(String name);

	/** Returns the special attribute for the given special name. */
	public Attribute getSpecial(String name);

	/** Returns the attribute role for the given attribute. */
	public AttributeRole getRole(Attribute attribute);

	/** Returns the attribute role for the given name. */
	public AttributeRole getRole(String name);

	/** Returns the label attribute or null if no label attribute is defined. */
	public Attribute getLabel();

	/** Sets the label attribute. If the given attribute is null, no label attribute will be used. */
	public void setLabel(Attribute label);

	/** Returns the predicted label attribute or null if no label attribute is defined. */
	public Attribute getPredictedLabel();

	/**
	 * This method will return the confidence attribute of the given class or null if no confidence
	 * attribute exists for this class
	 */
	public Attribute getConfidence(String classLabel);

	/**
	 * Sets the predicted label attribute. If the given attribute is null, no predicted label
	 * attribute will be used.
	 */
	public void setPredictedLabel(Attribute predictedLabel);

	/** Returns the id attribute or null if no label attribute is defined. */
	public Attribute getId();

	/** Sets the id attribute. If the given attribute is null, no id attribute will be used. */
	public void setId(Attribute id);

	/** Returns the weight attribute or null if no label attribute is defined. */
	public Attribute getWeight();

	/** Sets the weight attribute. If the given attribute is null, no weight attribute will be used. */
	public void setWeight(Attribute weight);

	/** Returns the cluster attribute or null if no label attribute is defined. */
	public Attribute getCluster();

	/**
	 * Sets the cluster attribute. If the given attribute is null, no cluster attribute will be
	 * used.
	 */
	public void setCluster(Attribute cluster);

	/** Returns the outlier attribute or null if no label attribute is defined. */
	public Attribute getOutlier();

	/**
	 * Sets the outlier attribute. If the given attribute is null, no outlier attribute will be
	 * used.
	 */
	public void setOutlier(Attribute outlier);

	/** Returns the cost attribute or null if no label attribute is defined. */
	public Attribute getCost();

	/** Sets the cost attribute. If the given attribute is null, no cost attribute will be used. */
	public void setCost(Attribute cost);

	/**
	 * Sets the special attribute for the given name. If the given attribute is null, no special
	 * attribute with this name will be used.
	 */
	public void setSpecialAttribute(Attribute attribute, String specialName);

	/**
	 * This method creates an attribute array from all regular attributes. ATTENTION: This method
	 * should only be used if it is ensured that the attribute roles and number of attributes cannot
	 * be changed, otherwise the delivered array will not be synchronized with the attribute roles
	 * object. Therefore, the iterator methods of this class should be preferred.
	 */
	public Attribute[] createRegularAttributeArray();

	/** Returns a string representation of this attribute set. */
	@Override
	public String toString();

	/**
	 * Finds the {@link AttributeRole} belonging to the attribute with the given name (both regular
	 * and special). Search is performed case sensitive.
	 */
	public AttributeRole findRoleByName(String name);

	/**
	 * Finds the {@link AttributeRole} belonging to the attribute with the given name (both regular
	 * and special). Whether the search is performed case sensitive depends on the boolean parameter.
	 * <p>
	 * <strong>Attention</strong>: Case insensitive search is not optimized and takes linear time with number of attributes.
	 */
	public AttributeRole findRoleByName(String name, boolean caseSensitive);

	/** Finds the {@link AttributeRole} with the given special name (both regular and special). */
	public AttributeRole findRoleBySpecialName(String specialName);

	/**
	 * Finds the {@link AttributeRole} with the given special name (both regular and special). Whether
	 * the search is performed case sensitive depends on the boolean parameter.
	 * <p>
	 * <strong>Attention</strong>: Case insensitive search is not optimized and takes linear time with number of attributes.
	 */
	public AttributeRole findRoleBySpecialName(String specialName, boolean caseSensitive);

	/** @see #rename(Attribute, String) */
	public void rename(AttributeRole attributeRole, String newSpecialName);

	/**
	 * Notifies the Attributes that this attribute will rename itself to the given name immediately
	 * after this method returns.
	 */
	public void rename(Attribute attribute, String newName);

}
