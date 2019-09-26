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
package com.rapidminer.gui.actions.search;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.Icon;

import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.SerializationUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryparser.classic.ParseException;

import com.rapidminer.gui.DockableMenu;
import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.PerspectiveChangeListener;
import com.rapidminer.gui.PerspectiveModel;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.actions.WorkspaceAction;
import com.rapidminer.gui.security.PasswordManager;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.gui.RepositoryBrowser;
import com.rapidminer.search.AbstractGlobalSearchManager;
import com.rapidminer.search.GlobalSearchDefaultField;
import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.search.GlobalSearchResult;
import com.rapidminer.search.GlobalSearchResultBuilder;
import com.rapidminer.search.GlobalSearchUtilities;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observer;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.DummyDockable;


/**
 * Manages Global Search for {@link ResourceAction}s.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class ActionsGlobalSearchManager extends AbstractGlobalSearchManager {

	private static final Map<String, String> ADDITIONAL_FIELDS;

	private static final float FIELD_BOOST_DESCRIPTION = 0.5f;

	/** this is a flag field which is set for view actions so we can delete them */
	private static final String FIELD_VIEW_TYPE = "action_view_type";

	/** this is a flag field which is set for dockable actions so we can delete them */
	private static final String FIELD_DOCKABLE_TYPE = "action_dockable_type";

	protected static final String FIELD_DESCRIPTION = "description";
	protected static final String FIELD_SERIALIZED_ACTION = "serializedAction";

	static {
		ADDITIONAL_FIELDS = new HashMap<>();
		ADDITIONAL_FIELDS.put(FIELD_DESCRIPTION, "The description of an action. More verbose than just their name.");
	}

	/** observe perspective additions/removals */
	private final Observer<List<Perspective>> perspectiveObserver;

	/** when the perspective changes, we need to re-index the dockables to have them up-to-date */
	private final PerspectiveChangeListener perspectiveChangeListener;

	/**
	 * actions that are registered are stored here. This is used for a quick lookup if an action is part of the index.
	 * Searching the actual index takes way more time.
	 */
	private final Set<String> registeredActions;


	protected ActionsGlobalSearchManager() {
		super(ActionsGlobalSearch.CATEGORY_ID, ADDITIONAL_FIELDS, new GlobalSearchDefaultField(FIELD_DESCRIPTION, FIELD_BOOST_DESCRIPTION));

		perspectiveObserver = (observable, perspectives) -> {

			deleteAllPerspectiveActions();
			for (Perspective perspective : perspectives) {
				addDocumentToIndex(createDocumentFromPerspective(perspective));
			}
		};

		perspectiveChangeListener = perspective -> {

			// delete all dockable actions and re-add them (because some may not be available in some views)
			deleteAllDockableActions();
			indexDockables();
		};

		registeredActions = ConcurrentHashMap.newKeySet();
	}

	@Override
	protected void init() {
		PerspectiveModel model = RapidMinerGUI.getMainFrame().getPerspectiveController().getModel();

		model.addObserver(perspectiveObserver, false);
		model.addPerspectiveChangeListener(perspectiveChangeListener);
	}

	@Override
	protected List<Document> createInitialIndex(final ProgressThread progressThread) {
		List<Document> mainFrameActions = new LinkedList<>();

		// scoop up actions from MainFrame and add them
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().NEW_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().OPEN_ACTION));
		mainFrameActions.add(createDocument(RapidMinerGUI.getMainFrame().SAVE_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().SAVE_AS_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().IMPORT_DATA_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().IMPORT_PROCESS_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().EXPORT_PROCESS_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().UNDO_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().REDO_ACTION));
		mainFrameActions.add(createDocument(RapidMinerGUI.getMainFrame().RUN_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().STOP_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().VALIDATE_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().AUTO_WIRE));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().NEW_PERSPECTIVE_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().MANAGE_CONFIGURABLES_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().EXPORT_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().EXIT_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().SETTINGS_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().RESTORE_PERSPECTIVE_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().CREATE_CONNECTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().TUTORIAL_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().BROWSE_VIDEOS_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().BROWSE_DOCUMENTATION_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().BROWSE_COMMUNITY_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().BROWSE_SUPPORT_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) RapidMinerGUI.getMainFrame().ABOUT_ACTION));
		mainFrameActions.add(createDocument((ResourceAction) PasswordManager.OPEN_WINDOW));
		mainFrameActions.add(createDocument((ResourceAction) RepositoryBrowser.ADD_REPOSITORY_ACTION));

		// add perspectives
		for (Perspective perspective : RapidMinerGUI.getMainFrame().getPerspectiveController().getModel().getAllPerspectives()) {
			mainFrameActions.add(createDocumentFromPerspective(perspective));
		}

		// because createDocument can return null, we need to filter nulls again now
		// this is just a precaution, should not happen here
		return mainFrameActions.stream().filter(Objects::nonNull).collect(Collectors.toList());
	}

	/**
	 * Adds the given action to the Global Search index. If it is already part of the index, it will be updated.
	 * If it has no name, it will not be added to the index.
	 *
	 * @param action
	 * 		the action to add to the search index. Must <strong>NOT</strong> be a {@link ToggleAction}!
	 */
	public void addAction(final ResourceAction action) {
		if (action == null) {
			throw new IllegalArgumentException("action must not be null!");
		}
		if (action.getValue(Action.NAME) == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.actions.search.ActionsGlobalSearchManager.error.index_action", action.getKey());
			return;
		}
		if (action instanceof WorkspaceAction) {
			// these are special, we don't want to have the regular perspective actions update our custom built ones, so ignore them
			return;
		}

		Document doc = createDocument(action);
		if (doc != null) {
			addDocumentToIndex(doc);
		}
	}

	/**
	 * Removes the given action from the Global Search index.
	 *
	 * @param action
	 * 		the action to remove from the search index
	 */
	public void removeAction(final ResourceAction action) {
		if (action == null) {
			throw new IllegalArgumentException("action must not be null!");
		}

		Document doc = createDocument(action);
		if (doc != null) {
			removeDocumentFromIndex(doc);
			registeredActions.remove(action.getKey());
		}
	}

	/**
	 * Checks if the given action is registered to the Global Search.
	 *
	 * @param action
	 * 		the action to check
	 * @return {@code true} if the action is already indexed in the Global Search; {@code false} otherwise
	 */
	public boolean isActionRegistered(final ResourceAction action) {
		if (action == null) {
			throw new IllegalArgumentException("action must not be null!");
		}

		// search for action with given key
		return registeredActions.contains(action.getKey());
	}

	/**
	 * Adds all {@link com.vlsolutions.swing.docking.Dockable}s to the Global Search.
	 */
	private void indexDockables() {
		List<Document> dockableDocuments = new ArrayList<>();

		for (final DockableState state : RapidMinerGUI.getMainFrame().getDockingDesktop().getDockables()) {
			DockKey dockKey = state.getDockable().getDockKey();
			if (state.getDockable() instanceof DummyDockable || DockableMenu.isDockableHiddenFromMenu(dockKey)) {
				continue;
			}

			dockableDocuments.add(createDocumentFromDockKey(dockKey));
		}

		addDocumentsToIndex(dockableDocuments);
	}

	/**
	 * Deletes all perspective actions from the index.
	 *
	 */
	private void deleteAllPerspectiveActions() {
		// search for all elements that start with "workspace_"
		GlobalSearchResultBuilder builder = new GlobalSearchResultBuilder(FIELD_VIEW_TYPE + GlobalSearchUtilities.QUERY_FIELD_SPECIFIER + String.valueOf(Boolean.TRUE));
		builder.setMaxNumberOfResults(Integer.MAX_VALUE).setSearchCategories(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(getSearchCategoryId()));
		try {
			GlobalSearchResult result = builder.runSearch();
			removeDocumentsFromIndex(result.getResultDocuments());
		} catch (ParseException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.actions.search.ActionsGlobalSearchManager.error.delete_views_error", e);
		}
	}

	/**
	 * Deletes all dockable actions from the index.
	 *
	 */
	private void deleteAllDockableActions() {
		// now actually search for all elements that start with "workspace_"
		GlobalSearchResultBuilder builder = new GlobalSearchResultBuilder(FIELD_DOCKABLE_TYPE + GlobalSearchUtilities.QUERY_FIELD_SPECIFIER + String.valueOf(Boolean.TRUE));
		builder.setMaxNumberOfResults(Integer.MAX_VALUE).setSearchCategories(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(getSearchCategoryId()));
		try {
			GlobalSearchResult result = builder.runSearch();
			removeDocumentsFromIndex(result.getResultDocuments());
		} catch (ParseException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.actions.search.ActionsGlobalSearchManager.error.delete_dockables_error", e);
		}
	}

	/**
	 * Creates an action search document for the given {@link ResourceAction}.
	 *
	 * @param action
	 * 		the action for which to create the search document
	 * @return the document or {@code null} if it could not be created
	 */
	private Document createDocument(final ResourceAction action) {
		List<Field> fields = new ArrayList<>();
		Object name = action.getValue(Action.NAME);
		// serialize the action so that we can actually trigger it on retrieval later
		byte[] serializedAction = createSerializedAction(action);
		if (name == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.actions.search.ActionsGlobalSearchManager.error.index_action.name_null", action.getKey());
			return null;
		}
		if (serializedAction == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.actions.search.ActionsGlobalSearchManager.error.index_action.serialization_failed", action.getKey());
			return null;
		}

		// description field
		Object tooltip = action.getValue(Action.SHORT_DESCRIPTION);
		if (tooltip != null) {
			fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_DESCRIPTION, String.valueOf(tooltip)));
		}

		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForBinary(FIELD_SERIALIZED_ACTION, serializedAction));

		if (action instanceof WorkspaceAction) {
			fields.add(GlobalSearchUtilities.INSTANCE.createFieldForIdentifiers(FIELD_VIEW_TYPE, String.valueOf(Boolean.TRUE)));
		} else if (action instanceof DockableAction) {
			fields.add(GlobalSearchUtilities.INSTANCE.createFieldForIdentifiers(FIELD_DOCKABLE_TYPE, String.valueOf(Boolean.TRUE)));
		}

		// add to registered actions map
		registeredActions.add(action.getKey());

		// i18n key is the unique ID for the action category
		return GlobalSearchUtilities.INSTANCE.createDocument(action.getKey(), String.valueOf(name), fields.toArray(new Field[0]));
	}

	/**
	 * Creates an action search document for the given {@link Perspective}.
	 *
	 * @param perspective
	 * 		the perspective for which to create the action as well as the document
	 * @return the document or {@code null} if it could not be created
	 */
	private Document createDocumentFromPerspective(final Perspective perspective) {
		WorkspaceAction action = RapidMinerGUI.getMainFrame().getPerspectiveController().createPerspectiveAction(perspective);
		// make label a bit more descriptive
		String name = action.getValue(ResourceAction.NAME) != null ? String.valueOf(action.getValue(ResourceAction.NAME)) : perspective.getName().replaceAll("_", " ");
		action.putValue(ResourceAction.NAME, I18N.getMessage(I18N.getGUIBundle(), "gui.action.global_search.action.perspective.name", SwingTools.capitalizeString(name)));
		return createDocument(action);
	}

	/**
	 * Creates an action search document for the given {@link DockKey}.
	 *
	 * @param dockKey
	 * 		the dockKey for which to create the action as well as the document
	 * @return the document or {@code null} if it could not be created
	 */
	private Document createDocumentFromDockKey(final DockKey dockKey) {
		String description = null;
		if (dockKey instanceof ResourceDockKey) {
			description = ((ResourceDockKey) dockKey).getShortDescription();
		}
		String name = I18N.getGUIMessage("gui.action.global_search.action.dockable.name", dockKey.getName());
		Icon icon = dockKey.getIcon();
		String tip = dockKey.getTooltip();

		DockableAction action = new DockableAction(dockKey.getKey());
		action.putValue(ResourceAction.NAME, name);
		action.putValue(ResourceAction.SHORT_DESCRIPTION, description);
		action.putValue(ResourceAction.LONG_DESCRIPTION, tip);
		action.putValue(ResourceAction.SMALL_ICON, icon);

		return createDocument(action);
	}

	/**
	 * Tries to serialize the given action to a byte array.
	 *
	 * @param action
	 * 		the action to serialize
	 * @return the byte array or {@code null} if something goes wrong
	 */
	private byte[] createSerializedAction(final ResourceAction action) {
		// remove listeners which are NOT transient...
		PropertyChangeListener[] propertyChangeListeners = action.getPropertyChangeListeners();
		for (PropertyChangeListener propertyChangeListener : propertyChangeListeners) {
			action.removePropertyChangeListener(propertyChangeListener);
		}

		try {
			return SerializationUtils.serialize(action);
		} catch (SerializationException e) {
			LogService.getRoot().log(Level.WARNING, "", e);
			return null;
		} finally {
			// restore listeners which are NOT transient...
			for (PropertyChangeListener propertyChangeListener : propertyChangeListeners) {
				action.addPropertyChangeListener(propertyChangeListener);
			}

		}
	}


}
