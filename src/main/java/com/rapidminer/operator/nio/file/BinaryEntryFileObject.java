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
package com.rapidminer.operator.nio.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.Icon;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.gui.IconProvider;
import com.rapidminer.repository.gui.RepositoryEntryIconRegistry;
import com.rapidminer.repository.gui.RepositoryTreeCellRenderer;


/**
 * Implementation of a {@link FileObject} backed by a {@link RepositoryLocation}. The repository entry must be a {@link
 * BinaryEntry}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class BinaryEntryFileObject extends FileObject {

	private RepositoryLocation location;


	public BinaryEntryFileObject(RepositoryLocation location) {
		this.location = location;
	}

	@Override
	public InputStream openStream() throws OperatorException {
		try {
			return getBinaryEntry().openInputStream();
		} catch (RepositoryException e) {
			throw new OperatorException("319", e, location.getAbsoluteLocation());
		}
	}

	@Override
	public File getFile() throws OperatorException {
		try {
			return getBinaryEntry().toPath().toFile();
		} catch (IOException e) {
			throw new OperatorException("319", e, location.getAbsoluteLocation());
		}
	}

	@Override
	public long getLength() throws OperatorException {
		return getBinaryEntry().getSize();
	}

	/**
	 * Returns the file name, see {@link BinaryEntry#getName()}.
	 *
	 * @return the name including the suffix, never {@code null}
	 */
	@Override
	public String getName() {
		return location.getName();
	}

	/**
	 * Returns the file name, see {@link BinaryEntry#getName()}.
	 *
	 * @return the name including the suffix, never {@code null}
	 */
	@Override
	public String getFilename() {
		return getName();
	}

	@Override
	public Icon getResultIcon() {
		String suffix = RepositoryTools.getSuffixFromFilename(getFilename());
		IconProvider iconProvider = RepositoryEntryIconRegistry.getInstance().getCallback(suffix);
		if (iconProvider != null) {
			return SwingTools.createIcon("16/" + iconProvider.getIconName());
		} else {
			return RepositoryTreeCellRenderer.getIconForFileSuffix(suffix);
		}
	}

	@Override
	public String toString() {
		return "Repository location: " + location.getAbsoluteLocation();
	}

	/**
	 * The location of the {@link BinaryEntry} behind this file object.
	 *
	 * @return the location, never {@code null}
	 */
	public RepositoryLocation getLocation() {
		return location;
	}

	private BinaryEntry getBinaryEntry() throws OperatorException {
		try {
			BinaryEntry entry = location.locateData();
			if (entry != null) {
				return entry;
			} else {
				throw new OperatorException("312", null, location.getAbsoluteLocation(), "No BinaryEntry exists at this location");
			}
		} catch (RepositoryException e) {
			throw new OperatorException("312", e, location.getAbsoluteLocation(), "Failed to load entry");
		}
	}
}