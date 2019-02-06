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

import java.io.IOException;
import java.security.Key;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.rapidminer.parameter.ParameterType;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.cipher.KeyGeneratorTool;
import com.rapidminer.tools.config.actions.ConfigurableAction;


/**
 * Abstract standard implementation of the {@link Configurable} class.
 * <p>
 * Contains additional methods which are not part of the {@link Configurable} interface for
 * compatiblity reasons, e.g {@link #getActions()} and {@link #getTestAction()}.
 * </p>
 *
 * @author Simon Fischer, Dominik Halfkann, Marco Boeck
 *
 */
public abstract class AbstractConfigurable implements Configurable {

	private int id = -1;
	private String name = "name undefined";
	private Map<String, String> parameters = new HashMap<>();
	private RemoteRepository source;

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the parameter value for the given key. The value has gone through the
	 * {@link ParameterType#transformNewValue(String)} method.
	 *
	 * @param key
	 * @return
	 */
	@Override
	public String getParameter(String key) {
		// we only have a map of key - value Strings here, but we need to apply special handling,
		// e.g. for ParameterTypePassword

		// iterate over ParameterTypes in Configurator, find the one matching the key and return the
		// value returned from the transformNewValue method
		AbstractConfigurator<? extends Configurable> configurator = ConfigurationManager.getInstance()
				.getAbstractConfigurator(getTypeId());
		List<ParameterType> parameterTypes = configurator.getParameterTypes(configurator.getParameterHandler(this));
		for (ParameterType type : parameterTypes) {
			if (type.getKey().equals(key)) {
				return type.transformNewValue(parameters.get(key));
			}
		}

		return parameters.get(key);
	}

	/**
	 * Returns the xml representation of this parameter value.
	 *
	 * @param key
	 * @return
	 */
	public String getParameterAsXMLString(String key) {
		return parameters.get(key);
	}

	/**
	 * Returns the xml representation of this parameter value. If the {@link ParameterType} uses
	 * encryption, will decrypt the value with the given old key and encrypt it again with the
	 * specified new key.
	 *
	 * @param key
	 *            key of the parameter
	 * @param decryptKey
	 *            used to decrypt the parameter values
	 * @param encryptKey
	 *            used to encrypt the parameter values again
	 * @return
	 */
	public String getParameterAndChangeEncryption(String key, Key decryptKey, Key encryptKey) {
		String value = parameters.get(key);
		// we only have a map of key - value Strings here, but we need to apply special handling,
		// e.g. for ParameterTypePassword

		// iterate over ParameterTypes in Configurator, find the one matching the key and return the
		// decrypted and encrypted again value
		AbstractConfigurator<? extends Configurable> configurator = ConfigurationManager.getInstance()
				.getAbstractConfigurator(getTypeId());
		for (ParameterType type : configurator.getParameterTypes(configurator.getParameterHandler(this))) {
			if (type.getKey().equals(key)) {

				// store current key (will most likely be identical to the decrypt key)
				Key currentKey = null;
				try {
					currentKey = KeyGeneratorTool.getUserKey();
				} catch (IOException e) {
					// should not happen, if it does we simply cannot restore the original key
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.tools.config.AbstractConfigurable.cannot_backup_key");
				}

				// configurables are stored encrypted, so set the decryption key
				KeyGeneratorTool.setUserKey(decryptKey);
				// decrypt it with decryption key
				value = type.transformNewValue(parameters.get(key));
				// set encryption key
				KeyGeneratorTool.setUserKey(encryptKey);
				// encrypt it again with encryption key
				value = type.toString(value);

				// restore key which was used before this call
				if (currentKey != null) {
					KeyGeneratorTool.setUserKey(currentKey);
				}

				break;
			}
		}

		return value;
	}

	@Override
	public void setParameter(String key, String value) {
		parameters.put(key, value);
	}

	@Override
	public void configure(Map<String, String> parameters) {
		this.parameters.clear();
		this.parameters.putAll(parameters);
	}

	@Override
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setSource(RemoteRepository source) {
		this.source = source;
	}

	@Override
	public RemoteRepository getSource() {
		return source;
	}

	@Override
	public String getShortInfo() {
		return null;
	}

	@Override
	public boolean hasSameValues(Configurable comparedConfigurable) {
		if (!name.equals(comparedConfigurable.getName())) {
			return false;
		}

		if (this.parameters.size() != comparedConfigurable.getParameters().size()) {
			return false;
		}

		for (Map.Entry<String, String> parameterEntry : this.parameters.entrySet()) {
			if (!parameterEntry.getValue().toString()
					.equals(comparedConfigurable.getParameter(parameterEntry.getKey()).toString())) {
				// If the string comparison of the 2 objects with equals() returns false
				return false;
			}
		}
		return true;
	}

	/**
	 * @deprecated Use {@link AbstractConfigurable#isEmptyOrDefault(AbstractConfigurator)} instead.
	 */
	@Deprecated
	@Override
	public boolean isEmptyOrDefault(Configurator<? extends Configurable> configurator) {
		return isEmptyOrDefault((AbstractConfigurator<? extends Configurable>) configurator);
	}

	/**
	 * Checks if the Configurable is empty (has no values/only empty values/default values)
	 *
	 * @param configurator
	 *            The configurator to resolve the default values from
	 **/
	public boolean isEmptyOrDefault(AbstractConfigurator<? extends Configurable> configurator) {
		if (this.getName() != null && !this.getName().equals("")) {
			return false;
		} else if (this.getParameters() != null && this.getParameters().size() > 0) {
			for (String key : this.getParameters().keySet()) {
				// find default value
				String defaultValue = "";
				List<ParameterType> parameterTypes = configurator.getParameterTypes(configurator.getParameterHandler(this));
				for (ParameterType type : parameterTypes) {
					if (type.getKey().equals(key)) {
						defaultValue = type.getDefaultValueAsString();
					}
				}
				if (this.getParameters().get(key) != null && !this.getParameters().get(key).equals("")
						&& !this.getParameters().get(key).equals(defaultValue)) {
					return false;
				}
			}
			return true;
		}
		return true;
	}

	/**
	 * Returns a list of {@link ConfigurableAction}s. They can be used to perform various tasks
	 * associated with this configuration type, e.g. clearing a cache.
	 * <p>
	 * If no actions are required for this configuration type, returns <code>null</code>.
	 * </p>
	 * These actions can be defined per {@link Configurable} instance, so two {@link Configurable}s
	 * of the same {@link Configurator} type can have different actions. </p>
	 * <p>
	 * Also the actions can be changed dynamically, as they are retrieved each time they are
	 * required.
	 * </p>
	 *
	 * @return
	 */
	public Collection<ConfigurableAction> getActions() {
		return null;
	}

	/**
	 * Returns a {@link TestConfigurableAction} which tests the settings for the
	 * {@link Configurable}, e.g. a connection.
	 * <p>
	 * If no test action is required, returns <code>null</code>.
	 * </p>
	 * These actions can be defined per {@link Configurable} instance, so two {@link Configurable}s
	 * of the same {@link Configurator} type can have different actions. </p>
	 * <p>
	 * Also the actions can be changed dynamically, as they are retrieved each time they are
	 * required.
	 * </p>
	 *
	 * @return
	 */
	public TestConfigurableAction getTestAction() {
		return null;
	}
}
