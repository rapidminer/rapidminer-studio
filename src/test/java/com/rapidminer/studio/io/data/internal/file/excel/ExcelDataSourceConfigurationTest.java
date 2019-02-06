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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.source.DataSourceConfiguration;
import com.rapidminer.studio.io.data.internal.file.excel.ExcelDataSource;


/**
 * Unit tests for the {@link ExcelDataSource} class which test the configuration created by
 * {@link ExcelDataSource#getConfiguration()} and the configure method.
 *
 * @author Nils Woehler
 *
 */
public class ExcelDataSourceConfigurationTest {

	@Test
	public void testDifferentConfigurationInstances() throws DataSetException {
		try (ExcelDataSource ds = new ExcelDataSource()) {
			assertTrue(ds.getConfiguration() != ds.getConfiguration());
		}
	}

	@Test
	public void testEqualConfigurationParameters() throws DataSetException {
		try (ExcelDataSource ds = new ExcelDataSource()) {
			assertEquals(ds.getConfiguration().getParameters(), ds.getConfiguration().getParameters());
			assertEquals(ds.getConfiguration().getVersion(), ds.getConfiguration().getVersion());
		}
	}

	@Test
	public void testDifferntConfigurationParametersOnChange() throws DataSetException {
		try (ExcelDataSource ds = new ExcelDataSource()) {
			DataSourceConfiguration storedConfiguration = ds.getConfiguration();
			assertEquals(storedConfiguration.getParameters(), storedConfiguration.getParameters());

			// change data source
			Path newLocation = Paths.get("new/file/path/location");
			ds.setLocation(newLocation);

			// check that the configuration has changed
			assertThat(ds.getConfiguration().getParameters(), not(equalTo(storedConfiguration.getParameters())));
			assertEquals(newLocation.toString(), ds.getConfiguration().getParameters().get("excel.fileLocation"));
		}
	}

}
