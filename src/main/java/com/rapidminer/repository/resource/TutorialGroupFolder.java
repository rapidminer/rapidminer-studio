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
package com.rapidminer.repository.resource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tutorial.Tutorial;
import com.rapidminer.tutorial.TutorialGroup;


/**
 * Folder that lists the content of a single {@link TutorialGroup}.
 *
 * @author Marcel Michel
 * @since 7.0.0
 */
public class TutorialGroupFolder extends ResourceFolder {

	private List<Folder> folders;
	private List<DataEntry> data;

	private TutorialGroup tutorialGroup;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	protected TutorialGroupFolder(ResourceFolder parent, TutorialGroup tutorialGroup, String parentPath,
			ResourceRepository repository) {
		super(parent, tutorialGroup.getName(), parentPath + "/" + tutorialGroup.getName(), repository);
		this.tutorialGroup = tutorialGroup;
	}

	@Override
	public boolean containsEntry(String name) throws RepositoryException {
		acquireReadLock();
		try {
			if (isLoaded()) {
				return containsEntryNotThreadSafe(name);
			}
		} finally {
			releaseReadLock();
		}
		acquireWriteLock();
		try {
			ensureLoaded();
			return containsEntryNotThreadSafe(name);
		} finally {
			releaseWriteLock();
		}

	}

	private boolean containsEntryNotThreadSafe(String name) {
		for (Entry entry : data) {
			if (entry.getName().equals(name)) {
				return true;
			}
		}
		for (Entry entry : folders) {
			if (entry.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<DataEntry> getDataEntries() throws RepositoryException {
		acquireReadLock();
		try {
			if (isLoaded()) {
				return Collections.unmodifiableList(data);
			}
		} finally {
			releaseReadLock();
		}
		acquireWriteLock();
		try {
			ensureLoaded();
			return Collections.unmodifiableList(data);
		} finally {
			releaseWriteLock();
		}
	}

	@Override
	protected boolean isLoaded() {
		return folders != null && data != null;
	}

	@Override
	protected void ensureLoaded() throws RepositoryException {
		if (isLoaded()) {
			return;
		}
		this.folders = new LinkedList<Folder>();
		this.data = new LinkedList<DataEntry>();

		for (Tutorial tutorial : tutorialGroup.getTutorials()) {
			// do not add empty folder
			if (tutorial.getProcessName() != null || !tutorial.getDemoData().isEmpty()) {
				String name = tutorial.getStreamPath().replaceFirst("/", "");
				folders.add(new ZipResourceFolder(this, name, tutorial, getPath(), getRepository()));
			}
		}
	}

	@Override
	public List<Folder> getSubfolders() throws RepositoryException {
		acquireReadLock();
		try {
			if (isLoaded()) {
				return Collections.unmodifiableList(folders);
			}
		} finally {
			releaseReadLock();
		}
		acquireWriteLock();
		try {
			ensureLoaded();
			return Collections.unmodifiableList(folders);
		} finally {
			releaseWriteLock();
		}
	}

	@Override
	public void refresh() throws RepositoryException {
		acquireWriteLock();
		try {
			folders = null;
			data = null;
		} finally {
			releaseWriteLock();
		}
		getRepository().fireRefreshed(this);
	}

	private void acquireReadLock() throws RepositoryException {
		try {
			readLock.lock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not get read lock", e);
		}
	}

	private void releaseReadLock() throws RepositoryException {
		try {
			readLock.unlock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not release read lock", e);
		}
	}

	private void acquireWriteLock() throws RepositoryException {
		try {
			writeLock.lock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not get write lock", e);
		}
	}

	private void releaseWriteLock() throws RepositoryException {
		try {
			writeLock.unlock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not release write lock", e);
		}
	}
}
