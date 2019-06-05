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

import java.io.IOException;

import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.Tools;


/**
 * @author Simon Fischer, Jan Czogalla
 */
public class SimpleProcessEntry extends SimpleDataEntry implements ProcessEntry {

	public SimpleProcessEntry(String name, SimpleFolder containingFolder, LocalRepository repository) {
		super(name, containingFolder, repository);
	}

	@Override
	public String retrieveXML() throws RepositoryException {
		try {
			return Tools.readTextFile(getDataFile());
		} catch (IOException e) {
			throw new RepositoryException("Cannot read " + getDataFile() + ": " + e, e);
		}
	}

	@Override
	public void storeXML(String xml) throws RepositoryException {
		try {
			boolean existed = getDataFile().exists();
			Tools.writeTextFile(getDataFile(), xml);
			if (existed) {
				getRepository().fireEntryChanged(this);
			}
		} catch (IOException e) {
			throw new RepositoryException("Cannot write " + getDataFile() + ": " + e, e);
		}
	}

	@Override
	public String getSuffix() {
		return RMP_SUFFIX;
	}

	@Override
	public String getDescription() {
		return "Local process";
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		boolean shouldResetLocation = RapidMinerGUI.isMainFrameProcessLocation(getLocation());
		super.rename(newName);
		if (shouldResetLocation) {
			RapidMinerGUI.resetProcessLocation(new RepositoryProcessLocation(getLocation()));
		}
		return true;
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		boolean shouldResetLocation = RapidMinerGUI.isMainFrameProcessLocation(getLocation());
		super.move(newParent);
		if (shouldResetLocation) {
			RapidMinerGUI.resetProcessLocation(new RepositoryProcessLocation(getLocation()));
		}
		return true;
	}

	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		boolean shouldResetLocation = RapidMinerGUI.isMainFrameProcessLocation(getLocation());
		super.move(newParent, newName);
		if (shouldResetLocation) {
			RapidMinerGUI.resetProcessLocation(new RepositoryProcessLocation(getLocation()));
		}
		return true;
	}

}
