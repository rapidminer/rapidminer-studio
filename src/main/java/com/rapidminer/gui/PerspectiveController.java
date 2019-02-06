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

import java.io.File;
import java.util.logging.Level;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.Timer;

import com.rapidminer.gui.actions.WorkspaceAction;
import com.rapidminer.gui.processeditor.ProcessLogTab;
import com.rapidminer.gui.processeditor.results.DockableResultDisplay;
import com.rapidminer.gui.processeditor.results.ResultTab;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.DockingContext;
import com.vlsolutions.swing.docking.DockingDesktop;
import com.vlsolutions.swing.docking.ws.WSDockKey;


/**
 * The {@link PerspectiveController} manages a {@link PerspectiveModel} to show, delete and
 * manipulates application {@link Perspective}s. The {@link PerspectiveModel} itself is an
 * {@link Observable} and can notify listeners about new registered perspectives and perspective
 * changes.
 *
 * @author Marcel Michel
 * @since 7.0.0
 */
public class PerspectiveController {

	private final DockingContext context;

	private final PerspectiveModel model;

	/** @since 8.2.1 */
	private volatile Perspective switchTo;
	/** @since 8.2.1 */
	private final Timer switchTimer;

	/**
	 * Creates a new {@link PerspectiveController} with the given docking context.
	 *
	 * @param context
	 *            the docking context which should be used
	 */
	public PerspectiveController(final DockingContext context) {
		this.context = context;
		this.model = new PerspectiveModel();
		context.setDockableResolver(key -> {
			if (key.startsWith(ResultTab.DOCKKEY_PREFIX)) {
				ResultTab tab = new ResultTab(key);
				tab.showResult(null);
				return tab;
			} else if (key.startsWith(ProcessLogTab.DOCKKEY_PREFIX)) {
				ProcessLogTab tab = new ProcessLogTab(key);
				tab.setDataTableViewer(null);
				return tab;
			} else {
				return null;
			}
		});
		this.model.makePredefined();
		switchTimer = new Timer(10, e -> changePerspective());
		switchTimer.setRepeats(false);
	}

	/**
	 * Displays the given perspective, identified by the name.
	 *
	 * @param perspectiveName
	 *            the perspective which should be shown.
	 */
	public void showPerspective(final String perspectiveName) {
		queuePerspective(perspectiveName);
	}

	/**
	 * Queues the perspective with the given name to be shown.
	 *
	 * @param perspectiveName
	 * 		the name of the perspective to be shown
	 * @since 8.2.1
	 */
	private void queuePerspective(String perspectiveName) {
		queuePerspective(model.getPerspective(perspectiveName));
	}

	/**
	 * Displays the given perspective.
	 *
	 * @param perspective
	 *            the perspective which should be shown.
	 */
	public void showPerspective(final Perspective perspective) {
		queuePerspective(perspective);
	}

	/**
	 * Queues the given perspective to be shown.
	 *
	 * @param perspective
	 * 		the perspective to bw shown
	 * @since 8.2.1
	 */
	private void queuePerspective(Perspective perspective) {
		// add this perspective to the queue and restart timer if necessary
		switchTo = perspective;
		switchTimer.restart();
	}

