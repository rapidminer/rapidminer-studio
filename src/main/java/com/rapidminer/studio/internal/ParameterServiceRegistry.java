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

import com.rapidminer.security.PluginSandboxPolicy;
import com.rapidminer.tools.ParameterService;


/**
 * Registry for a {@link ParameterServiceProvider}.
 *
 * @author Marcel Michel
 * @since 7.2.0
 */
public enum ParameterServiceRegistry {

	INSTANCE;

	private ParameterServiceProvider provider;

	/**
	 * Registers the provider.
	 *
	 * Note: Only one registration is allowed. All following request will result in an
	 * {@link IllegalStateException}.
	 *
	 * @param provider
	 *            the provider to register
	 * @throws SecurityException
	 *             if caller does not have {@link RuntimePermission} for
	 *             {@code accessClassInPackage.rapidminer.internal}
	 */
	public void register(ParameterServiceProvider provider) {
		if (System.getSecurityManager() != null) {
			AccessController.checkPermission(new RuntimePermission(PluginSandboxPolicy.RAPIDMINER_INTERNAL_PERMISSION));
		}
		if (provider == null) {
			throw new IllegalArgumentException("Provider cannot be null");
		}
		if (this.provider != null) {
			throw new IllegalStateException("Provider already defined");
		}
		this.provider = provider;
	}

	/**
	 * If a {@link #provider} is defined it will query the registered provider. Otherwise the
	 * {@link ParameterService} will be used.
	 *
	 * @return The value of the given parameter or {@code null} if this parameter is unknown.
	 */
	public String getParameterValue(String key) {
		if (provider == null) {
			return ParameterService.getParameterValue(key);
		} else {
			return provider.getParameterValue(key);
		}
	}
}
