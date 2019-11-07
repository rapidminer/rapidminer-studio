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

import java.util.ArrayList;
import java.util.List;


/**
 * Contents of a folder: a list of entries and the folder path
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class FolderContentsResponse extends Response {
	protected List<EntryResponse> entries;
	protected String location;

	/**
	 * The {@link EntryResponse entries} of the folder
	 *
	 * @return a {@link List} of {@link EntryResponse entries}, never {@code null}
	 */
	public List<EntryResponse> getEntries() {
		if (entries == null) {
			entries = new ArrayList<>();
		}
		return entries;
	}

	public void setEntries(List<EntryResponse> entries) {
		this.entries = entries;
	}

	/**
	 * String representation of the location of this folder
	 *
	 * @return the location of the folder, may be {@code null}
	 */
	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
}
