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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.tika.io.CountingInputStream;
import org.xml.sax.Attributes;

import com.rapidminer.operator.nio.model.ParseException;
import com.rapidminer.operator.nio.model.ParsingError;
import com.rapidminer.operator.nio.model.ParsingError.ErrorCode;
import com.rapidminer.operator.nio.model.xlsx.XlsxResultSet.XlsxReadMode;
import com.rapidminer.operator.nio.model.xlsx.XlsxUtilities.XlsxCell;


/**
 * A StAX parser for XLSX worksheets based on specifications from ECMA-376, 4th Edition.
 *
 * @author Nils Woehler
 * @since 6.3.0
 */
class XlsxSheetContentParser implements AutoCloseable {

	/**
	 * An enumeration representing the cell's data type.
	 *
	 * @see ECMA-376, 4th Edition, 18.3.1.4 Cell (p. 1589)
	 */
	private static final String ATT_CELL_TYPE = "t";

	/**
	 * The index of this cell's style. Style records are stored in the Styles Part.
	 *
	 * @see ECMA-376, 4th Edition, 18.3.1.4 Cell (p. 1589)
	 */
	private static final String ATT_CELL_STYLE = "s";

	/**
	 * This element expresses the value contained in a cell. If the cell contains a string, then
	 * this value is an index into the shared string table, pointing to the actual string value.
	 * Otherwise, the value of the cell is expressed directly in this element.
	 *
	 * Cells containing formulas express the last calculated result of the formula in this element.
	 * For applications not wanting to implement the shared string table, an 'inline string' can be
	 * expressed in an <is> element under <c> (instead of a <v> element under <c>),in the same way a
	 * string would be expressed in the shared string table.
	 *
	 * [Example: In this example, cell B4 contains the number "360", cell C4 contains the local date
	 * and time 22 November 1976, 08:30, and cell C5 contains the 1900 date system serial date-time
	 * for the date-time in cell C4.
	 *
	 * <pre>
	 * {@code
	 * <c r="B4">
	 * 	<v>360</v>
	 * </c>
	 * <c r="C4" t="d">
	 * 	<v>1976-11-22T08:30</v>
	 * </c>
	 * <c r="C5">
	 * 	<f>C4</f>
	 * 	<v>28086.3541666667</v>
	 * </c>
	 * }
	 * </pre>
	 *
	 * end example]
	 *
	 * @see ECMA-376, 4th Edition, 18.3.1.96 Cell Value (pp. 1699 ff.)
	 */
	private static final String TAG_VALUE = "v";

	/**
	 * This element allows for strings to be expressed directly in the cell definition instead of
	 * implementing the shared string table.<br/>
	 * [Example:
	 *
	 * <pre>
	 * {@code
	 * <c r="A1">
	 *  <is>
	 *   <t>String</t>
	 *  </is>
	 * </c>
	 * }
	 * </pre>
	 *
	 * end example]
	 *
	 * @see ECMA-376, 4th Edition, 18.3.1.53 Rich Text Inline (pp. 1648 ff.)
	 */
	private static final String TAG_INLINE_STRING = "is";

	/** The XLSX file itself */
	private final File xlsxFile;

	/** The path to the worksheet within the XLSX zip file */
	private final String workbookZipEntryPath;

	/** Strings shared in multiple sheets */
	private final String[] sharedStrings;

	/** Number formats defined for XLSX cells */
	private final XlsxNumberFormats numberFormats;

	/** The ZipFile object used to open the {@link #reader} */
	private ZipFile xlsxZipFile;

	/** Reads XML stream */
	private XMLStreamReader reader;

	/** CountingInputStream that is used to determine the operator progress */
	private CountingInputStream cis;

	/** The current row content. */
	private XlsxCell[] currentRowContent;

	/**
	 * Keeps track of the current {@code 0-based} row index that was extracted from the XLSX sheet.
	 * Is {@code -1} if no row was parsed yet.
	 */
	private int currentRowIndex;

	/** Cache for the next row that contains element */
	private XlsxCell[] nextRowWithContent;

	/**
	 * The index of the row that has already been parsed ({@code 0-based}). Is {@code -1} if no row
	 * has been parsed yet. <br/>
	 */
	private int parsedRowIndex;

	/** A flag which represents if more rows with content are available */
	private boolean hasMoreContent;

	/**
	 * The parsed sheet meta data.
	 */
	private XlsxSheetMetaData sheetMetaData;

	/**
	 * An array which stores information about the content of a column.
	 */
	private final boolean[] emptyColumn;

