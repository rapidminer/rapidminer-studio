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
package com.rapidminer.tools.config.gui.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.EventListenerList;
import javax.xml.ws.WebServiceException;

import com.rapidminer.gui.security.UserCredential;
import com.rapidminer.gui.security.Wallet;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.internal.remote.RemoteInfoService;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.PasswordInputCanceledException;
import com.rapidminer.tools.config.AbstractConfigurable;
import com.rapidminer.tools.config.Configurable;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.gui.ConfigurableDialog;
import com.rapidminer.tools.config.gui.event.ConfigurableEvent;
import com.rapidminer.tools.config.gui.event.ConfigurableEvent.EventType;
import com.rapidminer.tools.config.gui.event.ConfigurableModelEventListener;
import com.rapidminer.tools.container.Pair;


/**
 * The backing model of the {@link ConfigurableDialog}.
 *
 * @author Marco Boeck, Sabrina Kirstein
 *
 */
public class ConfigurableModel implements Observer<Pair<EventType, Configurable>> {

	/** comparator for Configurables */
	private static Comparator<Configurable> COMPARATOR = new ConfigurationManager.ConfigurableComparator();

	/** event listener for this model */
	private final EventListenerList eventListener;

	/** the list of all {@link Configurable}s in this model */
	private List<Configurable> listOfConfigurables;

	/**
	 * this map stores the original parameters for each {@link Configurable} which existed during
	 * construction
	 */
	private Map<Configurable, Map<String, String>> originalParameters;

	/**
	 * this map stores the original names for each {@link Configurable} which existed during
	 * construction
	 */
	private Map<Configurable, String> originalNames;

	/**
	 * this map stores a set with the original user groups that have access to a remote
	 * {@link Configurable} for each {@link Configurable}. Is empty for local {@link Configurable}s.
	 */
	private Map<Configurable, Set<String>> originalPermittedUserGroups;

	/**
	 * stores the original credentials of {@link #source}, if {@link #source} is not null
	 */
	private UserCredential originalCredentials;

	/**
	 * model contains the configurables with the defined source
	 */
	private RemoteRepository source = null;

	/**
	 * defines whether the editing of the parameters is possible
	 */
	private boolean editingPossible = true;

	/**
	 * defines whether the check was done if the editing of the parameters is possible
	 */
	private boolean checkDone = false;

	/**
	 * defines whether the user is allowed to edit, add or save configurables
	 */
	private boolean adminRights = false;

	/**
	 * Creates a new {@link ConfigurableModel} instance including all available {@link Configurable}
	 * s.
	 */
	public ConfigurableModel() {

		this.eventListener = new EventListenerList();
		// creates the model with all available configurables for all sources
		initModel(true, null);
	}

	/**
	 * Creates a new {@link ConfigurableModel} instance including all available {@link Configurable}
	 * s of the given {@link RemoteRepository}.
	 *
	 * @param source
	 *            only {@link Configurable}s of this {@link RemoteRepository} are stored in the
	 *            model, can be null for local connections
	 */
	public ConfigurableModel(RemoteRepository source) {

		this.eventListener = new EventListenerList();
		// creates one model per source (and adds only the configurables with the same source of the
		// model)
		initModel(false, source);
	}

