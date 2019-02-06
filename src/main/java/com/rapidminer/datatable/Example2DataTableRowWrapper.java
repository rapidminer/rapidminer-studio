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
package com.rapidminer.datatable;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;

import java.util.List;


/**
 * This class allows to use {@link com.rapidminer.example.Example}s as basis for
 * {@link com.rapidminer.datatable.DataTableRow}.
 * 
 * @author Ingo Mierswa
 */
public class Example2DataTableRowWrapper implements DataTableRow {

	private Example example;

	private List<Attribute> allAttributes;

	private Attribute idAttribute;

	/** Creates a new wrapper. If the Id Attribute is null, the DataTableRow will not contain an Id. */
	public Example2DataTableRowWrapper(Example example, List<Attribute> allAttributes, Attribute idAttribute) {
		this.example = example;
		this.allAttributes = allAttributes;
		this.idAttribute = idAttribute;
	}

	@Override
	public String getId() {
		if (idAttribute == null) {
			return null;
		} else {
			return this.example.getValueAsString(idAttribute);
		}
	}

	@Override
	public double getValue(int index) {
		return this.example.getValue(allAttributes.get(index));
	}

	@Override
	public int getNumberOfValues() {
		return allAttributes.size();
	}
}