	/**
	 * Flag that defines whether the first empty rows should be skipped.
	 */
	private boolean isUseFirstRowAsNames;

	/** The encoding used to read the XML stream */
	private final Charset encoding;

	/**
	 * Constructs object by saving the shared strings and instantiating a XML reader.
	 *
	 * @param xlsxFile
	 *            The xlsx file itself
	 * @param workbookZipEntryPath
	 *            the path of the workbook Zip entry
	 * @param sharedStrings
	 *            A ordered list of shared strings to generate complete cell values.
	 * @param numberFormats
	 *            the parsed XLSX number formats
	 * @param maximumCellRange
	 *            the maximum cell that should be parsed
	 * @param columnOffset
	 *            the offset of the first column to use.
	 * @throws XMLStreamException
	 *             On errors creating a XML stream reader.
	 * @throws IOException
	 *             in case opening the workbook does not work
	 */
	public XlsxSheetContentParser(File xlsxFile, String workbookZipEntryPath, String[] sharedStrings,
			XlsxNumberFormats numberFormats, XlsxSheetMetaData sheetMetaData, XMLInputFactory factory, Charset encoding)
			throws XMLStreamException, IOException {
		this.xlsxFile = xlsxFile;
		this.workbookZipEntryPath = workbookZipEntryPath;
		this.sharedStrings = sharedStrings;
		this.numberFormats = numberFormats;
		this.sheetMetaData = sheetMetaData;
		this.encoding = encoding;
		this.emptyColumn = new boolean[sheetMetaData.getNumberOfColumns()];
		Arrays.fill(emptyColumn, true);
		reset(factory);
	}

	/**
	 * Continues parsing the XML stream until a complete row is found.
	 *
	 * @param readMode
	 *            defines whether reading is done from an operator which means the parameters should
	 *            be obeyed or if reading is done from a Wizard which means all data should be read
	 * @throws XMLStreamException
	 *             If there is an error processing the underlying XML source.
	 * @throws ParseException
	 *             If there is an error on parsing a single XML item.
	 */
	public void next(XlsxReadMode readMode) throws XMLStreamException, ParseException {

		// If reading from an operator or wizard preview skip rows up to row index before actual
		// first row that should be parsed
		if (readMode != XlsxReadMode.WIZARD_WORKPANE) {
			skipToStartRow();
		}

		// If no row has been parsed yet ..
		if (nextRowWithContent == null) {
			// parse next row with content
			this.nextRowWithContent = parseNextRowWithContent();
		}

		// Increase current row index
		++currentRowIndex;

		/*
		 * The current row is empty if the row index is smaller than the index of the next parsed
		 * row.
		 */
		boolean isRowEmpty = currentRowIndex < parsedRowIndex;

		// Check for first imported row whether empty rows should be skipped
		if (currentRowContent == null) {
			assignNextCurrentRow(isRowEmpty && (!isUseFirstRowAsNames || readMode == XlsxReadMode.WIZARD_PREVIEW));
		} else {
			// All other empty rows will be added
			assignNextCurrentRow(isRowEmpty);
		}

	}

	/**
	 * Assigns the next current row by checking whether an empty row or the next parsed row with
	 * content should be assigned. In case the next row with content was assigned as current row,
	 * the next row with content is parsed subsequently.
	 *
	 * @param assignEmptyRow
	 *            defines whether an empty row or the next row with content should be assigned
	 * @throws XMLStreamException
	 *             If there is an error processing the underlying XML source.
	 * @throws ParseException
	 *             If there is an error on parsing a single XML item.
	 */
	private void assignNextCurrentRow(boolean assignEmptyRow) throws ParseException, XMLStreamException {
		if (assignEmptyRow) {
			// If current row is empty initialize a new (empty) array for current row
			this.currentRowContent = new XlsxCell[sheetMetaData.getNumberOfColumns()];
		} else {
			// ... otherwise assign next row with content to current row and parse next row with
			// content
			this.currentRowContent = nextRowWithContent;
			this.currentRowIndex = parsedRowIndex;
			this.nextRowWithContent = parseNextRowWithContent();
		}
	}

