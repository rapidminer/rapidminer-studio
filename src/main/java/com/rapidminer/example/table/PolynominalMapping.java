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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.Example;
import com.rapidminer.tools.Tools;


/**
 * This is an implementation of {@link NominalMapping} which can be used for nominal attributes with
 * an arbitrary number of different values.
 *
 * @author Ingo Mierswa
 */
public class PolynominalMapping implements NominalMapping {

	private static final long serialVersionUID = 5021638750496191771L;

	/** The map between symbolic values and their indices. */
	private final Map<String, Integer> symbolToIndexMap = new LinkedHashMap<>();

	/** The map between indices of nominal values and the actual nominal value. */
	private final List<String> indexToSymbolMap = new ArrayList<>();

	public PolynominalMapping() {}

	public PolynominalMapping(Map<Integer, String> map) {
		this.symbolToIndexMap.clear();
		this.indexToSymbolMap.clear();
		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			int index = entry.getKey();
			String value = entry.getValue();
			this.symbolToIndexMap.put(value, index);
			while (this.indexToSymbolMap.size() <= index) {
				this.indexToSymbolMap.add(null);
			}
			this.indexToSymbolMap.set(index, value);
		}
	}

	/* pp */ PolynominalMapping(NominalMapping mapping) {
		this.symbolToIndexMap.clear();
		this.indexToSymbolMap.clear();
		for (int i = 0; i < mapping.size(); i++) {
			int index = i;
			String value = mapping.mapIndex(index);
			this.symbolToIndexMap.put(value, index);
			this.indexToSymbolMap.add(value);
		}
	}

	@Override
	public Object clone() {
		return new PolynominalMapping(this);
	}

	@Override
	public boolean equals(NominalMapping mapping) {
		if (mapping.size() != size()) {
			return false;
		}
		for (String value : mapping.getValues()) {
			if (!symbolToIndexMap.containsKey(value)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the index for the nominal attribute value <code>str</code>. If the string is unknown,
	 * a new index value is assigned. Returns -1, if str is null.
	 */
	@Override
	public int mapString(String str) {
		if (str == null) {
			return -1;
		}
		// lookup string in hashtable
		int index = getIndex(str);
		// if string is not yet in the map, add it
		if (index < 0) {
			indexToSymbolMap.add(str);
			index = indexToSymbolMap.size() - 1;
			symbolToIndexMap.put(str, index);
		}
		return index;
	}

	/**
	 * Returns the index of the given nominal value or -1 if this value was not mapped before by
	 * invoking the method {@link #mapIndex(int)}.
	 */
	@Override
	public int getIndex(String str) {
		Integer index = symbolToIndexMap.get(str);
		if (index == null) {
			return -1;
		} else {
			return index.intValue();
		}
	}

	/**
	 * Returns the attribute value, that is associated with this index. Index counting starts with
	 * 0. <b>WARNING:</b> In order to iterate over all values please use the collection returned by
	 * {@link #getValues()}.
	 */
	@Override
	public String mapIndex(int index) {
		if (index < 0 || index >= indexToSymbolMap.size()) {
			throw new AttributeTypeException(
					"Cannot map index of nominal attribute to nominal value: index " + index + " is out of bounds!");
		}
		return indexToSymbolMap.get(index);
	}

	/**
	 * Sets the given mapping. Please note that this will overwrite existing mappings and might
	 * cause data changes in this way.
	 */
	@Override
	public void setMapping(String nominalValue, int index) {
		String oldValue = indexToSymbolMap.get(index);
		indexToSymbolMap.set(index, nominalValue);
		symbolToIndexMap.remove(oldValue);
		symbolToIndexMap.put(nominalValue, index);
	}

	/**
	 * Returns the index of the first value if this attribute is a classification attribute, i.e. if
	 * it is binominal.
	 */
	@Override
	public int getNegativeIndex() {
		ensureClassification();
		if (mapIndex(0) == null) {
			throw new AttributeTypeException("Attribute: Cannot use FIRST_CLASS_INDEX for negative class!");
		}
		return 0;
	}

	/**
	 * Returns the index of the second value if this attribute is a classification attribute. Works
	 * for all binominal attributes.
	 */
	@Override
	public int getPositiveIndex() {
		ensureClassification();
		if (mapIndex(0) == null) {
			throw new AttributeTypeException("Attribute: Cannot use FIRST_CLASS_INDEX for negative class!");
		}
		Iterator<Integer> i = symbolToIndexMap.values().iterator();
		while (i.hasNext()) {
			int index = i.next();
			if (index != 0) {
				return index;
			}
		}
		throw new AttributeTypeException("Attribute: No other class than FIRST_CLASS_INDEX found!");
	}

	@Override
	public String getNegativeString() {
		return mapIndex(getNegativeIndex());
	}

	@Override
	public String getPositiveString() {
		return mapIndex(getPositiveIndex());
	}

	/** Returns the values of the attribute as an enumeration of strings. */
	@Override
	public List<String> getValues() {
		return indexToSymbolMap;
	}

	/** Returns the number of different nominal values. */
	@Override
	public int size() {
		return indexToSymbolMap.size();
	}

	/**
	 * This method rearranges the string to number mappings such that they are in alphabetical
	 * order. <br>
	 * <b>VERY IMPORTANT NOTE:</b> Do not call this method when this attribute is already associated
	 * with an {@link ExampleTable} and it already contains {@link Example}s. All examples will be
	 * messed up since the indices will not be replaced in the data table.
	 */
	@Override
	public void sortMappings() {
		List<String> allStrings = new LinkedList<>(symbolToIndexMap.keySet());
		Collections.sort(allStrings);
		symbolToIndexMap.clear();
		indexToSymbolMap.clear();
		Iterator<String> i = allStrings.iterator();
		while (i.hasNext()) {
			mapString(i.next());
		}
	}

	/** Clears all mappings for nominal values. */
	@Override
	public void clear() {
		symbolToIndexMap.clear();
		indexToSymbolMap.clear();
	}

	/**
	 * Throws a runtime exception if this attribute is not a classification attribute.
	 */
	private void ensureClassification() {
		if (size() != 2) {
			throw new AttributeTypeException("Attribute " + this.toString() + " is not a classification attribute!");
		}
	}

	@Override
	public String toString() {
		return indexToSymbolMap.toString() + Tools.getLineSeparator() + symbolToIndexMap.toString();
	}
}
