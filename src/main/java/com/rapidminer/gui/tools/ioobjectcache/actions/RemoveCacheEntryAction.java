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
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectMap;


/**
 * Attempts to remove the specified element from the corresponding {@link IOObjectMap}.
 *
 * @author Michael Knopf
 */
public class RemoveCacheEntryAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private final IOObjectMap map;
	private final String key;

	/**
	 * Creates a new {@link RemoveCacheEntryAction} for the specified {@link IOObjectMap} and key.
	 *
	 * @param map
	 *            The corresponding {@link IOObjectMap};
	 * @param key
	 *            The key of the {@link IOObject} to be displayed.
	 * @throws NullPointerException
	 *             If one of the parameters is <code>null</code>.
	 */
	public RemoveCacheEntryAction(IOObjectMap map, String key) {
		super(true, "ioobject_viewer.remove");
		Objects.requireNonNull(map);
		Objects.requireNonNull(key);
		this.map = map;
		this.key = key;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		this.map.remove(this.key);
	}
}
