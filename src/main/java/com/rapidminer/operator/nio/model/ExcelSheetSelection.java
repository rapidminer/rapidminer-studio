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
package com.rapidminer.operator.nio.model;

import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.rapidminer.operator.nio.model.xlsx.XlsxWorkbookParser;


/**
 * Sheet Selection methods
 * <p>
 * Supports selection by either name or index
 *
 * @author Jonas Wilms-Pfau
 * @since 8.1.0
 */
public interface ExcelSheetSelection {

	/**
	 * Selects the sheet by index
	 *
	 * @param index
	 * @return
	 */
	static ExcelSheetSelection byIndex(int index) {
		return new ByIndex(index);
	}

	/**
	 * Selects the sheet by name
	 *
	 * @param sheetName
	 * @return
	 */
	static ExcelSheetSelection byName(String sheetName) {
		return new ByName(sheetName);
	}

	/**
	 * Returns the selected sheet
	 *
	 * @param sheets
	 * @return the selected sheet
	 * @throws SheetNotFoundException
	 */
	XlsxWorkbookParser.XlsxWorkbookSheet selectSheetFrom(List<XlsxWorkbookParser.XlsxWorkbookSheet> sheets) throws SheetNotFoundException;

	/**
	 * Returns the selected sheet
	 *
	 * @param workbookJXL
	 * @return the selected sheet
	 * @throws SheetNotFoundException
	 */
	jxl.Sheet selectSheetFrom(jxl.Workbook workbookJXL) throws SheetNotFoundException;

	/**
	 * Returns the selected sheet
	 *
	 * @param workbook
	 * @return
	 * @throws SheetNotFoundException
	 */
	Sheet selectSheetFrom(Workbook workbook) throws SheetNotFoundException;


	/**
	 * Selects a sheet by it's index
	 */
	class ByIndex implements ExcelSheetSelection {
		private static final String ERROR_MESSAGE = "Can't select sheet number %d on a file with %d sheets.";
		private final int index;

		public ByIndex(int index) {
			this.index = index;
		}

		@Override
		public XlsxWorkbookParser.XlsxWorkbookSheet selectSheetFrom(List<XlsxWorkbookParser.XlsxWorkbookSheet> sheets) throws SheetNotFoundException {
			if (sheets.size() > index) {
				return sheets.get(index);
			}
			throw new SheetNotFoundException(ERROR_MESSAGE, index + 1, sheets.size());
		}

		@Override
		public jxl.Sheet selectSheetFrom(jxl.Workbook workbookJXL) throws SheetNotFoundException {
			if (workbookJXL.getNumberOfSheets() > index) {
				return workbookJXL.getSheet(index);
			}
			throw new SheetNotFoundException(ERROR_MESSAGE, index + 1, workbookJXL.getNumberOfSheets());
		}

		@Override
		public Sheet selectSheetFrom(Workbook workbook) throws SheetNotFoundException {
			if (workbook.getNumberOfSheets() > index) {
				return workbook.getSheetAt(index);
			}
			throw new SheetNotFoundException(ERROR_MESSAGE, index + 1, workbook.getNumberOfSheets());
		}

	}

	class ByName implements ExcelSheetSelection {

		private static final String ERROR_MESSAGE = "Can't find sheet with name \"%s\".";

		private final String name;


		public ByName(String name) {
			this.name = name;
		}

		@Override
		public XlsxWorkbookParser.XlsxWorkbookSheet selectSheetFrom(List<XlsxWorkbookParser.XlsxWorkbookSheet> sheets) throws SheetNotFoundException {
			for (XlsxWorkbookParser.XlsxWorkbookSheet sheet : sheets) {
				if (name.equals(sheet.name)) {
					return sheet;
				}
			}
			throw new SheetNotFoundException(ERROR_MESSAGE, name);
		}

		@Override
		public jxl.Sheet selectSheetFrom(jxl.Workbook workbookJXL) throws SheetNotFoundException {
			jxl.Sheet sheet = workbookJXL.getSheet(name);
			if (sheet == null) {
				throw new SheetNotFoundException(ERROR_MESSAGE, name);
			}
			return sheet;
		}

		@Override
		public Sheet selectSheetFrom(Workbook workbook) throws SheetNotFoundException {
			Sheet sheet = workbook.getSheet(name);
			if (sheet == null) {
				throw new SheetNotFoundException(ERROR_MESSAGE, name);
			}
			return sheet;
		}
	}

	/**
	 * Thrown if the sheet could not be found
	 */
	class SheetNotFoundException extends Exception {
		public SheetNotFoundException(String s) {
			super(s);
		}

		public SheetNotFoundException(String s, Object... arguments) {
			this(String.format(s, arguments));
		}
	}
}
