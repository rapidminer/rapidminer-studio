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
 * This precondition checks whether the delivered object is of a given type or a collection of the
 * given type (or a collection of such collections etc.).
 * 
 * @author Simon Fischer
 * 
 */
public class CollectionPrecondition implements Precondition {

	private final Precondition nestedPrecondition;

	public CollectionPrecondition(Precondition precondition) {
		this.nestedPrecondition = precondition;
	}

	@Override
	public void assumeSatisfied() {
		nestedPrecondition.assumeSatisfied();
	}

	@Override
	public void check(MetaData md) {
		if (md != null) {
			if (md instanceof CollectionMetaData) {
				check(((CollectionMetaData) md).getElementMetaData());
				return;
			}
		}
		nestedPrecondition.check(md);
	}

	@Override
	public String getDescription() {
		return nestedPrecondition + " (collection)";
	}

	@Override
	public boolean isCompatible(MetaData input, CompatibilityLevel level) {
		if (input instanceof CollectionMetaData) {
			return isCompatible(((CollectionMetaData) input).getElementMetaData(), level);
		} else {
			return nestedPrecondition.isCompatible(input, level);
		}
	}

	@Override
	public MetaData getExpectedMetaData() {
		return new CollectionMetaData(nestedPrecondition.getExpectedMetaData());
	}

}
