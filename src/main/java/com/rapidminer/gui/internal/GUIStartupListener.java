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
package com.rapidminer.gui.internal;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;


/**
 * A listener that is notified on GUI startup events. It can be registered to {@link RapidMinerGUI}
 * before the startup of RapidMiner.
 * <p>
 * This is an internal interface and might be changed or removed without further notice.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public interface GUIStartupListener {

	/**
	 * Will be called right before the Splash screen is shown.
	 */
	void splashWillBeShown();

	/**
	 * Will be called after the {@link MainFrame} has been initialized and after all plugin GUIs
	 * have been initialized but before the {@link MainFrame} is shown.
	 *
	 * @param mainFrame
	 *            the {@link MainFrame}
	 */
	void mainFrameInitialized(MainFrame mainFrame);

	/**
	 * Will be called right after the Splash screen was hidden.
	 */
	void splashWasHidden();

	/**
	 * Will be called at the end of the GUI startup.
	 */
	void startupCompleted();

}
