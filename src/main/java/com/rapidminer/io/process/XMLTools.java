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
package com.rapidminer.io.process;

import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * This class offers several convenience methods for treating XML documents-
 * 
 * @author Sebastian Land, Simon Fischer
 */
public class XMLTools {

	private static final Map<URI, Validator> VALIDATORS = new HashMap<URI, Validator>();

	private final static DocumentBuilderFactory BUILDER_FACTORY;

	public static final String SCHEMA_URL_PROCESS = "http://www.rapidminer.com/xml/schema/RapidMinerProcess";

	static {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		BUILDER_FACTORY = domFactory;
	}

	/**
	 * Creates a new {@link DocumentBuilder} instance.
	 * 
	 * Needed because DocumentBuilder is not thread-safe and crashes when different threads try to
	 * parse at the same time.
	 * 
	 * @return
	 * @throws IOException
	 *             if it fails to create a {@link DocumentBuilder}
	 */
	private static DocumentBuilder createDocumentBuilder() throws IOException {
		try {
			synchronized (BUILDER_FACTORY) {
				return BUILDER_FACTORY.newDocumentBuilder();
			}
		} catch (ParserConfigurationException e) {
			LogService.getRoot().log(Level.WARNING, "Unable to create document builder", e);
			throw new IOException(e);
		}
	}

	private static Validator getValidator(URI schemaURI) throws XMLException {
		if (schemaURI == null) {
			throw new NullPointerException("SchemaURL is null!");
		}
		synchronized (VALIDATORS) {
			if (VALIDATORS.containsKey(schemaURI)) {
				return VALIDATORS.get(schemaURI);
			} else {
				SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
				Validator validator;
				try {
					validator = factory.newSchema(schemaURI.toURL()).newValidator();
				} catch (SAXException e) {
					throw new XMLException("Cannot parse XML schema: " + e.getMessage(), e);
				} catch (MalformedURLException e) {
					throw new XMLException("Cannot parse XML schema: " + e.getMessage(), e);
				}
				VALIDATORS.put(schemaURI, validator);
				return validator;
			}
		}
	}

	/**
	 * This method should not be called since it is slower than
	 * {@link #parseAndValidate(InputStream, URI, String)}
	 */
	public static Document parseAndValidate(InputStream in, URL schemaURL, String sourceName) throws XMLException,
			IOException {
		try {
			return parseAndValidate(in, new URI(schemaURL.toString()), sourceName);
		} catch (URISyntaxException e) {
			throw new XMLException("Could not resolve URL.", e);
		}
	}

	/**
	 * The schema URL might be given as URI for performance reasons.
	 */
	public static Document parseAndValidate(InputStream in, URI schemaURL, String sourceName) throws XMLException,
			IOException {
		XMLErrorHandler errorHandler = new XMLErrorHandler(sourceName);

		Document doc;
		try {
			doc = createDocumentBuilder().parse(in);
		} catch (SAXException e) {
			throw new XMLException(errorHandler.toString(), e);
		}

		Source source = new DOMSource(doc);
		DOMResult result = new DOMResult();
		Validator validator = getValidator(schemaURL);
		validator.setErrorHandler(errorHandler);
		try {
			validator.validate(source, result);
		} catch (SAXException e) {
			throw new XMLException(errorHandler.toString(), e);
		}
		if (errorHandler.hasErrors()) {
			throw new XMLException(errorHandler.toString());
		}
		return (Document) result.getNode();
	}

	public static Document parse(String string) throws SAXException, IOException {
		return createDocumentBuilder().parse(new ByteArrayInputStream(string.getBytes(Charset.forName("UTF-8"))));
		// new ReaderInputStream(new StringReader(string)));
	}

	public static Document parse(InputStream in) throws SAXException, IOException {
		return createDocumentBuilder().parse(in);
	}

	public static Document parse(File file) throws SAXException, IOException {
		return createDocumentBuilder().parse(file);
	}

