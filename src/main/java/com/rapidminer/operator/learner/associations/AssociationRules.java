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
package com.rapidminer.operator.learner.associations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.tools.Tools;


/**
 * A set of {@link AssociationRule}s which can be constructed from frequent item sets.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class AssociationRules extends ResultObjectAdapter implements Iterable<AssociationRule> {

	private static final long serialVersionUID = 3734387908954857589L;

	private static final int MAXIMUM_NUMBER_OF_RULES_IN_OUTPUT = 100;

	private List<AssociationRule> associationRules = new ArrayList<AssociationRule>();

	/**
	 * Adds an AssociationRule
	 * <p>
	 * Call {@link sort()} after adding Rules for a sorted List
	 * </p>
	 *
	 * @param rule
	 */
	public void addItemRule(AssociationRule rule) {
		associationRules.add(rule);
	}

	/**
	 * Sorts the associationRules into ascending order, according to the confidence.
	 * <p>
	 * Warning: Do not call while using {@link iterator()}
	 * </p>
	 */
	public void sort() {
		Collections.sort(associationRules);
	}

	public int getNumberOfRules() {
		return associationRules.size();
	}

	public AssociationRule getRule(int index) {
		return associationRules.get(index);
	}

	public String getExtension() {
		return "asr";
	}

	public String getFileDescription() {
		return "Association Rules";
	}

	@Override
	public String toResultString() {
		return toString(MAXIMUM_NUMBER_OF_RULES_IN_OUTPUT);
	}

	/**
	 * Returns a list of up to {@link MAXIMUM_NUMBER_OF_RULES_IN_OUTPUT} Asssociation Rules
	 *
	 */
	@Override
	public String toString() {
		return toString(MAXIMUM_NUMBER_OF_RULES_IN_OUTPUT);
	}

	/**
	 * Returns a list of {@link maxNumber} Association Rules
	 *
	 * @param maxNumber
	 *            maximum number of printed Association Rules
	 * @return Multiline formatted list of Association Rules
	 */
	public String toString(int maxNumber) {
		StringBuffer buffer = new StringBuffer("Association Rules" + Tools.getLineSeparator());
		int counter = 0;
		for (AssociationRule rule : associationRules) {
			if (maxNumber >= 0 && counter > maxNumber) {
				buffer.append("... " + (associationRules.size() - maxNumber) + " other rules ...");
				break;
			}
			buffer.append(rule.toString());
			buffer.append(Tools.getLineSeparator());
			counter++;
		}
		return buffer.toString();
	}

	@Override
	public Iterator<AssociationRule> iterator() {
		return associationRules.iterator();
	}

	public Item[] getAllConclusionItems() {
		SortedSet<Item> conclusions = new TreeSet<Item>();
		for (AssociationRule rule : this) {
			Iterator<Item> i = rule.getConclusionItems();
			while (i.hasNext()) {
				conclusions.add(i.next());
			}
		}
		Item[] itemArray = new Item[conclusions.size()];
		conclusions.toArray(itemArray);
		return itemArray;
	}
}
