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
package com.rapidminer.operator;

/**
 * The interface for values which can logged and plotted during process definitions. Operators
 * should define their values in their constructor.
 * 
 * @author Robert Rudolph, Ingo Mierswa
 */
public interface ValueInterface {

	/** Returns a human readable description. */
	public String getDescription();

	/** Returns the key. */
	public String getKey();

	/** Returns true if this value should be documented. */
	public boolean isDocumented();

	/**
	 * Returns the current value which can be logged by the process log operator.
	 */
	public Object getValue();

	/** Returns true if the value is nominal. */
	public boolean isNominal();

}
