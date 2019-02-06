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
package com.rapidminer.gui.actions;

import java.util.List;

import javax.swing.Action;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceMenu;


/**
 * Factory to create {@link OperatorActionContext} specific {@link Action}s.
 *
 * @author Michael Knopf
 * @since 6.5
 */
public interface OperatorActionFactory {

	/**
	 * Wrapper class for {@link ResourceAction} and {@link ResourceMenu} instances. Intended to be
	 * used only in conjunction with an {@link OperatorActionFactory}.
	 *
	 * @author Michael Knopf
	 */
	static final class ResourceEntry {

		private final ResourceAction action;
		private final ResourceMenu menu;

		/**
		 * Creates a new {@code ResourceEntry} wrapping the given {@link ResourceAction}.
		 *
		 * @param action
		 *            the resource action to be wrapped
		 */
		public ResourceEntry(ResourceAction action) {
			this.action = action;
			this.menu = null;
		}

		/**
		 * Creates a new {@code ResourceEntry} wrapping the given {@link ResourceMenu}.
		 *
		 * @param menu
		 *            the resource menu to be wrapped
		 */
		public ResourceEntry(ResourceMenu menu) {
			this.action = null;
			this.menu = menu;
		}

		/**
		 * Tells whether the entry wraps a {@link ResourceMenu}.
		 *
		 * @return {@code true] iff the the entry wraps a resource menu, {@code false} iff the entry
		 *         wraps a resource action
		 */
		public boolean isMenu() {
			return menu != null;
		}

		/**
		 * Returns the wrapped {@link ResourceAction} (if any).
		 *
		 * @return the resource action
		 * @throws UnsupportedOperationException
		 *             if the entry does not wrap a resource action
		 */
		public ResourceAction getAction() {
			if (action == null) {
				throw new UnsupportedOperationException();
			}
			return action;
		}

		/**
		 * Returns the wrapped {@link ResourceMenu} (if any).
		 *
		 * @return the wrapped menu
		 * @throws UnsupportedOperationException
		 *             if the entry does not wrap a resource menu
		 */
		public ResourceMenu getMenu() {
			if (menu == null) {
				throw new UnsupportedOperationException();
			}
			return menu;
		}

	}

	/**
	 * Creates a list of {@link ResourceEntry} instances ({@link ResourceAction}s and
	 * {@link ResourceMenu}s) for the given {@link OperatorActionContext}.
	 * <p>
	 * Implementations must not return {@code null} but are allowed to return empty lists.
	 * <p>
	 * Implementations may reuse and pool resources.
	 *
	 * @param context
	 *            the action context
	 * @return the list of resource entries
	 * @since 6.5
	 */
	public List<ResourceEntry> create(OperatorActionContext context);
}
