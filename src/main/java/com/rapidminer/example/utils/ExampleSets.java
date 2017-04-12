/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.example.utils;

import java.util.List;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.GrowingExampleTable;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.table.internal.ColumnarExampleTable;
import com.rapidminer.tools.ParameterService;


/**
 * This class consists exclusively of static methods that help to build new {@link ExampleSet}s.
 *
 * @author Gisa Schaefer
 * @since 7.3
 */
public final class ExampleSets {

	private ExampleSets() {}

	/**
	 * Creates a builder for an {@link ExampleSet} starting from the given attributes. If the given
	 * attributes are {@code null}, the example will have no attributes.
	 *
	 * @param attributes
	 *            the attributes for the new {@link ExampleSet}, can be {@code null}
	 * @return the {@link ExampleSetBuilder} to build the example set
	 */
	public static ExampleSetBuilder from(List<Attribute> attributes) {
		if (Boolean.valueOf(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES))) {
			return new ColumnarExampleSetBuilder(attributes);
		} else {
			return new MemoryExampleSetBuilder(attributes);
		}
	}

	/**
	 * Creates a builder for an {@link ExampleSet} starting from the given attributes.
	 *
	 * @param attributes
	 *            the attributes for the new {@link ExampleSet}
	 * @return the {@link ExampleSetBuilder} to build the example set
	 */
	public static ExampleSetBuilder from(Attribute... attributes) {
		if (Boolean.valueOf(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES))) {
			return new ColumnarExampleSetBuilder(attributes);
		} else {
			return new MemoryExampleSetBuilder(attributes);
		}
	}

	/**
	 * Creates an {@link ExampleTable} to which rows can be added. Only use this if it is not
	 * possible to use an {@link ExampleSetBuilder}.
	 *
	 * @param attributes
	 *            the attributes for the new {@link ExampleTable}
	 * @return a table that can grow
	 */
	@SuppressWarnings("deprecation")
	public static GrowingExampleTable createTableFrom(List<Attribute> attributes) {
		if (Boolean.valueOf(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES))) {
			return new ColumnarExampleTable(attributes);
		} else {
			return new MemoryExampleTable(attributes);
		}
	}

}
