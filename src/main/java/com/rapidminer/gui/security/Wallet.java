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
package com.rapidminer.gui.security;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.io.Base64;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryManagerListener;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.GlobalAuthenticator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.cipher.CipherException;
import com.rapidminer.tools.cipher.CipherTools;


/**
 * The Wallet stores user credentials (username and passwords). It is used by the
 * {@link GlobalAuthenticator} and can be edited with the {@link PasswordManager}.
 * {@link UserCredential}s are stored per {@link URL}.
 *
 * Note: Though this class has a {@link #getInstance()} method, it is not a pure singleton. In fact,
 * the {@link PasswordManager} sets a new instance via {@link #setInstance(Wallet)} after editing.
 *
 * @author Miguel Buescher, Marco Boeck
 */
@SuppressWarnings("deprecation")
public class Wallet {

	private static final String CACHE_FILE_NAME = "secrets.xml";
	public static final int URL_ID_SPLIT_COUNT = 2;
	private Map<String, UserCredential> walletContent = new HashMap<>();

	/**
	 * Version of the secrets xml that is necessary, triggers migration by saving the real secrets.xml is below this version
	 */
	private static final VersionNumber SECRET_VERSION_NR = new VersionNumber(7, 5, 4);

	private static final String VERSION_ATTRIBUTE = "version";
	private static final String ID_PREFIX_URL_SEPERATOR = "___";
	public static final String ID_MARKETPLACE = "Marketplace";

	private static Wallet instance = new Wallet();

	private static final boolean ACTIVE_MIGRATION_DEFAULT_VALUE = false;
	private boolean activeMigration = ACTIVE_MIGRATION_DEFAULT_VALUE;

	static {
		instance.readCache();
	}

	/**
	 * Singleton access.
	 *
	 * @return
	 */
	public static synchronized Wallet getInstance() {
		return instance;
	}

	public static synchronized void setInstance(Wallet wallet) {
		instance = wallet;
	}

	/**
	 * Reads the walletContent from the secrets.xml file in the user's home directory.
	 */
	public void readCache() {
		final File userConfigFile = FileSystemService.getUserConfigFile(CACHE_FILE_NAME);
		if (!userConfigFile.exists()) {
			return;
		}
		LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.security.Wallet.reading_secrets_file");

		Document doc;
		try {
			doc = XMLTools.parse(userConfigFile);
		} catch (Exception e) {
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.security.Wallet.reading_secrets_file_error", e), e);
			return;
		}
		VersionNumber secretXmlVersion = getVersionNumber(doc);
		activeMigration = isMigrationRequired(secretXmlVersion);
		try {
			readCache(doc);
			if (isMigrationRequired(secretXmlVersion)) {
				saveCache();
			}
		} finally {
			activeMigration = ACTIVE_MIGRATION_DEFAULT_VALUE;
		}
	}

	private VersionNumber getVersionNumber(Document doc) {
		VersionNumber secretXmlVersion = null;
		try {
			NodeList rootNode = doc.getElementsByTagName(CACHE_FILE_NAME);
			NamedNodeMap attributes = rootNode.item(0).getAttributes();
			Node versionValue = attributes.getNamedItem(VERSION_ATTRIBUTE);
			secretXmlVersion = new VersionNumber(versionValue.getNodeValue());
		} catch (Exception e) {
			// attribute missing
		}
		return secretXmlVersion;
	}

	private boolean isMigrationRequired(VersionNumber secretXmlVersion) {
		return secretXmlVersion == null || !secretXmlVersion.equals(SECRET_VERSION_NR) && secretXmlVersion.isAtMost(SECRET_VERSION_NR);
	}

