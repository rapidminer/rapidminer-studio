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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;
import com.rapidminer.connection.valueprovider.ValueProviderParameterImpl;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.tools.LogService;

/**
 * Helps with injecting values into other value providers. Works on a single parameter that is a list separated by
 * "{@value ParameterTypeEnumeration#SEPERATOR_CHAR}". This kind of {@link ValueProvider} should be created automatically
 * and not by the user. Value providers of this type will always return an {@link Collections#emptyMap() empty map},
 * since they only update the value providers they are concerned with. This implies that only a cloned {@link ConnectionConfiguration}
 * should be used when injecting values.
 * <p>
 * To represent a tree of injections, create multiple chains that overlap at the ends and insert them in the right order.
 * Chain VPs should preferably be added as the first VPs.
 *
 * @since 9.3
 * @author Jan Czogalla
 */
public class ChainingValueProviderHandler extends BaseValueProviderHandler {

	/** The type that this handler can process */
	public static final String TYPE = "chaining";
	/** The parameter key that indicates the list of chained provider names */
	public static final String PARAMETER_CHAINED_VPS = "value_providers";

	private static final ChainingValueProviderHandler INSTANCE = new ChainingValueProviderHandler();

	private ChainingValueProviderHandler() {
		super(TYPE, Collections.singletonList(new ValueProviderParameterImpl(PARAMETER_CHAINED_VPS)));
	}

	public static ChainingValueProviderHandler getInstance() {
		return INSTANCE;
	}

