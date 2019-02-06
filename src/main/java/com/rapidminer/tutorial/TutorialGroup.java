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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.rapidminer.RapidMiner;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.resource.ZipStreamResource;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.NonClosingZipInputStream;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;


/**
 * The tutorial group must be a .zip file renamed to .tutorial containing
 *
 * <ul>
 * <li>a {@code group.properties} file which contains the default English properties</li>
 * <li>an arbitrarily number of localized {@code group_xy.properties} files (e.g.
 * {@code group_de.properties}) with ISO-8859-1 encoding</li>
 * <li>an arbitrarily number of folders which are defined in the {@link Tutorial} class</li>
 * </ul>
 * Any {@code group.properties} file has to contain the keys
 * <ul>
 * <li>group.name (which defines the title of the tutorial group)</li>
 * <li>group.description (which defines the description of the tutorial group)</li>
 * </ul>
 * </br>
 * The tutorial group files can be added into .RapidMiner/tutorials/ or under tutorial/ in the
 * extension resources (
 * {@code src/main/resources/com.rapidminer.resources.tutorial/[fileName].tutorial}). Tutorial files
 * in the resources have to be registered via {@link TutorialRegistry#register(String)} while the
 * files from the .RapidMiner folder are loaded automatically.
 * <p>
 * Tutorials placed in the .RapidMiner folder will be preferred over tutorials with the same name
 * registered via {@link TutorialRegistry#register(String)}.
 * <p>
 * <strong>NOTE:</strong> <br/>
 * The content of the tutorial group will be mirrored in the Sample Repository, see the
 * {@link Tutorial} documentation for the path definition.
 *
 * @author Gisa Schaefer, Marcel Michel
 * @since 7.0.0
 *
 */
public class TutorialGroup implements ZipStreamResource {

	private static final Comparator<Tutorial> TUTORIAL_COMPARATOR = new Comparator<Tutorial>() {

		@Override
		public int compare(Tutorial t1, Tutorial t2) {
			String n1 = t1.getStreamPath();
			String n2 = t1.getStreamPath();
			if (n1 == null && n2 == null) {
				return 0;
			}
			if (n1 == null && n2 != null) {
				return -1;
			}
			if (n1 != null && n2 == null) {
				return +1;
			}
			return n1.compareTo(n2);
		}
	};

	/** key of the description in property files */
	private static final String KEY_GROUP_DESCRIPTION = "group.description";

	/** key of the title in property files */
	private static final String KEY_GROUP_NAME = "group.name";

	/** the resources location for tutorials */
	private static final String RESOURCES_LOCATION = "tutorial/";

	private static final String NO_DESCRIPTION = I18N.getGUILabel("tutorial.group.no_description");
	private static final String NO_TITLE = I18N.getGUILabel("tutorial.group.no_title");

	private static final String DEFAULT_PROPERTY_FILE = "group.properties";
	private static final String PROPERTY_FILE_TEMPLATE = "group_%s.properties";

	private String title = NO_TITLE;
	private String description = NO_DESCRIPTION;
	private Path path;

	private List<Tutorial> tutorials;

	private String name;

	TutorialGroup(String name) throws IOException, RepositoryException {
		this.name = name;
		load();
	}

	TutorialGroup(Path path) throws IOException, RepositoryException {
		this.name = path.getFileName().toString().replaceAll("\\.tutorial", "");
		this.path = path;
		load();
	}

	/**
	 * @return the stream to load resources associated with this tutorial
	 */
	@Override
	public ZipInputStream getStream() throws IOException, RepositoryException {
		return new ZipInputStream(getInputStream());
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getStreamPath() {
		return null;
	}

	/**
	 * @return the name of the tutorial which is also the name of the zip file
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the contained {@link Tutorial}s as {@link List}
	 */
	public List<Tutorial> getTutorials() {
		return Collections.unmodifiableList(tutorials);
	}

	/**
	 * Uses the {@link TutorialManager} to query the number of completed {@link Tutorial}s.
	 *
	 * @return the number of completed {@link Tutorial}s
	 */
	public int getNumberOfCompletedTutorials() {
		int count = 0;
		for (Tutorial tutorial : tutorials) {
			if (TutorialManager.INSTANCE.hasCompletedTutorial(tutorial.getIdentifier())) {
				count++;
			}
		}
		return count;
	}

	/**
	 * @return the number of contained {@link Tutorial}s
	 */
	public int getNumberOfTutorials() {
		return tutorials.size();
	}

	/**
	 * @return {@code true} if the result of {@link #getNumberOfTutorials()} is equal to the result
	 *         of {@link #getNumberOfCompletedTutorials()}, otherwise {@code false}
	 */
	public boolean hasCompleted() {
		return getNumberOfCompletedTutorials() == getNumberOfTutorials();
	}

	/**
	 * Loads the content of the zip file.
	 */
	private void load() throws IOException, RepositoryException {
		tutorials = new ArrayList<>();
		try (InputStream rawIn = getInputStream()) {
			NonClosingZipInputStream zip = new NonClosingZipInputStream(rawIn);
			try {
				ZipEntry entry;
				String localeFileName = getPropertyFileName();
				Properties defaultProps = new Properties();
				Properties localProps = new Properties();
				while ((entry = zip.getNextEntry()) != null) {
					if (entry.getName().replaceFirst("/", "").contains("/")) {
						// ignore second folder level and above
						continue;
					}
					String entryName = entry.getName();
					if (entry.isDirectory()) {
						if (path != null) {
							tutorials.add(new Tutorial(this, path, entryName));
						} else {
							tutorials.add(new Tutorial(this, entryName));
						}
					} else if (DEFAULT_PROPERTY_FILE.equals(entryName)) {
						defaultProps.load(zip);
					} else if (localeFileName.equals(entryName)) {
						localProps.load(zip);
					}
				}

				// load title and description from default props
				title = defaultProps.getProperty(KEY_GROUP_NAME, NO_TITLE);
				description = defaultProps.getProperty(KEY_GROUP_DESCRIPTION, NO_DESCRIPTION);

				// exchange titel and description by locale prop if available
				if (!localProps.isEmpty()) {
					title = localProps.getProperty(KEY_GROUP_NAME, title);
					description = localProps.getProperty(KEY_GROUP_DESCRIPTION, description);
				}

				if (title.isEmpty()) {
					title = NO_TITLE;
				}
				if (description.isEmpty()) {
					description = NO_DESCRIPTION;
				}
				Collections.sort(tutorials, TUTORIAL_COMPARATOR);
			} finally {
				zip.close(); // noop ; to avoid compile time warning about resource leak
				zip.close2();
			}
		}
	}

	/**
	 * @return the {@link InputStream} to of this tutorial group
	 */
	private InputStream getInputStream() throws IOException, RepositoryException {
		if (path != null) {
			return Files.newInputStream(path);
		} else {
			return Tools.getResourceInputStream(RESOURCES_LOCATION + name + ".tutorial");
		}
	}

	/**
	 * @return the name of the localized property file (e.g. tutorial_de.properties)
	 */
	private String getPropertyFileName() {
		String localeLanguage = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_LOCALE_LANGUAGE);
		Locale locale = Locale.getDefault();
		if (localeLanguage != null) {
			locale = new Locale(localeLanguage);
		}
		return String.format(PROPERTY_FILE_TEMPLATE, locale.getLanguage());
	}
}
