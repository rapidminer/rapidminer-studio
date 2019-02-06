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
import java.util.Comparator;
import java.util.Iterator;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.tools.Tools;


/**
 * Contains a collection of {@link FrequentItemSet}s.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class FrequentItemSets extends ResultObjectAdapter implements Iterable<FrequentItemSet>, Cloneable {

	private static final long serialVersionUID = -6195363961857170621L;

	private static final int MAX_NUMBER_OF_ITEMSETS = 100;

	private int numberOfTransactions;

	private int maximumSetSize = 0;

	private ArrayList<FrequentItemSet> frequentSets;

	public FrequentItemSets(int numberOfTransactions) {
		this.numberOfTransactions = numberOfTransactions;
		this.frequentSets = new ArrayList<>();
	}

	/**
	 * Clone constructor.
	 * @since 9.0.2
	 */
	private FrequentItemSets(FrequentItemSets other) {
		this.numberOfTransactions = other.numberOfTransactions;
		this.frequentSets = new ArrayList<>();
		this.maximumSetSize = other.maximumSetSize;
		for (FrequentItemSet set : other.frequentSets) {
			frequentSets.add((FrequentItemSet) set.clone());
		}
	}

	/**
	 * Adds a frequent item set to this container. ConditionalItems and frequentItems are merged.
	 * 
	 * @param itemSet
	 *            the frequent set
	 */
	public void addFrequentSet(FrequentItemSet itemSet) {
		frequentSets.add(itemSet);
		maximumSetSize = Math.max(itemSet.getNumberOfItems(), maximumSetSize);
	}

	public String getExtension() {
		return "frq";
	}

	public String getFileDescription() {
		return "frequent item set";
	}

	public int getMaximumSetSize() {
		return this.maximumSetSize;
	}

	@Override
	public Iterator<FrequentItemSet> iterator() {
		return frequentSets.iterator();
	}

	public FrequentItemSet getItemSet(int index) {
		return frequentSets.get(index);
	}

	public void sortSets() {
		Collections.sort(frequentSets);
	}

	public void sortSets(Comparator<FrequentItemSet> comparator) {
		Collections.sort(frequentSets, comparator);
	}

	public int size() {
		return frequentSets.size();
	}

	public int getNumberOfTransactions() {
		return this.numberOfTransactions;
	}

	@Override
	public String toResultString() {
		if (size() > MAX_NUMBER_OF_ITEMSETS) {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(toString(MAX_NUMBER_OF_ITEMSETS));
			stringBuilder.append("... (" + (size() - MAX_NUMBER_OF_ITEMSETS) + " more)");
			return stringBuilder.toString();
		} else {
			return toString(-1);
		}
	}

	/** This method generates the a string representation of this object. */
	@Override
	public String toString() {
		return toString(MAX_NUMBER_OF_ITEMSETS);
	}

	/** This method generates the a string representation of this object. */
	public String toString(int maxNumber) {
		StringBuffer output = new StringBuffer("Frequent Item Sets (" + size() + "):" + Tools.getLineSeparator());
		if (frequentSets.size() == 0) {
			output.append("no itemsets found");
		} else {
			int counter = 0;
			for (FrequentItemSet set : frequentSets) {
				counter++;
				if ((maxNumber > 0) && (counter > maxNumber)) {
					output.append("... " + (size() - maxNumber) + " additional item sets ...");
					break;
				} else {
					output.append(set.getItemsAsString());
					output.append(" / ");
					output.append(Tools.formatNumber((double) set.getFrequency() / (double) numberOfTransactions));
					output.append(Tools.getLineSeparator());
				}

			}
		}
		return output.toString();
	}

	@Override
	public IOObject copy() {
		return (IOObject) clone();
	}

	@Override
	public Object clone() {
		return new FrequentItemSets(this);
	}

}
