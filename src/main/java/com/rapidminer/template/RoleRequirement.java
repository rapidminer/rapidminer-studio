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

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;

import java.util.Properties;
import java.util.logging.Level;


/**
 * A requirement to specify a role in the input data set.
 * 
 * Potential improvement once we add more tempaltes: Add data type limitations; add specification
 * how to guess what that attribute is; add specification of how to guess positive class
 * 
 * @author Simon Fischer
 */
public class RoleRequirement {

	private String roleName;
	private String humanName;
	private String description;
	private int valueType = Ontology.ATTRIBUTE_VALUE;

	/**
	 * Extracts all properties from the props object, where parameter keys start with
	 * "template.role_requirement."+index
	 */
	public RoleRequirement(Properties props, int index) {
		humanName = props.getProperty("template.role_requirement." + index + ".human_name");
		roleName = props.getProperty("template.role_requirement." + index + ".role_name");
		description = props.getProperty("template.role_requirement." + index + ".description");
		String valueTypeName = props.getProperty("template.role_requirement." + index + ".value_type");
		if (valueTypeName != null) {
			this.valueType = Ontology.ATTRIBUTE_VALUE_TYPE.mapName(valueTypeName);
			if (valueType == -1) {
				LogService.getRoot().log(Level.WARNING, "Illegal value type specified in template: " + valueTypeName);
				this.valueType = Ontology.ATTRIBUTE_VALUE;
			}
		}
	}

	public String getRoleName() {
		return roleName;
	}

	public String getHumanName() {
		return humanName;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return getHumanName() + " (" + getRoleName() + "): " + getDescription();
	}

	public int getValueType() {
		return valueType;
	}
}
