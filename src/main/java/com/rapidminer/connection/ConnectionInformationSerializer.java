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
package com.rapidminer.connection;

import static com.rapidminer.connection.ConnectionInformation.DIRECTORY_NAME_FILES;
import static com.rapidminer.connection.ConnectionInformation.DIRECTORY_NAME_LIB;
import static com.rapidminer.connection.ConnectionInformation.ENTRY_NAME_ANNOTATIONS;
import static com.rapidminer.connection.ConnectionInformation.ENTRY_NAME_CONFIG;
import static com.rapidminer.connection.ConnectionInformation.ENTRY_NAME_STATS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.NonClosingZipInputStream;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * ConnectionInformationSerializer used for reading and writing of {@link ConnectionInformation} objects
 *
 * @author Jan Czogalla, Andreas Timm, Jonas Wilms-Pfau, Marco Boeck
 * @since 9.3
 */
public final class ConnectionInformationSerializer {

	/**
	 * Serializer which must be used to convert JSON to Connection related objects / the other way around.
	 *
	 * @since 9.7
	 */
	public static final ConnectionInformationSerializer INSTANCE = new ConnectionInformationSerializer(createObjectMapper());

	/**
	 * Default Charset used for storing
	 */
	static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * File extension of the MD5 hash text file for bundled files
	 */
	static final String MD5_SUFFIX = ".md5";

	/**
	 * File separator according to .ZIP File Format Specification 4.4.17.1 and ISO/IEC 21320-1:2015
	 */
	private static final char ZIP_FILE_SEPARATOR_CHAR = '/';

	/**
	 * The encryption context provider for each thread. As we need to encrypt fields that need to be encrypted when
	 * writing JSON, but it's not easily possible to get a stateful handler to those places, we need to reference the
	 * currently required encryption context per thread. As only one write/load operation can take place in any given
	 * thread at the same time, this works fine. It's not the most elegant solution, but works without problems.
	 *
	 * @since 9.7
	 */
	private static final ThreadLocal<String> ENCRYPTION_CONTEXT_PROVIDER = ThreadLocal.withInitial(() -> EncryptionProvider.DEFAULT_CONTEXT);


	/**
	 * ObjectWriter, kept a cache version for faster access
	 */
	private final ObjectWriter objectWriter;

	/**
	 * ObjectMapper, cached for faster access
	 */
	private final ObjectMapper objectMapper;

	/**
	 * Reader for ConnectionStatistics
	 */
	private final ObjectReader connectionStatisticsReader;

	/**
	 * Reader for ConnectionConfiguration
	 */
	private final ObjectReader connectionConfigurationReader;


	/**
	 * Creates a new ConnectionInformationSerializer
	 *
	 * @param objectMapper The preconfigured objectMapper
	 */
	private ConnectionInformationSerializer(ObjectMapper objectMapper) {
		ValidationUtil.requireNonNull(objectMapper, "objectMapper");
		this.objectMapper = objectMapper;
		this.objectWriter = objectMapper.writer();
		ObjectReader reader = objectMapper.reader();
		this.connectionStatisticsReader = reader.forType(ConnectionStatistics.class);
		this.connectionConfigurationReader = reader.forType(ConnectionConfiguration.class);
	}

	/**
	 * Load a {@link ConnectionConfiguration} from the inputStream. Will use the specified encryption context (see
	 * {@link EncryptionProvider}) when trying to decrypt encrypted values.
	 *
	 * @param inputStream       the input stream, must not be {@code null}
	 * @param encryptionContext the encryption context that will be used to potentially decrypt values (see {@link
	 *                          com.rapidminer.tools.encryption.EncryptionProvider})
	 * @return the configuration or {@code null} if {@code null} was passed
	 * @throws IOException if converting the stream to the configuration goes wrong
	 * @since 9.7
	 */
	public ConnectionConfiguration loadConfiguration(InputStream inputStream, String encryptionContext) throws IOException {
		if (inputStream == null) {
			return null;
		}

		try (ConnectionEncryptionContextSwapper swapper = ConnectionEncryptionContextSwapper.withEncryptionContext(encryptionContext, ENCRYPTION_CONTEXT_PROVIDER::get, ENCRYPTION_CONTEXT_PROVIDER::set)) {
			return connectionConfigurationReader.readValue(inputStream);
		}
	}

