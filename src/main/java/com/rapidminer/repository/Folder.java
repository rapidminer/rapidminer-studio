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

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.ProgressListener;

import java.util.List;


/**
 * An entry containing sub-entries.
 * 
 * @author Simon Fischer
 * 
 */
public interface Folder extends Entry {

	String TYPE_NAME = "folder";

	@Override
	default String getType() {
		return TYPE_NAME;
	}

	List<DataEntry> getDataEntries() throws RepositoryException;

	List<Folder> getSubfolders() throws RepositoryException;

	void refresh() throws RepositoryException;

	boolean containsEntry(String name) throws RepositoryException;

	Folder createFolder(String name) throws RepositoryException;

	IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator,
									  ProgressListener progressListener) throws RepositoryException;

	ProcessEntry createProcessEntry(String name, String processXML) throws RepositoryException;

	BlobEntry createBlobEntry(String name) throws RepositoryException;

	/**
	 * Returns true iff a child with the given name exists and a {@link #refresh()} would find this
	 * entry (or it is already loaded).
	 * 
	 * @throws RepositoryException
	 */
	boolean canRefreshChild(String childName) throws RepositoryException;
}
