/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.connection.gui.model;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.connection.util.ParameterUtility;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderImpl;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;


/**
 * Converts between {@link ValueProviderModel} and {@link ValueProviderImpl}
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public final class ValueProviderModelConverter {

	/**
	 * Prevent utility class instantiation.
	 */
	private ValueProviderModelConverter() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Converts a {@link ValueProviderImpl} into an {@link ValueProviderModel}
	 *
	 * @param valueProvider
	 * 		the {@link ValueProviderImpl}
	 * @return an observable {@link ValueProviderModel}
	 */
	static ValueProviderModel toModel(ValueProvider valueProvider) {
		return new ValueProviderModel(valueProvider);
	}

	/**
	 * Converts the given {@link ValueProviderModel} into a {@link ValueProviderImpl}
	 *
	 * @param model
	 * 		the observable model
	 * @return the {@link ValueProviderImpl}
	 */
	static ValueProviderImpl toValueProvider(ValueProvider model) {
		List<ValueProviderParameter> parameters = new ArrayList<>();
		for (ValueProviderParameter param : model.getParameters()) {
			parameters.add(ParameterUtility.getVPPBuilder(param.getName(), param.isEncrypted())
					.withValue(param.getValue()).enable(param.isEnabled()).build());
		}
		return new ValueProviderImpl(model.getName(), model.getType(), parameters);
	}

}
