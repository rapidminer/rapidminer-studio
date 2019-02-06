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
import java.util.logging.Level;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.processeditor.results.ResultDisplay;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectMap;
import com.rapidminer.tools.LogService;


/**
 * This action opens an entry of the specified {@link IOObjectMap} the results perspective.
 *
 * @author Michael Knopf
 */
public class OpenCacheEntryAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private final IOObjectMap map;
	private final String key;

	/**
	 * Creates a new {@link OpenCacheEntryAction} for the specified {@link IOObjectMap} and key.
	 *
	 * @param map
	 *            The corresponding {@link IOObjectMap};
	 * @param key
	 *            The key of the {@link IOObject} to be displayed.
	 * @throws NullPointerException
	 *             If one of the parameters is <code>null</code>.
	 */
	public OpenCacheEntryAction(IOObjectMap map, String key) {
		super(true, "ioobject_viewer.open", key);
		Objects.requireNonNull(map);
		Objects.requireNonNull(key);
		this.map = map;
		this.key = key;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		ResultDisplay display = RapidMinerGUI.getMainFrame().getResultDisplay();
		IOObject object = map.get(key);
		if (object != null) {
			object.setSource(key);
			IOContainer objectContainer = new IOContainer(object);
			display.showData(objectContainer, key);
		} else {
			LogService.getRoot().log(Level.WARNING, "Could not open '" + key + "', entry does not exist.");
		}
	}

}