	/**
	 * Prepares the referenced value providers if they can be found in the given {@link ConnectionConfiguration}.
	 * Will always return an empty map and ignore the injectables parameter.
	 */
	@Override
	public Map<String, String> injectValues(ValueProvider vp, Map<String, String> injectables, Operator operator, ConnectionInformation connection) {
		if (!isValid(vp)) {
			return Collections.emptyMap();
		}
		ConnectionConfiguration configuration = connection.getConfiguration();
		Set<ValueProvider> chainedList = findChainedProviders(vp, configuration.getValueProviderMap());
		ValueProvider prev = null;
		for (ValueProvider next : chainedList) {
			if (prev != null) {
				Map<String, String> vpInjectables = next.getParameters().stream()
						.filter(p -> p.getValue() == null && p.isEnabled())
						.map(ValueProviderParameter::getName)
						.collect(Collectors.toMap(n -> n, n -> n, (a, b) -> b, LinkedHashMap::new));
				String type = prev.getType();
				if (!ValueProviderHandlerRegistry.getInstance().isTypeKnown(type)) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.injection.missing_value_provider", type);
					return Collections.emptyMap();
				}
				ValueProviderHandler handler = ValueProviderHandlerRegistry.getInstance().getHandler(type);
				Map<String, String> injected = handler.injectValues(prev, vpInjectables, operator, connection);
				next.getParameters().stream().filter(p -> injected.containsKey(p.getName()))
						.forEach(p -> p.setValue(injected.get(p.getName())));
			}
			prev = next;
		}
		return Collections.emptyMap();
	}

	/** Creates a new provider from the given names of value providers. */
	public ValueProvider createNewProvider(String name, List<String> vpNames) {
		vpNames = ValidationUtil.requireNonEmptyList(vpNames, "value provider names");
		ValueProvider provider = createNewProvider(name);
		String value = ParameterTypeEnumeration.transformEnumeration2String(vpNames);
		ValueProviderParameter parameter = provider.getParameterMap().get(PARAMETER_CHAINED_VPS);
		if (parameter != null) {
			parameter.setValue(value);
		}
		return provider;
	}

	/**
	 * Get the list of chained {@link ValueProvider ValueProviders} in the appropriate order. Will return an empty set if
	 * <ol>
	 * <li>the {@link ValueProvider} to check is not of type {@value #TYPE}</li>
	 * <li>none or only one value providers listed or</li>
	 * <li>nonexistent value providers listed or</li>
	 * <li>duplicates listed</li>
	 * </ol>
	 *
	 * @param chaining
	 * 		the provider to be presumed of type {@value #TYPE}
	 * @param allProviders
	 * 		a map of all available providers
	 * @return the ordered set of providers or an empty set; never {@code null}
	 */
	public Set<ValueProvider> findChainedProviders(ValueProvider chaining, Map<String, ValueProvider> allProviders) {
		if (!isValid(chaining)) {
			return new LinkedHashSet<>();
		}
		List<String> names = findChainedNames(chaining);
		if (names.size() <= 1) {
			// no chain, no gain
			return new LinkedHashSet<>();
		}
		Set<ValueProvider> chainedProviders = names.stream().map(allProviders::get)
				.filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
		if (chainedProviders.size() < names.size()) {
			chainedProviders.clear();
		}
		return chainedProviders;
	}

	/**
	 * Returns a sorted list of value providers, using the given name/value provider map. First this finds all chaining
	 * value providers. If there are none, simply the list of all other value providers is returned. Else the chaining
	 * value providers will be sorted according to the dependencies of their chained providers.
	 * <p>
	 * The returned list has the chaining value providers in the front, followed by the other providers. The latter will
	 * keep the order in which they are presented to this method.
	 * <p>
	 * The chaining providers will be sorted as specified above, if they don't contain loops and have only single dependencies.
	 * An invalid combination of chains would be <em>a -> c, b -> c</em> for example.
	 *
	 * @param providerMap
	 * 		the map of names to providers
	 * @return a sorted list of value providers
	 */
	public List<ValueProvider> sortValueProviders(Map<String, ValueProvider> providerMap) {
		Map<Boolean, List<ValueProvider>> chainsAndNormal = providerMap.values().stream()
				.collect(Collectors.partitioningBy(ChainingValueProviderHandler::isValid));
		Map<String, Set<String>> dependencies = new HashMap<>();
		List<ValueProvider> chains = chainsAndNormal.get(true);
		if (chains.isEmpty()) {
			return chainsAndNormal.get(false);
		}
		// find the final VP name for each chain and collect all preceding names as dependencies
		Map<String, String> lastSegmentToChain = chains.stream().collect(Collectors.toMap(vp -> {
			List<String> names = findChainedNames(vp);
			String lastSegment = names.remove(names.size() - 1);
			dependencies.put(lastSegment, new HashSet<>(names));
			return lastSegment;
		}, ValueProvider::getName, (a, b) -> a));
		List<String> sortedChains = Collections.emptyList();
		// make sure there is no duplicate chain end!
		if (lastSegmentToChain.size() == chains.size()) {
			sortedChains = ValidationUtil.dependencySortEmptyListForLoops(dependencies::get, dependencies.keySet());
		}
		if (sortedChains.isEmpty()) {
			chains.addAll(chainsAndNormal.get(false));
			return chains;
		}
		// transfer sorting to actual chained value providers
		List<ValueProvider> sortedProviders = sortedChains.stream()
				// get corresponding chain provider name for each "normal" VP; ignore those that don't represent a chain
				.map(lastSegmentToChain::get).filter(Objects::nonNull)
				// get actual chain provider
				.map(providerMap::get).collect(Collectors.toList());
		sortedProviders.addAll(chainsAndNormal.get(false));
		return sortedProviders;
	}

	private List<String> findChainedNames(ValueProvider chaining) {
		return ParameterTypeEnumeration.transformString2List(chaining.getParameterMap().get(PARAMETER_CHAINED_VPS).getValue());
	}

	/** Checks whether the value provider is valid. Checks for {@code null}, type and parameter existence. */
	private static boolean isValid(ValueProvider vp) {
		return vp != null && TYPE.equals(vp.getType()) && vp.getParameterMap().get(PARAMETER_CHAINED_VPS) != null;
	}
}
