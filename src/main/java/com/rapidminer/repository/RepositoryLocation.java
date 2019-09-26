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


/**
 * A location in a repository. Format:
 *
 * //Repository/path/to/object
 *
 * All constructors throw IllegalArugmentExceptions if names are malformed, contain illegal
 * characters etc.
 *
 * @author Simon Fischer, Adrian Wilke
 *
 */
public class RepositoryLocation {

	public static final char SEPARATOR = '/';
	public static final String REPOSITORY_PREFIX = "//";
	public static final String[] BLACKLISTED_STRINGS = new String[]{"/", "\\", ":", "<", ">", "*", "?", "\"", "|"};
	private static final String SEPARATOR_CHAR = String.valueOf(SEPARATOR);
	private final String[] path;
	private String repositoryName;
	private RepositoryAccessor accessor;


	/**
	 * Constructs a RepositoryLocation from a string of the form //Repository/path/to/object.
	 */
	public RepositoryLocation(String absoluteLocation) throws MalformedRepositoryLocationException {
		if (isAbsolute(absoluteLocation)) {
			this.path = initializeFromAbsoluteLocation(absoluteLocation);
		} else {
			repositoryName = null;
			this.path = initializeAbsolutePath(absoluteLocation);
		}
	}

	/**
	 * Creates a RepositoryLocation for a given repository and a set of path components which will
	 * be concatenated by a /.
	 *
	 * @throws MalformedRepositoryLocationException
	 */
	public RepositoryLocation(String repositoryName, String[] pathComponents) throws MalformedRepositoryLocationException {
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

	/**
	 * Appends a child entry to a given parent location. Child can be composed of subcomponents
	 * separated by /. Dots ("..") will resolve to the parent folder.
	 *
	 * @throws MalformedRepositoryLocationException
	 */
	public RepositoryLocation(RepositoryLocation parent, String childName) throws MalformedRepositoryLocationException {
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
	 * Locates the corresponding entry in the repository. It returns null if it doesn't exist yet.
	 * An exception is thrown if this location is invalid.
	 *
	 * @throws RepositoryException
	 *             If repository can not be found or entry can not be located.
	 * */
	public Entry locateEntry() throws RepositoryException {
		Repository repos = getRepository();
		if (repos != null) {
			return repos.locate(getPath());
		} else {
			return null;
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
				parent = new RepositoryLocation(this.repositoryName, pathCopy);
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
		Entry entry = locateEntry();
		if (entry == null) {
			Folder parentFolder = parent().createFoldersRecursively();
			try {
				entry = parentFolder.createFolder(getName());
			} catch (RepositoryException re) {
				//Recover from concurrent createFolder calls
				entry = locateEntry();
				//Rethrow the RepositoryException if recovery failed
				if (!(entry instanceof Folder)) {
					throw re;
				}
			}
		}

		if (entry instanceof Folder) {
			return (Folder) entry;
		} else {
			throw new RepositoryException(toString() + " is not a folder.");
		}

	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && this.toString().equals(obj.toString());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
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
	 * repository w.r.t. case insensitivity is the {@link LocalRepository}
	 *
	 * @return if this location represents a connection path entry or not
	 * @see com.rapidminer.repository.local.SimpleFolder#isConnectionsFolderName(String, boolean) SimpleFolder.isConnectionsFolderName(String, boolean)
	 * @since 9.3.1
	 */
	public boolean isConnectionPath() {
		try {
			return path.length == 2 && Folder.isConnectionsFolderName(path[0],
					repositoryName == null || !(getRepository() instanceof LocalRepository));
		} catch (RepositoryException e) {
			return false;
		}
	}

	/**
	 * Checks whether the given location string is located in the
	 * {@link Folder#isConnectionsFolderName(String, boolean) connection folder} of the repository.
	 * <p>
	 * <strong>Note:</strong> This method might depend on the repository implementation. Currently the only allowed
	 * repository w.r.t. case insensitivity is the {@link LocalRepository}
	 *
	 * @return if the given location represents a connection path entry or not
	 * @see com.rapidminer.repository.local.SimpleFolder#isConnectionsFolderName(String, boolean) SimpleFolder.isConnectionsFolderName(String, boolean)
	 * @since 9.3.1
	 */
	public static boolean isConnectionPath(String loc) {
		try {
			return new RepositoryLocation(loc).isConnectionPath();
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

		for (String forbiddenString : BLACKLISTED_STRINGS) {
			if (name.contains(forbiddenString)) {
				return forbiddenString;
			}
		}
		return null;
	}

	/**
	 * Removes locations from list, which are already included in others.
	 *
	 * Example: [/1/2/3, /1, /1/2] becomes [/1]
	 */
	public static List<RepositoryLocation> removeIntersectedLocations(List<RepositoryLocation> repositoryLocations) {
		return removeIntersectedLocations((Collection<RepositoryLocation>) repositoryLocations);
	}

	/**
	 * Removes locations from list, which are already included in others.
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
				if (locationA.path.length > locationB.path.length
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
	 * Returns the repository location for the provided path and operator. In case it is relative
	 * the operators process is used base path.
	 *
	 * @param loc
	 *            the relative or absolute repository location path as String
	 * @param op
	 *            the operator for which should be used as base path in case the location is
	 *            relative
	 * @return the repository location for the specified path
	 * @throws UserError
	 *             in case the location is malformed
	 */
	public static RepositoryLocation getRepositoryLocation(String loc, Operator op) throws UserError {
		Process process = op == null ? null : op.getProcess();
		if (process != null) {
			RepositoryLocation result;
			try {
				result = process.resolveRepositoryLocation(loc);
			} catch (MalformedRepositoryLocationException e) {
				throw new UserError(op, e, 319, e.getMessage());
			}
			result.setAccessor(process.getRepositoryAccessor());
			return result;
		} else {
			if (RepositoryLocation.isAbsolute(loc)) {
				RepositoryLocation result;
				try {
					result = new RepositoryLocation(loc);
				} catch (MalformedRepositoryLocationException e) {
					throw new UserError(op, e, 319, e.getMessage());
				}
				return result;
			} else {
				throw new UserError(op, 320, loc);
			}
		}
	}
}
