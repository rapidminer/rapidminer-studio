/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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

import java.security.AccessControlException;
import java.security.AccessController;

import com.rapidminer.Process;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.UserData;
import com.rapidminer.security.PluginSandboxPolicy;
import com.rapidminer.studio.concurrency.internal.ConcurrencyExecutionService;
import com.rapidminer.studio.concurrency.internal.StudioConcurrencyContext;


/**
 * Provides utility methods to access feature from the new API that cannot be injected directly yet.
 * Note that this class is supposed to be removed once the injection is implemented.
 *
 * @author Gisa Schaefer, Michael Knopf
 * @since 6.2.0
 */
public class Resources {

	private static final String USER_DATA_KEY = "com.rapidminer.core.concurrency.ContextUserData";

	/**
	 * Wrapper to store {@link ConcurrencyContext} within the root operator of a process.
	 *
	 * @author Michael Knopf
	 */
	private static class ContextUserData implements UserData<Object> {

		private final ConcurrencyContext context;

		private ContextUserData(ConcurrencyContext context) {
			this.context = context;
		}

		@Override
		public UserData<Object> copyUserData(Object newParent) {
			return this;
		}

		private ConcurrencyContext getContext() {
			return this.context;
		}

	}

	/**
	 * Wrapper to store {@link ConcurrencyContext} within the root operator of a process.
	 *
	 * </p> Internal, do not use. Throws {@link UnsupportedOperationException} if used by 3rd parties.
	 *
	 * @author Marco Boeck
	 */
	public static class OverridingContextUserData implements UserData<Object> {

		private final ConcurrencyContext context;

		public OverridingContextUserData(ConcurrencyContext context) {
			// make sure this cannot be called without RapidMiner internal permissions
			try {
				if (System.getSecurityManager() != null) {
					AccessController.checkPermission(new RuntimePermission(PluginSandboxPolicy.RAPIDMINER_INTERNAL_PERMISSION));
				}
			} catch (AccessControlException e) {
				throw new UnsupportedOperationException("Internal API, cannot be called by unauthorized sources.");
			}
			this.context = context;
		}

		@Override
		public final UserData<Object> copyUserData(Object newParent) {
			return this;
		}

		private ConcurrencyContext getContext() {
			return this.context;
		}

	}

	/**
	 * Provides a {@link ConcurrencyContext} for the given {@link Operator}.
	 *
	 * @param operator
	 *            the operator
	 * @return the context
	 */
	public static ConcurrencyContext getConcurrencyContext(Operator operator) {
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null");
		}

		// if anyone has set a ConcurrencyContext that should override the regular ones, use it
		// currently used by RapidMiner Server web services
		ProcessRootOperator rootOperator = operator.getProcess().getRootOperator();
		if (rootOperator.getUserData(ConcurrencyExecutionService.OVERRIDING_CONTEXT) != null) {
			OverridingContextUserData data = (OverridingContextUserData) rootOperator.getUserData(ConcurrencyExecutionService.OVERRIDING_CONTEXT);
			return data.getContext();
		}

		Operator root = operator.getRoot();
		if (root.getUserData(USER_DATA_KEY) != null) {
			ContextUserData data = (ContextUserData) root.getUserData(USER_DATA_KEY);
			return data.getContext();
		}

		Process process = operator.getProcess();
		StudioConcurrencyContext context = new StudioConcurrencyContext(process);
		ContextUserData data = new ContextUserData(context);
		root.setUserData(USER_DATA_KEY, data);
		return context;
	}
}
