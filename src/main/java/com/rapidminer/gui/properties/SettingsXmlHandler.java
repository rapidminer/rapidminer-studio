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
package com.rapidminer.gui.properties;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.rapidminer.gui.properties.SettingsItem.Type;


/**
 * XML SAX parser handler for the hierarchy of settings items. The parsing process is started by
 * {@link SettingsXmlHandler#parse(URI)}.
 *
 * @author Adrian Wilke
 */
public class SettingsXmlHandler extends DefaultHandler {

	/** XML file with settings structure */
	public static final String SETTINGS_XML_FILE = "settings.xml";

	/** Prefix of subgroups in settings XML */
	public final static String SUBGROUP_PREFIX = "rapidminer.preferences.subgroup.";

	/** Maximum number of nested group tags */
	private final static int MAX_GROUP_LEVEL = 2;

	/* Used XML elements */
	private final static String TAG_ROOT = "settings";
	private final static String TAG_GROUP = "group";
	private final static String TAG_PROPERTY = "property";
	private final static String ATTRIBUTE_KEY = "key";

	/* Parsing result: Maps ID to object */
	private Map<String, SettingsItem> settingsItems = new LinkedHashMap<>();

	/* Parser state */
	private List<SettingsItem> itemStack = new LinkedList<>();

	/**
	 * Parses Settings XML file.
	 *
	 * @param xmlFileUri
	 *            The URI of the related XML settings file.
	 *
	 * @return Map containing item IDs which identify the related {@link SettingsItem}s.
	 *
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public Map<String, SettingsItem> parse(URI xmlFileUri) throws ParserConfigurationException, SAXException, IOException,
			URISyntaxException {
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		saxParser.parse(xmlFileUri.toString(), this);
		return settingsItems;
	}

	/** XML Parser: Opening XML tag. */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		Map<String, String> attributesMap = getAttributesMap(attributes);

		if (qName.equals(TAG_ROOT)) {
			if (!itemStack.isEmpty()) {
				throw new SAXException("Invalid use of element " + TAG_ROOT);
			}

		} else if (qName.equals(TAG_GROUP) && itemStack.isEmpty()) {
			String propertyKey = getAttribute(attributesMap, ATTRIBUTE_KEY, true);
			itemStack.add(new SettingsItem(propertyKey, getLastStackItem(), Type.GROUP));

		} else if (qName.equals(TAG_GROUP) && !itemStack.isEmpty() && itemStack.size() < MAX_GROUP_LEVEL) {
			if (!getLastStackItem().getType().equals(SettingsItem.Type.GROUP)) {
				throw new SAXException("Invalid use of element " + TAG_GROUP + ". Parent element is not " + TAG_GROUP);
			}
			String propertyKey = getAttribute(attributesMap, ATTRIBUTE_KEY, true);
			if (!propertyKey.startsWith(SUBGROUP_PREFIX)) {
				throw new SAXException("Invalid value for attribute '" + ATTRIBUTE_KEY + "' in element '" + TAG_GROUP
						+ "': " + propertyKey);
			}
			itemStack.add(new SettingsItem(propertyKey, getLastStackItem(), Type.SUB_GROUP));

		} else if (qName.equals(TAG_GROUP)) {
			String propertyKey = getAttribute(attributesMap, ATTRIBUTE_KEY, true);
			throw new SAXException("Invalid use of element '" + TAG_GROUP + "' (key: " + propertyKey + "')");

		} else if (qName.equals(TAG_PROPERTY)) {
			if (itemStack.isEmpty()) {
				throw new SAXException("Invalid use of element " + TAG_PROPERTY);
			} else if (!getLastStackItem().getType().equals(SettingsItem.Type.GROUP)
					&& !getLastStackItem().getType().equals(SettingsItem.Type.SUB_GROUP)) {
				throw new SAXException("Invalid use of element " + TAG_PROPERTY + ". Parent element is not " + TAG_GROUP);
			}
			String propertyKey = getAttribute(attributesMap, ATTRIBUTE_KEY, true);
			itemStack.add(new SettingsItem(propertyKey, getLastStackItem(), Type.PARAMETER));

		} else {
			throw new SAXException("Unknown tag or usage: " + qName);
		}
	}

	/** XML Parser: Closing XML tag. */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		if (qName.equals(TAG_ROOT)) {
			// Just maintains a valid XML structure

		} else if (qName.equals(TAG_GROUP) || qName.equals(TAG_PROPERTY)) {

			// Update stack
			SettingsItem item = itemStack.remove(itemStack.size() - 1);

			// Add to results
			settingsItems.put(item.getKey(), item);

		} else {
			throw new SAXException("Unknown tag or usage: " + localName);
		}
	}

	/**
	 * Gets the last parsed settings item.
	 *
	 * @return Parent settings item, if exists. Or null, if no parent settings item exists.
	 */
	private SettingsItem getLastStackItem() {
		if (itemStack.isEmpty()) {
			return null;
		} else {
			return itemStack.get(itemStack.size() - 1);
		}
	}

	/**
	 * Gets the attribute of the specified key.
	 *
	 * @return Attribute, if key is in map. Or null, if key is not in map.
	 * @throws SAXException
	 *             if key not in map and isNecessary is set
	 */
	private String getAttribute(Map<String, String> attributes, String key, boolean isNecessary) throws SAXException {
		if (attributes.containsKey(key)) {
			return attributes.get(key);
		} else if (isNecessary) {
			throw new SAXException("Attribute " + key + " not found.");
		} else {
			return null;
		}

	}

	/**
	 * Returns a map representation of the specified attributes.
	 */
	private Map<String, String> getAttributesMap(Attributes attributes) {
		Map<String, String> map = new TreeMap<>();
		for (int i = 0; i < attributes.getLength(); i++) {
			map.put(attributes.getLocalName(i), attributes.getValue(i));
		}
		return map;
	}

}