	/**
	 * Parses next XLSX rows until a row with content was found.
	 *
	 * @return if the next row with content or an empty row in case no row with content was found
	 *         anymore
	 * @throws XMLStreamException
	 *             If there is an error processing the underlying XML source.
	 * @throws ParseException
	 *             If there is an error on parsing a single XML item.
	 */
	private XlsxCell[] parseNextRowWithContent() throws ParseException, XMLStreamException {

		boolean isRowWithContent = false;
		boolean isValue = false;
		boolean contentFound = false;
		int columnIndex = 0;

		int numberOfColumns = sheetMetaData.getNumberOfColumns();

		XlsxCell[] nextRowWithContent = new XlsxCell[numberOfColumns];
		while (!isRowWithContent && reader.hasNext()) {

			// Parse the next element
			switch (reader.next()) {
				case XMLStreamReader.START_ELEMENT:
					String startLocalName = reader.getLocalName();
					if (startLocalName.equals(XlsxUtilities.TAG_ROW)) {
						// We need to subtract 1 as XLSX indices start with 1
						String indexValue = XlsxUtilities.getAttributes(reader).getValue(XlsxUtilities.TAG_ROW_INDEX);
						try {
							parsedRowIndex = Integer.parseInt(indexValue) - 1;
						} catch (NumberFormatException e) {
							throw new ParseException(
									new ParsingError(parsedRowIndex, columnIndex, ErrorCode.FILE_SYNTAX_ERROR, indexValue));
						}
					} else if (startLocalName.equals(XlsxUtilities.TAG_CELL)) {
						Attributes attributes = XlsxUtilities.getAttributes(reader);

						// Update column index
						String cellReference = attributes.getValue(XlsxUtilities.TAG_CELL_REFERENCE);
						try {
							columnIndex = sheetMetaData
									.mapColumnIndex(XlsxUtilities.convertCellRefToCoordinates(cellReference).columnNumber);
						} catch (IllegalArgumentException e) {
							throw new ParseException(new ParsingError(parsedRowIndex, columnIndex,
									ParsingError.ErrorCode.FILE_SYNTAX_ERROR, cellReference));
						}

						// Check if the current column should be skipped
						if (sheetMetaData.isSkipColumn(columnIndex)) {
							break;
						}

						// Parse cell type and create new XLSX cell
						String cellType = attributes.getValue(ATT_CELL_TYPE);
						String cellStyle = attributes.getValue(ATT_CELL_STYLE);
						XlsxCellType type = XlsxCellType.getCellType(cellType, numberFormats, cellStyle);
						if (type != null) {
							nextRowWithContent[columnIndex] = new XlsxCell(type);
						} else {
							throw new ParseException(new ParsingError(parsedRowIndex, columnIndex,
									ParsingError.ErrorCode.FILE_SYNTAX_ERROR, cellType));
						}
					} else if (startLocalName.equals(TAG_VALUE) || startLocalName.equals(TAG_INLINE_STRING)) {
						// Information for value parsing
						isValue = true;
					}
					break;
				case XMLStreamReader.END_ELEMENT:
					String endLocalName = reader.getLocalName();
					if (endLocalName.equals(XlsxUtilities.TAG_ROW)) {
						// At least one value has been parsed for current row
						// so stop parsing any further
						isRowWithContent = contentFound;
					} else if (endLocalName.equals(TAG_VALUE) || endLocalName.equals(TAG_INLINE_STRING)) {
						// Text part ends
						isValue = false;
					}
					break;
				case XMLStreamReader.CHARACTERS:
					if (!sheetMetaData.isSkipColumn(columnIndex) && isValue) {
						String text = reader.getText();
						if (nextRowWithContent[columnIndex].cellType.equals(XlsxCellType.SHARED_STRING)) {
							nextRowWithContent[columnIndex].value = sharedStrings[Integer.parseInt(text)];
						} else {
							nextRowWithContent[columnIndex].value = text;
						}
						emptyColumn[columnIndex] = false;
						contentFound = true;
					}
					break;
				case XMLStreamReader.END_DOCUMENT:
					// end of document was reached but no new row with content was found
					this.hasMoreContent = false;
					break;
				default:
					// ignore other events
					break;
			}
		}
		return nextRowWithContent;
	}

	/**
	 * Skips all rows before the selected starting row.
	 *
	 * @throws XMLStreamException
	 *             If there is an error processing the underlying XML source.
	 */
	private void skipToStartRow() throws XMLStreamException {
		int rowIndexBeforeFirstRow = sheetMetaData.getFirstRowIndex() - 1;
		boolean skipRows = currentRowIndex < rowIndexBeforeFirstRow;
		while (skipRows && reader.hasNext()) {
			int nextEvent = reader.next();
			if (nextEvent == XMLStreamReader.START_ELEMENT) {
				// If a row was found..
				if (reader.getLocalName().equals(XlsxUtilities.TAG_ROW)) {
					// ... parse the row index (subtract 1 as XLSX indices start with 1)
					parsedRowIndex = Integer
							.parseInt(XlsxUtilities.getAttributes(reader).getValue(XlsxUtilities.TAG_ROW_INDEX)) - 1;

					// Check whether the parsed index is already
					// behind the desired end index (which means that all rows before were
					// empty)
					if (parsedRowIndex > rowIndexBeforeFirstRow) {
						currentRowIndex = rowIndexBeforeFirstRow;
						skipRows = false;
					} else {
						// Update the current row index if parsed index is still
						// before first row to read
						currentRowIndex = parsedRowIndex;
					}
				}
			} else if (nextEvent == XMLStreamReader.END_ELEMENT && reader.getLocalName().equals(XlsxUtilities.TAG_ROW)) {
				// parse until end of row if we haven't parsed too far yet
				skipRows = currentRowIndex < rowIndexBeforeFirstRow;
			}
		}
	}

