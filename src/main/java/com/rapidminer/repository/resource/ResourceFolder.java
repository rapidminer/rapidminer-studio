/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.repository.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;


/**
 * Reference on a folder in the repository.
 *
 * @author Simon Fischer
 */
public class ResourceFolder extends ResourceEntry implements Folder {

	protected List<Folder> folders;
	private List<DataEntry> data;

	protected ResourceFolder(ResourceFolder parent, String name, String resource, ResourceRepository repository) {
		super(parent, name, resource, repository);
	}

	@Override
	public boolean containsEntry(String name) throws RepositoryException {
		ensureLoaded();
		for (Entry entry : data) {
			if (entry.getName().equals(name)) {
				return true;
			}
		}
		for (Entry entry : folders) {
			if (entry.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public BlobEntry createBlobEntry(String name) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot create new entries.");
	}

	@Override
	public Folder createFolder(String name) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot create new entries.");
	}

	@Override
	public IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator,
			ProgressListener newParam) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot create new entries.");
	}

	@Override
	public ProcessEntry createProcessEntry(String name, String processXML) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot create new entries.");
	}

	@Override
	public List<DataEntry> getDataEntries() throws RepositoryException {
		ensureLoaded();
		return data;
	}

	protected void ensureLoaded() throws RepositoryException {
		if (folders != null && data != null) {
			return;
		}
		String resourcePath = getResource() + "/CONTENTS";
		InputStream in = null;
		try {
			in = Tools.getResourceInputStream(resourcePath);
		} catch (IOException e1) {
			throw new RepositoryException("Cannot find contents of folder " + getResource(), e1);
		}
		this.folders = new LinkedList<Folder>();
		this.data = new LinkedList<DataEntry>();
		try {
			String[] lines = Tools.readTextFile(new InputStreamReader(in, "UTF-8")).split("\n");
			for (String line : lines) {
				line = line.trim();
				if (!line.isEmpty()) {
					int space = line.indexOf(" ");
					String name = space != -1 ? line.substring(space + 1).trim() : null;
					if (line.startsWith("FOLDER ")) {
						folders.add(new ResourceFolder(this, name, getPath() + "/" + name, getRepository()));
					} else if (line.startsWith("ENTRY")) {
						String nameWOExt = name.substring(0, name.length() - 4);
						if (name.endsWith(".rmp")) {
							data.add(
									new ResourceProcessEntry(this, nameWOExt, getPath() + "/" + nameWOExt, getRepository()));
						} else if (name.endsWith(".ioo")) {
							data.add(new ResourceIOObjectEntry(this, nameWOExt, getPath() + "/" + nameWOExt,
									getRepository()));
						} else {
							throw new RepositoryException("Unknown entry type infolder '" + getName() + "': " + name);
						}
					} else {
						throw new RepositoryException("Illegal entry type in folder '" + getName() + "': " + line);
					}
				}
			}

		} catch (Exception e) {
			throw new RepositoryException("Error reading contents of folder " + getName() + ": " + e, e);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public List<Folder> getSubfolders() throws RepositoryException {
		ensureLoaded();
		return folders;
	}

	@Override
	public void refresh() throws RepositoryException {
		folders = null;
		data = null;
		getRepository().fireRefreshed(this);
	}

	@Override
	public String getDescription() {
		return getResource();
	}

	@Override
	public String getType() {
		return Folder.TYPE_NAME;
	}

	@Override
	public boolean canRefreshChild(String childName) throws RepositoryException {
		return containsEntry(childName);
	}

	/**
	 * Adds a folder to the list of folders.
	 *
	 * @param folder
	 *            the folder to add
	 * @throws RepositoryException
	 */
	void addFolder(Folder folder) throws RepositoryException {
		this.folders.add(folder);
	}

}
