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
package com.rapidminer.tools.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;


/**
 * This is a {@link NamespaceContext} to use with the Java XML API that is based upon a map. That
 * means, that each uri can only have one prefix. It is able to define a default namespace.
 * 
 * @author Sebastian Land
 */
public final class MapBasedNamespaceContext implements NamespaceContext {

	private final Map<String, String> idNamespacesMap;
	private final Map<String, String> namespaceIdsMap;
	private String defaultNamespaceURI = null;

	public MapBasedNamespaceContext(Map<String, String> idNamespacesMap) {
		this(idNamespacesMap, null);
	}

	/**
	 * This creates a {@link NamespaceContext} with the given map. The map maps from ids to the
	 * namespaces' URIs. A default namespace can be used.
	 * 
	 * @param idNamespaceURIsMap
	 * @param defaultNamespaceURI
	 */
	public MapBasedNamespaceContext(Map<String, String> idNamespaceURIsMap, String defaultNamespaceURI) {
		this.defaultNamespaceURI = defaultNamespaceURI;
		this.idNamespacesMap = idNamespaceURIsMap;
		namespaceIdsMap = new HashMap<String, String>();
		for (Entry<String, String> entry : idNamespaceURIsMap.entrySet()) {
			namespaceIdsMap.put(entry.getValue(), entry.getKey());
		}
	}

	@Override
	public String getNamespaceURI(String prefix) {
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			if (defaultNamespaceURI == null) {
				return XMLConstants.NULL_NS_URI;
			} else {
				return defaultNamespaceURI;
			}
		} else if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
			return XMLConstants.XML_NS_URI;
		} else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		} else if (prefix == null) {
			throw new IllegalArgumentException("Null prefix not allowed");
		} else {
			String uri = idNamespacesMap.get(prefix);
			if (uri == null) {
				return XMLConstants.NULL_NS_URI;
			} else {
				return uri;
			}
		}
	}

	@Override
	public String getPrefix(String uri) {
		if (defaultNamespaceURI != null && defaultNamespaceURI.equals(uri)) {
			return XMLConstants.DEFAULT_NS_PREFIX;
		} else if (uri == null) {
			throw new IllegalArgumentException("Null not allowed as prefix");
		} else if (uri.equals(XMLConstants.XML_NS_URI)) {
			return XMLConstants.XML_NS_PREFIX;
		} else if (uri.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
			return XMLConstants.XMLNS_ATTRIBUTE;
		} else {
			return namespaceIdsMap.get(uri);
		}
	}

	@Override
	public Iterator<String> getPrefixes(String uri) {
		return Collections.singletonList(getPrefix(uri)).iterator();
	}
}
