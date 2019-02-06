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
package com.rapidminer.security;

import java.security.Permission;


/**
 * The security manager used by RM Studio to restrict permissions for unsigned extensions. See
 * {@link PluginSandboxPolicy} for more details.
 *
 * @author Marco Boeck
 * @since 7.2
 *
 */
public final class PluginSecurityManager extends SecurityManager {

	@Override
	public final void checkPermission(Permission perm) {
		if (perm instanceof RuntimePermission) {
			// prevent ANY code from changing the SecurityManager after it has been installed
			if ("setSecurityManager".equals(perm.getName())) {
				throw new SecurityException("SecurityManager cannot be replaced!");
			}
		}
		super.checkPermission(perm);
	}

}
