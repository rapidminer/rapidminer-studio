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
package com.rapidminer.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.logging.Level;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.properties.ProxyParameterSaver;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;


/**
 * This class applies the proxy settings on the JVM environment
 *
 * @author Jonas Wilms-Pfau
 * @since 7.3.0
 *
 */
public class ProxySettings {

	/**
	 * Non Proxy Settings
	 * <p>
	 * Set some random proxy but ignore every URL -> Proxy is bypassed
	 * </p>
	 */
	// Could be every non empty String
	private final static String NON_PROXY_HOST = "127.0.0.1";
	// Could be every valid Port
	private final static String NON_PROXY_PORT = "80";
	// Must be * to ignore all proxy settings
	private final static String NON_PROXY_RULE = "*";
	/** Default value for Linux and Windows */
	private static final String DEFAULT = "";

	public static final String PROXY_PREFIX = "rapidminer.proxy.";
	public static final String SYSTEM_PREFIX = "";

	private static final String HTTP_NON_PROXY_RULE = "http.nonProxyHosts";
	private static final String FTP_NON_PROXY_RULE = "ftp.nonProxyHosts";
	private static final String SOCKS_NON_PROXY_RULE = "socksNonProxyHosts";

	/* RapidMiner proxy settings */
	private final static String PROXY_HOSTS[] = { RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_HOST,
			RapidMiner.PROPERTY_RAPIDMINER_HTTPS_PROXY_HOST, RapidMiner.PROPERTY_RAPIDMINER_FTP_PROXY_HOST,
			RapidMiner.PROPERTY_RAPIDMINER_SOCKS_PROXY_HOST };
	private final static String PROXY_PORTS[] = { RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_PORT,
			RapidMiner.PROPERTY_RAPIDMINER_HTTPS_PROXY_PORT, RapidMiner.PROPERTY_RAPIDMINER_FTP_PROXY_PORT,
			RapidMiner.PROPERTY_RAPIDMINER_SOCKS_PROXY_PORT };
	/* Real system settings */
	private final static String PROXY_RULES[] = { HTTP_NON_PROXY_RULE, FTP_NON_PROXY_RULE, SOCKS_NON_PROXY_RULE };

