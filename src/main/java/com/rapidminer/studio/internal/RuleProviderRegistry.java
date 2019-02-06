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
package com.rapidminer.studio.internal;

import java.security.AccessController;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import com.rapidminer.security.PluginSandboxPolicy;


/**
 * Registry for {@link RuleProvider}.
 *
 * @author Jonas Wilms-Pfau
 * @since 7.5
 */
public enum RuleProviderRegistry {

	INSTANCE;

	/** Static remote rules */
	public static final int PRECEDENCE_REMOTE = 50;
	/** License depending rules from nexus */
	public static final int PRECEDENCE_NEXUS = 100;
	/** Local rules for testing */
	public static final int PRECEDENCE_LOCAL = 150;

	private static final class Ordering {

		private static final Comparator<Integer> DESC = (a, b) -> Integer.compare(b, a);
	}

	/** Descending sorted map */
	private SortedMap<Integer, RuleProvider> ruleProvider = Collections.synchronizedSortedMap(new TreeMap<>(Ordering.DESC));

	/**
	 * Registers the rule provider.
	 *
	 * @param provider
	 *            the rule provider to register
	 * @param precedence
	 *            the importance of this provider
	 * @throws SecurityException
	 *             if caller does not have {@link RuntimePermission} for
	 *             {@code accessClassInPackage.rapidminer.internal}
	 */
	public void register(RuleProvider provider, int precedence) {
		if (System.getSecurityManager() != null) {
			AccessController.checkPermission(new RuntimePermission(PluginSandboxPolicy.RAPIDMINER_INTERNAL_PERMISSION));
		}
		if (provider == null) {
			throw new IllegalArgumentException("provider cannot be null");
		}
		while (ruleProvider.keySet().contains(precedence)) {
			precedence--;
		}
		if (!this.ruleProvider.containsValue(provider)) {
			this.ruleProvider.put(precedence, provider);
		}

	}

	/**
	 * Getter for the registered {@link RuleProvider}.
	 *
	 * @return The the registered rule Provider ordered by precedence
	 */
	public Collection<RuleProvider> getRuleProvider() {
		return ruleProvider.values();
	}

}
