/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.template;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.rapidminer.Process;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.tools.IOObjectSerializer;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;


/**
 * Description of a process template, i.e. a use cases from a business perspective. A template
 * consists of description texts, a process, configuration options for that process, and a
 * description of the output.
 * 
 * @author Simon Fischer
 * 
 */
public class Template {

	private static final Pattern RESULT_PLOT_PATTERN = Pattern.compile("result_([0-9]+).*.properties");

	/**
	 * ZipInputStream with a noop close() method so we can actually pass this stream to utility
	 * methods which illegally call close().
	 */
	private final class NonClosingZipInputStream extends ZipInputStream {

		private NonClosingZipInputStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() throws IOException {
			// noop to prevent utility methods from closing the zip.
		}

		public void close2() throws IOException {
			super.close();
		}
	}

	private String title = "Unnamed template";
	private String shortDescription = "This template does not have a description.";
	private String processXML;
	private List<Properties> resultPlotterSettings = new ArrayList<>();
	private Icon icon;
	/** Resources found in the resources/ folder of the zip file. */
	private Map<String, byte[]> resources = new HashMap<>();

	private EnumMap<Step, String> helpTexts = new EnumMap<>(Step.class);

	private List<RoleRequirement> roleRequirements = new ArrayList<>();

	private String name;
	private String learnMoreURL;
	private ExampleSet demoData;
	private String requiredExtensions;
	private String requiredExtensionNames;

	public Template(String name) throws IOException, RepositoryException {
		this.name = name;
		InputStream in = Tools.getResourceInputStream("template/" + name + ".zip");
		try {
			load(in);
		} finally {
			in.close();
		}
	}

	public Template(File file) throws IOException, RepositoryException {
		this.name = file.getName().replaceAll("\\.zip", "");
		FileInputStream in = new FileInputStream(file);
		try {
			load(in);
		} finally {
			in.close();
		}
	}

	public String getTitle() {
		return title;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public Process makeProcess() {
		try {
			return new Process(processXML);
		} catch (IOException | XMLException e) {
			throw new RuntimeException("Illegal process file format: " + e, e);
		}
	}

	public int getNumberOfRoleRequirements() {
		return roleRequirements.size();
	}

	public RoleRequirement getRoleRequirement(int i) {
		return roleRequirements.get(i);
	}

	public List<RoleRequirement> getRoleRequirements() {
		return Collections.unmodifiableList(roleRequirements);
	}

	public String getHelpText(Step step) {
		return helpTexts.get(step);
	}

	public Icon getIcon() {
		return icon;
	}

	public List<Properties> getResultPlotterSettings() {
		return Collections.unmodifiableList(resultPlotterSettings);
	}

	private void load(InputStream rawIn) throws IOException {
		NonClosingZipInputStream zip = new NonClosingZipInputStream(rawIn);

		try {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				String entryName = entry.getName();
				if ("template.properties".equals(entryName)) {
					Properties props = new Properties();
					props.load(zip);
					title = props.getProperty("template.name", "Unnamed template");
					shortDescription = props.getProperty("template.short_description",
							"This template does not have a description.");
					learnMoreURL = props.getProperty("template.learn_more_url");
					requiredExtensions = props.getProperty("template.required_extensions");
					requiredExtensionNames = props.getProperty("template.required_extension_names");
					int numberOfInputRoles = parseInt(props.getProperty("template.number_role_requirements", "1"),
							"Illegal number of input roles");
					for (int i = 0; i < numberOfInputRoles; i++) {
						roleRequirements.add(new RoleRequirement(props, i + 1));
					}
				} else if ("process.xml".equals(entryName)) {
					processXML = Tools.readTextFile(zip);
				} else if ("input.ioo".equals(entryName)) {
					this.demoData = (ExampleSet) IOObjectSerializer.getInstance().deserialize(zip);
					this.demoData.getAnnotations().removeAnnotation(Annotations.KEY_SOURCE);
				} else if ("icon.png".equals(entryName)) {
					icon = new ImageIcon(Tools.readInputStream(zip));
				} else if ("template.html".equals(entryName)) { // documentation texts
					helpTexts.put(Step.TEMPLATE, Tools.readTextFile(zip));
				} else if ("data.html".equals(entryName)) {
					helpTexts.put(Step.DATA, Tools.readTextFile(zip));
				} else if ("results.html".equals(entryName)) {
					helpTexts.put(Step.RESULTS, Tools.readTextFile(zip));
				} else if (entryName.matches("resources/.+")) { // Need to have at least one more
																// character! Otherwise matches
																// folder
					String resourceName = entryName.substring("resources/".length());
					byte[] resource = Tools.readInputStream(zip);
					resources.put(resourceName, resource);
				} else {
					Matcher matcher = RESULT_PLOT_PATTERN.matcher(entryName);
					if (matcher.matches()) {
						// The index in the file name indicates what result port these settings
						// refer to
						String indexString = matcher.group(1);
						int index;
						try {
							index = Integer.parseInt(indexString);
						} catch (NumberFormatException e) {
							throw new IOException("Illegal result properties name in template file: " + entry.getName(), e);
						}
						while (resultPlotterSettings.size() < index) {
							resultPlotterSettings.add(null);
						}
						Properties props = new Properties();
						props.load(zip);
						// numbering starts at 0, file name numbering at 1
						resultPlotterSettings.set(index - 1, props);
					}
				}
			}
		} finally {
			zip.close(); // noop ; to avoid compile time warning about resource leek
			zip.close2();
		}
	}

	/** Throws an IOException upon unsuccessful parse. */
	private static int parseInt(String intStr, String errorMessage) throws IOException {
		try {
			return Integer.parseInt(intStr);
		} catch (NumberFormatException e) {
			throw new IOException(errorMessage + ": " + intStr, e);
		}
	}

	public String getName() {
		return name;
	}

	public InputStream getResource(String resourceName) {
		return new ByteArrayInputStream(resources.get(resourceName));
	}

	public Collection<String> getResourceNames() {
		return resources.keySet();
	}

	public ExampleSet getDemoData() {
		return demoData;
	}

	public String getLearnMoreURL() {
		return learnMoreURL;
	}

	public String[] getRequiredExtensions() {
		if (requiredExtensions == null) {
			return null;
		}
		return requiredExtensions.trim().split(",");
	}

	public String getRequiredExtensionNames() {
		if (requiredExtensionNames == null) {
			return null;
		}
		return requiredExtensionNames;
	}

}
