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
package com.rapidminer.tutorial.gui;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JButton;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.PerspectiveChangeListener;
import com.rapidminer.gui.PerspectiveModel;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.processeditor.ExtendedProcessEditor;
import com.rapidminer.gui.tools.DockingTools;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tutorial.Tutorial;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.event.DockableStateWillChangeEvent;
import com.vlsolutions.swing.docking.event.DockableStateWillChangeListener;


/**
 * Model which holds a {@link Tutorial} and notifies {@link Observer}s.
 *
 * @since 7.0.0
 * @author Marcel Michel
 */
public class TutorialSelector extends AbstractObservable<Tutorial> {

	/**
	 * Dialog to confirm the closing of the tutorial browser. The dialog displays three options: (1)
	 * Leave tutorial and start from scratch, (2) Leave tutorial and continue with current process,
	 * and (3) cancel and continue with the tutorial.
	 *
	 * @author Michael Knopf
	 * @since 7.0.0
	 */
	static class LeaveTutorialDialog extends ConfirmDialog {

		private static final long serialVersionUID = 1L;

		public LeaveTutorialDialog() {
			super(RapidMinerGUI.getMainFrame(), "close_tutorial_browser", ConfirmDialog.YES_NO_CANCEL_OPTION, false);
		}

		@Override
		protected JButton makeYesButton() {
			return new JButton(new ResourceAction("close_tutorial_browser.new") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					LeaveTutorialDialog.this.setReturnOption(YES_OPTION);
					LeaveTutorialDialog.this.setVisible(false);
				}

			});
		};

		@Override
		protected JButton makeNoButton() {
			return new JButton(new ResourceAction("close_tutorial_browser.keep") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					LeaveTutorialDialog.this.setReturnOption(NO_OPTION);
					LeaveTutorialDialog.this.setVisible(false);
				}

			});
		};

	}

	/** the observed mainFrame */
	private final MainFrame mainFrame;

	/** the observed perspective model */
	private final PerspectiveModel perspectiveModel;

	/** the current tutorial */
	private Tutorial selectedTutorial;

	/** listener for process change events of the {@link #mainFrame} */
	private ExtendedProcessEditor processListener;

	/** listener for perspective change events of the {@link #perspectiveModel} */
	private PerspectiveChangeListener perspectiveListener;

	public TutorialSelector(MainFrame mainFrame, PerspectiveModel perspectveModel) {
		this.mainFrame = mainFrame;
		this.perspectiveModel = perspectveModel;
		registerListeners();
	}

	/**
	 * Registers the listeners to observe the process.
	 */
	private void registerListeners() {
		processListener = new ExtendedProcessEditor() {

			@Override
			public void setSelection(List<Operator> selection) {
				// nothing to do
			}

			@Override
			public void processUpdated(Process process) {
				// nothing to do
			}

			@Override
			public void processChanged(Process process) {
				// check if the process is not the tutorial process anymore
				// since a new process is created on every process change, we check the redo and
				// undo steps, which are reset when a new process is opened
				MainFrame mainFrame = RapidMinerGUI.getMainFrame();
				if (mainFrame != null && process.getRootOperator() != null
						&& process.getRootOperator().getUserData(Tutorial.KEY_USER_DATA_FLAG) == null) {
					// If getUserData(Tutorial.KEY_USER_DATA_FLAG) == null the current process is
					// not considered to be a tutorial process. Thus, leave the tutorial.
					setSelectedTutorial(null);
					closeAllTutorialBrowsers();
				}
			}

			@Override
			public void processViewChanged(Process process) {
				// nothing to do
			}
		};
		mainFrame.addExtendedProcessEditor(processListener);

		perspectiveListener = new PerspectiveChangeListener() {

			@Override
			public void perspectiveChangedTo(Perspective perspective) {
				if (PerspectiveModel.RESULT.equals(perspective.getName())
						|| PerspectiveModel.DESIGN.equals(perspective.getName())) {
					if (getSelectedTutorial() != null) {
						// the user has opened a tutorial, ensure that the tutorial browser is
						// displayed
						SwingTools.invokeLater(() -> DockingTools.openDockable(TutorialBrowser.TUTORIAL_BROWSER_DOCK_KEY, null,
								TutorialBrowser.POSITION));
					} else {
						// no tutorial selected, ensure that no tutorial browser is displayed
						closeAllTutorialBrowsers();
					}
				}
			}
		};
		perspectiveModel.addPerspectiveChangeListener(perspectiveListener);

		DockableStateWillChangeListener listener = new DockableStateWillChangeListener() {

			@Override
			public void dockableStateWillChange(DockableStateWillChangeEvent event) {
				if (getSelectedTutorial() == null) {
					// no tutorial selected -> close right away
					return;
				}

				DockableState current = event.getCurrentState();
				DockableState future = event.getFutureState();

				if (current.getDockable().getDockKey().getKey().equals(TutorialBrowser.TUTORIAL_BROWSER_DOCK_KEY)
						&& future.isClosed()) {

					LeaveTutorialDialog dialog = new LeaveTutorialDialog();

					dialog.setVisible(true);

					if (dialog.getReturnOption() == ConfirmDialog.CANCEL_OPTION) {
						event.cancel();
					} else {
						setSelectedTutorial(null);
						// this event will only close the panel in the current perspective
						mainFrame.getPerspectiveController().removeFromInvisiblePerspectives(current.getDockable());
						if (dialog.getReturnOption() == ConfirmDialog.YES_OPTION) {
							// start with new process (discard unsaved work without warning)
							mainFrame.newProcess(false);
						} else {
							// Keep process but remove the tutorial process flag and the fire remove
							// background event (to remove tutorial background).
							ProcessRootOperator rootOperator = mainFrame.getProcess().getRootOperator();
							rootOperator.setUserData(Tutorial.KEY_USER_DATA_FLAG, null);
							mainFrame.getProcessPanel().getBackgroundImageHandler()
									.makeRemoveBackgroundImageAction(rootOperator.getSubprocess(0)).actionPerformed(null);
						}
					}

					dialog.dispose();
				}
			}
		};
		mainFrame.getDockingDesktop().addDockableStateWillChangeListener(listener);
	}

	/**
	 * @return the selected tutorial
	 */
	public Tutorial getSelectedTutorial() {
		return selectedTutorial;
	}

	/**
	 * Updates the selected tutorial and notifies the registered observers.
	 */
	public void setSelectedTutorial(Tutorial selectedTutorial) {
		this.selectedTutorial = selectedTutorial;
		fireUpdate(selectedTutorial);
	}

	/**
	 * Attempts to close all instances of the {@link TutorialBrowser} (both visible and hidden).
	 */
	private void closeAllTutorialBrowsers() {
		DockableState state = DockingTools.getDockableState(TutorialBrowser.TUTORIAL_BROWSER_DOCK_KEY);
		if (state != null && !state.isClosed()) {
			SwingTools.invokeLater(() -> {
				Dockable browser = state.getDockable();
				mainFrame.getDockingDesktop().close(browser);
				mainFrame.getPerspectiveController().removeFromInvisiblePerspectives(browser);
			});
		}
	}
}
