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
package com.rapidminer.operator.learner.associations.gsp;

/**
 * @author Sebastian Land
 * 
 */
public class HashTreeRootNode extends HashTreeInnerNode {

	/**
	 * This method implements the root node behavior of counting. t is ignored, depth assumed to be
	 * zero anyway.
	 */
	@Override
	public void countCoveredCandidates(DataSequence sequence, double t, CountingInformations counting) {
		for (Transaction transaction : sequence) {
			for (Item item : transaction) {
				HashTreeNode child = children.get(item);
				if (child != null) {
					child.countCoveredCandidates(sequence, transaction.getTime(), counting);
				}
			}
		}
	}
}
