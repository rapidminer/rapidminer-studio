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
package com.rapidminer.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.vlsolutions.swing.docking.DockingContext;
import com.vlsolutions.swing.docking.ws.Workspace;
import com.vlsolutions.swing.docking.ws.WorkspaceException;


/**
 *
 * @author Simon Fischer
 *
 */
@SuppressWarnings("deprecation")
public class Perspective {

	private final String name;
	private final Workspace workspace = new Workspace();
	private boolean userDefined = false;
	private final ApplicationPerspectives owner;
	private final PerspectiveModel model;
	private final PerspectiveProperties properties = new PerspectiveProperties();

	public Perspective(ApplicationPerspectives owner, String name) {
		this.name = name;
		this.owner = owner;
		this.model = null;
	}

	public Perspective(PerspectiveModel model, String name) {
		this.name = name;
		this.model = model;
		this.owner = null;
	}

	public String getName() {
		return name;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public void store(DockingContext dockingContext) {
		properties.store();
		try {
			workspace.loadFrom(dockingContext);
		} catch (WorkspaceException e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.gui.Perspective.saving_workspace_error", e), e);

		}

	}

	public void save() {
		File file = getFile();
		try (OutputStream out = new FileOutputStream(file)) {
			workspace.writeXML(out);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.gui.Perspective.saving_perspective_error", file, e), e);
		}
	}

	public void load() {
		LogService.getRoot().log(Level.FINE, "com.rapidminer.gui.Perspective.loading_perspective", getName());
		File file = getFile();
		if (!file.exists()) {
			return;
		}
		try (InputStream in = new FileInputStream(file)) {
			workspace.readXML(in);
		} catch (Exception e) {

			if (!userDefined) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.gui.Perspective.reading_perspective_error_restoring", file, e), e);

				if (owner != null) {
					owner.restoreDefault(getName());
				}
				if (model != null) {
					model.restoreDefault(getName());
				}
			} else {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.gui.Perspective.reading_perspective_error_clearing", file, e), e);
				workspace.clear();
			}
		}
	}

	public void setUserDefined(boolean b) {
		this.userDefined = b;
	}

	public boolean isUserDefined() {
		return this.userDefined;
	}

	public void delete() {
		File file = getFile();
		if (file.exists()) {
			file.delete();
		}
	}

	protected boolean apply(DockingContext dockingContext) {
		try {
			workspace.apply(dockingContext);
			if (model != null) {
				model.notifyChangeListener();
			}
			properties.apply();
			return true;
		} catch (WorkspaceException e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.gui.Perspective.applying_workspace_error", name), e);
			return false;
		}
	}

	/**
	 * Returns the perspective properties.
	 *
	 * @return the perspective properties, never {@code null}
	 * @since 8.2.2
	 */
	PerspectiveProperties getProperties() {
		return properties;
	}

	File getFile() {
		return FileSystemService
				.getUserConfigFile("vlperspective-" + (isUserDefined() ? "user-" : "predefined-") + name + ".xml");
	}

}
