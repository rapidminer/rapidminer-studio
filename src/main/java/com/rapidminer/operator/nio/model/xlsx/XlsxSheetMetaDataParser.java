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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xml.sax.Attributes;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.operator.nio.model.xlsx.XlsxResultSet.XlsxReadMode;
import com.rapidminer.operator.nio.model.xlsx.XlsxUtilities.XlsxCellCoordinates;


/**
 * StAX parser for XLSX worksheet meta data.
 *
 * FIXME adapt It opens the selected worksheet XML file and looks for the 'dimension' element to
 * extract the worksheet cell ranges. In case the 'dimension' element cannot be found or has invalid
 * content it has to perform a full scan of the worksheet XML to extract the cell range which can -
 * depending on the size of the provided Excel file - can take some time but is necessary for
 * pyramid like structured Excel files.
 *
 * @author Nils Woehler
 * @since 6.3.0
 */
public class XlsxSheetMetaDataParser {

	/**
	 * This element specifies the used range of the worksheet. It specifies the row and column
	 * bounds of used cells in the worksheet. This is optional and is not required. Used cells
	 * include cells with formulas, text content, and cell formatting. When an entire column is
	 * formatted, only the first cell in that column is considered used.
	 *
	 * @see ECMA-376, 4th Edition, 18.3.1.35 Worksheet dimensions (p. 1617). This tag is optional.
	 */
	public static final String TAG_DIMENSION = "dimension";

	/**
	 * The row and column bounds of all cells in this worksheet. Corresponds to the range that would
	 * contain all c elements written under sheetData. Does not support whole column or whole row
	 * reference notation.
	 *
	 * @see ECMA-376, 4th Edition, 18.3.1.35 Worksheet dimensions (p. 1617)
	 */
	public static final String ATT_DIMENSION_REF = "ref";

	/**
	 * Optimization only, and not required. Specifies the range of non-empty columns (in the format
	 * X:Y) for the block of rows to which the current row belongs. To achieve the optimization,
	 * span attribute values in a single block should be the same.
	 *
	 * @see ECMA-376, 4th Edition, 18.3.1.73 Row (p. 1670)
	 */
	public static final String ATT_SPANS = "spans";

	/**
	 * Currently, as of 16.01.2015, XLSX supports up to 104.857.76 rows per sheet.
	 */
	public static final int MAXIMUM_XLSX_ROW_INDEX = 104_857_75;

	/**
	 * Currently, as of 16.01.2015, XLSX supports up to 16.384 columns per sheet.
	 */
	public static final int MAXIMUM_XLSX_COLUMN_INDEX = 16_383;

	private final File xlsxFile;
	private final XMLInputFactory xmlInputFactory;
	private final String workbookZipEntryPath;

	public XlsxSheetMetaDataParser(File xlsxFile, String workbookZipEntryPath, XMLInputFactory xmlInputFactory) {
		this.xlsxFile = xlsxFile;
		this.workbookZipEntryPath = workbookZipEntryPath;
		this.xmlInputFactory = xmlInputFactory;
	}

