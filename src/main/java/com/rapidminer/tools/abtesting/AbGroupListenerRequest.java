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
package com.rapidminer.tools.abtesting;

import com.rapidminer.gui.tools.SwingTools;


/**
 * Holder object for {@link AbGroupChangedListener} requests
 *
 * @author Jonas Wilms-Pfau
 * @since 8.2
 */
class AbGroupListenerRequest {

	/** The listener that should get informed */
	private final AbGroupChangedListener listener;

	/** Number of A/B groups */
	private final int numberOfGroups;

	/** Determines if the listener should be informed on the Event Dispatch Thread */
	private final boolean onEDT;

	/**
	 * Creates a new AbGroupListenerRequest
	 *
	 * @param listener
	 * 		the listener instance
	 * @param numberOfGroups
	 * 		number of A/B groups
	 * @param onEDT
	 * 		if the listener should be notified on the edt
	 * @throws IllegalArgumentException
	 * 		if groups is &lt;= 0, or listener is {@code null}
	 */
	public AbGroupListenerRequest(AbGroupChangedListener listener, int numberOfGroups, boolean onEDT) {
		if (numberOfGroups <= 0) {
			throw new IllegalArgumentException("numberOfGroups must be bigger than 0");
		}
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null");
		}
		this.listener = listener;
		this.numberOfGroups = numberOfGroups;
		this.onEDT = onEDT;
	}

	/**
	 * The A/B Group Size
	 *
	 * @return numberOfGroups &gt; 0
	 */
	public int getNumberOfGroups() {
		return numberOfGroups;
	}

	/**
	 * Notifies the listener in case the group has changed
	 *
	 * @param newGroup the new group
	 * @param oldGroup the previous group
	 */
	public void groupUpdated(int newGroup, int oldGroup){
		if (newGroup != oldGroup) {
			if (onEDT) {
				SwingTools.invokeLater(() -> listener.groupChanged(newGroup, oldGroup, numberOfGroups));
			} else {
				listener.groupChanged(newGroup, oldGroup, numberOfGroups);
			}
		}
	}

	/**
	 * Compares the internal listener with the given listener
	 *
	 * @param listener
	 * 		the listener that should be compared to the internal listener
	 * @return true
	 * 		if the internal listener equals the given listener
	 */
	public boolean listenerEquals(AbGroupChangedListener listener) {
		return this.listener.equals(listener);
	}

}