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
package com.rapidminer.gui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.rapidminer.gui.processeditor.ProcessLogTab;
import com.rapidminer.gui.processeditor.results.ResultDisplay;
import com.rapidminer.gui.processeditor.results.ResultTab;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.DockingContext;
import com.vlsolutions.swing.docking.DummyDockable;


/**
 *
 * @author Simon Fischer
 */
public class DockableMenu extends ResourceMenu {

	private static final long serialVersionUID = -5602297374075268751L;

	private static final List<String> HIDE_IN_DOCKABLE_MENU_PREFIX_REGISTRY = new LinkedList<>();

	private static final String DOCKABLE_HTML = "<html><div style='margin-left:5'><b>%s</b><br/>%s</div></html>";

	/**
	 * Here you can register prefixes that will be used to test if a {@link DockableState} start
	 * with the provided prefix and thus won't be shown in the created {@link DockableMenu}.
	 *
	 * @param prefix
	 *            the prefix of {@link DockableState} {@link DockKey}s to hide
	 */
	public static void registerHideInDockableMenuPrefix(String prefix) {
		HIDE_IN_DOCKABLE_MENU_PREFIX_REGISTRY.add(prefix);
	}

	static {
		registerHideInDockableMenuPrefix(ResultTab.DOCKKEY_PREFIX);
		registerHideInDockableMenuPrefix(ProcessLogTab.DOCKKEY_PREFIX);
	}

	// private DockingDesktop dockingDesktop;
	private final DockingContext dockingContext;

	public DockableMenu(DockingContext dockingContext) {
		super("show_view");
		this.dockingContext = dockingContext;
		addMenuListener(new MenuListener() {

			@Override
			public void menuCanceled(MenuEvent e) {}

			@Override
			public void menuDeselected(MenuEvent e) {}

			@Override
			public void menuSelected(MenuEvent e) {
				fill();
			}
		});
	}

	private void fill() {
		removeAll();
		DockableState[] dockables = dockingContext.getDesktopList().get(0).getDockables();
		List<DockableState> sorted = new LinkedList<>();
		sorted.addAll(Arrays.asList(dockables));
		sorted.sort(Comparator.comparing(o -> o.getDockable().getDockKey().getName()));
		for (final DockableState state : sorted) {
			if (state.getDockable() instanceof DummyDockable) {
				continue;
			}
			DockKey dockKey = state.getDockable().getDockKey();
			boolean cont = false;
			for (String prefix : HIDE_IN_DOCKABLE_MENU_PREFIX_REGISTRY) {
				if (dockKey.getKey().startsWith(prefix)) {
					cont = true;
					break;
				}
			}
			if (cont) {
				continue;
			}
			String description = null;
			if (dockKey instanceof ResourceDockKey) {
				description = ((ResourceDockKey) dockKey).getShortDescription();
			}
			description = description != null ? description : "";
			String text = dockKey.getName();
			if (SystemInfoUtilities.getOperatingSystem() != OperatingSystem.OSX) {
				// OS X cannot use html in menus so only do it for other OS
				text = String.format(DOCKABLE_HTML, dockKey.getName(), description);
			}
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(text, dockKey.getIcon());

			item.setSelected(!state.isClosed());
			item.addActionListener(e -> {

				if (state.isClosed()) {
					dockingContext.getDesktopList().get(0).addDockable(state.getDockable());
				} else {
					dockingContext.getDesktopList().get(0).close(state.getDockable());
				}
			});

			// special handling for results overview dockable in Results perspective
			// this dockable is not allowed to be closed so we disable this item while in said
			// perspective
			if (RapidMinerGUI.getMainFrame().getPerspectiveController().getModel().getSelectedPerspective().getName()
					.equals(PerspectiveModel.RESULT)
					&& ResultDisplay.RESULT_DOCK_KEY.equals(state.getDockable().getDockKey().getKey())) {
				item.setEnabled(false);
			}

			add(item);
		}

	}

	public DockingContext getDockingContext() {
		return dockingContext;
	}


	/**
	 * Checks if the given dock key belongs to a dockable that should not appear in the dockable menu.
	 *
	 * @param dockKey
	 * 		the key to test
	 * @return {@code true} if the key belongs to a dockable that is hidden from the menu; {@code false} otherwise
	 * @since 8.1
	 */
	public static boolean isDockableHiddenFromMenu(final DockKey dockKey) {
		for (String prefix : HIDE_IN_DOCKABLE_MENU_PREFIX_REGISTRY) {
			if (dockKey.getKey().startsWith(prefix)) {
				return true;
			}
		}

		return false;
	}
}
