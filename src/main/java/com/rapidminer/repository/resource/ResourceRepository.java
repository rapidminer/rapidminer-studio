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
package com.rapidminer.repository.resource;

import java.util.logging.Level;

import javax.swing.event.EventListenerList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryListener;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.gui.RepositoryConfigurationPanel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * Repository backed by Java resources. Folders must contain a "CONTENTS" file, otherwise the
 * contents cannot be scanned.
 *
 * @author Simon Fischer
 *
 */
public class ResourceRepository extends ResourceFolder implements Repository {

	private final EventListenerList listeners = new EventListenerList();

	private String resourcePrefix;

	private final boolean addTemplateFolder;

	private final boolean addTutorialFolder;

	public ResourceRepository(String name, String resourcePrefix) {
		this(name, resourcePrefix, true, true);
	}

	public ResourceRepository(String name, String resourcePrefix, boolean addTemplateFolder, boolean addTutorialFolder) {
		super(null, name, "", null);
		this.resourcePrefix = resourcePrefix;
		this.addTemplateFolder = addTemplateFolder;
		this.addTutorialFolder = addTutorialFolder;
		setRepository(this);
	}

	protected String getResourceRoot() {
		return resourcePrefix;
	}

	@Override
	protected void ensureLoaded() throws RepositoryException {
		if (isLoaded()) {
			return;
		}
		super.ensureLoaded();

		// add template and tutorial folder in case folders are not available yet
		// and repository should contain the template or tutorial folder
		if (addTemplateFolder) {
			try {
				addFolder(new TemplatesFolder(this, getPath(), getRepository()));
			} catch (RepositoryException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.repository.resource.ResourceRepository.error_adding_template_folder", e);
			}
		}
		if (addTutorialFolder) {
			try {
				addFolder(new TutorialFolder(this, getPath(), getRepository()));
			} catch (RepositoryException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.repository.resource.ResourceRepository.error_adding_tutorial_folder", e);
			}
		}
	}

	@Override
	public void addRepositoryListener(RepositoryListener l) {
		listeners.add(RepositoryListener.class, l);
	}

	@Override
	public void removeRepositoryListener(RepositoryListener l) {
		listeners.remove(RepositoryListener.class, l);
	}

	protected void fireRefreshed(final Folder folder) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			l.folderRefreshed(folder);
		}
	}

	@Override
	public Element createXML(Document doc) {
		return null;
	}

	@Override
	public String getState() {
		return null;
	}

	@Override
	public String getIconName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.repository.resource.icon");
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			return new RepositoryLocation(getName(), new String[0]);
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Entry locate(String entry) throws RepositoryException {
		return RepositoryManager.getInstance(null).locate(this, entry, false);
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	@Override
	public void postInstall() {}

	@Override
	public void preRemove() {}

	@Override
	public boolean isConfigurable() {
		return false;
	}

	@Override
	public RepositoryConfigurationPanel makeConfigurationPanel() {
		throw new UnsupportedOperationException("Resource repository cannot be configured.");
	}
}
