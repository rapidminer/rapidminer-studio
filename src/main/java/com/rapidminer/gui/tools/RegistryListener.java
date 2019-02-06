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

/**
 * 
 * Listener which listens for {@link RegistryEvent}s and {@link UnregisterEvent}s contains the new
 * registered or unregistered object, which caused the event
 * 
 * @author Sabrina Kirstein
 * 
 */
public interface RegistryListener<T> {

	/**
	 * this method is called when a new {@link RegisterEvent<T>} happened
	 */
	public void eventTriggered(RegistryEvent<T> event);

}
