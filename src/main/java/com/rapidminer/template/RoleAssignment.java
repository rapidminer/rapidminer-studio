/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.template;

/**
 * Assignment to an attribute to a {@link RoleRequirement} and selection of a positive class.
 * 
 * @author Simon Fischer
 */
public class RoleAssignment {

	private String attributeName;
	private String positiveClass;

	/**
	 * @param attributeName
	 *            cannot be null
	 */
	public RoleAssignment(String attributeName, String positiveClass) {
		super();
		if (attributeName == null) {
			throw new NullPointerException("Cannot assign null attribute");
		}
		this.attributeName = attributeName;
		this.positiveClass = positiveClass;
	}

	/** Cannot be null. Otherwise, whole assignment should be null. */
	public String getAttributeName() {
		return attributeName;
	}

	public String getPositiveClass() {
		return positiveClass;
	}

}
