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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.processeditor.ProcessLogTab;
import com.rapidminer.gui.processeditor.results.ResultDisplay;
import com.rapidminer.gui.processeditor.results.ResultTab;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.gui.tools.ScrollableJPopupMenu;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.DockingContext;
import com.vlsolutions.swing.docking.DockingDesktop;
import com.vlsolutions.swing.docking.DummyDockable;


/**
 * This menu is specialized to show all {@link com.vlsolutions.swing.docking.Dockable Dockables}, better known as Panels,
 * except some which are registered to be hidden via {@link DockableMenu#registerHideInDockableMenuPrefix(String)}. The
 * {@link javax.swing.JPopupMenu} which contains the entries is scrollable.
 *
 * @author Simon Fischer, Andreas Timm
 */
public class DockableMenu extends ResourceMenu {

	private static final long serialVersionUID = -5602297374075268751L;

	private static final List<String> HIDE_IN_DOCKABLE_MENU_PREFIX_REGISTRY = new LinkedList<>();

	private static final String DOCKABLE_HTML = "<html><div style='margin-left:5'><b>%s</b><br/>%s</div></html>";

	/**
	 * This definition of maximum visible entries will be used to calculate the height of the popup
	 */
	private static final int MAX_SHOWN_ITEMS = 18;

	/**
	 * Here you can register prefixes that will be used to test if a {@link DockableState} start
	 * with the provided prefix and thus won't be shown in the created {@link DockableMenu}.
	 *
	 * @param prefix
	 * 		the prefix of {@link DockableState} {@link DockKey}s to hide
	 */
	public static void registerHideInDockableMenuPrefix(String prefix) {
		HIDE_IN_DOCKABLE_MENU_PREFIX_REGISTRY.add(prefix);
	}

	static {
		registerHideInDockableMenuPrefix(ResultTab.DOCKKEY_PREFIX);
		registerHideInDockableMenuPrefix(ProcessLogTab.DOCKKEY_PREFIX);
	}

	private final DockingContext dockingContext;

	public DockableMenu(DockingContext dockingContext) {
		super("show_view");
		this.dockingContext = dockingContext;
		ensurePopupMenuCreated();
		configureScrollable();
	}

	/**
	 * Tries to set the popup menu to an instance of {@link ScrollableJPopupMenu} using reflection. If that fails,
	 * the default {@link JPopupMenu} will be used.
	 * <p>
	 * This is inspired by {@link JMenu#ensurePopupMenuCreated()} which sadly is private.
	 *
	 * @since 8.2
	 */
	private void ensurePopupMenuCreated() {
		// OSX special handling
		if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
			// get popup menu once to trigger creation in parent
			getPopupMenu();
			return;
		}
		Field popupMenuField;
		ScrollableJPopupMenu popupMenu = new ScrollableJPopupMenu(ScrollableJPopupMenu.SIZE_HUGE);
		popupMenu.setInvoker(this);
		WinListener popupListener = new WinListener(popupMenu);
		// set listener first; if something goes wrong later, the private field will jsut be replaced in super method
		try {
			Field popupListenerField = JMenu.class.getDeclaredField("popupListener");
			popupListenerField.setAccessible(true);
			popupListenerField.set(this, popupListener);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.DockableMenu.listener_error_make_popup_scrollable");
			return;
		}
		// set popup second; if the set operation does not succeed, the private field will stay at null,
		// since this is invoked from the constructor before any call to the super method could be made.
		try {
			popupMenuField = JMenu.class.getDeclaredField("popupMenu");
			popupMenuField.setAccessible(true);
			popupMenuField.set(this, popupMenu);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.DockableMenu.popup_setting_error");
		}
	}

	/**
	 * Configures the {@link JScrollPane} of the {@link ScrollableJPopupMenu} if set,
	 * namely forbids the usage of the horizontal scrollbar.
	 */
	private void configureScrollable() {
		JPopupMenu popupMenu = getPopupMenu();
		if (!(popupMenu instanceof ScrollableJPopupMenu)) {
			return;
		}
		JScrollPane scrollPane = ((ScrollableJPopupMenu) popupMenu).getScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	}

	void fill() {
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
			String keyName = dockKey.getKey();
			boolean cont = false;
			for (String prefix : HIDE_IN_DOCKABLE_MENU_PREFIX_REGISTRY) {
				if (keyName.startsWith(prefix)) {
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
			item.addActionListener(e -> toggleState(state));

			// special handling for results overview dockable in Results perspective
			// and process view dockable in Design perspective
			// these dockables are not allowed to be closed so we disable this item while in respective perspective
			String perspectiveName = RapidMinerGUI.getMainFrame().getPerspectiveController().getModel().getSelectedPerspective().getName();
			if ((PerspectiveModel.RESULT.equals(perspectiveName) && ResultDisplay.RESULT_DOCK_KEY.equals(keyName))
					|| (PerspectiveModel.DESIGN.equals(perspectiveName) && ProcessPanel.PROCESS_PANEL_DOCK_KEY.equals(keyName))) {
				item.setEnabled(false);
				item.setToolTipText(I18N.getGUIMessage("gui.label.dockable.unclosable.tip"));
			}
			add(item);
			ensurePopupHeight();
		}
	}

	/**
	 * Ensures that the correct maximum height is set for the popup menu and sets the scroll increment.
	 * Will only take effect with the first item added. Makes sure the height is set to {@link #MAX_SHOWN_ITEMS}*itemHeight.
	 *
	 * @since 8.2
	 */
	private void ensurePopupHeight() {
		JPopupMenu popupMenu = getPopupMenu();
		if (popupMenu.getSubElements().length != 1 || !(popupMenu instanceof ScrollableJPopupMenu)) {
			return;
		}
		ScrollableJPopupMenu scrollablePopup = (ScrollableJPopupMenu) popupMenu;
		int itemHeight = scrollablePopup.getComponentsInsideScrollpane()[0].getPreferredSize().height;
		int maxHeight = MAX_SHOWN_ITEMS * itemHeight;
		maxHeight = Math.min(maxHeight, ScrollableJPopupMenu.SIZE_HUGE);
		scrollablePopup.setMaxHeight(maxHeight);
		JScrollPane scrollPane = scrollablePopup.getScrollPane();
		JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
		verticalScrollBar.setUnitIncrement(itemHeight);
		verticalScrollBar.setBlockIncrement(maxHeight);
	}

	/**
	 * Toggle the state of this {@link DockableState} component
	 *
	 * @param state
	 * 		component to toggle the state of
	 * @since 8.2
	 */
	private void toggleState(DockableState state) {
		if (state == null) {
			return;
		}
		final DockingDesktop dockingDesktop = dockingContext.getDesktopList().get(0);
		if (state.isClosed()) {
			dockingDesktop.addDockable(state.getDockable());
		} else {
			dockingDesktop.close(state.getDockable());
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
