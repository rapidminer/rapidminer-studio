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
package com.rapidminer.tools.att;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;


/**
 * Reference to source of an attribute, i.e. file, column number (token number). Statics methods of
 * this class can be used to parse an attribute description file.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class AttributeDataSource {

	private File file;

	private int column;

	private Attribute attribute;

	private String attributeType;

	public AttributeDataSource(Attribute attribute, File file, int column, String attributeType) {
		this.attribute = attribute;
		this.file = file;
		this.column = column;
		this.attributeType = attributeType;
	}

	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}

	public Attribute getAttribute() {
		return attribute;
	}

	public int getColumn() {
		return column;
	}

	public File getFile() {
		return file;
	}

	public void setType(String type) {
		this.attributeType = type;
	}

	public String getType() {
		return attributeType;
	}

	public void setSource(File file, int column) {
		this.file = file;
		this.column = column;
	}

	public Element writeXML(Document document, File defaultSource) throws IOException {
		Element attributeElement = document.createElement(attributeType);
		attributeElement.setAttribute("name", attribute.getName());
		if (!getFile().equals(defaultSource)) {
			attributeElement.setAttribute("sourcefile", getFile().getAbsolutePath());
		}
		attributeElement.setAttribute("sourcecol", (getColumn() + 1) + "");
		attributeElement.setAttribute("valuetype", Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attribute.getValueType()));
		if (!Ontology.ATTRIBUTE_BLOCK_TYPE.isA(attribute.getBlockType(), Ontology.SINGLE_VALUE)) {
			attributeElement.setAttribute("blocktype", Ontology.ATTRIBUTE_BLOCK_TYPE.mapIndex(attribute.getBlockType()));
		}

		if ((Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NOMINAL))
				&& (!attributeType.equals(Attributes.KNOWN_ATTRIBUTE_TYPES[Attributes.TYPE_ID]))) {

			Iterator<String> i = attribute.getMapping().getValues().iterator();
			while (i.hasNext()) {
				Element valueElement = document.createElement("value");
				valueElement.setTextContent(i.next());
				attributeElement.appendChild(valueElement);
			}
		}
		return attributeElement;
	}

	/** Returns a list of {@link AttributeDataSource}s read from the file. */
	public static AttributeDataSources createAttributeDataSources(File attributeDescriptionFile, boolean sourceColRequired,
			LoggingHandler logging) throws XMLException, SAXException, IOException {
		Document document = XMLTools.createDocumentBuilder().parse(attributeDescriptionFile);

		Element attributeSet = document.getDocumentElement();
		if (!attributeSet.getTagName().equals("attributeset")) {
			throw new XMLException("Outer tag of attribute description file must be <attributeset>");
		}
		File defaultSource = null;
		if (attributeSet.getAttribute("default_source") != null) {
			defaultSource = Tools.getFile(attributeDescriptionFile.getParentFile(),
					attributeSet.getAttribute("default_source"));
		}
		Charset encoding = null;
		if (attributeSet.getAttribute("encoding") != null) {
			try {
				encoding = Charset.forName(attributeSet.getAttribute("encoding"));
			} catch (IllegalCharsetNameException e) {
			} catch (IllegalArgumentException e) {
			}
		}

		// attributes
		List<AttributeDataSource> attributeDataSources = new LinkedList<AttributeDataSource>();
		NodeList attributes = attributeSet.getChildNodes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node node = attributes.item(i);
			if (node instanceof Element) {
				Element attributeTag = (Element) node;
				String type = attributeTag.getTagName();
				String name = attributeTag.getAttribute("name");

				String file = null;
				Attr fileAttr = attributeTag.getAttributeNode("sourcefile");
				if (fileAttr != null) {
					file = fileAttr.getValue();
				}

				int firstSourceCol = -1;
				Attr sourcecolAttr = attributeTag.getAttributeNode("sourcecol");
				if (sourcecolAttr != null) {
					if (sourcecolAttr.getValue().equals("none")) {
						firstSourceCol = -1;
					} else {
						try {
							firstSourceCol = Integer.parseInt(sourcecolAttr.getValue()) - 1;
						} catch (NumberFormatException e) {
							throw new XMLException("Attribute sourcecol must be 'none' or an integer (was: '"
									+ sourcecolAttr.getValue() + "')!");
						}
					}
				}

				int lastSourceCol = -1;
				Attr sourceEndAttr = attributeTag.getAttributeNode("sourcecol_end");
				if (sourceEndAttr != null) {
					try {
						lastSourceCol = Integer.parseInt(sourceEndAttr.getValue()) - 1;
					} catch (NumberFormatException e) {
						throw new XMLException("Attribute sourcecol_end must be 'none' or an integer (was: '"
								+ sourceEndAttr.getValue() + "')!");
					}
				}

				int valueType = Ontology.VALUE_TYPE;
				Attr valueTypeAttr = attributeTag.getAttributeNode("valuetype");
				if (valueTypeAttr != null) {
					try {
						valueType = Integer.parseInt(valueTypeAttr.getValue());
					} catch (NumberFormatException e) {
						valueType = Ontology.ATTRIBUTE_VALUE_TYPE.mapName(valueTypeAttr.getValue());
						if (valueType < 0) {
							throw new XMLException("valuetype must be an index number or a legal value type name (was: '"
									+ valueTypeAttr.getValue() + "')");
						}
					}
				}

				int blockType = Ontology.SINGLE_VALUE;
				Attr blockTypeAttr = attributeTag.getAttributeNode("blocktype");
				if (blockTypeAttr != null) {
					try {
						blockType = Integer.parseInt(blockTypeAttr.getValue());
					} catch (NumberFormatException e) {
						blockType = Ontology.ATTRIBUTE_BLOCK_TYPE.mapName(blockTypeAttr.getValue());
						if (blockType < 0) {
							throw new XMLException("blocktype must be an index number or a legal block type name (was: '"
									+ blockTypeAttr.getValue() + "')");
						}
					}
				}

				List<String> classList = null;
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL)) {
					// nominal? check possible values...
					classList = new LinkedList<String>();

					// try inner tags <value>...</value>
					NodeList values = attributeTag.getElementsByTagName("value");
					for (int v = 0; v < values.getLength(); v++) {
						Node value = values.item(v);
						String valueText = value.getTextContent();
						classList.add(valueText);
					}

					// if list is still empty try depreciated 'classes' attribute
					Attr classesAttr = attributeTag.getAttributeNode("classes");
					if (classesAttr != null) {
						if (classList.size() == 0) {
							StringTokenizer tokenizer = new StringTokenizer(classesAttr.getValue());
							while (tokenizer.hasMoreTokens()) {
								classList.add(tokenizer.nextToken());
							}
						} else {
							logging.logWarning("XML attribute 'classes' ignored since possible values are already defined by inner <value>...</value> tags.");
						}
					}

					if (classList.size() == 0) { // still empty class list? --> Warning
						if (type.equals(Attributes.ID_NAME)) {
							logging.logNote("The ID attribute '"
									+ name
									+ "' is defined with a nominal value type but the possible values are not defined! "
									+ "Although this often does not lead to problems (unlike for labels or regular nominal attributes) you might want "
									+ "to specify the possible values by inner tags <value>first</value><value>second</value>....");
						} else if (type.equals(Attributes.LABEL_NAME)) {
							logging.logError("The label attribute (class) '"
									+ name
									+ "' is defined with a nominal value type but the possible values are not defined! "
									+ "Please specify the possible values by inner tags <value>first</value><value>second</value>.... "
									+ "Otherwise it might happen that the same nominal values of two example sets are handled in different ways which might cause flipped predictions.");
						} else {
							logging.logWarning("At least one of the attributes is defined with a nominal value type but the possible values are not defined! "
									+ "Please specify the possible values by inner tags <value>first</value><value>second</value>.... "
									+ "Otherwise it might happen that the same nominal values of two example sets are handled in different ways which might cause less accurate models.");
						}
					}
				}

				if (lastSourceCol == -1) {
					lastSourceCol = firstSourceCol;
				}
				if (sourceColRequired) {
					if (firstSourceCol < 0) {
						throw new XMLException("sourcecol not defined for " + type + " '" + name + "'!");
					}
					if (lastSourceCol < firstSourceCol) {
						throw new XMLException("sourcecol < sourcecol_end must hold.");
					}
				}

				for (int col = firstSourceCol; col <= lastSourceCol; col++) {
					int thisBlockType = blockType;
					String theName = name;
					if (lastSourceCol > firstSourceCol) {
						theName = name + "_" + (col + 1);
						if (col == firstSourceCol && blockType == Ontology.VALUE_SERIES) {
							thisBlockType = Ontology.VALUE_SERIES_START;
						}
						if (col == lastSourceCol && blockType == Ontology.VALUE_SERIES) {
							thisBlockType = Ontology.VALUE_SERIES_END;
						}
					}
					Attribute attribute = AttributeFactory.createAttribute(theName, valueType, thisBlockType);
					if (attribute.isNominal() && classList != null) {
						NominalMapping mapping = attribute.getMapping();
						classList.forEach(mapping::mapString);
					}
					if (!attribute.isNominal() && classList != null && classList.size() != 0) {
						// LogService.getGlobal().log("Ignoring classes for non-nominal attribute "
						// + theName + ".", LogService.WARNING);
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.tools.att.AttributeDataSource.ignoring_classes_for_non_nominal_attribute",
								theName);
					}

					attributeDataSources.add(new AttributeDataSource(attribute, (file != null) ? Tools.getFile(
							attributeDescriptionFile.getParentFile(), file) : defaultSource, col, type));
				}
			}
		}
		return new AttributeDataSources(attributeDataSources, defaultSource, encoding);
	}

	@Override
	public String toString() {
		return attribute.getName() + " (type: " + attributeType + ", value type: "
				+ Ontology.VALUE_TYPE_NAMES[attribute.getValueType()] + ") from " + file.getName() + " (" + column + ")";
	}
}
