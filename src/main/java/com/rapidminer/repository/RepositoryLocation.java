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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.rapidminer.Process;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.versioned.FilesystemRepositoryAdapter;
import com.rapidminer.tools.ValidationUtil;


/**
 * A location in a repository. It consists of a main part, which is the path to the location, in the form of
 * "//Repository/path/to/object". Up until version 9.7, this path was guaranteed to be unique, but starting with 9.7, it
 * no longer is. See {@link RepositoryLocationBuilder}. Therefore, additional properties have been added to the
 * repository location:
 * <br>
 * 1) {@link #getLocationType()} which determines whether this location references a {@link
 * RepositoryLocationType#FOLDER}, a {@link RepositoryLocationType#DATA_ENTRY}, or that is unknown ({@link
 * RepositoryLocationType#UNKNOWN}).
 *
 * <br>
 * 2) {@link #getExpectedDataEntryType()} if it references data, the {@link DataEntry} (sub-)type can be specified. For
 * all intents and purposes, these will be the 4 main types: {@link ProcessEntry}, {@link IOObjectEntry}, {@link
 * ConnectionEntry}, and finally the new {@link BinaryEntry} (and for legacy repositories the deprecated {@link
 * BlobEntry}.
 * <p>
 * Create an instance via the {@link RepositoryLocationBuilder}.
 *
 * @author Simon Fischer, Adrian Wilke, Marco Boeck
 */
public class RepositoryLocation {

	public static final char SEPARATOR = '/';
	public static final String REPOSITORY_PREFIX = "//";
	public static final String[] BLACKLISTED_STRINGS = new String[]{"/", "\\", ":", "<", ">", "*", "?", "\"", "|"};
	private static final String SEPARATOR_CHAR = String.valueOf(SEPARATOR);
	private static final String GIT_FOLDER = ".git";
	private final String[] path;
	private String repositoryName;
	private RepositoryAccessor accessor;
	private RepositoryLocationType locationType;
	// only relevant if locationType != RepositoryLocationType.FOLDER
	private Class<? extends DataEntry> expectedDataEntryType = DataEntry.class;
	private boolean failIfDuplicateIOObjectExists = true;


	/**
	 * Constructs a RepositoryLocation from a string of the form //Repository/path/to/object.
	 *
	 * @deprecated since 9.7, use {@link RepositoryLocationBuilder#buildFromAbsoluteLocation(String)} instead!
	 */
	@Deprecated
	public RepositoryLocation(String absoluteLocation) throws MalformedRepositoryLocationException {
		this(absoluteLocation, RepositoryLocationType.UNKNOWN);
	}

	/**
	 * Creates a RepositoryLocation for a given repository and a set of path components which will be concatenated by a
	 * /.
	 *
	 * @deprecated since 9.7, use {@link RepositoryLocationBuilder#buildFromPathComponents(String, String[])} instead!
	 */
	@Deprecated
	public RepositoryLocation(String repositoryName, String[] pathComponents) throws MalformedRepositoryLocationException {
		this(repositoryName, pathComponents, RepositoryLocationType.UNKNOWN);
	}

	/**
	 * Appends a child entry to a given parent location. Child can be composed of subcomponents separated by /. Dots
	 * ("..") will resolve to the parent folder.
	 *
	 * @deprecated since 9.7, use {@link RepositoryLocationBuilder#buildFromParentLocation(RepositoryLocation, String)}
	 * instead!
	 */
	@Deprecated
	public RepositoryLocation(RepositoryLocation parent, String childName) throws MalformedRepositoryLocationException {
		this(parent, childName, RepositoryLocationType.UNKNOWN);
	}

	RepositoryLocation(String absoluteLocation, RepositoryLocationType locationType) throws MalformedRepositoryLocationException {
		if (absoluteLocation == null) {
			throw new MalformedRepositoryLocationException("absoluteLocation must not be null!");
		}
		this.locationType = ValidationUtil.requireNonNull(locationType, "locationType");
		if (isAbsolute(absoluteLocation)) {
			this.path = initializeFromAbsoluteLocation(absoluteLocation);
		} else {
			repositoryName = null;
			this.path = initializeAbsolutePath(absoluteLocation);
		}
	}