	/**
	 * Load a {@link ConnectionInformation} from the given {@link InputStream} and {@link RepositoryLocation}. The
	 * {@link ConnectionConfiguration} will get the repository location's name as its name. Will use the specified
	 * encryption context (see {@link EncryptionProvider}) when trying to decrypt encrypted values.
	 *
	 * @param stream             the {@link InputStream} to read the {@link ConnectionInformation} content from. This
	 *                           should be the data that was produced by {@link ConnectionInformationSerializer#serialize(ConnectionInformation,
	 *                           OutputStream, String)}
	 * @param repositoryLocation the repository location this entry belongs too; might be {@code null}
	 * @param encryptionContext  the encryption context that will be used to potentially decrypt values (see {@link
	 *                           com.rapidminer.tools.encryption.EncryptionProvider})
	 * @return the {@link ConnectionInformation} the stream contained
	 * @throws IOException in case of reading errors
	 * @since 9.7
	 */
	public ConnectionInformation loadConnection(InputStream stream, RepositoryLocation repositoryLocation, String encryptionContext) throws IOException {
		ConnectionInformationImpl ci = new ConnectionInformationImpl();
		NonClosingZipInputStream zis = new NonClosingZipInputStream(stream);
		// track found files, need the file's input stream and the md5 to add it
		Map<String, String> md5hashForLibFile = new HashMap<>();
		Map<String, String> md5hashForOtherFile = new HashMap<>();
		Map<String, InputStream> libFileStreams = new HashMap<>();
		Map<String, InputStream> otherFileStreams = new HashMap<>();

		for (ZipEntry zipEntry; (zipEntry = zis.getNextEntry()) != null; ) {
			if (zipEntry.isDirectory()) {
				zis.closeEntry();
				continue;
			}
			if (ENTRY_NAME_CONFIG.equals(zipEntry.getName())) {
				ConnectionConfiguration configuration = loadConfiguration(zis, encryptionContext);
				// sync config name with repo location
				if (repositoryLocation != null) {
					String name = repositoryLocation.getName();
					ci.configuration = new ConnectionConfigurationBuilder(configuration, name).build();
				} else {
					ci.configuration = configuration;
				}
			} else if (ENTRY_NAME_STATS.equals(zipEntry.getName())) {
				ci.statistics = loadStatistics(zis);
			} else if (ENTRY_NAME_ANNOTATIONS.equals(zipEntry.getName())) {
				ci.annotations = Annotations.fromPropertyStyle(zis);
			} else if (zipEntry.getName().endsWith(MD5_SUFFIX)) {
				final String md5Hash = IOUtils.toString(zis, DEFAULT_CHARSET);
				if (zipEntry.getName().startsWith(DIRECTORY_NAME_FILES)) {
					final String filename = zipEntry.getName().substring(DIRECTORY_NAME_FILES.length() + 1, zipEntry.getName().length() - MD5_SUFFIX.length());
					handleOtherMD5(ci, md5hashForOtherFile, otherFileStreams, md5Hash, filename);
				} else if (zipEntry.getName().startsWith(DIRECTORY_NAME_LIB)) {
					final String filename = zipEntry.getName().substring(DIRECTORY_NAME_LIB.length() + 1, zipEntry.getName().length() - MD5_SUFFIX.length());
					handleLibMD5(ci, md5hashForLibFile, libFileStreams, md5Hash, filename);
				} else {
					LogService.getRoot().log(Level.WARNING, "Could not use entry from connection information called '" + zipEntry.getName() + "'");
				}
			} else if (zipEntry.getName().startsWith(DIRECTORY_NAME_FILES)) {
				final String filename = zipEntry.getName().substring(DIRECTORY_NAME_FILES.length() + 1);
				handleOtherFile(ci, md5hashForOtherFile, otherFileStreams, zis, filename);

			} else if (zipEntry.getName().startsWith(DIRECTORY_NAME_LIB)) {
				final String filename = zipEntry.getName().substring(DIRECTORY_NAME_LIB.length() + 1);
				handleLibFile(ci, md5hashForLibFile, libFileStreams, zis, filename);
			} else {
				LogService.getRoot().log(Level.WARNING, "Could not use entry from connection information called '" + zipEntry.getName() + "'");
			}
			zis.closeEntry();
		}
		zis.close();
		zis.close2();


		// The following code is only used for the very unlikely case that the md5 file is missing
		if (!otherFileStreams.isEmpty()) {
			otherFileStreams.forEach((k, v) -> {
				try {
					ci.addOtherFile(k, v, null);
				} catch (IOException e) {
					LogService.getRoot().log(Level.SEVERE, "Could not add file to connection information", e);
				}
			});
		}
		if (!libFileStreams.isEmpty()) {
			libFileStreams.forEach((k, v) -> {
				try {
					ci.addLibFile(k, v, null);
				} catch (IOException e) {
					LogService.getRoot().log(Level.SEVERE, "Could not add lib file to connection information", e);
				}
			});
		}

		try {
			if (repositoryLocation != null) {
				ci.repository = repositoryLocation.getRepository();
			}
		} catch (RepositoryException e) {
			// ignore
		}
		return ci;
	}

