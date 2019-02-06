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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.model.xlsx.XlsxWorkbookParser.XlsxWorkbookSheet;
import com.rapidminer.operator.nio.model.xlsx.XlsxWorkbookRelationParser.XlsxWorkbookRel;


/**
 * SAX parser for XLSX Workbook relations.
 *
 * This parser extracts paths of files, which are included inside a XLSX archive.
 *
 * @author Adrian Wilke, Nils Woehler
 * @since 6.3.0
 */
public class XlsxWorkbookRelationParser extends AbstractXlsxSAXHandler<XlsxWorkbookRel> {

	/** Container for XLSX workbook relations. */
	static final class XlsxWorkbookRel {

		/** Path of the shared strings XML file */
		public String sharedStringsPath;

		/** Path to the styles XML file */
		public String stylesPath;

		/** Mapping of Relationship IDs and worksheet files */
		public String worksheetsPath;

	}

	/** Path of the embedded workbook relation file */
	private static final String FILE_WORKBOOK_REL = "xl/_rels/workbook.xml.rels";

	private static final String ATT_RELATIONSHIP_ID = "Id";
	private static final String ATT_RELATIONSHIP_TARGET = "Target";
	private static final String ATT_RELATIONSHIP_TYPE = "Type";
	private static final String TAG_RELATIONSHIP = "Relationship";

	/**
	 * Possible type declarations of the shared strings file.
	 *
	 * @see Apache POI project org.apache.poi.xssf.usermodel.XSSFRelation.java
	 */
	private static final Set<String> TYPES_SHARED_STRINGS = new HashSet<>();
	static {
		TYPES_SHARED_STRINGS.add("application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml");
		TYPES_SHARED_STRINGS.add("http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings");
		TYPES_SHARED_STRINGS.add("/xl/sharedStrings.xml");
	}

	/**
	 * Possible type declarations of styles files.
	 *
	 * @see Apache POI project org.apache.poi.xssf.usermodel.XSSFRelation.java
	 */
	public static final Set<String> TYPES_STYLES = new HashSet<>();
	static {
		TYPES_STYLES.add("application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml");
		TYPES_STYLES.add("http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles");
		TYPES_STYLES.add("/xl/styles.xml");
	}

	/**
	 * Possible type declarations of worksheet files.
	 *
	 * @see Apache POI project org.apache.poi.xssf.usermodel.XSSFRelation.java
	 */
	private static final Set<String> TYPES_WORKSHEET = new HashSet<>();
	static {
		TYPES_WORKSHEET.add("application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml");
		TYPES_WORKSHEET.add("http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet");
		TYPES_WORKSHEET.add("/xl/worksheets/sheet#.xml");
	}

	/** The container to access results */
	private final XlsxWorkbookRel xlsxWorkbookRel = new XlsxWorkbookRel();

	/** Map to store all parsed worksheet paths */
	private final Map<String, String> worksheetsPaths = new HashMap<>();

	/** The 0-based sheet index. */
	private final int sheetIndex;

	/** The list of parsed workbook sheets */
	private final List<XlsxWorkbookSheet> xlsxWorkbookSheets;

	/** The calling operator */
	private final Operator callingOperator;

	/** The XLSX zip file */
	private final ZipFile zipFile;

	public XlsxWorkbookRelationParser(Operator callingOperator, ZipFile zipFile, List<XlsxWorkbookSheet> xlsxWorkbookSheets,
			int sheetIndex) {
		this.callingOperator = callingOperator;
		this.zipFile = zipFile;
		this.xlsxWorkbookSheets = xlsxWorkbookSheets;
		this.sheetIndex = sheetIndex;
	}

	/**
	 * Gets the result of the parsing process.
	 *
	 * @return A container object
	 */
	@Override
	public XlsxWorkbookRel getResult() throws UserError {
		if (sheetIndex >= xlsxWorkbookSheets.size()) {
			throw new UserError(callingOperator, 953, sheetIndex + 1);
		}

		// Get name of zip entry by using the sheet relationship ID
		String worksheetPath = worksheetsPaths.get(xlsxWorkbookSheets.get(sheetIndex).rId);

		// Lookup zip entry to check if it exists
		ZipEntry worksheetZipEntry = zipFile.getEntry(worksheetPath);
		if (worksheetZipEntry == null) {
			throw new UserError(callingOperator, "xlsx_file_missing_entry", worksheetPath);
		}

		// If found worksheet file was found, set worksheet path
		xlsxWorkbookRel.worksheetsPath = worksheetPath;

		return xlsxWorkbookRel;
	}

	/**
	 * Returns a map representation of the specified attributes.
	 *
	 * @param attributes
	 *            The attributes which are parsed.
	 * @param useQualifiedNames
	 *            If <code>true</code> prefixed attribute names are used, if <code>false</code>
	 *            local names are used.
	 * @return A map with attribute names and the respective values.
	 */
	private Map<String, String> getAttributesMap(Attributes attributes, boolean useQualifiedNames) {
		Map<String, String> map = new TreeMap<>();
		for (int i = 0; i < attributes.getLength(); i++) {
			if (useQualifiedNames) {
				map.put(attributes.getQName(i), attributes.getValue(i));
			} else {
				map.put(attributes.getLocalName(i), attributes.getValue(i));
			}
		}
		return map;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals(TAG_RELATIONSHIP)) {
			Map<String, String> attributesMap = getAttributesMap(attributes, true);

			// Get the path of the shared strings
			if (attributesMap.containsKey(ATT_RELATIONSHIP_TYPE)) {
				String typeValue = attributesMap.get(ATT_RELATIONSHIP_TYPE);

				// Check if relationship is shared strings
				if (TYPES_SHARED_STRINGS.contains(typeValue)) {
					xlsxWorkbookRel.sharedStringsPath = attributesMap.get(ATT_RELATIONSHIP_TARGET);
				}

				// Check if relationship is styles
				if (TYPES_STYLES.contains(typeValue)) {
					xlsxWorkbookRel.stylesPath = attributesMap.get(ATT_RELATIONSHIP_TARGET);
				}

				// Check if relationship is worksheet
				if (TYPES_WORKSHEET.contains(typeValue)) {
					worksheetsPaths.put(attributesMap.get(ATT_RELATIONSHIP_ID), XlsxUtilities.XLSX_PATH_PREFIX
							+ attributesMap.get(ATT_RELATIONSHIP_TARGET));
				}

			} else {
				throw new SAXException("Workbook relations entry malformed. XML attribute '" + ATT_RELATIONSHIP_TYPE
						+ "' not found.");
			}
		}
	}

	@Override
	protected String getZipEntryPath() {
		return FILE_WORKBOOK_REL;
	}
}
