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
package com.rapidminer.operator.learner.associations.fpgrowth;

/**
 * A stack for frequencies.
 * 
 * @author Sebastian Land
 */
public interface FrequencyStack {

	/**
	 * Increases the frequency stored on stackHeight level of stack by value, if stackHeight is the
	 * top of stack, or stackHeight is top of stack + 1
	 * 
	 * @param stackHeight
	 *            describes the level of stack, counted from bottom on which the value is added
	 * @param value
	 *            is the amount added
	 */
	public void increaseFrequency(int stackHeight, int value);

	/**
	 * This method deletes the heightTH element of stack.
	 */
	public void popFrequency(int height);

	/**
	 * Returns the frequency stored on height of stack.
	 */
	public int getFrequency(int height);
}
