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
package com.rapidminer.gui.tools;

import java.io.IOException;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tutorial.Tutorial;
import com.rapidminer.tutorial.TutorialManager;
import com.rapidminer.tutorial.gui.TutorialBrowser;


/**
 * Utility methods for {@link Tutorial}s.
 *
 * @author Marcel Michel
 * @since 7.2.0
 */
public class TutorialTools {

	/**
	 * Opens the given {@link Tutorial} in a {@link ProgressThread}.
	 *
	 * @param tutorial
	 *            the tutorial which should be opened
	 */
	public static void openTutorial(final Tutorial tutorial) {
		if (!RapidMinerGUI.getMainFrame().close()) {
			return;
		}
		new ProgressThread("open_tutorial") {

			@Override
			public void run() {
				try {
					MainFrame mainFrame = RapidMinerGUI.getMainFrame();
					Process tutorialProcess = tutorial.makeProcess();
					mainFrame.setOpenedProcess(tutorialProcess);
					mainFrame.getTutorialSelector().setSelectedTutorial(tutorial);
					TutorialManager.INSTANCE.completedTutorial(tutorial.getIdentifier());

					DockingTools.openDockable(TutorialBrowser.TUTORIAL_BROWSER_DOCK_KEY, null, TutorialBrowser.POSITION);
				} catch (RuntimeException | MalformedRepositoryLocationException | IOException | XMLException e) {
					SwingTools.showSimpleErrorMessage("cannot_open_tutorial", e, tutorial.getTitle(), e.getMessage());
				}
			}
		}.start();
	}

}
