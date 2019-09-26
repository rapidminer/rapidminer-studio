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
package com.rapidminer.connection;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeLinkButton;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.ListenerTools;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;


/**
 * Util method collection for loading, copying and moving {@link ConnectionInformation}
 *
 * @author Jan Czogalla, Andreas Timm
 * @since 9.3
 */
public final class ConnectionInformationFileUtils {

	/**
	 * {@link java.nio.file.FileVisitor} that creates or resolves cache files.
	 * The cache is created in the given target, mirroring the directory structure and saving each file inside the directory
	 * {@code <file name>/<md5 hash>}. For files that do not have a corresponding {@code .md5} path the hash
	 * will be calculated if possible, otherwise they will be ignored.
	 *
	 * @author Jan Czogalla
	 * @since 9.3
	 */
	private static final class CachingFileVisitor extends SimpleFileVisitor<Path> {

		private final Path source;
		private final Path target;
		private final List<Path> fileList;

		private CachingFileVisitor(Path source, Path target, List<Path> fileList) {
			if (source.toFile().isFile()) {
				source = source.getParent();
			}
			this.source = source;
			this.target = target;
			this.fileList = fileList;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if ((attrs == null ? file.toFile().isDirectory() : attrs.isDirectory()) || file.getFileName().endsWith(ConnectionInformationSerializer.MD5_SUFFIX)) {
				return FileVisitResult.CONTINUE;
			}
			// check if it already is a cache file
			if (file.startsWith(target.toString())) {
				return FileVisitResult.CONTINUE;
			}
			String md5Hash = getMD5Hex(file);
			if (md5Hash == null) {
				// skip files without valid md5
				return FileVisitResult.CONTINUE;
			}
			Path relative = source.relativize(file);
			// resolve cached file location
			Path cacheLocation = target.resolve(Paths.get(relative.toString(), md5Hash, file.getFileName().toString()));
			if (cacheLocation.toFile().exists()) {
				// cache exists, collect
				fileList.add(cacheLocation);
				return FileVisitResult.CONTINUE;
			}
			// create new cached file
			Files.createDirectories(cacheLocation.getParent());
			fileList.add(Files.copy(file, cacheLocation));
			return FileVisitResult.CONTINUE;
		}

		/**
		 * Get md5 hash from sibling .md5 file or calculate it using {@link DigestUtils#md5Hex(InputStream)}
		 */
		private String getMD5Hex(Path file) {
			if (!file.toFile().exists()) {
				return null;
			}
			Path md5File = file.getParent().resolve(file.getFileName() + ConnectionInformationSerializer.MD5_SUFFIX);
			if (md5File.toFile().exists()) {
				// read md5 hash from file if possible
				try (BufferedReader br = Files.newBufferedReader(md5File)) {
					return StringUtils.trimToNull(br.readLine());
				} catch (IOException e) {
					// ignore, handling below
				}
			}
			// calculate md5 hash
			try (InputStream is = Files.newInputStream(file)) {
				return DigestUtils.md5Hex(is);
			} catch (IOException e) {
				// ignore
			}
			return null;
		}
	}

	private static final String KEY_CLEAR_CACHE_NOW = "connection.clear_cache_now";
	private static final String PARAMETER_KEEP_CACHE = "rapidminer.system.file_cache.connection.keep";
	private static final String PARAMETER_CLEAR_CACHE_ONCE = "rapidminer.system.file_cache.connection.clear_once";

	private static final String STRATEGY_INDEFINITELY = "indefinitely";
	private static final String[] KEEP_CACHE_STRATEGIES = {STRATEGY_INDEFINITELY, "never"};

	/** Action to force clearing the cache and restarting Rapidminer */
	private static final ResourceAction CLEAR_CACHE_NOW_ACTION = new ResourceAction(KEY_CLEAR_CACHE_NOW) {
		@Override
		protected void loggedActionPerformed(ActionEvent e) {
			int result = SwingTools.showConfirmDialog(KEY_CLEAR_CACHE_NOW, ConfirmDialog.OK_CANCEL_OPTION);
			if (result == ConfirmDialog.CANCEL_OPTION) {
				return;
			}
			RapidMiner.addShutdownHook(ConnectionInformationFileUtils::clearCache);
			ParameterService.setParameterValue(PARAMETER_CLEAR_CACHE_ONCE, "true");
			ParameterService.saveParameters();
			RapidMinerGUI.getMainFrame().exit(true);
		}
	};

	/**
	 * Utility class.
	 */
	private ConnectionInformationFileUtils() {
		throw new AssertionError("Do not instantiate this utility class");
	}

	/** Initialize the {@link ParameterType} for the {@value #PARAMETER_KEEP_CACHE} setting. */
	public static void initSettings() {
		ParameterType cacheType = new ParameterTypeTupel(PARAMETER_KEEP_CACHE, "",
				new ParameterTypeCategory("keep_strategy", "", KEEP_CACHE_STRATEGIES, 0),
				new ParameterTypeLinkButton("clear_cache", "", CLEAR_CACHE_NOW_ACTION));
		RapidMiner.registerParameter(cacheType, "system");
	}

