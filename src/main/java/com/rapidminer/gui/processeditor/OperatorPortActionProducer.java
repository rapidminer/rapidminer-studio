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
package com.rapidminer.gui.processeditor;

import com.rapidminer.adaption.belt.AtPortConverter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.Port;


/**
 * Adding a new entry to Operator Port context menus is as simple as implementing an anonymous instance of this interface
 * and adding it to the {@link OperatorPortActionRegistry}.
 *
 * @author Andreas Timm
 * @since 9.1
 */
public interface OperatorPortActionProducer {

	/**
	 * This is a first check to see if the implementation does accept these kind of {@link IOObject} as its input.
	 *
	 * @param ioobject
	 * 		from the {@link Port} that was accessed
	 * @return {@code true} if this producer wants to add actions for the {@link IOObject} type.
	 */
	public boolean accepts(Class<? extends IOObject> ioobject);


	/**
	 * This is a first check to see if the implementation does accept these kind of {@link IOObject}s or
	 * {@link IOObject}s that are automatically converted by ports as its input.
	 *
	 * @param ioobject
	 * 		from the {@link Port} that was accessed
	 * @return {@code true} if this producer wants to add actions for the {@link IOObject} type or those types that are
	 * 		automatically converted to the {@link IOObject} by ports.
	 */
	default boolean acceptsConvertible(Class<? extends IOObject> ioobject) {
		if (accepts(ioobject)) {
			return true;
		} else {
			return (AtPortConverter.isConvertible(ioobject, ExampleSet.class) && accepts(ExampleSet.class)) ||
					(AtPortConverter.isConvertible(ioobject, IOTable.class) && accepts(IOTable.class));
		}
	}

	/**
	 * Will only be called if the accepts method returned true. Here is the place to insert the creation of a {@link ResourceAction}
	 * that will be added to a context menu of the right-clicked {@link Port}
	 *
	 * @param port
	 * 		the port that was right-clicked
	 * @return a {@link ResourceAction} or null
	 */
	public ResourceAction createAction(Port port);
}
