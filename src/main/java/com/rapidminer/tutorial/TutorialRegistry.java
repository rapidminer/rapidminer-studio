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
package com.rapidminer.tutorial;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.io.GlobFilenameFilter;


/**
 * Registry for {@link TutorialGroup}s. Use {@link #register(String)} to register a new tutorial
 * group.
 *
 * @author Gisa Schaefer, Marcel Michel
 * @since 7.0.0
 *
 */
public enum TutorialRegistry {

	INSTANCE;

	/** the folder inside .RapidMiner containing the tutorials */
	private static final String FOLDER_NAME_TUTORIALS = "tutorials";

	private Map<String, TutorialGroup> tutorialGroupsByName = new LinkedHashMap<>();

	private TutorialRegistry() {
		// register tutorials from bundled resources
		register("Basics");
		register("Data Handling");
		register("Modeling, Scoring, and Validation");
		register("RapidMiner Server");
		register("RapidMiner Radoop");

		// Load tutorials from .RapidMiner folder to allow sharing
		File tempDir = new File(FileSystemService.getUserRapidMinerDir(), FOLDER_NAME_TUTORIALS);
		if (tempDir.exists() && tempDir.isDirectory()) {
			for (File file : tempDir.listFiles(new GlobFilenameFilter("*.tutorial"))) {
				try {
					register(new TutorialGroup(Paths.get(file.toURI())));
				} catch (IOException | RepositoryException e) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.tutorial.TutorialRegistry.failed_to_load_tutorialfile",
							new Object[] { file, e });
				}
			}
		}
	}

	/**
	 * Registers a tutorial group that should be loaded from the resources at
	 * tutorial/[tutorialGroupName].tutorial. </br>
	 * Given a file called {@code my-extension.tutorial} is present at
	 * {@code src/main/resources/com.rapidminer.resources.tutorial/} the following snippet will
	 * register the tutorial:
	 *
	 * <pre>
	 * {@code public static void initGui(MainFrame mainframe) {
	 * 	TutorialRegistry.INSTANCE.register("my-extension");
	 * }
	 * </pre>
	 *
	 * @see TutorialGroup Description of the .tutorial file contents
	 *
	 * @param tutorialGroupName
	 *            the unique name of the tutorial group
	 */
	public void register(String tutorialGroupName) {
		if (tutorialGroupName == null) {
			throw new IllegalArgumentException("tutorialGroupName must not be null!");
		}
		if (tutorialGroupsByName.containsKey(tutorialGroupName)) {
			LogService.getRoot().log(Level.INFO,
					"Tutorial group with name '" + tutorialGroupName + "' was already registerd. Skipping registration.");
			return;
		}
		try {
			register(new TutorialGroup(tutorialGroupName));
		} catch (IOException | RepositoryException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tutorial.TutorialRegistry.failed_to_load_tutorial", e);
		}
	}

	private void register(TutorialGroup tutorialGroup) {
		tutorialGroupsByName.put(tutorialGroup.getName(), tutorialGroup);
	}

	/**
	 * Gets a {@link TutorialGroup} by name.
	 *
	 * @param tutorialGroupName
	 *            the name the tutorial group
	 * @return the found tutorial group or {@code null}
	 */
	public TutorialGroup getTutorialGroup(String tutorialGroupName) {
		return tutorialGroupsByName.get(tutorialGroupName);
	}

	/**
	 * @return all registered {@link TutorialGroup}s
	 */
	public List<TutorialGroup> getAllTutorialGroups() {
		return new ArrayList<>(tutorialGroupsByName.values());
	}
}
