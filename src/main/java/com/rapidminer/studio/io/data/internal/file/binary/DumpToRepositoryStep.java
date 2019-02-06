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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.JPanel;

import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.WizardStep;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.internal.remote.RemoteFolder;
import com.rapidminer.studio.io.gui.internal.steps.AbstractToRepositoryStep;


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
		return new BinaryImportDestinationChooser(source, initialDestination);
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
					parent.createBlobEntry(entryLocation.getName());
					Entry newEntry = entryLocation.locateEntry();
					if (newEntry == null) {
						throw new RepositoryException("Creation of blob entry failed.");
					}
					BlobEntry blob = (BlobEntry) newEntry;
					try (FileInputStream fileInputStream = new FileInputStream(source.getLocation().toFile());
						 OutputStream outputStream = blob.openOutputStream(getChooser().getMediaType())) {
						byte[] buffer = new byte[1024 * 20];
						int length;
						while ((length = fileInputStream.read(buffer)) != -1) {
							if (isCancelled()) {
								break;
							}
							outputStream.write(buffer, 0, length);
						}
						outputStream.flush();
					}
					if (isCancelled()) {
						blob.delete();
					}
				} catch (RepositoryException | IOException e) {
					error = true;
					SwingTools.showSimpleErrorMessage(wizard.getDialog(), "import_blob_failed", e, e.getMessage());
				}
			}

		};
		importWorker.setIndeterminate(true);
		return importWorker;
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
