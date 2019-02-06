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

import java.util.Locale;

import javax.xml.stream.XMLStreamReader;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import com.rapidminer.operator.nio.ImportWizardUtils;
import com.rapidminer.tools.Tools;


/**
 * A class that contains utility methods for parsing XLSX files.
 *
 * @author Nils Woehler
 * @since 6.3.0
 */
public final class XlsxUtilities {

	/**
	 * The minimum limit for number of row shown in the new data access sheet selection panel
	 * preview table.
	 */
	private static final int SHEET_SELECTION_ROW_NUMBER_LIMIT = 5_000;

	/** @see ECMA-376, 4th Edition, 18.3.1.4 Cell (p. 1599), 18.18.7 Cell Reference (p. 2432) */
	public static final String TAG_CELL_REFERENCE = "r";

	/**
	 * Row index. Indicates to which row in the sheet this <row> definition corresponds.
	 *
	 * @see ECMA-376, 4th Edition, 18.3.1.73 Row (p. 1670)
	 */
	public static final String TAG_ROW_INDEX = "r";

	/**
	 * The element expresses information about an entire row of a worksheet, and contains all cell
	 * definitions for a particular row in the worksheet.
	 *
	 * [Example: This row expresses information about row 2 in the worksheet, and contains 3 cell
	 * definitions.
	 *
	 * <pre>
	 * {@code
	 * <row r="2" spans="2:12">
	 * 	<c r="C2" s="1">
	 * 		<f>PMT(B3/12,B4,-B5)</f>
	 * 		<v>672.68336574300008</v>
	 * 	</c>
	 * 	<c r="D2">
	 * 		<v>180</v>
	 * 	</c>
	 * 	<c r="E2">
	 * 		<v>360</v>
	 * 	</c>
	 * </row>
	 * }
	 * </pre>
	 *
	 * @see ECMA-376, 4th Edition, 18.3.1.73 Row (pp. 1667 ff.)
	 */
	public static final String TAG_ROW = "row";

	/**
	 * This collection represents a cell in the worksheet. Information about the cell's location
	 * (reference), value, data type, formatting, and formula is expressed here. <br/>
	 * <br/>
	 * [Example: This example shows the information stored for a cell whose address in the grid is
	 * C6, whose style index is '6', and whose value metadata index is '15'. The cell contains a
	 * formula as well as a calculated result of that formula.
	 *
	 * <pre>
	 * {@code
	 * <c r="C6" s="1" vm="15">
	 * 	<f>CUBEVALUE("xlextdat9 Adventure Works",C$5,$A6)</f>
	 * 	<v>2838512.355</v>
	 * </c>
	 * }
	 * </pre>
	 *
	 * end example] <br/>
	 * <br/>
	 * / While a cell can have a formula element f and a value element v, when the cell's type t is
	 * inlineStr then only the element is is allowed as a child element. <br/>
	 * <br/>
	 * [Example: Here is an example of expressing a string in the cell rather than using the shared
	 * string table.
	 *
	 * <pre>
	 * {@code
	 * <row r="1" spans="1:1">
	 * 	<c r="A1" t="inlineStr">
	 * 		<is><t>This is inline string example</t></is>
	 * 	</c>
	 * </row>
	 * }
	 * </pre>
	 *
	 * end example]
	 *
	 * @see ECMA-376, 4th Edition, 18.3.1.4 Cell (pp. 1588 ff.)
	 */
	public static final String TAG_CELL = "c";

	/** Default prefix of paths defined inside XLSX */
	public static final String XLSX_PATH_PREFIX = "xl/";

	/**
	 * Container class that contains Excel cell coordinates.
	 */
	public static final class XlsxCellCoordinates {

		/**
		 * Value used to specify that the coordinates reference a column only.
		 */
		public static final int NO_ROW_NUMBER = -1;

		/** {@code 0} based column number */
		public int columnNumber;

		/**
		 * {@code 0} based row number or {@value #NO_ROW_NUMBER} in case only a column is specified.
		 */
		public int rowNumber;

		/**
		 * Creates an {@link XlsxCellCoordinates} instance with only the column number defined. The
		 * row number is set to {@value #NO_ROW_NUMBER}.
		 *
		 * @param columnNumber
		 *            the column number
		 */
		public XlsxCellCoordinates(int columnNumber) {
			this(columnNumber, NO_ROW_NUMBER);
		}