	public static String toString(Document document) throws XMLException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		Charset utf8 = Charset.forName("UTF-8");
		stream(document, buf, utf8);
		return new String(buf.toByteArray(), utf8);
	}

	/**
	 * @param document
	 * @param encoding
	 * @return
	 * @throws XMLException
	 * @deprecated use {@link #toString(Document)} instead
	 */
	@Deprecated
	public static String toString(Document document, Charset encoding) throws XMLException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		stream(document, buf, encoding);
		return new String(buf.toByteArray(), encoding);
	}

	public static void stream(Document document, File file, Charset encoding) throws XMLException {

		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			stream(document, out, encoding);
		} catch (IOException e) {
			throw new XMLException("Cannot save XML to " + file + ": " + e, e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static void stream(Document document, OutputStream out, Charset encoding) throws XMLException {
		stream(new DOMSource(document), out, encoding);
	}

	public static void stream(DOMSource source, OutputStream out, Charset encoding) throws XMLException {
		// we wrap this in a Writer to fix a Java bug
		// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
		if (encoding == null) {
			encoding = Charset.forName("UTF-8");
		}
		stream(source, new StreamResult(new OutputStreamWriter(out, encoding)), encoding);
	}

	public static void stream(Document document, Result result, Charset encoding) throws XMLException {
		stream(new DOMSource(document), result, encoding);
	}

	public static void stream(DOMSource source, Result result, Charset encoding) throws XMLException {
		stream(source, result, encoding, null);
	}

	public static void stream(DOMSource source, Result result, Charset encoding, Properties outputProperties)
			throws XMLException {
		Transformer transformer;
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			try {
				tf.setAttribute("indent-number", Integer.valueOf(2));
			} catch (IllegalArgumentException e) {
				// LogService.getRoot().log(Level.WARNING,
				// "XML transformer does not support indentation: " + e);
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.io.process.XMLTools.xml_transformer_does_not_support_identation", e));
			}
			transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			if (outputProperties != null) {
				transformer.setOutputProperties(outputProperties);
			}

			if (encoding != null) {
				transformer.setOutputProperty(OutputKeys.ENCODING, encoding.name());
			}
		} catch (TransformerConfigurationException e) {
			throw new XMLException("Cannot transform XML: " + e, e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new XMLException("Cannot transform XML: " + e, e);
		}
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new XMLException("Cannot transform XML: " + e, e);
		}
	}

	/**
	 * As {@link #getTagContents(Element, String, boolean)}, but never throws an exception. Returns
	 * null if can't retrieve string.
	 */
	public static String getTagContents(Element element, String tag) {
		try {
			return getTagContents(element, tag, false);
		} catch (XMLException e) {
			// cannot happen
			return null;
		}
	}

	public static String getTagContents(Element element, String tag, String deflt) {
		String result = getTagContents(element, tag);
		if (result == null) {
			return deflt;
		} else {
			return result;
		}
	}

	/**
	 * For a tag <parent> <tagName>content</tagName> <something>else</something> ... </parent>
	 * 
	 * returns "content". This will return the content of the first occurring child element with
	 * name tagName. If no such tag exists and {@link XMLException} is thrown if
	 * throwExceptionOnError is true. Otherwise null is returned.
	 * */
	public static String getTagContents(Element parent, String tagName, boolean throwExceptionOnError) throws XMLException {
		NodeList nodeList = parent.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node instanceof Element && ((Element) node).getTagName().equals(tagName)) {
				Element child = (Element) node;
				return child.getTextContent();
			}
		}
		if (throwExceptionOnError) {
			throw new XMLException("Missing tag: <" + tagName + "> in <" + parent.getTagName() + ">.");
		} else {
			return null;
		}
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as integer. If no such child element can be found an XMLException is thrown. If more
	 * than one exists, the first is used. A {@link XMLException} is thrown if the text content is
	 * not a valid integer.
	 */
	public static int getTagContentsAsInt(Element element, String tag) throws XMLException {
		final String string = getTagContents(element, tag, true);
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			throw new XMLException("Contents of tag <" + tag + "> must be integer, but found '" + string + "'.");
		}
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as integer. If no such child element can be found, the given default value is
	 * returned. If more than one exists, the first is used. A {@link XMLException} is thrown if the
	 * text content is not a valid integer.
	 */
	public static int getTagContentsAsInt(Element element, String tag, int dfltValue) throws XMLException {
		final String string = getTagContents(element, tag, false);
		if (string == null) {
			return dfltValue;
		}
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			throw new XMLException("Contents of tag <" + tag + "> must be integer, but found '" + string + "'.");
		}
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as long. If no such child element can be found an XMLException is thrown. If more
	 * than one exists, the first is used. A {@link XMLException} is thrown if the text content is
	 * not a valid long.
	 */
	public static long getTagContentsAsLong(Element element, String tag) throws XMLException {
		final String string = getTagContents(element, tag, true);
		try {
			return Long.parseLong(string);
		} catch (NumberFormatException e) {
			throw new XMLException("Contents of tag <" + tag + "> must be integer, but found '" + string + "'.");
		}
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as long. If no such child element can be found, the given default value is returned.
	 * If more than one exists, the first is used. A {@link XMLException} is thrown if the text
	 * content is not a valid long.
	 */
	public static long getTagContentsAsLong(Element element, String tag, int dfltValue) throws XMLException {
		final String string = getTagContents(element, tag, false);
		if (string == null) {
			return dfltValue;
		}
		try {
			return Long.parseLong(string);
		} catch (NumberFormatException e) {
			throw new XMLException("Contents of tag <" + tag + "> must be integer, but found '" + string + "'.");
		}
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as double. If no such child element can be found, the given default value is
	 * returned. If more than one exists, the first is used. A {@link XMLException} is thrown if the
	 * text content is not a valid integer.
	 */
	public static double getTagContentsAsDouble(Element element, String tag, double dfltValue) throws XMLException {
		final String string = getTagContents(element, tag, false);
		if (string == null) {
			return dfltValue;
		}
		try {
			return Double.parseDouble(string);
		} catch (NumberFormatException e) {
			throw new XMLException("Contents of tag <" + tag + "> must be double, but found '" + string + "'.");
		}
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as boolean. If no such child element can be found the default is returned. If more
	 * than one exists, the first is used. A {@link NumberFormatException} is thrown if the text
	 * content is not a valid integer.
	 */
	public static boolean getTagContentsAsBoolean(Element parent, String tagName, boolean dflt) throws XMLException {
		String string = getTagContents(parent, tagName, false);
		if (string == null) {
			return dflt;
		}
		try {
			return Boolean.parseBoolean(string);
		} catch (NumberFormatException e) {
			throw new XMLException("Contents of tag <" + tagName + "> must be true or false, but found '" + string + "'.");
		}
	}

	/**
	 * If parent has a direct child with the given name, the child's children are removed and are
	 * replaced by a single text node with the given text. If no direct child of parent with the
	 * given tag name exists, a new one is created.
	 */
	public static void setTagContents(Element parent, String tagName, String value) {
		if (value == null) {
			value = "";
		}
		Element child = null;
		NodeList list = parent.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node instanceof Element) {
				if (((Element) node).getTagName().equals(tagName)) {
					child = (Element) node;
					break;
				}
			}
		}
		if (child == null) {
			child = parent.getOwnerDocument().createElement(tagName);
			parent.appendChild(child);
		} else {
			while (child.hasChildNodes()) {
				child.removeChild(child.getFirstChild());
			}
		}
		child.appendChild(parent.getOwnerDocument().createTextNode(value));
	}

	/**
	 * This method removes all child elements with the given name of the given element.
	 */
	public static void deleteTagContents(Element parentElement, String name) {
		NodeList children = parentElement.getElementsByTagName(name);
		for (int i = 0; i < children.getLength(); i++) {
			Element child = (Element) children.item(i);
			parentElement.removeChild(child);
		}
	}

	public static XMLGregorianCalendar getXMLGregorianCalendar(Date date) {
		if (date == null) {
			return null;
		}
		// Calendar calendar = Calendar.getInstance();
		// calendar.setTimeInMillis(date.getTime());
		DatatypeFactory datatypeFactory;
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("Failed to create XMLGregorianCalendar: " + e, e);
		}
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(date);
		return datatypeFactory.newXMLGregorianCalendar(c);
		//
		// XMLGregorianCalendar xmlGregorianCalendar = datatypeFactory.newXMLGregorianCalendar();
		// xmlGregorianCalendar.setYear(calendar.get(Calendar.YEAR));
		// xmlGregorianCalendar.setMonth(calendar.get(Calendar.MONTH) + 1);
		// xmlGregorianCalendar.setDay(calendar.get(Calendar.DAY_OF_MONTH));
		// xmlGregorianCalendar.setHour(calendar.get(Calendar.HOUR_OF_DAY));
		// xmlGregorianCalendar.setMinute(calendar.get(Calendar.MINUTE));
		// xmlGregorianCalendar.setSecond(calendar.get(Calendar.SECOND));
		// xmlGregorianCalendar.setMillisecond(calendar.get(Calendar.MILLISECOND));
		// //
		// xmlGregorianCalendar.setTimezone(calendar.get(((Calendar.DST_OFFSET)+calendar.get(Calendar.ZONE_OFFSET))/(60*1000)));
		// return xmlGregorianCalendar;
	}

	/**
	 * This will return the inner tag of the given element with the given tagName. If no such
	 * element can be found, or if there are more than one, an {@link XMLException} is thrown.
	 */
	public static Element getUniqueInnerTag(Element element, String tagName) throws XMLException {
		return getUniqueInnerTag(element, tagName, true);
	}

	/**
	 * This method will return null if the element doesn't exist if obligatory is false. Otherwise
	 * an exception is thrown. If the element is not unique, an exception is thrown in any cases.
	 */
	public static Element getUniqueInnerTag(Element element, String tagName, boolean obligatory) throws XMLException {
		NodeList children = element.getChildNodes();
		Collection<Element> elements = new ArrayList<Element>();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i) instanceof Element) {
				Element child = (Element) children.item(i);
				if (tagName.equals(child.getTagName())) {
					elements.add(child);
				}
			}
		}
		switch (elements.size()) {
			case 0:
				if (obligatory) {
					throw new XMLException("Missing inner tag <" + tagName + "> inside <" + element.getTagName() + ">.");
				} else {
					return null;
				}
			case 1:
				return elements.iterator().next();
			default:
				throw new XMLException("Inner tag <" + tagName + "> inside <" + element.getTagName()
						+ "> must be unique, but found " + children.getLength() + ".");
		}

	}

	/**
	 * This method will return a Collection of all Elements that are direct child elements of the
	 * given element and have the given tag name.
	 */
	public static Collection<Element> getChildElements(Element father, String tagName) {
		LinkedList<Element> elements = new LinkedList<Element>();
		NodeList list = father.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node instanceof Element) {
				if (node.getNodeName().equals(tagName)) {
					elements.add((Element) node);
				}
			}
		}
		return elements;
	}

	/**
	 * This method will return a Collection of all Elements that are direct child elements of the
	 * given element.
	 */
	public static Collection<Element> getChildElements(Element father) {
		LinkedList<Element> elements = new LinkedList<Element>();
		NodeList list = father.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node instanceof Element) {
				elements.add((Element) node);
			}
		}
		return elements;
	}

	/**
	 * This method will return the single inner child with the given name of the given father
	 * element. If obligatory is true, an Exception is thrown if the element is not present. If it's
	 * ambiguous, an execption is thrown in any case.
	 */
	public static Element getChildElement(Element father, String tagName, boolean mandatory) throws XMLException {
		Collection<Element> children = getChildElements(father, tagName);
		switch (children.size()) {
			case 0:
				if (mandatory) {
					throw new XMLException("Missing child tag <" + tagName + "> inside <" + father.getTagName() + ">.");
				} else {
					return null;
				}
			case 1:
				return children.iterator().next();
			default:
				throw new XMLException("Child tag <" + tagName + "> inside <" + father.getTagName()
						+ "> must be unique, but found " + children.size() + ".");
		}

	}

	/**
	 * This is the same as {@link #getChildElement(Element, String, boolean)}, but its always
	 * obligatory to have the child element.
	 * 
	 * @throws XMLException
	 */
	public static Element getUniqueChildElement(Element father, String tagName) throws XMLException {
		return getChildElement(father, tagName, true);
	}

	/**
	 * This adds a single tag with the given content to the given parent element. The new tag is
	 * automatically appended.
	 */
	public static void addTag(Element parent, String name, String textValue) {
		Element child = parent.getOwnerDocument().createElement(name);
		child.setTextContent(textValue);
		parent.appendChild(child);
	}

	/**
	 * Creates a new, empty document.
	 */
	public static Document createDocument() {
		try {
			DocumentBuilder builder = createDocumentBuilder();
			return builder.newDocument();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * This will add an empty new tag to the given fatherElement with the given name.
	 */
	public static Element addTag(Element fatherElement, String tagName) {
		Element createElement = fatherElement.getOwnerDocument().createElement(tagName);
		fatherElement.appendChild(createElement);
		return createElement;
	}

	/**
	 * Returns the unique child of the given element with the given tag name. This child tag must be
	 * unique, or an exception will be raised. If optional is false and the tag is missing, this
	 * method also raises an exception. Otherwise it returns null.
	 */
	public static Element getChildTag(Element element, String xmlTagName, boolean optional) throws XMLException {
		NodeList children = element.getChildNodes();
		Element found = null;
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n instanceof Element) {
				if (((Element) n).getTagName().equals(xmlTagName)) {
					if (found != null) {
						throw new XMLException("Tag <" + xmlTagName + "> in <" + element.getTagName() + "> must be unique.");
					} else {
						found = (Element) n;
					}
				}
			}
		}
		if (!optional && found == null) {
			throw new XMLException("Tag <" + xmlTagName + "> in <" + element.getTagName() + "> is missing.");
		} else {
			return found;
		}
	}

	/**
	 * Returns the contents of the inner tags with the given name as String array.
	 */
	public static String[] getChildTagsContentAsStringArray(Element father, String childElementName) {
		Collection<Element> valueElements = XMLTools.getChildElements(father, childElementName);
		String[] values = new String[valueElements.size()];
		int i = 0;
		for (Element valueElement : valueElements) {
			values[i] = valueElement.getTextContent();
			i++;
		}

		return values;
	}

	/**
	 * Returns the contents of the inner tags with the given name as int array.
	 * 
	 * @throws XMLException
	 */
	public static int[] getChildTagsContentAsIntArray(Element father, String childElementName) throws XMLException {
		Collection<Element> valueElements = XMLTools.getChildElements(father, childElementName);
		int[] values = new int[valueElements.size()];
		int i = 0;
		for (Element valueElement : valueElements) {
			try {
				values[i] = Integer.valueOf(valueElement.getTextContent().trim());
			} catch (NumberFormatException e) {
				throw new XMLException("Invalid format for element content of type " + childElementName, e);
			}
			i++;
		}

		return values;
	}

	/**
	 * This method will get a XPath expression matching all elements given. This works by following
	 * this algorithm: 1. Check whether the last element is of same type Yes: if paths of elements
	 * are of same structure, keep it, but remove counters where necessary if not,
	 */
	public static String getXPath(Document document, Element... elements) {
		Map<String, List<Element>> elementTypeElementsMap = new HashMap<String, List<Element>>();
		for (Element element : elements) {
			List<Element> typeElements = elementTypeElementsMap.get(element.getTagName());
			if (typeElements == null) {
				typeElements = new LinkedList<Element>();
				elementTypeElementsMap.put(element.getTagName(), typeElements);
			}
			typeElements.add(element);
		}

		// for each single type of element build single longest common path of all elements

		Element[] parentElements = new Element[elements.length];

		for (int i = 0; i < elements.length; i++) {
			parentElements[i] = (Element) elements[i].getParentNode();
		}

		return "";
	}
}
