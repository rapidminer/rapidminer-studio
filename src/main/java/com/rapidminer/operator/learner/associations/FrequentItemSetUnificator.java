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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * This operator compares a number of FrequentItemSet sets and removes every not unique
 * FrequentItemSet.
 *
 * @author Sebastian Land, Jonas Wilms-Pfau
 */
public class FrequentItemSetUnificator extends Operator {

	private PortPairExtender portExtender = new PortPairExtender("frequent item sets", getInputPorts(), getOutputPorts(),
			new MetaData(FrequentItemSets.class));

	public FrequentItemSetUnificator(OperatorDescription description) {
		super(description);

		portExtender.ensureMinimumNumberOfPorts(2);
		getTransformer().addRule(portExtender.makePassThroughRule());
		portExtender.start();
	}

	@Override
	public void doWork() throws OperatorException {
		List<FrequentItemSets> allSets = portExtender.getData(FrequentItemSets.class);

		// Item names only of the duplicates
		Set<List<String>> duplicates = findDuplicates(allSets);

		// Store modified sets as results
		List<FrequentItemSets> results = new ArrayList<>();

		// Remove duplicates
		for (FrequentItemSets sets : allSets) {
			FrequentItemSets clonedSet = (FrequentItemSets) sets.clone();
			Iterator<FrequentItemSet> iterator = clonedSet.iterator();
			while (iterator.hasNext()) {
				if (duplicates.contains(getItemNames(iterator.next()))) {
					iterator.remove();
				}
			}
			results.add(clonedSet);
		}

		portExtender.deliver(results);
	}

	/**
	 * Returns the item names in a list, does not contain the frequency information
	 *
	 * @param set
	 * 		the set
	 * @return item names
	 * @since 9.0.2
	 */
	private static List<String> getItemNames(FrequentItemSet set) {
		List<String> setAsStrings = new ArrayList<>(set.getNumberOfItems());
		for (Item item : set.getItems()) {
			setAsStrings.add(item.toString());
		}
		return setAsStrings;
	}

	/**
	 * Find duplicates in the item sets
	 *
	 * @param allItemSets
	 * 		all {@link FrequentItemSets}
	 * @return the names of the duplicated item sets
	 * @since 9.0.2
	 */
	private static Set<List<String>> findDuplicates(List<FrequentItemSets> allItemSets) {
		Set<List<String>> duplicates = new HashSet<>();
		Set<List<String>> all = new HashSet<>();

		// Find duplicates
		for (FrequentItemSets sets : allItemSets) {
			for (FrequentItemSet set : sets) {
				List<String> itemNames = getItemNames(set);
				if (!all.add(itemNames)) {
					duplicates.add(itemNames);
				}
			}
		}

		return duplicates;
	}

}
