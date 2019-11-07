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
package com.rapidminer.tools.config;

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.SimpleListBasedParameterHandler;
import com.rapidminer.repository.ConnectionListener;
import com.rapidminer.repository.ConnectionRepository;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryAccessor;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryListener;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryManagerListener;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.repository.internal.remote.ResponseContainer;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.config.gui.event.ConfigurableEvent;
import com.rapidminer.tools.config.gui.event.ConfigurableEvent.EventType;
import com.rapidminer.tools.container.ComparablePair;
import com.rapidminer.tools.container.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileNotFoundException;
import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;


/**
 * Singleton to access configurable items and to provide means to configure them by the user.
 *
 * @author Simon Fischer, Marco Boeck, Sabrina Kirstein
 *
 */
public abstract class ConfigurationManager implements Observable<Pair<EventType, Configurable>> {

	/**
	 * Check if Configurable is of a given type
	 */
	private static class ConfigurableByTypeFilter implements Predicate<Configurable> {

		private String type;

		public ConfigurableByTypeFilter(String type) {
			this.type = type;
		}

		@Override
		public boolean test(Configurable configurable) {
			return Objects.equals(type, configurable.getTypeId());
		}
	}

	/**
	 * Checks if a Configurable is stored at a given source
	 **/
	private static class ConfigurableBySourceFilter implements Predicate<Configurable> {

		private String source;

		public ConfigurableBySourceFilter(String source) {
			this.source = source;
		}

		@Override
		public boolean test(Configurable configurable) {
			String sourceName = configurable.getSource() == null ? null : configurable.getSource().getName();
			return Objects.equals(source, sourceName);
		}
	}

	/**
	 * Check if the Configurable is accessible by the current user
	 */
	private Predicate<Configurable> isAccessible = configurable -> {
		try {
			checkAccess(configurable.getTypeId(), configurable.getName(), null);
			return true;
		} catch (ConfigurationException e) {
			return false;
		}
	};

	/**
	 * Compares {@link Configurable}s.
	 */
	public static class ConfigurableComparator implements Comparator<Configurable> {

		@Override
		public int compare(Configurable o1, Configurable o2) {
			if (o1 == null) {
				return -1;
			}
			if (o2 == null) {
				return 1;
			}
			AbstractConfigurator<? extends Configurable> configurator1 = ConfigurationManager.getInstance()
			        .getAbstractConfigurator(o1.getTypeId());
			AbstractConfigurator<? extends Configurable> configurator2 = ConfigurationManager.getInstance()
			        .getAbstractConfigurator(o2.getTypeId());

			// sort by type name first
			if (!configurator1.getName().equals(configurator2.getName())) {
				return configurator1.getName().compareTo(configurator2.getName());
			}
			return o1.getName().compareTo(o2.getName());
		}

	}

	/**
	 * {@link ParameterHandler} which is used in
	 * {@link ConfigurationManager#createAndRegisterConfigurables(AbstractConfigurator, java.util.Map, java.util.Map, com.rapidminer.repository.internal.remote.RemoteRepository)}
	 * for mocking a {@link ParameterHandler} when retrieving the list of parameters.
	 */
	private static final ParameterHandler EMPTY_PARAMETER_HANDLER = new SimpleListBasedParameterHandler() {

		@Override
		public List<ParameterType> getParameterTypes() {
			return Collections.emptyList();
		}

	};

	/**
	 * URL from which configurations are loaded from RapidMiner Server via the ConfigurationServlet
	 * (includes trailing slash).
	 */
	public static final String RM_SERVER_CONFIGURATION_URL_PREFIX = "/api/rest/configuration/";

	/**
	 * User name of the default admin, used to check the access of a user to remote connections
	 */
	public static final String RM_SERVER_CONFIGURATION_USER_ADMIN = "admin";

	/**
	 * Source name if the remote repository is local (null)
	 */
	public static final String RM_SERVER_CONFIGURATION_SOURCE_NAME_LOCAL = "123%%%local%%%123";

	/** @since 8.2.1 */
	private static final String CONFIGURATION_TAG = "configuration";

	/** singleton instance */
	private static ConfigurationManager theInstance;

	/** Map from {@link Configurator#getTypeId()} to {@link Configurator}. */
	private Map<String, AbstractConfigurator<? extends Configurable>> configurators = new TreeMap<>();

	/** Loads configurations provided by this repository whenever the repository is connected
	 * and removes them again when the repository is disconnected. */
	private ConnectionListener updateOnConnectionStateListener = new ConnectionListener() {

		@Override
		public void connectionLost(ConnectionRepository rmServer) {
			if (rmServer instanceof  RemoteRepository) {
				removeFromRepository((RemoteRepository) rmServer);
			}
		}

		@Override
		public void connectionEstablished(ConnectionRepository rmServer) {
			if (rmServer instanceof  RemoteRepository) {
				loadFromRepository((RemoteRepository) rmServer);
			}
		}
	};

	/**
	 * Adds and removes listeners and configurables for {@link RemoteRepository RemoteRepositories}.
	 *
	 * @since 8.2.1
	 */
	private RepositoryManagerListener updateOnRepoManagerListener = new RepositoryManagerListener() {

		@Override
		public void repositoryWasAdded(Repository repository) {
			if (repository instanceof  RemoteRepository) {
				loadFromRepository((RemoteRepository) repository);
				((RemoteRepository) repository).addConnectionListener(updateOnConnectionStateListener);
				repository.addRepositoryListener(loadOnRefreshListener);
			}
		}

		@Override
		public void repositoryWasRemoved(Repository repository) {
			if (repository instanceof  RemoteRepository) {
				removeFromRepository((RemoteRepository) repository);
				((RemoteRepository) repository).removeConnectionListener(updateOnConnectionStateListener);
				repository.removeRepositoryListener(loadOnRefreshListener);
			}
		}
	};

