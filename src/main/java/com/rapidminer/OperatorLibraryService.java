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
package com.rapidminer;

import com.rapidminer.gui.tools.actions.SelectionDependentAction;
import com.rapidminer.gui.tools.actions.SelectionDependentAction.SelectionDependency;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.libraries.LibraryOperatorDescription;
import com.rapidminer.operator.libraries.OperatorLibrary;
import com.rapidminer.tools.OperatorService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * This singleton Service keeps track of all currently loaded {@link OperatorLibrary}s. It provides
 * methods for loading and unloading {@link OperatorLibrary}s.
 * 
 * Currently this is a singleton implementation with a static method for accessing the singleton but
 * in future this might be replaced by a per process definition.
 * 
 * Additionally it holds information for editing the Libraries. Actions can be added to the toolbar
 * as well as to the context menu. They are only available on entries of the according class.
 * 
 * @author Sebastian Land
 */
public class OperatorLibraryService {

	/**
	 * Classes implementing this interface and having added with
	 * {@link OperatorLibraryService#registerOperatorLibraryListener(OperatorLibraryListener)} will
	 * be informed whenever an {@link OperatorLibrary} is added or removed.
	 * 
	 * @author Sebastian Land
	 */
	public interface OperatorLibraryListener {

		public void informLibraryLoaded(OperatorLibrary library);

		public void informLibraryUnloaded(OperatorLibrary library);

		public void informLibraryChanged(OperatorLibrary library);
	}

	private static List<SelectionDependentAction> toolbarActions = new LinkedList<SelectionDependentAction>();
	private static Map<Class<?>, List<SelectionDependentAction>> contextMenuActions = new HashMap<Class<?>, List<SelectionDependentAction>>();

	private static OperatorLibraryService serviceSingleton = new OperatorLibraryService();

	private List<OperatorLibrary> libraries = new ArrayList<OperatorLibrary>();
	private List<OperatorLibraryListener> listeners = new LinkedList<OperatorLibraryService.OperatorLibraryListener>();

	/**
	 * This method loads the given library and registers all contained operators.
	 */
	public void loadLibrary(OperatorLibrary library) {
		libraries.add(library);

		// register operators

		// TODO: Check if we need this at all!
		// for (String operatorKey: library.getOperatorKeys()) {
		// try {
		// OperatorService.registerOperator(library.getDescription(operatorKey),
		// library.getDocumentationBundle());
		// } catch (OperatorCreationException e) {
		// // TODO: after removing instanciation.
		// e.printStackTrace();
		// }
		// }
		// I think can be replaced by:
		try {
			library.registerOperators();
		} catch (OperatorCreationException e) {
			// TODO: after removing instanciation.
			e.printStackTrace();
		}

		// inform listener
		for (OperatorLibraryListener listener : listeners) {
			listener.informLibraryLoaded(library);
		}
	}

	/**
	 * This method loads the given library and registers all contained operators.
	 */
	public void unloadLibrary(OperatorLibrary library) {
		libraries.remove(library);

		// register operators
		for (String operatorKey : library.getOperatorKeys()) {
			OperatorService.unregisterOperator(library.getDescription(operatorKey));
		}

		// inform listener
		for (OperatorLibraryListener listener : listeners) {
			listener.informLibraryUnloaded(library);
		}
	}

	/**
	 * This adds a listener to the operator library Service that is informed of any registered or
	 * unregistered OperatorLibrary.
	 */
	public void registerOperatorLibraryListener(OperatorLibraryListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * This returns the number of the currently loaded libraries.
	 */
	public int getNumberOfLibraries() {
		return libraries.size();
	}

	/**
	 * This returns the library with the given index.
	 */
	public OperatorLibrary getLibrary(int index) {
		return libraries.get(index);
	}

	/**
	 * This returns the index of the given OperatorLibrary. Returns -1 if it can't be found.
	 */
	public int getIndexOf(OperatorLibrary child) {
		return libraries.indexOf(child);
	}

	/*
	 * STATIC METHODS
	 */

	/**
	 * This returns the service singleton.
	 */
	public static OperatorLibraryService getService() {
		return serviceSingleton;
	}

	/**
	 * This simply returns a list of all registered actions for the toolbar of the operator library
	 * view.
	 */
	public static List<SelectionDependentAction> getToolbarActions(SelectionDependency dependency) {
		List<SelectionDependentAction> result = new LinkedList<SelectionDependentAction>();
		for (SelectionDependentAction action : toolbarActions) {
			SelectionDependentAction clone = (SelectionDependentAction) action.clone();
			clone.setDependency(dependency);
			result.add(clone);
		}
		return result;
	}

	/**
	 * This registers the given action for the toolbar;
	 */
	public static void registerToolbarAction(SelectionDependentAction action) {
		toolbarActions.add(action);
	}

	/**
	 * This returns all actions that refer to an object of the given class.
	 */
	public static List<SelectionDependentAction> getContextMenuActions(Class<?> objectClass, SelectionDependency dependency) {
		List<SelectionDependentAction> result = new LinkedList<SelectionDependentAction>();
		for (Entry<Class<?>, List<SelectionDependentAction>> entry : contextMenuActions.entrySet()) {
			if (entry.getKey().isAssignableFrom(objectClass)) {
				for (SelectionDependentAction action : entry.getValue()) {
					SelectionDependentAction clone = (SelectionDependentAction) action.clone();
					clone.setDependency(dependency);
					result.add(clone);
				}
			}
		}

		return result;
	}

	/**
	 * This registers the given action for the given class. Actions in context menu will depend on
	 * the class the current item has.
	 */
	public static void registerContextMenuAction(Class<?> associatedClass, SelectionDependentAction action) {
		List<SelectionDependentAction> actions = contextMenuActions.get(associatedClass);
		if (actions == null) {
			actions = new LinkedList<SelectionDependentAction>();
			contextMenuActions.put(associatedClass, actions);
		}
		actions.add(action);
	}

	/**
	 * This notifies that the given operator has changed
	 */
	public void notifiyOperatorChanged(LibraryOperatorDescription libraryOperatorDescription) {
		notifyLibraryChanged(libraryOperatorDescription.getLibrary());
	}

	/**
	 * This notifies that the given library has changed.
	 */
	public void notifyLibraryChanged(OperatorLibrary operatorLibrary) {
		// inform listener
		for (OperatorLibraryListener listener : listeners) {
			listener.informLibraryChanged(operatorLibrary);
		}
	}
}
