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

/**
 * Allows to change the A/B group for testing or development purposes
 *
 * @author Jonas Wilms-Pfau
 * @since 8.2
 */
public final class AbGroupChanger {

	private static final int ONE_MILLION = 1_000_000;

	private AbGroupChanger() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Changes the A/B group of the {@link AbGroupProvider#get global AbGroupProvider} by brute forcing the seed
	 *
	 * @param targetGroup
	 * 		the group that should be selected [0, numberOfGroups)
	 * @param numberOfGroups
	 * 		number of ab groups &gt; 1
	 * @throws IllegalArgumentException
	 * 		if targetGroup &lt; 0 or &gt;= numberOfGroups
	 * @throws BruteForceFailedException
	 * 		if the target group could not be set
	 */
	public static void changeGroup(int targetGroup, int numberOfGroups) {
		changeGroup(targetGroup, numberOfGroups, AbGroupProvider.get());
	}

	/**
	 * Changes the A/B group of the given provider by brute forcing the seed
	 *
	 * @param targetGroup
	 * 		the group that should be selected [0, numberOfGroups)
	 * @param numberOfGroups
	 * 		number of ab groups &gt; 1
	 * @param provider
	 * 		the provider that should get updated
	 * @throws IllegalArgumentException
	 * 		if targetGroup &lt; 0 or &gt;= numberOfGroups
	 * @throws BruteForceFailedException
	 * 		if the target group could not be set
	 */
	public static void changeGroup(int targetGroup, int numberOfGroups, AbGroupProvider provider) {
		if (numberOfGroups < 1) {
			throw new IllegalArgumentException("numberOfGroups must be greater than 0");
		}
		if (targetGroup < 0 || targetGroup >= numberOfGroups) {
			throw new IllegalArgumentException("targetGroup must be greater than or equal 0 and less than numberOfGroup");
		}
		if (provider.getGroup(numberOfGroups) == targetGroup) {
			return;
		}
		AbGroupProvider dummyProvider = new AbGroupProvider(0);
		for (long seed = 0; seed < ONE_MILLION; seed++) {
			dummyProvider.updateSeed(seed);
			if (dummyProvider.getGroup(numberOfGroups) == targetGroup) {
				provider.updateSeed(seed);
				return;
			}
		}
		throw new BruteForceFailedException("could not brute force the group in one million iterations");
	}

	/**
	 * Thrown if the brute force attempt has failed.
	 *
	 * @author Jonas Wilms-Pfau
	 * @since 8.2
	 */
	public static class BruteForceFailedException extends RuntimeException {

		/**
		 * Constructs a new BruteForceFailedException with the given message
		 *
		 * @param message
		 * 		the detail message
		 * @see RuntimeException#RuntimeException(String)
		 */
		BruteForceFailedException(String message) {
			super(message);
		}
	}
}
