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
package com.rapidminer.gui.properties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.properties.SettingsItem.Type;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.ParameterTypeSuggestion;
import com.rapidminer.parameter.SimpleListBasedParameterHandler;
import com.rapidminer.parameter.SuggestionProvider;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ProgressListener;


/**
 * Quick testing possibility for the updated {@link SettingsDialog}. Runs the dialog and shows two tabs.
 * There is a hidden parameter that should not be shown and a drop down parameter with 5 values in the first sub group
 * on the first tab. The first two values trigger two different parameters in the second sub group on the first tab.
 * Values 3 and 4 trigger parameters on the second tab.
 *
 * @author Jan Czogalla
 * @since 9.1
 */
public class SettingsDialogTestField {

	private static final String[] GROUPS = prefixedNumberedValues(2, "group_");
	private static final String[] SUBGROUPS = prefixedNumberedValues(4, "subgroup_");
	private static final String[] PARAMETERS = prefixedNumberedValues(7, "param_");

	private static final Map<String, String> KEY_TO_GROUP_KEY = new HashMap<>();
	private static final Map<String, Type> KEY_TO_TYPE = new HashMap<>();
	private static final Map<String, ParameterType> KEY_TO_PARAM_TYPE = new HashMap<>();
	private static ParameterHandler handler;

	public static void main(String[] args) {
		initRMProps();
		init();
		ParameterHandler handler = prepareHandler();
		SettingsItemProvider provider = prepareProvider(handler);
		SwingTools.invokeLater(() -> new SettingsDialog(handler, provider, null).setVisible(true));
	}

	private static void initRMProps() {
		ParameterService.init();
		ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_FONT_CONFIG, "Standard fonts");
	}

	private static void init() {
		for (String group : GROUPS) {
			KEY_TO_TYPE.put(group, Type.GROUP);
		}
		for (int i = 0; i < SUBGROUPS.length; i++) {
			String subgroup = SUBGROUPS[i];
			KEY_TO_TYPE.put(subgroup, Type.SUB_GROUP);
			KEY_TO_GROUP_KEY.put(subgroup, GROUPS[i/2]);
		}
		for (int i = 0; i < PARAMETERS.length; i++) {
			String parameter = PARAMETERS[i];
			KEY_TO_GROUP_KEY.put(parameter, SUBGROUPS[i/2]);
		}
		String[] conditionValues = prefixedNumberedValues(5, "value_");
		ParameterTypeStringCategory category = new ParameterTypeStringCategory(PARAMETERS[0], "", conditionValues, conditionValues[4]);
		category.setEditable(false);
		KEY_TO_PARAM_TYPE.put(PARAMETERS[0], category);
		ParameterTypeString hiddenParameter = new ParameterTypeString(PARAMETERS[1], "", "hidden parameter");
		hiddenParameter.setHidden(true);
		KEY_TO_PARAM_TYPE.put(PARAMETERS[1], hiddenParameter);
		for (int i = 0; i < conditionValues.length - 1; i++) {
			String conditionValue = conditionValues[i];
			ParameterType parameter = new ParameterTypeString(PARAMETERS[i + 2], "", "visible for " + conditionValue);
			parameter.registerDependencyCondition(new EqualStringCondition(null, PARAMETERS[0], false, conditionValue));
			KEY_TO_PARAM_TYPE.put(parameter.getKey(), parameter);
		}
		ParameterTypeSuggestion suggestions = new ParameterTypeSuggestion(PARAMETERS[6], "", new SuggestionProvider<String>() {
			@Override
			public List<String> getSuggestions(Operator op, ProgressListener pl) {
				return KEY_TO_PARAM_TYPE.values().stream().filter(pt -> !pt.isHidden()).map(ParameterType::getKey).collect(Collectors.toList());
			}

			@Override
			public ResourceAction getAction() {
				return null;
			}
		});
		KEY_TO_PARAM_TYPE.put(PARAMETERS[6], suggestions);
	}

	private static ParameterHandler prepareHandler() {
		ArrayList<ParameterType> parameterTypes = new ArrayList<>(KEY_TO_PARAM_TYPE.values());
		SimpleListBasedParameterHandler handler = new SimpleListBasedParameterHandler() {
			@Override
			public List<ParameterType> getParameterTypes() {
				return parameterTypes;
			}
		};
		parameterTypes.stream().flatMap(pt -> pt.getConditions().stream()).forEach(pc -> pc.setParameterHandler(handler));
		return handler;
	}

	private static SettingsItemProvider prepareProvider(ParameterHandler handler) {
		SettingsItemProvider provider = new AbstractSettingsItemProvider() {
			@Override
			protected URI getGroupDefinitions() throws URISyntaxException {
				// not used
				return null;
			}


			@Override
			public String getValue(String key) {
				return handler.getParameters().getParameterOrNull(key);
			}

			@Override
			public SettingsItem createAndAddItem(String key, Type type) {
				SettingsItem parent = null;

				// If type is parameter, choose or create the related group
				String groupKey = KEY_TO_GROUP_KEY.get(key);
				if (type != Type.GROUP) {
					if (!containsKey(groupKey)) {
						parent = createAndAddItem(groupKey, KEY_TO_TYPE.get(groupKey));
					} else {
						parent = get(groupKey);
					}
				}

				// Create new SettingsItem
				SettingsItem settingsItem = new SettingsItem(groupKey, key, parent, type);

				// Add to SettingsItems
				put(key, settingsItem);

				return settingsItem;
			}

			@Override
			public boolean isGroupingLoaded() {
				return false;
			}

			@Override
			public void applyValues(ParameterHandler parameterHandler) {
				// noop
			}

			@Override
			public void saveSettings() {
				// noop
			}
		};
		KEY_TO_GROUP_KEY.keySet().stream().filter(k -> !KEY_TO_TYPE.containsKey(k)).forEach(k -> provider.createAndAddItem(k, Type.PARAMETER));
		return provider;
	}

	private static String[] prefixedNumberedValues(int max, String prefix) {
		return IntStream.rangeClosed(1, max).mapToObj(i -> prefix + i).toArray(String[]::new);
	}

}