	/**
	 * Get an {@link InputStream} containing the storage structure of a {@link ConnectionInformation}. It is a zipped
	 * file containing a Config entry for standard configuration, a Stats entry for usage stats and two folder for Lib
	 * and Other files. Will use the specified encryption context (see {@link EncryptionProvider}) when trying to
	 * decrypt encrypted values.
	 *
	 * @param connectionInformation the {@link ConnectionInformation} to be stored
	 * @param out                   the outputStream to write to
	 * @param encryptionContext     the encryption context that will be used to potentially encrypt values (see {@link
	 *                              com.rapidminer.tools.encryption.EncryptionProvider})
	 * @throws IOException in case creating the result was not possible
	 * @since 9.7
	 */
	public void serialize(ConnectionInformation connectionInformation, OutputStream out, String encryptionContext) throws IOException {
		if (connectionInformation == null) {
			throw new IOException("Object connection information is null");
		}

		if (out == null) {
			throw new IOException("The output stream is null");
		}

		try (ZipOutputStream zos = new ZipOutputStream(out)) {
			zos.setLevel(ZipOutputStream.STORED);
			try (ConnectionEncryptionContextSwapper swapper = ConnectionEncryptionContextSwapper.withEncryptionContext(encryptionContext, ENCRYPTION_CONTEXT_PROVIDER::get, ENCRYPTION_CONTEXT_PROVIDER::set)) {
				serializeAsZipEntry(ENTRY_NAME_CONFIG, connectionInformation.getConfiguration(), zos);
			}

			if (connectionInformation.getStatistics() != null) {
				serializeAsZipEntry(ENTRY_NAME_STATS, connectionInformation.getStatistics(), zos);
			}

			if (connectionInformation.getAnnotations() != null) {
				serializeAsZipEntry(ENTRY_NAME_ANNOTATIONS, connectionInformation.getAnnotations(), zos);
			}

			writeAsZipEntriesWithMD5(Paths.get(DIRECTORY_NAME_LIB), connectionInformation.getLibraryFiles(), zos);
			writeAsZipEntriesWithMD5(Paths.get(DIRECTORY_NAME_FILES), connectionInformation.getOtherFiles(), zos);
			zos.finish();
		}
	}

	/**
	 * Helper method to create the serialized {@link ConnectionInformation}. Write an {@link Object} as JSON format to
	 * the {@link OutputStream}.
	 *
	 * @param out               {@link OutputStream} to write to
	 * @param object            to be written
	 * @param encryptionContext the encryption context that will be used to potentially encrypt values (see {@link
	 *                          com.rapidminer.tools.encryption.EncryptionProvider})
	 * @throws IOException if writing failed
	 * @since 9.7
	 */
	public void writeJson(OutputStream out, Object object, String encryptionContext) throws IOException {
		if (out != null && object != null) {
			try (ConnectionEncryptionContextSwapper swapper = ConnectionEncryptionContextSwapper.withEncryptionContext(encryptionContext, ENCRYPTION_CONTEXT_PROVIDER::get, ENCRYPTION_CONTEXT_PROVIDER::set)) {
				objectWriter.writeValue(out, object);
			}
		}
	}


