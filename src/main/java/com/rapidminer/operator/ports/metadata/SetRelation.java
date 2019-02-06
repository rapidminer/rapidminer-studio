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
package com.rapidminer.operator.ports.metadata;

/**
 * Can be used to indicate whether we know the attribute set exactly, or only know that it is a
 * subset/superset of a given set.
 * 
 * @author Simon Fischer
 * */
public enum SetRelation {
	UNKNOWN("unknown"), SUPERSET("\u2287"), SUBSET("\u2286"), EQUAL("=");

	private String description;

	private SetRelation(String description) {
		this.description = description;
	}

	public SetRelation merge(SetRelation relation) {
		switch (relation) {
			case EQUAL:
				return this;
			case SUBSET:
				switch (this) {
					case SUBSET:
					case EQUAL:
						return SUBSET;
					case SUPERSET:
					case UNKNOWN:
						return UNKNOWN;
				}
			case SUPERSET:
				switch (this) {
					case SUPERSET:
					case EQUAL:
						return SUPERSET;
					case SUBSET:
					case UNKNOWN:
						return UNKNOWN;
				}
			case UNKNOWN:
			default:
				return UNKNOWN;
		}
	}

	@Override
	public String toString() {
		return description;
	}
}
