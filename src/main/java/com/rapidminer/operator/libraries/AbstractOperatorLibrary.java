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
package com.rapidminer.operator.libraries;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.documentation.OperatorDocBundle;
import com.rapidminer.tools.plugin.Plugin;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * This is an abstract superclass for all {@link OperatorLibrary}s. It provides common functionality
 * for handling version numbers, name, registering and deregistering Operators and so on.
 * 
 * @author Sebastian Land
 */
public abstract class AbstractOperatorLibrary implements OperatorLibrary {

	public static final String LIBRARY_MIME_TYPE = "application/vnd.rapidminer.operator-library";

	private static final String ELEMENT_LIBRARY = "OperatorLibrary";
	private static final String ELEMENT_DOCUMENTATION = "Documentation";
	private static final String ELEMENT_SYNOPSIS = "Synopsis";

	private static final String ATTRIBUTE_NAMESPACE = "namespace";
	private static final String ATTRIBUTE_NAMESPACE_ID = "namespace-id";
	private static final String ATTRIBUTE_NAME = "name";
	private static final String ATTRIBUTE_VERSION = "version";
	private static final String ATTRIBUTE_CLASS = "class";

	private static final long serialVersionUID = 1L;

	private String repositoryLocation;

	private String name;

	private String namespaceIdentifier;
	private String namespaceName;

	private VersionNumber version;
	/**
	 * A short synopsis of the functionality of this operator library
	 */
	private String synopsis;
	/**
	 * A piece of HTML that describes in detail the inner workings of this operator library.
	 */
	private String documentation;

	private OperatorLibraryDocBundle docBundle = new OperatorLibraryDocBundle(this);

	public AbstractOperatorLibrary(String repositoryLocation, Element element) {
		this.repositoryLocation = repositoryLocation;
		readXML(element);
	}

	public AbstractOperatorLibrary(String repositoryLocation, String namespaceName, String description, VersionNumber version) {
		this.repositoryLocation = repositoryLocation;
		this.namespaceName = namespaceName;
		this.namespaceIdentifier = "rmol_" + namespaceName + "(" + RandomGenerator.getGlobalRandomGenerator().nextString(8)
				+ ")";

		this.synopsis = description;
		this.version = version;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getNamespace() {
		return namespaceIdentifier;
	}

	@Override
	public String getNamespaceName() {
		return namespaceName;
	}

	@Override
	public String getSynopsis() {
		return synopsis;
	}

	@Override
	public String getDocumentation() {
		return documentation;
	}

	@Override
	public VersionNumber getVersion() {
		return version;
	}

	@Override
	public OperatorDocBundle getDocumentationBundle() {
		return docBundle;
	}

	@Override
	public void setSynopsis(String newSyopsis) {
		this.synopsis = newSyopsis;
	}

	@Override
	public void setDocumentation(String newDocumentation) {
		this.documentation = newDocumentation;
	}

	@Override
	public void setVersion(VersionNumber newVersion) {
		this.version = newVersion;
	}

	@Override
	public void setName(String newName) {
		this.name = newName;
	}

	@Override
	public void registerOperators() throws OperatorCreationException {
		Set<String> keys = OperatorService.getOperatorKeys();
		for (OperatorDescription description : getOperatorDescriptions()) {
			if (!keys.contains(description.getKey())) {
				OperatorService.registerOperator(description, docBundle);
			}
		}
	}

	@Override
	public void unregisterOperators() {
		Set<String> keys = OperatorService.getOperatorKeys();
		for (OperatorDescription description : getOperatorDescriptions()) {
			if (keys.contains(description.getKey())) {
				OperatorService.unregisterOperator(description);
			}
		}
	}

	@Override
	public String getRepositoryLocation() {
		return repositoryLocation;
	}

	@Override
	public void save(BlobEntry entry) {
		try {
			save(entry.openOutputStream(LIBRARY_MIME_TYPE));
		} catch (RepositoryException e) {
			SwingTools.showSimpleErrorMessage("cannot_access_repository", e);
		}
	}

	@Override
	public void save(OutputStream out) {
		DocumentBuilder builder;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.newDocument();

			Element rootElement = document.createElement(ELEMENT_LIBRARY);
			document.appendChild(rootElement);

			rootElement.setAttribute(ATTRIBUTE_CLASS, this.getClass().getCanonicalName());
			rootElement.setAttribute(ATTRIBUTE_NAME, name);
			rootElement.setAttribute(ATTRIBUTE_NAMESPACE_ID, namespaceIdentifier);
			rootElement.setAttribute(ATTRIBUTE_NAMESPACE, namespaceName);
			rootElement.setAttribute(ATTRIBUTE_VERSION, version.getLongVersion());

			XMLTools.addTag(rootElement, ELEMENT_SYNOPSIS, synopsis);
			XMLTools.addTag(rootElement, ELEMENT_DOCUMENTATION, documentation);

			writeXML(rootElement);

			XMLTools.stream(document, out, null);
		} catch (ParserConfigurationException e) {
			// TODO
			e.printStackTrace();
		} catch (XMLException e) {
			e.printStackTrace();
			// TODO
		}
	}

	/**
	 * This method must be implemented by subclasses in order to export their settings to the given
	 * element. They must not use the tags as named by {@link #ELEMENT_SYNOPSIS} or
	 * {@link #ELEMENT_DOCUMENTATION}.
	 */
	protected abstract void writeXML(Element element);

	/**
	 * This will load all definitions from the given XML element as written by
	 * {@link #writeXML(Element)}
	 */
	private void readXML(Element element) {
		this.namespaceIdentifier = element.getAttribute(ATTRIBUTE_NAMESPACE_ID);
		this.namespaceName = element.getAttribute(ATTRIBUTE_NAMESPACE);
		this.name = element.getAttribute(ATTRIBUTE_NAME);
		this.version = new VersionNumber(element.getAttribute(ATTRIBUTE_VERSION));

		this.synopsis = XMLTools.getTagContents(element, ELEMENT_SYNOPSIS);
		this.documentation = XMLTools.getTagContents(element, ELEMENT_DOCUMENTATION);
	}

	/**
	 * This method will load a library that has been stored into the given entry.
	 * 
	 */
	public static OperatorLibrary loadLibrary(BlobEntry entry) throws Exception {
		Document document = XMLTools.parse(entry.openInputStream());
		String className = document.getDocumentElement().getAttribute(ATTRIBUTE_CLASS);

		Constructor<?> constructor = Class.forName(className, false, Plugin.getMajorClassLoader()).getConstructor(
				String.class, Element.class);
		return (OperatorLibrary) constructor.newInstance(entry.getLocation().getAbsoluteLocation(),
				document.getDocumentElement());

	}
}