	/**
	 * To support OSX we have to store the System settings before the RapidMiner cfg file is loaded
	 */
	public static void storeSystemSettings() {
		if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
			Arrays.asList(toNative(PROXY_HOSTS)).stream().forEach(SystemSettings::store);
			Arrays.asList(toNative(PROXY_PORTS)).stream().forEach(SystemSettings::store);
			Arrays.asList(PROXY_RULES).stream().forEach(SystemSettings::store);
		} else {
			Arrays.asList(toNative(PROXY_HOSTS)).stream().forEach(SystemSettings::storeDefault);
			Arrays.asList(toNative(PROXY_PORTS)).stream().forEach(SystemSettings::storeDefault);
			Arrays.asList(PROXY_RULES).stream().forEach(SystemSettings::storeDefault);
		}

	}

	/**
	 * This Method
	 * <ul>
	 * <li>Migrates the old proxy settings if needed</li>
	 * <li>Applies the current proxy Settings</li>
	 * <li>Registers a ChangeListener to change the proxy settings on save</li>
	 * </ul>
	 *
	 */
	public static void init() {
		ProxySettings.storeSystemSettings();
		ProxyIntegrator.updateOldInstallation();
		ParameterService.registerParameterChangeListener(new ProxyParameterSaver());
	}

	/**
	 * Applies the RapidMiner proxy settings on the corresponding JVM System properties
	 */
	public static void apply() {

		switch (ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_PROXY_MODE)) {
			case RapidMiner.RAPIDMINER_PROXY_MODE_SYSTEM:
				// System Proxy
				SystemSettings.apply();
				break;
			case RapidMiner.RAPIDMINER_PROXY_MODE_DIRECT:
				// No Proxy
				setSystemValue(NON_PROXY_HOST, toNative(PROXY_HOSTS));
				setSystemValue(NON_PROXY_PORT, toNative(PROXY_PORTS));
				setSystemValue(NON_PROXY_RULE, PROXY_RULES);
				break;
			case RapidMiner.RAPIDMINER_PROXY_MODE_MANUAL:
				// User Settings
				copyParameterToSystem(PROXY_HOSTS, toNative(PROXY_HOSTS));
				copyParameterToSystem(PROXY_PORTS, toNative(PROXY_PORTS));
				String exclusionRule = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_PROXY_EXCLUDE);
				setSystemValue(exclusionRule, PROXY_RULES);
				// Apply Socks Version
				int socksVersionOffset = Arrays.asList(RapidMiner.RAPIDMINER_SOCKS_VERSIONS)
						.indexOf(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SOCKS_VERSION));
				int initialSocksVersion = 4;
				ParameterService.setParameterValue(toNative(RapidMiner.PROPERTY_RAPIDMINER_SOCKS_VERSION),
						String.valueOf(initialSocksVersion + socksVersionOffset));
				break;
		}
		GlobalAuthenticator.refreshProxyAuthenticators();
	}

	/**
	 * Set one value to all System Keys
	 *
	 * @param value
	 * @param systemKey
	 */
	private static void setSystemValue(String value, String[] systemKeys) {
		for (String parameterKey : systemKeys) {
			setSystemProperty(parameterKey, value);
		}
	}

	private static void setSystemProperty(String key, String value) {
		if (value != null && key != null) {
			System.setProperty(key, value);
		}

	}

	/**
	 * Copies the ParameterService values from the source keys to target System property keys
	 * <p>
	 * Warning: both arrays must have the same length.
	 * </p>
	 *
	 * @param sourceKeys
	 *            ParameterService keys
	 * @param targetKeys
	 *            System keys
	 */
	private static void copyParameterToSystem(String[] sourceKeys, String[] targetKeys) {
		for (int i = 0; i < sourceKeys.length; i++) {
			String sourceValue = ParameterService.getParameterValue(sourceKeys[i]);
			setSystemProperty(targetKeys[i], sourceValue);
		}
	}

	/**
	 * Converts the given keys to native System keys
	 *
	 * @param keys
	 * @return
	 */
	private static String[] toNative(String keys[]) {
		return Arrays.asList(keys).stream().map(ProxySettings::toNative).toArray(String[]::new);
	}

	/**
	 * Converts the given key to a native System key
	 *
	 * @param key
	 * @return
	 */
	private static String toNative(String key) {
		return key.replace(PROXY_PREFIX, SYSTEM_PREFIX);
	}

	/**
	 * This class migrates the old proxy settings into the new structure.
	 * <p>
	 * Use ProxyService.init() to check for updates
	 * </p>
	 *
	 * @author Jonas Wilms-Pfau
	 *
	 */
	private static class ProxyIntegrator {

		private static final String OLD_KEY = "http.proxyUsername";
		private static final String NEW_KEY = RapidMiner.PROPERTY_RAPIDMINER_SOCKS_VERSION;

		/**
		 * Update an old installation
		 * <p>
		 * Copies the old native properties into the new RapidMiner properties
		 * </p>
		 *
		 */
		private static void updateOldInstallation() {
			if (ParameterService.getParameterValue(OLD_KEY) != null && ParameterService.getParameterValue(NEW_KEY) == null) {
				LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.ProxyService.migrate");
				// Copy from old System properties to new RapidMiner properties
				copyParameterValues(toNative(PROXY_HOSTS), PROXY_HOSTS);
				copyParameterValues(toNative(PROXY_PORTS), PROXY_PORTS);

				// merge exclusionRules together
				HashSet<String> rules = new LinkedHashSet<>();
				for (String ruleKey : PROXY_RULES) {
					String rule = System.getProperty(ruleKey);
					if (rule != null && !"".equals(rule)) {
						rules.addAll(Arrays.asList(rule.split("\\|")));
					}
				}
				String exclusionRule = String.join("|", rules);
				setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_PROXY_EXCLUDE, exclusionRule);
				setSystemValue(exclusionRule, PROXY_RULES);
			}
		}

		/**
		 * Copies the parameter values from source to target
		 * <p>
		 * Warning: both arrays must have the same length.
		 * </p>
		 *
		 * @param sourceKey
		 *            ParameterService keys
		 * @param targetKey
		 *            ParameterService keys
		 */
		private static void copyParameterValues(String[] sourceKeys, String[] targetKeys) {
			for (int i = 0; i < sourceKeys.length; i++) {
				String sourceValue = ParameterService.getParameterValue(sourceKeys[i]);
				setParameterValue(targetKeys[i], sourceValue);
			}
		}

		/**
		 * Set a value to the given ParameterService key
		 *
		 * @param key
		 * @param value
		 */
		private static void setParameterValue(String key, String value) {
			if (value != null && value != "") {
				ParameterService.setParameterValue(key, value);
			}
		}

	}

	/**
	 * Helper Class to support Mac OS X
	 */
	private static class SystemSettings {

		private static HashMap<String, String> settings = new HashMap<String, String>();

		/**
		 * Stores the current System property or the default value for the given key
		 *
		 * @param key
		 */
		private static void store(String key) {
			settings.putIfAbsent(key, System.getProperty(key, DEFAULT));
		}

		/**
		 * Stores the default value for the given key
		 *
		 * @param key
		 */
		private static void storeDefault(String key) {
			settings.putIfAbsent(key, DEFAULT);
		}

		/**
		 * Applies all stored settings to the system
		 */
		private static void apply() {
			settings.forEach(System::setProperty);
		}

	}

}
