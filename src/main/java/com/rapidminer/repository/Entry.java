/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.util.Collection;

import javax.swing.Action;

import com.rapidminer.operator.io.RepositorySource;


/**
 * An entry in a repository. Can be either a folder or a data entry (leaf). This is either a view on
 * the local file system or a view on a remote repository.
 *
 * @author Simon Fischer, Nils Woehler
 *
 */
public interface Entry {

	/** Returns the name, the last part of the location. */
	public String getName();

	/** Returns a string describing the type: "folder", "data", "blob", or "process". */
	public String getType();

	/**
	 * Returns the user name of the owner. Returns <code>null</code> in case no owner has been
	 * specified.
	 */
	public String getOwner();

	/** Returns a human readable description. */
	public String getDescription();

	/** Returns true if this entry cannot be written to. */
	public boolean isReadOnly();

	/**
	 * Changes the name of the entry. The entry stays in the same folder.
	 *
	 * @throws RepositoryException
	 */
	public boolean rename(String newName) throws RepositoryException;

	/**
	 * Needs to be implemented only for folders in the same repository. Moving between different
	 * repositories is implemented by
	 * {@link RepositoryManager#move(RepositoryLocation, Folder, com.rapidminer.tools.ProgressListener)}
	 * using a sequence of copy and delete.
	 *
	 * @throws RepositoryException
	 */
	public boolean move(Folder newParent) throws RepositoryException;

	/**
	 * Needs to be implemented only for folders in the same repository. Moving between different
	 * repositories is implemented by
	 * {@link RepositoryManager#move(RepositoryLocation, Folder, com.rapidminer.tools.ProgressListener)}
	 * using a sequence of copy and delete.
	 *
	 * @param newName
	 *            New name for moved entry. If moved entry shouldn't be renamed: newName=null.
	 * @throws RepositoryException
	 */
	public boolean move(Folder newParent, String newName) throws RepositoryException;

	/** Returns the folder containing this entry. */
	public Folder getContainingFolder();

	/**
	 * Subclasses can use this method to signal whether getting information from this entry will
	 * block the current thread, e.g. because information must be fetched over the network.
	 */
	public boolean willBlock();

	/**
	 * A location, that can be used, e.g. as a parameter in the {@link RepositorySource} or which
	 * can be used to locate the entry using {@link RepositoryManager#resolve(String)}.
	 */
	public RepositoryLocation getLocation();

	/**
	 * Deletes the entry and its contents from the repository.
	 *
	 * @throws RepositoryException
	 */
	public void delete() throws RepositoryException;

	/** Returns custom actions to be displayed in this entry's popup menu. */
	public Collection<Action> getCustomActions();
}