	/**
	 * Creates the model either for all available configurables or for all configurables of the
	 * given source
	 *
	 * @param allConfigurables
	 *            defines whether the model should contain all configurables or just the
	 *            configurables of a given source
	 * @param source
	 *            defines the source of the model, if allConfigurables is false (is null otherwise)
	 */
	private void initModel(boolean allConfigurables, RemoteRepository source) {

		this.listOfConfigurables = Collections.synchronizedList(new LinkedList<Configurable>());
		this.originalParameters = new HashMap<>();
		this.originalNames = new HashMap<>();
		this.originalPermittedUserGroups = new HashMap<>();
		this.source = source;
		checkForAdminRights();
		if (source != null) {
			originalCredentials = Wallet.getInstance().getEntry(source.getAlias(), source.getBaseUrl().toString());
			if (originalCredentials == null) {
				originalCredentials = new UserCredential(source.getBaseUrl().toString(), source.getUsername(), null);
			}
		}

		for (Configurable configurable : ConfigurationManager.getInstance().getAllConfigurables()) {

			boolean sameLocalSource = source == null && configurable.getSource() == null;
			boolean sameRemoteSource = source != null && configurable.getSource() != null
			        && configurable.getSource().getName().equals(source.getName());
			if (allConfigurables || sameLocalSource || sameRemoteSource) {
				this.listOfConfigurables.add(configurable);
				// copy key-value pairs and save them in new map
				Map<String, String> parameterMap = new HashMap<>();
				for (String key : configurable.getParameters().keySet()) {
					if (configurable instanceof AbstractConfigurable) {
						parameterMap.put(key, ((AbstractConfigurable) configurable).getParameterAsXMLString(key));
					} else {
						parameterMap.put(key, configurable.getParameter(key));
					}
				}
				originalParameters.put(configurable, new HashMap<>(parameterMap));
				originalNames.put(configurable, configurable.getName());
				originalPermittedUserGroups.put(configurable,
				        ConfigurationManager.getInstance().getPermittedGroupsForConfigurable(configurable));
			}
		}

		// make sure we are notified when a Configurable is registered/removed via the
		// ConfigurationManager
		ConfigurationManager.getInstance().addObserver(this, false);
	}

	/**
	 * If the configurations have been loaded after the creation of the model, the backup parameters
	 * need to be stored separately.
	 */
	private void updateBackup() {
		for (Configurable configurable : listOfConfigurables) {
			// copy key-value pairs and save them in new map
			Map<String, String> parameterMap = new HashMap<>();
			for (String key : configurable.getParameters().keySet()) {
				if (configurable instanceof AbstractConfigurable) {
					parameterMap.put(key, ((AbstractConfigurable) configurable).getParameterAsXMLString(key));
				} else {
					parameterMap.put(key, configurable.getParameter(key));
				}
			}
			originalParameters.put(configurable, new HashMap<>(parameterMap));
			originalNames.put(configurable, configurable.getName());
			originalPermittedUserGroups.put(configurable,
			        ConfigurationManager.getInstance().getPermittedGroupsForConfigurable(configurable));
		}
	}

	/**
	 * checks if the source is a instance of {@link RemoteRepository}, if the editing of the
	 * containing configurables is possible
	 */
	public void checkVersion() {
		if (source != null) {
			if (!checkDone) {
				// check the server version
				RemoteInfoService versionService = source.getInfoService();

				if (versionService != null) {
					if (versionService.getVersionNumber() != null) {
						VersionNumber serverVersion = new VersionNumber(versionService.getVersionNumber());
						// server connections are not editable
						if (!serverVersion.isAtLeast(new VersionNumber(2, 4, 0, "SNAPSHOT"))) {
							editingPossible = false;
						}
						checkDone = true;
					}
				}
			}
		}
	}

	/**
	 * Adds a {@link ConfigurableModelEventListener} which will be informed of all changes to this
	 * model.
	 *
	 * @param listener
	 */
	public void registerEventListener(final ConfigurableModelEventListener listener) {
		eventListener.add(ConfigurableModelEventListener.class, listener);
	}

	/**
	 * Removes the {@link ConfigurableModelEventListener} from this model.
	 *
	 * @param listener
	 */
	public void removeEventListener(final ConfigurableModelEventListener listener) {
		eventListener.remove(ConfigurableModelEventListener.class, listener);
	}

