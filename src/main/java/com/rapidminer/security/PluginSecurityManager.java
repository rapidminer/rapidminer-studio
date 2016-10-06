/**
 * Copyright (C) 2001-2016 RapidMiner GmbH
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