	/**
	 * Reads the wallet from a {@link Document} object.
	 *
	 * @param doc
	 * 		the document which contains the secrets
	 */
	public void readCache(Document doc) {
		NodeList secretElems = doc.getDocumentElement().getElementsByTagName("secret");
		UserCredential usercredential;
		for (int i = 0; i < secretElems.getLength(); i++) {
			Element secretElem = (Element) secretElems.item(i);
			String id = XMLTools.getTagContents(secretElem, "id");
			String url = XMLTools.getTagContents(secretElem, "url");
			String user = XMLTools.getTagContents(secretElem, "user");
			char[] password;
			try {
				String passwordTagContent = XMLTools.getTagContents(secretElem, "password");
				password = getDecodedPassword(passwordTagContent).toCharArray();
			} catch (IOException | CipherException e) {
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.gui.security.Wallet.reading_entry_in_secrets_file_error", id, e), e);
				continue;
			}
			usercredential = new UserCredential(url, user, password);
			if (id != null) {
				// new entries with an id
				walletContent.put(buildKey(id, usercredential.getURL()), usercredential);
			} else {
				// for old entries which do not have an id
				walletContent.put(usercredential.getURL(), usercredential);
			}
		}
	}

	/**
	 * Adds the given credentials.
	 *
	 * @deprecated use {@link #registerCredentials(String, UserCredential)} instead.
	 * @param authentication
	 */
	@Deprecated
	public void registerCredentials(UserCredential authentication) {
		walletContent.put(authentication.getURL(), authentication);
	}

	/**
	 * Adds the given credentials with the given ID. ID is necessary because there may be more
	 * credentials than one for the same URL.
	 *
	 * @param id
	 * @param authentication
	 * @throws IllegalArgumentException
	 * 		if the key is <code>null</code>
	 */
	public void registerCredentials(String id, UserCredential authentication) {
		if (id == null) {
			throw new IllegalArgumentException("id must not be null!");
		}
		walletContent.put(buildKey(id, authentication.getURL()), authentication);

		// we need to consider possibility of an old entry for the same URL, remove it here because
		// it got replaced
		if (getEntry(authentication.getURL()) != null) {
			removeEntry(authentication.getURL());
		}
	}

	/**
	 * Returns the number of entries in the {@link Wallet}.
	 *
	 * @return
	 */
	public int size() {
		return walletContent.size();
	}

	/**
	 * Default empty constructor.
	 */
	public Wallet() {
		super();
		RepositoryManager.getInstance(null).addRepositoryManagerListener(new RepositoryManagerListener() {
			@Override
			public void repositoryWasAdded(Repository repository) {
				//Not needed since this only cares about deletions.
			}

			@Override
			public void repositoryWasRemoved(Repository repository) {
				if (repository instanceof RemoteRepository) {
					String url = ((RemoteRepository) repository).getBaseUrl().toString();
					String id = ((RemoteRepository) repository).getAlias();
					Wallet.getInstance().removeEntry(id, url);
					Wallet.getInstance().saveCache();
				}
			}
		});
	}

	/**
	 * Deep clone constructor.
	 */
	public Wallet(Wallet clonethis) {
		for (String key : clonethis.getKeys()) {
			String[] urlAndMaybeID = key.split(ID_PREFIX_URL_SEPERATOR);
			if (urlAndMaybeID.length == URL_ID_SPLIT_COUNT) {
				// this is for new keys which have an ID prefix
				UserCredential entry = clonethis.getEntry(buildKey(urlAndMaybeID[0], urlAndMaybeID[1]));
				registerCredentials(urlAndMaybeID[0], new UserCredential(entry));
			} else {
				// old keys which do not yet have the ID prefix
				UserCredential entry = clonethis.getEntry(key);
				registerCredentials(new UserCredential(entry));
			}
		}
	}

	/**
	 * Returns a {@link List} of {@link String} keys in this {@link Wallet}.
	 *
	 * @return
	 */
	public List<String> getKeys() {
		return new LinkedList<>(walletContent.keySet());
	}

	/**
	 * Returns the {@link UserCredential} for the given url {@link String} or <code>null</code> if
	 * there is no key matching this url.
	 *
	 * @deprecated use {@link #getEntry(String, String)} instead.
	 * @param url
	 * @return
	 */
	@Deprecated
	public UserCredential getEntry(String url) {
		return walletContent.get(url);
	}

