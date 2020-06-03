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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.exceptions.DataRetrievalException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;


/**
 * A process entry, used for RapidMiner processes.
 *
 * @author Andreas Timm
 * @since 9.7
 */
public class BasicProcessEntry extends BasicDataEntry<String> implements ProcessEntry {


	/**
	 * Create a new ProcessEntry with a given name in a target folder
	 *
	 * @param name   of this entry, with the correct suffix
	 * @param parent folder for this entry, must not be {@code null}
	 */
	BasicProcessEntry(String name, BasicFolder parent) {
		super(name, parent, String.class);
	}

	@Override
	public String retrieveXML() throws RepositoryException {
		try {
			return getData();
		} catch (DataRetrievalException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void storeXML(String xml) throws RepositoryException {
		try {
			setData(xml);
		} catch (RepositoryFileException | RepositoryImmutableException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	protected String read(InputStream load) throws IOException {
		return IOUtils.toString(load, StandardCharsets.UTF_8);
	}

	@Override
	protected void write(String data) throws IOException, RepositoryImmutableException {
		try (OutputStream os = getOutputStream()) {
			os.write(data.getBytes(StandardCharsets.UTF_8));
		}
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		boolean shouldResetLocation = RapidMinerGUI.isMainFrameProcessLocation(getLocation());
		boolean renamed = super.rename(newName);
		if (renamed && shouldResetLocation) {
			RapidMinerGUI.resetProcessLocation(new RepositoryProcessLocation(getLocation()));
		}
		return renamed;
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		boolean shouldResetLocation = RapidMinerGUI.isMainFrameProcessLocation(getLocation());
		boolean moved = super.move(newParent);
		if (moved && shouldResetLocation) {
			RapidMinerGUI.resetProcessLocation(new RepositoryProcessLocation(getLocation()));
		}
		return moved;
	}

	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		boolean shouldResetLocation = RapidMinerGUI.isMainFrameProcessLocation(getLocation());
		boolean moved = super.move(newParent, newName);
		if (moved && shouldResetLocation) {
			RapidMinerGUI.resetProcessLocation(new RepositoryProcessLocation(getLocation()));
		}
		return moved;
	}

	@Override
	protected boolean checkDataSummary(DataSummary dataSummary) {
		return false;
	}
}
