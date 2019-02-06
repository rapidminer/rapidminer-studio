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
package com.rapidminer.gui;

import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;


/**
 * Factory that creates a list of menu items. Wraps an {@link Action} (simple item), a {@link JMenu}
 * (sub-menu), or an arbitrary {@link JComponent} (custom entries).
 *
 * @author Michael Knopf
 * @since 7.0.0
 */
public interface MenuItemFactory {

	/**
	 * Wrapper for a single menu entry.
	 */
	static final class MenuEntry {

		private final Action action;
		private final JComponent component;
		private final JMenu menu;

		/**
		 * Creates a wrapper for a single item.
		 *
		 * @param action
		 *            the action of the menu item
		 */
		public MenuEntry(Action action) {
			this.action = action;
			this.component = null;
			this.menu = null;
		}

		/**
		 * Creates a wrapper for both {@link JMenu}s and general {@link JComponent}s.
		 *
		 * @param component
		 *            the sub-menu or custom component
		 */
		public MenuEntry(JComponent component) {
			this.action = null;
			if (component instanceof JMenu) {
				this.component = null;
				this.menu = (JMenu) component;
			} else {
				this.component = component;
				this.menu = null;
			}
		}

		public boolean isMenu() {
			return menu != null;
		}

		public boolean isAction() {
			return action != null;
		}

		public boolean isComponent() {
			return component != null;
		}

		public Action getAction() {
			if (action == null) {
				throw new UnsupportedOperationException();
			}
			return action;
		}

		public JMenu getMenu() {
			if (menu == null) {
				throw new UnsupportedOperationException();
			}
			return menu;
		}

		public JComponent getComponent() {
			if (component == null) {
				throw new UnsupportedOperationException();
			}
			return component;
		}

	}

	/**
	 * @return The list of menu entries, may be empty but must not be {@code null}.
	 */
	public List<MenuEntry> create();

}
