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

import com.rapidminer.Process;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;

import java.io.IOException;
import java.util.logging.Level;

import org.w3c.dom.Element;


/**
 * An example process with a description.
 * 
 * @author Simon Fischer
 * */
public class ExampleProcess {

	private String processXML;
	private String comment;
	private final Element element;

	public ExampleProcess(Element exampleElement) {
		this.element = exampleElement;
		if (element != null) {
			this.processXML = XMLTools.getTagContents(element, "process");
			this.comment = XMLTools.getTagContents(element, "comment");
		}
	}

	public void setComment(String comment) {
		this.comment = comment;
		if (element != null) {
			XMLTools.setTagContents(element, "comment", comment);
		}
	}

	public String getComment() {
		return comment;
	}

	public void setProcessXML(String xml) {
		this.processXML = xml;
		if (element != null) {
			XMLTools.setTagContents(element, "process", xml);
		}
	}

	public String getProcessXML() {
		return processXML;
	}

	public Process getProcess() {
		try {
			return new Process(getProcessXML());
		} catch (IOException e) {
			// LogService.getRoot().log(Level.WARNING, "Cannot parse example process: "+e, e);
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.documentation.ExampleProcess.parsing_example_process_error", e), e);
			return null;
		} catch (XMLException e) {
			// LogService.getRoot().log(Level.WARNING, "Cannot parse example process: "+e, e);
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.documentation.ExampleProcess.parsing_example_process_error", e), e);
			return null;
		}
	}

	public Element getElement() {
		return element;
	}

}
