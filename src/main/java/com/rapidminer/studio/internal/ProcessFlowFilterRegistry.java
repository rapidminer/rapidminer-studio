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

import com.rapidminer.operator.execution.ProcessFlowFilter;
import com.rapidminer.security.PluginSandboxPolicy;


/**
 * Registry for a {@link ProcessFlowFilter}.
 *
 * @author Marcel Michel
 * @since 7.2.2
 */
public enum ProcessFlowFilterRegistry {

	INSTANCE;

	private ProcessFlowFilter filter;

	/**
	 * Registers the filter.
	 *
	 * Note: Only one registration is allowed. All following request will result in an
	 * {@link IllegalStateException}.
	 *
	 * @param filter
	 *            the filter to register
	 * @throws SecurityException
	 *             if caller does not have {@link RuntimePermission} for
	 *             {@code accessClassInPackage.rapidminer.internal}
	 */
	public void register(ProcessFlowFilter filter) {
		if (System.getSecurityManager() != null) {
			AccessController.checkPermission(new RuntimePermission(PluginSandboxPolicy.RAPIDMINER_INTERNAL_PERMISSION));
		}
		if (filter == null) {
			throw new IllegalArgumentException("Filter cannot be null");
		}
		if (this.filter != null) {
			throw new IllegalStateException("Filter already defined");
		}
		this.filter = filter;
	}

	/**
	 * Getter for the registered {@link ProcessFlowFilter}.
	 *
	 * @return The the registered filter or {@code null}
	 */
	public ProcessFlowFilter getProcessFlowFilter() {
		return filter;
	}
}
