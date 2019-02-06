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
package com.rapidminer.io.process;

import java.util.LinkedList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * A simple error handler collecting all errors.
 * 
 * @author Simon Fischer
 */
public class XMLErrorHandler implements ErrorHandler {

	private final String sourceName;
	private final List<SAXParseException> warnings = new LinkedList<SAXParseException>();
	private final List<SAXParseException> errors = new LinkedList<SAXParseException>();

	public XMLErrorHandler(String sourceName) {
		this.sourceName = sourceName;
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		errors.add(exception);
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		errors.add(exception);
		throw exception;
	}

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		warnings.add(exception);
	}

	private String toString(SAXParseException e) {
		StringBuilder buf = new StringBuilder();
		buf.append(sourceName);
		buf.append(" (");
		buf.append(e.getLineNumber());
		buf.append(":");
		buf.append(e.getColumnNumber());
		buf.append(" at ");
		buf.append(e.getPublicId());
		buf.append(") ");
		buf.append(e.getLocalizedMessage());
		return buf.toString();
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		if (!errors.isEmpty()) {
			b.append("Errors:\n");
			for (SAXParseException e : errors) {
				b.append(toString(e));
				b.append("\n");
			}
		}
		if (!warnings.isEmpty()) {
			b.append("\nWarnings:\n");
			for (SAXParseException e : warnings) {
				b.append(toString(e));
				b.append("\n");
			}
		}
		return b.toString();
	}

	public boolean hasErrors() {
		return !errors.isEmpty();
	}

}
