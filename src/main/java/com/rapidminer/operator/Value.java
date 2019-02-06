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
 * A value contains a key and a description. The current value can be asked by the process log
 * operator.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public abstract class Value implements ValueInterface {

	/** The key which can be asked by the process log operator. */
	private String key;

	/** The human readable description of this value. */
	private String description;

	/** Indicates if this value should be documented. */
	private boolean documented = true;

	/**
	 * Creates a new Value object with the given key as name and the given description. This value
	 * will be documented.
	 */
	public Value(String key, String description) {
		this(key, description, true);
	}

	/** Creates a new Value object. */
	public Value(String key, String description, boolean documented) {
		this.key = key;
		this.description = description;
		this.documented = documented;
	}

	/** Returns a human readable description. */
	@Override
	public String getDescription() {
		return description;
	}

	/** Returns the key. */
	@Override
	public String getKey() {
		return key;
	}

	/** Returns true if this value should be documented. */
	@Override
	public boolean isDocumented() {
		return documented;
	}
}
