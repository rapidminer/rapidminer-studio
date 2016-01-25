/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Level;

import javax.swing.Action;

import com.rapidminer.gui.processeditor.ProcessLogTab;
import com.rapidminer.gui.processeditor.results.ResultTab;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableResolver;
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

	private final Action restoreDefaultAction = new ResourceAction("restore_predefined_perspective_default") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (!getModel().getSelectedPerspective().isUserDefined()) {
				getModel().restoreDefault(getModel().getSelectedPerspective().getName());
				getModel().getSelectedPerspective().apply(context);
			}
		}
	};

	/**
	 * Creates a new {@link PerspectiveController} with the given docking context.
	 *
	 * @param context
	 *            the docking context which should be used
	 */
	public PerspectiveController(final DockingContext context) {
		this.context = context;
		this.model = new PerspectiveModel();
		context.setDockableResolver(new DockableResolver() {

			@Override
			public Dockable resolveDockable(final String key) {
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
			}
		});
		this.model.makePredefined();
	}

	/**
	 * Displays the given perspective, identified by the name.
	 *
	 * @param perspective
	 *            the perspective which should be shown.
	 */
	public void showPerspective(final String perspectiveName) {
		showPerspective(model.getPerspective(perspectiveName));
	}

	/**
	 * Displays the given perspective.
	 *
	 * @param perspective
	 *            the perspective which should be shown.
	 */
	public void showPerspective(final Perspective perspective) {
		Perspective oldPerspective = model.getSelectedPerspective();
		if (oldPerspective == perspective) {
			return;
		}
		model.setSelectedPerspective(perspective);
		if (oldPerspective != null) {
			oldPerspective.store(context);
			ActionStatisticsCollector.getInstance().stopTimer(ActionStatisticsCollector.TYPE_PERSPECTIVE,
					oldPerspective.getName(), null);
		}
		perspective.apply(context);
		getRestoreDefaultAction().setEnabled(!perspective.isUserDefined());
		if (perspective != null) {
			ActionStatisticsCollector.getInstance().startTimer(ActionStatisticsCollector.TYPE_PERSPECTIVE,
					perspective.getName(), null);
			ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_PERSPECTIVE, perspective.getName(),
					"show");
		}
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
		File[] userPerspectiveFiles = FileSystemService.getUserRapidMinerDir().listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return name.startsWith("vlperspective-user-");
			}
		});
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
	 */
	public Action getRestoreDefaultAction() {
		return restoreDefaultAction;
	}
}
