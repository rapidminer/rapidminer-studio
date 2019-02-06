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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.tika.Tika;

import com.rapidminer.core.io.data.source.DataSourceFactory;
import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;
import com.rapidminer.core.io.data.source.FileDataSource;
import com.rapidminer.core.io.data.source.FileDataSourceFactory;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.WizardStep;
import com.rapidminer.tools.container.Pair;


/**
 * The factory for the {@link LocalFileDataSource}.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
public final class LocalFileDataSourceFactory implements DataSourceFactory<LocalFileDataSource> {

	@Override
	public LocalFileDataSource createNew() {
		return new LocalFileDataSource();
	}

	/**
	 * Creates a new instance of the {@link LocalFileDataSource} and sets the provided path as the
	 * file location.
	 *
	 * @param wizard
	 *            the import wizard
	 * @param path
	 *            the file location for the new {@link LocalFileDataSource} instance
	 * @param factory
	 *            the {@link FileDataSourceFactory} to read the file, or {@code null} if none has
	 *            been chosen yet
	 * @return the new {@link LocalFileDataSource} instance
	 */
	public <D extends FileDataSource> LocalFileDataSource createNew(ImportWizard wizard, Path path,
			FileDataSourceFactory<D> factory) {
		LocalFileDataSource localFileDataSource = new LocalFileDataSource();
		localFileDataSource.setLocation(path);

		if (factory != null) {
			// create a file data source if factory is provided
			D fileDataSource = factory.createNew(path);

			// update the local file data source
			localFileDataSource.setFileDataSourceFactory(factory);
			localFileDataSource.setFileDataSource(fileDataSource);

			// and add file data source steps to the wizard
			for (WizardStep step : factory.createCustomSteps(wizard, fileDataSource)) {
				wizard.addStep(step);
			}
		}
		return localFileDataSource;
	}

	@Override
	public String getI18NKey() {
		return "local_file";
	}

	@Override
	public Class<LocalFileDataSource> getDataSourceClass() {
		return LocalFileDataSource.class;
	}

	@Override
	public List<WizardStep> createCustomSteps(ImportWizard wizard, LocalFileDataSource dataSource) {
		return Collections.emptyList();
	}

	@Override
	public WizardStep createLocationStep(ImportWizard wizard) {
		return createLocationStepForFactory(wizard, null);
	}

	/**
	 * Same as {@link #createLocationStep(ImportWizard)}, but for a specific factory instead of the generic import
	 * wizard with all factories
	 *
	 * @param wizard
	 * 		the wizard instance
	 * @param factoryI18NKey
	 * 		the factory i18n key. If {@code null}, behaves the same as {@link #createLocationStep(ImportWizard)}
	 * @return the new location wizard step
	 * @since 9.0.0
	 */
	public WizardStep createLocationStepForFactory(ImportWizard wizard, String factoryI18NKey) {
		List<FileDataSourceFactory<?>> factories = DataSourceFactoryRegistry.INSTANCE.getFileFactories();
		List<Pair<String, Set<String>>> selectedFileEndings = new LinkedList<>();
		for (FileDataSourceFactory<?> factory : factories) {
			if (factoryI18NKey == null || factory.getI18NKey().equals(factoryI18NKey)) {
				selectedFileEndings.add(new Pair<>(factory.getI18NKey(), factory.getFileExtensions()));
				if (factoryI18NKey != null) {
					// for specific factory, stop after hit
					break;
				}
			}
		}
		return new LocalFileLocationWizardStep(selectedFileEndings, wizard, factoryI18NKey);
	}


	/**
	 * As described in the {@link FileDataSourceFactory#getMimeTypes()} and
	 * {@link FileDataSourceFactory#getFileExtensions()} methods this method looks up the
	 * responsible {@link FileDataSourceFactory} for the provided file. It first uses {@link Tika}
	 * to look-up the MIME type of the file and checks whether a {@link FileDataSource} for the
	 * detected MIME type is available. If no {@link FileDataSource} for the selected MIME type is
	 * registered it checks whether a {@link LocalFileDataSourceFactory} is responsible for the file
	 * extension. If still no match could be found {@code null} is returned.
	 *
	 * @param filePath
	 *            the path to the file which should be imported
	 * @return the responsible {@link FileDataSourceFactory} or {@code null} if none could be found
	 */
	public static FileDataSourceFactory<?> lookupFactory(Path filePath) {
		List<FileDataSourceFactory<?>> fileDataSourceFactories = DataSourceFactoryRegistry.INSTANCE.getFileFactories();

		try {
			Tika defaultTika = new Tika();
			String mimeType = defaultTika.detect(filePath);

			// go through file data sources and check for file MIME types first
			for (FileDataSourceFactory<? extends FileDataSource> factory : fileDataSourceFactories) {
				if (factory.getMimeTypes().contains(mimeType)) {
					return factory;
				}
			}
		} catch (IOException ioEx) {
			// ignore
		}

		// In case the MIME type is unknown go through file data sources again and check for file
		// ending first
		for (FileDataSourceFactory<? extends FileDataSource> factory : fileDataSourceFactories) {
			for (String fileExtension : factory.getFileExtensions()) {
				String glob = String.format("glob:**.%s", fileExtension);
				PathMatcher matcher = FileSystems.getDefault().getPathMatcher(glob);
				if (matcher.matches(filePath)) {
					return factory;
				}
			}
		}

		return null;
	}

}
