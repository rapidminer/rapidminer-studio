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
 * The super class for loggable String values.
 * 
 * @author Ingo Mierswa
 */
public abstract class ValueString extends Value {

	/**
	 * Creates a new Value object with the given key as name and the given description. This value
	 * will be documented.
	 */
	public ValueString(String key, String description) {
		super(key, description);
	}

	/** Creates a new Value object. */
	public ValueString(String key, String description, boolean documented) {
		super(key, description, documented);
	}

	/** Returns the double value which should be logged. */
	public abstract String getStringValue();

	@Override
	public final boolean isNominal() {
		return true;
	}

	@Override
	public final Object getValue() {
		return getStringValue();
	}
}