	/**
	 * Actually changes the perspective. This should only be called by the {@link #switchTimer} to prevent breaking views.
	 * The timer switches the views that are queued and stops if there are no more switches left in the queue. The timer
	 * will be restarted when a new switch is queued.
	 * 
	 * @since 8.2.1
	 */
	private void changePerspective() {
		final Perspective target = switchTo;
		if (target == null) {
			return;
		}
		Perspective oldPerspective = model.getSelectedPerspective();
		if (oldPerspective == target) {
			switchTo = null;
			return;
		}
		if (!model.getAllPerspectives().contains(target)) {
			switchTo = null;
			return;
		}
		model.setSelectedPerspective(target);
		if (oldPerspective != null) {
			oldPerspective.store(context);
			ActionStatisticsCollector.getInstance().stopTimer(oldPerspective);
		}
		if (!target.apply(context)) {
			// retry if switching did not work the first time
			if (!target.apply(context)) {
				if (oldPerspective != null) {
					ActionStatisticsCollector.getInstance().startTimer(oldPerspective, ActionStatisticsCollector.TYPE_PERSPECTIVE,
							oldPerspective.getName(), null);
				}
				// rollback if changing did not work, will be collected by the PerspectiveController switchTimer.
				switchTo = oldPerspective;
				return;
			}
		}
		RapidMinerGUI.getMainFrame().RESTORE_PERSPECTIVE_ACTION.setEnabled(!target.isUserDefined() && (PerspectiveModel.DESIGN.equals(target.getName()) || PerspectiveModel.RESULT.equals(target.getName())));
		ActionStatisticsCollector.getInstance().startTimer(target, ActionStatisticsCollector.TYPE_PERSPECTIVE,
				target.getName(), null);
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_PERSPECTIVE, target.getName(),
				"show");
		switchTo = null;
	}

	/**
	 * Removes the given perspective. If the perspective which should be deleted is also the
	 * selected perspective, the first perspective will be shown.
	 *
	 * @param name
	 *            the name of the perspective which should be removed
	 */
	public void removePerspective(String name) {
		Perspective perspective = model.getPerspective(name);
		if (perspective != null) {
			removePerspective(perspective);
		}
	}

	/**
	 * Removes the given perspective. If the perspective which should be deleted is also the
	 * selected perspective, the first perspective will be shown.
	 *
	 * @param perspective
	 *            the perspective which should be deleted
	 */
	public void removePerspective(Perspective perspective) {
		if (!perspective.isUserDefined()) {
			return;
		}
		model.deletePerspective(perspective);
		if (model.getSelectedPerspective() == perspective && !model.getAllPerspectives().isEmpty()) {
			showPerspective(model.getAllPerspectives().get(0));
		}
	}

	/**
	 * Removes all perspectives and the given dockable.
	 */
	public void removeFromAllPerspectives(final Dockable dockable) {
		context.unregisterDockable(dockable);
		// Should also be removed from the workspaces, but the
		// vldocking framework does not support this
		removeFromInvisiblePerspectives(dockable);
	}

	/**
	 * Removes the given {@link Dockable} from all perspectives except the one currently displayed.
	 *
	 * @param dockable
	 *            the dockable to close
	 */
	public void removeFromInvisiblePerspectives(final Dockable dockable) {
		WSDockKey key = new WSDockKey(dockable.getDockKey().getKey());
		for (Perspective persp : model.getAllPerspectives()) {
			if (persp == model.getSelectedPerspective()) {
				continue;
			}
			persp.getWorkspace().getDesktop(0).removeNode(key);
		}
	}

	/**
	 * Shows the tab as a child of the given dockable in all perspectives.
	 */
	public void showTabInAllPerspectives(final Dockable dockable, final Dockable parent) {
		DockableState dstate = context.getDockableState(dockable);
		if (dstate != null && !dstate.isClosed()) {
			return;
		}

		DockingDesktop dockingDesktop = context.getDesktopList().get(0);
		context.registerDockable(dockable);

		WSDockKey parentKey = new WSDockKey(parent.getDockKey().getKey());
		WSDockKey key = new WSDockKey(dockable.getDockKey().getKey());
		for (Perspective persp : model.getAllPerspectives()) {
			if (persp == model.getSelectedPerspective()) {
				continue;
			}

			// We don't need to show it if
			// 1. We don't know the parent
			// 2. We already have the child
			boolean containsParent = persp.getWorkspace().getDesktop(0).containsNode(parentKey);
			boolean containsChild = persp.getWorkspace().getDesktop(0).containsNode(key);
			if (containsParent && !containsChild) {
				persp.getWorkspace().getDesktop(0).createTab(parentKey, key, 1);

				// for result tabs, make sure to switch actively viewed tab to new result
				if (dockable instanceof ResultTab && parent.getDockKey().getKey().equals(DockableResultDisplay.RESULT_DOCK_KEY)) {
					persp.getProperties().setNewFocusedResultTab(dockable);
				}
			}
		}

		DockableState[] states = dockingDesktop.getDockables();
		for (DockableState state : states) {
			if (state.getDockable() == parent && !state.isClosed()) {
				dockingDesktop.createTab(state.getDockable(), dockable, 1, true);
				break;
			}
		}
	}

	/**
	 * Saves all perspectives to the file system.
	 */
	public void saveAll() {
		LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.ApplicationPerspectives.saving_perspectives");
		if (model.getSelectedPerspective() != null) {
			model.getSelectedPerspective().store(context);
		}
		for (Perspective perspective : model.getAllPerspectives()) {
			perspective.save();
		}
	}

	/**
	 * Loads the default and user perspectives from the file system.
	 */
	public void loadAll() {
		LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.ApplicationPerspectives.loading_perspectives");
		for (Perspective perspective : model.getAllPerspectives()) {
			perspective.load();
		}
		File[] userPerspectiveFiles = FileSystemService.getUserRapidMinerDir()
				.listFiles((dir, name) -> name.startsWith("vlperspective-user-"));
		for (File file : userPerspectiveFiles) {
			String name = file.getName();
			name = name.substring("vlperspective-user-".length());
			name = name.substring(0, name.length() - ".xml".length());
			Perspective perspective = createUserPerspective(name, false);
			perspective.load();
		}
	}

	/**
	 * Creates a user-defined perspectives, and possibly switches to this new perspective
	 * immediately. The new perspective will be a copy of the current one.
	 */
	public Perspective createUserPerspective(final String name, final boolean show) {
		Perspective perspective = model.addPerspective(name, true);
		perspective.store(context);
		if (show) {
			showPerspective(name);
		}
		return perspective;
	}

	public void restoreDefaultPerspective() {
		if (!getModel().getSelectedPerspective().isUserDefined()) {
			String viewName = getModel().getSelectedPerspective().getName();
			getModel().restoreDefault(viewName);
			getModel().getSelectedPerspective().apply(context);

			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.PerspectiveController.restore_default", viewName);
		}
	}

	/**
	 * Getter for the underlying model.
	 *
	 * @return The used {@link PerspectiveModel}
	 */
	public PerspectiveModel getModel() {
		return model;
	}

	/**
	 * Getter for the restore default action for a predefined perspective.
	 *
	 * @return the restore default {@link Action}
	 * @deprecated use RapidMinerGUI.getMainFrame().RESTORE_PERSPECTIVE_ACTION instead
	 */
	@Deprecated
	public Action getRestoreDefaultAction() {
		return RapidMinerGUI.getMainFrame().RESTORE_PERSPECTIVE_ACTION;
	}

	/**
	 * Creates the workspace switch action for the given perspective.
	 *
	 * @param p
	 * 		the perspective
	 * @return the action, never {@code null}
	 * @since 8.1
	 */
	public WorkspaceAction createPerspectiveAction(final Perspective p) {
		String name = p.getName();
		WorkspaceAction action = new WorkspaceAction(name);

		if (p.isUserDefined()) {
			action.putValue(Action.ACTION_COMMAND_KEY, "perspective-" + name);
			action.putValue(Action.NAME, name);
			ImageIcon createIconSmall = SwingTools
					.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.action.workspace_user.icon"));
			ImageIcon createIconLarge = SwingTools
					.createIcon("24/" + I18N.getMessage(I18N.getGUIBundle(), "gui.action.workspace_user.icon"));
			action.putValue(Action.LARGE_ICON_KEY, createIconLarge);
			action.putValue(Action.SMALL_ICON, createIconSmall);
			action.putValue(Action.SHORT_DESCRIPTION,
					I18N.getMessage(I18N.getGUIBundle(), "gui.action.workspace_user.tip", name));
		}

		return action;
	}

	/**
	 * Called as part of the gui shutdown hook.
	 */
	public void shutdown() {
		ActionStatisticsCollector.getInstance().stopTimer(getModel().getSelectedPerspective());
	}
}
