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
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tutorial.Tutorial;
import com.rapidminer.tutorial.TutorialGroup;
import com.rapidminer.tutorial.TutorialRegistry;


/**
 * Class for the folder that contains all the data for the {@link Tutorial}s.
 *
 * @author Gisa Schaefer, Marcel Michel, Jan Czogalla
 * @since 7.0.0
 */
public class TutorialFolder extends ResourceFolder {

	private static final String TUTORIAL_FOLDER_NAME = "Tutorials";

	protected TutorialFolder(ResourceFolder parent, String parentPath, ResourceRepository repository) {
		super(parent, TUTORIAL_FOLDER_NAME, parentPath + RepositoryLocation.SEPARATOR + TUTORIAL_FOLDER_NAME, repository);
	}

	@Override
	protected void ensureLoaded(List<Folder> folders, List<DataEntry> data) {
		TutorialRegistry registry = TutorialRegistry.INSTANCE;

		for (TutorialGroup tutorialGroup : registry.getAllTutorialGroups()) {
			// do not add empty tutorial groups
			if (!tutorialGroup.getTutorials().isEmpty()) {
				folders.add(new TutorialGroupFolder(this, tutorialGroup, getPath(), getRepository()));
			}
		}
	}
}
