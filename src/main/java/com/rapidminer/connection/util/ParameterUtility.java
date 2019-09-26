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
package com.rapidminer.connection.util;

import java.util.function.BiConsumer;

import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.configuration.ConfigurationParameterImpl;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.PlaceholderParameter;
import com.rapidminer.connection.configuration.PlaceholderParameterImpl;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;
import com.rapidminer.connection.valueprovider.ValueProviderParameterImpl;
import com.rapidminer.tools.ValidationUtil;


/**
 * A utility class to get builders for {@link ValueProviderParameter}, {@link ConfigurationParameter} and
 * {@link PlaceholderParameter}, as well as other parameter helper functions.
 *
 * @author Jan Czogalla
 * @see ParameterBuilder
 * @since 9.4.1
 */
public final class ParameterUtility {

	/**
	 * A builder for a {@link ValueProviderParameter} or sub-interface.
	 *
	 * @param <P>
	 * 		the type of {@link ValueProviderParameter} this builder handles
	 * @author Jan Czogalla
	 * @since 9.4.1
	 */
	public static class ParameterBuilder<P extends ValueProviderParameter> {

		P parameter;

		ParameterBuilder(P parameter) {
			this.parameter = parameter;
		}

		/**
		 * Set the value of the Parameter
		 *
		 * @see ValueProviderParameter#setValue(String)
		 */
		public ParameterBuilder<P> withValue(String value) {
			parameter.setValue(value);
			return this;
		}

		/**
		 * Enable the parameter
		 *
		 * @see ValueProviderParameter#setEnabled(boolean) ValueProviderParameter.setEnabled(true)
		 */
		public ParameterBuilder<P> enable() {
			parameter.setEnabled(true);
			return this;
		}

		/**
		 * Disable the parameter
		 *
		 * @see ValueProviderParameter#setEnabled(boolean) ValueProviderParameter.setEnabled(false)
		 */

		public ParameterBuilder<P> disable() {
			parameter.setEnabled(false);
			return this;
		}

		/**
		 * Get the {@link ValueProviderParameter}
		 */
		public P build() {
			return parameter;
		}

		/**
		 * Set the injector for this parameter if possible
		 *
		 * @see ConfigurationParameter#setInjectorName(String)
		 */
		public ParameterBuilder<P> withInjector(String injectorName) {
			// noop
			return this;
		}

	}
	/**
	 * A helper subclass that adds the functionality to set the injector for a parameter
	 *
	 * @param <P>
	 * 		the type of {@link ConfigurationParameter} this builder handles
	 * @author Jan Czogalla
	 * @since 9.4.1
	 */
	private static class InjectableParameterBuilder<P extends ConfigurationParameter> extends ParameterBuilder<P> {

		InjectableParameterBuilder(P parameter) {
			super(parameter);
		}

		@Override
		public ParameterBuilder<P> withInjector(String injectorName) {
			parameter.setInjectorName(injectorName);
			return this;
		}

	}
	private ParameterUtility() {
		// no instantiation of utility class
	}

	/**
	 * Checks that the given parameter is set (aka either injected or non-null and non-empty).
	 */
	public static void validateParameterValue(String fullKey, ConfigurationParameter parameter,
											  BiConsumer<String, String> errorCollector) {
		if (parameter == null || !ValidationUtil.isValueSet(parameter)) {
			errorCollector.accept(fullKey, ValidationResult.I18N_KEY_VALUE_MISSING);
		}
	}

	/**
	 * Finds the parameter specified by group and parameter key in the given {@link ConnectionConfiguration} and checks
	 * if it is set (aka either injected or non-null and non-empty). If the parameter does not exist or is not set,
	 * adds an error for the full key and of type {@value ValidationResult#I18N_KEY_VALUE_MISSING} through the error collector.
	 * Does nothing if either the configuration or error collector are {@code null}.
	 */
	public static void validateParameterValue(String groupKey, String parameterKey, ConnectionConfiguration config,
											  BiConsumer<String, String> errorCollector) {
		if (config == null || errorCollector == null){
			return;
		}
		String fullKey = groupKey + '.' + parameterKey;
		ConfigurationParameter parameter = config.getParameter(fullKey);
		validateParameterValue(fullKey, parameter, errorCollector);
	}

	/**
	 * Get a builder for an unencrypted {@link ValueProviderParameter}
	 *
	 * @param name
	 * 		the key of the new parameter
	 */
	public static ParameterBuilder<ValueProviderParameter> getVPPBuilder(String name) {
		return getVPPBuilder(name, false);
	}

	/**
	 * Get a builder for a {@link ValueProviderParameter}
	 *
	 * @param name
	 * 		the key of the new parameter
	 * @param encrypted
	 * 		whether or not the parameter should be encrypted
	 */
	public static ParameterBuilder<ValueProviderParameter> getVPPBuilder(String name, boolean encrypted) {
		return new ParameterBuilder<>(new ValueProviderParameterImpl(name, null, encrypted));
	}

	/**
	 * Get a builder for an unencrypted {@link ConfigurationParameter}
	 *
	 * @param name
	 * 		the key of the new parameter
	 */
	public static ParameterBuilder<ConfigurationParameter> getCPBuilder(String name) {
		return getCPBuilder(name, false);
	}

	/**
	 * Get a builder for a {@link ConfigurationParameter}
	 *
	 * @param name
	 * 		the key of the new parameter
	 * @param encrypted
	 * 		whether or not the parameter should be encrypted
	 */
	public static ParameterBuilder<ConfigurationParameter> getCPBuilder(String name, boolean encrypted) {
		return new InjectableParameterBuilder<>(new ConfigurationParameterImpl(name, null, encrypted));
	}

	/**
	 * Get a builder for an unencrypted {@link PlaceholderParameter}
	 *
	 * @param name
	 * 		the key of the new parameter
	 * @param group
	 * 		the group of the new placeholder
	 */
	public static ParameterBuilder<PlaceholderParameter> getPPBuilder(String name, String group) {
		return getPPBuilder(name, group, false);
	}

	/**
	 * Get a builder for a {@link PlaceholderParameter}
	 *
	 * @param name
	 * 		the key of the new parameter
	 * @param group
	 * 		the group of the new placeholder
	 * @param encrypted
	 * 		whether or not the parameter should be encrypted
	 */
	public static ParameterBuilder<PlaceholderParameter> getPPBuilder(String name, String group, boolean encrypted) {
		return new InjectableParameterBuilder<>(new PlaceholderParameterImpl(name, null, group, encrypted, null, true));
	}

}