	RepositoryLocation(String repositoryName, String[] pathComponents, RepositoryLocationType locationType) throws MalformedRepositoryLocationException {
		this.locationType = ValidationUtil.requireNonNull(locationType, "locationType");
		// actually check submitted parameters
		if (repositoryName == null || repositoryName.isEmpty()) {
			throw new MalformedRepositoryLocationException("repositoryName must not contain null or empty!");
		}
		if (pathComponents == null) {
			throw new MalformedRepositoryLocationException("pathComponents must not be null!");
		}
		for (String pathComp : pathComponents) {
			if (pathComp == null || pathComp.isEmpty()) {
				throw new MalformedRepositoryLocationException("path must not contain null or empty strings!");
			}
		}

		this.repositoryName = repositoryName;
		this.path = pathComponents;
	}

	RepositoryLocation(RepositoryLocation parent, String childName, RepositoryLocationType locationType) throws MalformedRepositoryLocationException {
		this.locationType = ValidationUtil.requireNonNull(locationType, "locationType");
		this.accessor = parent.accessor;
		if (isAbsolute(childName)) {
			this.path = initializeFromAbsoluteLocation(childName);
		} else if (childName.startsWith(SEPARATOR_CHAR)) {
			this.repositoryName = parent.repositoryName;
			this.path = initializeAbsolutePath(childName);
		} else {
			this.repositoryName = parent.repositoryName;
			String[] components = childName.split(SEPARATOR_CHAR);

			// skip empty path components
			LinkedList<String> newComponents = new LinkedList<>();
			for (String pathComp : parent.path) {
				if (pathComp != null && !pathComp.isEmpty()) {
					newComponents.add(pathComp);
				}
			}

			for (String component : components) {
				if ("..".equals(component)) {
					if (!newComponents.isEmpty()) {
						newComponents.removeLast();
					}
					// If we have more ../ than folder levels we can go up, we would end up outside of our repository.
					// Usually caused by copying from a deeper structure to a less nested structure
					// Even though this is technically an incorrect path, we gracefully ignore it and thus never exceed the repository top level
				} else if (!".".equals(component)) {
					newComponents.add(component);
				}
			}
			this.path = newComponents.toArray(new String[0]);
		}
	}

	private String[] initializeFromAbsoluteLocation(String absoluteLocation) throws MalformedRepositoryLocationException {
		if (!isAbsolute(absoluteLocation)) {
			throw new MalformedRepositoryLocationException(
					"Repository location '"
							+ absoluteLocation
							+ "' is not absolute! Absolute repository locations look for example like this: '//Repository/path/to/object'.");
		}

		String tmp = absoluteLocation.substring(2);
		int nextSlash = tmp.indexOf(SEPARATOR);
		if (nextSlash != -1) {
			repositoryName = tmp.substring(0, nextSlash);
		} else {
			throw new MalformedRepositoryLocationException("Malformed repositoy location '" + absoluteLocation
					+ "': path component missing.");
		}
		return initializeAbsolutePath(tmp.substring(nextSlash));
	}

	private String[] initializeAbsolutePath(String path) throws MalformedRepositoryLocationException {
		if (!path.startsWith(SEPARATOR_CHAR)) {
			throw new MalformedRepositoryLocationException("No absolute path: '" + path
					+ "'. Absolute paths look e.g. like this: '/path/to/object'.");
		}
		path = path.substring(1);
		return path.split(SEPARATOR_CHAR);
	}

	/** Returns the absolute location of this {@link RepositoryLocation}. */
	public String getAbsoluteLocation() {
		return REPOSITORY_PREFIX + repositoryName + getPath();
	}

