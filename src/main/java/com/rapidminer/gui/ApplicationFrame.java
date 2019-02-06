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

import javax.swing.JFrame;

import com.rapidminer.gui.tools.StatusBar;


/**
 * This is a singelton for getting access to the main frame of an application.
 *
 * @author Simon Fischer
 */
public abstract class ApplicationFrame extends JFrame {

	private static final long serialVersionUID = -4888325793866511406L;

	private static ApplicationFrame applicationFrame = null;

	// The status bar of the application, usually displayed at the bottom
	// of the frame.
	private final StatusBar statusBar = new StatusBar();

	public ApplicationFrame(String title) {
		super(title);
		if (applicationFrame != null) {
			throw new RuntimeException("Can only have one application frame.");
		}
		applicationFrame = this;
	}

	/**
	 * Returns the status bar of the application.
	 *
	 * @return status bar
	 */
	public StatusBar getStatusBar() {
		return statusBar;
	}

	public static ApplicationFrame getApplicationFrame() {
		return applicationFrame;
	}
}
