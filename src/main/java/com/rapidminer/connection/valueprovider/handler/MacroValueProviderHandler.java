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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;
import com.rapidminer.connection.valueprovider.ValueProviderParameterImpl;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.LogService;


/**
 * Extracts the required keys out of the macro context of the current process.
 * <p>
 * Use {@link #createNewProvider(String name, String prefix)} to allow for multiple connections of the same type in a single process.
 * The macro key is either just "key" or "prefix{@value PARAMETER_PREFIX_SEPARATOR}key"
 * </p>
 *
 * @since 9.3
 * @author Jonas Wilms-Pfau
 */
public final class MacroValueProviderHandler extends BaseValueProviderHandler {

	/**
	 * The singleton instance
	 */
	private static final MacroValueProviderHandler INSTANCE = new MacroValueProviderHandler();

	/**
	 * Configuration prefix parameter name, might be empty
	 */
	public static final String PARAMETER_PREFIX = "prefix";

	/**
	 * Separator char used between prefix and key
	 */
	public static final String PARAMETER_PREFIX_SEPARATOR = "_";

	/**
	 * Get the instance of this singleton
	 */
	public static MacroValueProviderHandler getInstance() {
		return INSTANCE;
	}

	/**
	 * Type of this value provider
	 */
	public static final String TYPE = "macro_value_provider";

	/**
	 * Creates a new MacroValueProviderHandler
	 */
	private MacroValueProviderHandler() {
		super(TYPE, Collections.singletonList(new ValueProviderParameterImpl(PARAMETER_PREFIX)));
	}

	@Override
	public Map<String, String> injectValues(ValueProvider vp, Map<String, String> injectables, Operator operator, ConnectionInformation connection) {
		if (!isValid(vp, operator) || injectables == null || injectables.isEmpty()) {
			return Collections.emptyMap();
		}

		String prefix = getPrefix(vp);
		Map<String, String> result = new LinkedHashMap<>();
		for (Entry<String, String> entry : injectables.entrySet()) {
			String fullKey = entry.getKey();
			String needed = entry.getValue();
			String value = null;
			String key = prefix + needed;
			try {
				value = operator.getProcess().getMacroHandler().getMacro(key, operator);
			} catch (Exception e) {
				// this can only happen with detached operators
				LogService.log(LogService.getRoot(), Level.WARNING, e, "com.rapidminer.connection.valueprovider.handler.MacroValueProviderHandler.retrieval_failed", key, vp.getName());
			}
			if (value != null) {
				result.put(fullKey, value);
			} else {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.valueprovider.handler.MacroValueProviderHandler.macro_not_found", key);
			}
		}

		return result;
	}

	/**
	 * Creates a new ValueProvider with the given name, this handler's type and a custom parameter prefix.
	 *
	 * @param name The name of the provider
	 * @param prefix The prefix used for the macro
	 * @return the new value provider
	 */
	public ValueProvider createNewProvider(String name, String prefix) {
		ValueProvider provider = createNewProvider(name);
		ValueProviderParameter prefixParam = provider.getParameterMap().get(PARAMETER_PREFIX);
		if (prefixParam != null) {
			prefixParam.setValue(prefix);
		}
		return provider;
	}

	/**
	 * Verifies the given value provider, creates a log entry if the operator context is missing
	 *
	 * @param vp
	 * 		the value provider to check
	 * @param context
	 * 		the operator that gives context to the value provider
	 * @return {@code true} if valid
	 */
	private static boolean isValid(ValueProvider vp, Operator context) {
		if (vp != null && context == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.valueprovider.handler.MacroValueProviderHandler.no_context", vp.getName());
		}
		return vp != null && TYPE.equals(vp.getType()) && context != null;
	}

	/**
	 * Returns the prefix for a {@link ValueProvider}
	 *
	 * @param provider
	 * 		the value provider
	 * @return the prefix, or an empty string
	 */
	static String getPrefix(ValueProvider provider) {
		ValueProviderParameter prefixParam = provider.getParameterMap().get(PARAMETER_PREFIX);
		if (prefixParam == null) {
			return "";
		}
		String prefix = StringUtils.trimToEmpty(prefixParam.getValue());
		return !prefix.isEmpty() ? prefix + PARAMETER_PREFIX_SEPARATOR : prefix;
	}
}
