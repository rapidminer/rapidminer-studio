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
package com.rapidminer.template;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.rapidminer.Process;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.resource.ZipStreamResource;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.NonClosingZipInputStream;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;


/**
 * Templates for new processes. The template must be a .zip file renamed to .template containing
 *
 * <ul>
 * <li>an {@code icon.png} of size 64x64</li>
 * <li>a {@code template.properties} file</li>
 * <li>one {@code .rmp} process file</li>
 * <li>an arbitrarily number of {@code .ioo} and {@code .md} files that are used in the process</li>
 * </ul>
 * The {@code template.properties} file has to contain the keys
 * <ul>
 * <li>template.name (which defines the title of the template)</li>
 * <li>template.short_description (which defines the description of the template)</li>
 * </ul>
 * The process and the data files are automatically added to the folder
 * //Samples/Templates/[template.title] and the data files must be referred from the process with an
 * absolute path accordingly. </br> </br> The template files can be added into
 * .Rapidminer/templates/ or under template/ in the extension resources (
 * {@code src/main/resources/com.rapidminer.resources.template/[fileName].template}). Template files
 * in the resources have to be registered via {@link TemplateManager#registerTemplate(String)} while
 * the files from the .Rapidminer folder are loaded automatically.
 *
 * @author Simon Fischer, Gisa Schaefer
 *
 */
public class Template implements ZipStreamResource {

	/** the repository location for templates */
	private static final String TEMPLATES_PATH = "//Samples/Templates/";

	/** the resources location for templates */
	private static final String RESOURCES_LOCATION = "template/";

	private static final String NO_DESCRIPTION = I18N.getGUILabel("template.no_description");
	private static final String NO_TITLE = I18N.getGUILabel("template.no_title");

	/** special template which is not loaded from a resource but simply creates a new, empty process */
	public static final Template BLANK_PROCESS_TEMPLATE = new Template() {

		@Override
		public Process makeProcess() throws IOException, XMLException, MalformedRepositoryLocationException {
			return new Process();
		}
	};

	private String title = NO_TITLE;
	private String shortDescription = NO_DESCRIPTION;
	private String processName;
	private List<String> demoData;
	private Icon icon;

	private final Path path;
	private final String name;

	/**
	 * Private constructor for special blank process template only.
	 */
	private Template() {
		this.title = I18N.getGUILabel("getting_started.new.empty.title");
		this.shortDescription = I18N.getGUILabel("getting_started.new.empty.description");
		this.icon = SwingTools.createIcon("64/" + I18N.getGUILabel("getting_started.new.empty.icon"));
		this.demoData = new LinkedList<>();
		this.name = this.title;
		this.processName = null;
		this.path = null;
	}

	Template(String name) throws IOException, RepositoryException {
		this.name = name;
		this.path = null;
		load();
	}

	Template(Path path) throws IOException, RepositoryException {
		this.name = path.getFileName().toString().replaceAll("\\.template", "");
		this.path = path;
		load();
	}

	/**
	 * @return the {@link ZipInputStream} to load resources associated with this template
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
		return shortDescription;
	}

	@Override
	public String getStreamPath() {
		return null;
	}

	/**
	 * @return the name of the template which is also the name of the zip file
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return a new process that contains the template process
	 * @throws XMLException
	 * @throws IOException
	 * @throws MalformedRepositoryLocationException
	 */
	public Process makeProcess() throws IOException, XMLException, MalformedRepositoryLocationException {
		String processLocation = TEMPLATES_PATH + title + "/" + processName;
		RepositoryProcessLocation repoLocation = new RepositoryProcessLocation(new RepositoryLocation(processLocation));
		return new Process(repoLocation.getRawXML());
	}

	/**
	 * @return the name of the process behind this template
	 */
	public String getProcessName() {
		return processName;
	}

	/**
	 * @return the icon to display
	 */
	public Icon getIcon() {
		return icon;
	}

	/**
	 * @return the list of demo data names
	 */
	public List<String> getDemoData() {
		return demoData;
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
				while ((entry = zip.getNextEntry()) != null) {
					if (entry.isDirectory()) {
						throw new RepositoryException("Template malformed. A template must not contain a directory.");
					}
					String entryName = entry.getName();
					if ("template.properties".equals(entryName)) {
						Properties props = new Properties();
						props.load(zip);
						title = props.getProperty("template.name", NO_TITLE);
						shortDescription = props.getProperty("template.short_description", NO_DESCRIPTION);
					} else if (entryName.endsWith(".rmp")) {
						processName = entryName.split("\\.")[0];
					} else if (entryName.endsWith(".ioo")) {
						demoData.add(entryName.split("\\.")[0]);
					} else if ("icon.png".equals(entryName)) {
						icon = new ImageIcon(Tools.readInputStream(zip));
					}
				}
			} finally {
				zip.close(); // noop ; to avoid compile time warning about resource leak
				zip.close2();
			}
		}
	}

	/**
	 * @return the {@link InputStream} to of this template
	 */
	private InputStream getInputStream() throws IOException, RepositoryException {
		if (path != null) {
			return Files.newInputStream(path);
		} else {
			return Tools.getResourceInputStream(RESOURCES_LOCATION + name + ".template");
		}
	}
}
