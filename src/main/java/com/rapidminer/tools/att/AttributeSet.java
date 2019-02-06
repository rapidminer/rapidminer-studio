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

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.XMLException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;


/**
 * Set of regular and special attributes that need not necessarily be associated with an
 * {@link com.rapidminer.example.ExampleSet}.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class AttributeSet {

	/** List of regular attributes. */
	private List<Attribute> regularAttributes = new ArrayList<Attribute>();

	/** Names, i.e. Strings are mapped to special attributes. */
	private Map<String, Attribute> specialAttributes = new HashMap<String, Attribute>();

	/** The default source file. */
	private File defaultSource;

	/** Creates an empty attribute set. */
	public AttributeSet() {}

	/** Creates an empty attribute set. */
	public AttributeSet(int initialCapacity) {
		regularAttributes = new ArrayList<Attribute>(initialCapacity);
	}

	/**
	 * Creates an attribute set from the given collection of {@link AttributeDataSource}s.
	 */
	public AttributeSet(AttributeDataSources attributeDataSources) throws UserError {
		Iterator<AttributeDataSource> i = attributeDataSources.getDataSources().iterator();
		while (i.hasNext()) {
			AttributeDataSource ads = i.next();
			if (ads.getType().equals("attribute")) {
				addAttribute(ads.getAttribute());
			} else {
				Attribute attribute = specialAttributes.get(ads.getType());
				if (attribute != null) {
					throw new UserError(
							null,
							402,
							"Special attribute name '"
									+ ads.getType()
									+ "' was used more than one time. Please make sure that the names of special attributes (e.g. 'label' or 'id') are unique.");
				}
				setSpecialAttribute(ads.getType(), ads.getAttribute());
			}
		}
		this.defaultSource = attributeDataSources.getDefaultSource();
	}

	/** Reads an xml attribute description file and creates an attribute set. */
	public AttributeSet(File attributeDescriptionFile, boolean sourceColRequired, LoggingHandler logging)
			throws XMLException, ParserConfigurationException, SAXException, IOException, UserError {
		this(AttributeDataSource.createAttributeDataSources(attributeDescriptionFile, sourceColRequired, logging));
	}

	public AttributeSet(List<Attribute> regularAttributes, Map<String, Attribute> specialAttributes) {
		this.regularAttributes = regularAttributes;
		this.specialAttributes = specialAttributes;
	}

	/** Returns the default file. */
	public File getDefaultSource() {
		return defaultSource;
	}

	/** Returns an attribute by index. */
	public Attribute getAttribute(int index) {
		return regularAttributes.get(index);
	}

	/** Adds an attribute at the end of the list. */
	public void addAttribute(Attribute attribute) {
		regularAttributes.add(attribute);
	}

	/** Returns a special attribute by name. */
	public Attribute getSpecialAttribute(String name) {
		return specialAttributes.get(name);
	}

	/** Adds a named special attribute. */
	public void setSpecialAttribute(String name, Attribute attribute) {
		specialAttributes.put(name, attribute);
	}

	/** Returns a list of all names (Strings) of all special attributes. */
	public Set<String> getSpecialNames() {
		return specialAttributes.keySet();
	}

	/** Returns a list of all regular attributes. */
	public List<Attribute> getRegularAttributes() {
		return regularAttributes;
	}

	/** Returns the number of regular attributes. */
	public int getNumberOfRegularAttributes() {
		return regularAttributes.size();
	}

	/** Returns a Map mapping names to special attributes. */
	public Map<String, Attribute> getSpecialAttributes() {
		return specialAttributes;
	}

	/**
	 * Returns a list of all, i.e. regular and special attributes. This method creates a list. The
	 * first elements in the list will be the regular attributes, the last elements will be the
	 * special attributes.
	 */
	public List<Attribute> getAllAttributes() {
		List<Attribute> attributes = new LinkedList<Attribute>();
		attributes.addAll(regularAttributes);
		attributes.addAll(specialAttributes.values());
		return attributes;
	}
}
