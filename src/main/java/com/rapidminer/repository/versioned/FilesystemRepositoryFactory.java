/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.versioned;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.repository.CustomRepositoryFactory;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.gui.RepositoryConfigurationPanel;
import com.rapidminer.repository.versioned.gui.FilesystemRepositoryConfigurationPanel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * Factory to create a filesystem Repository.
 *
 * @author Andreas Timm
 * @since 9.7
 */
public class FilesystemRepositoryFactory implements CustomRepositoryFactory {

	private static final String FILESYSTEM_REPOSITORY_TAG = "filesystemRepository";
	private static final String XML_ENCRYPTION_CONTEXT = "encryption-context";


	@Override
	public boolean enableRepositoryConfiguration() {
		return true;
	}

	@Override
	public RepositoryConfigurationPanel getRepositoryConfigurationPanel() {
		return new FilesystemRepositoryConfigurationPanel(true);
	}

	@Override
	public Repository fromXML(Element element) throws RepositoryException, XMLException {
		String path = XMLTools.getTagContents(element, "path", true);
		String alias = XMLTools.getTagContents(element, "alias", true);
		String encryptionContext;
		try {
			encryptionContext = XMLTools.getTagContents(element, XML_ENCRYPTION_CONTEXT, true);
		} catch (XMLException e) {
			// this is the case for repos that have been created before encryptionContext was introduced. It was the default encryption in that case, so do that
			encryptionContext = EncryptionProvider.DEFAULT_CONTEXT;
		}
		boolean editable = true;

		// in case XML contains duplicate, avoid adding it again (path cannot be null here, would have thrown above)
		checkConfiguration(Paths.get(path), alias);

		try {
			return new FilesystemRepositoryAdapter(alias, path, editable, encryptionContext);
		} catch (FileNotFoundException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public String getXMLTag() {
		return FILESYSTEM_REPOSITORY_TAG;
	}

	@Override
	public String getI18NKey() {
		return "new_filesystem_repository";
	}

	@Override
	public Class<? extends Repository> getRepositoryClass() {
		return FilesystemRepositoryAdapter.class;
	}

	/**
	 * Put the information about the repository into an XML structure, adding it to the given document.
	 * Path, alias and user of the versioned repository will be stored here.
	 *
	 * @param repositoryAdapter to be stored, making it accessible later
	 * @param doc               create {@link Element} using this {@link Document}
	 * @return the new {@link Element} with the necessary information to load the repository again
	 */
	public static Element toXml(FilesystemRepositoryAdapter repositoryAdapter, Document doc) {
		Element repositoryElement = doc.createElement(FILESYSTEM_REPOSITORY_TAG);

		Element file = doc.createElement("path");
		file.appendChild(doc.createTextNode(repositoryAdapter.getRoot().toAbsolutePath().toString()));
		repositoryElement.appendChild(file);

		Element name = doc.createElement("alias");
		name.appendChild(doc.createTextNode(repositoryAdapter.getName()));
		repositoryElement.appendChild(name);

		String encryptionContext = repositoryAdapter.getEncryptionContext();
		if (encryptionContext != null) {
			Element encryptionContextElement = doc.createElement(XML_ENCRYPTION_CONTEXT);
			encryptionContextElement.appendChild(doc.createTextNode(encryptionContext));
			repositoryElement.appendChild(encryptionContextElement);
		}

		return repositoryElement;
	}

	/**
	 * Throws a {@link RepositoryException} if the given configuration is invalid.
	 *
	 * @param path  the path where the repository should be located
	 * @param alias the name of the repository
	 * @throws RepositoryException if the check is failed
	 */
	public static void checkConfiguration(Path path, String alias) throws RepositoryException {
		// make sure that it's not possible to create multiple repositories in the same location or
		// with the same alias
		for (Repository repo : RepositoryManager.getInstance(null).getRepositories()) {
			if (repo instanceof FilesystemRepositoryAdapter) {
				if (((FilesystemRepositoryAdapter) repo).getRoot().equals(path)) {
					throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(),
							"repository.repository_creation_duplicate_location", repo.getName()));
				}
			}
			if (repo.getName().equals(alias)) {
				throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(),
						"repository.repository_creation_duplicate_alias"));
			}
		}
	}

	/**
	 * Creates a {@link FilesystemRepositoryAdapter} which is editable.
	 *
	 * @param alias             of the repository, must not be null or empty
	 * @param targetPath        of the base directory
	 * @param encryptionContext the encryption context key that is used by the {@link FilesystemRepositoryAdapter} for
	 *                          encrypting files in that repo. See {@link com.rapidminer.tools.encryption.EncryptionProvider}.
	 * @return the repository, never {@code null}
	 * @throws RepositoryException in case creation failed
	 */
	public static Repository createRepository(String alias, Path targetPath, String encryptionContext) throws RepositoryException {
		return createRepository(alias, targetPath, true, false, encryptionContext);
	}

	/**
	 * Creates a {@link FilesystemRepositoryAdapter}.
	 *
	 * @param alias             of the repository, must not be null or empty
	 * @param targetPath        of the base directory
	 * @param editable          if the repo should be editable or not
	 * @param isTransient       see {@link Repository#isTransient()}
	 * @param encryptionContext the encryption context key that is used by the {@link FilesystemRepositoryAdapter} for
	 *                          encrypting files in that repo. See {@link com.rapidminer.tools.encryption.EncryptionProvider}.
	 * @return the repository, never {@code null}
	 * @throws RepositoryException in case creation failed
	 */
	public static Repository createRepository(String alias, Path targetPath, boolean editable, boolean isTransient, String encryptionContext) throws RepositoryException {
		if (alias == null || alias.length() == 0) {
			alias = targetPath.getFileName().toString();
		}
		checkConfiguration(targetPath, alias);
		try {
			return new FilesystemRepositoryAdapter(alias, targetPath.toString(), editable, isTransient, encryptionContext);
		} catch (FileNotFoundException e) {
			throw new RepositoryException(e);
		}
	}

}
