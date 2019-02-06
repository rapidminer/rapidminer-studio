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

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import com.rapidminer.license.LicenseManager;
import com.rapidminer.license.LicenseManagerRegistry;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Determines a group for A/B testing
 * <dl>
 *  <dt>Example:</dt>
 *  <dd>{@code AbGroupProvider.get(10)} returns a number between 0 and 9</dd>
 * </dl>
 * The selected group should stay the same as long as the {@linkplain com.rapidminer.license.LicenseUser#getEmail() license user email address} does not change.
 * <p>
 * Use {@link #registerAbGroupChangedListener} to get informed on group changes.
 * </p>
 *
 * @author Jonas Wilms-Pfau
 * @since 8.2
 */
public class AbGroupProvider {

	/**
	 * The default instance that should be based on the license email if possible
	 */
	private static final AbGroupProvider INSTANCE = new AbGroupProvider(IdentifierProvider.getIdentifier()).registerLicenseManagerListenerIfPossible();

	/**
	 * Defines if the AbGroupProvider is registered as a listener to the LicenseManager
	 */
	private volatile boolean isRegisteredToLicenseManager = false;

	/**
	 * The seed used to determine the group
	 */
	private final AtomicLong seed;

	/**
	 * Listeners that get informed whenever their group has changed
	 */
	private final List<AbGroupListenerRequest> groupChangedListener = new CopyOnWriteArrayList<>();

	/**
	 * Creates a new AbGroupProvider that uses the given seed
	 *
	 * @param seed used to determine the {@link #getGroup(int) A/B group}
	 */
	AbGroupProvider(long seed) {
		this.seed = new AtomicLong(seed);
	}

	/**
	 * Initializes with {@link String#hashCode} of the seed
	 *
	 * @param seed uses the {@link String#hashCode hash} of this {@code String} as seed
	 * @see AbGroupProvider#AbGroupProvider(long)
	 */
	AbGroupProvider(String seed) {
		this(("" + seed).hashCode());
	}

	/**
	 * Returns an {@link IdentifierProvider#getIdentifier} based AbGroupProvider
	 *
	 * @return The global instance
	 */
	public static AbGroupProvider get() {
		if (!INSTANCE.isRegisteredToLicenseManager) {
			INSTANCE.registerLicenseManagerListenerIfPossible();
		}
		return INSTANCE;
	}

	/**
	 * Returns a Group between inclusive 0 and exclusive {@code numberOfGroups}
	 * <p>
	 * Shortcut for {@code AbGroupProvider.get().getGroup(int)}
	 * </p>
	 *
	 * @param numberOfGroups
	 * 		must be &gt; 0
	 * @return a number between 0 and {@code numberOfGroups-1}
	 */
	public static int get(int numberOfGroups) {
		return get().getGroup(numberOfGroups);
	}

	/**
	 * Returns a Group between inclusive 0 and exclusive {@code numberOfGroups}
	 *
	 * @param numberOfGroups
	 * 		must be &gt; 0
	 * @return a number between 0 and {@code numberOfGroups-1}
	 * @throws IllegalArgumentException
	 * 		if numberOfGroups is not &gt; 0
	 */
	public int getGroup(int numberOfGroups) {
		int group = new Random(seed.get()).nextInt(numberOfGroups);
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_AB_GROUP, String.valueOf(numberOfGroups), String.valueOf(group));
		return group;
	}

	/**
	 * Registers a listener for a/b group changes
	 *
	 * @param listener
	 *      the listener that gets informed on group changes
	 * @param numberOfGroups
	 * 		number of A/B groups
	 * @param onEDT
	 * 		if the listener should be notified on the event dispatch thread
	 * @throws IllegalArgumentException
	 * 		if groups is &lt;= 0, or listener is {@code null}
	 */
	public void registerAbGroupChangedListener(AbGroupChangedListener listener, int numberOfGroups, boolean onEDT) {
		groupChangedListener.add(new AbGroupListenerRequest(listener, numberOfGroups, onEDT));
	}

	/**
	 * Removes the listener
	 *
	 * @param listener the listener that should no longer be informed
	 */
	public void removeAbGroupChangedListener(AbGroupChangedListener listener) {
		groupChangedListener.removeIf(request -> request.listenerEquals(listener));
	}

	/**
	 * Updates the seed
	 * <p>
	 * Notifies all {@link #groupChangedListener}, in case their group has changed
	 * </p>
	 *
	 * @param newSeed
	 * 		the new seed of this AbGroupProvider
	 */
	protected void updateSeed(long newSeed){
		long oldSeed = seed.getAndSet(newSeed);
		if (oldSeed == newSeed || groupChangedListener.isEmpty()) {
			return;
		}
		//Notify listener
		AbGroupProvider oldProvider = new AbGroupProvider(oldSeed);
		for (AbGroupListenerRequest listener : groupChangedListener) {
			int newGroup = getGroup(listener.getNumberOfGroups());
			int oldGroup = oldProvider.getGroup(listener.getNumberOfGroups());
			listener.groupUpdated(newGroup, oldGroup);
		}
	}

	/**
	 * Registers a change listener to the LicenseManager and updates the seed, in case the LicenseManager is available
	 *
	 * @return {@code this}
	 */
	private synchronized AbGroupProvider registerLicenseManagerListenerIfPossible() {
		LicenseManager licenseManager = LicenseManagerRegistry.INSTANCE.get();
		if (!isRegisteredToLicenseManager && licenseManager != null) {
			licenseManager.registerLicenseManagerListener(new UpdateSeedOnLicenseChange(this));
			updateSeed(IdentifierProvider.getIdentifier());
			isRegisteredToLicenseManager = true;
		}
		return this;
	}

}