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
package com.rapidminer.tools.parameter.admin;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import javax.swing.Timer;

import com.rapidminer.gui.tools.MultiSwingWorker;
import com.rapidminer.tools.LogService;


/**
 * Administrators can provide an URL or path to an enforced configuration file via the environment variable {@value SystemEnvironmentParameterProvider#ENFORCE_CONFIG_ENV}.
 * Settings contained in this file can't be changed by the user.
 * If the file or url is not accessible on startup an error dialog is displayed and a RuntimeException is thrown,
 * which will prevent RapidMiner from starting.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0.0
 * @see SystemEnvironmentParameterProvider
 */
public final class ParameterEnforcer {

	/**
	 * Rate in seconds in which the admin config should be refetched
	 */
	public static final String RAPIDMINER_CONFIG_REFRESH = "rapidminer.admin_config.refetch_seconds";

	/**
	 * Reference to another config file, useful for local file based configs
	 */
	public static final String RAPIDMINER_REMOTE_CONFIG = "rapidminer.admin_config.location";

	/**
	 * Shortest refetch rate
	 */
	private static final long MIN_REFETCH = Duration.ofMinutes(1).toMillis();

	/**
	 * Default refetch rate
	 */
	private static final long DEFAULT_REFETCH = Duration.ofHours(1).toMillis();

	/**
	 * All available ParameterProviders, ordered by importance
	 */
	private static final List<ParameterProvider> PARAMETER_PROVIDERS = Collections.unmodifiableList(Arrays.asList(new WindowsRegistryParameterProvider(), new InstallationFolderParameterProvider(), new SystemEnvironmentParameterProvider()));

	/**
	 * Callback function to set values
	 */
	private final BiConsumer<String, String> setValue;

	/**
	 * Callback function to get values
	 */
	private final Function<String, String> getValue;

	/**
	 * Callback function to get default values
	 */
	private final Function<String, String> getDefaultValue;

	/**
	 * Keeps the original values
	 * <p>
	 * Must be mutable
	 */
	private final Map<String, String> originalValues = new HashMap<>();

	/**
	 * Currently loaded properties
	 * <p>
	 * Must be mutable
	 */
	private Map<String, String> properties = new HashMap<>();

	/**
	 * Swing timer used to refetch the properties
	 */
	private Timer timer;

	/**
	 * Creates a new instance
	 *
	 * @param setValue
	 * 		The callback function to set the value
	 * @param getValue
	 * 		The callback function to get a value
	 * @parame getDefaultValue
	 *      Returns a default value
	 */
	public ParameterEnforcer(BiConsumer<String, String> setValue, Function<String, String> getValue, Function<String, String> getDefaultValue) {
		this.setValue = setValue;
		this.getValue = getValue;
		this.getDefaultValue = getDefaultValue;
	}

	/**
	 * Returns the value of the given key
	 *
	 * @param key
	 * 		must not be null
	 * @return the value or null if not found
	 */
	public String getProperty(String key) {
		return properties.get(key);
	}

	/**
	 * Checks if the given key is enforced
	 *
	 * @param key
	 * 		must not be {@code null}
	 * @return {@code true} if the key is enforced
	 */
	public boolean containsKey(String key) {
		return properties.containsKey(key);
	}

	/**
	 * Returns the original value of an enforced property
	 *
	 * @param key
	 * 		must not be null
	 * @return the original value
	 */
	public String getOriginalValue(String key) {
		return originalValues.get(key);
	}

	/**
	 * Returns {@code true} if the {@link #properties} map contains no key-value mappings.
	 *
	 * @return {@code true} if the {@link #properties} map contains no key-value mappings
	 */
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	/**
	 * Loads the data and starts the timer
	 */
	public void init() {
		try {
			reloadProperties();
		} catch (ProvidedConfigurationException pce) {
			LogService.getRoot().log(Level.SEVERE, pce.getDialogMessage(), pce);
			// Show error message and wait
			StartupFailedDialogProvider.showErrorMessage(pce);
			throw pce;
		}
		restartTimer();
	}

	/**
	 * Reloads the properties
	 */
	private synchronized void reloadProperties() {
		try {
			Optional<Map<String, String>> adminSettings = PARAMETER_PROVIDERS.stream().map(ParameterProvider::readProperties).filter(Objects::nonNull).findFirst();
			// read remote config
			Optional<Map<String, String>> remoteAdminSetting = adminSettings.map(m -> m.get(RAPIDMINER_REMOTE_CONFIG)).map(FileParameterProvider::new).map(ParameterProvider::readProperties);
			// apply properties
			applyProperties(remoteAdminSetting.orElse(adminSettings.orElseGet(HashMap::new)));
		} catch (ProvidedConfigurationException pce) {
			throw pce;
		} catch (Exception e) {
			throw new ProvidedConfigurationException(e.getMessage(), e);
		}
	}

	/**
	 * Applies the properties on the ParameterService
	 *
	 * @param newProperties
	 * 		The new properties
	 */
	private void applyProperties(Map<String, String> newProperties) {
		Map<String, String> oldProperties = properties;
		// Add Telemetry specific settings to the enforced values map
		TelemetryDependentSettings.addSettings(newProperties);
		properties = newProperties;
		// set new properties
		properties.forEach((key, value) -> {
			if (!Objects.equals(value, oldProperties.remove(key))) {
				// Don't override null values
				if (!originalValues.containsKey(key)) {
					originalValues.putIfAbsent(key, getValue.apply(key));
				}
				setValue.accept(key, value);
			}
		});
		// clean up
		oldProperties.forEach((key, value) -> setValue.accept(key, Objects.toString(originalValues.remove(key), getDefaultValue.apply(key))));
	}

	/**
	 * (Re-)starts the timer
	 */
	private void restartTimer() {
		int timeTillNextIteration;
		try {
			timeTillNextIteration = (int) Math.max(Duration.ofSeconds(Long.parseLong(properties.get(RAPIDMINER_CONFIG_REFRESH))).toMillis(), MIN_REFETCH);
		} catch (NumberFormatException nfe) {
			timeTillNextIteration = (int) DEFAULT_REFETCH;
		}
		// Update timer
		if (timer == null) {
			timer = new Timer(timeTillNextIteration, e -> readAndRestart());
			timer.setRepeats(false);
		} else if (timeTillNextIteration != timer.getDelay()) {
			timer.setInitialDelay(timeTillNextIteration);
		}
		timer.restart();
	}

	/**
	 * Reloads the properties and starts the next timer
	 */
	private void readAndRestart() {
		new MultiSwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() {
				try {
					reloadProperties();
				} catch (ProvidedConfigurationException pce) {
					LogService.getRoot().log(Level.WARNING, pce.getDialogMessage(), pce);
				}
				restartTimer();
				return null;
			}
		}.start();
	}

}
