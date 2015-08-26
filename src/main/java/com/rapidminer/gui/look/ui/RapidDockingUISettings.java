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
package com.rapidminer.gui.look.ui;

import com.vlsolutions.swing.docking.ui.DockingUISettings;

import javax.swing.BorderFactory;
import javax.swing.UIManager;


/**
 * 
 * @author Simon Fischer
 */
public class RapidDockingUISettings extends DockingUISettings {

	/** installs the borders */
	@Override
	public void installBorderSettings() {
		UIManager.put("DockView.singleDockableBorder", null);
		UIManager.put("DockView.tabbedDockableBorder", null);
		UIManager.put("DockView.maximizedDockableBorder", null);
	}

	/** installs the DockVieTitleBar related properties */
	@Override
	public void installDockViewTitleBarSettings() {
		UIManager.put("DockViewTitleBarUI", "com.vlsolutions.swing.docking.ui.DockViewTitleBarUI");

		// TODO: internationalize strings
		UIManager.put("DockViewTitleBar.height", Integer.valueOf(20));
		UIManager.put("DockViewTitleBar.closeButtonText", UIManager.getString("InternalFrameTitlePane.closeButtonText"));
		UIManager.put("DockViewTitleBar.minimizeButtonText",
				UIManager.getString("InternalFrameTitlePane.minimizeButtonText"));
		UIManager.put("DockViewTitleBar.restoreButtonText", UIManager.getString("InternalFrameTitlePane.restoreButtonText"));
		UIManager.put("DockViewTitleBar.maximizeButtonText",
				UIManager.getString("InternalFrameTitlePane.maximizeButtonText"));
		UIManager.put("DockViewTitleBar.floatButtonText", "Detach");
		UIManager.put("DockViewTitleBar.attachButtonText", "Attach");

		// font to be used in the title bar
		UIManager.put("DockViewTitleBar.titleFont", UIManager.get("InternalFrame.titleFont"));

		// are buttons displayed or just accessible from the contextual menu ?
		// setting one of these flags to false hide the button from the title
		// bar
		// setting to true not necessarily shows the button, as it then depends
		// on the DockKey allowed states.
		UIManager.put("DockViewTitleBar.isCloseButtonDisplayed", Boolean.TRUE);
		UIManager.put("DockViewTitleBar.isHideButtonDisplayed", Boolean.TRUE);
		UIManager.put("DockViewTitleBar.isDockButtonDisplayed", Boolean.TRUE);
		UIManager.put("DockViewTitleBar.isMaximizeButtonDisplayed", Boolean.TRUE);
		UIManager.put("DockViewTitleBar.isRestoreButtonDisplayed", Boolean.TRUE);
		UIManager.put("DockViewTitleBar.isFloatButtonDisplayed", Boolean.TRUE);
		UIManager.put("DockViewTitleBar.isAttachButtonDisplayed", Boolean.TRUE);

		UIManager.put("DockViewTitleBar.border", BorderFactory.createEmptyBorder());
	}

	@Override
	public void installToolBarSettings() {
		super.installToolBarSettings();
	}
}
