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
package com.rapidminer.operator.ports;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * 
 * @author Simon Fischer
 * 
 */
public interface InputPorts extends Ports<InputPort> {

	/** Checks all preconditions at the ports. */
	public void checkPreconditions();

	/** Creates an input port with a simple precondition requiring input of type clazz. */
	public InputPort createPort(String name, Class<? extends IOObject> clazz);

	/** Creates an input port with a simple precondition requiring input with given meta data. */
	public InputPort createPort(String name, MetaData metaData);

}