	/**
	 * Returns the {@link UserCredential} for the given id and url {@link String}s. If there is no
	 * key matching the given id and url tries to return the {@link UserCredential} for the given
	 * url (fallback for old entries). If both fail, returns <code>null</code>.
	 *
	 * @param id
	 * @param url
	 * @return
	 */
	public UserCredential getEntry(String id, String url) {
		UserCredential credentials = walletContent.get(buildKey(id, url));
		if (credentials == null) {
			// fallback for old entries which can supply a null key
			credentials = walletContent.get(url);
		}

		return credentials;
	}

	/**
	 * Removes the {@link UserCredential} for the given url {@link String}.
	 *
	 * @deprecated use {@link #removeEntry(String, String)} instead.
	 * @param url
	 */
	@Deprecated
	public void removeEntry(String url) {
		walletContent.remove(url);
	}

	/**
	 * Removes the {@link UserCredential} for the given id and url {@link String}s.
	 *
	 * @param id
	 * @param url
	 */
	public void removeEntry(String id, String url) {
		walletContent.remove(buildKey(id, url));
	}

	/**
	 * Creates a XML representation of the walletContent.
	 *
	 * @return The XML document.
	 */
	public Document getWalletAsXML() {
		Document doc = XMLTools.createDocument();
		Element root = doc.createElement(CACHE_FILE_NAME);
		root.setAttribute(VERSION_ATTRIBUTE, SECRET_VERSION_NR.getLongVersion());
		doc.appendChild(root);
		for (String key : getKeys()) {
			try {
				Element entryElem = doc.createElement("secret");
				String[] urlAndMaybeID = key.split(ID_PREFIX_URL_SEPERATOR);
				if (urlAndMaybeID.length == URL_ID_SPLIT_COUNT) {
					// this is for new keys which have an ID prefix
					XMLTools.setTagContents(entryElem, "id", urlAndMaybeID[0]);
					XMLTools.setTagContents(entryElem, "url", urlAndMaybeID[1]);
				} else {
					// old keys which do not yet have the ID prefix
					XMLTools.setTagContents(entryElem, "url", key);
				}
				XMLTools.setTagContents(entryElem, "user", walletContent.get(key).getUsername());
				XMLTools.setTagContents(entryElem, "password", getEncodedPassword(new String(walletContent.get(key).getPassword())));
				root.appendChild(entryElem);
			} catch (CipherException e) {
				LogService.getRoot().log(
						Level.INFO,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.gui.security.Wallet.store_entry_failed", key, e), e);
			}
		}
		return doc;
	}

	private String getEncodedPassword(String string) throws CipherException {
		return CipherTools.encrypt(string);
	}

	private String getDecodedPassword(String string) throws CipherException, IOException {
		if (CipherTools.isKeyAvailable()) {
			try {
				return CipherTools.decrypt(string);
			} catch (CipherException e) {
				// old password storage detected, use obsolete password decoding mechanism for migration only
				if (!activeMigration) {
					// not accepting old decoding in a up-to-date secrets file
					throw e;
				}
			}
		}
		return new String(Base64.decode(string));
	}

	/**
	 * Saves the walletContent to the secrets.xml file in the users home directory.
	 */
	public void saveCache() {
		LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.security.Wallet.saving_secrets_file");
		Document doc = getWalletAsXML();
		File file = FileSystemService.getUserConfigFile(CACHE_FILE_NAME);
		try {
			XMLTools.stream(doc, file, null);
		} catch (XMLException e) {
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.security.Wallet.saving_secrets_file_error", e), e);
		}
	}

	/**
	 * Builds the {@link String} from an ID and an URL {@link String} which is used to
	 * store/retrieve entries in the {@link Wallet}.
	 *
	 * @param id
	 * @param url
	 * @return
	 */
	private String buildKey(String id, String url) {
		return id + ID_PREFIX_URL_SEPERATOR + url;
	}

	/**
	 * Returns the id {@link String} contained in the key or <code>null</code> if there is no id.
	 *
	 * @param key
	 * @return
	 */
	public String extractIdFromKey(String key) {
		String[] urlAndMaybeID = key.split(ID_PREFIX_URL_SEPERATOR);
		if (urlAndMaybeID.length == URL_ID_SPLIT_COUNT) {
			return urlAndMaybeID[0];
		} else {
			return null;
		}
	}
}
