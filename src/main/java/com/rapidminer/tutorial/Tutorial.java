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
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.io.process.ProcessOriginProcessXMLFilter;
import com.rapidminer.io.process.ProcessOriginProcessXMLFilter.ProcessOriginState;
import com.rapidminer.operator.FlagUserData;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.resource.ZipStreamResource;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.NonClosingZipInputStream;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;


/**
 * A tutorial is a folder in the {@link TutorialGroup} zip file containing
 *
 * <ul>
 * <li>a {@code tutorial.properties} file which contains the default English properties</li>
 * <li>an arbitrarily number of localized {@code tutorial_xy.properties} files (e.g.
 * {@code tutorial_de.properties} with ISO-8859-1 encoding)</li>
 * <li>one {@code .rmp} process file</li>
 * <li>an arbitrarily number of {@code .blob} files
 * <p>
 * (a .blob file is a binary file which was imported into a local RapidMiner repository, e.g. a PNG
 * process background image)</li>
 * <li>an arbitrarily number of {@code .ioo} and {@code .md} files that are used in the process</li>
 * </ul>
 * The {@code tutorial.properties} file has to contain the keys
 * <ul>
 * <li>tutorial.name (which defines the title of the tutorial)</li>
 * <li>tutorial.description (which defines the description of the tutorial)</li>
 * </ul>
 * <strong>NOTE:</strong> <br/>
 * The content of the tutorial folder will be mirrored in the Sample Repository with the path
 * {@value #TUTORIALS_PATH} + {@link #name} + "/" + {@link #folder}. All resources which are used in
 * the tutorial process needs to adapt the resource location accordingly.
 *
 * @since 7.0.0
 * @author Gisa Schaefer, Marcel Michel
 * @see TutorialGroup Description of the .tutorial file contents
 */
public class Tutorial implements ZipStreamResource {

	/**
	 * User data key to flag processes as tutorial process: If the root operator's entry for this
	 * key is non-null, the process is considered a tutorial.
	 */
	public static final String KEY_USER_DATA_FLAG = "com.rapidminer.tutorial.Tutorial";

	/** key of the description in property files */
	private static final String KEY_TUTORIAL_DESCRIPTION = "tutorial.description";

	/** key of the title in property files */
	private static final String KEY_TUTORIAL_NAME = "tutorial.name";

	/** the repository location for tutorials */
	private static final String TUTORIALS_PATH = "//Samples/Tutorials/";

	/** the file name of the step file */
	private static final String STEPS_XML = "steps.xml";
	private static final String STEPS_XML_TEMPLATE = "steps_%s.xml";

	/** the resources location for tutorials */
	private static final String RESOURCES_LOCATION = "tutorial/";

	private static final String NO_DESCRIPTION = I18N.getGUILabel("tutorial.no_description");
	private static final String NO_TITLE = I18N.getGUILabel("tutorial.no_title");

	private static final String DEFAULT_PROPERTY_FILE = "tutorial.properties";
	private static final String PROPERTY_FILE_TEMPLATE = "tutorial_%s.properties";

	private String title = NO_TITLE;
	private String description = NO_DESCRIPTION;
	private String processName;
	private List<String> demoData;

	private final Path path;
	private final TutorialGroup group;
	private final String folder;

	Tutorial(TutorialGroup group, String folder) throws IOException, RepositoryException {
		this(group, null, folder);
	}

	Tutorial(TutorialGroup group, Path path, String folder) throws IOException, RepositoryException {
		this.group = group;
		this.path = path;
		this.folder = folder;
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
		return folder;
	}

	/**
	 * @return the name of the process behind this tutorial
	 */
	public String getProcessName() {
		return processName;
	}

	/**
	 * @return the list of demo data names
	 */
	public List<String> getDemoData() {
		return demoData;
	}

	/**
	 * @return the identifier for this tutorial
	 */
	public String getIdentifier() {
		return getGroup().getName() + "-" + folder;
	}

	/**
	 * @return the group which contains the tutorial
	 */
	public TutorialGroup getGroup() {
		return group;
	}

