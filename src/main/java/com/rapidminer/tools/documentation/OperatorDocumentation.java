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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.Process;
import com.rapidminer.io.process.XMLTools;


/**
 * The operator documentation, currently consisting of a short synopsis, a long help text plus
 * examples. None of the getters returns null. If no long description is given, the synopsis will be
 * returned.
 *
 * Setting properties of instances of this class will, as a side effect, also modify the original
 * DOM element used to create this instance, so the DOM is always in sync with the data.
 *
 * @author Simon Fischer
 *
 */
public class OperatorDocumentation {

	private final OperatorDocBundle operatorDocBundle;
	private String name;
	private String shortName;
	private String synopsis;
	private String documentation;
	private String deprecation;
	private List<String> tags;
	private final Element element;
	private final List<ExampleProcess> exampleProcesses = new LinkedList<ExampleProcess>();

	public OperatorDocumentation(String name) {
		this.name = this.shortName = name;
		this.documentation = this.synopsis = null;
		this.element = null;
		this.operatorDocBundle = null;
		this.tags = new ArrayList<>();
	}

	OperatorDocumentation(OperatorDocBundle operatorDocBundle, Element element) {
		this.operatorDocBundle = operatorDocBundle;
		this.name = XMLTools.getTagContents(element, "name");
		this.shortName = XMLTools.getTagContents(element, "shortName");
		this.synopsis = XMLTools.getTagContents(element, "synopsis");
		this.documentation = XMLTools.getTagContents(element, "help");
		this.deprecation = XMLTools.getTagContents(element, "deprecation");
		this.tags = new ArrayList<>();
		for (Element tagsElement : XMLTools.getChildElements(element, "tags")) {
			tags.addAll(Arrays.asList(XMLTools.getChildTagsContentAsStringArray(tagsElement, "tag")));
		}
		this.element = element;
		if (synopsis == null) {
			synopsis = "";
		}
		if (documentation == null) {
			documentation = synopsis;
		}
		NodeList exampleNodes = element.getElementsByTagName("example");
		for (int i = 0; i < exampleNodes.getLength(); i++) {
			exampleProcesses.add(new ExampleProcess((Element) exampleNodes.item(i)));
		}
	}

	public String getName() {
		if (name != null) {
			return name;
		} else {
			return "";
		}
	}

	public void setName(String name) {
		this.name = name;
		if (element != null) {
			XMLTools.setTagContents(element, "name", name);
		}
	}

	public String getShortName() {
		if (shortName != null) {
			return shortName;
		} else {
			return name;
		}
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
		if (element != null) {
			XMLTools.setTagContents(element, "shortName", shortName);
		}
	}

	public String getSynopsis() {
		return synopsis;
	}

	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String text) {
		this.documentation = text;
		if (element != null) {
			XMLTools.setTagContents(element, "help", documentation);
		}
	}

	public void setSynopsis(String text) {
		this.synopsis = text;
		if (element != null) {
			XMLTools.setTagContents(element, "synopsis", text);
		}
	}

	public String getDeprecation() {
		return deprecation;
	}

	public void setDeprecation(String deprecation) {
		if (deprecation != null) {
			deprecation = deprecation.trim();
			if (deprecation.isEmpty()) {
				deprecation = null;
			}
		}
		this.deprecation = deprecation;
		if (element != null) {
			XMLTools.setTagContents(element, "deprecation", deprecation);
		}
	}

	public List<String> getTags() {
		return Collections.unmodifiableList(tags);
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
		if (element != null) {
			XMLTools.deleteTagContents(element, "tags");
			if (tags != null && !tags.isEmpty()) {
				Element tagsElement = element.getOwnerDocument().createElement("tags");
				element.appendChild(tagsElement);
				for (String tagValue : tags) {
					Element tagElement = tagsElement.getOwnerDocument().createElement("tag");
					tagsElement.appendChild(tagElement);
					tagElement.appendChild(tagsElement.getOwnerDocument().createTextNode(tagValue));
				}
			}
		}
	}

	public void addExample(Process process, String comment) {
		Element exampleElement = element != null ? element.getOwnerDocument().createElement("example") : null;
		ExampleProcess exampleProcess = new ExampleProcess(exampleElement);
		exampleProcess.setProcessXML(process.getRootOperator().getXML(true));
		if (comment != null) {
			exampleProcess.setComment(comment);
		}
		if (element != null) {
			element.appendChild(exampleElement);
		}
		exampleProcesses.add(exampleProcess);
	}

	public List<ExampleProcess> getExamples() {
		return Collections.unmodifiableList(exampleProcesses);
	}

	public void removeExample(int index) {
		ExampleProcess process = exampleProcesses.get(index);
		if (element != null) {
			element.removeChild(process.getElement());
		}
		exampleProcesses.remove(index);

	}

	public OperatorDocBundle getBundle() {
		return this.operatorDocBundle;
	}
}
