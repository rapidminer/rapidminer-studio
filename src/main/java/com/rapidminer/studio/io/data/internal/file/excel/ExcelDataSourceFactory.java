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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.rapidminer.core.io.data.source.FileDataSourceFactory;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.WizardStep;


/**
 * The factory for {@link ExcelDataSource}s.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
public final class ExcelDataSourceFactory extends FileDataSourceFactory<ExcelDataSource> {

	private static final Set<String> EXCEL_MIME_TYPES = new HashSet<>(Arrays.asList("application/x-tika-ooxml",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel"));
	private static final Set<String> EXCEL_FILE_ENDINGS = new HashSet<>(Arrays.asList("xlsx", "xls"));

	public static final String EXCEL_DATA_SOURCE_FACTORY_I18N_KEY = "excel";


	/**
	 * Constructs a new factory instance.
	 */
	public ExcelDataSourceFactory() {
		super(EXCEL_DATA_SOURCE_FACTORY_I18N_KEY, EXCEL_MIME_TYPES, EXCEL_FILE_ENDINGS,
				ExcelSheetSelectionWizardStep.EXCEL_SHEET_SELECTION_STEP_ID);

	}

	@Override
	public List<WizardStep> createCustomSteps(ImportWizard wizard, ExcelDataSource dataSource) {
		List<WizardStep> customSteps = new LinkedList<>();
		customSteps.add(new ExcelSheetSelectionWizardStep(dataSource, wizard));
		return customSteps;
	}

	@Override
	public Class<ExcelDataSource> getDataSourceClass() {
		return ExcelDataSource.class;
	}

	@Override
	public ExcelDataSource createNew() {
		return new ExcelDataSource();
	}

}
