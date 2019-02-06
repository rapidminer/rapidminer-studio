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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xml.sax.Attributes;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;


/**
 * StAX parser for XLSX Shared String Table.
 *
 * @see ECMA-376, 4th Edition, 18.4 Shared String Table (pp. 1709 ff.)
 *
 * @author Nils Woehler
 * @since 6.3.0
 */
public class XlsxSharedStringsParser {

	/**
	 * ECMA-376: 'optional unless uniqueCount is used'
	 *
	 * An integer representing the total count of strings in the workbook. This count does not
	 * include any numbers, it counts only the total of text strings in the workbook.
	 *
	 * This attribute is optional unless uniqueCount is used, in which case it is required.
	 *
	 * The possible values for this attribute are defined by the W3C XML Schema unsignedInt
	 * datatype.
	 *
	 * */
	private static final String ATT_SHARED_STRING_TABLE_COUNT = "count";

	/**
	 * ECMA-376: 'optional unless count is used'
	 *
	 * An integer representing the total count of unique strings in the Shared String Table. A
	 * string is unique even if it is a copy of another string, but has different formatting applied
	 * at the character level.
	 *
	 * [Example:
	 *
	 * World, [italic]World[italic], and World.
	 *
	 * The count would be 3, and the uniqueCount would be 2. Only one entry for "World" would show
	 * in the table because it is the same string, just with different formatting applied at the
	 * cell level (i.e., applied to the entire string in the cell). The "World" string would get a
	 * separate unique entry in the shared string table because it has different formatting applied
	 * to specific characters.
	 *
	 * end example]
	 *
	 * This attribute is optional unless count is used, in which case it is required.
	 *
	 * The possible values for this attribute are defined by the W3C XML Schema unsignedInt
	 * datatype.
	 *
	 * */
	private static final String ATT_SHARED_STRING_TABLE_UNIQUE_COUNT = "uniqueCount";

	/**
	 * Shared String Table top level element.
	 *
	 * @see ECMA-376, 4th Edition, subclause 18.4.9
	 */
	private static final String TAG_SHARED_STRING_TABLE = "sst";

	/**
	 * String Item child element.
	 *
	 * @see ECMA-376, 4th Edition, subclause 18.4.8
	 */
	private static final String TAG_STRING_ITEM = "si";

	/**
	 * This element represents the text content shown as part of a string.
	 *
	 * The possible values for this element are defined by the ST_Xstring simple type (22.9.2.19).
	 *
	 * @see ECMA-376, 4th Edition, subclause 18.4.12
	 */
	private static final String TAG_TEXT = "t";

	/** The XLSX file */
	private final File xlsxFile;

	/** The factory used to create {@link XMLStreamReader} */
	private final XMLInputFactory xmlFactory;

	/**
	 * The path of the shared strings file.
	 */
	private final String sharedStringsFilePath;

	public XlsxSharedStringsParser(File xlsxFile, String sharedStringsFilePath, XMLInputFactory xmlFactory) {
		this.xlsxFile = xlsxFile;
		this.sharedStringsFilePath = sharedStringsFilePath;
		this.xmlFactory = xmlFactory;
	}

	/**
	 * Parses the XLSX shared strings XML file and returns the parsed Strings as an array.
	 *
	 * @return the parsed shared strings as an array
	 * @throws IOException
	 *             in case the Shared Strings Zip entry cannot be opened
	 * @throws XMLStreamException
	 *             in case the {@link XMLInputFactory} cannot create a {@link XMLStreamReader}
	 * @throws UserError
	 *             in case the shared string content is malformed
	 * @throws XlsxException
	 *             in case the shared string XML content is invalid
	 */
	public String[] parseSharedStrings(Operator op, Charset encoding) throws XMLStreamException, IOException, UserError {

		boolean isCurrentTagText = false;
		int numberOfItems = 0;
		int stringItemCounter = 0;
		String[] xlsxSharedStrings = null;
		XMLStreamReader reader = null;
		try (ZipFile zipFile = new ZipFile(xlsxFile)) {
			ZipEntry zipEntry = zipFile.getEntry(XlsxUtilities.XLSX_PATH_PREFIX + sharedStringsFilePath);
			if (zipEntry == null) {
				// no shared strings defined
				return new String[0];
			}

			InputStream inputStream = zipFile.getInputStream(zipEntry);
			reader = xmlFactory.createXMLStreamReader(new InputStreamReader(inputStream, encoding));
			while (reader.hasNext()) {
				switch (reader.next()) {
					case XMLStreamReader.START_ELEMENT:
						Attributes attributes = XlsxUtilities.getAttributes(reader);
						if (reader.getLocalName().equals(TAG_SHARED_STRING_TABLE)) {

							// retrieve uniqueCount values
							String uniqueCount = attributes.getValue(ATT_SHARED_STRING_TABLE_UNIQUE_COUNT);

							if (uniqueCount != null) {
								// in case uniqueCount is set use it as counter
								numberOfItems = Integer.parseInt(uniqueCount);
							} else {
								String count = attributes.getValue(ATT_SHARED_STRING_TABLE_COUNT);

								// in case only count is set, use count
								if (count != null) {
									numberOfItems = Integer.parseInt(count);
								}
							}

							// initialize String array
							xlsxSharedStrings = new String[numberOfItems];

						} else if (reader.getLocalName().equals(TAG_TEXT)) {
							// we ignore formatting stored within the Shared Table XML because we
							// are only looking for the actual text
							isCurrentTagText = true;
						}
						break;
					case XMLStreamReader.END_ELEMENT:
						if (reader.getLocalName().equals(TAG_STRING_ITEM)) {
							stringItemCounter++;
						} else if (reader.getLocalName().equals(TAG_TEXT)) {
							isCurrentTagText = false;
						}
						break;
					case XMLStreamReader.CHARACTERS:
						if (isCurrentTagText) {
							if (xlsxSharedStrings[stringItemCounter] == null) {
								// no text found yet for current TAG_STRING_ITEM
								xlsxSharedStrings[stringItemCounter] = reader.getText();
							} else {
								// append new text to other text for current TAG_STRING_ITEM
								xlsxSharedStrings[stringItemCounter] += reader.getText();
							}
						}
						break;
					case XMLStreamReader.END_DOCUMENT:
						// Final check of correctness of logic
						if (stringItemCounter != numberOfItems) {
							throw new UserError(op, "xlsx_content_malformed");
						}
						break;
					default:
						// ignore other cases
						break;
				}
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return xlsxSharedStrings;
	}
}
