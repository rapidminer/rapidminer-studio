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

import java.util.HashSet;
import java.util.Set;

import com.rapidminer.example.Example;


/**
 * 
 * SplitCondition for Radoop's Decision Tree. Returns true if the value of the desired attribute is
 * not in the given set.
 * 
 * @author Zsolt Toth
 * 
 */
public class NotContainsSplitCondition extends AbstractSplitCondition {

	private static final long serialVersionUID = 8093614015273139537L;

	private int maxDisplayedCategories = 2;
	private Set<String> categories;

	public NotContainsSplitCondition(String attributeName, String[] splittingCategories) {
		super(attributeName);
		categories = new HashSet<String>();
		for (String cat : splittingCategories) {
			categories.add(cat);
		}
	}

	@Override
	public String getRelation() {
		return "not in";
	}

	@Override
	public String getValueString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		int i = 0;
		for (String cat : categories) {
			if (i != 0) {
				sb.append(", ");
			}

			if (i == maxDisplayedCategories) {
				sb.append("...");
				break;
			}
			sb.append(cat);
			i++;
		}
		sb.append("}");
		return sb.toString();
	}

	@Override
	public boolean test(Example example) {
		return !categories.contains(example.getValueAsString(example.getAttributes().get(getAttributeName())));
	}

	@Override
	public String toString() {
		return getAttributeName() + " " + getRelation() + " " + getFullValueString();
	}

	private String getFullValueString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		boolean first = true;
		for (String cat : categories) {
			if (!first) {
				sb.append(", ");
			}
			sb.append(cat);
			first = false;
		}
		sb.append("}");
		return sb.toString();
	}

	public void setMaxDisplayedCategories(int maxDisplayedCategories) {
		this.maxDisplayedCategories = maxDisplayedCategories;
	}

	public Set<String> getCategories() {
		return categories;
	}
}
