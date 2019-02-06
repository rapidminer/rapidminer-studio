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
import java.util.Iterator;


/**
 * @author Sebastian Land
 *
 */
public class Sequence extends ArrayList<Transaction> implements Comparable<Sequence> {

	private static final long serialVersionUID = -5774432548086375L;

	public Sequence() {};

	public Sequence(Sequence sequence) {
		addAll(sequence);
	}

	public int getNumberOfItems() {
		int size = 0;
		for (Transaction transaction : this) {
			size += transaction.size();
		}
		return size;
	}

	/**
	 * This method returns the n-th item in the sequence counting all items in every transaction
	 */
	public Item getItem(int n) {
		for (Transaction transaction : this) {
			if (n - transaction.size() < 0) {
				return transaction.get(n);
			}
			n -= transaction.size();
		}
		return null;
	}

	public Transaction getLastTransaction() {
		return get(size() - 1);
	}

	/**
	 * This method implements an enhanced equals method where the specified elements of the
	 * sequences are treated as if not existent. This avoids construction of new sequences.
	 */
	public boolean equals(int ignoreOwnElement, Sequence sequence, int ignoreItsElement) {
		int ownIndex = 0;
		int itsIndex = 0;
		Iterator<Item> ownIterator = itemIterator();
		Iterator<Item> itsIterator = sequence.itemIterator();
		while (true) {
			// shift both iterators to next items
			if (ignoreOwnElement == ownIndex) {
				ownIterator.next();
				ownIndex++;
			}
			if (ignoreItsElement == itsIndex) {
				itsIterator.next();
				itsIndex++;
			}

			// testing if abort
			if (!ownIterator.hasNext() || !itsIterator.hasNext()) {
				if (!ownIterator.hasNext() && !itsIterator.hasNext()) {
					return true;
				}
				return false;
			}

			Item ownItem = ownIterator.next();
			Item itsItem = itsIterator.next();
			ownIndex++;
			itsIndex++;
			if (!ownItem.equals(itsItem)) {
				return false;
			}
		}
	}

	public Iterator<Item> itemIterator() {
		return new Iterator<Item>() {

			private Iterator<Transaction> transactionIterator = iterator();
			private Iterator<Item> itemIterator;

			@Override
			public boolean hasNext() {
				if (transactionIterator.hasNext()) {
					return true;
				}
				if (itemIterator != null) {
					if (itemIterator.hasNext()) {
						return true;
					}
				}
				return false;
			}

			@Override
			public Item next() {
				if (itemIterator == null) {
					itemIterator = transactionIterator.next().iterator();
				}
				if (!itemIterator.hasNext()) {
					itemIterator = transactionIterator.next().iterator();
				}
				return itemIterator.next();
			}

			@Override
			public void remove() {
				// not supported
			}
		};
	}

	/**
	 * This method returns a Sequence, with the specified transaction appended. It has to take care,
	 * that neither the original sequence nor the original transaction is altered.
	 */
	public static Sequence appendTransaction(Sequence sequence1, Transaction lastTransaction) {
		Sequence result = new Sequence(sequence1);
		result.add(lastTransaction);
		return result;
	}

	/**
	 * This method returns a Sequence, with the specified item appended on the last transaction. It
	 * has to take care, that neither the original sequence nor the original transaction is altered.
	 */
	public static Sequence appendItem(Sequence sequence1, Item lastItem) {
		Sequence result = new Sequence(sequence1);
		int lastIndex = result.size() - 1;
		Transaction newTransaction = new Transaction(result.get(lastIndex));

		result.remove(lastIndex);
		newTransaction.add(lastItem);

		result.add(newTransaction);
		return result;
	}

	public static Sequence removeItem(Sequence candidate, int transactionIndex, int itemIndex) {
		Sequence result = new Sequence(candidate);
		Transaction newTransaction = new Transaction(result.get(transactionIndex));
		newTransaction.remove(itemIndex);
		if (newTransaction.size() > 0) {
			result.set(transactionIndex, newTransaction);
		} else {
			result.remove(transactionIndex);
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (Transaction transaction : this) {
			buffer.append("<" + transaction.toString() + ">  ");
		}
		return buffer.toString();
	}

	@Override
	public int compareTo(Sequence o) {
		int itsSize = o.getNumberOfItems();
		int mySize = getNumberOfItems();
		if (itsSize == mySize) {
			Iterator<Item> myItem = itemIterator();
			Iterator<Item> itsItem = o.itemIterator();
			while (myItem.hasNext() && itsItem.hasNext()) {
				int itsIndex = itsItem.next().getIndex();
				int myIndex = myItem.next().getIndex();
				if (itsIndex != myIndex) {
					return myIndex > itsIndex ? 1 : -1;
				}
			}
			for (int i = 0; i < this.size(); i++) {
				int myLocalSize = this.get(i).size();
				int itsLocalSize = o.get(i).size();
				if (myLocalSize != itsLocalSize) {
					return myLocalSize > itsLocalSize ? 1 : -1;
				}
			}
			return 0;
		} else {
			return mySize > itsSize ? 1 : -1;
		}
	}
}
