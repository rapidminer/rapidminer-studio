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
package com.rapidminer.studio.io.data.internal.file.excel;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.rapidminer.studio.io.data.internal.file.excel.ExcelDataSource;


/**
 * Unit tests for the {@link ExcelDataSource#getData()} method with XLSX files.
 *
 * @author Nils Woehler
 *
 */
public class XlsxDataSourceDataTest extends AbstractExcelDataSourceDataTest {

	// remember system locale
	private static Locale systemLocale = Locale.getDefault();

	@BeforeClass
	public static void setup() throws URISyntaxException, IOException {
		testFile = new File(XlsxDataSourceDataTest.class.getResource("test.xlsx").toURI());
		nominalDateTestFile = new File(XlsxDataSourceDataTest.class.getResource("nominal_date_1.xlsx").toURI());
		dateDateTestFile = new File(XlsxDataSourceDataTest.class.getResource("date_date_1.xlsx").toURI());

		// we need to set the local as otherwise test results might differ depending on the system
		// local running the test
		Locale.setDefault(Locale.ENGLISH);
	}

	@AfterClass
	public static void tearDown() {
		// restore system locale
		Locale.setDefault(systemLocale);
	}

	@Override
	protected String getCategoricalBelowOne() {
		return "0.2";
	}

	@Override
	protected String getCategoricalInteger() {
		return "3";
	}

	@Override
	protected long getDataToDateRow20() {
		return 1486020287911L;
	}

	@Override
	protected long getDateToDateRow50() {
		return 1465640687911L;
	}

	@Override
	protected void configureDataSource(ExcelDataSource dataSource) {
		// Noop
	}

}
