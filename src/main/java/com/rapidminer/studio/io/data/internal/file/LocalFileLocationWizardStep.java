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
package com.rapidminer.studio.io.data.internal.file;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;
import com.rapidminer.core.io.data.source.FileDataSource;
import com.rapidminer.core.io.data.source.FileDataSourceFactory;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardDirection;
import com.rapidminer.core.io.gui.WizardStep;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.studio.io.gui.internal.DataImportWizardUtils;
import com.rapidminer.studio.io.gui.internal.DataWizardEventType;
import com.rapidminer.studio.io.gui.internal.steps.AbstractWizardStep;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.container.Pair;


/**
 * A step that allows to select a local file location for data import with a {@link FileDataSource}.
 * The file type is detected automatically and the responsible {@link FileDataSource} is selected.
 *
 * @author Nils Woehler
 * @since 7.0.0
 *
 */
final class LocalFileLocationWizardStep extends AbstractWizardStep {

	/**
	 * Listens for changes in the view to update the current selected path.
	 */
	private final ChangeListener changeListener = e -> fireStateChanged();

	private transient LocalFileLocationChooserView view;
	private final ImportWizard wizard;
	private String factoryI18NKey;


	/**
	 * Creates a new {@link LocalFileLocationWizardStep} instance.
	 *
	 * @param allFileEndingsAndDescriptions
	 *            all list of all registered file endings with descriptions
	 */
	public LocalFileLocationWizardStep(List<Pair<String, Set<String>>> allFileEndingsAndDescriptions, ImportWizard wizard) {
		this(allFileEndingsAndDescriptions, wizard, null);
	}

	/**
	 * Creates a new {@link LocalFileLocationWizardStep} instance, which should use only one
	 * specified factory.
	 *
	 * @param allFileEndingsAndDescriptions
	 *            all list of all registered file endings with descriptions
	 * @param factoryI18NKey
	 *            the i18n key of the factory, that should be the only usable one (optional)
	 * @since 9.0.0
	 */
	public LocalFileLocationWizardStep(List<Pair<String, Set<String>>> allFileEndingsAndDescriptions, ImportWizard wizard,
									   String factoryI18NKey) {
		this.wizard = wizard;
		List<FileFilter> fileFilters = new LinkedList<>();
		this.factoryI18NKey = factoryI18NKey;
		for (final Pair<String, Set<String>> item : allFileEndingsAndDescriptions) {
			if (item.getSecond().isEmpty()) {
				// If there are no file endings associated with this item, skip it (it is covered by
				// the 'All Files' entry).
				continue;
			}
			fileFilters.add(new FileFilter() {

				@Override
				public String getDescription() {
					StringBuilder builder = new StringBuilder();
					builder.append(I18N.getGUIMessage("gui.io.dataimport.source." + item.getFirst() + ".label"));
					builder.append(" (");

					boolean first = true;
					for (String fileEnding : item.getSecond()) {
						if (first) {
							first = false;
						} else {
							builder.append(", ");
						}
						builder.append(".");
						builder.append(fileEnding);
					}
					builder.append(")");
					return builder.toString();
				}

				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					String lowerCaseName = f.getName().toLowerCase(Locale.ENGLISH);
					for (String fileEnding : item.getSecond()) {
						if (lowerCaseName.endsWith(fileEnding)) {
							return true;
						}
					}
					return false;
				}
			});
		}

