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

import java.nio.file.Path;

import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.data.source.DataSourceConfiguration;
import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;
import com.rapidminer.core.io.data.source.DataSourceFeature;
import com.rapidminer.core.io.data.source.FileDataSource;
import com.rapidminer.core.io.data.source.FileDataSourceFactory;


/**
 * A {@link DataSource} that loads data from local files. It is a meta data source as actually does
 * not import the data itself but wraps {@link FileDataSource}s that are selected based on the
 * selected file type.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
class LocalFileDataSource extends FileDataSource {

	/**
	 * Key to store the file factory I18N key for the {@link DataSource} configuration.
	 */
	private static final String LOCAL_FILE_DATASOURCE_FILE_FACTORY = "local_file_datasource.file_factory";

	private Path fileLocation;
	private FileDataSource fileDataSource;
	private FileDataSourceFactory<?> fileDataSourceFactory;

	@Override
	public DataSet getData() throws DataSetException {
		if (fileDataSource != null) {
			return fileDataSource.getData();
		}
		return null;
	}

	@Override
	public void close() throws DataSetException {
		if (fileDataSource != null) {
			fileDataSource.close();
		}
	}

	/**
	 * @return the location of the file for this data source. Can be {@code null} in case no
	 *         location has been selected yet.
	 */
	@Override
	public Path getLocation() {
		return fileLocation;
	}

	/**
	 * @param selectedLocation
	 *            the new location of the file for this data source
	 */
	@Override
	public void setLocation(Path selectedLocation) {
		this.fileLocation = selectedLocation;
	}

	/**
	 * @return the {@link FileDataSource} for this {@link DataSource}. Can be {@code null} in case
	 *         no file has been selected yet.
	 */
	FileDataSource getFileDataSource() {
		return fileDataSource;
	}

	/**
	 * Updates the {@link FileDataSource} for this {@link LocalFileDataSource}.
	 *
	 * @param fileDataSource
	 *            the new {@link FileDataSource}
	 */
	void setFileDataSource(FileDataSource fileDataSource) {
		this.fileDataSource = fileDataSource;
	}

	/**
	 * @return the current {@link FileDataSourceFactory}. Might be {@code null} in case no factory
	 *         was specified yet.
	 */
	FileDataSourceFactory<?> getFileDataSourceFactory() {
		return fileDataSourceFactory;
	}

	/**
	 * Updates the {@link FileDataSourceFactory} for this {@link DataSource}.
	 *
	 * @param fileDataSourceFactory
	 *            the new factory instance
	 */
	void setFileDataSourceFactory(FileDataSourceFactory<?> fileDataSourceFactory) {
		this.fileDataSourceFactory = fileDataSourceFactory;
	}

	@Override
	public DataSet getPreview(int maxPreviewSize) throws DataSetException {
		return getFileDataSource().getPreview(maxPreviewSize);
	}

	@Override
	public DataSetMetaData getMetadata() throws DataSetException {
		return getFileDataSource().getMetadata();
	}

	@Override
	public DataSourceConfiguration getConfiguration() {
		DataSourceConfiguration configuration = fileDataSource.getConfiguration();
		configuration.getParameters().put(LOCAL_FILE_DATASOURCE_FILE_FACTORY, getFileDataSourceFactory().getI18NKey());
		return configuration;
	}

	@Override
	public void configure(DataSourceConfiguration configuration) throws DataSetException {
		String fileDatasourceKey = configuration.getParameters().get(LOCAL_FILE_DATASOURCE_FILE_FACTORY);
		for (FileDataSourceFactory<?> factory : DataSourceFactoryRegistry.INSTANCE.getFileFactories()) {
			if (factory.getI18NKey().equals(configuration.getParameters().get(fileDatasourceKey))) {
				// set factory and create new file data source instance
				setFileDataSourceFactory(factory);
				setFileDataSource(getFileDataSourceFactory().createNew());

				// remove local file data source related keys
				configuration.getParameters().remove(LOCAL_FILE_DATASOURCE_FILE_FACTORY);

				// configure instance
				getFileDataSource().configure(configuration);
				return;
			}
		}
		throw new DataSetException("Unknown file data source for key '" + fileDatasourceKey + "'");
	}

	@Override
	public boolean supportsFeature(DataSourceFeature feature) {
		return fileDataSource != null ? fileDataSource.supportsFeature(feature) : super.supportsFeature(feature);
	}
}
