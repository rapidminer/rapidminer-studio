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
package com.rapidminer.repository.resource;

import java.util.List;

import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tutorial.Tutorial;
import com.rapidminer.tutorial.TutorialGroup;


/**
 * Folder that lists the content of a single {@link TutorialGroup}.
 *
 * @author Marcel Michel, Jan Czogalla
 * @since 7.0.0
 */
public class TutorialGroupFolder extends ResourceFolder {

	private TutorialGroup tutorialGroup;

	protected TutorialGroupFolder(ResourceFolder parent, TutorialGroup tutorialGroup, String parentPath, ResourceRepository repository) {
		super(parent, tutorialGroup.getName(), parentPath + "/" + tutorialGroup.getName(), repository);
		this.tutorialGroup = tutorialGroup;
	}

	@Override
	protected void ensureLoaded(List<Folder> folders, List<DataEntry> data) throws RepositoryException {
		for (Tutorial tutorial : tutorialGroup.getTutorials()) {
			// do not add empty folder
			if (tutorial.getProcessName() != null || !tutorial.getDemoData().isEmpty()) {
				String name = tutorial.getStreamPath().replaceFirst("/", "");
				folders.add(new ZipResourceFolder(this, name, tutorial, getPath(), getRepository()));
			}
		}
	}
}