	/**
	 * Parses the XLSX worksheet file to retrieve sheet meta data.
	 *
	 * @param configuration
	 *            the result set configuration
	 * @param readMode
	 *
	 * @return the parsed sheet meta data
	 *
	 * @throws XMLStreamException
	 *             if there is an error processing the underlying XML source
	 * @throws IOException
	 *             if an I/O error has occurred
	 * @throws UserError
	 *             in case something is configured wrong (e.g. wrong cell parsing range)
	 */
	public XlsxSheetMetaData parseMetaData(Operator callingOperator, ExcelResultSetConfiguration configuration,
			XlsxReadMode readMode) throws XMLStreamException, IOException, UserError {

		// use 0 as first row index in case the whole column is specified
		// offset via -1 for row number
		int firstRowIndex = Math.max(configuration.getRowOffset(), 0);
		int firstColumnIndex = configuration.getColumnOffset();
		int userSpecifiedLastRow = configuration.getRowLast();
		int userSpecifiedLastColumn = configuration.getColumnLast();

		// Check configured range
		if (firstColumnIndex > userSpecifiedLastColumn || firstRowIndex > userSpecifiedLastRow || firstColumnIndex < 0
				|| firstRowIndex < 0) {
			throw new UserError(callingOperator, 223, convertOffsetToHumanReadableFormat(firstColumnIndex, firstRowIndex,
					userSpecifiedLastRow, userSpecifiedLastColumn));
		}

		if (readMode != XlsxReadMode.WIZARD_WORKPANE) {
			// If the user has specified an end range just return the specified range
			if (userSpecifiedLastColumn != Integer.MAX_VALUE) {
				return new XlsxSheetMetaData(firstColumnIndex, firstRowIndex, userSpecifiedLastColumn,
						Math.min(userSpecifiedLastRow, MAXIMUM_XLSX_ROW_INDEX));
			}
		} else {
			firstRowIndex = 0;
			firstColumnIndex = 0;
		}

		// Otherwise parse work sheet to obtain meta data
		boolean isRowWithoutSpan = false;
		int maximumColumn = -1;

		XMLStreamReader reader = null;
		try (ZipFile xlsxZipFile = new ZipFile(xlsxFile)) {
			ZipEntry workbookZipEntry = xlsxZipFile.getEntry(workbookZipEntryPath);
			if (workbookZipEntry == null) {
				throw new FileNotFoundException("Selected workbook is missing in XLSX file. Path: " + workbookZipEntryPath);
			}
			reader = xmlInputFactory.createXMLStreamReader(xlsxZipFile.getInputStream(workbookZipEntry));

			while (reader.hasNext()) {
				int eventCode = reader.next();
				if (eventCode == XMLStreamReader.START_ELEMENT) {
					Attributes attributes = XlsxUtilities.getAttributes(reader);
					switch (reader.getLocalName()) {
						case TAG_DIMENSION:
							String dimension = attributes.getValue(ATT_DIMENSION_REF);

							/*
							 * Continue with next tags and parse all rows if dimension does not have
							 * a "ref" attribute
							 */
							if (dimension == null || dimension.isEmpty()) {
								continue;
							}

							int dimColonIndex = dimension.indexOf(':');

							/*
							 * If dimension does not contain a colon (e.g. when generated by
							 * RapidMiner Studio) it is invalid and should not be evaluated,
							 * otherwise try to parse the maximum cell range.
							 */
							if (dimColonIndex != -1) {
								String maxCellRange = dimension.substring(dimColonIndex + 1);
								try {
									XlsxCellCoordinates cellRange = XlsxUtilities.convertCellRefToCoordinates(maxCellRange);
									return new XlsxSheetMetaData(firstColumnIndex, firstRowIndex, cellRange.columnNumber,
											/*
											 * Always return the maximum Integer value for the last
											 * row index to be able to read the whole excel file in
											 * case the internal format is broken (e.g. files
											 * created by Libre Office)
											 */
											Integer.MAX_VALUE);
								} catch (IllegalArgumentException e) {
									// ignore malformed cell reference and continue parsing whole
									// file
								}
							}

							/*
							 * If we haven't found the dimension yet after parsing the dimension
							 * element we have to go over all rows and count the rows and columns
							 * one by one.
							 */
							break;
						case XlsxUtilities.TAG_ROW:
							// Check if row contains "spans" attribute
							String spans = attributes.getValue(ATT_SPANS);
							if (spans != null) {
								/*
								 * In case it is present extract the maximum column range of this
								 * row if a colon character can be found
								 */
								int rowColonIndex = spans.indexOf(':');
								if (rowColonIndex != -1) {
									/*
									 * We need to subtract 1 as Excel stores columns with a 1 based
									 * index.
									 */
									int maxColumn = Integer.parseInt(spans.substring(rowColonIndex + 1, spans.length())) - 1;
									if (maxColumn > maximumColumn) {
										maximumColumn = maxColumn;
									}
								}
							} else {
								isRowWithoutSpan = true;
							}
							break;
						case XlsxUtilities.TAG_CELL:
							// only parse cells in case the row does not contain a "spans" attribute
							if (isRowWithoutSpan) {
								XlsxCellCoordinates columnAndRowIndices = XlsxUtilities
										.convertCellRefToCoordinates(attributes.getValue(XlsxUtilities.TAG_CELL_REFERENCE));
								if (columnAndRowIndices.columnNumber > maximumColumn) {
									maximumColumn = columnAndRowIndices.columnNumber;
								}
							}
							break;
						default:
							// ignore other local names
							break;
					}
				} else if (eventCode == XMLStreamReader.END_ELEMENT) {
					if (reader.getLocalName().equals(XlsxUtilities.TAG_ROW)) {
						// Closing row element -> Row completed
						isRowWithoutSpan = false;
					}
				}
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

		/*
		 * Always return the maximum Integer value for the last row index to be able to read the
		 * whole excel file in case the internal format is broken (e.g. files created by Libre
		 * Office)
		 */
		return new XlsxSheetMetaData(firstColumnIndex, firstRowIndex, maximumColumn, Integer.MAX_VALUE);
	}

	/**
	 * Converts the provided row and column offset and last column and row indices to a human
	 * readable Excel format so we can display a proper error message.
	 */
	private String convertOffsetToHumanReadableFormat(int columnOffset, int confRowOffset, int columnLast, int rowLast) {
		StringBuilder rangeBuilder = new StringBuilder();
		if (columnOffset >= 0) {
			rangeBuilder.append(XlsxUtilities.convertToColumnName(columnOffset));
		}
		if (confRowOffset >= 0) {
			rangeBuilder.append(confRowOffset + 1);
		}
		boolean colonAdded = false;
		if (rowLast == -1 || columnLast == -1) {
			rangeBuilder.append(":");
			colonAdded = true;
		}

		if (columnLast != Integer.MAX_VALUE && columnLast != -1) {
			if (!colonAdded) {
				rangeBuilder.append(":");
				colonAdded = true;
			}
			rangeBuilder.append(XlsxUtilities.convertToColumnName(columnLast));
		}

		if (rowLast != Integer.MAX_VALUE && rowLast != -1) {
			if (!colonAdded) {
				rangeBuilder.append(":");
				colonAdded = true;
			}
			rangeBuilder.append(rowLast + 1);
		}
		return rangeBuilder.toString();
	}
}