	/**
	 * Checks if the cache should be cleared using the parameters {@value #PARAMETER_KEEP_CACHE} and {@value #PARAMETER_CLEAR_CACHE_ONCE}.
	 * If the cache should not be kept or if it should be cleared once, {@link #clearCache()} is called to clean up.
	 * This should only be called during Studio startup to prevent breaking class loaders and others.
	 */
	public static void checkForCacheClearing() {
		String keepCacheValue = ParameterService.getParameterValue(PARAMETER_KEEP_CACHE);
		keepCacheValue = ParameterTypeTupel.transformString2Tupel(keepCacheValue)[0];
		boolean keepCache = keepCacheValue == null || keepCacheValue.equals(STRATEGY_INDEFINITELY);
		boolean clearOnce = Boolean.parseBoolean(ParameterService.getParameterValue(PARAMETER_CLEAR_CACHE_ONCE));
		if (!keepCache || clearOnce) {
			clearCache();
		}
		if (clearOnce) {
			ParameterService.setParameterValue(PARAMETER_CLEAR_CACHE_ONCE, "false");
			ParameterService.saveParameters();
		}
	}

	/**
	 * Turns all regular files in the given path into {@link Path Paths} in the file system,
	 * by either creating a cached version of the file or resolving it to an existing cached file.
	 * The cache is created in the user's .RapidMiner directory.
	 *
	 * @param path
	 * 		the path to a single file to cache or a subfolder to cache all children
	 * @see CachingFileVisitor
	 */
	public static List<Path> getOrCreateCacheFiles(Path path) throws IOException {
		Path cacheBaseLocation = getCacheLocation();
		List<Path> fileList = new ArrayList<>();
		if (path.toFile().exists()) {
			Files.walkFileTree(path, Collections.emptySet(), 1, new CachingFileVisitor(path, cacheBaseLocation, fileList));
		}
		return fileList;
	}

	/**
	 * Saves this connection information after updating either the configuration or statistics
	 * <p>
	 * <strong>Note:</strong> This might overwrite changes done in the file system.
	 */
	public static void save(ConnectionInformation connectionInformation, Path zipFile) throws IOException {
		if (zipFile == null) {
			throw new NoSuchFileException("Target file was not set");
		}
		try (OutputStream out = new FileOutputStream(zipFile.toFile())) {
			ConnectionInformationSerializer.LOCAL.serialize(connectionInformation, out);
		}
	}

