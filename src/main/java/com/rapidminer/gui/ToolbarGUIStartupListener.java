/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import com.rapidminer.gui.internal.GUIStartupListener;


/**
 * Update the {@link MainToolBar} once all extensions are loaded.
 *
 * @author Michael Knopf
 * @since 6.6
 */
public class ToolbarGUIStartupListener implements GUIStartupListener {

	@Override
	public void splashWillBeShown() {
		// ignore
	}

	@Override
	public void mainFrameInitialized(MainFrame mainFrame) {
		// ignore
	}

	@Override
	public void splashWasHidden() {
		RapidMinerGUI.getMainFrame().updateToolbar();
	}

	@Override
	public void startupCompleted() {
		// ignore
	}

}
