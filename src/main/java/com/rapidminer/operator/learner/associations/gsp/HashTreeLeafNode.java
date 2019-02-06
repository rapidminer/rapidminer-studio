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
package com.rapidminer.operator.learner.associations.gsp;

import java.util.ArrayList;


/**
 * @author Sebastian Land
 * 
 */
public class HashTreeLeafNode implements HashTreeNode {

	private int[] candidateIndices = new int[128];
	private int candidateIndicesSize = 0;

	@Override
	public void addSequence(Sequence candidate, int candidateIndex, int depth, HashTreeNode father,
			ArrayList<Sequence> allCandidates) {
		candidateIndicesSize++;
		if (candidateIndicesSize > candidateIndices.length) {
			if (depth < candidate.size() - 1) {
				// exchange this leaf node by inner node if it could become inner node
				HashTreeInnerNode newInner = new HashTreeInnerNode();
				father.replaceNode(candidate.getItem(depth - 1), newInner);

				// and adding all sequences and last candidate
				for (int i = 0; i < candidateIndices.length; i++) {
					newInner.addSequence(allCandidates.get(candidateIndices[i]), candidateIndices[i], depth, father,
							allCandidates);
				}
				newInner.addSequence(allCandidates.get(candidateIndex), candidateIndex, depth, father, allCandidates);
			} else {
				int[] newIndices = new int[candidateIndices.length * 2];
				System.arraycopy(candidateIndices, 0, newIndices, 0, candidateIndices.length);
				candidateIndices = newIndices;
				candidateIndices[candidateIndicesSize - 1] = candidateIndex;
			}
		} else {
			candidateIndices[candidateIndicesSize - 1] = candidateIndex;
		}
	}

	@Override
	public void countCoveredCandidates(DataSequence sequence, double t, CountingInformations counting) {
		for (int i = 0; i < candidateIndicesSize; i++) {
			counting.candidateCounter[candidateIndices[i]] = DataSequence.containsSequence(sequence,
					counting.allCandidates.get(candidateIndices[i]), counting);
		}
	}

	@Override
	public void replaceNode(Item which, HashTreeNode replacement) {
		// cannot occur in leaf node!
	}
}