	/**
	 * Moves this connection information to the designated new location.
	 * <p>
	 * <strong>Note:</strong> This will overwrite an existing file without asking. Make sure to check for overwriting first.
	 *
	 * @param target
	 * 		the target {@link Path}; must not be {@code null}
	 */
	public static void moveTo(Path sourcePath, Path target) throws IOException {
		Files.move(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
	}


	/**
	 * Creates and returns a copy of this connection information at the designated new location.
	 * <p>
	 * <strong>Note:</strong> This will overwrite an existing file without asking. Make sure to check for overwriting first.
	 *
	 * @param target
	 * 		the target path; must not be {@code null}
	 */
	public static void copyTo(Path source, Path target) throws IOException {
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
	}


	/**
	 * Copies the given files to the specified {@code root/dirName} and creates {@code .md5} hash files for each.
	 */
	static void copyFilesToZip(Path root, List<Path> filesToCopy, String dirName) throws IOException {
		if (filesToCopy == null || filesToCopy.isEmpty() || dirName == null) {
			return;
		}
		Path path = root.resolve(dirName);
		Files.createDirectory(path);
		MessageDigest md5 = DigestUtils.getMd5Digest();
		for (Path fileToCopy : filesToCopy) {
			Path fileName = fileToCopy.getFileName();
			Path nestedFilePath = path.resolve(fileName.toString());
			try (InputStream is = Files.newInputStream(fileToCopy);
				 DigestInputStream dis = new DigestInputStream(is, md5);
				 BufferedWriter md5Writer = Files.newBufferedWriter(path.resolve(fileName + ConnectionInformationSerializer.MD5_SUFFIX))) {
				Files.copy(dis, nestedFilePath);
				md5Writer.write(Hex.encodeHexString(md5.digest()));
			}
		}
	}

	/**
	 * Load a {@link ConnectionInformation} from an existing zip file.
	 *
	 * @param zipFile
	 * 		the zip file to load from; must not be {@code null}
	 */
	public static ConnectionInformation loadFromZipFile(Path zipFile) throws IOException {
		return ConnectionInformationSerializer.LOCAL.loadConnection(new FileInputStream(zipFile.toFile()));
	}

	/**
	 * This method tries to add native libraries from a connection (defined as other files ending in *.so, *.dylib, and
	 * *.dll) to the native library lookup path. If a native lib has already been added, it will not be added again. If
	 * the connection does not have any files when calling {@link ConnectionInformation#getOtherFiles()}, nothing
	 * happens.
	 *
	 * @param ci the connection, never {@code null}
	 * @since 9.4.0
	 */
	public static void addNativeLibraries(ConnectionInformation ci) {
		if (ci == null) {
			throw new IllegalArgumentException("ci must not be null!");
		}

		for (Path otherFile : ci.getOtherFiles()) {
			String fileName = otherFile.getFileName().toString();
			if (isNativeLibrary(fileName)) {
				String parentFolderPath = otherFile.getParent().toAbsolutePath().toString();

				// add folders to usr_paths in ClassLoader (static variable), if they are not yet contained in there
				AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
					try {
						final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
						usrPathsField.setAccessible(true);
						String[] paths = (String[]) usrPathsField.get(null);
						if (paths == null) {
							// if not yet initialized, trigger loading a fake lib so the arrays in the Classloader class get initialized
							try {
								System.loadLibrary(UUID.randomUUID().toString());
							} catch (Throwable t) {
								// ignore
							}
							paths = (String[]) usrPathsField.get(null);
						}
						boolean skipLoading = false;
						// need to add path?
						for (String path : paths) {
							if (path != null && path.equals(parentFolderPath)) {
								skipLoading = true;
								break;
							}
						}

						if (!skipLoading) {
							// path not yet contained, add it
							final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
							newPaths[newPaths.length - 1] = parentFolderPath;
							usrPathsField.set(null, newPaths);
							LogService.getRoot().log(Level.INFO, "com.rapidminer.connection.ConnectionInformationFileUtils.added_to_native_path", fileName);
						}
					} catch (Throwable t) {
						LogService.getRoot().log(Level.SEVERE, "com.rapidminer.connection.ConnectionInformationFileUtils.failed_add_native_path", fileName);
						LogService.getRoot().log(Level.SEVERE, "", t);
					}

					return null;
				});
			}
		}
	}

	/**
	 * Checks if the given file name indicates a native library (ends in *.dll, *.so, or *.dylib).
	 *
	 * @param fileName the file name including file suffix, must not be {@code null}
	 * @return {@code true} if it is a native library according to the file name; {@code false} otherwise
	 * @since 9.4.0
	 */
	public static boolean isNativeLibrary(String fileName) {
		if (fileName == null) {
			throw new IllegalArgumentException("fileName must not be null!");
		}

		return fileName.endsWith(".so") || fileName.endsWith(".dylib") || fileName.endsWith(".dll");
	}

	private static Path getCacheLocation() {
		return FileSystemService.getUserRapidMinerDir().toPath().resolve(FileSystemService.RAPIDMINER_INTERNAL_CACHE_CONNECTION_FULL);
	}

	/**
	 * Internally store the file in a cached folder, will not overwrite existing
	 *
	 * @param name
	 * 		of the file to be added
	 * @param inputStream
	 * 		source stream of the file
	 * @param md5Hash
	 * 		of the file, used for caching, will be calculated if missing
	 */
	static Path addFileInternally(String name, InputStream inputStream, String md5Hash) throws IOException {
		final MessageDigest msgDigest = DigestUtils.getMd5Digest();
		inputStream = new DigestInputStream(inputStream, msgDigest);
		final Path tempFile = Files.createTempFile("conninfo", "tmp");
		try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
			IOUtils.copy(inputStream, fos);
		}
		String testHash = Hex.encodeHexString(msgDigest.digest());
		tempFile.toFile().deleteOnExit();

		if (md5Hash == null) {
			md5Hash = testHash;
		} else if (!md5Hash.equals(testHash)) {
			silentDelete(tempFile);
			throw new IOException("Mismatched md5 hash!");
		}

		final Path cacheLocation = ConnectionInformationFileUtils.getCacheLocation().resolve(name).resolve(md5Hash).resolve(name);

		if (!cacheLocation.toFile().exists()) {
			try {
				Files.createDirectories(cacheLocation.getParent());
				moveTo(tempFile, cacheLocation);
			} catch (IOException e) {
				silentDelete(tempFile);
				throw e;
			}
		}
		return cacheLocation;
	}

	/** Silently delete the given path. */
	private static void silentDelete(Path tempFile) {
		try {
			Files.delete(tempFile);
		} catch (IOException e) {
			// ignore
		}
	}

	/** Clear the connection file cache. Will log the first occurring error and how many errors occurred in total. */
	private static void clearCache() {
		try (Stream<Path> paths = Files.walk(getCacheLocation())) {
			ListenerTools.informAllAndThrow(paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList()),
					Files::delete, IOException.class);
		} catch (IOException ioe) {
			Throwable[] suppressed = ioe.getSuppressed();
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.file_cache.unable_to_delete", new Object[]{ioe.getMessage(), suppressed.length + 1});
		}
	}
}
