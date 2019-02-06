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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;


/**
 * Additionally to the Sequence methods, this one stores information for fast access on items within
 * time constraints.
 * 
 * @author Sebastian Land
 * 
 */
public class DataSequence extends Sequence {

	private static final long serialVersionUID = -7914523040469057204L;

	private double[][] times;

	public DataSequence(int numberOfItems) {
		super();
		times = new double[numberOfItems][];
	}

	public void buildAccessStructure() {
		int itemCount[] = new int[times.length];
		Iterator<Item> itemIterator = itemIterator();
		while (itemIterator.hasNext()) {
			int itemIndex = itemIterator.next().getIndex();
			itemCount[itemIndex]++;
		}

		for (int i = 0; i < times.length; i++) {
			times[i] = new double[itemCount[i]];
			itemCount[i] = 0; // deleting for further use
		}

		for (Transaction transaction : this) {
			double time = transaction.getTime();
			for (Item item : transaction) {
				int itemIndex = item.getIndex();
				times[itemIndex][itemCount[itemIndex]++] = time;
			}
		}
	}

	private TransactionSet findTransaction(Transaction findWhat, double t, CountingInformations countingInformations) {
		TransactionSet result = new TransactionSet();
		boolean includeStartValue = false;

		while (true) {
			for (Item item : findWhat) {
				double foundTime = firstOccurenceAfter(item, t, includeStartValue);
				if (Double.isNaN(foundTime)) {
					return null; // then no item later currentTime has been found
				}
				result.addTimeOfTransaction(foundTime);
			}
			if (result.getEndTime() - result.getStartTime() > countingInformations.windowSize) {
				t = result.getEndTime() - countingInformations.windowSize; // we can use the end
																			// time here, because
																			// the
																			// earliest occurrence
																			// of at least one item
																			// is at this position.
																			// Since it must be
																			// included.

				includeStartValue = true;
				result.reset();
			} else {
				return result;
			}
		}

	}

	private double firstOccurenceAfter(Item item, double currentTime, boolean includeCurrentTime) {
		int itemIndex = item.getIndex();
		for (int i = 0; i < times[itemIndex].length; i++) {
			if (times[itemIndex][i] > currentTime || includeCurrentTime && times[itemIndex][i] == currentTime) {
				return times[itemIndex][i];
			}
		}
		return Double.NaN;
	}

	public static boolean containsSequence(DataSequence data, Sequence candidate, CountingInformations countingInformations) {
		ListIterator<Transaction> candidateIterator = candidate.listIterator();
		LinkedList<TransactionSet> matches = new LinkedList<TransactionSet>();
		ListIterator<TransactionSet> matchesIterator = matches.listIterator();
		// loop until true or false
		double t = Double.NEGATIVE_INFINITY;
		while (true) {
			// forward step

			while (candidateIterator.hasNext()) {
				Transaction currentTransaction = candidateIterator.next();
				TransactionSet currentSet = data.findTransaction(currentTransaction, t, countingInformations);
				if (currentSet != null) {
					double difference = currentSet.getEndTime() - t;

					if (matches.isEmpty()
							|| difference <= countingInformations.maxGap && difference >= countingInformations.minGap) { // matches
																											// is
																											// empty
																											// as
																											// indicator
																											// for
																											// first
																											// run!
																											// no
																											// previous
																											// to
																											// check
						matchesIterator.add(currentSet);
						t = currentSet.getEndTime();
					} else {
						// Go to the last valid entry
						if (candidateIterator.hasPrevious()) {
							candidateIterator.previous();
						}
						t = currentSet.getEndTime();
						break;
					}

				} else {
					return false; // candidate not contained
				}

			}
			// checking if complete candidate is contained
			if (!candidateIterator.hasNext()) {
				return true;
			}

			// backward step
			if (matchesIterator.hasPrevious()) {
				matchesIterator.previous();
			} else {
				return false;
			}
			while (true) {
				Transaction currentTransaction = candidateIterator.previous();
				TransactionSet currentSet = data.findTransaction(currentTransaction, t - countingInformations.maxGap,
						countingInformations);
				if (currentSet != null) {
					if (matchesIterator.hasPrevious()) {
						TransactionSet last = matchesIterator.previous();
						double difference = currentSet.getStartTime() - last.getEndTime(); // difference
																							// can
																							// be
																							// negative
						// because of window!
						if (difference <= countingInformations.maxGap && difference >= countingInformations.minGap
								&& difference > 0d || !candidateIterator.hasPrevious()) {
							t = currentSet.getEndTime();
							matchesIterator.add(currentSet);
							break;
						} else {
							t = currentSet.getStartTime();
						}
					} else {
						matchesIterator.add(currentSet);
						t = currentSet.getEndTime();
						break;
					}
				} else {
					return false; // not contained
				}
			}

			// deleting all subsequent but deleted matches
			while (matchesIterator.hasNext()) {
				matchesIterator.next();
				matchesIterator.remove();
			}
			matchesIterator.previous();
			// this one has found on backward: Next one should be following not itself
			candidateIterator.next();

		}
	}
}
