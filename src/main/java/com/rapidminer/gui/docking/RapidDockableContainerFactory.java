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
package com.rapidminer.gui.docking;

import com.vlsolutions.swing.docking.DockViewAsTab;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.FloatingDockableContainer;
import com.vlsolutions.swing.docking.MaximizedDockViewAsTab;
import com.vlsolutions.swing.docking.SingleDockableContainer;
import com.vlsolutions.swing.docking.TabFactory;
import com.vlsolutions.swing.docking.TabbedDockView;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;


/**
 * Dockable container factory for docking framework, that specifically handles new floating
 * dockables.
 * 
 * @author Tobias Malbrecht
 */
public class RapidDockableContainerFactory extends TabFactory {

	@Override
	public SingleDockableContainer createDockableContainer(Dockable dockable, ParentType parentType) {
		switch (parentType) {
			case PARENT_TABBED_CONTAINER:
				return new TabbedDockView(dockable);
			case PARENT_DESKTOP:
				return new MaximizedDockViewAsTab(dockable);
			case PARENT_SPLIT_CONTAINER:
				return new DockViewAsTab(dockable);
			case PARENT_DETACHED_WINDOW:
				// specific floating dock view is anchored here
				return new DetachedDockViewAsTab(dockable);

			default:
				throw new RuntimeException("Wrong dockable container type");
		}
	}

	@Override
	public FloatingDockableContainer createFloatingDockableContainer(Window owner) {
		if (owner instanceof Dialog) {
			return new RapidFloatingDialog((Dialog) owner);
		} else {
			return new RapidFloatingDialog((Frame) owner);
		}
	}
}
