/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.adaption.belt;

import com.rapidminer.RapidMiner;
import com.rapidminer.belt.BeltConverter;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.studio.internal.Resources;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.parameter.ParameterChangeListener;


/**
 * Utility methods to convert from belt {@link IOTable}s to {@link ExampleSet}s and vice versa at ports.
 *
 * Please note that this class is not part of any public API and might be modified or removed in future releases without
 * prior warning.
 *
 * @author Gisa Meier
 * @since 9.0.0
 */
public final class AtPortConverter {

	// Suppress default constructor for noninstantiability
	private AtPortConverter() {throw new AssertionError();}

	private static boolean betaMode = Boolean.parseBoolean(
			ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES));

	private static final ParameterChangeListener betaFeaturesListener = new ParameterChangeListener() {

		@Override
		public void informParameterChanged(String key, String value) {
			if (RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES.equals(key)) {
				setBetaMode(value);
			}
		}

		@Override
		public void informParameterSaved() {
			// do nothing
		}

	};

	static {
		ParameterService.registerParameterChangeListener(betaFeaturesListener);
	}

	/**
	 * Checks if is is possible to convert the dataClass into the desired class. Only conversion from an {@link
	 * ExampleSet} to a {@link IOTable} and vice versa is possible.
	 *
	 * @param dataClass
	 * 		the actual class
	 * @param desiredClass
	 * 		the desired class
	 * @return whether conversion is possible
	 */
	public static boolean isConvertible(Class<? extends IOObject> dataClass, Class<? extends IOObject> desiredClass) {
		return (ExampleSet.class.equals(desiredClass) && IOTable.class.equals(dataClass))
				|| (IOTable.class.equals(desiredClass) && ExampleSet.class.isAssignableFrom(dataClass));
	}

	/**
	 * Converts an {@link ExampleSet} into a {@link IOTable} or vice versa.
	 *
	 * @param data
	 * 		the data to convert
	 * @param port
	 * 		the port at which the conversion takes place
	 * @return the converted object
	 */
	public static IOObject convert(IOObject data, Port port) {
		ConcurrencyContext context = Resources.getConcurrencyContext(port.getPorts().getOwner().getOperator());
		if (data instanceof ExampleSet) {
			return BeltConverter.convert((ExampleSet) data, context);
		} else if (data instanceof IOTable) {
			return BeltConverter.convert((IOTable) data, context);
		} else {
			throw new UnsupportedOperationException("Conversion not supported");
		}
	}

	/**
	 * Converts an {@link IOObject} if it is a type that should not leave an operator.
	 *
	 * @param data
	 * 		the data to check
	 * @param port
	 * 		the port where the conversion takes place
	 * @return a converted object or the same object if no conversion is necessary.
	 */
	public static IOObject convertIfNecessary(IOObject data, Port port) {
		if (data instanceof IOTable && !betaMode) {
			ConcurrencyContext context = Resources.getConcurrencyContext(port.getPorts().getOwner().getOperator());
			return BeltConverter.convert((IOTable) data, context);
		} else {
			return data;
		}
	}

	/**
	 * Set the beta mode static field to the parsed value.
	 */
	private static void setBetaMode(String value) {
		betaMode = Boolean.parseBoolean(value);
	}

}