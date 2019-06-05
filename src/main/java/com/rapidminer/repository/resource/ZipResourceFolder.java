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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.EntryCreator;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;


/**
 * Class for a folder in the repository that is associated with a {@link ZipStreamResource} and
 * contains the process and input data for this template.
 *
 * @author Gisa Schaefer, Marcel Michel, Jan Czogalla
 * @since 7.0.0
 */
public class ZipResourceFolder extends ResourceFolder {

	/**
	 * A map of {@link EntryCreator}, one for each zip version of {@link ResourceDataEntry}.
	 * @since 9.3
	 */
	private static final Map<String, EntryCreator<String[], ? extends ResourceDataEntry, ZipResourceFolder, ResourceRepository>> CREATOR_MAP;
	static {
		Map<String, EntryCreator<String[], ? extends ResourceDataEntry, ZipResourceFolder, ResourceRepository>> creatorMap = new HashMap<>();
		creatorMap.put(BlobEntry.BLOB_SUFFIX, (l, f, r) -> new ZipResourceBlobEntry(f, l[0], l[1], r, f.zipStream));
		creatorMap.put(ProcessEntry.RMP_SUFFIX, (l, f, r) -> new ZipResourceProcessEntry(f, l[0], l[1], r, f.zipStream));
		creatorMap.put(IOObjectEntry.IOO_SUFFIX, (l, f, r) -> new ZipResourceIOObjectEntry(f, l[0], l[1], r, f.zipStream));
		// ignore connections outside connection folder
		creatorMap.put(ConnectionEntry.CON_SUFFIX, (l, f, r) -> f.isSpecialConnectionsFolder() ? new ZipResourceConnectionEntry(f, l[0], l[1], r, f.zipStream) : null);
		CREATOR_MAP = Collections.unmodifiableMap(creatorMap);
	}

	private final ZipStreamResource zipStream;

	protected ZipResourceFolder(ResourceFolder parent, String name, ZipStreamResource zipStream, String parentPath,
								ResourceRepository repository) {
		super(parent, name, parentPath + "/" + name, repository);
		this.zipStream = zipStream;
	}

	@Override
	protected void ensureLoaded(List<Folder> folders, List<DataEntry> data) throws RepositoryException {
		try (ZipInputStream zip = zipStream.getStream()) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory() || entry.getName().replaceFirst("/", "").contains("/")
						|| zipStream.getStreamPath() != null && !entry.getName().startsWith(zipStream.getStreamPath())) {
					continue;
				}
				String entryName = entry.getName();
				String[] split = Paths.get(entryName).getFileName().toString().split("\\.");
				String suffix = "";
				if (split.length > 1) {
					suffix = '.' + split[split.length - 1];
				}
				String dataEntryName = split[0];
				ResourceDataEntry dataEntry = CREATOR_MAP.getOrDefault(suffix, EntryCreator.nullCreator())
						.create(new String[]{dataEntryName, getPath() + "/" + dataEntryName}, this, getRepository());
				if (dataEntry != null) {
					data.add(dataEntry);
				}
			}
		} catch (IOException e) {
			throw new RepositoryException("Cannot load data from '" + getResource() + ": " + e, e);
		}
	}
}
