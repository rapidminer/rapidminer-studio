/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.studio.io.gui.internal.steps;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.core.io.data.ParseException;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardStep;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.gui.RepositoryLocationChooser;
import com.rapidminer.studio.io.data.DataSetReader;


/**
 * {@link WizardStep} that allows to select a repository location as destination of the data import.
 * Triggers the actual import of the {@link DataSet} on success.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 *
 */
public final class StoreToRepositoryStep extends AbstractToRepositoryStep<RepositoryLocationChooser> {

	private boolean error;

	public StoreToRepositoryStep(ImportWizard wizard) {
		super(wizard);
	}

	@Override
	public String getI18NKey() {
		return ImportWizard.STORE_DATA_STEP_ID;
	}

	@Override
	protected RepositoryLocationChooser initializeChooser(String initialDestination) {
		return new RepositoryLocationChooser(null, null, initialDestination, true, false, true, true, Colors.WHITE);
	}

	@Override
	protected ProgressThread getImportThread(final RepositoryLocation entryLocation, Folder parent)
			throws InvalidConfigurationException {
		DataSource dataSource = wizard.getDataSource(DataSource.class);

		ProgressThread importThread = createImportThread(entryLocation, dataSource);
		importThread.setIndeterminate(true);
		return importThread;
	}

	/**
	 * Creates a {@link ProgressThread} that stores the dataSet at the given entryLocation.
	 *
	 * @param entryLocation
	 *            where to store the data
	 * @param metaData
	 *            the meta data to use
	 * @param dataSet
	 *            the data set, can be {@code null}
	 * @param exception
	 *            explaining why dataSet is {@code null}
	 * @return the process thread to store the data
	 */
	private ProgressThread createImportThread(final RepositoryLocation entryLocation, final DataSource dataSource) {
		error = false;
		ProgressThread importThread = new ProgressThread("import_data") {

			@Override
			public void run() {
				DataSetReader importer;
				DataSetMetaData metaData;
				try {
					metaData = dataSource.getMetadata();
					importer = new DataSetReader(null, metaData.getColumnMetaData(),
							dataSource.getMetadata().isFaultTolerant());
				} catch (DataSetException e) {
					error = true;
					SwingTools.showSimpleErrorMessage("cannot_read_data_set", e);
					return;
				}

				ExampleSet exampleSet;
				try {
					exampleSet = importer.read(dataSource.getData(), getProgressListener());
				} catch (ParseException e) {
					Integer columnIndex = e.getColumnIndex();
					String columnName = null;
					if (columnIndex != null && metaData != null && columnIndex < metaData.getColumnMetaData().size()) {
						columnName = metaData.getColumnMetaData(columnIndex).getName();
					}
					error = true;
					if (columnName != null) {
						SwingTools.showSimpleErrorMessage("cannot_parse_data_set", e, columnName, e.getMessage());
					} else {
						SwingTools.showSimpleErrorMessage("cannot_read_data_set", e);
					}
					return;
				} catch (OperatorException | DataSetException e) {
					error = true;
					SwingTools.showSimpleErrorMessage("cannot_read_data_set", e);
					return;
				}

				try {
					RepositoryManager.getInstance(null).store(exampleSet, entryLocation, null);
					// show result
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							// Select repository entry
							if (RapidMinerGUI.getMainFrame() != null) {
								RapidMinerGUI.getMainFrame().getRepositoryBrowser()
										.expandToRepositoryLocation(entryLocation);
								// Switch to result
								try {
									Entry entry = entryLocation.locateEntry();
									if (entry != null && entry instanceof IOObjectEntry) {
										OpenAction.showAsResult((IOObjectEntry) entry);
									}
								} catch (RepositoryException e) {
									SwingTools.showSimpleErrorMessage("cannot_open_imported_data", e);
								}
							}
						}
					});
				} catch (RepositoryException ex) {
					error = true;
					SwingTools.showSimpleErrorMessage("cannot_store_obj_at_location", ex, entryLocation.getPath());
					return;
				}

				getProgressListener().complete();
			}

		};
		return importThread;
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