	/**
	 * Returns the {@code 0-based} index of the current row, which was extracted from the worksheet.
	 *
	 * @return the {@code 0} based index of the current parsed row. Returns {@code -1} in case no
	 *         row has been parsed yet. It is increased each time calling {@link #next} and is
	 *         reset to {@code -1} in case {@link #reset(XMLInputFactory)} is called.
	 */
	int getCurrentRowIndex() {
		return currentRowIndex;
	}

	/**
	 * @return the content of the current parsed row. Returns <code>null</code> in case no row has
	 *         been parsed yet.
	 */
	XlsxCell[] getRowContent() {
		return currentRowContent;
	}

	@Override
	public void close() throws XMLStreamException, IOException {

		// Close the XMLStreamReader
		if (reader != null) {
			reader.close();
		}

		// close Zip file and its open InputStreams
		if (xlsxZipFile != null) {
			xlsxZipFile.close();
		}
	}

	/**
	 * @return <code>true</code> in case the {@link XMLStreamReader} has more rows with content
	 *         available.
	 */
	boolean hasNext() {
		return hasMoreContent && sheetMetaData.getLastRowIndex() >= 0 && currentRowIndex < sheetMetaData.getLastRowIndex();
	}

	/**
	 * Closes the current open {@link XMLStreamReader} and creates a new one which starts the
	 * reading process at the first row. It is assumed the the XLSX content and operator
	 * configuration remain the same.
	 *
	 * @param xmlFactory
	 *            the {@link XMLInputFactory} that should be used to open the
	 *            {@link XMLStreamReader}.
	 *
	 * @throws IOException
	 *             if an I/O error has occurred
	 * @throws XMLStreamException
	 *             if there are errors freeing associated XML reader resources or creating a new XML
	 *             reader
	 */
	void reset(XMLInputFactory xmlFactory) throws IOException, XMLStreamException {

		// close open file and reader object
		close();

		// create new file and stream reader objects
		xlsxZipFile = new ZipFile(xlsxFile);
		ZipEntry workbookZipEntry = xlsxZipFile.getEntry(workbookZipEntryPath);
		if (workbookZipEntry == null) {
			throw new FileNotFoundException(
					"XLSX file is malformed. Reason: Selected workbook is missing in XLSX file. Path: "
							+ workbookZipEntryPath);
		}
		InputStream inputStream = xlsxZipFile.getInputStream(workbookZipEntry);
		cis = new CountingInputStream(inputStream);
		reader = xmlFactory.createXMLStreamReader(new InputStreamReader(cis, encoding));

		// reset other variables
		currentRowIndex = -1;
		parsedRowIndex = -1;
		currentRowContent = null;
		nextRowWithContent = null;
		hasMoreContent = true;
		Arrays.fill(emptyColumn, true);
	}

	/**
	 * @return an array which stores if an column was empty during parsing. You should only call
	 *         this method if {@link #hasNext()} returns <code>false</code>.
	 */
	boolean[] getEmptyColumns() {
		return emptyColumn;
	}

	/**
	 * @param isFirstRowAsNames
	 *            defines whether the first row should be used as names. If set to <code>true</code>
	 *            the worksheet parser will skip all beginning empty rows until the first row with
	 *            content was found.
	 */
	void setUseFirstRowAsNames(boolean isFirstRowAsNames) {
		this.isUseFirstRowAsNames = isFirstRowAsNames;
	}

	/**
	 *
	 * @return The total size of the entry data, which can be used to determine the total operator
	 *         progress
	 */
	long getTotalSize() {
		return xlsxZipFile.getEntry(workbookZipEntryPath).getSize();
	}

	/**
	 *
	 * @return The current position in the entry data, which can be used to determine the current
	 *         operator progress
	 */
	long getCurrentPosition() {
		return cis.getByteCount();
	}

}