	/**
	 * Returns the repository associated with this location.
	 *
	 * @throws RepositoryException
	 */
	public Repository getRepository() throws RepositoryException {
		return RepositoryManager.getInstance(getAccessor()).getRepository(repositoryName);
	}

	/** Returns the name of the repository associated with this location. */
	public String getRepositoryName() {
		return repositoryName;
	}

	/** Returns the path within the repository. */
	public String getPath() {
		StringBuilder builder = new StringBuilder(path.length * 8);
		for (String p : path) {
			builder.append(SEPARATOR).append(p);
		}
		return builder.toString();
	}

	/**
	 * Locates a folder in the repository. Returns {@code null} if it there is no folder under this location. An
	 * exception is thrown if this location is invalid.
	 *
	 * @return the {@link Folder} or {@code null} if it does not exist
	 * @throws RepositoryException if something goes wrong or if {@link #getLocationType()} is {@link
	 *                             RepositoryLocationType#DATA_ENTRY}.
	 * @since 9.7
	 */
	public Folder locateFolder() throws RepositoryException {
		// check to find errors early
		switch (getLocationType()) {
			case DATA_ENTRY:
				throw new RepositoryException("Cannot locate folder for a location with location type DATA_ENTRY");
			case FOLDER:
			case UNKNOWN:
			default:
				// all good, we only want the data case to fail
				break;
		}

		Repository repo = getRepository();
		if (repo != null) {
			return repo.locateFolder(getPath());
		} else {
			return null;
		}
	}

	/**
	 * Locates data in the repository. Returns {@code null} if it there is no data under this location. An exception is
	 * thrown if this location is invalid.
	 * <p>
	 * Note: If multiple data entries of different types but with the same name (prefix) exist, will return the first
	 * one to be found if the expected type is not specified! Call {@link #setExpectedDataEntryType(Class)} to specify
	 * the requested (sub-)data type for this location. Defaults to {@link DataEntry}, meaning the first data entry to
	 * be found will be returned (that might be a process, an example set, ...)
	 * </p>
	 * <br>
	 * Note: {@link IOObjectEntry IOObjectEntries} can have subtypes, meaning in file-based repositories it could even
	 * happen that multiple IOObjects sit next to each other, all having the same prefix but with distinct suffixes.
	 * (test.ioo, test.rmhdf5table, ...) For the purpose of this method, this scenario is not considered hereif {@link
	 * #isFailIfDuplicateIOObjectExists()} is set to {@code true}. Even if you specify a specific subtype of {@link
	 * IOObjectEntry} as the expected data type, it will return the first {@link IOObjectEntry} it finds with the given
	 * name (aka prefix in this example). Because for historical reasons, {@link RepositoryLocation} only consists of a
	 * string which only includes the prefix of such entries, it would be impossible to later determine which specific
	 * subtype of an IOObject was requested. Therefore, the creation of such scenarios is prohibited for the user. The
	 * only scenario in which this case can happen, is if this state is achieved from the outside (think Git pull on
	 * versioned repositories).
	 * <br>
	 * If however {@link #isFailIfDuplicateIOObjectExists()} is {@code false}, you can get more specific IOObjectEntry
	 * subtypes if the repository supports it.
	 *
	 * @return the {@link DataEntry} or {@code null} if it does not exist
	 * @throws RepositoryIOObjectEntryDuplicateFoundException if the expectedDataType is of type {@link IOObjectEntry}
	 *                                                        AND more than one exists with the same name (prefix) AND
	 *                                                        {@link #setFailIfDuplicateIOObjectExists(boolean)} was set
	 *                                                        to {@code true}
	 * @throws RepositoryException                            if something goes wrong or if {@link #getLocationType()}
	 *                                                        is {@link RepositoryLocationType#FOLDER}.
	 * @since 9.7
	 */
	@SuppressWarnings("unchecked")
	public <T extends DataEntry> T locateData() throws RepositoryException {
		// check to find errors early
		switch (getLocationType()) {
			case FOLDER:
				throw new RepositoryException("Cannot locate data entry for a location with location type FOLDER");
			case DATA_ENTRY:
			case UNKNOWN:
			default:
				// all good, we only want the folder case to fail
				break;
		}

		Repository repo = getRepository();
		if (repo != null) {
			return (T) repo.locateData(getPath(), getExpectedDataEntryType(), isFailIfDuplicateIOObjectExists());
		} else {
			return null;
		}
	}

