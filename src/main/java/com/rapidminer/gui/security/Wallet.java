/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.io.Base64;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.GlobalAuthenticator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;


/**
 * The Wallet stores user credentials (username and passwords). It is used by the
 * {@link GlobalAuthenticator} and can be edited with the {@link PasswordManager}.
 * {@link UserCredential}s are stored per {@link URL}.
 *
 * Note: Though this class has a {@link #getInstance()} method, it is not a pure singleton. In fact,
 * the {@link PasswordManager} sets a new instance via {@link #setInstance(Wallet)} after editing.
 *
 * @author Miguel Buescher, Marco Boeck
 *
 */
public class Wallet {

	private static final String CACHE_FILE_NAME = "secrets.xml";
	private HashMap<String, UserCredential> wallet = new HashMap<String, UserCredential>();

	private static final String ID_PREFIX_URL_SEPERATOR = "___";
	public static final String ID_MARKETPLACE = "Marketplace";

	private static Wallet instance = new Wallet();

	static {
		instance.readCache();
	}

	/**
	 * Singleton access.
	 *
	 * @return
	 */
	public static Wallet getInstance() {
		return instance;
	}

	public static void setInstance(Wallet wallet) {
		instance = wallet;
	}

	/**
	 * Reads the wallet from the secrets.xml file in the user's home directory.
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
		readCache(doc);
	}

	/**
	 * Reads the wallet from a {@link Document} object.
	 *
	 * @param doc
	 *            the document which contains the secrets
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
				password = new String(Base64.decode(XMLTools.getTagContents(secretElem, "password"))).toCharArray();
			} catch (IOException e) {
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.gui.security.Wallet.reading_entry_in_secrets_file_error", e), e);
				continue;
			}
			usercredential = new UserCredential(url, user, password);
			if (id != null) {
				// new entries with an id
				wallet.put(buildKey(id, usercredential.getURL()), usercredential);
			} else {
				// for old entries which do not have an id
				wallet.put(usercredential.getURL(), usercredential);
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
		wallet.put(authentication.getURL(), authentication);
	}

	/**
	 * Adds the given credentials with the given ID. ID is necessary because there may be more
	 * credentials than one for the same URL.
	 *
	 * @param id
	 * @param authentication
	 * @throws IllegalArgumentException
	 *             if the key is <code>null</code>
	 */
	public void registerCredentials(String id, UserCredential authentication) throws IllegalArgumentException {
		if (id == null) {
			throw new IllegalArgumentException("id must not be null!");
		}
		wallet.put(buildKey(id, authentication.getURL()), authentication);

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
		return wallet.size();
	}

	/**
	 * Deep clone.
	 */
	@Override
	public Wallet clone() {
		Wallet clone = new Wallet();
		for (String key : this.getKeys()) {
			String[] urlAndMaybeID = key.split(ID_PREFIX_URL_SEPERATOR);
			if (urlAndMaybeID.length == 2) {
				// this is for new keys which have an ID prefix
				UserCredential entry = wallet.get(buildKey(urlAndMaybeID[0], urlAndMaybeID[1]));
				clone.registerCredentials(urlAndMaybeID[0], entry.clone());
			} else {
				// old keys which do not yet have the ID prefix
				UserCredential entry = wallet.get(key);
				clone.registerCredentials(entry.clone());
			}
		}
		return clone;
	}

	/**
	 * Returns a {@link List} of {@link String} keys in this {@link Wallet}.
	 *
	 * @return
	 */
	public LinkedList<String> getKeys() {
		Iterator<String> it = wallet.keySet().iterator();
		LinkedList<String> keyset = new LinkedList<String>();
		while (it.hasNext()) {
			keyset.add(it.next());
		}
		return keyset;
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
		return wallet.get(url);
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
		UserCredential credentials = wallet.get(buildKey(id, url));
		if (credentials == null) {
			// fallback for old entries which can supply a null key
			credentials = wallet.get(url);
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
		wallet.remove(url);
	}

	/**
	 * Removes the {@link UserCredential} for the given id and url {@link String}s.
	 *
	 * @param id
	 * @param url
	 */
	public void removeEntry(String id, String url) {
		wallet.remove(buildKey(id, url));
	}

	/**
	 * Creates a XML representation of the wallet.
	 *
	 * @return The XML document.
	 */
	public Document getWalletAsXML() {
		Document doc = XMLTools.createDocument();
		Element root = doc.createElement(CACHE_FILE_NAME);
		doc.appendChild(root);
		for (String key : getKeys()) {
			Element entryElem = doc.createElement("secret");
			root.appendChild(entryElem);
			String[] urlAndMaybeID = key.split(ID_PREFIX_URL_SEPERATOR);
			if (urlAndMaybeID.length == 2) {
				// this is for new keys which have an ID prefix
				XMLTools.setTagContents(entryElem, "id", urlAndMaybeID[0]);
				XMLTools.setTagContents(entryElem, "url", urlAndMaybeID[1]);
			} else {
				// old keys which do not yet have the ID prefix
				XMLTools.setTagContents(entryElem, "url", key);
			}
			XMLTools.setTagContents(entryElem, "user", wallet.get(key).getUsername());
			XMLTools.setTagContents(entryElem, "password",
					Base64.encodeBytes(new String(wallet.get(key).getPassword()).getBytes()));
		}
		return doc;
	}

	/**
	 * Saves the wallet to the secrets.xml file in the users home directory.
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
		if (urlAndMaybeID.length == 2) {
			return urlAndMaybeID[0];
		} else {
			return null;
		}
	}

}
