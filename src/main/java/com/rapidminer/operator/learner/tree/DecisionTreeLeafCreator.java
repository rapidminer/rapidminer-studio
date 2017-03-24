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
package com.rapidminer.operator.learner.tree;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;


/**
 * This class can be used to transform an inner tree node into a leaf.
 * 
 * @author Ingo Mierswa, Christian Bockermann
 */
public class DecisionTreeLeafCreator implements LeafCreator {

	@Override
	public void changeTreeToLeaf(Tree node, ExampleSet exampleSet) {
		Attribute label = exampleSet.getAttributes().getLabel();
		exampleSet.recalculateAttributeStatistics(label);
		int labelValue = (int) exampleSet.getStatistics(label, Statistics.MODE);
		String labelName = label.getMapping().mapIndex(labelValue);
		node.setLeaf(labelName);
		for (String value : label.getMapping().getValues()) {
			int count = (int) exampleSet.getStatistics(label, Statistics.COUNT, value);
			node.addCount(value, count);
		}
	}
}
