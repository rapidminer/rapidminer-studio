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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.settings.Telemetry;
import com.rapidminer.tools.update.internal.UpdateManager;


/**
 * Adds Telemetry dependent settings to the enforced values if needed
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0
 */
final class TelemetryDependentSettings {

	/**
	 * Utility class constructor.
	 */
	private TelemetryDependentSettings() {
		throw new AssertionError("Utility class");
	}

	/**
	 * List of settings that don't have any effect with disabled updates
	 */
	private static final List<String> UPDATES_SUB_KEYS = Arrays.asList(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_UPDATE_CHECK, RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_PURCHASED_NOT_INSTALLED_CHECK, UpdateManager.PARAMETER_UPDATE_INCREMENTALLY);

	/**
	 * List of settings that don't have any effect without the recommender
	 */
	private static final List<String> RECOMMENDER_SUB_KEYS = Arrays.asList("rapidminer.recommender.enable_recommendations_6_5", "rapidminer.recommender.confidence", "rapidminer.recommender.parameter.confidence", "rapidminer.recommender.parameter.threshold", "rapidminer.recommender.highlight.threshold", "rapidminer.recommender.initialized_6_5");

	/**
	 * Adds settings, that don't have any effect without telemetry, to the enforced settings
	 *
	 * @param enforcedSettings
	 * 		The enforced settings
	 */
	static final void addSettings(Map<String, String> enforcedSettings) {
		boolean allTelemetryDisabled = Boolean.parseBoolean(enforcedSettings.get(Telemetry.ALL_TELEMETRY.getKey()));
		boolean updatesDisabled = allTelemetryDisabled || Boolean.parseBoolean(enforcedSettings.get(Telemetry.MARKETPLACE.getKey()));
		boolean recommenderDisabled = allTelemetryDisabled || Boolean.parseBoolean(enforcedSettings.get(Telemetry.WISDOM_OF_CROWDS.getKey()));
		if (updatesDisabled) {
			UPDATES_SUB_KEYS.forEach(key -> enforcedSettings.putIfAbsent(key, null));
		}
		if (recommenderDisabled) {
			RECOMMENDER_SUB_KEYS.forEach(key -> enforcedSettings.putIfAbsent(key, null));
		}
	}
}