	/** Reloads configurations provided by this repository whenever the root folder is refreshed. */
	private final RepositoryListener loadOnRefreshListener = new RepositoryListener() {

		@Override
		public void folderRefreshed(Folder folder) {
			if (folder instanceof RemoteRepository && consumeRefreshRequest(((RemoteRepository) folder).getRepository())) {
				loadFromRepository((RemoteRepository) folder);
			}
		}

		@Override
		public void entryRemoved(com.rapidminer.repository.Entry removedEntry, Folder parent, int oldIndex) {}

		@Override
		public void entryChanged(com.rapidminer.repository.Entry entry) {}

		@Override
		public void entryAdded(com.rapidminer.repository.Entry newEntry, Folder parent) {}
	};

	/** Private singleton constructor. */
	protected ConfigurationManager() {}

	/** mapping between configuration type ids and configurables */
	private Map<String, Map<ComparablePair<String, String>, Configurable>> configurables = new HashMap<>();

	/** stores refresh requests **/
	private Map<RemoteRepository, Integer> refreshRequests = Collections.synchronizedMap(new WeakHashMap<>());

	/** mapping configurables to permitted groups */
	private static Map<String, Map<ComparablePair<String, String>, Set<String>>> permittedGroups = new HashMap<>();

	private boolean initialized = false;

	private List<Observer<Pair<EventType, Configurable>>> observers = Collections
	        .synchronizedList(new LinkedList<Observer<Pair<EventType, Configurable>>>());

	private Object LOCK = new Object();

	public static synchronized void setInstance(ConfigurationManager manager) {
		if (theInstance != null) {
			throw new RuntimeException("Configuration manager already set.");
		}
		ConfigurationManager.theInstance = manager;
	}

	public static synchronized ConfigurationManager getInstance() {
		if (theInstance == null) {
			theInstance = new ClientConfigurationManager();
		}
		return theInstance;
	}

	/**
	 * Loads all parameters from a configuration file or database. The returned map uses (id,value)
	 * pairs as IDs and key-value parameter map as values.
	 *
	 * @since 6.2.0
	 */
	protected abstract Map<Pair<Integer, String>, Map<String, String>> loadAllParameters(
	        AbstractConfigurator<?> configurator) throws ConfigurationException;

	/**
	 * Registers a new {@link Configurator}. Will create GUI actions and JSF pages to configure it.
	 *
	 * @deprecated Extending {@link Configurator} is not recommended anymore. Extend
	 *             {@link AbstractConfigurator} and use {@link #register(AbstractConfigurator)}
	 *             instead.
	 */
	@Deprecated
	public synchronized void register(Configurator<? extends Configurable> configurator) {
		register((AbstractConfigurator<? extends Configurable>) configurator);
	}

	/**
	 * Registers a new {@link AbstractConfigurator}. Will create GUI actions and JSF pages to
	 * configure it.
	 *
	 * @since 6.2.0
	 */
	public synchronized void register(AbstractConfigurator<? extends Configurable> configurator) {

		if (configurator == null) {
			throw new NullPointerException("Registered configurator is null.");
		}
		LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.config.ConfigurationManager.registered",
		        configurator.getName());
		final String typeId = configurator.getTypeId();
		if (typeId == null) {
			throw new RuntimeException("typeID must not be null for " + configurator.getClass() + "!");
		}
		configurators.put(typeId, configurator);
		configurables.put(typeId, new TreeMap<>());

