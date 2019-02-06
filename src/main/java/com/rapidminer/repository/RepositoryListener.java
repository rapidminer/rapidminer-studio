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
package com.rapidminer.repository;

import java.util.EventListener;

import com.rapidminer.repository.internal.remote.RemoteRepository;


/**
 * A listener listening to changes of a repository.
 * <p>
 *     <strong>Note that since 8.1, the new method {@link #entryMoved(Entry, Folder, String)} is available!</strong>
 * </p>
 * 
 * @author Simon Fischer, Marco Boeck
 */
public interface RepositoryListener extends EventListener {

	/**
	 * Fired when an entry has been added.
	 *
	 * @param newEntry
	 * 		the new entry that has been added
	 * @param parent
	 * 		the folder where the entry has been added
	 */
	void entryAdded(Entry newEntry, Folder parent);

	/**
	 * Fired when the content/type of an entry did change, but <strong>not</strong> the location.
	 * <p>
	 * <strong>Note that before 8.1, this was also called if an entry was moved/renamed. Since 8.1, {@link #entryMoved(Entry, Folder, String)} is called instead IF YOU OVERWRITE IT!
	 * <br/>
	 * If you do not overwrite it, this method is still called for those events as well.</strong>
	 * </p>
	 *
	 * @param entry
	 * 		the entry that has changed
	 */
	void entryChanged(Entry entry);

	/**
	 * Fired when an entry has been moved or renamed.
	 * <p>
	 * <strong>Since 8.1, this method is called instead of the old {@link #entryChanged(Entry)}!
	 * <br/>
	 * However, if you do not overwrite this method's default implementation, the old method is still called for those events as well.</strong>
	 * </p>
	 *
	 * @param newEntry
	 * 		the new entry that is the result of the move/rename
	 * @param formerParent
	 * 		the folder where the entry was located in before. If it was a rename, it may still be the same folder now. Will be {@code null} for repository rename events!
	 * @param formerName
	 * 		the name of the entry before. If it was a move, it may still have the same name now
	 * @since 8.1
	 */
	default void entryMoved(Entry newEntry, Folder formerParent, String formerName) {
		// this did not exist before, by default make it fire the change event
		entryChanged(newEntry);
	}

	/**
	 * Fired when an entry has been removed.
	 *
	 * @param removedEntry
	 * 		the entry that has been removed
	 * @param parent
	 * 		the folder where the entry has been removed from
	 * @param oldIndex
	 * 		the former index in the repository of the removed entry
	 */
	void entryRemoved(Entry removedEntry, Folder parent, int oldIndex);

	/**
	 * Fired when a repository folder has been refreshed.
	 * This can result in lots of things having changed in that folder and subfolders, or no change at all.
	 * Listeners need to check the contents of the folder to make sure they work on the latest information.
	 *
	 * @param folder
	 * 		the folder that has been refreshed.
	 */
	void folderRefreshed(Folder folder);

	/**
	 * Fired when a {@link RemoteRepository} has been disconnected. Does nothing by default.
	 *
	 * @param repository
	 * 		the repository that ahs been disconnected
	 * @since 8.2.1
	 */
	default void repositoryDisconnected(RemoteRepository repository){}

}