	/**
	 * Helper method to create a JSON representation of the given input object related to connections.
	 *
	 * @param object            the input object that should be serialized to JSON, must not be {@code null}
	 * @param encryptionContext the encryption context that will be used to potentially encrypt values (see {@link
	 *                          com.rapidminer.tools.encryption.EncryptionProvider})
	 * @return the json of the input object
	 * @throws IOException if serialization fails
	 * @since 9.7
	 */
	public String createJsonFromObject(Object object, String encryptionContext) throws IOException {
		try (ConnectionEncryptionContextSwapper swapper = ConnectionEncryptionContextSwapper.withEncryptionContext(encryptionContext, ENCRYPTION_CONTEXT_PROVIDER::get, ENCRYPTION_CONTEXT_PROVIDER::set)) {
			return objectWriter.writeValueAsString(object);
		}
	}

	/**
	 * Helper method to create a deserialized object related to connections from the given JSON string.
	 *
	 * @param json              the json string, must not be {@code null}
	 * @param targetClass       the target class that the JSON should be deserialized to
	 * @param collectionClass   optional. If the expected output is a collection of the targetClass, this needs to be
	 *                          set to the collection type, e.g. {@code List.class}. If {@code null}, a single instance
	 *                          of type targetClass is expected
	 * @param encryptionContext the encryption context that will be used to potentially decrypt values (see {@link
	 *                          com.rapidminer.tools.encryption.EncryptionProvider})
	 * @return the deserialized object
	 * @throws IOException if deserialization fails
	 * @since 9.7
	 */
	@SuppressWarnings({"rawtypes"})
	public <T> T createObjectFromJson(String json, Class<T> targetClass, Class<? extends Collection> collectionClass, String encryptionContext) throws IOException {
		try (ConnectionEncryptionContextSwapper swapper = ConnectionEncryptionContextSwapper.withEncryptionContext(encryptionContext, ENCRYPTION_CONTEXT_PROVIDER::get, ENCRYPTION_CONTEXT_PROVIDER::set)) {
			if (collectionClass != null) {
				CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(collectionClass, targetClass);
				return objectMapper.readValue(json, collectionType);
			} else {
				return objectMapper.readValue(json, targetClass);
			}
		}
	}

