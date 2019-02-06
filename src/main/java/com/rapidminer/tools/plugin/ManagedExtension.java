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
package com.rapidminer.tools.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 *
 * @author Simon Fischer
 *
 */
public class ManagedExtension {

	/** Maps {@link ManagedExtension#getPackageId()} to the ManagedExtension itself. */
	private static final Map<String, ManagedExtension> MANAGED_EXTENSIONS = new TreeMap<>((ext1, ext2) -> {
		if (ext1 == null && ext2 == null) {
			return 0;
		}

		if (ext1 == null) {
			return 1;
		}

		if (ext2 == null) {
			return -1;
		}
		return ext1.compareTo(ext2);
	});

	private final SortedSet<VersionNumber> installedVersions = new TreeSet<>();
	private final String packageID;
	private final String name;
	private VersionNumber selectedVersion;
	private boolean active;
	private final String license;

	private ManagedExtension(Element element) {
		this.packageID = XMLTools.getTagContents(element, "id");
		this.name = XMLTools.getTagContents(element, "name");
		this.license = XMLTools.getTagContents(element, "license");
		this.active = Boolean.parseBoolean(XMLTools.getTagContents(element, "active"));
		String versionString = XMLTools.getTagContents(element, "selected-version");
		try {
			this.selectedVersion = new VersionNumber(versionString);
		} catch (VersionNumber.VersionNumberException vne) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.tools.plugin.ManagedExtension.malformed_version",
					new Object[]{versionString,vne.getLocalizedMessage()});
			// use max installed version
		}
		NodeList versions = element.getElementsByTagName("installed-version");
		for (int i = 0; i < versions.getLength(); i++) {
			String versionnumber = versions.item(i).getTextContent();
			try {
				installedVersions.add(new VersionNumber(versionnumber));
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.plugin.ManagedExtension.malformed_installedversion",
						new Object[]{name, versionnumber, e.getLocalizedMessage()});
			}
		}
		if(selectedVersion == null) {
			selectedVersion = installedVersions.last();
		}
	}

	private ManagedExtension(String id, String name, String license) {
		super();
		this.packageID = id;
		this.name = name;
		this.license = license;
		this.selectedVersion = null;
		this.setActive(true);
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public String getName() {
		return name;
	}

	private File findFile() {
		return findFile(getSelectedShortLongVersion());
	}

	private File findFile(String version) {
		File file = new File(getUserExtensionsDir(), packageID + "-" + version + ".jar");
		if (file.exists()) {
			return file;
		}
		return null;
	}

	/**
	 * This method returns the jar file of the extension or throws an {@link FileNotFoundException}
	 * exception.
	 */
	public JarFile findArchive() throws IOException {
		File findFile = findFile();
		if (findFile != null) {
			return new JarFile(findFile);
		}
		throw new FileNotFoundException("Could not access file of installed extension.");
	}

	public JarFile findArchive(String version) throws IOException {
		File findFile = findFile(version);
		if (findFile == null) {
			throw new IOException(
					"Failed to find extension jar file (extension " + getName() + ", version " + version + ").");
		} else {
			try {
				return new JarFile(findFile);
			} catch (IOException e) {
				throw new IOException("Failed to open jar file " + findFile + ": " + e, e);
			}
		}
	}

	public VersionNumber getSelectedVersion() {
		return selectedVersion;
	}

	public static File getUserExtensionsDir() {
		return FileSystemService.getUserConfigFile("managed");
	}

	private String getSelectedShortLongVersion() {
		return selectedVersion == null ? null : selectedVersion.getShortLongVersion();
	}

	private Element toXML(Document doc) {
		Element result = doc.createElement("extension");
		XMLTools.setTagContents(result, "id", packageID);
		XMLTools.setTagContents(result, "name", name);
		XMLTools.setTagContents(result, "active", "" + active);
		XMLTools.setTagContents(result, "license", license);
		XMLTools.setTagContents(result, "selected-version", getSelectedShortLongVersion());
		for (VersionNumber v : installedVersions) {
			Element elem = doc.createElement("installed-version");
			result.appendChild(elem);
			elem.appendChild(doc.createTextNode(v.getShortLongVersion()));
		}
		return result;
	}

	private static Document toXML() throws ParserConfigurationException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("extensions");
		doc.appendChild(root);
		for (ManagedExtension ext : MANAGED_EXTENSIONS.values()) {
			root.appendChild(ext.toXML(doc));
		}
		return doc;
	}

	public static void saveConfiguration() {
		try {
			File localDir = getUserExtensionsDir();
			if (!localDir.exists()) {
				localDir.mkdirs();
			}
			XMLTools.stream(toXML(), new File(localDir, "extensions.xml"), Charset.forName("UTF-8"));
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapid_i.deployment.update.client.ManagedExtension.saving_local_user_extensions_error", e),
					e);

		}
		LogService.getRoot().log(Level.CONFIG,
				"com.rapid_i.deployment.update.client.ManagedExtension.saved_extension_state");
	}

	/** Reads configuration files. */
	private static void readConfiguration() {
		MANAGED_EXTENSIONS.clear();
		try {
			File file = new File(getUserExtensionsDir(), "extensions.xml");
			if (file.exists()) {
				parse(XMLTools.parse(file));
			}
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapid_i.deployment.update.client.ManagedExtension.reading_local_extensions_state_error", e),
					e);
		}
		LogService.getRoot().log(Level.CONFIG, "com.rapid_i.deployment.update.client.ManagedExtension.read_extansion_state");
	}

	private static void parse(Document parse) {
		NodeList extensions = parse.getDocumentElement().getElementsByTagName("extension");
		for (int i = 0; i < extensions.getLength(); i++) {
			register(new ManagedExtension((Element) extensions.item(i)));
		}
	}

	private static void register(ManagedExtension ext) {
		MANAGED_EXTENSIONS.put(ext.packageID, ext);
	}

	public static List<File> getActivePluginJars() {
		List<File> result = new LinkedList<>();
		for (ManagedExtension ext : MANAGED_EXTENSIONS.values()) {
			if (ext.isActive()) {
				File file = ext.findFile();
				if (file != null) {
					result.add(file);
				}
			}
		}
		return result;
	}

	public static ManagedExtension get(String packageId) {
		return MANAGED_EXTENSIONS.get(packageId);
	}

	public static ManagedExtension getOrCreate(String packageId, String packageName, String license) {
		ManagedExtension ext = MANAGED_EXTENSIONS.get(packageId);
		if (ext == null) {
			ext = new ManagedExtension(packageId, packageName, license);
			MANAGED_EXTENSIONS.put(packageId, ext);
			saveConfiguration();
		}
		return ext;
	}

	public static ManagedExtension remove(String packageId) {
		return MANAGED_EXTENSIONS.remove(packageId);
	}

	public String getPackageId() {
		return packageID;
	}

	public void addAndSelectVersion(String version) {
		VersionNumber selected = new VersionNumber(version);
		installedVersions.add(selected);
		this.selectedVersion = selected;
		saveConfiguration();
	}

	public File getDestinationFile(String version) throws IOException {
		return new File(getUserExtensionsDir(), packageID + "-" + version + ".jar");
	}

	public static void init() {
		readConfiguration();
	}

	public static Collection<ManagedExtension> getAll() {
		return MANAGED_EXTENSIONS.values();
	}

	/**
	 * VersionNumbers for all installed versions of this extension
	 * @since 8.0
	 * @return sorted array of VersionNumbers
	 */
	public VersionNumber[] getInstalledVersions() {
		return installedVersions.toArray(new VersionNumber[installedVersions.size()]);
	}

	public void setSelectedVersion(VersionNumber version) {
		this.selectedVersion = version;
	}

	/**
	 * Find the installed VersionNumber that is younger than the given version
	 * @since 8.0
	 * @param version a VersionNumber String representation that is the search limit
	 * @return newest installed version before param version
	 */
	public VersionNumber getLatestInstalledVersionBefore(String version) {
		SortedSet<VersionNumber> head = installedVersions.headSet(new VersionNumber(version));
		return head.isEmpty() ? null : head.last();
	}

	/**
	 * Latest installed version
	 * @since 8.0
	 * @return VersionNumber of the latest installed version or null if none are present
	 */
	public VersionNumber getLatestInstalledVersion() {
		return installedVersions.isEmpty() ? null : installedVersions.last();
	}

	/** Returns true if uninstall was successful. */
	public boolean uninstallActiveVersion() {
		File file = findFile();
		// we only mark as uninstalled if
		// (1) File does not exist, probably was removed manually
		// (2) We were able to remove it (requires administrator permissions if installed globally).
		if (file != null && file.exists()) {
			file.delete();
		}
		installedVersions.remove(selectedVersion);
		selectedVersion = null;
		active = false;
		if (installedVersions.isEmpty()) {
			MANAGED_EXTENSIONS.remove(this.getPackageId());
		}
		saveConfiguration();
		return true;
	}
}
