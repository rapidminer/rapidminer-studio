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

import com.rapidminer.example.Example;

import java.io.Serializable;
import java.util.List;


/**
 * This class is used to map between the nominal values for a certain attribute and the internal
 * double value representations which is used for nominal values in order to reduce memory usage.
 * 
 * @author Ingo Mierswa
 */
public interface NominalMapping extends Cloneable, Serializable {

	/**
	 * This should return true if all the mappings contain the same values regardless of their
	 * internal order.
	 */
	public boolean equals(NominalMapping mapping);

	/** Should return a deep clone of this nominal mapping. */
	public Object clone();

	/**
	 * Returns the index of a positive class (if available). Returns -1 otherwise.
	 */
	public int getPositiveIndex();

	/**
	 * Returns the nominal value of a positive class (if available). Returns null otherwise.
	 */
	public String getPositiveString();

	/**
	 * Returns the index of a negative class (if available). Returns -1 otherwise.
	 */
	public int getNegativeIndex();

	/**
	 * Returns the nominal value of a negative class (if available). Returns null otherwise.
	 */
	public String getNegativeString();

	/**
	 * Returns the internal double representation (actually an integer index) for the given nominal
	 * value without creating a mapping if none exists.
	 * 
	 * @return the integer of the index or -1 if no mapping for this value exists
	 */
	public int getIndex(String nominalValue);

	/**
	 * Returns the internal double representation (actually an integer index) for the given nominal
	 * value. This method creates a mapping if it did not exist before.
	 */
	public int mapString(String nominalValue);

	/**
	 * Returns the nominal value for an internal double representation (actually an integer index).
	 * This method only works for nominal values which were formerly mapped via
	 * {@link #mapString(String)}.
	 */
	public String mapIndex(int index);

	/**
	 * Sets the given mapping. This might be practical for example for replacing a nominal value
	 * (without a data scan!).
	 */
	public void setMapping(String nominalValue, int index);

	/**
	 * Returns an unmodifiable list of all nominal values which were mapped via {@link #mapString(String)} until
	 * now.
	 */
	public List<String> getValues();

	/**
	 * Returns the number of different nominal values which were mapped via
	 * {@link #mapString(String)} until now.
	 */
	public int size();

	/**
	 * This method rearranges the string to number mappings such that they are in alphabetical
	 * order. <br>
	 * <b>VERY IMPORTANT NOTE:</b> Do not call this method when this attribute is already associated
	 * with an {@link AbstractExampleTable} and it already contains {@link Example}s. All examples
	 * will be messed up otherwise!
	 */
	public void sortMappings();

	/** Clears the mapping. */
	public void clear();

}
