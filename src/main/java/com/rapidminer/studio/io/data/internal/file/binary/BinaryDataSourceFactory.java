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
package com.rapidminer.studio.io.data.internal.file.binary;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.rapidminer.core.io.data.source.FileDataSourceFactory;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.WizardStep;


/**
 * Provides a factory for binary data sources such as image files.
 *
 * @author Michael Knopf
 * @since 7.0.0
 */
public class BinaryDataSourceFactory extends FileDataSourceFactory<BinaryDataSource> {

	private static final Set<String> BINARY_MIME_TYPES = Collections.emptySet();
	private static final Set<String> BINARY_FILE_ENDINGS = Collections.emptySet();

	public BinaryDataSourceFactory() {
		super("blob", BINARY_MIME_TYPES, BINARY_FILE_ENDINGS, DumpToRepositoryStep.STEP_ID);
	}

	@Override
	public BinaryDataSource createNew() {
		return new BinaryDataSource();
	}

	@Override
	public Class<BinaryDataSource> getDataSourceClass() {
		return BinaryDataSource.class;
	}

	@Override
	public List<WizardStep> createCustomSteps(ImportWizard wizard, BinaryDataSource dataSource) {
		return Collections.singletonList((WizardStep) new DumpToRepositoryStep(dataSource, wizard));
	}

}