	/**
	 * @return the steps.xml file as {@link InputStream}, or {@code null}
	 */
	public InputStream getSteps() throws IOException, RepositoryException {
		String localeStepsName = getStepsFileName();
		boolean localeAvailable = false;
		// sadly we need to traverse the zip file two times,
		// because the localized steps file should always be preferred, but it must not be available
		try (InputStream rawIn = getInputStream(); ZipInputStream zip = new ZipInputStream(rawIn)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory() || !entry.getName().startsWith(folder)
						|| entry.getName().replaceFirst("/", "").contains("/")) {
					continue;
				}
				String entryName = entry.getName();
				if (localeStepsName.equals(entryName.replaceFirst(folder, ""))) {
					// the input stream will automatically be closed, return it later
					localeAvailable = true;
					break;
				}
			}
		}
		InputStream rawIn = null;
		ZipInputStream zip = null;
		try {
			rawIn = getInputStream();
			zip = new ZipInputStream(rawIn);
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory() || !entry.getName().startsWith(folder)
						|| entry.getName().replaceFirst("/", "").contains("/")) {
					continue;
				}
				String entryName = entry.getName();
				if (localeStepsName.equals(entryName.replaceFirst(folder, ""))
						|| !localeAvailable && STEPS_XML.equals(entryName.replaceFirst(folder, ""))) {
					return zip;
				}
			}
		} catch (Exception e) {
			if (zip != null) {
				try {
					zip.close();
				} catch (IOException ioe) {
					// ignore
				}
			} else if (rawIn != null) {
				try {
					rawIn.close();
				} catch (IOException ioe) {
					// ignore
				}
			}
			throw e;
		}
		return null;
	}

	/**
	 * @return a new process that contains the tutorial process
	 * @throws XMLException
	 * @throws IOException
	 * @throws MalformedRepositoryLocationException
	 */
	public Process makeProcess() throws IOException, XMLException, MalformedRepositoryLocationException {
		String processLocation = TUTORIALS_PATH + getGroup().getName() + "/" + folder + processName;
		RepositoryProcessLocation repoLocation = new RepositoryProcessLocation(new RepositoryLocation(processLocation));
		Process newProcess = new Process(repoLocation.getRawXML());
		newProcess.getRootOperator().setUserData(KEY_USER_DATA_FLAG, new FlagUserData());
		ProcessOriginProcessXMLFilter.setProcessOriginState(newProcess, ProcessOriginState.GENERATED_TUTORIAL);
		return newProcess;
	}

	/**
	 * Loads the content of the zip file.
	 */
	private void load() throws IOException, RepositoryException {
		try (InputStream rawIn = getInputStream()) {
			demoData = new LinkedList<>();
			NonClosingZipInputStream zip = new NonClosingZipInputStream(rawIn);

			try {
				ZipEntry entry;
				String localeFileName = getPropertyFileName();
				Properties defaultProps = new Properties();
				Properties localProps = new Properties();
				while ((entry = zip.getNextEntry()) != null) {
					if (entry.isDirectory() || !entry.getName().startsWith(folder)
							|| entry.getName().replaceFirst("/", "").contains("/")) {
						continue;
					}
					String entryName = entry.getName();
					if (DEFAULT_PROPERTY_FILE.equals(entryName.replaceFirst(folder, ""))) {
						defaultProps.load(zip);
					} else if (localeFileName.equals(entryName.replaceFirst(folder, ""))) {
						localProps.load(zip);
					} else if (entryName.endsWith(ProcessEntry.RMP_SUFFIX)) {
						processName = Paths.get(entryName).getFileName().toString().split("\\.")[0];
					} else if (entryName.endsWith(IOObjectEntry.IOO_SUFFIX)) {
						demoData.add(Paths.get(entryName).getFileName().toString().split("\\.")[0]);
					}
				}

				// load title and description from default props
				title = defaultProps.getProperty(KEY_TUTORIAL_NAME, NO_TITLE);
				description = defaultProps.getProperty(KEY_TUTORIAL_DESCRIPTION, NO_DESCRIPTION);

				// exchange titel and description by locale prop if available
				if (!localProps.isEmpty()) {
					title = localProps.getProperty(KEY_TUTORIAL_NAME, title);
					description = localProps.getProperty(KEY_TUTORIAL_DESCRIPTION, description);
				}

				if (title.isEmpty()) {
					title = NO_TITLE;
				}
				if (description.isEmpty()) {
					description = NO_DESCRIPTION;
				}

			} finally {
				zip.close(); // noop ; to avoid compile time warning about resource leak
				zip.close2();
			}
		}
	}

	/**
	 * @return the {@link InputStream} to of this tutorial
	 */
	private InputStream getInputStream() throws IOException, RepositoryException {
		if (path != null) {
			return Files.newInputStream(path);
		} else {
			return Tools.getResourceInputStream(RESOURCES_LOCATION + getGroup().getName() + ".tutorial");
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

	/**
	 * @return the name of the localized step file (e.g. steps_de.xml)
	 */
	private String getStepsFileName() {
		String localeLanguage = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_LOCALE_LANGUAGE);
		Locale locale = Locale.getDefault();
		if (localeLanguage != null) {
			locale = new Locale(localeLanguage);
		}
		return String.format(STEPS_XML_TEMPLATE, locale.getLanguage());
	}
}
