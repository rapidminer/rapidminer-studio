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
package com.rapidminer.tools.documentation;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.XMLParserException;


/**
 * A resource bundle that maps operator names to {@link OperatorDocumentation} instances. Instances
 * of this class always return {@link OperatorDocumentation}s from their {@link #getObject(String)}
 * methods.
 * 
 * The XML structure of the documentation is as follows: For every operator there is a tag
 * 
 * <pre>
 *  &lt;operator&gt;
 *    &lt;synopsis&gt;SYNOPSIS&lt;/synopsis&gt;
 *    &lt;help&gt;LONG HELP TEXT&lt;/help&gt;
 *    &lt;example&gt;
 *      &lt;process&gt;XML process string&lt;/process&gt;
 *      &lt;comment&gt;COMMENT&lt;/comment&gt;
 *    &lt;/example&gt;
 *    &lt;example&gt;
 *       ...
 *    &lt;/example&gt;
 *  &lt;/operator&gt;
 * </pre>
 * 
 * @author Simon Fischer
 * 
 */
public class XMLOperatorDocBundle extends OperatorDocBundle {

	/**
	 * Control to load XML files. Code is largely stolen from the javadoc of {@link Control}.
	 * 
	 * @author Simon Fischer
	 * 
	 */
	private static class XMLControl extends Control {

		@Override
		public List<String> getFormats(String baseName) {
			if (baseName == null) {
				throw new NullPointerException("baseName is null.");
			}
			return Arrays.asList("xml");
		}

		@Override
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
				throws IllegalAccessException, InstantiationException, IOException {
			if ((baseName == null) || (locale == null) || (format == null) || (loader == null)) {
				throw new NullPointerException();
			}
			// LogService.getRoot().fine("Looking up operator documentation for "+baseName+", locale "+locale+".");
			LogService.getRoot().log(Level.FINE,
					"com.rapidminer.tools.documentation.XMLOperatorDocBundle.looking_up_operator_documentation",
					new Object[] { baseName, locale });
			if (format.equals("xml")) {
				String bundleName = toBundleName(baseName, locale);
				String resourceName = toResourceName(bundleName, format);
				URL url = loader.getResource(resourceName);
				if (url != null) {
					// LogService.getRoot().config("Loading operator documentation from "+url+".");
					LogService.getRoot().log(Level.CONFIG,
							"com.rapidminer.tools.documentation.XMLOperatorDocBundle.loading_operator_documentation", url);
					try {
						return new XMLOperatorDocBundle(url, resourceName);
					} catch (Exception e) {
						// LogService.getRoot().log(Level.WARNING,
						// "Exception creating OperatorDocBundle: "+e, e);
						LogService
								.getRoot()
								.log(Level.WARNING,
										I18N.getMessage(
												LogService.getRoot().getResourceBundle(),
												"com.rapidminer.tools.documentation.XMLOperatorDocBundle.exception_creating_operatordocbundle",
												e), e);

						return null;
					}
				}
			}
			return null;
		}
	}

	/**
	 * Constructs a new OperatorDocBundle
	 * 
	 * @param url
	 *            The URL from which we are reading.
	 * @param resourceName
	 *            The original resource name. This is the last part of the path of the URL and will
	 *            be used to locate the source file, when this bundle is saved.
	 * @throws IOException
	 */
	public XMLOperatorDocBundle(URL url, String resourceName) throws IOException {
		Document document;
		try {
			document = XMLTools.createDocumentBuilder().parse(url.openStream());
		} catch (SAXException e) {
			throw new IOException("Malformed XML operator help bundle: " + e, e);
		} catch (XMLParserException e) {
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.documentation.XMLOperatorDocBundle.creating_xml_parser_error", e), e);

			return;
		}
		NodeList helpElements = document.getDocumentElement().getElementsByTagName("operator");
		for (int i = 0; i < helpElements.getLength(); i++) {
			Element element = (Element) helpElements.item(i);
			OperatorDocumentation operatorDocumentation = new OperatorDocumentation(this, element);
			try {
				String operatorKey = XMLTools.getTagContents(element, "key", false);
				if (operatorKey == null) {
					operatorKey = XMLTools.getTagContents(element, "name", true);
					// LogService.getRoot().fine("Operator help is missing <key> tag. Using <name> as <key>: "+operatorKey);
					LogService.getRoot().log(Level.FINE,
							"com.rapidminer.tools.documentation.XMLOperatorDocBundle.missing_operator_help", operatorKey);
				}
				addOperatorDoc(operatorKey, operatorDocumentation);
			} catch (XMLException e) {
				// LogService.getRoot().log(Level.WARNING, "Malformed operoator documentation: "+e,
				// e);
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.tools.documentation.XMLOperatorDocBundle.malformed_operator_documentation",
								e), e);

			}
		}

		NodeList groupElements = document.getDocumentElement().getElementsByTagName("group");
		for (int i = 0; i < groupElements.getLength(); i++) {
			Element element = (Element) groupElements.item(i);
			GroupDocumentation doc = new GroupDocumentation(element);
			addGroupDoc(doc.getKey(), doc);
		}

		// LogService.getRoot().fine("Loaded documentation for "+ helpElements.getLength()
		// +" operators and " + groupElements.getLength() +
		// " groups.");
		LogService.getRoot().log(Level.FINE, "com.rapidminer.tools.documentation.XMLOperatorDocBundle.loaded_documentation",
				new Object[] { helpElements.getLength(), groupElements.getLength() });
	}

	/** Loads the default "OperatorDoc.xml" file from the given resource base name. */
	public static OperatorDocBundle load(ClassLoader classLoader, String resource) {
		return (OperatorDocBundle) ResourceBundle.getBundle(resource, Locale.getDefault(), classLoader, new XMLControl());
	}
}