		if (permittedGroups.get(typeId) == null) {
			permittedGroups.put(typeId, new TreeMap<>());
		}

	}

	/**
	 * @return the {@link Configurator} with the given {@link Configurator#getTypeId()}.
	 * @deprecated use {@link #getAbstractConfigurator(String)} instead
	 * @throws IllegalArgumentException
	 *             in case the selected configurator is not a {@link Configurator}
	 **/
	@Deprecated
	public Configurator<? extends Configurable> getConfigurator(String typeId) {
		AbstractConfigurator<? extends Configurable> configurator = configurators.get(typeId);
		if (configurator != null && !(configurator instanceof Configurator)) {
			throw new IllegalArgumentException(
			        String.format("The selected Configurator with typeId %s is not of class Configurator but %s", typeId,
			                configurator.getClass().getSimpleName()));
		}
		return (Configurator<? extends Configurable>) configurator;
	}

	/**
	 * @return the {@link AbstractConfigurator} with the given
	 *         {@link AbstractConfigurator#getTypeId()}.
	 * @since 6.2.0
	 **/
	public AbstractConfigurator<? extends Configurable> getAbstractConfigurator(String typeId) {
		return configurators.get(typeId);
	}

	/** Returns all registered {@link Configurator#getTypeId()}s. */
	public List<String> getAllTypeIds() {
		List<String> result = new LinkedList<>();
		result.addAll(configurators.keySet());
		return result;
	}

	/**
	 * Returns <code>true</code> if there is <strong>no</strong> {@link Configurator} registered;
	 * <code>false</code> otherwise.
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return configurators.size() <= 0;
	}

	public boolean hasTypeId(String typeId) {
		return configurators.keySet().contains(typeId);
	}

	/**
	 * Refreshes the given repository
	 *
	 * @param source The repository that should be refreshed
	 * @throws RepositoryException if the refresh failed
	 */
	public void refresh(RemoteRepository source) throws RepositoryException {
		refreshRequests.merge(source, 1, Integer::sum);
		try {
			source.refresh();
		} catch (RepositoryException e) {
			refreshRequests.remove(source);
			throw e;
		}
	}

	/**
	 * Returns all configurable names. Better to use
	 * {@link #getAllConfigurableNamesAndSources(String)}.
	 *
	 * @param typeId
	 * @return the names of all configurables
	 * @deprecated Use {@link #getAllConfigurableNamesAndSources(String)} instead
	 */
	@Deprecated
	public List<String> getAllConfigurableNames(String typeId) {

		List<String> configurableNames = new LinkedList<>();
		List<ComparablePair<String, String>> namesAndSources = getAllConfigurableNamesAndSources(typeId);
		for (ComparablePair<String, String> key : namesAndSources) {
			if (!configurableNames.contains(key.getFirst())) {
				configurableNames.add(key.getFirst());
			}
		}
		return configurableNames;
	}

	/**
	 * Returns all the configurables as combination of name and source.
	 *
	 * @param typeId
	 * @return list with unique keys for all configurables
	 */
	public List<ComparablePair<String, String>> getAllConfigurableNamesAndSources(String typeId) {
		Map<ComparablePair<String, String>, Configurable> configurablesForType = configurables.get(typeId);
		if (configurablesForType == null) {
			throw new IllegalArgumentException("Unknown configurable type: " + typeId);
		}
		return new LinkedList<>(configurablesForType.keySet());
	}

	/**
	 * Looks up a {@link Configurable} of the given name and type. If there are two configurables
	 * with the same name and typeId, i.e. one located locally and one located on a RM Server, the
	 * local one would be returned. The configurable is first searched in the local connections and
	 * if there was no such configurable, it is searched in each existing RM Server.
	 *
	 * @param typeId
	 *            must be one of {@link #getAllTypeIds()}
	 * @param name
	 *            must be a {@link Configurable#getName()} where {@link Configurable} is registered
	 *            under the given type.
	 * @param accessor
	 *            represents the user accessing the repository. Can and should be taken from
	 *            {@link com.rapidminer.Process#getRepositoryAccessor()}.
	 * @return the configurable which was found first for the name and typeId
	 * @throws ConfigurationException
	 */
	public Configurable lookup(String typeId, String name, RepositoryAccessor accessor) throws ConfigurationException {
		checkAccess(typeId, name, accessor);
		Map<ComparablePair<String, String>, Configurable> nameAndSourceToConfigurable = configurables.get(typeId);
		if (nameAndSourceToConfigurable == null) {
			throw new ConfigurationException("No such configuration type: " + typeId);
		}
		// check first for local connections with this name; if there is no local connection with this name,
		// search for a remote connection with this name
		Configurable result = nameAndSourceToConfigurable.keySet().stream().filter(key -> key.getFirst().equals(name))
				.min(Comparator.comparing(key -> !key.getSecond().equals(ConfigurationManager.RM_SERVER_CONFIGURATION_SOURCE_NAME_LOCAL)))
				.map(nameAndSourceToConfigurable::get).orElse(null);
		if (result == null) {
			AbstractConfigurator<? extends Configurable> configurator = configurators.get(typeId);
			throw new ConfigurationException("No such configured object of name " + name + " of " + (configurator != null ? configurator.getName() : "typeId: " + typeId));
		}
		return result;
	}

	/**
	 * Checks access to the {@link Configurable} with the given type and name. If access is
	 * permitted, throws. The default implementation does nothing (everyone can access everything).
	 */
	protected void checkAccess(String typeId, String name, RepositoryAccessor accessor) throws ConfigurationException {}

	/**
	 * Adds the configurable to internal maps. Once they are added, they can be obtained via
	 * {@link #lookup(String, String, RepositoryAccessor)}.
	 */
	public void registerConfigurable(String typeId, Configurable configurable) throws ConfigurationException {
		boolean changed;
		synchronized (LOCK) {
			String source = getSourceNameForConfigurable(configurable);
			Map<ComparablePair<String, String>, Configurable> configurablesForType = configurables.get(typeId);
			if (configurablesForType == null) {
				throw new ConfigurationException("No such configuration type: " + typeId);
			}
			Configurable previous = configurablesForType.put(new ComparablePair<>(configurable.getName(), source),
			        configurable);

			if (permittedGroups.get(typeId).get(new ComparablePair<>(configurable.getName(), source)) == null) {
				permittedGroups.get(typeId).put(new ComparablePair<>(configurable.getName(), source), new HashSet<String>());
			}
			changed = previous != null;
		}

		// notify listeners of addition
		for (Observer<Pair<EventType, Configurable>> obs : observers) {
			if (changed) {
				obs.update(this, new Pair<>(ConfigurableEvent.EventType.CONFIGURABLE_REMOVED, configurable));
			}
			obs.update(this, new Pair<>(ConfigurableEvent.EventType.CONFIGURABLE_ADDED, configurable));
		}
	}

	/**
	 * Returns all currently registered {@link Configurable}s.
	 *
	 * @return
	 */
	public Collection<Configurable> getAllConfigurables() {
		List<Configurable> listOfConfigurables = new LinkedList<>();
		for (Map<ComparablePair<String, String>, Configurable> map : configurables.values()) {
			for (Configurable c : map.values()) {
				listOfConfigurables.add(c);
			}
		}
		return listOfConfigurables;
	}

	/**
	 * Inits the {@link ConfigurationManager}. This includes initial configuration loading as well
	 * as registering listeners to remote repositories.
	 */
	public void initialize() {
		if (initialized) {
			loadConfiguration();
			return;
		}
		loadConfiguration();
		// add listeners to already registered repositories as well as to all upcoming ones
		RepositoryManager repositoryManager = RepositoryManager.getInstance(null);
		repositoryManager.addRepositoryManagerListener(updateOnRepoManagerListener);
		for (RemoteRepository ra : repositoryManager.getRemoteRepositories()) {
			ra.addConnectionListener(this.updateOnConnectionStateListener);
			ra.addRepositoryListener(this.loadOnRefreshListener);
		}
		initialized = true;
	}

	/** Loads configurations from the given repository. */
	private void loadFromRepository(RemoteRepository ra) {

		// load configuration typeIds from this repository
		try {
			List<String> typeIds = ra.getClient().loadConfigurationTypes();
			ra.setTypeIds(typeIds);
		} catch (Exception e) {
			LogService.log(LogService.getRoot(), Level.WARNING, e,
			        "com.rapidminer.tools.config.ConfigurationManager.loading_configuration_types_error", ra.getName(),
			        e.toString());
		}

		// TODO Remove old entries from this repository in case of update
		for (String typeId : getAllTypeIds()) {
			AbstractConfigurator<?> configurator = getAbstractConfigurator(typeId);
			try {
				ResponseContainer response = ra.getClient().loadConfigurationType(typeId);

				if (response.getResponseCode() == 404) {
					LogService.getRoot().log(Level.INFO,
							"com.rapidminer.tools.config.ConfigurationManager.loading_configuration.unknown",
							new Object[]{typeId, ra.getName()});
					continue;
				}
				Document doc = XMLTools.parse(response.getInputStream());
				Map<Pair<Integer, String>, Map<String, String>> configurationParameters = fromXML(doc, configurator);
				int counter = configurationParameters.size();
				Map<Pair<Integer, String>, Set<String>> configurationPermittedGroups = permittedGroupsfromXML(doc,
						configurator);

				createAndRegisterConfigurables(configurator, configurationParameters, configurationPermittedGroups, ra);
				LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.config.ClientConfigurationManager.loaded_from_ra",
						new Object[]{ra.getName(), configurator.getName(), counter});

			} catch (FileNotFoundException fnfe) {
				LogService.getRoot().log(Level.INFO,
						"com.rapidminer.tools.config.ConfigurationManager.loading_configuration.unknown",
						new Object[]{typeId, ra.getName()});
			} catch (Exception e) {
				LogService.log(LogService.getRoot(), Level.WARNING, e,
						"com.rapidminer.tools.config.ClientConfigurationManager.error_loading_from_ra", ra.getName(),
						configurator.getName(), e.toString());
			}
		}

		Configurable config = new AbstractConfigurable() {

			@Override
			public String getTypeId() {
				return null;
			}
		};
		config.setSource(ra);
		for (Observer<Pair<EventType, Configurable>> obs : observers) {
			obs.update(this, new Pair<>(ConfigurableEvent.EventType.LOADED_FROM_REPOSITORY, config));
		}
	}

	/**
	 * Removes all {@link Configurable Configurables} that were pulled from the given repository
	 * from the {@link ConfigurationManager}.
	 *
	 * @param ra
	 * 		the remote repository whose configurables should be removed
	 * @since 8.2.1
	 */
	private static void removeFromRepository (RemoteRepository ra) {
		ConfigurationManager instance = getInstance();
		instance.getAllConfigurables().stream().filter(c -> c.getSource() == ra)
				.forEach(c -> instance.removeConfigurable(c.getTypeId(), c.getName(), ra.getAlias()));
	}

	/**
	 * Checks if {@code requestCount} can be initialized or decremented
	 *
	 * @param repository
	 * 		The repository that should be refreshed (unused)
	 * @param requestCount
	 * 		The current refresh request count
	 * @return 0 if {@code requestCount} is {@code null}, {@code requestCount}-1 if {@code requestCount} > 0
	 * @throws IllegalArgumentException
	 * 		if {@code requestCount} is <= 0
	 */
	private static Integer decrementOrInitializeRequestCount(final RemoteRepository repository, final Integer requestCount) {
		if (requestCount == null) {
			//Initial call
			return 0;
		} else if (requestCount <= 0) {
			//Already loaded, no request
			throw new IllegalArgumentException("requestCount should not be 0");
		} else {
			//Request exists, decrement
			return requestCount - 1;
		}
	}

	/**
	 * Atomically decrements the request counter
	 *
	 * @param source
	 * @return true if a refresh request exists for the source
	 */
	private boolean consumeRefreshRequest(RemoteRepository source) {
		try {
			refreshRequests.compute(source, ConfigurationManager::decrementOrInitializeRequestCount);
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	/**
	 * Loads all configurations from the configuration database or file.
	 * <p>
	 * Note: In general there is no need to call this method, cause the {@link ConfigurationManager}
	 * should notice all changes. But some edge cases might require a reload (e.g. configuration
	 * file change).
	 * </p>
	 */
	public void loadConfiguration() {
		for (AbstractConfigurator<? extends Configurable> configurator : configurators.values()) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.config.ConfigurationManager.loading_configuration",
			        configurator.getName());
			Map<Pair<Integer, String>, Map<String, String>> parameters;
			try {
				parameters = loadAllParameters(configurator);
			} catch (ConfigurationException e1) {
				LogService.getRoot().log(Level.WARNING,
				        I18N.getMessage(LogService.getRoot().getResourceBundle(),
				                "com.rapidminer.tools.config.ConfigurationManager.loading_configuration_error",
				                configurator.getName(), e1),
				        e1);
				continue;
			}
			createAndRegisterConfigurables(configurator, parameters, null, null);
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.config.ConfigurationManager.loaded_configurations",
			        new Object[] { configurables.get(configurator.getTypeId()).size(), configurator.getName() });
		}
	}

	/**
	 * Creates a new {@link Configurable} and registers it in this manager.
	 *
	 * @param configurator
	 * @param parameters
	 *            parameters for the given configurables
	 * @param configurationPermittedGroups
	 *            user group permissions for the given configurables
	 * @param sourceRA
	 *            source of the given configurables
	 */
	private void createAndRegisterConfigurables(AbstractConfigurator<? extends Configurable> configurator,
	        Map<Pair<Integer, String>, Map<String, String>> parameters,
	        Map<Pair<Integer, String>, Set<String>> configurationPermittedGroups, RemoteRepository sourceRA) {
		for (Entry<Pair<Integer, String>, Map<String, String>> entry : parameters.entrySet()) {
			try {
				Map<String, String> paramKeysToParamValues = new HashMap<>();
				Map<String, ParameterType> paramTypeKeysToParamTypes = parameterListToMap(
				        configurator.getParameterTypes(EMPTY_PARAMETER_HANDLER));
				for (Entry<String, String> parameter : entry.getValue().entrySet()) {
					String paramKey = parameter.getKey();
					ParameterType type = paramTypeKeysToParamTypes.get(paramKey);
					String paramValue = parameter.getValue();
					if (paramValue == null && type != null) {
						paramValue = type.getDefaultValueAsString();
					}
					paramKeysToParamValues.put(paramKey, paramValue);
				}
				String configurableName = entry.getKey().getSecond();
				Configurable configurable = configurator.create(configurableName, paramKeysToParamValues);
				int id = entry.getKey().getFirst();
				if (id != -1) {
					configurable.setId(id);
				}
				configurable.setSource(sourceRA);
				if (configurationPermittedGroups != null) {
					Pair<Integer, String> pair = new Pair<>(id, configurableName);
					Set<String> newPermittedGroups = configurationPermittedGroups.get(pair);
					setPermittedGroupsForConfigurable(configurable, newPermittedGroups);
				}
				registerConfigurable(configurator.getTypeId(), configurable);
			} catch (ConfigurationException e) {
				LogService.getRoot().log(Level.WARNING,
				        I18N.getMessage(LogService.getRoot().getResourceBundle(),
				                "com.rapidminer.tools.config.ConfigurationManager.configuring_configurable_error",
				                configurator.getName(), e),
				        e);
			}

		}

	}

	/**
	 * Creates and <strong>registers</strong> a {@link Configurable}.
	 *
	 * @param typeId
	 * @param name
	 * @return
	 * @throws ConfigurationException
	 */
	public Configurable create(String typeId, String name) throws ConfigurationException {
		Configurable configurable = createWithoutRegistering(typeId, name, null);
		registerConfigurable(typeId, configurable);
		return configurable;
	}

	/**
	 * Creates a new {@link Configurable} without registering it.
	 *
	 * @param typeId
	 * @param name
	 * @return the created configurable
	 * @deprecated Use {@link #createWithoutRegistering(String, String, RemoteRepository)} instead
	 * @throws ConfigurationException
	 */
	@Deprecated
	public Configurable createWithoutRegistering(String typeId, String name) throws ConfigurationException {
		return createWithoutRegistering(typeId, name, null);
	}

	/**
	 * Creates a new {@link Configurable} without registering it.
	 *
	 * @param typeId
	 * @param name
	 * @param source
	 *            source of the configurable, can be null (for local configurables)
	 * @return the created configurable
	 * @throws ConfigurationException
	 */
	public Configurable createWithoutRegistering(String typeId, String name, RemoteRepository source)
	        throws ConfigurationException {
		AbstractConfigurator<? extends Configurable> configurator = configurators.get(typeId);
		if (configurator == null) {
			throw new ConfigurationException("Unknown configurable type: " + typeId);
		}
		final Configurable configurable = configurator.create(name, Collections.<String, String> emptyMap());
		if (source != null) {
			configurable.setSource(source);
		}
		return configurable;
	}

	/**
	 * Saves the configuration, e.g. when RapidMiner exits.
	 *
	 * @throws ConfigurationException
	 */
	public void saveConfiguration() throws ConfigurationException {
		for (String typeId : getAllTypeIds()) {
			saveConfiguration(typeId);
		}
	}

	/**
	 * Saves one configuration with the given typeID
	 *
	 * @throws ConfigurationException
	 */
	public abstract void saveConfiguration(String typeId) throws ConfigurationException;

	/**
	 * Returns the permitted user groups of a configurable
	 *
	 * @param configurable
	 * @return the permitted user groups of the given configurable
	 */
	public Set<String> getPermittedGroupsForConfigurable(Configurable configurable) {

		String source = getSourceNameForConfigurable(configurable);
		String typeId = configurable.getTypeId();
		if (permittedGroups.get(typeId) == null) {
			permittedGroups.put(typeId, new TreeMap<ComparablePair<String, String>, Set<String>>());
		}
		if (permittedGroups.get(typeId).get(new ComparablePair<>(configurable.getName(), source)) == null) {
			permittedGroups.get(typeId).put(new ComparablePair<>(configurable.getName(), source), new HashSet<String>());
		}

		return new HashSet<>(permittedGroups.get(typeId).get(new ComparablePair<>(configurable.getName(), source)));
	}

	/**
	 * Returns the name of the configurable source ({@link RemoteRepository})
	 *
	 * @param configurable
	 * @return the name of the source of the given configurable
	 */
	private static String getSourceNameForConfigurable(Configurable configurable) {
		return configurable.getSource() == null ? ConfigurationManager.RM_SERVER_CONFIGURATION_SOURCE_NAME_LOCAL
		        : configurable.getSource().getName();
	}

	/**
	 * Sets the permitted user groups of a configurable
	 *
	 * @param configurable
	 * @param newPermittedGroups
	 */
	public void setPermittedGroupsForConfigurable(Configurable configurable, Set<String> newPermittedGroups) {

		String source = getSourceNameForConfigurable(configurable);
		String typeId = configurable.getTypeId();

		if (permittedGroups.get(typeId) == null) {
			permittedGroups.put(typeId, new TreeMap<ComparablePair<String, String>, Set<String>>());
		}
		if (permittedGroups.get(typeId).get(new ComparablePair<>(configurable.getName(), source)) == null) {
			permittedGroups.get(typeId).put(new ComparablePair<>(configurable.getName(), source), new HashSet<String>());
		} else {
			permittedGroups.get(typeId).get(new ComparablePair<>(configurable.getName(), source)).clear();
		}
		permittedGroups.get(typeId).get(new ComparablePair<>(configurable.getName(), source)).addAll(newPermittedGroups);

	}

	/**
	 * Maps keys of ParameterTypes to ParameterTypes
	 */
	public Map<String, ParameterType> parameterListToMap(List<ParameterType> parameterTypes) {
		Map<String, ParameterType> result = new HashMap<>();
		for (ParameterType type : parameterTypes) {
			result.put(type.getKey(), type);
		}
		return result;
	}

	/**
	 * Removes the {@link Configurable} with the given type id and name.
	 *
	 * @param typeId
	 * @param name
	 * @deprecated Use {@link #removeConfigurable(String, String, String)} instead
	 */
	@Deprecated
	public void removeConfigurable(String typeId, String name) {
		removeConfigurable(typeId, name, null);
	}

	/**
	 * Removes the {@link Configurable} with the given type id, name and source.
	 *
	 * @param typeId
	 * @param name
	 * @param source
	 *            name of the source of the configurable, can be null (for local configurables)
	 */
	public void removeConfigurable(String typeId, String name, String source) {
		Configurable removedConfigurable = null;

		if (source == null) {
			source = RM_SERVER_CONFIGURATION_SOURCE_NAME_LOCAL;
		}
		removedConfigurable = configurables.get(typeId).remove(new ComparablePair<>(name, source));
		permittedGroups.get(typeId).remove(new ComparablePair<>(name, source));

		// notify listeners of removal
		if (removedConfigurable != null) {
			for (Observer<Pair<EventType, Configurable>> obs : observers) {
				obs.update(this, new Pair<>(ConfigurableEvent.EventType.CONFIGURABLE_REMOVED, removedConfigurable));
			}
		}
	}

	/**
	 * Returns the xml representation of the registered configurables.
	 *
	 * @deprecated Use {@link #getConfigurablesAsXML(AbstractConfigurator, boolean)} instead.
	 */
	@Deprecated
	public Document getConfigurablesAsXML(Configurator<? extends Configurable> configurator, boolean onlyLocal) {
		return getConfigurablesAsXML((AbstractConfigurator<? extends Configurable>) configurator, onlyLocal);
	}

	/**
	 * Returns the xml representation of the registered configurables.
	 *
	 * @return the configurable as XML document
	 * @since 6.2.0
	 */
	public Document getConfigurablesAsXML(AbstractConfigurator<? extends Configurable> configurator, boolean onlyLocal) {
		Document doc = XMLTools.createDocument();
		Element root = doc.createElement(CONFIGURATION_TAG);
		doc.appendChild(root);
		for (Configurable configurable : configurables.get(configurator.getTypeId()).values()) {
			try {
				checkAccess(configurator.getTypeId(), configurable.getName(), null);
			} catch (ConfigurationException e) {
				continue;
			}
			if (onlyLocal && configurable.getSource() != null) {
				continue;
			}
			root.appendChild(toXML(doc, configurator, configurable));
		}
		return doc;
	}

	/**
	 * Returns the xml representation of the given configurables that match the given type,
	 * are stored at the given source and are accessible by the current user.
	 *
	 * @param typeId
	 *            the configurables of this typeId should be returned
	 * @param configurables
	 *            the configurables of one source
	 * @param source
	 *            the source of the given configurables, can be null (for local configurables)
	 * @return the configurables as XML document
	 * @since 6.4.0
	 */
	public Document getConfigurablesAsXML(String typeId, List<Configurable> configurables, String source) {
		return getConfigurablesAsXMLAndChangeEncryption(typeId, configurables, source, null, null);
	}

	/**
	 * Returns the xml representation of the given configurables that match the given type,
	 * are stored at the given source and are accessible by the current user,
	 * by using the given old key to decrypt the information and the specified new key to encrypt the information.
	 *
	 * @param typeId
	 *            the configurables of this typeId should be returned
	 * @param configurables
	 *            the configurables of one source
	 * @param source
	 *            the source of the given configurables, can be null (for local configurables)
	 * @param decryptKey
	 *            {@link Key} used to decrypt the configurable values
	 * @param encryptKey
	 *            {@link Key} which should be used to encrypt them in the returned xml
	 * @return the configurables as XML document
	 * @since 7.6.2
	 */
	public Document getConfigurablesAsXMLAndChangeEncryption(String typeId, List<Configurable> configurables, String source, Key decryptKey, Key encryptKey) {
		Document doc = XMLTools.createDocument();
		Element root = doc.createElement(CONFIGURATION_TAG);
		doc.appendChild(root);
		AbstractConfigurator<? extends Configurable> configurator = ConfigurationManager.getInstance()
		        .getAbstractConfigurator(typeId);
		//Filter results
		Predicate<Configurable> byType = new ConfigurableByTypeFilter(typeId);
		Predicate<Configurable> bySource = new ConfigurableBySourceFilter(source);
		List<Configurable> filteredList = configurables.stream()
				.filter(byType)
				.filter(bySource)
				.filter(isAccessible).collect(Collectors.toList());
		for (Configurable configurable : filteredList) {
			//This is used by the getConfigurablesAsXML without decryptKey & encryptKey
			if (decryptKey == null && encryptKey == null) {
				root.appendChild(toXML(doc, configurator, configurable));
			} else {
				root.appendChild(toXMLAndChangeEncryption(doc, configurator, configurable, decryptKey, encryptKey));
			}
		}
		return doc;
	}

	/**
	 * Creates an XML-element where the tag name equals {@link Configurator#getTypeId()}. This tag
	 * has name and id attributes corresponding to {@link Configurable#getName()} and
	 * {@link Configurable#getId()}. The parameters are encoded as tags whose name matches
	 * {@link ParameterType#getKey()} and the text-contents of these tags matches the parameter
	 * value.
	 *
	 * @deprecated Use {@link #toXML(Document, AbstractConfigurator, Configurable)} instead
	 */
	@Deprecated
	public static Element toXML(Document doc, Configurator<? extends Configurable> configurator, Configurable configurable) {
		return toXML(doc, (AbstractConfigurator<? extends Configurable>) configurator, configurable);
	}

	/**
	 * Creates an XML-element where the tag name equals {@link Configurator#getTypeId()}. This tag
	 * has name and id attributes corresponding to {@link Configurable#getName()} and
	 * {@link Configurable#getId()}. The parameters are encoded as tags whose name matches
	 * {@link ParameterType#getKey()} and the text-contents of these tags matches the parameter
	 * value.
	 *
	 * @since 6.2.0
	 */
	public static Element toXML(Document doc, AbstractConfigurator<? extends Configurable> configurator,
	        Configurable configurable) {
		Element element = doc.createElement(configurator.getTypeId());
		element.setAttribute("name", configurable.getName());
		if (configurable.getId() != -1) {
			element.setAttribute("id", String.valueOf(configurable.getId()));
		}
		String source = getSourceNameForConfigurable(configurable);
		if (permittedGroups != null && permittedGroups.get(configurable.getTypeId()) != null && permittedGroups
		        .get(configurable.getTypeId()).get(new ComparablePair<>(configurable.getName(), source)) != null) {
			Element permittedGroupsElement = doc.createElement("permittedGroups");
			Set<String> configPermittedGroups = permittedGroups.get(configurable.getTypeId())
			        .get(new ComparablePair<>(configurable.getName(), source));
			for (String group : configPermittedGroups) {
				Element valueElement = doc.createElement("value");
				valueElement.appendChild(doc.createTextNode(group));
				permittedGroupsElement.appendChild(valueElement);
			}
			element.appendChild(permittedGroupsElement);
		}
		for (Entry<String, String> param : configurable.getParameters().entrySet()) {
			String key = param.getKey();
			String value = null;

			// if we use AbstractConfigurables, do not use the method which converts parameters to
			// non-xml form here, otherwise passwords would be saved in plaintext
			if (configurable instanceof AbstractConfigurable) {
				value = ((AbstractConfigurable) configurable).getParameterAsXMLString(key);
			} else {
				value = configurable.getParameter(key);
			}

			// skip null values on save
			if (value == null) {
				continue;
			}

			Element paramElement = doc.createElement(key);
			paramElement.appendChild(doc.createTextNode(value)); // This escapes the value
			element.appendChild(paramElement);
		}
		return element;
	}

	/**
	 * Returns the xml representation of the registered configurables by using the given old key to
	 * decrypt the information and the specified new key to encrypt the information.
	 *
	 * @param configurator
	 * @param onlyLocal
	 * @param decryptKey
	 *            {@link Key} used to decrypt the configurable values
	 * @param encryptKey
	 *            {@link Key} which should be used to encrypt them in the returned xml
	 * @deprecated Use
	 *             {@link #getConfigurablesAsXMLAndChangeEncryption(AbstractConfigurator, boolean, Key, Key)}
	 *             instead
	 */
	@Deprecated
	public Document getConfigurablesAsXMLAndChangeEncryption(Configurator<? extends Configurable> configurator,
	        boolean onlyLocal, Key decryptKey, Key encryptKey) {
		return getConfigurablesAsXMLAndChangeEncryption((AbstractConfigurator<? extends Configurable>) configurator,
		        onlyLocal, decryptKey, encryptKey);
	}

	/**
	 * Returns the xml representation of the registered configurables by using the given old key to
	 * decrypt the information and the specified new key to encrypt the information.
	 *
	 * @param configurator
	 * @param onlyLocal
	 * @param decryptKey
	 *            {@link Key} used to decrypt the configurable values
	 * @param encryptKey
	 *            {@link Key} which should be used to encrypt them in the returned xml
	 * @return
	 * @since 6.2.0
	 */
	public Document getConfigurablesAsXMLAndChangeEncryption(AbstractConfigurator<? extends Configurable> configurator,
	        boolean onlyLocal, Key decryptKey, Key encryptKey) {
		Document doc = XMLTools.createDocument();
		Element root = doc.createElement(CONFIGURATION_TAG);
		doc.appendChild(root);
		for (Configurable configurable : configurables.get(configurator.getTypeId()).values()) {
			if (onlyLocal && configurable.getSource() != null) {
				continue;
			}
			root.appendChild(toXMLAndChangeEncryption(doc, configurator, configurable, decryptKey, encryptKey));
		}
		return doc;
	}

	/**
	 * Creates an XML-element where the tag name equals {@link Configurator#getTypeId()}. This tag
	 * has name and id attributes corresponding to {@link Configurable#getName()} and
	 * {@link Configurable#getId()}. The parameters are encoded as tags whose name matches
	 * {@link ParameterType#getKey()} and the text-contents of these tags matches the parameter
	 * value. Uses the specified old key to decrypt parameter values and the new key to encrypt them
	 * again.
	 */
	private static Element toXMLAndChangeEncryption(Document doc, AbstractConfigurator<? extends Configurable> configurator,
	        Configurable configurable, Key decryptKey, Key encryptKey) {
		Element element = doc.createElement(configurator.getTypeId());
		element.setAttribute("name", configurable.getName());
		if (configurable.getId() != -1) {
			element.setAttribute("id", String.valueOf(configurable.getId()));
		}
		String source = getSourceNameForConfigurable(configurable);
		if (permittedGroups != null && permittedGroups.get(configurable.getTypeId()) != null && permittedGroups
				.get(configurable.getTypeId()).get(new ComparablePair<>(configurable.getName(), source)) != null) {
			Element permittedGroupsElement = doc.createElement("permittedGroups");
			Set<String> configPermittedGroups = permittedGroups.get(configurable.getTypeId())
					.get(new ComparablePair<>(configurable.getName(), source));
			for (String group : configPermittedGroups) {
				Element valueElement = doc.createElement("value");
				valueElement.appendChild(doc.createTextNode(group));
				permittedGroupsElement.appendChild(valueElement);
			}
			element.appendChild(permittedGroupsElement);
		}
		for (Entry<String, String> param : configurable.getParameters().entrySet()) {
			String key = param.getKey();
			String value = null;

			// if we use AbstractConfigurables, potentially decrypt in-memory encrypted configurable
			// and encrypt it with the new key
			if (configurable instanceof AbstractConfigurable) {
				value = ((AbstractConfigurable) configurable).getParameterAndChangeEncryption(key, decryptKey, encryptKey);
			} else {
				// otherwise just store in plaintext
				value = configurable.getParameter(key);
			}

			// skip null values on save
			if (value == null) {
				continue;
			}

			Element paramElement = doc.createElement(key);
			paramElement.appendChild(doc.createTextNode(value));
			element.appendChild(paramElement);
		}
		return element;
	}

	/**
	 * The returned map uses (id,value) pairs as IDs and key-value parameter map as values.
	 *
	 * @see #toXML(Document, Configurator, Configurable)
	 * @deprecated use {@link #fromXML(Document, AbstractConfigurator)} instead
	 */
	@Deprecated
	public static Map<Pair<Integer, String>, Map<String, String>> fromXML(Document doc,
	        Configurator<? extends Configurable> configurator) throws ConfigurationException {
		return fromXML(doc, (AbstractConfigurator<? extends Configurable>) configurator);
	}

	/**
	 * The returned map uses (id,value) pairs as IDs and key-value parameter map as values.
	 *
	 * @see #toXML(Document, AbstractConfigurator, Configurable)
	 * @since 6.2.0
	 */
	public static Map<Pair<Integer, String>, Map<String, String>> fromXML(Document doc,
	        AbstractConfigurator<? extends Configurable> configurator) throws ConfigurationException {
		Map<Pair<Integer, String>, Map<String, String>> result = new TreeMap<>(Comparator.comparing(Pair::getSecond));
		Element root = doc.getDocumentElement();
		if (!CONFIGURATION_TAG.equals(root.getTagName())) {
			throw new ConfigurationException("XML root tag must be <configuration>");
		}

		for (Element element : XMLTools.getChildElements(root, configurator.getTypeId())) {
			String name = element.getAttribute("name");
			if (name == null || name.isEmpty()) {
				throw new ConfigurationException("Malformed configuration: name missing");
			}
			String idStr = element.getAttribute("id");
			int id = -1;
			if (idStr != null && !idStr.isEmpty()) {
				try {
					id = Integer.parseInt(idStr);
				} catch (NumberFormatException e) {
					throw new ConfigurationException("Malformed configuration: Illegal ID: " + idStr);
				}
			}
			HashMap<String, String> parameters = new HashMap<>();
			for (Element paramElem : XMLTools.getChildElements(element)) {
				String key = paramElem.getTagName();
				if (key.equals("permittedGroups")) {
					continue;
				}
				String value = paramElem.getTextContent();
				parameters.put(key, value);
			}
			result.put(new Pair<>(id, name), parameters);
		}
		return result;
	}

	/**
	 * The returned list contains (id,name) pairs with the new ids from the configurables that have
	 * been saved on the server
	 */
	public static List<Pair<Integer, String>> newIdsFromXML(Document doc) throws ConfigurationException {
		Element root = doc.getDocumentElement();
		if (!CONFIGURATION_TAG.equals(root.getTagName())) {
			throw new ConfigurationException("XML root tag must be <configuration>");
		}
		List<Pair<Integer, String>> newIds = new LinkedList<>();
		for (Element element : XMLTools.getChildElements(root, "newIds")) {
			for (Element newId : XMLTools.getChildElements(element)) {
				int id = -1;
				String name = "";
				for (Element idElement : XMLTools.getChildElements(newId, "id")) {
					id = Integer.parseInt(idElement.getTextContent());
				}
				for (Element nameElement : XMLTools.getChildElements(newId, "name")) {
					name = nameElement.getTextContent();
				}
				newIds.add(new Pair<>(id, name));
			}
		}
		return newIds;
	}

	/**
	 * The returned map uses (id,value) pairs as IDs and permitted user group lists as values.
	 */
	public static Map<Pair<Integer, String>, Set<String>> permittedGroupsfromXML(Document doc,
	        AbstractConfigurator<? extends Configurable> configurator) throws ConfigurationException {
		Map<Pair<Integer, String>, Set<String>> result = new TreeMap<>(Comparator.comparing(Pair::getSecond));

		Element root = doc.getDocumentElement();
		if (!CONFIGURATION_TAG.equals(root.getTagName())) {
			throw new ConfigurationException("XML root tag must be <configuration>");
		}

		for (Element element : XMLTools.getChildElements(root, configurator.getTypeId())) {
			String name = element.getAttribute("name");
			if (name == null || name.isEmpty()) {
				throw new ConfigurationException("Malformed configuration: name missing");
			}
			String idStr = element.getAttribute("id");
			int id = -1;
			if (idStr != null && !idStr.isEmpty()) {
				try {
					id = Integer.parseInt(idStr);
				} catch (NumberFormatException e) {
					throw new ConfigurationException("Malformed configuration: Illegal ID: " + idStr);
				}
			}

			HashSet<String> permittedGroups = new HashSet<>();
			for (Element elem : XMLTools.getChildElements(element)) {
				if (elem.getTagName().equals("permittedGroups")) {

					for (Element value : XMLTools.getChildElements(elem)) {
						permittedGroups.add(value.getTextContent());
					}
					break;
				}
			}
			result.put(new Pair<>(id, name), permittedGroups);
		}
		return result;
	}

	/**
	 * <strong>WARNING:</strong> This method replaces all {@link Configurable}s with a given source
	 * in this manager with the given ones. While this method works, no {@link Configurable}s can be
	 * added/removed in the meantime.
	 *
	 * @param newConfigurables
	 * @deprecated Use {@link #replaceConfigurables(Collection, String)} instead
	 */
	@Deprecated
	public void replaceConfigurables(Collection<Configurable> newConfigurables) {
		replaceConfigurables(newConfigurables, null);
	}

	/**
	 * <strong>WARNING:</strong> This method replaces all {@link Configurable}s with a given source
	 * in this manager with the given ones. While this method works, no {@link Configurable}s can be
	 * added/removed in the meantime.
	 *
	 * @param newConfigurables
	 * @param source
	 *            the source of the replacing configurables, can be null for local configurables
	 */
	public void replaceConfigurables(Collection<Configurable> newConfigurables, final String source) {

		synchronized (LOCK) {

			// remove all existing configurables with the given source
			for (Map<ComparablePair<String, String>, Configurable> map : configurables.values()) {

				Set<ComparablePair<String, String>> keySet = new HashSet<>(map.keySet());
				for (ComparablePair<String, String> confKey : keySet) {

					boolean noRegConfSource = map.get(confKey).getSource() == null;
					if (noRegConfSource && source == null || noRegConfSource && confKey.getSecond().equals(source)) {
						map.remove(confKey);
					}
				}
			}

			// read new configurables
			for (Configurable newConfig : newConfigurables) {
				Map<ComparablePair<String, String>, Configurable> map = configurables.get(newConfig.getTypeId());
				map.put(new ComparablePair<>(newConfig.getName(), getSourceNameForConfigurable(newConfig)), newConfig);
			}

		}

		// notify listeners of change
		for (Observer<Pair<EventType, Configurable>> obs : observers) {
			obs.update(this, new Pair<>(ConfigurableEvent.EventType.CONFIGURABLES_CHANGED, (Configurable) null));
		}
	}

	/**
	 * Adds an observer that will always be notified <strong>outside</strong> the EDT.
	 */
	@Override
	public void addObserver(Observer<Pair<EventType, Configurable>> observer, boolean onEDT) {
		observers.add(observer);
	}

	@Override
	public void removeObserver(Observer<Pair<EventType, Configurable>> observer) {
		observers.remove(observer);
	}

	/**
	 * Adds an observer that will always be notified <strong>outside</strong> the EDT.
	 */
	@Override
	public void addObserverAsFirst(Observer<Pair<EventType, Configurable>> observer, boolean onEDT) {
		observers.add(0, observer);
	}

}
