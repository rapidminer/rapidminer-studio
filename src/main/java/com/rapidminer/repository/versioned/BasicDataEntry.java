/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.versioned;

import java.io.IOException;
import java.util.Collection;
import javax.swing.Action;

import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.repository.RepositoryLocationType;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryStoreOtherInConnectionsFolderException;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.versioning.repository.RepositoryFolder;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFolderException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;
import com.rapidminer.versioning.repository.exceptions.RepositoryNamingException;


/**
 * A data entry, with base functionality required by all basic entry types.
 *
 * @author Andreas Timm
 * @since 9.7
 */
public abstract class BasicDataEntry<T> extends BasicEntry<T> implements DataEntry {


	protected BasicDataEntry(String name, BasicFolder parent, Class<T> dataType) {
		super(name, parent, dataType);
	}

	@Override
	public long getDate() {
		return getLastModified();
	}

	@Override
	public String getOwner() {
		return null;
	}

	@Override
	public String getDescription() {
		return getName();
	}

	@Override
	public boolean isReadOnly() {
		return !getRepositoryAdapter().getGeneralRepository().isEditable();
	}

	@Override
	public String getName() {
		// Overriden because of backwards compatibility with old RapidMiner Repository framework
		// It did only store prefixes, never the full filename
		// As such, retrieval would be impossible if getName reports the name including suffix
		return getPrefix();
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		if (getContainingFolder().containsData(newName, getClass())) {
			throw new RepositoryException("Entry with name '" + newName + "' already exists");
		}
		try {
			// data entries have a suffix, but the renaming will not provide one, so add it manually
			newName = newName + "." + getSuffix();
			getRepositoryAdapter().getGeneralRepository().renameFile(this, newName, true);
			return true;
		} catch (RepositoryImmutableException | RepositoryNamingException | RepositoryFileException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		return moveInternal(newParent, getFullName());
	}

	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		// data entries have a suffix, but the renaming will not provide one, so add it manually
		return moveInternal(newParent, newName + "." + getSuffix());
	}

	/**
	 * Legacy repository framework compatibility. Will never block.
	 *
	 * @return always {@code false}
	 */
	@Override
	public boolean willBlock() {
		return false;
	}

	@Override
	public RepositoryLocation getLocation() {
		RepositoryLocation loc = getRepositoryLocationWithoutSuffix(getPath(), getSuffix(), getRepositoryAdapter().getName());
		loc.setExpectedDataEntryType(getClass());
		return loc;
	}

	/**
	 * Get the repository location for a {@link DataEntry}.
	 *
	 * @param path           of the data entry, never {@code null}
	 * @param suffix         optional, will be removed if available
	 * @param repositoryName name of the repository of this data entry
	 * @return a {@link RepositoryLocation} as usually used to look up entries
	 */
	public static RepositoryLocation getRepositoryLocationWithoutSuffix(String path, String suffix, String repositoryName) {
		try {
			int suffixIndex = (suffix == null || suffix.isEmpty()) ? -1 : path.lastIndexOf(suffix);
			if (suffixIndex > 0) {
				path = path.substring(0, suffixIndex - 1);
			}
			if (path.startsWith(String.valueOf(RepositoryLocation.SEPARATOR))) {
				path = path.substring(1);
			}
			if (path.indexOf(RepositoryLocation.SEPARATOR) > 0) {
				return new RepositoryLocationBuilder().withFailIfDuplicateIOObjectExists(false).
						withLocationType(RepositoryLocationType.DATA_ENTRY).buildFromPathComponents(repositoryName, path.split(String.valueOf(RepositoryLocation.SEPARATOR)));
			} else {
				return new RepositoryLocationBuilder().withFailIfDuplicateIOObjectExists(false).
						withLocationType(RepositoryLocationType.DATA_ENTRY).buildFromPathComponents(repositoryName, new String[]{path});
			}
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete() throws RepositoryException {
		try {
			getRepositoryAdapter().getGeneralRepository().deleteFile(this);
		} catch (IOException | RepositoryImmutableException e) {
			throw new RepositoryException("Could not delete " + getPath(), e);
		}
	}

	@Override
	public Collection<Action> getCustomActions() {
		return null;
	}

	@Override
	public String toString() {
		return getName();
	}

	private boolean moveInternal(Folder newParent, String name) throws RepositoryException {
		if (RepositoryTools.isInSpecialConnectionsFolder(newParent) && !(this instanceof ConnectionEntry)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(Folder.MESSAGE_CONNECTION_FOLDER);
		}
		try {
			Folder locate = RepositoryManager.getInstance(null).locateFolder(getRepositoryAdapter(), newParent.getLocation().getPath(), false);
			RepositoryFolder newParentFolder = null;
			if (locate instanceof BasicFolder) {
				newParentFolder = ((BasicFolder) locate).getFsFolder();
			} else if (locate instanceof FilesystemRepositoryAdapter) {
				newParentFolder = ((FilesystemRepositoryAdapter) locate).getRootFolder().getFsFolder();
			}
			getRepositoryAdapter().getGeneralRepository().move(this, newParentFolder, name, true);
			return true;
		} catch (RepositoryImmutableException | RepositoryNamingException | RepositoryFolderException e) {
			throw new RepositoryException(e);
		}
	}
}
