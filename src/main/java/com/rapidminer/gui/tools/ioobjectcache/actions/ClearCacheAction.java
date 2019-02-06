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
package com.rapidminer.gui.tools.ioobjectcache.actions;

import java.awt.event.ActionEvent;
import java.util.Objects;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.IOObjectMap;


/**
 * Removes all element from a specified {@link IOObjectMap}.
 *
 * @author Michael Knopf
 */
public class ClearCacheAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private final IOObjectMap map;

	/**
	 * Creates a new {@link ClearCacheAction} for the specified {@link IOObjectMap}.
	 *
	 * @param map
	 *            The corresponding {@link IOObjectMap};
	 * @throws NullPointerException
	 *             If specified map is <code>null</code>.
	 */
	public ClearCacheAction(IOObjectMap map) {
		super(true, "ioobject_viewer.clear");
		Objects.requireNonNull(map);
		this.map = map;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		this.map.clearStorage();
	}
}
