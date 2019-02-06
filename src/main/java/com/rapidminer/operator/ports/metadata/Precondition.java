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

import com.rapidminer.operator.ports.InputPort;


/**
 * A simple precondition of an {@link InputPort}. In an early design phase, preconditions used to be
 * special {@link MDTransformationRule}s rules, but having them separately attached to the input
 * ports has advantages for GUI design and auto-wiring processes.
 * 
 * @author Simon Fischer
 * */
public interface Precondition {

	/**
	 * Checks whether the precondition is satisfied, registering a {@link MetaDataError} with the
	 * input port if not.
	 * 
	 * @param metaData
	 *            the delivered meta data. Note that this may differ from the meta data currently
	 *            assigned to the input port for which this Precondition was created, e.g. for a
	 *            ClooectionPrecondition.
	 * */
	public void check(MetaData metaData);

	/** Returns a human readable description. */
	public String getDescription();

	/** Returns true if the given object is compatible with this precondition. */
	public boolean isCompatible(MetaData input, CompatibilityLevel level);

	/**
	 * Assume that the precondition is satisfied, i.e., artificially generate compatible meta data
	 * at the input port. This method is used to check what output would be generated if the input
	 * was correctly delivered.
	 */
	public void assumeSatisfied();

	/** Returns the meta data required by this precondition. */
	public MetaData getExpectedMetaData();
}
