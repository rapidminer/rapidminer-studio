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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.SimpleListBasedParameterHandler;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.I18N;


/**
 * Can be used to configure {@link Configurable}s. The {@link ConfigurationManager} will take care
 * of saving the configuration to configuration files or to a database and to provide access to
 * dialogs which can be used to edit these configurables.
 * <p>
 * The I18N key conventions can be found in the {@link Configurable} interface.
 * </p>
 *
 * @author Simon Fischer, Dominik Halfkann, Marco Boeck, Adrian Wilke, Nils Woehler
 * @since 6.2.0
 */
public abstract class AbstractConfigurator<T extends Configurable> {

	/** Maps names of {@link Configurable}s to ParameterHandlers */
	private final Map<String, ParameterHandler> parameterHandlers = new HashMap<>();

	/** Returns the {@link Configurable} implementation that this configurator can configure. */
	public abstract Class<T> getConfigurableClass();

	/**
	 * The ID used for identifying this Configurator. Must be a valid XML tag identifier and file
	 * name. Should include the plugin namespace. Example: "olap_connection".
	 */
	public abstract String getTypeId();

	/** The base key used in I18N property files. */
	public abstract String getI18NBaseKey();

	/**
	 * @return the parameter types used to configure {@link Configurable}s.
	 * @param parameterHandler
	 *            the {@link ParameterHandler} which should be used to register
	 *            {@link ParameterCondition}s.
	 */
	public abstract List<ParameterType> getParameterTypes(ParameterHandler parameterHandler);

	/**
	 * Creates a new {@link Configurable} based on parameters. The parameters passed to this method
	 * match the ones specified by {@link #getParameterTypes()}.
	 *
	 * @throws ConfigurationException
	 * @name a unique (user defined) name identifying this {@link Configurable}.
	 */
	public final T create(String name, Map<String, String> parameters) throws ConfigurationException {
		T instance;
		try {
			instance = getConfigurableClass().newInstance();
			instance.setName(name);
			instance.configure(parameters);
		} catch (InstantiationException e) {
			throw new ConfigurationException("Cannot instantiate " + getConfigurableClass(), e);
		} catch (IllegalAccessException e) {
			throw new ConfigurationException("Cannot access " + getConfigurableClass(), e);
		} catch (Throwable e) {
			throw new ConfigurationException("Cannot instantiate " + getConfigurableClass() + " (fatal error)", e);
		}
		return instance;
	}

	/** The display name used in UI components. Based on {@link #getI18NBaseKey()}. */
	public final String getName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.configurable." + getI18NBaseKey() + ".name");
	}

	/** A short help text to be used in dialogs. Based on {@link #getI18NBaseKey()}. */
	public final String getDescription() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.configurable." + getI18NBaseKey() + ".description");
	}

	/** A short help text to be used in dialogs. Based on {@link #getI18NBaseKey()}. */
	public final String getIconName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.configurable." + getI18NBaseKey() + ".icon");
	}

	/**
	 * Gets the {@link ParameterHandler} for the specified {@link Configurable}. If no related
	 * ParameterHandler exists, it is created.
	 *
	 * @param configurableName
	 *            The name of the configurable which is related to the requested ParameterHandler.
	 * @return The ParameterHandler related to the specified Configurable.
	 */
	public final ParameterHandler getParameterHandler(Configurable configurable) {
		if (configurable == null) {
			throw new IllegalArgumentException("No configurable specified.");
		}

		String configurableName = configurable.getName();
		if (!parameterHandlers.containsKey(configurableName)) {
			parameterHandlers.put(configurableName, new SimpleListBasedParameterHandler() {

				@Override
				public List<ParameterType> getParameterTypes() {
					return AbstractConfigurator.this.getParameterTypes(this);
				}

			});
		}
		return parameterHandlers.get(configurableName);
	}

	/**
	 * Removes {@link ParameterHandler} for specified Configurable.
	 *
	 * @param configurable
	 *            The configurable related to the ParameterHandler to remove.
	 */
	public final void removeCachedParameterHandler(Configurable configurable) {
		if (configurable == null) {
			throw new IllegalArgumentException("No configurable specified.");
		}
		parameterHandlers.remove(configurable.getName());
	}

	/**
	 * Updates the {@link Configurable} key for the related {@link ParameterHandler}.
	 *
	 * @param configurable
	 *            A configurable, whose name has changed.
	 * @param oldConfigurableName
	 *            The old name of the configurable.
	 */
	public final void reregisterCachedParameterHandler(Configurable configurable, String oldConfigurableName) {
		if (configurable == null) {
			throw new IllegalArgumentException("No configurable specified.");
		} else if (oldConfigurableName == null) {
			throw new IllegalArgumentException("No old configurable name specified.");
		} else if (!parameterHandlers.containsKey(oldConfigurableName)) {
			throw new IllegalArgumentException("Unknown old configurable name given.");
		}
		parameterHandlers.put(configurable.getName(), parameterHandlers.remove(oldConfigurableName));
	}


	@Override
	public String toString() {
		return getName();
	}

}
