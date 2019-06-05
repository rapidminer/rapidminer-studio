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
package com.rapidminer.connection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.operator.Annotations;
import com.rapidminer.repository.Repository;


/**
 * Builder for {@link ConnectionInformation}. Can create a new instance based on an existing {@link ConnectionInformation}
 * or an instance of {@link ConnectionConfiguration}.
 *
 * @author Jan Czogalla, Andreas Timm
 * @since 9.3
 */
public class ConnectionInformationBuilder {

	private boolean isUpdatable = false;

	private ConnectionConfiguration configuration;
	private ConnectionStatistics statistics;
	private Annotations annotations;
	private List<Path> libraryFiles;
	private List<Path> otherFiles;
	private Repository repository;

	/**
	 * Create a builder based on an existing {@link ConnectionInformation}
	 */
	public ConnectionInformationBuilder(ConnectionInformation original) throws IOException {
		ValidationUtil.requireNonNull(original, "original connection information");
		isUpdatable = true;
		this.configuration = new ConnectionConfigurationBuilder(original.getConfiguration()).build();
		this.statistics = new ConnectionStatisticsBuilder(original.getStatistics()).build();
		this.annotations = new Annotations(original.getAnnotations());
		this.libraryFiles = new ArrayList<>(original.getLibraryFiles());
		this.otherFiles = new ArrayList<>(original.getOtherFiles());
		this.repository = original.getRepository();
	}

	/**
	 * Create a builder based on a {@link ConnectionConfiguration}
	 *
	 * @param configuration
	 * 		the original configuration; must not be {@code null}
	 */
	public ConnectionInformationBuilder(ConnectionConfiguration configuration) {
		this.configuration = ValidationUtil.requireNonNull(configuration, "configuration");
	}

	/**
	 * Update the {@link ConnectionConfiguration} if this builder was created with {@link #ConnectionInformationBuilder(ConnectionInformation)}
	 *
	 * @param configuration
	 * 		the updated configuration; must not be {@code null}
	 */
	public ConnectionInformationBuilder updateConnectionConfiguration(ConnectionConfiguration configuration) {
		if (!isUpdatable) {
			throw new IllegalArgumentException("Cannot update a new Connection Information object");
		}
		this.configuration = ValidationUtil.requireNonNull(configuration, "configuration");
		return this;
	}

	/**
	 * Sets the repository that this connection will be saved in.
	 *
	 * @param repository
	 * 		the repository, must not be {@code null}
	 */
	public ConnectionInformationBuilder inRepository(Repository repository) {
		this.repository = ValidationUtil.requireNonNull(repository, "repository");
		return this;
	}

	/**
	 * Add/overwrite the {@link ConnectionStatistics} for the {@link ConnectionInformation}
	 *
	 * @param statistics
	 * 		the statistics; must not be {@code null}
	 */
	public ConnectionInformationBuilder withStatistics(ConnectionStatistics statistics) {
		this.statistics = ValidationUtil.requireNonNull(statistics, "statistics");
		return this;
	}

	/**
	 * Add/overwrite the library files for the {@link ConnectionInformation}. Throws an error on non-existing entries.
	 *
	 * @param libraryFiles
	 * 		the list of library files; can be {@code null} or empty; all non-{@code null} elements must exist
	 */
	public ConnectionInformationBuilder withLibraryFiles(List<Path> libraryFiles) {
		libraryFiles = ValidationUtil.stripToEmptyList(libraryFiles);
		List<Path> nonExistent = libraryFiles.stream().filter(path -> !Files.exists(path)).collect(Collectors.toList());
		if (!nonExistent.isEmpty()) {
			throw new IllegalArgumentException("Non-existing paths found: " + nonExistent);
		}
		this.libraryFiles = libraryFiles;
		return this;
	}

	/**
	 * Add/overwrite the general files for the {@link ConnectionInformation}. Throws an error on non-existing entries.
	 *
	 * @param otherFiles
	 * 		the list of other files; can be {@code null} or empty; all non-{@code null} elements must exist
	 */
	public ConnectionInformationBuilder withOtherFiles(List<Path> otherFiles) {
		otherFiles = ValidationUtil.stripToEmptyList(otherFiles);
		List<Path> nonExistent = otherFiles.stream().filter(path -> !Files.exists(path)).collect(Collectors.toList());
		if (!nonExistent.isEmpty()) {
			throw new IllegalArgumentException("Non-existing paths found: " + nonExistent);
		}
		this.otherFiles = otherFiles;
		return this;
	}

	/**
	 * Add these {@link Annotations} already.
	 *
	 * @param annotations
	 * 		to be added to this {@link ConnectionInformation}
	 */
	public ConnectionInformationBuilder withAnnotations(Annotations annotations) {
		this.annotations = annotations;
		return this;
	}

	/**
	 * Creates the final {@link ConnectionInformation} with the content added through this builder.
	 */
	public ConnectionInformation build() {
		ConnectionInformationImpl ci = new ConnectionInformationImpl();
		ci.configuration = configuration;
		if (statistics != null) {
			ci.statistics = statistics;
		}
		if (libraryFiles != null) {
			ci.libraryFiles = libraryFiles;
		}
		if (otherFiles != null) {
			ci.otherFiles = otherFiles;
		}
		if (annotations != null) {
			ci.annotations = annotations;
		}
		if (repository != null) {
			ci.repository = repository;
		}
		return ci;
	}


}
