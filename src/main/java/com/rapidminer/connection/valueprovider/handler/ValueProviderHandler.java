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

import java.util.List;
import java.util.Map;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.util.GenericHandler;
import com.rapidminer.connection.util.TestExecutionContext;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;
import com.rapidminer.operator.Operator;


/**
 * An interface for handler/factory for {@link ValueProvider ValueProviders}. Implementations provide the possibility to
 * create a key/value map of injected values, as well as creating a new {@link ValueProvider}. They can be registered using
 * {@link ValueProviderHandlerRegistry#registerHandler ValueProviderHandlerRegistry.registerHandler(ValueProviderHandler)}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public interface ValueProviderHandler extends GenericHandler<ValueProvider> {

	/** Whether providers created by this handler have parameters */
	boolean isConfigurable();

	/**
	 * Returns a list of {@link ValueProviderParameter ValueProviderParameters} that can be found in each {@link ValueProvider}
	 * created by this handler. Implementations should return a deep copy of their default parameters here.
	 */
	List<ValueProviderParameter> getParameters();

	/**
	 * Creates a new {@link ValueProvider} with the given name, this handler's type and a copy of the default parameters.
	 *
	 * @param name
	 * 		the name of the new provider; must be neither {@code null} nor empty
	 * @see #getParameters()
	 */
	ValueProvider createNewProvider(String name);

	/**
	 * Returns a key/value map of values that can be injected, limited to the provided map of injectable keys. The
	 * {@code injectables} are mapping fully qualified keys to the parameter key. The resulting map should have the same
	 * sort order as the incoming map and should have the fully qualified parameter key as the map key.
	 *
	 * @param vp
	 * 		the value provider to use for injection; must not be {@code null}
	 * @param injectables
	 * 		the map of needed injectable values; must not be {@code null}
	 * @param operator
	 * 		the operator that gives context for the value provider; can be {@code null}
	 * @param connection
	 * 		the connection this value provider belongs to; should not be {@code null}
	 * @return the map with the same order of keys as the given injectables map, must never be {@code null}
	 */
	Map<String, String> injectValues(ValueProvider vp, Map<String, String> injectables, Operator operator, ConnectionInformation connection);

	/**
	 * Validates the given handled object. This can be used to check if all parameters have sensible values. This
	 * should only take a very small amount of time as opposed to {@link #test(TestExecutionContext)} which might run a longer
	 * operation. Should return a {@link com.rapidminer.connection.util.TestResult.ResultType#NONE ResultType.NONE}
	 * result if the object is {@code null}
	 * <p>
	 * This method is called instead of {@link #validate(Object)} from the UI.
	 *
	 * @param object
	 * 		the object to be validated
	 * @param information
	 * 		the information this value provider is used in
	 * @see ValidationResult
	 */
	ValidationResult validate(ValueProvider object, ConnectionInformation information);
}
