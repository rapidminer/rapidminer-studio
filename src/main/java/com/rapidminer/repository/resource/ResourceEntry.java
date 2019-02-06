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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.swing.Action;

import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.Tools;


/**
 *
 * @author Simon Fischer, Jan Czogalla
 *
 */
public abstract class ResourceEntry implements Entry {

	private ResourceFolder container;
	private String name;
	private String path;
	private ResourceRepository repository;

	protected ResourceEntry(ResourceFolder parent, String name, String path, ResourceRepository repository) {
		this.container = parent;
		this.name = name;
		this.path = path;
		this.repository = repository;
	}

	@Override
	public void delete() throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot delete entries.");
	}

	@Override
	public Folder getContainingFolder() {
		return container;
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			return new RepositoryLocation(getRepository().getLocation().toString() + getPath());
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOwner() {
		return null;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot move entries.");
	}

	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot move or rename entries.");
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		throw new RepositoryException("Repository is read only.");
	}

	@Override
	public boolean willBlock() {
		return false;
	}

	protected String getResource() {
		return getRepository().getResourceRoot() + getPath();
	}

	protected String getPath() {
		return path;
	}

	protected ResourceRepository getRepository() {
		return repository;
	}

	protected void setRepository(ResourceRepository resourceRepository) {
		this.repository = resourceRepository;
	}

	/**
	 * Get the {@link InputStream} to the resource represented by this {@link Entry}
	 * and the given suffix.
	 *
	 * @param suffix
	 * 		the suffix to add to the resource path
	 * @return the input stream to the resource or null
	 * @throws RepositoryException
	 * 		if any error occurs
	 * @since 9.0
	 */
	protected InputStream getResourceStream(String suffix) throws RepositoryException {
		try {
			return Tools.getResourceInputStream(getResource() + suffix);
		} catch (IOException e) {
			throw new RepositoryException("Missing resource '" + getResource() + suffix + "': " + e, e);
		}
	}

	@Override
	public Collection<Action> getCustomActions() {
		return null;
	}
}
