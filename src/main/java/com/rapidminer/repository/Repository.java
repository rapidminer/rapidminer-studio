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
package com.rapidminer.repository;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.repository.gui.RepositoryConfigurationPanel;


/**
 * @author Simon Fischer
 */
public interface Repository extends Folder {

	void addRepositoryListener(RepositoryListener l);

	void removeRepositoryListener(RepositoryListener l);

	/**
	 * This will return the entry if existing or null if it can't be found.
	 */
	Entry locate(String string) throws RepositoryException;

	/** Returns some user readable information about the state of this repository. */
	String getState();

	/** Returns the icon name for the repository. */
	String getIconName();

	/** Returns a piece of XML to store the repository in a configuration file. */
	Element createXML(Document doc);

	boolean shouldSave();

	/**
	 * Called after the repository is added.
	 */
	void postInstall();

	/**
	 * Called directly before the repository is removed.
	 */
	void preRemove();

	/** Returns true if the repository is configurable. In that case, */
	boolean isConfigurable();

	/** Creates a configuration panel. */
	RepositoryConfigurationPanel makeConfigurationPanel();
}
