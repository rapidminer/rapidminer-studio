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
package com.rapidminer.connection.valueprovider.handler;

import static com.rapidminer.connection.valueprovider.handler.ValueProviderUtils.PLACEHOLDER_INDICATOR;
import static com.rapidminer.connection.valueprovider.handler.ValueProviderUtils.PLACEHOLDER_OPENING;
import static com.rapidminer.connection.valueprovider.handler.ValueProviderUtils.PLACEHOLDER_PREFIX;
import static com.rapidminer.connection.valueprovider.handler.ValueProviderUtils.PLACEHOLDER_SUFFIX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.PlaceholderParameter;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.connection.util.GenericHandlerRegistry;
import com.rapidminer.connection.util.GenericRegistrationEventListener;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * The registry for {@link ValueProviderHandler ValueProviderHandlers}. Handlers can be registered and unregistered,
 * searched by type and used to inject values for a given {@link ConnectionConfiguration}.
 * <p>
 * Listeners can be added to be notified for (un)registration events.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public final class ValueProviderHandlerRegistry extends GenericHandlerRegistry<ValueProviderHandler> {

	public static final String PARAMETER_ID = "ID";
	public static final String PARAMETER_NAME = "NAME";
	public static final String PARAMETER_TYPE = "TYPE";
	public static final String REMOTE = "REMOTE";

	/** Hide these Value Provider types from the user */
	private static final Collection<String> INVISIBLE_TYPES_REMOTE = Collections.singletonList(ChainingValueProviderHandler.TYPE);
	private static final Collection<String> INVISIBLE_TYPES = CollectionUtils.union(Collections.singletonList("remote_repository:rapidminer_vault"), INVISIBLE_TYPES_REMOTE);


	/**
	 * A simple helper class for place holder injection; can be used both for a constant or a placeholder.
	 *
	 * @author Jan Czogalla
	 */
	private static final class PlaceholderWrapper implements Function<Map<String, String>, String>, Supplier<String> {

		private static final PlaceholderWrapper EMPTY = new PlaceholderWrapper();

		private String key;
		private String constant;

		@Override
		public String apply(Map<String, String> parameters) {
			return Objects.toString(key == null ? constant : parameters.get(key), "");
		}

		@Override
		public String get() {
			return key;
		}

		/** Create a placeholder instance */
		private static PlaceholderWrapper withKey(String key) {
			PlaceholderWrapper phw = new PlaceholderWrapper();
			phw.key = key;
			return phw;
		}

		/** Create a constant instance */
		private static PlaceholderWrapper constant(String constant) {
			PlaceholderWrapper phw = new PlaceholderWrapper();
			phw.constant = constant;
			return phw;
		}

		/** Create an empty instance */
		private static PlaceholderWrapper empty() {
			return EMPTY;
		}
	}

	private static final ValueProviderHandlerRegistry INSTANCE = new ValueProviderHandlerRegistry();

	static {
		// Register default handlers
		INSTANCE.registerHandler(MacroValueProviderHandler.getInstance());
		INSTANCE.registerHandler(ChainingValueProviderHandler.getInstance());
	}

	/**
	 * Finds placeholders in the form of %{.*}; makes sure that nested placeholders are found first
	 * (e.g. %{foo%{bar}test} will find %{bar}, but not the surrounding placeholder expression.
	 * Nested placeholders are not allowed or at least not handled (i.e. no recursive resolution).
	 */
	private static final String PLACEHOLDER_REGEX = Pattern.quote(PLACEHOLDER_PREFIX) +
					"((?:[^" + Pattern.quote(PLACEHOLDER_INDICATOR) + "]|" +
					Pattern.quote(PLACEHOLDER_INDICATOR) + "(?!" + Pattern.quote(PLACEHOLDER_OPENING) + "))*?)" +
					Pattern.quote(PLACEHOLDER_SUFFIX);
	public static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(PLACEHOLDER_REGEX);

	/** Singleton class, no instantiation allowed except for internal purpose */
	private ValueProviderHandlerRegistry() {}

	/** Get the instance of this singleton */
	public static ValueProviderHandlerRegistry getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns a map of all actual parameter/value pairs. If only injected values are requested, all static parameters
	 * (i.e. those that are neither injected by a {@link ValueProvider} nor contain placeholders) will not be present in
	 * the returned map. Static parameters that will always be present are {@value #PARAMETER_ID}, {@value #PARAMETER_NAME}
	 * and {@value #PARAMETER_TYPE}, representing the respective fields of the {@link ConnectionConfiguration}.
	 * <p>
	 * This will find all {@link ConfigurationParameter ConfigurationParameters} and
	 * {@link PlaceholderParameter PlaceholderConfigurationParameters} that are marked as injected,
	 * as well as parameters that contain {@code %{[group.]name}} style placeholders and will try
	 * to find injections for these keys/placeholders.
	 * <p>
	 * Parameters that are marked as injected will be filled in using the {@link ValueProvider ValueProviders} of the
	 * {@link ConnectionConfiguration} first, after that placeholders will be injected using the static and already injected
	 * parameter values.
	 * <p>
	 * The order of value providers and placeholder injection are calculated using {@link ChainingValueProviderHandler#sortValueProviders(Map)}
	 * and the actual dependencies of the placeholders. If there are circular dependencies, nothing might be injected.
	 * <p>
	 * Placeholders may reference other parameter names inside their own group or fully qualified keys ({@code group.name})
	 * for parameters in other groups.
	 *
	 * @param connection
	 * 		the connection whose parameters should be injected
	 * @param operator
	 * 		the operator needed for context, may be {@code null}
	 * @param onlyInjected
	 * 		if only the injected values (value provider/placeholder) should be returned
	 */
	public Map<String, String> injectValues(ConnectionInformation connection, Operator operator, boolean onlyInjected) {
		if (connection == null) {
			// no configuration, no injection
			return null;
		}
		ConnectionConfiguration configuration = connection.getConfiguration();
		Map<String, ValueProvider> valueProviders = configuration.getValueProviderMap();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher("");
		Map<String, ConfigurationParameter> keys = configuration.getKeyMap();
		keys.putAll(configuration.getPlaceholderKeyMap());
		Map<String, String> staticParameters = new TreeMap<>();
		Map<String, List<PlaceholderWrapper>> keyPlaceholderMap = new LinkedHashMap<>();
		Map<ValueProvider, Map<String, String>> injectorKeyMap = new LinkedHashMap<>(valueProviders.size());
		ChainingValueProviderHandler.getInstance().sortValueProviders(valueProviders)
				.forEach(vp -> injectorKeyMap.put(vp, new LinkedHashMap<>()));
		// separate out static parameters, injected parameters and parameters with placeholders
		for (Entry<String, ConfigurationParameter> entry : keys.entrySet()) {
			String k = entry.getKey();
			ConfigurationParameter parameter = entry.getValue();

			//ignore disabled parameters
			if (!parameter.isEnabled()) {
				continue;
			}
			String parameterKey = parameter.getName();

			// find parameters that are injected
			if (parameter.isInjected()) {
				String injectorName = parameter.getInjectorName();
				ValueProvider vp = valueProviders.get(injectorName);
				if (vp == null) {
					continue;
				}
				injectorKeyMap.get(vp).put(k, parameterKey);
				continue;
			}

			String value = parameter.getValue();
			String keyPrefix = k.substring(0, k.length() - parameterKey.length());
			// find out if parameter value contains placeholders
			List<PlaceholderWrapper> phws = constructPlaceholders(keyPrefix, value, keys.keySet(), matcher);
			if (phws.isEmpty()) {
				staticParameters.put(k, value);
			} else {
				keyPlaceholderMap.put(k, phws);
			}
		}

		// static parameters (i.e. all parameters without injectables)
		// plus generic parameters as ID, name and connection type
		staticParameters.put(PARAMETER_ID, configuration.getId());
		staticParameters.put(PARAMETER_NAME, configuration.getName());
		staticParameters.put(PARAMETER_TYPE, configuration.getType());
		if (injectorKeyMap.isEmpty() && keyPlaceholderMap.isEmpty()) {
			// nothing to inject, only return static parameters
			return onlyInjected ? Collections.emptyMap() : staticParameters;
		}

		// check for circular dependencies and sort injectables by dependencies
		try {
			keyPlaceholderMap = ValidationUtil.dependencySortNoLoops(
					(key, phws) -> phws.stream().map(PlaceholderWrapper::get).filter(Objects::nonNull).collect(Collectors.toSet()),
					keyPlaceholderMap);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		// inject parameters that are marked as such
		Map<String, String> collectedParameters = new TreeMap<>(staticParameters);
		injectorKeyMap.forEach((vp, parameterMap) -> {
			String vpType = vp.getType();
			if (isTypeKnown(vpType)) {
				Map<String, String> injectedValues = getHandler(vpType).injectValues(vp, parameterMap, operator, connection);
				parameterMap.forEach((key, value) -> {
					if (injectedValues == null) {
						LogService.getRoot().log(Level.SEVERE, "com.rapidminer.connection.injection.bad_vp_null", vp.getName());
						logInjection(configuration, vpType, key, "bad_vp_null");
						return;
					}

					if (!injectedValues.containsKey(key)) {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.injection.value_not_injected",
								new Object[]{ConnectionI18N.getParameterName(configuration.getType(), key, value), vp.getName()});
						logInjection(configuration, vpType, key, "value_not_injected");
					} else {
						collectedParameters.putIfAbsent(key, injectedValues.get(key));
						logInjection(configuration, vpType, key, ActionStatisticsCollector.ARG_SUCCESS);
					}
				});
			} else {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.injection.missing_value_provider", vpType);
				logInjection(configuration, vpType, "", "missing_value_provider");
			}
		});

		// inject placeholders
		keyPlaceholderMap.forEach((key, phws) -> collectedParameters.putIfAbsent(key,
				phws.stream().map(phw -> phw.apply(collectedParameters)).collect(Collectors.joining())));

		if (onlyInjected) {
			staticParameters.keySet().forEach(collectedParameters::remove);
		}
		return collectedParameters;
	}

	/**
	 * Get a list of all the visible {@link ValueProvider} types which can be configured by the user
	 *
	 * @param filter
	 * 		to get visible {@link ValueProvider} types for remote connections use {@code ValueProviderHandlerRegistry#REMOTE} here
	 * @return a list of types of configurable ValueProviders
	 */
	public List<String> getVisibleTypes(String filter) {
		final List<String> allTypes = getAllTypes();
		if (REMOTE.equals(filter)) {
			allTypes.removeAll(INVISIBLE_TYPES_REMOTE);
		} else {
			allTypes.removeAll(INVISIBLE_TYPES);
		}
		return allTypes;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <G extends GenericRegistrationEventListener<ValueProviderHandler>, L extends G> Class<G> getListenerClass(L listener) {
		return (Class<G>) (listener == null || listener instanceof ValueProviderHandlerRegistryListener ?
				ValueProviderHandlerRegistryListener.class : listener.getClass());
	}

	@Override
	protected String getRegistryType() {
		return "value_provider";
	}

	/**
	 * Extract all placeholders from a parameter's value and resolve them to their fully qualified name. Returns a list
	 * of {@link PlaceholderWrapper PlaceholderWrappers} to easily transform them to their injected values.
	 * If nothing needs to be injected, an empty list is returned.
	 *
	 * @param keyPrefix
	 * 		the prefix of the full key, i.e. <em>group.</em>
	 * @param value
	 * 		the value to check for placeholders
	 * @param availableKeys
	 * 		set of available fully qualified keys
	 * @param matcher
	 * 		the matcher for placeholders
	 * @return the list of placeholder wrappers; might be empty
	 */
	private List<PlaceholderWrapper> constructPlaceholders(String keyPrefix, String value, Set<String> availableKeys, Matcher matcher) {
		if (value == null) {
			return Collections.emptyList();
		}
		matcher.reset(value);
		int pos = 0;
		List<PlaceholderWrapper> phws = new ArrayList<>();
		while (matcher.find(pos)) {
			int start = matcher.start();
			// constant part before the first match or after previous match
			if (start > pos) {
				phws.add(PlaceholderWrapper.constant(value.substring(pos, start)));
			}
			String placeholder = matcher.group(1);
			PlaceholderWrapper wrapper = PlaceholderWrapper.empty();
			if (!placeholder.isEmpty() && (availableKeys.contains(placeholder)
					|| availableKeys.contains(placeholder = keyPrefix + placeholder))) {
				wrapper = PlaceholderWrapper.withKey(placeholder);
			}
			phws.add(wrapper);
			pos = matcher.end();
		}
		// constant part at the end, after at least one placeholder was found
		if (pos > 0 && pos < value.length()) {
			phws.add(PlaceholderWrapper.constant(value.substring(pos)));
		}
		return phws;
	}

	/**
	 * Logs the result of the injection for a connection key.
	 */
	private void logInjection(ConnectionConfiguration configuration, String vpType, String key, String result) {
		ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_CONNECTION_INJECTION,
				configuration.getType() + ActionStatisticsCollector.ARG_SPACER + vpType,
						key + ActionStatisticsCollector.ARG_SPACER + result);
	}
}
