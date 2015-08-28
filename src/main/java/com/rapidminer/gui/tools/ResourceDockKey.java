/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.vlsolutions.swing.docking.DockKey;

import java.util.logging.Level;

import javax.swing.Icon;


/**
 * This generates a DockKey from the GUI resource bundle. It supports several for properties: <br>
 * gui.dockkey.-key-.name which is the name <br>
 * gui.dockkey.-key-.tip which will be shown as tool tip when hovering over this dockables tab <br>
 * gui.dockkey.-key-.icon this icon is loaded from the 16er icons by prepending an 16/. It is shown
 * in the upper left corner of the tab.
 * 
 * By default the DockKey is created to be part of the ROOT dock group. If it should be part of the
 * results instead, you will have to call
 * {@link #setDockGroup(com.vlsolutions.swing.docking.DockGroup)} with the result constant from the
 * mainframe.
 * 
 * @author Simon Fischer, Sebastian Land
 */
public class ResourceDockKey extends DockKey {

	public ResourceDockKey(String resourceKey) {
		super(resourceKey);
		setName(getMessage(resourceKey + ".name"));
		setTooltip(getMessage(resourceKey + ".tip"));
		String iconName = getMessageOrNull(resourceKey + ".icon");
		if (iconName != null) {
			Icon icon = SwingTools.createIcon("16/" + iconName);
			if (icon != null) {
				setIcon(icon);
			} else {
				// LogService.getRoot().warning("Missing icon: "+iconName);
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.ResourceDockKey.missing_icon", iconName);
			}
		}
		setFloatEnabled(true);
		setCloseEnabled(true);
		setAutoHideEnabled(true);

		// setting default dock group to root: Must be overriden if should be result
		setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	private static String getMessage(String key) {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.dockkey." + key);
	}

	private static String getMessageOrNull(String key) {
		return I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.dockkey." + key);
	}

}