	/**
	 * Locates the corresponding entry in the repository. It returns null if it doesn't exist yet. An exception is
	 * thrown if this location is invalid.
	 *
	 * @throws RepositoryException If repository can not be found or entry can not be located.
	 * @deprecated since 9.7, because it cannot distinguish between folders and files. Use {@link #locateFolder()} or
	 * {@link #locateData()} instead!
	 */
	@Deprecated
	public Entry locateEntry() throws RepositoryException {
		switch (getLocationType()) {
			case DATA_ENTRY:
				return locateData();
			case FOLDER:
				return locateFolder();
			case UNKNOWN:
			default:
				Entry entry = locateData();
				if (entry != null) {
					return entry;
				}
				return locateFolder();
		}
	}

	/** Returns the last path component. */
	public String getName() {
		if (path.length > 0) {
			return path[path.length - 1];
		} else {
			return null;
		}
	}

	public RepositoryLocation parent() {
		if (path.length == 0) {
			// we are at a root
			return null;
		} else {
			String[] pathCopy = new String[path.length - 1];
			System.arraycopy(path, 0, pathCopy, 0, path.length - 1);
			RepositoryLocation parent;
			try {
				parent = new RepositoryLocationBuilder().withLocationType(RepositoryLocationType.FOLDER).buildFromPathComponents(this.repositoryName, pathCopy);
			} catch (MalformedRepositoryLocationException e) {
				throw new RuntimeException(e);
			}
			parent.setAccessor(accessor);
			return parent;
		}

	}

	@Override
	public String toString() {
		return getAbsoluteLocation();
	}

	/**
	 * Assume absoluteLocation == "//MyRepos/foo/bar/object" and
	 * relativeToFolder=//MyRepos/foo/baz/, then this method will return "../bar/object".
	 * <p>
	 * Assume absoluteLocation == "//MyRepos/Connections/connection" and
	 * relativeToFolder=//MyRepos/foo/baz/, then this method will return
	 * "/Connections/connection".
	 *
	 * @see Folder#CONNECTION_FOLDER_NAME
	 */
	public String makeRelative(RepositoryLocation relativeToFolder) {
		// can only do something if repositories match.
		if (!this.repositoryName.equals(relativeToFolder.repositoryName)) {
			return getAbsoluteLocation();
		}

		if (isConnectionPath()) {
			return getPath();
		}

		int min = Math.min(this.path.length, relativeToFolder.path.length);
		// find common prefix
		int i = 0;
		while (i < min && this.path[i].equals(relativeToFolder.path[i])) {
			i++;
		}
		StringBuilder result = new StringBuilder();
		// add one ../ for each excess component in relativeComponent which we have to leave
		for (int j = i; j < relativeToFolder.path.length; j++) {
			result.append("..");
			result.append(RepositoryLocation.SEPARATOR);
		}
		// add components from each excess absoluteComponent
		for (int j = i; j < this.path.length; j++) {
			result.append(this.path[j]);
			if (j < this.path.length - 1) {
				result.append(RepositoryLocation.SEPARATOR);
			}
		}
		return result.toString();
	}

