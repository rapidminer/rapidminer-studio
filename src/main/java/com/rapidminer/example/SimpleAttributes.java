/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;


/**
 * A very basic and simple implementation of the {@link com.rapidminer.example.Attributes} interface
 * based on a linked list of {@link com.rapidminer.example.AttributeRole}s and simply delivers the
 * same {@link com.rapidminer.example.AttributeRoleIterator} which might skip either regular or
 * special attributes.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class SimpleAttributes extends AbstractAttributes {

	private static final long serialVersionUID = 6388263725741578818L;

	private List<AttributeRole> attributes = new LinkedList<>();

	private transient Map<String, AttributeRole> nameToAttributeRoleMap = new HashMap<>();
	private transient Map<String, AttributeRole> specialNameToAttributeRoleMap = new HashMap<>();

	public SimpleAttributes() {}

	private SimpleAttributes(SimpleAttributes attributes) {
		for (AttributeRole role : attributes.attributes) {
			register((AttributeRole) role.clone(), false);
		}
	}

	public Object readResolve() {
		if (nameToAttributeRoleMap == null) {
			// in earlier versions we didn't have this map, so set it up here. anyway, the maps are
			// transient.
			nameToAttributeRoleMap = new HashMap<>();
			specialNameToAttributeRoleMap = new HashMap<>();
		}
		for (AttributeRole attributeRole : attributes) {
			register(attributeRole, true);
		}
		return this;
	}

	@Override
	public Object clone() {
		return new SimpleAttributes(this);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SimpleAttributes)) {
			return false;
		}
		SimpleAttributes other = (SimpleAttributes) o;
		return attributes.equals(other.attributes);
	}

	@Override
	public int hashCode() {
		return attributes.hashCode();
	}

	@Override
	public Iterator<AttributeRole> allAttributeRoles() {
		final Iterator<AttributeRole> i = attributes.iterator();
		return new Iterator<AttributeRole>() {

			private AttributeRole current;

			@Override
			public boolean hasNext() {
				return i.hasNext();
			}

			@Override
			public AttributeRole next() {
				current = i.next();
				return current;
			}

			@Override
			public void remove() {
				i.remove();
				unregister(current, true);
			}
		};
	}

	/**
	 * @param onlyMaps
	 *            add only to maps, not to list. Useful for {@link #readResolve}
	 */
	private void register(AttributeRole attributeRole, boolean onlyMaps) {
		String name = attributeRole.getAttribute().getName();
		if (nameToAttributeRoleMap.containsKey(name)) {
			throw new IllegalArgumentException("Duplicate attribute name: " + name);
		}
		String specialName = attributeRole.getSpecialName();
		if (specialName != null) {
			if (specialNameToAttributeRoleMap.containsKey(specialName)) {
				throw new IllegalArgumentException("Duplicate attribute role: " + specialName);
			}
		}
		this.nameToAttributeRoleMap.put(name, attributeRole);
		if (specialName != null) {
			this.specialNameToAttributeRoleMap.put(specialName, attributeRole);
		}
		if (!onlyMaps) {
			this.attributes.add(attributeRole);
		}
		attributeRole.addOwner(this);
		attributeRole.getAttribute().addOwner(this);
	}

	/**
	 * 
	 * @param onlyMap
	 *            if true, removes the attribute only from the maps, but not from the list. this is
	 *            useful for the iterator, which has already removed the attribute from the list.
	 */
	private boolean unregister(AttributeRole attributeRole, boolean onlyMap) {
		if (!nameToAttributeRoleMap.containsKey(attributeRole.getAttribute().getName())) {
			return false;
		}
		this.nameToAttributeRoleMap.remove(attributeRole.getAttribute().getName());
		if (attributeRole.getSpecialName() != null) {
			this.specialNameToAttributeRoleMap.remove(attributeRole.getSpecialName());
		}
		if (!onlyMap) {
			this.attributes.remove(attributeRole);
		}
		attributeRole.removeOwner(this);
		attributeRole.getAttribute().removeOwner(this);
		return true;
	}

	@Override
	public void rename(AttributeRole attributeRole, String newSpecialName) {
		if (attributeRole.getSpecialName() != null) {
			AttributeRole role = specialNameToAttributeRoleMap.get(attributeRole.getSpecialName());
			if (role == null) {
				throw new NoSuchElementException("Cannot rename attribute role. No such attribute role: "
						+ attributeRole.getSpecialName());
			}
			if (role != attributeRole) {
				throw new RuntimeException("Broken attribute role map.");
			}
		}
		specialNameToAttributeRoleMap.remove(attributeRole.getSpecialName());
		if (newSpecialName != null) {
			specialNameToAttributeRoleMap.put(newSpecialName, attributeRole);
		}

	}

	@Override
	public void rename(Attribute attribute, String newName) {
		if (nameToAttributeRoleMap.containsKey(newName)) {
			throw new IllegalArgumentException("Cannot rename attribute. Duplicate name: " + newName);
		}
		AttributeRole role = nameToAttributeRoleMap.get(attribute.getName());
		if (role == null) {
			throw new NoSuchElementException("Cannot rename attribute. No such attribute: " + attribute.getName());
		}
		if (role.getAttribute() != attribute) {
			// this cannot happen
			throw new RuntimeException("Broken attribute map.");
		}
		nameToAttributeRoleMap.remove(role.getAttribute().getName());
		nameToAttributeRoleMap.put(newName, role);
	}

	@Override
	public void add(AttributeRole attributeRole) {
		register(attributeRole, false);
	}

	@Override
	public boolean remove(AttributeRole attributeRole) {
		return unregister(attributeRole, false);
	}

	@Override
	public AttributeRole findRoleByName(String name, boolean caseSensitive) {
		if (caseSensitive) {
			return nameToAttributeRoleMap.get(name);
		} else {
			String lowerSearchTerm = name.toLowerCase();
			for (Entry<String, AttributeRole> entry : nameToAttributeRoleMap.entrySet()) {
				if (lowerSearchTerm.equals(entry.getKey().toLowerCase())) {
					return entry.getValue();
				}
			}
			return null;
		}
	}

	@Override
	public AttributeRole findRoleBySpecialName(String specialName, boolean caseSensitive) {
		if (caseSensitive) {
			return specialNameToAttributeRoleMap.get(specialName);
		} else {
			String lowerSearchTerm = specialName.toLowerCase();
			for (Entry<String, AttributeRole> entry : specialNameToAttributeRoleMap.entrySet()) {
				if (lowerSearchTerm.equals(entry.getKey().toLowerCase())) {
					return entry.getValue();
				}
			}
			return null;
		}
	}

	@Override
	public int size() {
		return nameToAttributeRoleMap.size() - specialNameToAttributeRoleMap.size();
	}

	@Override
	public int allSize() {
		return nameToAttributeRoleMap.size();
	}

	@Override
	public int specialSize() {
		return specialNameToAttributeRoleMap.size();
	}
}
