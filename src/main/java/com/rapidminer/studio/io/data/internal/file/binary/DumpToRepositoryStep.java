/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.logging.Level;
import javax.swing.JPanel;

import org.apache.commons.io.IOUtils;

import com.rapidminer.core.io.data.source.FileDataSource;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardDirection;
import com.rapidminer.core.io.gui.WizardStep;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.internal.remote.RemoteFolder;
import com.rapidminer.studio.io.gui.internal.steps.AbstractToRepositoryStep;
import com.rapidminer.tools.LogService;


/**
 * Custom {@link WizardStep} that allows to select a repository location as destination of the
 * binary import. Triggers the actual import on success.
 *
 * @author Michael Knopf
 */
public class DumpToRepositoryStep extends AbstractToRepositoryStep<BinaryImportDestinationChooser> {

	/** I18n key to identify this step. */
	public static final String STEP_ID = "binary.dump_to_repo";

	/** The data source (e.g., a wrapper file). */
	private final BinaryDataSource source;

	/** Flag used to set the default file name once */
	private boolean defaultFileNameInitialized;

	private boolean error;

	public DumpToRepositoryStep(BinaryDataSource source, ImportWizard wizard) {
		super(wizard);
		this.source = source;
	}

	@Override
	public String getI18NKey() {
		return STEP_ID;
	}

	@Override
	protected BinaryImportDestinationChooser initializeChooser(String initialDestination) {
		return new BinaryImportDestinationChooser(initialDestination);
	}

	@Override
	protected ProgressThread getImportThread(final RepositoryLocation entryLocation, final Folder parent) {
		error = false;
		ProgressThread importWorker = new ProgressThread("import_data") {

			@Override
			public void run() {
				try {
					String originalFilename = source.getLocation().getFileName().toString();
					if (parent instanceof RemoteFolder &&
							((RemoteFolder) parent).getRepository().isFileExtensionBlacklisted(originalFilename)) {
						throw new RepositoryException("File extension blacklisted for " + originalFilename);
					}
					BinaryEntry binEntry = parent.createBinaryEntry(entryLocation.getName());
					try (FileInputStream fileInputStream = new FileInputStream(source.getLocation().toFile());
						 OutputStream outputStream = binEntry.openOutputStream()) {
						IOUtils.copy(fileInputStream, outputStream);
					}
					if (isCancelled()) {
						binEntry.delete();
					}
				} catch (RepositoryException | IOException e) {
					error = true;
					SwingTools.showSimpleErrorMessage(wizard.getDialog(), "import_blob_failed", e, e.getMessage());
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.studio.io.data.internal.file.binary.DumpToRepositoryStep.import_failed", e);
				}
			}

		};
		importWorker.setIndeterminate(true);
		return importWorker;
	}

	@Override
	public void viewWillBecomeVisible(WizardDirection direction) throws InvalidConfigurationException {
		wizard.setProgress(100);

		if (!defaultFileNameInitialized) {
			defaultFileNameInitialized = true;
			// try to get a file location
			Path filePath = null;
			try {
				// if there is a location it comes from a FileDataSource
				filePath = wizard.getDataSource(FileDataSource.class).getLocation();
			} catch (InvalidConfigurationException e) {
				// is not a data source with a location
			}

			if (filePath != null) {
				getChooser().setRepositoryEntryName(filePath.getFileName().toString());
			}
		}
	}

	@Override
	protected Class<? extends DataEntry> getDataEntryClass() {
		return BinaryEntry.class;
	}

	@Override
	protected JPanel getContentPanel() {
		return getChooser();
	}

	@Override
	protected boolean isImportSuccess() {
		return !error;
	}
}
