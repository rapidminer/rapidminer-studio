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

import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.rapidminer.operator.nio.model.xlsx.XlsxWorkbookParser.XlsxWorkbook;


/**
 * SAX parser for XLSX Workbook.
 *
 * @see "ECMA-376, 4th Edition, 18.2 Workbook (pp. 1537 ff.)"
 *
 * @author Adrian Wilke, Nils Woehler
 * @since 6.3.0
 */
public class XlsxWorkbookParser extends AbstractXlsxSAXHandler<XlsxWorkbook> {

	/**
	 * Container for XLSX workbook.
	 *
	 * @see "ECMA-376, 4th Edition, 18.2 Workbook (pp. 1537 ff.)"
	 */
	public static final class XlsxWorkbook {

		/** List of parsed Workbook Sheet elements */
		public List<XlsxWorkbookSheet> xlsxWorkbookSheets = new LinkedList<>();

		/**
		 * A boolean value that indicates whether the date systems used in the workbook starts in
		 * 1904.
		 *
		 * The default value is false, meaning that the workbook uses the 1900 date system, where
		 * 1/1/1900 is the first day in the system..
		 */
		public boolean isDate1904 = false;

	}

	/**
	 * Container for XLSX workbook sheets.
	 *
	 * @see "ECMA-376, 4th Edition, 18.2.19 sheet (pp. 1563 ff.)"
	 */
	public static final class XlsxWorkbookSheet {

		/** Sheet name (required) */
		public String name;

		/** Relationship ID (required) */
		public String rId;

		/** Sheet Tab ID (required) */
		public int sheetId;
	}

	/** Path of the embedded workbook file */
	private static final String FILE_WORKBOOK = "xl/workbook.xml";

	private static final String ATT_SHEET_ID = "sheetId";
	private static final String ATT_SHEET_NAME = "name";
	private static final String ATT_SHEET_RID = "r:id";
	private static final String TAG_SHEET = "sheet";

	/**
	 * This element defines a collection of workbook properties. <br/>
	 * [Example:
	 *
	 * <pre>
	 *  {@code
	 * <workbookPr showObjects="none" saveExternalLinkValues="0" defaultThemeVersion="123820"/>
	 *  }
	 * </pre>
	 *
	 * end example]
	 */
	private static final String TAG_WORKBOOK_PR = "workbookPr";

	/**
	 * Value that indicates whether to use a 1900 or 1904 date system when converting serial
	 * date-times in the workbook to dates. <br/>
	 * A value of 1 or true indicates the workbook uses the 1904 date system. <br/>
	 * A value of 0 or false indicates the workbook uses the 1900 date system. <br/>
	 * (See 18.17.4.1 for the definition of the date systems.) <br/>
	 * The default value for this attribute is false.
	 */
	private static final String ATT_DATE_1904 = "date1904";

	/** The container to access results */
	private final XlsxWorkbook xlsxWorkbook = new XlsxWorkbook();

	@Override
	public XlsxWorkbook getResult() {
		return xlsxWorkbook;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals(TAG_WORKBOOK_PR)) {
			String date1904 = attributes.getValue(ATT_DATE_1904);
			if (date1904 != null) {
				/*
				 * Even though the specification says only 0/1 are allowed as date1904 value some
				 * writers (e.g. Write Excel) write true/false instead so we need to check whether
				 * we have a digit or a string.
				 */
				if (date1904.length() == 1 && Character.isDigit(date1904.charAt(0))) {
					xlsxWorkbook.isDate1904 = Integer.parseInt(date1904) == 1;
				} else {
					xlsxWorkbook.isDate1904 = Boolean.parseBoolean(date1904);
				}
			}
		} else if (qName.equals(TAG_SHEET)) {
			XlsxWorkbookSheet sheet = new XlsxWorkbookSheet();
			sheet.name = attributes.getValue(ATT_SHEET_NAME);
			sheet.rId = attributes.getValue(ATT_SHEET_RID);
			String sheetId = attributes.getValue(ATT_SHEET_ID);
			if (sheetId != null) {
				sheet.sheetId = Integer.parseInt(sheetId);
			}
			xlsxWorkbook.xlsxWorkbookSheets.add(sheet);
		}
	}

	@Override
	protected String getZipEntryPath() {
		return FILE_WORKBOOK;
	}
}
