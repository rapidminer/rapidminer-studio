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

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @param the
 *            argument that is expected when observing other classes
 * 
 * @author Nils Woehler
 * 
 */
public abstract class AbstractChangeAwareSaveable<O> extends AbstractObservable<ChangeAwareSaveable> implements
		ChangeAwareSaveable {

	private static final Logger LOGGER = Logger.getLogger(AbstractChangeAwareSaveable.class.getName());

	private transient boolean isModified = false;

	private DelegatingObserver<O, ChangeAwareSaveable> delegatingObserver = new DelegatingObserver<O, ChangeAwareSaveable>(
			this, this);

	protected void observeForChanges(Observable<O> observable) {
		if (observable != null) {
			observable.addObserver(delegatingObserver, false);
		}
	}

	protected void stopObservingForChanges(Observable<O> observable) {
		if (observable != null) {
			observable.removeObserver(delegatingObserver);
		}
	}

	@Override
	protected void fireUpdate(ChangeAwareSaveable argument) {
		LOGGER.log(Level.FINE, "Fire update called");
		LOGGER.log(Level.FINE, "Is modified: " + isModified());
		LOGGER.log(Level.FINE, "Is initialized: " + isInitialized());
		if (!isModified() && isInitialized()) {
			setIsModified(true);
			LOGGER.log(Level.FINE, "Notify observers of changed state: CHANGED");
			super.fireUpdate(argument);
		}
	}

	@Override
	public void saved() {
		setIsModified(false);
		LOGGER.log(Level.FINE, "Set changed state to UNCHANGED");
	}

	@Override
	public boolean isModified() {
		return isModified;
	}

	/**
	 * @param isModified
	 *            the isModified to set
	 */
	public void setIsModified(boolean isModified) {
		LOGGER.log(Level.FINE, "Set is modified to " + isModified);
		this.isModified = isModified;
	}

}