	/**
	 * Creates a deep copy of the given JSON Java object. Will also take care of cases where the given object is a
	 * collection of objects. Note: Do not pass collections of collections, those will fail.
	 *
	 * @param jsonObject the input object that is serializable to JSON; will not be modified. If it contains encrypted
	 *                   values, they will be passed as-is to the new copy
	 * @return a object of the same type and with the same values, but technically is a completely separate object with
	 * no references to the input object. Encrypted values will be the same as before
	 * @throws IOException if something goes wrong during the copy or a non-JSON capable object has been provided
	 */
	@SuppressWarnings({"unchecked, rawtypes"})
	public <T> T createDeepCopy(T jsonObject) throws IOException {
		if (jsonObject == null) {
			return null;
		}

		try {
			return AccessController.doPrivileged((PrivilegedExceptionAction<T>) () -> {
				ObjectMapper mapper = createObjectMapper();

				// we need to check if its a regular simple POJO or if it is a collection of POJOs
				Class<? extends Collection> collectionClass = Collection.class.isAssignableFrom(jsonObject.getClass()) ? (Class<? extends Collection>) jsonObject.getClass() : null;
				if (collectionClass != null) {
					Collection collection = (Collection) jsonObject;
					if (collection.isEmpty()) {
						return (T) mapper.readValue(mapper.writeValueAsString(jsonObject), jsonObject.getClass());
					} else {
						Object innerObject = collection.iterator().next();
						CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(collectionClass, innerObject.getClass());
						return mapper.readValue(mapper.writeValueAsString(jsonObject), collectionType);
					}
				} else {
					return (T) mapper.readValue(mapper.writeValueAsString(jsonObject), jsonObject.getClass());
				}
			});
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof IOException) {
				throw (IOException) e.getException();
			} else if (e.getException() instanceof RuntimeException) {
				throw (RuntimeException) e.getException();
			} else {
				throw new IOException(e.getException());
			}
		}
	}

	/**
	 * Create {@link com.rapidminer.operator.ports.metadata.MetaData} for a {@link ConnectionInformation}.
	 *
	 * @param connectionInformation
	 * 		for which {@link com.rapidminer.operator.ports.metadata.MetaData} is required
	 * @return ConnectionInformationMetaData unless the connectionInformation is null, then it will be null
	 */
	public ConnectionInformationMetaData getMetaData(ConnectionInformation connectionInformation) {
		if (connectionInformation == null) {
			return null;
		}
		return new ConnectionInformationMetaData(connectionInformation.getConfiguration());
	}

	/**
	 * <p>Internal API, do not use!</p>
	 * The encryption context is set via {@link ThreadLocal} whenever {@link #writeJson(OutputStream, Object, String)},
	 * {@link #serialize(ConnectionInformation, OutputStream, String)}, {@link #loadConfiguration(InputStream, String)},
	 * or {@link #loadConnection(InputStream, RepositoryLocation, String)} are called.
	 *
	 * @return the encryption context for the current thread
	 */
	public String getEncryptionContextForCurrentThread() {
		return ENCRYPTION_CONTEXT_PROVIDER.get();
	}

	/**
	 * Load the {@link ConnectionStatistics} from an {@link InputStream}, mainly for deserializing a {@link
	 * ConnectionInformation}
	 *
	 * @param inputStream
	 * 		that contains the {@link ConnectionStatistics}
	 * @return the {@link ConnectionStatistics} that was read from the inputStream, can be null
	 * @throws IOException
	 * 		if reading failed
	 */
	ConnectionStatistics loadStatistics(InputStream inputStream) throws IOException {
		return connectionStatisticsReader.readValue(inputStream);
	}

	/**
	 * Copies the given files to the specified {@code root/dirName} and creates {@code .md5} hash files for each.
	 *
	 * @param targetFolder the target folder
	 * @param filesToCopy  the files to write
	 * @param zos          the ZipOutputStream to write to
	 * @throws IOException if writing the zip entry goes wrong
	 */
	static void writeAsZipEntriesWithMD5(Path targetFolder, List<Path> filesToCopy, ZipOutputStream zos) throws IOException {
		if (filesToCopy == null || filesToCopy.isEmpty()) {
			return;
		}

		if (targetFolder == null) {
			targetFolder = Paths.get("");
		}

		if (zos == null) {
			throw new IOException("Zip output stream cannot be null");
		}

		MessageDigest md5 = DigestUtils.getMd5Digest();
		for (Path file : filesToCopy) {
			String fileName = targetFolder.resolve(file.getFileName()).toString().replace(File.separatorChar, ZIP_FILE_SEPARATOR_CHAR);
			ZipEntry entry = new ZipEntry(fileName);
			ZipEntry md5Entry = new ZipEntry(fileName + MD5_SUFFIX);

			// We read every file twice to speed up reading later
			// - We don't have to copy every file into memory (see else case in handleOtherFile)
			// - Connections should be way often read than written

			// Write Checksum first
			try (InputStream is = Files.newInputStream(file);
				 DigestInputStream dis = new DigestInputStream(is, md5)) {
				zos.putNextEntry(md5Entry);
				IOUtils.skip(dis, Long.MAX_VALUE);
				zos.write(Hex.encodeHexString(md5.digest()).getBytes());
				zos.closeEntry();
			}

			// Copy entry
			try (InputStream is = Files.newInputStream(file)) {
				zos.putNextEntry(entry);
				IOUtils.copy(is, zos);
				zos.closeEntry();
			}
		}
	}

	/**
	 * Writes a single zip entry
	 *
	 * @param filePath The file path
	 * @param object   The object to serialize
	 * @param zos      The zip outputStream
	 * @throws IOException if writing the zip entry goes wrong
	 */
	private void serializeAsZipEntry(String filePath, Object object, ZipOutputStream zos) throws IOException {
		zos.putNextEntry(new ZipEntry(filePath));
		objectWriter.writeValue(zos, object);
		zos.closeEntry();
	}

	/**
	 * Handle finding a lib file's MD5 hash. See if the corresponding {@link File File's} ({@link InputStream}) was
	 * found or add the MD5 to the list of MD5s.
	 *
	 * @param ci                {@link ConnectionInformation} to add the lib file to
	 * @param md5hashForLibFile map of known filenames and their MD5 hashes
	 * @param libFileStreams    map of filenames to the {@link InputStream}
	 * @param md5Hash           md5Hash to check, either the corresponding file was found before then it can be added,
	 *                          else we store the MD5
	 * @param filename          for which the MD5 is
	 * @throws IOException if adding the lib md5 file goes wrong
	 */
	private static void handleLibMD5(ConnectionInformationImpl ci, Map<String, String> md5hashForLibFile, Map<String, InputStream> libFileStreams, String md5Hash, String filename) throws IOException {
		if (libFileStreams.containsKey(filename)) {
			ci.addLibFile(filename, libFileStreams.get(filename), md5Hash);
			libFileStreams.remove(filename);
		} else {
			md5hashForLibFile.put(filename, md5Hash);
		}
	}

	/**
	 * Handle 'other' file's MD5 hash. See if the corresponding {@link File File's} ({@link InputStream}) was found or
	 * add the MD5 to the list of MD5s.
	 *
	 * @param ci                  {@link ConnectionInformation} to add the lib file to
	 * @param md5hashForOtherFile map of known filenames and their MD5 hashes
	 * @param otherFileStreams    map of filenames to the {@link InputStream}
	 * @param md5Hash             md5Hash to check, either the corresponding file was found before then it can be added,
	 *                            else we store the MD5
	 * @param filename            for which the MD5 is
	 * @throws IOException if adding the other md5 file goes wrong
	 */
	private static void handleOtherMD5(ConnectionInformationImpl ci, Map<String, String> md5hashForOtherFile, Map<String, InputStream> otherFileStreams, String md5Hash, String filename) throws IOException {
		if (otherFileStreams.containsKey(filename)) {
			ci.addOtherFile(filename, otherFileStreams.get(filename), md5Hash);
			otherFileStreams.remove(filename);
		} else {
			md5hashForOtherFile.put(filename, md5Hash);
		}
	}

	/**
	 * Handle the lib file, add it to the {@link ConnectionInformation} if the MD5 was found or keep it to add when the
	 * MD5 was found
	 *
	 * @param ci                {@link ConnectionInformation} to add the lib file to
	 * @param md5hashForLibFile map of known filenames and their MD5 hashes
	 * @param libFileStreams    map of filenames to the {@link InputStream}
	 * @param inputStream       the {@link InputStream} of he lib file
	 * @param filename          for which the MD5 is
	 * @throws IOException if adding the lib file goes wrong
	 */
	private static void handleLibFile(ConnectionInformationImpl ci, Map<String, String> md5hashForLibFile, Map<String, InputStream> libFileStreams, InputStream inputStream, String filename) throws IOException {
		if (md5hashForLibFile.containsKey(filename)) {
			ci.addLibFile(filename, inputStream, md5hashForLibFile.get(filename));
			md5hashForLibFile.remove(filename);
		} else {
			libFileStreams.put(filename, copyInputStream(inputStream));
		}
	}

	/**
	 * Handle the file, add it to the {@link ConnectionInformation} if the MD5 was found or keep it to add when the MD5
	 * was found
	 *
	 * @param ci                  {@link ConnectionInformation} to add the file to
	 * @param md5hashForOtherFile map of known filenames and their MD5 hashes
	 * @param otherFileStreams    map of filenames to the {@link InputStream}
	 * @param inputStream         the {@link InputStream} of he lib file
	 * @param filename            for which the MD5 is
	 * @throws IOException if adding the other file goes wrong
	 */
	private static void handleOtherFile(ConnectionInformationImpl ci, Map<String, String> md5hashForOtherFile, Map<String, InputStream> otherFileStreams, InputStream inputStream, String filename) throws IOException {
		if (md5hashForOtherFile.containsKey(filename)) {
			ci.addOtherFile(filename, inputStream, md5hashForOtherFile.get(filename));
			md5hashForOtherFile.remove(filename);
		} else {
			otherFileStreams.put(filename, copyInputStream(inputStream));
		}
	}

	/**
	 * Creates an ObjectMapper that doesn't close output streams
	 *
	 * @return a preconfigured ObjectMapper
	 */
	private static ObjectMapper createObjectMapper() {
		JsonFactory jsonFactory = new JsonFactory();
		// Don't close underlying streams
		jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		ObjectMapper mapper = new ObjectMapper(jsonFactory);
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		return mapper;
	}

	/**
	 * Copies an InputStream into memory
	 *
	 * @param inputStream
	 * 		the input stream to copy
	 * @return the byte array based input stream copy
	 * @throws NullPointerException
	 * 		if the inputStream is {@code null}
	 * @throws IOException
	 * 		if an I/O error occurs
	 */
	private static InputStream copyInputStream(InputStream inputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copyLarge(inputStream, baos);
		return new ByteArrayInputStream(baos.toByteArray());
	}

}
