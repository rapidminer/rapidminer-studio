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
package com.rapidminer.repository.local;

import java.io.File;

import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;


/**
 * @author Simon Fischer, Jan Czogalla
 */
public abstract class SimpleDataEntry extends SimpleEntry implements DataEntry {

	public SimpleDataEntry(String name, SimpleFolder containingFolder, LocalRepository localRepository) {
		super(name, containingFolder, localRepository);
	}

	/**
	 * Suffix for the specialized entry type, like {@value SimpleIOObjectEntry#IOO_SUFFIX}
	 *
	 * @return the file suffix
	 * @since 9.3
	 */
	public abstract String getSuffix();

	@Override
	public long getDate() {
		return getDataFile().lastModified();
	}

	@Override
	public long getSize() {
		return getDataFile().length();
	}

	@Override
	public String getDescription() {
		return getName();
	}

	@Override
	public int getRevision() {
		return 1;
	}

	@Override
	public void delete() throws RepositoryException {
		if (getDataFile().exists()) {
			getDataFile().delete();
		}
		super.delete();
	}

	/**
	 * Retrieves the main file associated with this {@link DataEntry}.
	 *
	 * @see #getFile(String)
	 * @see #getSuffix()
	 * @since 9.3
	 */
	protected File getDataFile() {
		return getFile(getSuffix());
	}

	@Override
	protected void handleRename(String newName) throws RepositoryException {
		renameFile(getDataFile(), newName);
	}

	@Override
	protected void handleMove(Folder newParent, String newName) throws RepositoryException {
		moveFile(getDataFile(), ((SimpleFolder) newParent).getFile(), newName, getSuffix());
	}
}