	/**
	 * Creates this folder and its parents.
	 *
	 * @throws RepositoryException
	 */
	public Folder createFoldersRecursively() throws RepositoryException {
		Folder entry = locateFolder();
		if (entry == null) {
			Folder parentFolder = parent().createFoldersRecursively();
			try {
				entry = parentFolder.createFolder(getName());
			} catch (RepositoryException re) {
				//Recover from concurrent createFolder calls
				entry = locateFolder();
				//Rethrow the RepositoryException if recovery failed
				if (entry == null) {
					throw re;
				}
			}
		}

		return entry;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RepositoryLocation location = (RepositoryLocation) o;
		// we don't care about failIfDuplicateIOObjectExists here because it's an external behavior feature flag, not a location-defining item
		return Arrays.equals(path, location.path) &&
				repositoryName.equals(location.repositoryName) &&
				locationType == location.locationType &&
				expectedDataEntryType.equals(location.expectedDataEntryType);
	}

	@Override
	public int hashCode() {
		// we don't care about failIfDuplicateIOObjectExists here because it's an external behavior feature flag, not a location-defining item
		int result = Objects.hash(repositoryName, locationType, expectedDataEntryType);
		result = 31 * result + Arrays.hashCode(path);
		return result;
	}

	public void setAccessor(RepositoryAccessor accessor) {
		this.accessor = accessor;
	}

	public RepositoryAccessor getAccessor() {
		return accessor;
	}

	/**
	 * Checks whether this {@link RepositoryLocation} is located in the
	 * {@link Folder#isConnectionsFolderName(String, boolean) connection folder} of the repository.
	 * <p>
	 * <strong>Note:</strong> This method might depend on the repository implementation. Currently the only allowed
	 * repository w.r.t. case insensitivity is the {@link LocalRepository} or the {@link FilesystemRepositoryAdapter}
	 *
	 * @return if this location represents a connection path entry or not
	 * @see com.rapidminer.repository.local.SimpleFolder#isConnectionsFolderName(String, boolean) SimpleFolder.isConnectionsFolderName(String, boolean)
	 * @since 9.3.1
	 */
	public boolean isConnectionPath() {
		try {
			return path.length == 2 && Folder.isConnectionsFolderName(path[0],
					repositoryName == null || !(getRepository() instanceof LocalRepository || getRepository() instanceof FilesystemRepositoryAdapter));
		} catch (RepositoryException e) {
			return false;
		}
	}

	/**
	 * Returns the location type referenced by this location.
	 *
	 * @return the location type, never {@code null}
	 * @since 9.7
	 */
	public RepositoryLocationType getLocationType() {
		return locationType;
	}

	/**
	 * Sets the {@link RepositoryLocationType} for this location.
	 *
	 * @param locationType the location type, must not be {@code null}
	 * @since 9.7
	 */
	public void setLocationType(RepositoryLocationType locationType) {
		this.locationType = ValidationUtil.requireNonNull(locationType, "locationType");
	}

	/**
	 * Returns the expected {@link DataEntry} (sub-)type referenced by this location. Only relevant if {@link
	 * #getLocationType()} is NOT {@link RepositoryLocationType#FOLDER}! If not specified, will default to {@link
	 * DataEntry}.
	 *
	 * @return the expected data type, never {@code null}
	 * @since 9.7
	 */
	public Class<? extends DataEntry> getExpectedDataEntryType() {
		return expectedDataEntryType;
	}

