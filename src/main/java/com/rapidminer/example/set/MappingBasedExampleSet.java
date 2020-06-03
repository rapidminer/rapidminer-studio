/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.example.set;

import com.rapidminer.example.ExampleSet;


/**
 * A super interface for {@link ExampleSet}s that are basically a parent {@link ExampleSet} plus a mapping int-array,
 * like {@link MappedExampleSet}. For these example sets, methods are added to access the mapping and the parent in
 * order to make conversion in the belt-adapter more efficient.
 *
 * @author Gisa Meier
 * @since 9.7.0
 */
public interface MappingBasedExampleSet extends ExampleSet {

	/**
	 * @return a copy of the mapping array
	 */
	int[] getMappingCopy();

	/**
	 * Returns whether the parent is either a {@link SimpleExampleSet} or a {@link MappingBasedExampleSet}.
	 *
	 * @return {@code true} iff the parent is simple or mapped
	 */
	boolean isParentSimpleOrMapped();

	/**
	 * @return a clone of the parent {@link ExampleSet}
	 */
	ExampleSet getParentClone();
}