		this.view = new LocalFileLocationChooserView(fileFilters, this.factoryI18NKey);
		this.view.registerChangeListener(changeListener);

	}

	@Override
	public String getI18NKey() {
		return null;
	}

	@Override
	public LocalFileLocationChooserView getView() {
		return view;
	}

	@Override
	public void viewWillBecomeVisible(WizardDirection direction) throws InvalidConfigurationException {
		wizard.setProgress(20);

		LocalFileDataSource dataSource = wizard.getDataSource(LocalFileDataSource.class);
		Path location = dataSource.getLocation();
		if (location != null) {
			getView().setSelectedFile(location);
		}

		// in case a factory is available...
		FileDataSourceFactory<?> factory = dataSource.getFileDataSourceFactory();
		if (factory != null) {
			// ...use the provided data source file factory
			getView().setFileDataSourceFactory(factory);
		} else if (factoryI18NKey != null && !factoryI18NKey.trim().isEmpty()) {
			List<FileDataSourceFactory<?>> fileFactories = DataSourceFactoryRegistry.INSTANCE.getFileFactories();
			for (FileDataSourceFactory<?> fileFactory : fileFactories) {
				if (fileFactory.getI18NKey().equals(factoryI18NKey)) {
					getView().setFileDataSourceFactory(fileFactory);
					break;
				}
			}
		} else {
			// otherwise trigger a new file type lookup
			changeListener.stateChanged(null);
		}
	}

	@Override
	public void viewWillBecomeInvisible(WizardDirection direction) throws InvalidConfigurationException {
		FileDataSourceFactory<?> fileDataSourceFactory = getView().getFileDataSourceFactory();
		Path selectedLocation = getView().getSelectedLocation();

		LocalFileDataSource dataSource = wizard.getDataSource(LocalFileDataSource.class);
		boolean locationChanged = dataSource.getLocation() == null && selectedLocation != null
				|| dataSource.getLocation() != null && !dataSource.getLocation().equals(selectedLocation);

		FileDataSourceFactory<?> dsFileFactory = dataSource.getFileDataSourceFactory();
		boolean fileTypeChanged = dsFileFactory == null && fileDataSourceFactory != null || dsFileFactory != null
				&& !dsFileFactory.getDataSourceClass().equals(fileDataSourceFactory.getDataSourceClass());

		// update the data source location
		dataSource.setLocation(selectedLocation);

		// update the data source factory in case the location or file type has changed
		if (fileDataSourceFactory != null && (locationChanged || fileTypeChanged)) {
			updateFileDataSource(dataSource, fileDataSourceFactory, wizard, selectedLocation);
		}

		// if going on with the selected file store the last directory
		if (direction == WizardDirection.NEXT && selectedLocation != null) {
			SwingTools.storeLastDirectory(selectedLocation);
		}
	}

	@Override
	public void validate() throws InvalidConfigurationException {
		Path selectedLocation = getView().getSelectedLocation();
		if (selectedLocation == null) {
			throw new InvalidConfigurationException();
		}
		if (!Files.exists(selectedLocation)) {
			throw new InvalidConfigurationException();
		}
		if (getView().getFileDataSourceFactory() == null) {
			throw new InvalidConfigurationException();
		}
	}

	@Override
	public String getNextStepID() {
		FileDataSourceFactory<?> fileDataSourceFactory;
		try {
			fileDataSourceFactory = wizard.getDataSource(LocalFileDataSource.class).getFileDataSourceFactory();
		} catch (InvalidConfigurationException e) {
			// cannot happen, printing a stacktrace anyway
			e.printStackTrace();
			return null;
		}
		if (fileDataSourceFactory == null) {
			// return random string in case no file has been chosen yet. We cannot return null here
			// as this would indicate the end of the import wizard.
			return UUID.randomUUID().toString();
		}
		return fileDataSourceFactory.getFirstStepId();
	}

	private <F extends FileDataSource> void updateFileDataSource(LocalFileDataSource dataSource,
			FileDataSourceFactory<F> factory, ImportWizard wizard, Path selectedLocation) {

		// log selection
		DataImportWizardUtils.logStats(DataWizardEventType.FILE_DATASOURCE_SELECTED, factory.getI18NKey());

		// set file data source factory
		dataSource.setFileDataSourceFactory(factory);

		// create new file data source instance
		F fileDataSource = factory.createNew(selectedLocation);
		dataSource.setFileDataSource(fileDataSource);

		// add custom steps to the wizard
		for (WizardStep customStep : getCustomSteps(fileDataSource, factory, wizard)) {
			wizard.addStep(customStep);
		}
	}

	private static <F extends FileDataSource> List<WizardStep> getCustomSteps(F dataSource, FileDataSourceFactory<F> factory,
			ImportWizard wizard) {
		return factory.createCustomSteps(wizard, dataSource);
	}

}