	/**
	 * Sets the expected {@link DataEntry} (sub-)type for this location. Only relevant if {@link #getLocationType()} is
	 * NOT {@link RepositoryLocationType#FOLDER}!
	 *
	 * @param expectedDataType the expected specific {@link DataEntry} (sub-)type. Can be one of {@link ProcessEntry},
	 *                         {@link IOObjectEntry}, {@link ConnectionEntry}, and either {@link BinaryEntry} (if {@link
	 *                         Repository#isSupportingBinaryEntries()} is {@code true}) or {@link BlobEntry} (for legacy
	 *                         repositories that do not support the new binary entry concept).
	 *                         <br>
	 *                         Note: {@link IOObjectEntry IOObjectEntries} can have subtypes, meaning in file-based
	 *                         repositories it could even happen that multiple IOObjects sit next to each other, all
	 *                         having the same prefix but with distinct suffixes. (test.ioo, test.rmhdf5table, ...) For
	 *                         the purpose of this method, this scenario is not considered if {@link
	 *                         #isFailIfDuplicateIOObjectExists()} is set to {@code true}. Even if you specify a
	 *                         specific subtype of {@link IOObjectEntry} as the expected data type, it will return the
	 *                         first {@link IOObjectEntry} it finds with the given name (aka prefix in this example).
	 *                         Because for historical reasons, {@link RepositoryLocation} only consists of a string
	 *                         which only includes the prefix of such entries, it would be impossible to later determine
	 *                         which specific subtype of an IOObject was requested. Therefore, the creation of such
	 *                         scenarios is prohibited for the user. The only scenario in which this case can happen, is
	 *                         if this state is achieved from the outside (think Git pull on versioned repositories).
	 *                         <br>
	 *                         If however {@link #isFailIfDuplicateIOObjectExists()} is {@code false}, you can get
	 *                         more specific IOObjectEntry subtypes if the repository supports it.
	 * @since 9.7
	 */
	public void setExpectedDataEntryType(Class<? extends DataEntry> expectedDataType) {
		this.expectedDataEntryType = ValidationUtil.requireNonNull(expectedDataType, "expectedDataType");

		// make sure that anything more specific than IOObjectEntry is reverted to IOObjectEntry itself. See JD above.
		if (isFailIfDuplicateIOObjectExists() && IOObjectEntry.class.isAssignableFrom(expectedDataType)) {
			this.expectedDataEntryType = IOObjectEntry.class;
		}
	}

	/**
	 * Whether the {@link #locateData()} call should fail if the expected data type is of {@link IOObjectEntry} and more
	 * than one IOObject entry exists or not. Defaults to {@code true}.
	 *
	 * @return {@code true} if the call should fail; {@code false} otherwise.
	 * @since 9.7
	 */
	public boolean isFailIfDuplicateIOObjectExists() {
		return failIfDuplicateIOObjectExists;
	}

	/**
	 * Sets whether the {@link #locateData()} call should fail if the expected data type is of {@link IOObjectEntry} and
	 * more than one IOObject entry exists.
	 *
	 * @param failIfDuplicateIOObjectExists if {@code true} and the expected data type is of {@link IOObjectEntry}, it
	 *                                      will check that the repository folder contains only a single {@link
	 *                                      IOObjectEntry} with the requested name (prefix). Otherwise it will throw a
	 *                                      {@link RepositoryIOObjectEntryDuplicateFoundException}. See {@link
	 *                                      RepositoryLocation#locateData()} for more information.
	 * @since 9.7
	 */
	public void setFailIfDuplicateIOObjectExists(boolean failIfDuplicateIOObjectExists) {
		this.failIfDuplicateIOObjectExists = failIfDuplicateIOObjectExists;
	}

	/**
	 * Checks whether the given location string is located in the
	 * {@link Folder#isConnectionsFolderName(String, boolean) connection folder} of the repository.
	 * <p>
	 * <strong>Note:</strong> This method might depend on the repository implementation. Currently the only allowed
	 * repository w.r.t. case insensitivity is the {@link LocalRepository} or the {@link FilesystemRepositoryAdapter}.
	 *
	 * @return if the given location represents a connection path entry or not
	 * @see com.rapidminer.repository.local.SimpleFolder#isConnectionsFolderName(String, boolean) SimpleFolder.isConnectionsFolderName(String, boolean)
	 * @since 9.3.1
	 */
	public static boolean isConnectionPath(String loc) {
		try {
			return new RepositoryLocationBuilder().withExpectedDataEntryType(ConnectionEntry.class).buildFromAbsoluteLocation(loc).isConnectionPath();
		} catch (MalformedRepositoryLocationException e) {
			return false;
		}
	}

