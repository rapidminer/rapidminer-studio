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

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.DateUtil;


/**
 * Stores information about XLSX cell number formats and allows to check whether a cell has a date
 * format.
 *
 * @author Nils Woehler
 * @since 6.3.0
 */
public class XlsxNumberFormats {

	/** A map that contains all number formats which are identified by a number format ID */
	private final Map<Integer, String> numberFormats = new HashMap<>();

	/** Caches whether the number format for the specified ID is a date format */
	private Map<Integer, Boolean> isDateFormatCache = new HashMap<>();

	/** An array that contains the number format index references for the specific cell style */
	private int[] cellNumberFormatIds;

	/**
	 * Initializes the cell number format IDs array with the specified size
	 *
	 * @param count
	 *            the size of the cell number format IDs
	 */
	public void initializeCellNumberFormatIds(int count) {
		cellNumberFormatIds = new int[count];
	}

	/**
	 * Sets the number format Id for the specified index.
	 *
	 * @param index
	 *            the index of the cell format
	 * @param numberFormatId
	 *            the number format ID
	 */
	public void setCellNumberFormatId(int index, int numberFormatId) {
		cellNumberFormatIds[index] = numberFormatId;
	}

	/**
	 * Adds a new number format and fills the date format cache by checking whether it is a date
	 * format.
	 */
	public void addNumberFormat(int numberFormatId, String formatCode) {
		numberFormats.put(numberFormatId, formatCode);
		isDateFormatCache.put(numberFormatId, checkForDateFormat(numberFormatId, formatCode));
	}

	private boolean checkForDateFormat(int numberFormatId, String formatCode) {
		return DateUtil.isADateFormat(numberFormatId, formatCode);
	}

	/**
	 * @param cellStyleId
	 *            the cell style ID stored within the XLSX worksheet cell tag. <code>null</code> is
	 *            allowed and will return <code>false</code>
	 * @return <code>true</code> in case it is a date, <code>false</code> otherwise
	 */
	public boolean isDateFormatStyle(String cellStyleId) {
		if (cellStyleId == null) {
			return false;
		}

		/*
		 * Cell styles references are stored within the cell tag as a 0 based index of the cell
		 * formats.
		 */
		int numberFormatId = cellNumberFormatIds[Integer.parseInt(cellStyleId)];
		Boolean isNumberFormat = isDateFormatCache.get(numberFormatId);
		if (isNumberFormat == null) {
			// Check builtin formats if custom date format cache does not contain a hit
			String builtinFormat = BuiltinFormats.getBuiltinFormat(numberFormatId);
			if (builtinFormat != null) {
				isNumberFormat = checkForDateFormat(numberFormatId, builtinFormat);
				isDateFormatCache.put(numberFormatId, isNumberFormat);
			} else {
				// It is neither a custom nor a a built-in format -> probably not a date format
				isNumberFormat = false;
			}
		}
		return isNumberFormat;
	}
}
