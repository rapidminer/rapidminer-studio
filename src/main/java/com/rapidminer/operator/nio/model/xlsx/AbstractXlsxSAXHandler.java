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
package com.rapidminer.operator.nio.model.xlsx;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.rapidminer.operator.UserError;


/**
 * An abstract SAX parser that provides a method to return the typed parsing result.
 *
 * @param <T>
 *            The type of the parsed result.
 *
 * @author Nils Woehler
 * @since 6.3.0
 */
public abstract class AbstractXlsxSAXHandler<T> extends DefaultHandler {

	/**
	 * Gets the result of the parsing process.
	 *
	 * @return An object which contains the result of the parsing process.
	 */
	protected abstract T getResult() throws UserError;

	/**
	 * @return the path of the Zip entry the concrete implementation is made for
	 */
	protected abstract String getZipEntryPath();

	/**
	 * Parses entry of zip file using the specified handler. Will NOT close the Zip file afterwards.
	 *
	 * @param zipFile
	 *            The file containing the entry
	 * @throws XlsxException
	 *             If the zip entry could not be found.
	 * @throws IOException
	 *             On error accessing the zip data.
	 * @throws ParserConfigurationException
	 *             If a parser cannot be created which satisfies the used configuration.
	 * @throws SAXException
	 *             If any SAX errors occur during processing.
	 * @throws UserError
	 *             in case the XLSX file is malformed
	 */
	public T parseZipEntry(ZipFile zipFile) throws IOException, ParserConfigurationException, SAXException, UserError {

		// Lookup zip entry
		ZipEntry zipEntry = zipFile.getEntry(getZipEntryPath());
		if (zipEntry == null) {
			throw new UserError(null, "xlsx_file_missing_entry", getZipEntryPath());
		}

		// Get stream for entry
		try (InputStream zipInputStream = zipFile.getInputStream(zipEntry)) {
			SAXParserFactory.newInstance().newSAXParser().parse(zipInputStream, this);
		}

		return getResult();
	}

}
