/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.operator.learner.tree;

import com.rapidminer.example.Attribute;


/**
 * This class can be used to transform an inner tree node into a leaf.
 *
 * @author Gisa Schaefer
 */
public class ParallelDecisionTreeLeafCreator {

	/**
	 * Transforms the tree node into a leaf by storing the number of label values and naming the
	 * leaf by the majority label value.
	 *
	 * @param node
	 * @param columnTable
	 * @param selectedExamples
	 */
	public void changeTreeToLeaf(Tree node, ColumnExampleTable columnTable, int[] selectedExamples) {
		Attribute label = columnTable.getLabel();
		int[] labelColumn = columnTable.getLabelColumn();
		int numberOfLabels = label.getMapping().size();
		int[] labelValueCount = new int[numberOfLabels];
		// count the different labels for the example number in selection
		for (int i = 0; i < selectedExamples.length; i++) {
			int indexForAdd = labelColumn[selectedExamples[i]];
			labelValueCount[indexForAdd]++;
		}
		int maxcount = 0;
		String labelName = null;
		// save the frequency of different labels in the node and name it by the most frequent label
		for (String value : label.getMapping().getValues()) {
			int count = labelValueCount[label.getMapping().getIndex(value)];
			node.addCount(value, count);
			if (count > maxcount) {
				maxcount = count;
				labelName = value;
			}
		}
		node.setLeaf(labelName);
	}
}
