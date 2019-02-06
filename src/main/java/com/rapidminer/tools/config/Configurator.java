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

import java.util.List;

import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.SimpleListBasedParameterHandler;


/**
 * Can be used to configure {@link Configurable}s. The {@link ConfigurationManager} will take care
 * of saving the configuration to configuration files or to a database and to provide access to
 * dialogs which can be used to edit these configurables.
 * <p>
 * If parameter dependencies are required, you can use the {@link #getParameterHandler()} to do so.
 * </p>
 * <p>
 * The I18N key conventions can be found in the {@link Configurable} interface.
 * </p>
 *
 * @deprecated Please ALWAYS extend {@link AbstractConfigurator} instead of extending this class
 *             directly. Reason is that the {@link Configurator} class was not changed for
 *             compatibility reasons and the {@link AbstractConfigurator} contains vital methods for
 *             parameter dependencies handling.
 *
 * @author Simon Fischer, Dominik Halfkann, Marco Boeck, Adrian Wilke
 */
@Deprecated
public abstract class Configurator<T extends Configurable> extends AbstractConfigurator<T> {

	/**
	 * The default {@link ParameterHandler} implementation for {@link Configurator}s. This
	 * implementation uses the same {@link ParameterType}s for all {@link Configurable}s of the same
	 * {@link Configurator} type.
	 *
	 * This class can be used to convert and check parameters.
	 */
	private class DefaultParameterHandler extends SimpleListBasedParameterHandler {

		@Override
		public List<ParameterType> getParameterTypes() {
			return Configurator.this.getParameterTypes();
		}
	};

	/** This can be used for parameter dependencies */
	private final ParameterHandler defaultParamHandler = new DefaultParameterHandler();

	/**
	 * @return the {@link ParameterHandler} for this instance. Can be used for parameter
	 *         dependencies.
	 * @since 6.0.9
	 * @deprecated This method returns one {@link ParameterHandler} for all {@link Configurable}s.
	 *             Extend the class {@link AbstractConfigurator} instead of this class and implement
	 *             {@link #getParameterTypes(ParameterHandler)} for correct parameter dependency
	 *             handling.
	 */
	@Deprecated
	public final ParameterHandler getParameterHandler() {
		return defaultParamHandler;
	}

	/**
	 * Default implementation for all classes that extend {@link Configurator} instead of the new
	 * {@link AbstractConfigurator} class. It just returns the parameter types without correct
	 * dependency handling.
	 */
	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler parameterHandler) {
		return getParameterTypes();
	}

	/**
	 * In its default implementation this method returns an empty list of parameter types. Classes
	 * that still extend {@link Configurator} will override it with an actual list of parameter
	 * types and {@link Configurator#getParameterTypes(ParameterHandler parameterHandler)} will
	 * return the list of parameter types without correct dependency handling.
	 *
	 * @deprecated This method is not capable of handling parameter type dependencies correctly.
	 *             Extend the class {@link AbstractConfigurator} instead of {@link Configurator} and
	 *             implement {@link #getParameterTypes(ParameterHandler)}.
	 */
	@Deprecated
	public abstract List<ParameterType> getParameterTypes();

}