	public static boolean isAbsolute(String loc) {
		return loc.startsWith(RepositoryLocation.REPOSITORY_PREFIX);
	}

	/**
	 * Checks if the given name is valid as a repository entry. Checks against a blacklist of
	 * characters.
	 *
	 * @param name
	 * @return
	 */
	public static boolean isNameValid(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name must not be null!");
		}
		if (name.trim().isEmpty()) {
			return false;
		}
		// since 9.x we forbid .git as it is the name of the git folder
		if (GIT_FOLDER.equals(name.trim())) {
			return false;
		}

		for (String forbiddenString : BLACKLISTED_STRINGS) {
			if (name.contains(forbiddenString)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the sub{@link String} in the given name which is invalid or <code>null</code> if
	 * there are no illegal characters.
	 *
	 * @return
	 */
	public static String getIllegalCharacterInName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return null;
		}
		// since 9.x we forbid .git as it is the name of the git folder
		if (GIT_FOLDER.equals(name.trim())) {
			return GIT_FOLDER;
		}

		for (String forbiddenString : BLACKLISTED_STRINGS) {
			if (name.contains(forbiddenString)) {
				return forbiddenString;
			}
		}
		return null;
	}

	/**
	 * Removes locations from list, which are already included in others (where others must be {@link RepositoryLocationType#FOLDER}).
	 *
	 * Example: [/1/2/3, /1, /1/2] becomes [/1]
	 */
	public static List<RepositoryLocation> removeIntersectedLocations(List<RepositoryLocation> repositoryLocations) {
		return removeIntersectedLocations((Collection<RepositoryLocation>) repositoryLocations);
	}

	/**
	 * Removes locations from list, which are already included in others (where others must be {@link RepositoryLocationType#FOLDER}).
	 *
	 * Example: [/1/2/3, /1, /1/2] becomes [/1]
	 *
	 * @param repositoryLocations
	 * 		the collection of repository locations
	 * @return a filtered list of repository locations
	 * @since 9.4
	 */
	public static List<RepositoryLocation> removeIntersectedLocations(Collection<RepositoryLocation> repositoryLocations) {
		List<RepositoryLocation> locations = new LinkedList<>(repositoryLocations);
		Iterator<RepositoryLocation> iterator = locations.iterator();
		while (iterator.hasNext()) {
			RepositoryLocation locationA = iterator.next();
			for (RepositoryLocation locationB : locations) {
				boolean checkIntersection;
				switch (locationB.getLocationType()) {
					case DATA_ENTRY:
						// cannot possibly intersect, skip
						checkIntersection = false;
						break;
					case FOLDER:
						checkIntersection = true;
						break;
					case UNKNOWN:
					default:
						// unknown may be a folder, but we cannot know for sure
						// we can try to check, but if it would block (aka not loaded yet), no chance. Then it is NOT counted as intersected
						try {
							RepositoryLocation parentLocation = locationB.parent();
							Folder parentFolder = RepositoryManager.getInstance(null).locateFolder(parentLocation.getRepository(), parentLocation.getPath(), true);
							// there is no folder with the final path element of locationB in the parent of it, so NOT counted as intersected
							// there is a folder, so let's go to the actual intersection check below
							if (parentFolder == null) {
								// cannot even resolve anything, can't detect intersection
								checkIntersection = false;
							} else {
								// if there is a folder, let's go to the actual intersection check below
								checkIntersection = parentFolder.containsFolder(locationB.getName());
							}
						} catch (RepositoryException e) {
							// no matter, ignore
							checkIntersection = false;
						}
						break;
				}
				if (checkIntersection && locationA.path.length > locationB.path.length
						&& Objects.equals(locationA.getRepositoryName(), locationB.getRepositoryName())
						&& Arrays.equals(Arrays.copyOfRange(locationA.path, 0, locationB.path.length), locationB.path)) {
					iterator.remove();
					break;
				}
			}
		}
		return locations;
	}

