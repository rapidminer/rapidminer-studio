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
package com.rapidminer.repository;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.RapidMiner;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;


/**
 * This {@link RepositoryProvider} will use the {@value #FILE_NAME} to {@link #load()} and
 * {@link #save()} the repository configuration.
 *
 * @author Marcel Michel
 *
 */
public class FileRepositoryProvider implements RepositoryProvider {

	private static final Logger LOGGER = LogService.getRoot();

	public static final String TAG_LOCAL_REPOSITORY = "localRepository";
	public static final String TAG_REPOSITORIES = "repositories";

	public static final String FILE_NAME = "repositories.xml";

	private final List<Element> failedToLoad = new LinkedList<>();

	private File getConfigFile() {
		return FileSystemService.getUserConfigFile(FILE_NAME);
	}

	@Override
	public List<Repository> load() {

		// clear failed list first
		failedToLoad.clear();

		List<Repository> result = new LinkedList<>();
		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LOGGER.info("Cannot access file system in execution mode " + RapidMiner.getExecutionMode()
					+ ". Not loading repositories.");
			return result;
		}
		File file = getConfigFile();
		if (file.exists()) {
			LOGGER.config("Loading repositories from " + file);
			try {
				Document doc = XMLTools.createDocumentBuilder().parse(file);
				if (!doc.getDocumentElement().getTagName().equals(TAG_REPOSITORIES)) {
					LOGGER.warning("Broken repositories file. Root element must be <reposities>.");
					return result;
				}
				NodeList list = doc.getDocumentElement().getChildNodes();
				for (int i = 0; i < list.getLength(); i++) {
					if (list.item(i) instanceof Element) {
						Element element = (Element) list.item(i);
						String tagName = element.getTagName();
						if (TAG_LOCAL_REPOSITORY.equals(tagName)) {
							try {
								result.add(LocalRepository.fromXML(element));
							} catch (RepositoryException | XMLException e) {
								failedToLoad.add(element);
								LOGGER.log(Level.WARNING, "Cannot read local repository entry.", e);
							}
						} else {
							boolean foundFactory = false;
							for (CustomRepositoryFactory factory : CustomRepositoryRegistry.INSTANCE.getFactories()) {
								if (tagName.equals(factory.getXMLTag())) {
									try {
										result.add(factory.fromXML(element));
									} catch (NoClassDefFoundError | Exception e) {
										// this is needed to catch "NoClassDefFoundError"s when having a repository
										// from an extension that references some illegal class that is no longer available
										failedToLoad.add(element);
										LOGGER.log(Level.WARNING, "Cannot read custom repository entry.", e);
									}
									foundFactory = true;
									break;
								}
							}
							if (!foundFactory) {
								LOGGER.warning("Unknown tag: " + tagName);
								failedToLoad.add(element);
							}
						}
					}
				}
			} catch (RuntimeException | SAXException | IOException e) {
				LOGGER.log(Level.WARNING, "Cannot read repository configuration file '" + file + "': " + e, e);
			}
		}
		return result;
	}

	@Override
	public void save(List<Repository> repositories) {
		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LOGGER.config("Cannot access file system in execution mode " + RapidMiner.getExecutionMode()
					+ ". Not saving repositories.");
			return;
		}
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			LOGGER.log(Level.WARNING, "Cannot save repositories: " + e, e);
			return;
		}
		Element root = doc.createElement(TAG_REPOSITORIES);
		doc.appendChild(root);
		for (Repository repository : repositories) {
			if (repository.shouldSave()) {
				Element repositoryElement = repository.createXML(doc);
				if (repositoryElement != null) {
					root.appendChild(repositoryElement);
				}
			}
		}

		// Store failed element when saving repositories so they are not lost once an external error
		// (e.g. no write access) has been fixed
		for (Element failedElem : failedToLoad) {
			root.appendChild(doc.importNode(failedElem, true));
		}
		try {
			XMLTools.stream(doc, getConfigFile(), null);
		} catch (XMLException e) {
			LOGGER.log(Level.WARNING, "Cannot save repositories: " + e, e);
		}
	}
}
