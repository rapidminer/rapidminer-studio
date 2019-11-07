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
package com.rapidminer.repository.internal.remote.model;

/**
 * Data of an Entry in a Repository
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class EntryResponse extends Response {

	protected long date;
	protected String ioObjectClassName;
	protected int latestRevision;
	protected String location;
	protected int size;
	protected Long sizeLong;
	protected String type;
	protected String user;

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public String getIoObjectClassName() {
		return ioObjectClassName;
	}

	public void setIoObjectClassName(String ioObjectClassName) {
		this.ioObjectClassName = ioObjectClassName;
	}

	public int getLatestRevision() {
		return latestRevision;
	}

	public void setLatestRevision(int latestRevision) {
		this.latestRevision = latestRevision;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public Long getSizeLong() {
		return sizeLong;
	}

	public void setSizeLong(Long sizeLong) {
		this.sizeLong = sizeLong;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}
}
