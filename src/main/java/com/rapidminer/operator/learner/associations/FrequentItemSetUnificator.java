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
package com.rapidminer.operator.learner.associations;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.container.Tupel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * This operator compares a number of FrequentItemSet sets and removes every not unique
 * FrequentItemSet.
 * 
 * @author Sebastian Land
 */
public class FrequentItemSetUnificator extends Operator {

	private static class FrequencyIgnoringSetComparator implements Comparator<FrequentItemSet> {

		@Override
		public int compare(FrequentItemSet o1, FrequentItemSet o2) {
			// compare size
			Collection<Item> items = o1.getItems();
			Collection<Item> hisItems = o2.getItems();
			if (items.size() < hisItems.size()) {
				return -1;
			} else if (items.size() > hisItems.size()) {
				return 1;
			} else {
				// compare items
				Iterator<Item> iterator = hisItems.iterator();
				for (Item myCurrentItem : items) {
					int relation = myCurrentItem.toString().compareTo(iterator.next().toString());
					if (relation != 0) {
						return relation;
					}
				}
				// equal sets
				return 0;
			}
		}

	}

	private static class TupelComparator implements Comparator<Tupel<FrequentItemSet, Iterator<FrequentItemSet>>> {

		@Override
		public int compare(Tupel<FrequentItemSet, Iterator<FrequentItemSet>> o1,
				Tupel<FrequentItemSet, Iterator<FrequentItemSet>> o2) {
			FrequencyIgnoringSetComparator comparator = new FrequencyIgnoringSetComparator();
			return comparator.compare(o1.getFirst(), o2.getFirst());
		}

	}

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
		List<FrequentItemSets> sets = portExtender.getData(FrequentItemSets.class);
		for (FrequentItemSets set : sets) {
			set.sortSets(new FrequencyIgnoringSetComparator());
			sets.add(set);
		}

		ArrayList<Tupel<FrequentItemSet, Iterator<FrequentItemSet>>> iteratorTupels = new ArrayList<Tupel<FrequentItemSet, Iterator<FrequentItemSet>>>(
				2);
		for (FrequentItemSets classSets : sets) {
			Iterator<FrequentItemSet> iterator = classSets.iterator();
			iteratorTupels.add(new Tupel<FrequentItemSet, Iterator<FrequentItemSet>>(iterator.next(), iterator));
		}
		// running through iterators
		while (haveNext(iteratorTupels)) {
			// filling set to test if all frequent item sets are equal
			Set<FrequentItemSet> currentSets = new TreeSet<FrequentItemSet>(new FrequencyIgnoringSetComparator());
			for (Tupel<FrequentItemSet, Iterator<FrequentItemSet>> tupel : iteratorTupels) {
				currentSets.add(tupel.getFirst());
			}
			if (currentSets.size() == 1) {
				// not unique: deletion
				ArrayList<Tupel<FrequentItemSet, Iterator<FrequentItemSet>>> newTupels = new ArrayList<Tupel<FrequentItemSet, Iterator<FrequentItemSet>>>(
						2);
				for (Tupel<FrequentItemSet, Iterator<FrequentItemSet>> tupel : iteratorTupels) {
					Iterator<FrequentItemSet> currentIterator = tupel.getSecond();
					currentIterator.remove();
					if (currentIterator.hasNext()) {
						newTupels.add(new Tupel<FrequentItemSet, Iterator<FrequentItemSet>>(currentIterator.next(),
								currentIterator));
					}
				}
				iteratorTupels = newTupels;
			} else {
				// unique: no deletion but forward smallest iterator
				Collections.sort(iteratorTupels, new TupelComparator());
				Iterator<FrequentItemSet> currentIterator = iteratorTupels.get(0).getSecond();
				if (currentIterator.hasNext()) {
					iteratorTupels.add(new Tupel<FrequentItemSet, Iterator<FrequentItemSet>>(currentIterator.next(),
							currentIterator));
				}
				iteratorTupels.remove(0);
			}
		}

		portExtender.deliver(sets);
	}

	private boolean haveNext(ArrayList<Tupel<FrequentItemSet, Iterator<FrequentItemSet>>> iterators) {
		boolean hasNext = iterators.size() > 0;
		for (Tupel<FrequentItemSet, Iterator<FrequentItemSet>> iterator : iterators) {
			hasNext = hasNext || iterator.getSecond().hasNext();
		}
		return hasNext;
	}
}
