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
package com.rapidminer.studio.io.data.internal.file.csv;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.rapidminer.core.io.data.source.FileDataSourceFactory;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.WizardStep;


/**
 * The factory for {@link CSVDataSource}s.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
public final class CSVDataSourceFactory extends FileDataSourceFactory<CSVDataSource> {

	private static final Set<String> CSV_MIME_TYPES = new HashSet<>(
			Arrays.asList("text/csv", "text/tab-separated-values", "text/plain"));
	private static final Set<String> CSV_FILE_ENDINGS = new HashSet<>(Arrays.asList("csv", "tsv"));

	public static final String CSV_DATA_SOURCE_FACTORY_I18N_KEY = "csv";

	/**
	 * Constructs a new factory instance.
	 */
	public CSVDataSourceFactory() {
		super(CSV_DATA_SOURCE_FACTORY_I18N_KEY, CSV_MIME_TYPES, CSV_FILE_ENDINGS, CSVFormatSpecificationWizardStep.CSV_FORMAT_SPECIFICATION_STEP_ID);
	}

	@Override
	public List<WizardStep> createCustomSteps(ImportWizard wizard, CSVDataSource dataSource) {
		List<WizardStep> customSteps = new LinkedList<>();
		customSteps.add(new CSVFormatSpecificationWizardStep(dataSource, wizard));
		return customSteps;
	}

	@Override
	public Class<CSVDataSource> getDataSourceClass() {
		return CSVDataSource.class;
	}

	@Override
	public CSVDataSource createNew() {
		return new CSVDataSource();
	}

}
