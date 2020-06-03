/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

package com.rapidminer.example.table.internal;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.table.ExampleTable;


/**
 * Interface for {@link ExampleTable}s with the {@link #columnCleanupClone(Attributes)} method. The method is used to
 * free the memory of unused columns in an {@link ExampleTable}. The {@link com.rapidminer.operator.execution.SimpleUnitExecutor}
 * initializes this cleanup before an operator is executed using the {@link com.rapidminer.operator.execution.FlowCleaner}.
 *
 * @author Gisa Meier
 * @since 9.7.0
 */
public interface CleanableExampleTable extends ExampleTable{

	/**
	 * Creates a shallow clone of the table and removes all columns not contained in attributes. This is done to free
	 * the memory that is taken by columns in the {@link ExampleTable} which are not used in an
	 * {@link com.rapidminer.example.ExampleSet} anymore. This method is called before an operator is executed with the
	 * {@link com.rapidminer.operator.execution.FlowCleaner#checkCleanup()} via the
	 * {@link com.rapidminer.example.set.SimpleExampleSet#cleanup()} method .
	 *
	 * @param attributes
	 * 		the attributes to determine which columns to keep
	 * @return a new table with only column data when the column is associated to an attribute from attributes
	 */
	ExampleTable columnCleanupClone(Attributes attributes);
}