	/**
	 * Returns a list of the user defined names for all {@link Configurable}s of the specified type.
	 *
	 * @param typeId
	 * @return
	 */
	public List<String> getListOfUniqueNamesForType(String typeId) {
		List<String> list = new LinkedList<>();
		for (Configurable c : listOfConfigurables) {
			if (c.getTypeId().equals(typeId)) {
				list.add(c.getName());
			}
		}

		return list;
	}

	/**
	 * @return the source of the configurables in the model, can be null (if local)
	 */
	public RemoteRepository getSource() {
		return source;
	}

	/**
	 * This method returns all {@link Configurable}s which have existed during construction, as well
	 * as the parameters at that time.
	 *
	 * @return
	 */
	public Map<Configurable, Map<String, String>> getBackupParameters() {
		return new HashMap<>(originalParameters);
	}

	/**
	 * This method returns all {@link Configurable}s which have existed during construction, as well
	 * as their names at that time.
	 *
	 * @return
	 */
	public Map<Configurable, String> getBackupNames() {
		return new HashMap<>(originalNames);
	}

	/**
	 * This method returns all user groups which were originally permitted to have access to the
	 * configurables
	 *
	 * @return
	 */
	public Map<Configurable, Set<String>> getBackupPermittedUserGroups() {
		return originalPermittedUserGroups;
	}

	/**
	 *
	 * @return if the editing of the contained configurables is possible
	 */
	public boolean isEditingPossible() {
		if (!wasVersionCheckDone()) {
			try {
				checkVersion();
			} catch (WebServiceException e) {
				// no connection possible
				return false;
			}
		}
		return editingPossible;
	}

	/**
	 *
	 * @return if the check if the editing of the contained configurables is possible was done
	 *         already
	 */
	public boolean wasVersionCheckDone() {
		return checkDone;
	}

	/**
	 * checks whether the user logged in has admin rights
	 */
	public void checkForAdminRights() {
		if (source == null) {
			adminRights = true;
		} else if (source.getUsername().equals(ConfigurationManager.RM_SERVER_CONFIGURATION_USER_ADMIN)) {
			adminRights = true;
		}
	}

	/**
	 *
	 * @return whether the user is allowed to edit, add, remove and save configurables
	 */
	public boolean hasAdminRights() {
		return adminRights;
	}

	/**
	 * Adds a new {@link Configurable}.
	 *
	 * @param configurable
	 */
	public void addConfigurable(Configurable configurable) {

		boolean sameLocalSource = configurable.getSource() == null && this.source == null;
		boolean sameRemoteSource = configurable.getSource() != null && this.source != null
		        && configurable.getSource().getName().equals(source.getName());

		if (sameLocalSource || sameRemoteSource) {

			listOfConfigurables.add(configurable);
			Collections.sort(listOfConfigurables, COMPARATOR);
			fireConfigurableAddedEvent(configurable);
		}
	}

	/**
	 * Removes the given {@link Configurable}.
	 *
	 * @param configurable
	 */
	public void removeConfigurable(Configurable configurable) {
		listOfConfigurables.remove(configurable);
		ConfigurationManager.getInstance().setPermittedGroupsForConfigurable(configurable, new HashSet<String>());

		fireConfigurableRemovedEvent(configurable);
	}

	/**
	 * Get all {@link Configurable}s in this model.
	 *
	 * @return
	 */
	public List<Configurable> getConfigurables() {
		return new LinkedList<>(listOfConfigurables);
	}

	/**
	 * Fire when a {@link Configurable} has been added.
	 *
	 * @param configurable
	 */
	private void fireConfigurableAddedEvent(Configurable configurable) {
		fireEvent(EventType.CONFIGURABLE_ADDED, configurable);
	}

	/**
	 * Fire when a {@link Configurable} has been removed.
	 *
	 * @param configurable
	 */
	private void fireConfigurableRemovedEvent(Configurable configurable) {
		fireEvent(EventType.CONFIGURABLE_REMOVED, configurable);
	}

	/**
	 * Fire when all {@link Configurable}s have changed.
	 *
	 */
	private void fireConfigurablesChangedEvent() {
		fireEvent(EventType.CONFIGURABLES_CHANGED, null);
	}

