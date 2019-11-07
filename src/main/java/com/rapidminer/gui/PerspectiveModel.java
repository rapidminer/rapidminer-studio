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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.processeditor.NewOperatorEditor;
import com.rapidminer.gui.processeditor.results.ResultDisplay;
import com.rapidminer.gui.properties.OperatorPropertyPanel;
import com.rapidminer.repository.gui.RepositoryBrowser;
import com.rapidminer.tools.AbstractObservable;
import com.vlsolutions.swing.docking.DockingConstants;
import com.vlsolutions.swing.docking.ws.WSDesktop;
import com.vlsolutions.swing.docking.ws.WSDockKey;


/**
 * The {@link PerspectiveModel} is managed by a {@link PerspectiveController} and stores all
 * necessary information about the registered perspectives. This model can notify listeners/
 * observers about perspective changes and new registered perspectives.
 *
 * @author Marcel Michel
 * @since 7.0.0
 */
public class PerspectiveModel extends AbstractObservable<List<Perspective>> {

	public static final String RESULT = "result";
	public static final String DESIGN = "design";

	public static final String TURBO_PREP = "turbo_prep";
	public static final String MODEL_WIZARD = "model_wizard";
	public static final String DEPLOYMENTS = "deployments";
	public static final String HADOOP_DATA = "hadoop_data";

	private final Map<String, Perspective> perspectives = new LinkedHashMap<>();

	private Perspective selectedPerspective;

	private LinkedList<PerspectiveChangeListener> perspectiveChangeListenerList;

	/**
	 * Creates a new perspective, and possibly switches to this new perspective immediately. The new
	 * perspective will be a copy of the current one.
	 *
	 * @throws IllegalArgumentException
	 *             if name is already used
	 */
	public Perspective addPerspective(final String name, final boolean userDefined) {
		final Perspective p = new Perspective(this, name);
		if (!isValidName(name)) {
			throw new IllegalArgumentException("Invalid or duplicate view name: " + name);
		}
		p.setUserDefined(userDefined);
		perspectives.put(name, p);

		fireUpdate(new ArrayList<Perspective>(perspectives.values()));
		return p;
	}

	/**
	 * Removes the given perspective by name from the model.
	 *
	 * @param name
	 *            the name of the perspective which should be removed
	 */
	public void deletePerspective(final String name) {
		if (perspectives.containsKey(name)) {
			deletePerspective(perspectives.get(name));
		}
	}

	/**
	 * Removes the given perspective from the model.
	 *
	 * @param name
	 *            the perspective which should be removed
	 */
	public void deletePerspective(final Perspective p) {
		if (!p.isUserDefined()) {
			return;
		}
		perspectives.remove(p.getName());
		p.delete();
		fireUpdate(new ArrayList<>(perspectives.values()));
	}

	/**
	 * Adds the default perspectives to the model.
	 */
	public void makePredefined() {
		addPerspective(DESIGN, false);
		restoreDefault(DESIGN);
		addPerspective(RESULT, false);
		restoreDefault(RESULT);
	}

	/**
	 * Restores the default layout of the perspectives. This method only works for predefined
	 * perspectives (like {@link #DESIGN} and {@link #RESULT}).
	 *
	 * @param perspectiveName
	 *            the name of the perspective which should be restored
	 *
	 * @throws IllegalAccessException
	 *             if the perspective is not known
	 */
	public void restoreDefault(String perspectiveName) {
		WSDockKey processPanelKey = new WSDockKey(ProcessPanel.PROCESS_PANEL_DOCK_KEY);
		WSDockKey propertyTableKey = new WSDockKey(OperatorPropertyPanel.PROPERTY_EDITOR_DOCK_KEY);
		WSDockKey resultsKey = new WSDockKey(ResultDisplay.RESULT_DOCK_KEY);
		WSDockKey repositoryKey = new WSDockKey(RepositoryBrowser.REPOSITORY_BROWSER_DOCK_KEY);
		WSDockKey newOperatorEditorKey = new WSDockKey(NewOperatorEditor.NEW_OPERATOR_DOCK_KEY);
		WSDockKey operatorHelpKey = new WSDockKey(OperatorDocumentationBrowser.OPERATOR_HELP_DOCK_KEY);

		if (DESIGN.equals(perspectiveName)) {
			Perspective designPerspective = getPerspective(DESIGN);
			WSDesktop designDesktop = designPerspective.getWorkspace().getDesktop(0);
			designDesktop.clear();
			designDesktop.addDockable(processPanelKey);
			designDesktop.split(processPanelKey, propertyTableKey, DockingConstants.SPLIT_RIGHT, 0.8);
			designDesktop.split(propertyTableKey, operatorHelpKey, DockingConstants.SPLIT_BOTTOM, .66);
			designDesktop.split(processPanelKey, repositoryKey, DockingConstants.SPLIT_LEFT, 0.25);
			designDesktop.split(repositoryKey, newOperatorEditorKey, DockingConstants.SPLIT_BOTTOM, 0.5);
		} else if (RESULT.equals(perspectiveName)) {
			Perspective resultPerspective = getPerspective(RESULT);
			WSDesktop resultsDesktop = resultPerspective.getWorkspace().getDesktop(0);
			resultsDesktop.clear();
			resultsDesktop.addDockable(resultsKey);
			resultsDesktop.split(resultsKey, repositoryKey, DockingConstants.SPLIT_RIGHT, 0.8);
		} else {
			throw new IllegalArgumentException("Not a predefined view: " + perspectiveName);
		}
	}

