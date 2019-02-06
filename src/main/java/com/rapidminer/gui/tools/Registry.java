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
package com.rapidminer.gui.tools;

import java.util.List;


/**
 * General Registry to register custom objects and listener support to allow notification about
 * registering/unregistering events.
 * 
 * @author Sabrina Kirstein, Marco Boeck
 * 
 */
public interface Registry<T> {

	/**
	 * Registers the object t and informs the listener that the object registered.
	 * 
	 * @param T
	 *            object to register
	 */
	public void register(T t);

	/**
	 * Unregisters the object t and informs the listener that the object unregistered. If the object
	 * was not registered in the first place, does nothing.
	 * 
	 * @param T
	 *            object to unregister
	 */
	public void unregister(T t);

	/**
	 * Registers a RegisterListener to inform listeners about new registered objects.
	 * 
	 * @param listener
	 */
	public void registerListener(RegistryListener<T> listener);

	/**
	 * Unregisters a RegisterListener. If the listener was not registered in the first place, does
	 * nothing.
	 * 
	 * @param listener
	 */
	public void unregisterListener(RegistryListener<T> listener);

	/**
	 * Return a list of all registered objects.
	 * 
	 * @return
	 */
	public List<T> getRegisteredObjects();
}
