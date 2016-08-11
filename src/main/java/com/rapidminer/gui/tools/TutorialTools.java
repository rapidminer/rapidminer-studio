/**
 * Copyright (C) 2001-2016 RapidMiner GmbH
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
					mainFrame.setOpenedProcess(tutorialProcess, false, null);
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
