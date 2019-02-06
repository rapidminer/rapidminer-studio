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
package com.rapidminer.tools;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.SwingUtilities;


/**
 * Copy of the AbstractObservable, but does not allow duplicate listener
 *
 * @author Simon Fischer
 * @author Jonas Wilms-Pfau
 * @since 7.4
 *
 */
public class SimpleObservable<A> implements Observable<A> {

	private final LinkedList<Observer<A>> observersRegular = new LinkedList<>();
	private final LinkedList<Observer<A>> observersEDT = new LinkedList<>();

	private final Object lock = new Object();

	@Override
	public void addObserverAsFirst(Observer<A> observer, boolean onEDT) {
		if (observer == null) {
			throw new NullPointerException("Observer is null.");
		}
		if (onEDT) {
			synchronized (lock) {
				if (observersEDT.contains(observer)) {
					observersEDT.remove(observer);
				}
				observersEDT.addFirst(observer);
			}
		} else {
			synchronized (lock) {
				if (observersRegular.contains(observer)) {
					observersRegular.remove(observer);
				}
				observersRegular.addFirst(observer);
			}
		}
	}

	@Override
	public void addObserver(com.rapidminer.tools.Observer<A> observer, boolean onEDT) {
		if (observer == null) {
			throw new NullPointerException("Observer is null.");
		}
		if (onEDT) {
			synchronized (lock) {
				if (!observersEDT.contains(observer)) {
					observersEDT.add(observer);
				}
			}
		} else {
			synchronized (lock) {
				if (!observersRegular.contains(observer)) {
					observersRegular.add(observer);
				}
			}
		}
	}

	@Override
	public void removeObserver(com.rapidminer.tools.Observer<A> observer) {
		boolean success = false;
		synchronized (lock) {
			success |= observersRegular.remove(observer);
			success |= observersEDT.remove(observer);
		}
		if (!success) {
			throw new NoSuchElementException("No such observer: " + observer);
		}
	}

	/** Updates the observers in the given list. */
	private void fireUpdate(List<com.rapidminer.tools.Observer<A>> observerList, A argument) {
		for (com.rapidminer.tools.Observer<A> observer : observerList) {
			observer.update(this, argument);
		}
	}

	/** Equivalent to <code>fireUpdate(null)</code>. */
	protected void fireUpdate() {
		fireUpdate(null);
	}

	/** Updates all observers with the given argument. */
	protected void fireUpdate(final A argument) {
		// lists are copied in order to avoid ConcurrentModification occurs if updating
		// an observer triggers insertion of another
		List<com.rapidminer.tools.Observer<A>> copy;
		synchronized (lock) {
			copy = new LinkedList<>(observersRegular);
		}
		fireUpdate(copy, argument);
		if (!observersEDT.isEmpty()) {
			final List<com.rapidminer.tools.Observer<A>> copyEDT;
			synchronized (lock) {
				copyEDT = new LinkedList<>(observersEDT);
			}
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					fireUpdate(copyEDT, argument);
				}
			});
		}
	}

}
