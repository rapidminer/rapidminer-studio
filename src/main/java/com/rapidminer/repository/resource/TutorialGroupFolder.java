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
package com.rapidminer.repository.resource;

import java.util.LinkedList;
import java.util.List;

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

	protected TutorialGroupFolder(ResourceFolder parent, TutorialGroup tutorialGroup, String parentPath,
			ResourceRepository repository) {
		super(parent, tutorialGroup.getName(), parentPath + "/" + tutorialGroup.getName(), repository);
		this.tutorialGroup = tutorialGroup;
	}

	@Override
	public boolean containsEntry(String name) throws RepositoryException {
		ensureLoaded();
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
		ensureLoaded();
		return data;
	}

	@Override
	protected void ensureLoaded() throws RepositoryException {
		if (folders != null && data != null) {
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
		ensureLoaded();
		return folders;
	}

	@Override
	public void refresh() throws RepositoryException {
		folders = null;
		data = null;
		getRepository().fireRefreshed(this);
	}

}
