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
package com.rapidminer.repository.internal.remote;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;

import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.internal.remote.model.AccessRights;
import com.rapidminer.repository.internal.remote.model.EntryResponse;
import com.rapidminer.repository.internal.remote.model.FolderContentsResponse;
import com.rapidminer.repository.internal.remote.model.ProcessContentsResponse;
import com.rapidminer.repository.internal.remote.model.Response;
import com.rapidminer.tools.PasswordInputCanceledException;


/**
 * Allows to manage content of {@link RemoteRepository}s like moving, renaming or deletion of
 * repository entries.
 *
 * @author Nils Woehler
 * @since 6.5.0
 */
public interface RemoteContentManager {

	/**
	 * Retrieves remote entry information from the server.
	 *
	 * @param path
	 * 		the path to lookup the entry
	 * @return an {@link EntryResponse} which contains information about the entry
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	EntryResponse getEntry(String path) throws PasswordInputCanceledException, RepositoryException;

	/**
	 * Deletes a remote entry specified by the provided path
	 *
	 * @param path
	 * 		the path of the entry to be deleted
	 * @return a response which indicates whether the deletion was successful
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	Response deleteEntry(String path) throws PasswordInputCanceledException, RepositoryException;

	/**
	 * Renames a remote repository entry
	 *
	 * @param path
	 * 		the current path of the entry
	 * @param newName
	 * 		the new entry name
	 * @return a response which indicates whether the renaming was successful
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	EntryResponse rename(String path, String newName) throws PasswordInputCanceledException, RepositoryException;

	/**
	 * Moves an entry to a new path
	 *
	 * @param oldPath
	 * 		the current (old) path of the entry
	 * @param newPath
	 * 		the new path of the entry
	 * @return a response which indicates whether the moving was successful
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	EntryResponse move(String oldPath, String newPath) throws PasswordInputCanceledException, RepositoryException;

	/**
	 * Creates a new folder at the specified path
	 *
	 * @param path
	 * 		the path of the parent folder
	 * @param name
	 * 		the name of the new folder
	 * @return a response which indicates whether the moving was successful
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	EntryResponse makeFolder(String path, String name) throws PasswordInputCanceledException, RepositoryException;

	/**
	 * Retrieves the contents of a folder
	 *
	 * @param path
	 * 		the path of the folder
	 * @return a response which contains information about the folder contents
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	FolderContentsResponse getFolderContents(String path) throws PasswordInputCanceledException, RepositoryException;

	/**
	 * Creates a new (empty) blob entry at the specified path for the specified name
	 *
	 * @param path
	 * 		the path of the new blob entry
	 * @param name
	 * 		the name of the blob entry
	 * @return a response which indicates whether the creation was successful
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	EntryResponse createBlob(String path, String name) throws PasswordInputCanceledException, RepositoryException;

	/**
	 * Stores a process XML at the specified path.
	 *
	 * @param path
	 * 		the path of the process
	 * @param processXML
	 * 		the process XML
	 * @param lastTimestamp
	 * 		the change timestamp
	 * @return a response which indicates whether the storing was successful
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	Response storeProcess(String path, String processXML, XMLGregorianCalendar lastTimestamp)
			throws PasswordInputCanceledException, RepositoryException;

	/**
	 * Queries the server for process contents.
	 *
	 * @param path
	 * 		the path to the process
	 * @param revision
	 * 		the revision of the process to ask for
	 * @return a response which contains information about the process content
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	ProcessContentsResponse getProcessContents(String path, int revision) throws PasswordInputCanceledException,
			RepositoryException;

	/**
	 * Starts a new process revision
	 *
	 * @param path
	 * 		the path to the process
	 * @return a response which indiciates whether starting a new revision was successful
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	Response startNewRevision(String path) throws PasswordInputCanceledException, RepositoryException;

	/**
	 * Query the server for all group names
	 *
	 * @return a list that contains all currently available group names
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	List<String> getAllGroupNames() throws PasswordInputCanceledException, RepositoryException;

	/**
	 * Modifies the access rights for a server entry.
	 *
	 * @param path
	 * 		the path to the entry
	 * @param accessRights
	 * 		the new access rights
	 * @return a response which indicates whether the change was successful
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	Response setAccessRights(String path, List<AccessRights> accessRights) throws PasswordInputCanceledException,
			RepositoryException;

	/**
	 * Queries the server for current access rights for a remote entry.
	 *
	 * @param path
	 * 		the path of the entry
	 * @return the list of access rights for the entry specified by the path
	 * @throws RepositoryException
	 * 		on fail
	 * @throws PasswordInputCanceledException
	 * 		if the user canceled the login dialog
	 */
	List<AccessRights> getAccessRights(String path) throws PasswordInputCanceledException, RepositoryException;

	/**
	 * @return the {@link BindingProvider} for the content manager
	 */
	BindingProvider getBindingProvider();
}
