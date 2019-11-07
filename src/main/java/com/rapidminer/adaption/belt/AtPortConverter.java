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
package com.rapidminer.adaption.belt;

import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.studio.internal.Resources;


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
	 * @throws BeltConverter.ConversionException
	 * 		if the table cannot be converted because it contains custom columns
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

}