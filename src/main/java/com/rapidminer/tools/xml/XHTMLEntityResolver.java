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

import com.rapidminer.tools.container.Pair;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * This entity resolver uses a cached dtds from the resources instead of requesting each dtds from
 * the internet.
 * 
 * @author Sebastian Land
 */
public class XHTMLEntityResolver implements EntityResolver {

	private static final List<Pair<Pattern, String>> systemIdToResources = new LinkedList<Pair<Pattern, String>>();
	private static final Map<String, String> publicIdToResources = new HashMap<String, String>();

	static {
		systemIdToResources.add(new Pair<Pattern, String>(Pattern.compile("http://www\\.w3\\.org/TR/html4/(.*)"),
				"/com/rapidminer/resources/dtds/$1"));
		systemIdToResources.add(new Pair<Pattern, String>(Pattern.compile("http://www\\.w3\\.org/TR/xhtml1/DTD/(.*)"),
				"/com/rapidminer/resources/dtds/$1"));
		systemIdToResources.add(new Pair<Pattern, String>(Pattern.compile("http://www\\.w3\\.org/TR/xhtml11/DTD/(.*)"),
				"/com/rapidminer/resources/dtds/$1"));

		publicIdToResources.put("-//W3C//DTD HTML 4.01//EN", "/com/rapidminer/resources/dtds/strict.dtd");
		publicIdToResources.put("-//W3C//DTD HTML 4.01 Transitional//EN", "/com/rapidminer/resources/dtds/loose.dtd");
		publicIdToResources.put("-//W3C//DTD HTML 4.01 Frameset//EN", "/com/rapidminer/resources/dtds/frameset.dtd");
		publicIdToResources.put("-//W3C//DTD XHTML 1.0 Strict//EN", "/com/rapidminer/resources/dtds/xhtml1-strict.dtd");
		publicIdToResources.put("-//W3C//DTD XHTML 1.0 Transitional//EN",
				"/com/rapidminer/resources/dtds/xhtml1-transitional.dtd");
		publicIdToResources.put("-//W3C//DTD XHTML 1.0 Frameset//EN", "/com/rapidminer/resources/dtds/xhtml1-frameset.dtd");
		publicIdToResources.put("-//W3C//DTD XHTML 1.1//EN", "/com/rapidminer/resources/dtds/xhtml11.dtd");

	}
	private EntityResolver fallback;

	public XHTMLEntityResolver(EntityResolver fallback) {
		this.fallback = fallback;
	}

	public XHTMLEntityResolver() {
		this.fallback = null;
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		if (systemId != null) {
			for (Pair<Pattern, String> pair : systemIdToResources) {
				Matcher matcher = pair.getFirst().matcher(systemId);
				if (matcher.matches()) {
					URL resource = getClass().getResource(matcher.replaceAll(pair.getSecond()));
					if (resource != null) {
						InputSource inputSource = new InputSource(resource.toExternalForm());
						inputSource.setPublicId(publicId);
						return inputSource;
					}
				}
			}
		} else {
			String resourceLocation = publicIdToResources.get(publicId);
			if (resourceLocation != null) {
				URL resource = getClass().getResource(resourceLocation);
				if (resource != null) {
					InputSource inputSource = new InputSource(resource.toExternalForm());
					inputSource.setPublicId(publicId);
					return inputSource;
				}
			}
		}

		if (fallback != null) {
			return fallback.resolveEntity(publicId, systemId);
		} else {
			return null;
		}
	}
}