		/**
		 * Constructs a new {@link XlsxCellCoordinates} instance with both the column and the row
		 * number defined.
		 *
		 * @param columnNumber
		 *            the column number
		 * @param rowNumber
		 *            the row number
		 */
		public XlsxCellCoordinates(int columnNumber, int rowNumber) {
			this.columnNumber = columnNumber;
			this.rowNumber = rowNumber;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + columnNumber;
			result = prime * result + rowNumber;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			XlsxCellCoordinates other = (XlsxCellCoordinates) obj;
			if (columnNumber != other.columnNumber) {
				return false;
			}
			if (rowNumber != other.rowNumber) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "XlsxCellCoordinates [columnNumber=" + columnNumber + ", rowNumber=" + rowNumber + "]";
		}

	}

	/**
	 * A simple POJO object that represents a XSLX cell which consist of a {@link XlsxCellType} and
	 * a String value.
	 */
	static final class XlsxCell {

		final XlsxCellType cellType;
		String value;

		XlsxCell(XlsxCellType cellType) {
			this.cellType = cellType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (cellType == null ? 0 : cellType.hashCode());
			result = prime * result + (value == null ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			XlsxCell other = (XlsxCell) obj;
			if (cellType != other.cellType) {
				return false;
			}
			if (value == null) {
				if (other.value != null) {
					return false;
				}
			} else if (!value.equals(other.value)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "XlsxCell [cellType=" + cellType + ", value=" + value + "]";
		}

	}

	private XlsxUtilities() {
		throw new IllegalStateException("Cannot instantiate utility class");
	}

	/**
	 * Convert given Excel column name to column index, eg. 'A=0', 'AA=26'
	 *
	 * @param columnName
	 *            the column name
	 * @return {@code 0} based index of the column
	 */
	public static int convertToColumnIndex(String columnName) {
		// ensure provided String is upper case
		columnName = columnName.toUpperCase(Locale.ENGLISH);
		int index = 0;
		for (int i = 0; i < columnName.length(); i++) {
			index *= 26;
			index += columnName.charAt(i) - 'A' + 1;
		}
		return index - 1;
	}

	/**
	 * Convert given Excel column index to column name, eg. '0=A', '26=AA'
	 *
	 * @param index
	 *            the {@code 0} based column index
	 * @return the column name
	 */
	public static String convertToColumnName(int index) {
		if (index < 0) {
			throw new IllegalArgumentException("Indices below 0 are not allowed");
		}
		return Tools.getExcelColumnName(index);
	}

	/**
	 * Converts a cell reference String of format 'A12' to a pair that contains the column number
	 * (starting with {@code 0} for the first column) and the row number (starting with {@code 0}
	 * for the first row).
	 *
	 * @param cellReference
	 *            the cell reference string that starts with letters and ends with digits (e.g.
	 *            'AA123')
	 * @return the pair with column number as first (starts with {@code 0}) and row number as second
	 *         content (starts with {@code 0}, is {@link XlsxCellCoordinates#NO_ROW_NUMBER} in case
	 *         no row number is defined)
	 */
	public static XlsxCellCoordinates convertCellRefToCoordinates(String cellReference) {
		for (int i = 0; i < cellReference.length(); i++) {
			if (Character.isDigit(cellReference.charAt(i))) {
				if (i == 0) {
					throw new IllegalArgumentException(
							"The provided cell reference does not contain any letters (" + cellReference + ")");
				}
				int columnNumber = XlsxUtilities.convertToColumnIndex(cellReference.substring(0, i));
				int rowNumber = Integer.parseInt(cellReference.substring(i, cellReference.length())) - 1;
				return new XlsxCellCoordinates(columnNumber, rowNumber);
			}
		}

		// no digits specified -> return coordinates without row number
		return new XlsxCellCoordinates(XlsxUtilities.convertToColumnIndex(cellReference));
	}

	/**
	 * Generates a list of attributes of the current XML item.
	 *
	 * The generated attributes are instantiated without namespace URI.
	 *
	 * @param reader
	 *            the {@link XMLStreamReader} to use
	 *
	 * @return The available attributes
	 */
	static Attributes getAttributes(XMLStreamReader reader) {
		AttributesImpl attributes = new AttributesImpl();
		for (int i = 0; i < reader.getAttributeCount(); i++) {
			attributes.addAttribute("", reader.getAttributeLocalName(i), reader.getAttributeName(i).toString(),
					reader.getAttributeType(i), reader.getAttributeValue(i));
		}
		return attributes;
	}

	/**
	 * @return the row limit for the read mode WIZARD_SHEET_SELECTION
	 */
	public static int getSheetSelectionLength() {
		return Math.max(ImportWizardUtils.getPreviewLength(), SHEET_SELECTION_ROW_NUMBER_LIMIT);
	}
}
