/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.studio.io.gui.internal;

import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.nio.file.Path;

import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.data.source.DataSourceFactory;
import com.rapidminer.core.io.data.source.FileDataSourceFactory;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.studio.io.data.internal.file.LocalFileDataSourceFactory;
import com.rapidminer.studio.io.gui.internal.steps.LocationSelectionStep;
import com.rapidminer.studio.io.gui.internal.steps.StoreToRepositoryStep;
import com.rapidminer.studio.io.gui.internal.steps.TypeSelectionStep;
import com.rapidminer.studio.io.gui.internal.steps.configuration.ConfigureDataStep;


/**
 * A builder for the {@link DataImportWizard}.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
public final class DataImportWizardBuilder {

	private LocalFileDataSourceFactory localFileDataSourceFactory;
	private FileDataSourceFactory<?> fileDataSourceFactory;
	private String startingStepID;
	private Path filePath;

	/**
	 * Builds and layouts the configured {@link ImportWizard} dialog.
	 *
	 * @param owner
	 *            the dialog owner
	 * @return the new {@link ImportWizard} instance
	 */
	public ImportWizard build(Window owner) {
		DataImportWizard wizard = new DataImportWizard(owner, ModalityType.DOCUMENT_MODAL, null);

		// add common steps
		TypeSelectionStep typeSelectionStep = new TypeSelectionStep(wizard);
		wizard.addStep(typeSelectionStep);
		wizard.addStep(new LocationSelectionStep(wizard));
		wizard.addStep(new StoreToRepositoryStep(wizard));
		wizard.addStep(new ConfigureDataStep(wizard));

		// check whether a local file data source was specified
		if (localFileDataSourceFactory != null) {
			setDataSource(wizard, localFileDataSourceFactory,
					localFileDataSourceFactory.createNew(wizard, filePath, fileDataSourceFactory));
		}

		// Start with type selection
		String startingStep = typeSelectionStep.getI18NKey();

		// unless another starting step ID is specified
		if (startingStepID != null) {
			startingStep = startingStepID;
		}

		wizard.layoutDefault(ButtonDialog.HUGE, startingStep);
		return wizard;
	}

	/**
	 * Sets the data source for the {@link ImportWizard}.
	 *
	 * @param wizard
	 *            the wizard
	 * @param factory
	 *            the factory
	 * @param dataSource
	 *            the data source or {@code null} if no {@link DataSource} instance has been created
	 *            yet
	 */
	private <D extends DataSource> void setDataSource(DataImportWizard wizard, DataSourceFactory<D> factory, D dataSource) {
		wizard.setDataSource(dataSource == null ? factory.createNew() : dataSource, factory);
	}

	/**
	 * Configures the {@link DataImportWizard} to load data from the provided file. This way the
	 * data source type is skipped.
	 *
	 * @param filePath
	 *            the path to the local file
	 * @return the builder
	 */
	public DataImportWizardBuilder forFile(Path filePath) {
		this.filePath = filePath;
		this.localFileDataSourceFactory = new LocalFileDataSourceFactory();
		this.fileDataSourceFactory = LocalFileDataSourceFactory.lookupFactory(filePath);
		if (fileDataSourceFactory != null) {
			// start with first step of the chosen file type reader
			this.startingStepID = fileDataSourceFactory.getFirstStepId();
		} else {
			// otherwise the user has to choose the file type
			this.startingStepID = LocationSelectionStep.LOCATION_SELECTION_STEP_ID;
		}
		return this;
	}

}