	/**
	 * Fire when all {@link Configurable}s have been loaded.
	 *
	 */
	private void fireLoadedFromRepositoryEvent(Configurable configurable) {
		fireEvent(EventType.LOADED_FROM_REPOSITORY, configurable);
	}

	/**
	 * Fires the given {@link EventType} for the {@link Configurable} in question.
	 *
	 * @param type
	 * @param configurable
	 *            can be <code>null</code>
	 */
	private void fireEvent(final EventType type, Configurable configurable) {
		Object[] listeners = eventListener.getListenerList();
		// Process the listeners last to first
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ConfigurableModelEventListener.class) {
				ConfigurableEvent e = new ConfigurableEvent(type, configurable);
				((ConfigurableModelEventListener) listeners[i + 1]).modelChanged(e);
			}
		}
	}

	@Override
	public void update(Observable<Pair<EventType, Configurable>> observable, Pair<EventType, Configurable> arg) {

		boolean sameLocalSource = arg.getSecond() != null && arg.getSecond().getSource() == null && this.source == null;
		boolean sameRemoteSource = arg.getSecond() != null && arg.getSecond().getSource() != null && this.source != null
		        && arg.getSecond().getSource().getName().equals(source.getName());

		if (EventType.CONFIGURABLES_CHANGED.equals(arg.getFirst())) {

			fireConfigurablesChangedEvent();

		} else if (EventType.CONFIGURABLE_ADDED.equals(arg.getFirst())) {

			if (sameLocalSource || sameRemoteSource) {
				addConfigurable(arg.getSecond());
			}

		} else if (EventType.CONFIGURABLE_REMOVED.equals(arg.getFirst())) {

			if (sameLocalSource || sameRemoteSource) {
				removeConfigurable(arg.getSecond());
			}

		} else if (EventType.LOADED_FROM_REPOSITORY.equals(arg.getFirst())) {

			if (sameRemoteSource) {
				updateBackup();
				Configurable configurable = arg.getSecond();
				configurable.setSource(source);
				fireLoadedFromRepositoryEvent(configurable);
			}

		}

	}

	/**
	 * Resets the configurables in the cache.
	 */
	public void resetConfigurables() {
		if (source != null) {
			for (Configurable originalConfig : originalNames.keySet()) {
				ConfigurationManager.getInstance().removeConfigurable(originalConfig.getTypeId(),
				        originalNames.get(originalConfig), source.getName());
			}
			listOfConfigurables.clear();
		}
	}

	/**
	 * Resets the connection to the {@link RemoteRepository} of this model
	 *
	 * @throws RepositoryException
	 *             if the connection could not be established
	 */
	public void resetConnection() throws RepositoryException {
		RepositoryException firstException = null;
		RepositoryException secondException = null;
		try {
			// reset the repository service
			source.resetContentManager();
		} catch (RepositoryException e) {
			// if no connection could be established
			secondException = e;
		} catch (PasswordInputCanceledException e) {
			// ignore
		}
		try {
			// clear the cache
			source.refresh();
		} catch (RepositoryException e) {
			// if no connection could be established
			firstException = e;
		}
		checkForAdminRights();
		if (firstException != null) {
			throw firstException;
		} else if (secondException != null) {
			throw secondException;
		}
	}

	/**
	 * Resets the credentials of the {@link RemoteRepository} of this model if they have changed
	 */
	public void resetCredentials() {

		// if the original credentials are given and changed
		if (originalCredentials != null) {
			if (!source.getUsername().equals(originalCredentials.getUsername())) {
				source.setUsername(originalCredentials.getUsername());
				source.setPassword(originalCredentials.getPassword());
				resetConfigurables();
				try {
					resetConnection();
				} catch (RepositoryException e) {
					// reset connection failed
				}
			}
		}
	}
}
