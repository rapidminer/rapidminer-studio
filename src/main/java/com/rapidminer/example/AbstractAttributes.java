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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.tools.LogService;


/**
 * This is the abstract superclass for all attribute set implementations. It is sufficient for
 * subclasses to overwrite the method {@link Attributes#allAttributeRoles()} and the corresponding
 * add and remove methods.
 *
 * @author Ingo Mierswa
 */
public abstract class AbstractAttributes implements Attributes {

	private static final long serialVersionUID = -3419958538074776957L;

	@Override
	public abstract Object clone();

	@Override
	public Iterator<Attribute> iterator() {
		return new RegularAttributeIterator(allAttributeRoles());
	}

	@Override
	public Iterator<Attribute> allAttributes() {
		return new AttributeIterator(allAttributeRoles(), ALL);
	}

	@Override
	public Iterator<AttributeRole> specialAttributes() {
		return new AttributeRoleIterator(allAttributeRoles(), SPECIAL);
	}

	@Override
	public Iterator<AttributeRole> regularAttributes() {
		return new AttributeRoleIterator(allAttributeRoles(), REGULAR);
	}

	@Override
	public boolean contains(Attribute attribute) {
		return findAttributeRole(attribute.getName()) != null;
	}

	@Override
	public int allSize() {
		return calculateSize(allAttributes());
	}

	@Override
	public int size() {
		return calculateSize(iterator());
	}

	@Override
	public int specialSize() {
		return calculateSize(specialAttributes());
	}

	private int calculateSize(Iterator<?> i) {
		int counter = 0;
		while (i.hasNext()) {
			i.next();
			counter++;
		}
		return counter;
	}

	@Override
	public void addRegular(Attribute attribute) {
		add(new AttributeRole(attribute));
	}

	@Override
	public boolean remove(Attribute attribute) {
		AttributeRole role = getRole(attribute);
		if (role != null) {
			return remove(role);
		} else {
			return false;
		}
	}

	@Override
	public void clearRegular() {
		List<AttributeRole> toRemove = new LinkedList<AttributeRole>();
		Iterator<AttributeRole> i = allAttributeRoles();
		while (i.hasNext()) {
			AttributeRole role = i.next();
			if (!role.isSpecial()) {
				toRemove.add(role);
			}
		}

		for (AttributeRole role : toRemove) {
			remove(role);
		}
	}

	@Override
	public void clearSpecial() {
		List<AttributeRole> toRemove = new LinkedList<AttributeRole>();
		Iterator<AttributeRole> i = allAttributeRoles();
		while (i.hasNext()) {
			AttributeRole role = i.next();
			if (role.isSpecial()) {
				toRemove.add(role);
			}
		}

		for (AttributeRole role : toRemove) {
			remove(role);
		}
	}

	@Override
	public Attribute replace(Attribute first, Attribute second) {
		AttributeRole role = getRole(first);
		if (role != null) {
			role.setAttribute(second);
		} else {
			throw new java.util.NoSuchElementException("Attribute " + first + " cannot be replaced by attribute " + second
					+ ": " + first + " is not part of the example set!");
		}
		return second;
	}

	@Override
	public Attribute get(String name) {
		return get(name, true);
	}

	@Override
	public Attribute get(String name, boolean caseSensitive) {
		AttributeRole result = findRoleByName(name, caseSensitive);
		if (result == null) {
			result = findRoleBySpecialName(name, caseSensitive);
		}
		if (result != null) {
			return result.getAttribute();
		} else {
			return null;
		}
	}

	@Override
	public AttributeRole findRoleByName(String name) {
		return findRoleByName(name, true);
	}

	@Override
	public AttributeRole findRoleBySpecialName(String specialName) {
		return findRoleBySpecialName(specialName, true);
	}

	@Override
	public Attribute getRegular(String name) {
		AttributeRole role = findRoleByName(name);
		if (role != null) {
			if (!role.isSpecial()) {
				return role.getAttribute();
			} else {
				// LogService.getGlobal().logWarning("No regular attribute with name '"+name+"'
				// found, however, there is a special attribute with the same name.");
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.example.AbstractAttributes.no_regular_attribute_found", name);
				return null;
			}
		} else {
			return null;
		}
		// return findAttribute(name, iterator());
	}

	@Override
	public Attribute getSpecial(String name) {
		AttributeRole role = findRoleBySpecialName(name);
		if (role == null) {
			return null;
		} else {
			return role.getAttribute();
		}
	}

	@Override
	public AttributeRole getRole(Attribute attribute) {
		return getRole(attribute.getName());
	}

	@Override
	public AttributeRole getRole(String name) {
		return findAttributeRole(name);
	}

	@Override
	public Attribute getLabel() {
		return getSpecial(LABEL_NAME);
	}

	@Override
	public void setLabel(Attribute label) {
		setSpecialAttribute(label, LABEL_NAME);
	}

	@Override
	public Attribute getPredictedLabel() {
		return getSpecial(PREDICTION_NAME);
	}

	@Override
	public Attribute getConfidence(String classLabel) {
		return getSpecial(CONFIDENCE_NAME + "_" + classLabel);
	}

	@Override
	public void setPredictedLabel(Attribute predictedLabel) {
		setSpecialAttribute(predictedLabel, PREDICTION_NAME);
	}

	@Override
	public Attribute getId() {
		return getSpecial(ID_NAME);
	}

	@Override
	public void setId(Attribute id) {
		setSpecialAttribute(id, ID_NAME);
	}

	@Override
	public Attribute getWeight() {
		return getSpecial(WEIGHT_NAME);
	}

	@Override
	public void setWeight(Attribute weight) {
		setSpecialAttribute(weight, WEIGHT_NAME);
	}

	@Override
	public Attribute getCluster() {
		return getSpecial(CLUSTER_NAME);
	}

	@Override
	public void setCluster(Attribute cluster) {
		setSpecialAttribute(cluster, CLUSTER_NAME);
	}

	@Override
	public Attribute getOutlier() {
		return getSpecial(OUTLIER_NAME);
	}

	@Override
	public void setOutlier(Attribute outlier) {
		setSpecialAttribute(outlier, OUTLIER_NAME);
	}

	@Override
	public Attribute getCost() {
		return getSpecial(CLASSIFICATION_COST);
	}

	@Override
	public void setCost(Attribute cost) {
		setSpecialAttribute(cost, CLASSIFICATION_COST);
	}

	@Override
	public void setSpecialAttribute(Attribute attribute, String specialName) {
		AttributeRole oldRole = findRoleBySpecialName(specialName);
		if (oldRole != null) {
			remove(oldRole);
		}
		if (attribute != null) {
			remove(attribute);
			AttributeRole role = new AttributeRole(attribute);
			role.setSpecial(specialName);
			;
			add(role);
		}
	}

	@Override
	public Attribute[] createRegularAttributeArray() {
		int index = 0;
		Attribute[] result = new Attribute[size()];
		for (Attribute attribute : this) {
			result[index++] = attribute;
		}
		return result;
	}

	/** Returns a string representation of this attribute set. */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(getClass().getSimpleName() + ": ");
		Iterator<AttributeRole> r = allAttributeRoles();
		boolean first = true;
		while (r.hasNext()) {
			if (!first) {
				result.append(", ");
			}
			result.append(r.next());
			first = false;
		}
		return result.toString();
	}

	private AttributeRole findAttributeRole(String name) {
		AttributeRole role = findRoleByName(name);
		if (role != null) {
			return role;
		} else {
			return findRoleBySpecialName(name);
		}
	}
}
