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
package com.rapidminer.repository.versioned.datasummary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.PersistentContentMapperStore;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.repository.RepositoryLocationType;
import com.rapidminer.repository.versioned.FilesystemRepositoryAdapter;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ReferenceCache;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.Storage;

/**
 * A {@link Storage} implementation that keeps some {@link DataSummary DataSummaries} in memory utilizing a
 * {@link ReferenceCache} and using the {@link PersistentContentMapperStore} for persistent storage.
 * <p>
 * Each instance has a guaranteed in memory cache size of {@value #CACHE_SIZE}. Since the references kept are not
 * explicitly weak references, the in-memory size might vary depending on the rest of memory usage.
 * <p>
 * The persistent storage makes use of the {@link PersistentContentMapperStore} by employing a two-tiered approach.
 * Since each data summary has its own handler/serializer registered with {@link DataSummarySerializerRegistry},
 * it is not necessarily clear, which content key needs to be queried for a given object path (without additional cost
 * of entry look up and inspection). Thus the main content key used is {@link #METADATA_CONTENT_KEY metadata.key},
 * which is a plain string storing the actual content key to look up. The data summary's content key is comprised of
 * {@link DataSummarySerializer#getSuffix()} and {@link #METADATA_SUFFIX} separated by a period.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public class ContentMapperStorage implements Storage {

	public static final String METADATA_SUFFIX = "metadata";
	public static final String METADATA_CONTENT_KEY = METADATA_SUFFIX + ".key";

	private static final ReferenceCache<DataSummary>.Reference NULL_REF = new  ReferenceCache<DataSummary>(1).newReference(null);
	private static final int CACHE_SIZE = 10;

	private final ReferenceCache<DataSummary> dsCache = new ReferenceCache<>(CACHE_SIZE);
	private final Map<String, ReferenceCache<DataSummary>.Reference> locationCache =
			new ConcurrentHashMap<String, ReferenceCache<DataSummary>.Reference>() {
				@Override
				public ReferenceCache<DataSummary>.Reference get(Object key) {
					ReferenceCache<DataSummary>.Reference r = super.get(key);
					return r == null ? NULL_REF : r;
				}
			};
	private final FilesystemRepositoryAdapter repositoryAdapter;

	public ContentMapperStorage(FilesystemRepositoryAdapter repositoryAdapter) {
		this.repositoryAdapter = repositoryAdapter;
	}

	@Override
	public boolean contains(String objectPath) {
		ReferenceCache<DataSummary>.Reference reference = locationCache.get(objectPath);
		DataSummary dataSummary = reference.get();
		if (dataSummary == null && reference != NULL_REF) {
			locationCache.remove(objectPath);
		}
		if (dataSummary != null) {
			return true;
		}
		// check store
		return getContentKeyFromStore(getRepoLocation(objectPath)) != null;
	}

	@Override
	public DataSummary load(String objectPath) {
		ReferenceCache<DataSummary>.Reference reference = locationCache.get(objectPath);
		DataSummary dataSummary = reference.get();
		if (dataSummary == null && reference != NULL_REF) {
			locationCache.remove(objectPath);
		}
		if (dataSummary != null) {
			return dataSummary;
		}

		// retrieve from store
		String contentKey = getContentKeyFromStore(getRepoLocation(objectPath));
		if (contentKey == null) {
			return null;
		}
		DataSummarySerializer serializer = DataSummarySerializerRegistry.getInstance().getCallback(extractSuffix(contentKey));
		if (serializer == null) {
			return null;
		}
		try {
			return PersistentContentMapperStore.INSTANCE.retrieve(contentKey, serializer, getRepoLocation(objectPath), null);
		} catch (IOException e) {
			LogService.log(LogService.getRoot(), Level.WARNING, e,
					"com.rapidminer.repository.versioned.datasummary.ContentMapperStorage.error_retrieving_datasummary",
					objectPath, repositoryAdapter.getName());
			return null;
		}
	}

	@Override
	public void storeInternal(String objectPath, DataSummary md) {
		locationCache.put(objectPath, dsCache.newReference(md));
		List<DataSummarySerializer> serializers = DataSummarySerializerRegistry.getInstance().getCallbacks(md);
		if (serializers.isEmpty()) {
			return;
		}
		DataSummarySerializer serializer = serializers.get(0);
		String contentKey = makeContentKey(serializer.getSuffix());
		RepositoryLocation repoLocation = getRepoLocation(objectPath);
		// write to store
		try {
			PersistentContentMapperStore.INSTANCE.store(METADATA_CONTENT_KEY, contentKey, repoLocation);
			PersistentContentMapperStore.INSTANCE.store(contentKey, md, serializer, repoLocation);
		} catch (IOException e) {
			try {
				// clean up
				PersistentContentMapperStore.INSTANCE.store(METADATA_CONTENT_KEY, null, repoLocation);
			} catch (IOException ex) {
				// ignore
			}
			LogService.log(LogService.getRoot(), Level.WARNING, e,
					"com.rapidminer.repository.versioned.datasummary.ContentMapperStorage.error_writing_datasummary",
					objectPath, repositoryAdapter.getName(), md.getClass().getSimpleName());
		}
	}

	@Override
	public void clear() {
		locationCache.clear();
		clearKeysFromStore(repositoryAdapter.getLocation());
	}

	@Override
	public void remove(String objectPath) {
		locationCache.remove(objectPath);
		clearKeysFromStore(getRepoLocation(objectPath));
	}

	@Override
	public void rename(String oldLocation, String path) {
		copyOrMove(oldLocation, path, true);
		// PersistentContentMapperStore takes care of rename/move internally
	}

	@Override
	public void copyStartingWith(String originalPath, String targetPath) {
		copyOrMove(originalPath, targetPath, false);
		PersistentContentMapperStore.INSTANCE.copyContent(getRepoLocation(originalPath), getRepoLocation(targetPath));
	}

	/** Creates {@link RepositoryLocation} from the given relative path */
	private RepositoryLocation getRepoLocation(String objectPath) {
		try {
			return new RepositoryLocationBuilder().withLocationType(RepositoryLocationType.DATA_ENTRY).buildFromAbsoluteLocation(RepositoryLocation.REPOSITORY_PREFIX
					+ repositoryAdapter.getName()
					+ objectPath);
		} catch (MalformedRepositoryLocationException e) {
			// should not happen
			return null;
		}
	}

	/** Reads the actual content key and makes sure that it also exists */
	private String getContentKeyFromStore(RepositoryLocation repoLocation) {
		try {
			String contentKey = PersistentContentMapperStore.INSTANCE.retrieve(METADATA_CONTENT_KEY, repoLocation);
			if (contentKey == null) {
				return null;
			}
			Path path = PersistentContentMapperStore.INSTANCE.retrieve(contentKey, p -> p, repoLocation);
			if (path == null) {
				PersistentContentMapperStore.INSTANCE.store(METADATA_CONTENT_KEY, null, repoLocation);
				return null;
			}
			// store returns null if file does not exist
			return contentKey;
		} catch (IOException e) {
			LogService.log(LogService.getRoot(), Level.WARNING, e,
					"com.rapidminer.repository.versioned.datasummary.ContentMapperStorage.error_retrieving_contentkey",
					repoLocation);
			return null;
		}
	}

	/** Clears all registered keys and main key from the given location */
	private void clearKeysFromStore(RepositoryLocation repoLocation) {
		List<String> keysToClear = DataSummarySerializerRegistry.getInstance().getRegisteredSuffixes()
				.stream().map(ContentMapperStorage::makeContentKey).collect(Collectors.toList());
		keysToClear.add(METADATA_CONTENT_KEY);
		PersistentContentMapperStore.INSTANCE.clearKeys(keysToClear, repoLocation);
	}

	/** Copies or moves the references inside the location cache */
	private void copyOrMove(String originalPath, String targetPath, boolean move) {
		NavigableSet<String> keys = new TreeSet<>(locationCache.keySet());
		keys.subSet(originalPath, originalPath + Character.MAX_VALUE).forEach(key -> {
			ReferenceCache<DataSummary>.Reference reference = locationCache.get(key);
			if (reference.get() == null) {
				if (reference != NULL_REF) {
					locationCache.remove(key);
				}
				return;
			}
			locationCache.put(targetPath + key.substring(originalPath.length()), reference);
			if (move) {
				locationCache.remove(key);
			}
		});
	}

	/** Create content key from suffix */
	private static String makeContentKey(String suffix) {
		return suffix + '.' + METADATA_SUFFIX;
	}

	/** Extract suffix from content key */
	private static String extractSuffix(String contentKey) {
		return contentKey.substring(0, contentKey.length() - METADATA_SUFFIX.length() - 1);
	}
}
