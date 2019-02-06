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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


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

	private final List<AttributeRole> attributes;

	private transient Map<String, AttributeRole> nameToAttributeRoleMap = new HashMap<>();
	private transient Map<String, AttributeRole> specialNameToAttributeRoleMap = new HashMap<>();

	public SimpleAttributes() {
		this.attributes = Collections.synchronizedList(new ArrayList<AttributeRole>());
	}

	private SimpleAttributes(SimpleAttributes attributes) {
		this.attributes = Collections.synchronizedList(new ArrayList<AttributeRole>(attributes.allSize()));
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
			throw new DuplicateAttributeException(name);
		}
		String specialName = attributeRole.getSpecialName();
		if (specialName != null && specialNameToAttributeRoleMap.containsKey(specialName)) {
			throw new DuplicateAttributeException(name, true);
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
				throw new NoSuchAttributeException(attributeRole.getSpecialName(), true);
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
			throw new DuplicateAttributeException(newName);
		}
		AttributeRole role = nameToAttributeRoleMap.get(attribute.getName());
		if (role == null) {
			throw new NoSuchAttributeException(attribute.getName());
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
		return findRole(name, caseSensitive, nameToAttributeRoleMap);
	}

	@Override
	public AttributeRole findRoleBySpecialName(String specialName, boolean caseSensitive) {
		return findRole(specialName, caseSensitive, specialNameToAttributeRoleMap);
	}

	/**
	 * Finds the {@link AttributeRole} with the given key. The key will either be an attribute name or attribute role name
	 * (both regular and special). The key will be searched in the specified {@code roleMap} which should either be
	 * {@link #nameToAttributeRoleMap} or {@link #specialNameToAttributeRoleMap}. Whether the search is performed case
	 * sensitive depends on the boolean parameter.
	 * <p>
	 * <strong>Attention</strong>: Case insensitive search is not optimized and takes linear time with number of attributes.
	 *
	 * @param key
	 * 		the key to search for
	 * @param caseSensitive
	 * 		whether the search should be case sensitive
	 * @param roleMap
	 * 		the map to search in
	 * @return the attribute role or {@code null} if none was found
	 * @since 8.2
	 */
	private static AttributeRole findRole(String key, boolean caseSensitive, Map<String, AttributeRole> roleMap) {
		if (caseSensitive || key == null) {
			return roleMap.get(key);
		} else {
			for (Entry<String, AttributeRole> entry : roleMap.entrySet()) {
				if (key.equalsIgnoreCase(entry.getKey())) {
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
