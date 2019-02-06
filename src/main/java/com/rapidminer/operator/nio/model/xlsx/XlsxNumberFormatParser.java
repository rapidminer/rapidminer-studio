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
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xml.sax.Attributes;


/**
 * StAX parser that extracts number formats from the XLSX styles file.
 *
 * @see ECMA-376, 4th Edition, 18.8 Styles (pp. 1744 ff.)
 *
 * @author Nils Woehler
 * @since 6.3.0
 */
public class XlsxNumberFormatParser {

	/**
	 * This element defines the number formats in this workbook, consisting of a sequence of numFmt
	 * records, where each numFmt record defines a particular number format, indicating how to
	 * format and render the numeric value of a cell.
	 *
	 * @see ECMA-376, 4th Edition, subclause 18.8.31
	 **/
	private static final String TAG_NUMBER_FORMAT = "numFmt";

	/** Tag used to define number format IDs. */
	private static final String ATT_NUM_FORM_ID = "numFmtId";

	/** Tag used to define the actual number format code */
	private static final String ATT_FORMAT_CODE = "formatCode";

	/**
	 * This element contains the master formatting records (xf) which define the formatting applied
	 * to cells in this workbook. These records are the starting point for determining the
	 * formatting for a cell. Cells in the Sheet Part reference the xf records by zero-based index. <br/>
	 * <br/>
	 * A cell can have both direct formatting (e.g., bold) and a cell style (e.g., Explanatory)
	 * applied to it. Therefore, both the cell style xf records and cell xf records shall be read to
	 * understand the full set of formatting applied to a cell.
	 *
	 * @see ECMA-376, 4th Edition, subclause 18.8.10
	 */
	private static final String TAG_CELL_FORMATS = "cellXfs";

	/**
	 * A single xf element describes all of the formatting for a cell.
	 *
	 * @see ECMA-376, 4th Edition, subclause 18.8.45
	 */
	private static final String TAG_FORMAT = "xf";

	/** Tag that defines the amount of cell formats stored within the styles XML file. */
	private static final String ATT_COUNT = "count";

	/**
	 * Id of the number format (numFmt) record used by this cell format.
	 */
	private static final String ATT_NUMBER_FORMAT_ID = "numFmtId";

	/** The XLSX file */
	private final File xlsxFile;

	/** The factory used to create {@link XMLStreamReader} */
	private final XMLInputFactory xmlFactory;

	/** The path of the styles file */
	private final String stylesPath;

	public XlsxNumberFormatParser(File xlsxFile, String stylesPath, XMLInputFactory xmlFactory) {
		this.xlsxFile = xlsxFile;
		this.stylesPath = stylesPath;
		this.xmlFactory = xmlFactory;
	}

	/**
	 * Parses the XLSX styles XML file (with UTF-8 encoding) and returns the parsed number formats.
	 *
	 * @return the number formats stored within a {@link XlsxNumberFormats} object
	 * @throws IOException
	 *             in case the Shared Strings Zip entry cannot be opened
	 * @throws XMLStreamException
	 *             in case the {@link XMLInputFactory} cannot create a {@link XMLStreamReader}
	 * @throws XlsxException
	 *             in case the shared string XML content is invalid
	 */
	public XlsxNumberFormats parseNumberFormats() throws XMLStreamException, IOException {

		boolean isCellFormats = false;
		int cellFormatIndex = 0;
		XlsxNumberFormats xlsxNumberFormats = new XlsxNumberFormats();
		XMLStreamReader reader = null;
		try (ZipFile zipFile = new ZipFile(xlsxFile)) {
			ZipEntry zipEntry = zipFile.getEntry(XlsxUtilities.XLSX_PATH_PREFIX + stylesPath);
			if (zipEntry == null) {
				// no styles defined
				return null;
			}

			InputStream inputStream = zipFile.getInputStream(zipEntry);
			reader = xmlFactory.createXMLStreamReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			while (reader.hasNext()) {
				switch (reader.next()) {
					case XMLStreamReader.START_ELEMENT:
						if (TAG_NUMBER_FORMAT.equals(reader.getLocalName())) {
							Attributes attributes = XlsxUtilities.getAttributes(reader);
							xlsxNumberFormats.addNumberFormat(Integer.parseInt(attributes.getValue(ATT_NUM_FORM_ID)),
									attributes.getValue(ATT_FORMAT_CODE));
						} else if (TAG_CELL_FORMATS.equals(reader.getLocalName())) {
							isCellFormats = true;

							// create an array of the size of all defined cell formats
							xlsxNumberFormats.initializeCellNumberFormatIds(Integer.parseInt(XlsxUtilities.getAttributes(
									reader).getValue(ATT_COUNT)));
						} else if (isCellFormats && TAG_FORMAT.equals(reader.getLocalName())) {
							xlsxNumberFormats.setCellNumberFormatId(cellFormatIndex,
									Integer.parseInt(XlsxUtilities.getAttributes(reader).getValue(ATT_NUMBER_FORMAT_ID)));
							++cellFormatIndex;
						}
						break;
					case XMLStreamReader.END_ELEMENT:
						if (TAG_CELL_FORMATS.equals(reader.getLocalName())) {
							isCellFormats = false;
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
		return xlsxNumberFormats;
	}
}
