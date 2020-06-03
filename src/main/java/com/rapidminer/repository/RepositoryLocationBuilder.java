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
package com.rapidminer.repository;

import com.rapidminer.tools.ValidationUtil;


/**
 * A builder for creating {@link RepositoryLocation}s.
 * <p>This became necessary in version 9.7 because starting from there, folders and files could share the same name in
 * the repository (think "test" folder and "test.rmp" on disk, but "test" (folder) and "test" (process) in a Core
 * repository. Call {@link #withLocationType(RepositoryLocationType)} to set the expected return type.</p>
 * <p>
 * Furthermore, it is even now possible that different entry types (say an example set and a process) share the same
 * name (prefix), since on disk it is also perfectly fine to have a "test.rmp" and a "test.rmhdf5table" file in the same
 * location. Call {@link #withExpectedDataEntryType(Class)} to set the expected (sub-)data entry type.
 * </p>
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class RepositoryLocationBuilder {

	private RepositoryLocationType locationType = RepositoryLocationType.UNKNOWN;
	private Class<? extends DataEntry> expectedDataType = DataEntry.class;
	private boolean failIfDuplicateIOObjectExists = true;


	/**
	 * Set the location type which is used to determine whether a {@link DataEntry} or a {@link Folder} is referenced by
	 * this location.
	 * <p>
	 * Optional, but <strong>must</strong> be used for all situations where it is clear whether a data entry or a folder
	 * is referenced. Without this, you risk the Core loading the wrong type if a folder and a data entry with the same
	 * name exist. Will use {@link RepositoryLocationType#UNKNOWN} if not specified.
	 * </p>
	 *
	 * @param locationType the location type, must not be {@code null}
	 * @return this builder
	 */
	public RepositoryLocationBuilder withLocationType(RepositoryLocationType locationType) {
		this.locationType = ValidationUtil.requireNonNull(locationType, "locationType");
		return this;
	}

	/**
	 * Set the expected {@link DataEntry} (sub-)type for this location. Only relevant if {@link
	 * #withLocationType(RepositoryLocationType)} is NOT {@link RepositoryLocationType#FOLDER}! If called after {@link
	 * #withLocationType(RepositoryLocationType)} (or if that is not called at all), will set the location type to
	 * {@link RepositoryLocationType#DATA_ENTRY}.
	 * <p>
	 * Optional, but <strong>must</strong> be used for all situations where it is clear that a data entry is referenced.
	 * Without this, you risk the Core loading the wrong data entry if more than one (say an example set and a process)
	 * with the same name (prefix) exist. Will use {@link DataEntry} if not specified.
	 * </p>
	 *
	 * @param expectedDataType the expected specific {@link DataEntry} (sub-)type. Can be one of {@link ProcessEntry},
	 *                         {@link IOObjectEntry}, {@link ConnectionEntry}, and either {@link BinaryEntry} (if {@link
	 *                         Repository#isSupportingBinaryEntries()} is {@code true}) or {@link BlobEntry} (for legacy
	 *                         repositories that do not support the new binary entry concept). If {@code null}, will use
	 *                         {@link DataEntry} class.
	 *                         <br>
	 *                         Note: {@link IOObjectEntry IOObjectEntries} can have subtypes, meaning in file-based
	 *                         repositories it could even happen that multiple IOObjects sit next to each other, all
	 *                         having the same prefix but with distinct suffixes. (test.ioo, test.rmhdf5table, ...) For
	 *                         the purpose of this method, this scenario is not considered if {@link
	 *                         #withFailIfDuplicateIOObjectExists(boolean)} is set to {@code true}. Even if you specify
	 *                         a specific subtype of {@link IOObjectEntry} as the expected data type, it will return the
	 *                         first {@link IOObjectEntry} it finds with the given name (aka prefix in this example).
	 *                         Because for historical reasons, {@link RepositoryLocation} only consists of a string
	 *                         which only includes the prefix of such entries, it would be impossible to later determine
	 *                         which specific subtype of an IOObject was requested. Therefore, the creation of such
	 *                         scenarios is prohibited for the user. The only scenario in which this case can happen, is
	 *                         if this state is achieved from the outside (think Git pull on versioned repositories).
	 *                         <br>
	 *                         If however {@link #withFailIfDuplicateIOObjectExists(boolean)} is set to {@code false},
	 *                         you can get more specific IOObjectEntry subtypes if the repository supports it. Also see
	 *                         {@link RepositoryLocation#locateData()}.
	 * @return this builder
	 */
	public RepositoryLocationBuilder withExpectedDataEntryType(Class<? extends DataEntry> expectedDataType) {
		// null stays with DataEntry
		if (expectedDataType == null) {
			expectedDataType = DataEntry.class;
		}

		this.expectedDataType = expectedDataType;
		// convenience, if the type is set, it has to be a data entry
		this.locationType = RepositoryLocationType.DATA_ENTRY;
		return this;
	}

	/**
	 * Sets whether the {@link RepositoryLocation#locateData()} call should fail if the expected data type is of {@link
	 * IOObjectEntry} and more than one IOObject entry exists.
	 *
	 * @param failIfDuplicateIOObjectExists if {@code true} and the expected data type is of {@link IOObjectEntry}, it
	 *                                      will check that the repository folder contains only a single {@link
	 *                                      IOObjectEntry} with the requested name (prefix). Otherwise it will throw a
	 *                                      {@link RepositoryIOObjectEntryDuplicateFoundException}. See {@link
	 *                                      RepositoryLocation#locateData()} for more information.
	 */
	public RepositoryLocationBuilder withFailIfDuplicateIOObjectExists(boolean failIfDuplicateIOObjectExists) {
		this.failIfDuplicateIOObjectExists = failIfDuplicateIOObjectExists;
		return this;
	}

	/**
	 * Creates the {@link RepositoryLocation} instance with the given settings and for a given absolute location string
	 * in the form of '//Repository/path/to/object'. Can also be used to create
	 *
	 * @param absoluteLocation the absolute location path, e.g. '//Repository/path/to/object'
	 * @return the repository location, never {@code null}
	 * @throws MalformedRepositoryLocationException if the given location is not a valid repository location string
	 */
	public RepositoryLocation buildFromAbsoluteLocation(String absoluteLocation) throws MalformedRepositoryLocationException {
		RepositoryLocation location = new RepositoryLocation(absoluteLocation, locationType);
		setupDetails(location);
		return location;
	}

	/**
	 * Creates the {@link RepositoryLocation} instance with the given settings and for a given repository and path
	 * components. These components will be concatenated by the {@link RepositoryLocation#SEPARATOR}.
	 *
	 * @param repositoryName the name of the repository, must not be {@code null} or empty
	 * @param pathComponents the path components, must not be {@code null} but can be empty. If not empty, each element
	 *                       must neither be {@code null} nor empty.
	 * @return the repository location, never {@code null}
	 * @throws MalformedRepositoryLocationException if the given repository or path components are invalid
	 */
	public RepositoryLocation buildFromPathComponents(String repositoryName, String[] pathComponents) throws MalformedRepositoryLocationException {
		RepositoryLocation location = new RepositoryLocation(repositoryName, pathComponents, locationType);
		setupDetails(location);
		return location;
	}

	/**
	 * Creates the {@link RepositoryLocation} instance with the given settings and for a given parent and child name.
	 * The child can be composed of subcomponents separated by /. Dots ("..") will resolve to the parent folder.
	 *
	 * @param parent    the repository location which is the parent folder of the child, must not be {@code null}
	 * @param childName the name of the child, must not be {@code null}
	 * @return the repository location, never {@code null}
	 * @throws MalformedRepositoryLocationException if the given parent or child name are invalid
	 */
	public RepositoryLocation buildFromParentLocation(RepositoryLocation parent, String childName) throws MalformedRepositoryLocationException {
		RepositoryLocation location = new RepositoryLocation(parent, childName, locationType);
		setupDetails(location);
		return location;
	}

	/**
	 * Sets up some additional details for a location, based on the settings of this builder instance.
	 */
	private void setupDetails(RepositoryLocation location) {
		if (locationType != RepositoryLocationType.FOLDER) {
			location.setFailIfDuplicateIOObjectExists(failIfDuplicateIOObjectExists);
			location.setExpectedDataEntryType(expectedDataType);
		}
	}
}
