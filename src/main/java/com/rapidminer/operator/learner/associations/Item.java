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
package com.rapidminer.operator.learner.associations;

import java.io.Serializable;


/**
 * Item the base class for itemsets and provide all necessary frequency information.
 * 
 * @author Sebastian Land
 */
public interface Item extends Comparable<Item>, Serializable {

	/**
	 * This method returns the frequency of this item
	 * 
	 * @return the frequency of this item
	 */
	public int getFrequency();

	/**
	 * This method adds one to the frequency of this item
	 */
	public void increaseFrequency();

	/**
	 * This method increases the frequency of this item by value
	 * 
	 * @param value
	 *            is added to the frequency
	 */
	public void increaseFrequency(int value);

	/**
	 * This method returns a human readable String representation of this item.
	 * 
	 * @return the representing string
	 */
	@Override
	public String toString();

}
