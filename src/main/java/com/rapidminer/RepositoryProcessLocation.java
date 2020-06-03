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
package com.rapidminer;

import java.io.IOException;
import java.util.logging.Level;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.UserData;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.internal.remote.RemoteContentManager;
import com.rapidminer.repository.internal.remote.RemoteProcessEntry;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.PasswordInputCanceledException;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.XMLException;


/**
 * @author Simon Fischer
 */
public class RepositoryProcessLocation implements ProcessLocation {

	/** key for custom user property */
	public static final String UPDATE_REVISION_ON_SAVE_KEY = "update_revision_on_save";

	/** A simple {@link UserData} object to pass {@link Boolean} values */
	public static class SimpleBooleanUserData implements UserData<Object> {


		private boolean value;
		public SimpleBooleanUserData(boolean value) {
			this.value = value;
		}

		@Override
		public UserData<Object> copyUserData(Object newParent) {
			return this;
		}

		public boolean isSet() {
			return value;
		}


	}

	private static final String GENERIC_PROCESS_ICON = I18N.getGUILabel("getting_started.open_recent.icon");

	private final RepositoryLocation repositoryLocation;

	public RepositoryProcessLocation(RepositoryLocation location) {
		super();
		location.setExpectedDataEntryType(ProcessEntry.class);
		this.repositoryLocation = location;
	}

	private ProcessEntry getEntry() throws IOException {
		ProcessEntry entry;
		try {
			entry = repositoryLocation.locateData();
		} catch (RepositoryException e) {
			throw new IOException("Cannot locate entry '" + repositoryLocation + "': " + e, e);
		}
		if (entry == null) {
			throw new IOException("No such entry: " + repositoryLocation);
		} else {
			return (ProcessEntry) entry;
		}
	}

	@Override
	public String getRawXML() throws IOException {
		try {
			return getEntry().retrieveXML();
		} catch (RepositoryException e) {
			throw new IOException("Cannot access entry '" + repositoryLocation + "': " + e, e);
		}
	}

	@Override
	public Process load(ProgressListener listener) throws IOException, XMLException {
		if (listener != null) {
			listener.setCompleted(60);
		}
		final String xml = getRawXML();
		Process process;
		try {
			process = new Process(xml, repositoryLocation.getRepository().getEncryptionContext());
		} catch (RepositoryException e) {
			throw new IOException(e);
		}
		process.setProcessLocation(this);
		if (listener != null) {
			listener.setCompleted(80);
		}
		return process;
	}

	@Override
	public String toHistoryFileString() {
		return "repository " + repositoryLocation.toString();
	}

	@Override
	public void store(Process process, ProgressListener listener) throws IOException {
		try {
			ProcessEntry entry = repositoryLocation.locateData();
			Repository repository = repositoryLocation.getRepository();
			if (entry == null) {
				Folder folder = repositoryLocation.parent().createFoldersRecursively();
				folder.createProcessEntry(repositoryLocation.getName(), process.getRootOperator().getXML(false, repository.getEncryptionContext()));
			} else {
				boolean isReadOnly = repository.isReadOnly();
				if (isReadOnly) {
					SwingTools.showSimpleErrorMessage("save_to_read_only_repo", "", repositoryLocation.toString());
					return;
				}

				UserData<Object> updateRevisionOnSave = process.getRootOperator().getUserData(UPDATE_REVISION_ON_SAVE_KEY);
				if (updateRevisionOnSave != null && ((SimpleBooleanUserData) updateRevisionOnSave).isSet()) {
					if (entry instanceof RemoteProcessEntry) {
						RemoteRepository repo = ((RemoteProcessEntry) entry).getRepository();
						if (repo != null) {
							try {
								RemoteContentManager entryService = repo.getContentManager();
								entryService.startNewRevision(entry.getLocation().getPath());
								entry.getContainingFolder().refresh();
								ProcessEntry newRevisionEntry = repo.locateData(entry.getLocation().getPath(), ProcessEntry.class, true);
								// no encryption for remote
								newRevisionEntry.storeXML(process.getRootOperator().getXML(false, repository.getEncryptionContext()));
								process.getRootOperator().setUserData(UPDATE_REVISION_ON_SAVE_KEY, null);
							} catch (PasswordInputCanceledException e) {
								// do nothing
							}
						}
					}
				} else {
					entry.storeXML(process.getRootOperator().getXML(false, repository.getEncryptionContext()));
				}
			}
			LogService.getRoot().log(Level.INFO, "com.rapidminer.RepositoryProcessLocation.saved_process_definition",
					repositoryLocation);
		} catch (RepositoryException e) {
			throw new IOException("Cannot store process at " + repositoryLocation + ": " + e.getMessage(), e);
		}
	}

	public RepositoryLocation getRepositoryLocation() {
		return repositoryLocation;
	}

	@Override
	public String toMenuString() {
		return repositoryLocation.toString();
	}

	@Override
	public String toString() {
		return toMenuString();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof RepositoryProcessLocation)) {
			return false;
		} else {
			return ((RepositoryProcessLocation) o).repositoryLocation.equals(this.repositoryLocation);
		}
	}

	@Override
	public int hashCode() {
		return repositoryLocation.hashCode();
	}

	@Override
	public String getShortName() {
		return repositoryLocation.getName();
	}

	@Override
	public String getIconName() {
		try {
			return getRepositoryLocation().getRepository().getIconName();
		} catch (Exception e) {
			// can happen if a repository was removed, do not log anything in those cases
			return GENERIC_PROCESS_ICON;
		}
	}
}
