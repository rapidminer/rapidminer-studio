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
package com.rapidminer.tools.encryption;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.KeysetReader;
import com.google.crypto.tink.KeysetWriter;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;


/**
 * File utils for dealing with persisted keyset handle files.
 *
 * @author Marco Boeck
 * @since 9.7
 */
enum EncryptionFileUtils {

	INSTANCE;


	/**
	 * Simple container with a keyset handle, the context it is for, and the encryption type.
	 */
	static class KeysetHandleContainer {

		private KeysetHandle keysetHandle;
		private String context;
		private EncryptionType encryptionType;

		KeysetHandleContainer(KeysetHandle keysetHandle, String context, EncryptionType encryptionType) {
			this.keysetHandle = keysetHandle;
			this.context = context;
			this.encryptionType = encryptionType;
		}

		public KeysetHandle getKeysetHandle() {
			return keysetHandle;
		}

		public String getContext() {
			return context;
		}

		public EncryptionType getEncryptionType() {
			return encryptionType;
		}
	}

	/**
	 * Map containing mapping between {@link EncryptionType} and a string for path resolution. Must be updated when a new {@link EncryptionType} is added!
	 */
	private static final Map<EncryptionType, String> TYPE_TO_NAME_MAP = new HashMap<>();
	private static final String ENCRYPTION_FILE_SUFFIX = ".rmek";
	static {
		TYPE_TO_NAME_MAP.put(EncryptionType.SYMMETRIC, "symmetric");
		TYPE_TO_NAME_MAP.put(EncryptionType.STREAMING, "streaming");
	}


	/**
	 * Tries to load all keyset handle files from the .RapidMiner folder.
	 *
	 * @return the containers containing all handlers found on disk, can be empty but never {@code null}
	 */
	List<KeysetHandleContainer> loadAllKeysetHandlesFromDisk() {
		Path encryptionFolder = FileSystemService.getUserRapidMinerDir().toPath().resolve(FileSystemService.RAPIDMINER_ENCRYPTION_FOLDER);

		if (!Files.exists(encryptionFolder)) {
			try {
				Files.createDirectories(encryptionFolder);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, e, () -> "Failed to create encryption folder");
			}
		}

		List<KeysetHandleContainer> containers = new ArrayList<>();
		for (EncryptionType encryptionType : EncryptionType.values()) {
			String folderNameForEncryptionType = getPathForEncryptionType(encryptionType);
			if (folderNameForEncryptionType == null) {
				LogService.getRoot().log(Level.WARNING, () -> String.format("Unrecognized encryption type %s, cannot load existing encryption keys", encryptionType));
				continue;
			}

			Path typeFolder = encryptionFolder.resolve(folderNameForEncryptionType);

			if (!Files.exists(typeFolder)) {
				try {
					Files.createDirectories(typeFolder);
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, e, () -> String.format("Failed to create encryption folder %s", folderNameForEncryptionType));
				}
				continue;
			}

			List<Path> files;
			try (Stream<Path> fileStream = Files.list(typeFolder)) {
				files = fileStream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(ENCRYPTION_FILE_SUFFIX)).collect(Collectors.toList());
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, e, () -> String.format("Failed to list files in encryption folder %s, cannot load existing encryption keys", folderNameForEncryptionType));
				continue;
			}

			for (Path encryptionFile : files) {
				String context = RepositoryTools.removeSuffix(encryptionFile.getFileName().toString());
				try {
					// yes, Google Tink fails to close their streams if using any of the other utility methods, so we have to pass our own stream to avoid a file handler leak...
					// https://github.com/google/tink/issues/316
					InputStream inputStream = Files.newInputStream(encryptionFile);
					KeysetReader reader = JsonKeysetReader.withInputStream(inputStream);
					KeysetHandle keysetHandle = CleartextKeysetHandle.read(reader);
					containers.add(new KeysetHandleContainer(keysetHandle, context, encryptionType));
					try {
						inputStream.close();
					} catch (Exception e) {
						LogService.getRoot().log(Level.WARNING, "Failed to close encryption keyset handle reader!", e);
					}
				} catch (IOException | GeneralSecurityException e) {
					LogService.getRoot().log(Level.WARNING, e, () -> String.format("Failed to load encryption keyset handle file from disk for context: %s", context));
				}
			}
		}

		return containers;
	}

	/**
	 * Stores the given keyset handle for the given context and encryption type on disk. Will overwrite an existing file.
	 *
	 * @param keysetHandle   the keyset handle to persist to disk
	 * @param context        the context key of the keyset handle to persist
	 * @param encryptionType the {@link EncryptionType} of the keyset handle to persist
	 * @throws IOException if storing on disk fails
	 */
	void storeOnDisk(KeysetHandle keysetHandle, String context, EncryptionType encryptionType) throws IOException {
		// try to delete first
		deleteFromDisk(context, encryptionType);

		// yes, Google Tink fails to close their streams if using any of the other utility methods, so we have to pass our own stream to avoid a file handler leak...
		// https://github.com/google/tink/issues/316
		OutputStream outputStream = Files.newOutputStream(createFilePath(context, encryptionType));
		KeysetWriter writer = JsonKeysetWriter.withOutputStream(outputStream);
		CleartextKeysetHandle.write(keysetHandle, writer);
		try {
			outputStream.close();
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Failed to close encryption keyhandle writer!", e);
		}
	}

	/**
	 * Tries to delete the encryption keyset handle file for the given context and encryption type from disk. If the file does not exist,
	 * nothing happens.
	 *
	 * @param context        the context key of the keyset handle to delete
	 * @param encryptionType the {@link EncryptionType} of the keyset handle to delete
	 * @throws IOException if deleting on disk fails
	 */
	void deleteFromDisk(String context, EncryptionType encryptionType) throws IOException {
		Path filePath = createFilePath(context, encryptionType);
		if (Files.exists(filePath)) {
			Files.delete(filePath);
		}
	}

	private Path createFilePath(String context, EncryptionType encryptionType) {
		return FileSystemService.getUserRapidMinerDir().toPath().
				resolve(FileSystemService.RAPIDMINER_ENCRYPTION_FOLDER).
				resolve(getPathForEncryptionType(encryptionType)).
				resolve(context + ENCRYPTION_FILE_SUFFIX);
	}

	/**
	 * @return the string or {@code null} if type not contained in TYPE_TO_NAME_MAP
	 */
	private String getPathForEncryptionType(EncryptionType encryptionType) {
		return TYPE_TO_NAME_MAP.get(encryptionType);
	}

}
