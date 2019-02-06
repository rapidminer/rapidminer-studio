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
package com.rapidminer.operator.learner.tree;

import java.io.Serializable;


/**
 * The class edge holds the information about a split condition to a tree (child).
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class Edge implements Serializable, Comparable<Edge> {

	private static final long serialVersionUID = -6470281011799533198L;

	private SplitCondition condition;

	private Tree child;

	public Edge(Tree child, SplitCondition condition) {
		this.condition = condition;
		this.child = child;
	}

	public SplitCondition getCondition() {
		return this.condition;
	}

	public Tree getChild() {
		return this.child;
	}

	@Override
	public int compareTo(Edge o) {
		return (this.condition.getRelation() + this.condition.getValueString()).compareTo(o.condition.getRelation()
				+ o.condition.getValueString());
	}
}