	/**
	 * Returns the repository location for the provided path and operator. In case it is relative the operators process
	 * is used base path.
	 *
	 * @param loc the relative or absolute repository location path as String
	 * @param op  the operator for which should be used as base path in case the location is relative
	 * @return the repository location for the specified path
	 * @throws UserError in case the location is malformed
	 * @deprecated since 9.7, use {@link #getRepositoryLocationFolder(String, Operator)} or {@link
	 * #getRepositoryLocationData(String, Operator, Class)} instead
	 */
	@Deprecated
	public static RepositoryLocation getRepositoryLocation(String loc, Operator op) throws UserError {
		RepositoryLocation location = getRepositoryLocationData(loc, op, DataEntry.class);
		location.setLocationType(RepositoryLocationType.UNKNOWN);
		return location;
	}

	/**
	 * Returns the repository location for a folder for the provided path and operator. In case it is relative the operators process
	 * is used base path.
	 *
	 * @param loc              the relative or absolute repository location path as String
	 * @param op               the operator for which should be used as base path in case the location is relative
	 * @return the repository location for the specified path
	 * @throws UserError in case the location is malformed
	 * @since 9.7
	 */
	public static RepositoryLocation getRepositoryLocationFolder(String loc, Operator op) throws UserError {
		Process process = op == null ? null : op.getProcess();
		if (process != null) {
			RepositoryLocation result;
			try {
				result = process.resolveRepositoryLocation(loc, RepositoryLocationType.FOLDER);
			} catch (MalformedRepositoryLocationException e) {
				throw new UserError(op, e, 319, e.getMessage());
			}
			result.setAccessor(process.getRepositoryAccessor());
			return result;
		} else {
			if (RepositoryLocation.isAbsolute(loc)) {
				RepositoryLocation result;
				try {
					result = new RepositoryLocationBuilder().withLocationType(RepositoryLocationType.FOLDER).buildFromAbsoluteLocation(loc);
				} catch (MalformedRepositoryLocationException e) {
					throw new UserError(op, e, 319, e.getMessage());
				}
				return result;
			} else {
				throw new UserError(op, 320, loc);
			}
		}
	}

	/**
	 * Returns the repository location for a data entry for the provided path and operator. In case it is relative the
	 * operators process is used base path.
	 *
	 * @param loc              the relative or absolute repository location path as String
	 * @param op               the operator for which should be used as base path in case the location is relative
	 * @param expectedDataType the expected specific {@link DataEntry} (sub-)type. At the same repository location, for
	 *                         example a "test.rmhdf5table" (example set) and "test.rmp" (process) might live, and if
	 *                         the expected data entry subtype is not specified, this method will return the first one
	 *                         it finds. If {@code null}, will use {@link DataEntry}. Also see {@link
	 *                         #locateData()}.
	 * @return the repository location for the specified path
	 * @throws UserError in case the location is malformed
	 * @since 9.7
	 */
	public static RepositoryLocation getRepositoryLocationData(String loc, Operator op, Class<? extends DataEntry> expectedDataType) throws UserError {
		Process process = op == null ? null : op.getProcess();
		if (process != null) {
			RepositoryLocation result;
			try {
				result = process.resolveRepositoryLocation(loc, RepositoryLocationType.DATA_ENTRY);
			} catch (MalformedRepositoryLocationException e) {
				throw new UserError(op, e, 319, e.getMessage());
			}
			result.setAccessor(process.getRepositoryAccessor());
			result.setExpectedDataEntryType(expectedDataType);
			return result;
		} else if (RepositoryLocation.isAbsolute(loc)) {
			RepositoryLocation result;
			try {
				result = new RepositoryLocationBuilder().withExpectedDataEntryType(expectedDataType).buildFromAbsoluteLocation(loc);
			} catch (MalformedRepositoryLocationException e) {
				throw new UserError(op, e, 319, e.getMessage());
			}
			return result;
		} else {
			throw new UserError(op, 320, loc);
		}
	}

}