	/**
	 * Gets a perspective by name.
	 *
	 * @param name
	 *            the name of the perspective
	 * @return the resolved {@link Perspective}
	 * @throws NoSuchElementException
	 *             if the perspective is not known
	 */
	public Perspective getPerspective(final String name) {
		Perspective result = perspectives.get(name);
		if (result != null) {
			return result;
		} else {
			throw new NoSuchElementException("No such view: " + name);
		}
	}

	/**
	 * Getter for all registered perspectives
	 *
	 * @return all perspectives as {@link List}
	 */
	public List<Perspective> getAllPerspectives() {
		return new ArrayList<>(perspectives.values());
	}

	/**
	 * Registers a new {@link PerspectiveChangeListener}.
	 *
	 * @param listener
	 *            the listener which should be notified about perspective changes.
	 */
	public void addPerspectiveChangeListener(final PerspectiveChangeListener listener) {
		if (listener == null) {
			return;
		}
		if (perspectiveChangeListenerList == null) {
			perspectiveChangeListenerList = new LinkedList<>();
		}
		perspectiveChangeListenerList.add(listener);
	}

	/**
	 * Removes the given {@link PerspectiveChangeListener} from the listener list.
	 *
	 * @param listener
	 *            the listener which should be removed
	 * @return {@code true} if the listener could be removed, otherwise {@code false}
	 */
	public boolean removePerspectiveChangeListener(final PerspectiveChangeListener listener) {
		if (perspectiveChangeListenerList == null) {
			return false;
		}
		return perspectiveChangeListenerList.remove(listener);
	}

	/**
	 * Getter for the current selected perspective.
	 *
	 * @return the selected perspective
	 */
	public Perspective getSelectedPerspective() {
		return selectedPerspective;
	}

	/**
	 * Updates the selected perspective and notifies the {@link PerspectiveChangeListener}.
	 *
	 * @param name
	 * 		the name of the new selected perspective
	 * @deprecated since 8.2.1; use {@link PerspectiveController#showPerspective(String)} instead,
	 * as this method is not safe and won't be public in the future
	 */
	@Deprecated
	public void setSelectedPerspective(String name) {
		if (perspectives.containsKey(name)) {
			setSelectedPerspective(perspectives.get(name));
		}
	}

	/**
	 * Updates the selected perspective and notifies the {@link PerspectiveChangeListener}.
	 *
	 * @param perspective
	 * 		the new selected perspective
	 * @deprecated since 8.2.1; use {@link PerspectiveController#showPerspective(Perspective)} instead,
	 * as this method is not safe and won't be public in the future
	 */
	@Deprecated
	public void setSelectedPerspective(Perspective perspective) {
		if (selectedPerspective == perspective) {
			return;
		}
		selectedPerspective = perspective;
		this.notifyChangeListener();
	}

	/**
	 * Checks if the given string is valid as name of a new perspective.
	 *
	 * @param name
	 * @return validity
	 */
	public boolean isValidName(final String name) {
		if (name == null) {
			return false;
		}
		if (name.trim().isEmpty()) {
			return false;
		}
		for (Perspective perspective : perspectives.values()) {
			if (perspective.getName().equalsIgnoreCase(name)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Notifies the registered {@link PerspectiveChangeListener}s about the
	 * {@link #selectedPerspective}.
	 */
	public void notifyChangeListener() {
		// do not fire these in the EDT
		new Thread(() -> {
			if (perspectiveChangeListenerList != null) {
				new LinkedList<>(perspectiveChangeListenerList).forEach(listener -> listener.perspectiveChangedTo(selectedPerspective));
			}
		}).start();
	}
}
