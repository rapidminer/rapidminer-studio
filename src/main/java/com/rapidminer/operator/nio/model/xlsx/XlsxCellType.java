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

/**
 * Indicates a XLSX cell's data type.
 *
 * Available data types are: <br/>
 *
 * <table border="1">
 * <tr>
 * <td>b (Boolean)</td>
 * <td>Cell containing a boolean</td>
 * </tr>
 * <tr>
 * <td>d (Date)</td>
 * <td>Cell contains a date in the ISO 8601 format.</td>
 * </tr>
 * <tr>
 * <td>e (Error)</td>
 * <td>Cell containing an error.</td>
 * </tr>
 * <tr>
 * <td>inlineStr (Inline String)</td>
 * <td>Cell containing an (inline) rich string, i.e., one not in the shared string table. If this
 * cell type is used, then the cell value is in the is element rather than the v element in the cell
 * (c element).</td>
 * </tr>
 * <tr>
 * <td>n (Number)</td>
 * <td>Cell containing a number.</td>
 * </tr>
 * <tr>
 * <td>s (Shared String)</td>
 * <td>Cell containing a shared string.</td>
 * </tr>
 * <tr>
 * <td>str (String)</td>
 * <td>Cell containing a formula string.</td>
 * </tr>
 * </table>
 *
 * @see ECMA-376, 4th Edition, 18.18.11 Cell Type (pp. 2432 ff.)
 *
 * @author Nils Woehler
 * @since 6.3.0
 *
 */
public enum XlsxCellType {

	/**
	 * A boolean cell
	 */
	BOOLEAN("b"),

	/**
	 * A date cell
	 */
	DATE("d"),

	/**
	 * Cell that contains an error
	 */
	ERROR("e"),

	/**
	 * Cell that contains a string which is not stored within the shared string table
	 */
	INLINE_STRING("inlineStr"),

	/**
	 * Cell that contains a number
	 */
	NUMBER("n"),

	/**
	 * Cell that contains a string which is stored in the shared strings table
	 */
	SHARED_STRING("s"),

	/**
	 * Cell that contains a string
	 */
	STRING("str");

	private final String identifier;

	private XlsxCellType(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * @return the {@link XlsxCellType} identifier specified by ECMA-376
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Looks up the {@link XlsxCellType} by comparing the provided identifier with the
	 * {@link XlsxCellType#identifier}.
	 *
	 * @param identifier
	 *            the identifier that should be used to lookup the {@link XlsxCellType}
	 * @param cellStyleId
	 *            the parsed cell style index. Might be <code>null</code> in case it is empty.
	 * @param numberFormats
	 *            all parsed number formats
	 * @return the referenced XslxCellType or <code>null</code> if the XslxCellType is not known.
	 */
	public static XlsxCellType getCellType(String identifier, XlsxNumberFormats numberFormats, String cellStyleId) {
		// in case no identifier is given
		if (identifier == null) {
			// check if we have a number format which seems to be a date format
			return checkDateFormat(numberFormats, cellStyleId);
		}
		for (XlsxCellType type : XlsxCellType.values()) {
			if (type.getIdentifier().equals(identifier)) {
				if (type == NUMBER) {
					// dates can also be stored as a number with cell number formatting as date
					return checkDateFormat(numberFormats, cellStyleId);
				} else {
					return type;
				}
			}
		}
		return null;
	}

	/**
	 * Checks whether the provided cell style defines a date number format.
	 *
	 * @param numberFormats
	 *            the parsed number formats
	 * @param cellStyleId
	 *            the current cell style ID. Can be <code>null</code> if none was found.
	 * @return either {@link XlsxCellType#NUMBER} or {@link XlsxCellType#DATE}
	 */
	private static XlsxCellType checkDateFormat(XlsxNumberFormats numberFormats, String cellStyleId) {
		if (numberFormats != null && numberFormats.isDateFormatStyle(cellStyleId)) {
			return DATE;
		} else {
			return NUMBER;
		}
	}

}
