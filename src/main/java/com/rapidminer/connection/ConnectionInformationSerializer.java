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
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
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
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.connection.valueprovider.ValueProviderParameterImpl;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.NonClosingZipInputStream;


/**
 * ConnectionInformationSerializer used for reading and writing of {@link ConnectionInformation} objects
 *
 * @author Jan Czogalla, Andreas Timm, Jonas Wilms-Pfau
 * @since 9.3
 */
public final class ConnectionInformationSerializer {

	/**
	 * Local serializer, protected by local encryption key
	 */
	public static final ConnectionInformationSerializer LOCAL = new ConnectionInformationSerializer(getObjectMapper());

	/**
	 * Server Serializer, protected by transport security
	 */
	public static final ConnectionInformationSerializer REMOTE = new ConnectionInformationSerializer(getRemoteObjectMapper());

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
	 * ObjectWriter, kept a cache version for faster access
	 */
	private final ObjectWriter objectWriter;

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
		this.objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
		ObjectReader reader = objectMapper.reader();
		this.connectionStatisticsReader = reader.withType(ConnectionStatistics.class);
		this.connectionConfigurationReader = reader.withType(ConnectionConfiguration.class);
	}

	/**
	 * Load a {@link ConnectionConfiguration} from the given {@link Reader}
	 *
	 * @param src
	 * 		the source that contains the {@link ConnectionConfiguration}
	 * @return a loaded {@link ConnectionConfiguration}, can be null if the src was null
	 * @throws IOException
	 * 		in case the {@link Reader} fails
	 */
	public ConnectionConfiguration loadConfiguration(Reader src) throws IOException {
		if (src == null) {
			return null;
		}
		return connectionConfigurationReader.readValue(src);
	}

	/**
	 * Load a {@link ConnectionConfiguration} from the inputStream.
	 *
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public ConnectionConfiguration loadConfiguration(InputStream inputStream) throws IOException {
		if (inputStream == null) {
			return null;
		}
		return connectionConfigurationReader.readValue(inputStream);
	}

	/**
	 * Load a {@link ConnectionInformation} from the given {@link InputStream}
	 *
	 * @param stream
	 * 		the {@link InputStream} to read the {@link ConnectionInformation} content from. This should be the data that
	 * 		was produced by {@link ConnectionInformationSerializer#serialize(ConnectionInformation, OutputStream)}
	 * @return the {@link ConnectionInformation} the stream contained
	 * @throws IOException
	 * 		in case of reading errors
	 */
	public ConnectionInformation loadConnection(InputStream stream) throws IOException {
		return loadConnection(stream, null);
	}

	/**
	 * Load a {@link ConnectionInformation} from the given {@link InputStream} and {@link RepositoryLocation}.
	 * The {@link ConnectionConfiguration} will get the repository location's name as its name.
	 *
	 * @param stream
	 * 		the {@link InputStream} to read the {@link ConnectionInformation} content from. This should be the data that
	 * 		was produced by {@link ConnectionInformationSerializer#serialize(ConnectionInformation, OutputStream)}
	 * @param repositoryLocation
	 * 		the repository location this entry belongs too; might be {@code null}
	 * @return the {@link ConnectionInformation} the stream contained
	 * @throws IOException
	 * 		in case of reading errors
	 */
	public ConnectionInformation loadConnection(InputStream stream, RepositoryLocation repositoryLocation) throws IOException {
		ConnectionInformationImpl ci = new ConnectionInformationImpl();
		NonClosingZipInputStream zis = new NonClosingZipInputStream(stream);
		// track found files, need the file's inputstream and the md5 to add it
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
				ConnectionConfiguration configuration = loadConfiguration(zis);
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
	 * and Other files.
	 *
	 * @param connectionInformation
	 * 		the {@link ConnectionInformation} to be stored
	 * @param out the outputStream to write to
	 * @throws IOException
	 * 		in case creating the result was not possible
	 */
	public void serialize(ConnectionInformation connectionInformation, OutputStream out) throws IOException {
		if (connectionInformation == null) {
			throw new IOException("Object connection information is null");
		}

		if (out == null) {
			throw new IOException("The output stream is null");
		}

		ZipOutputStream zos = new ZipOutputStream(out);
		zos.setLevel(ZipOutputStream.STORED);
		serializeAsZipEntry(ENTRY_NAME_CONFIG, connectionInformation.getConfiguration(), zos);

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

	/**
	 * Helper methods to create the serialized {@link ConnectionInformation}. Write an {@link Object} as JSON format to
	 * the {@link OutputStream}.
	 *
	 * @param out
	 * 		{@link OutputStream} to write to
	 * @param object
	 * 		to be written
	 * @throws IOException
	 * 		if writing failed
	 */
	public void writeJson(OutputStream out, Object object) throws IOException {
		if (out != null && object != null) {
			objectWriter.writeValue(out, object);
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
	 * @param filesToCopy the files to write
	 * @param zos the ZipOutputStream to write to
	 * @throws IOException
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
	 * @param object The object to serialize
	 * @param zos The zip outputStream
	 * @throws IOException
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
	 * @param ci
	 * 		{@link ConnectionInformation} to add the lib file to
	 * @param md5hashForLibFile
	 * 		map of known filenames and their MD5 hashes
	 * @param libFileStreams
	 * 		map of filenames to the {@link InputStream}
	 * @param md5Hash
	 * 		md5Hash to check, either the corresponding file was found before then it can be added, else we store the MD5
	 * @param filename
	 * 		for which the MD5 is
	 * @throws IOException
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
	 * @param ci
	 * 		{@link ConnectionInformation} to add the lib file to
	 * @param md5hashForOtherFile
	 * 		map of known filenames and their MD5 hashes
	 * @param otherFileStreams
	 * 		map of filenames to the {@link InputStream}
	 * @param md5Hash
	 * 		md5Hash to check, either the corresponding file was found before then it can be added, else we store the MD5
	 * @param filename
	 * 		for which the MD5 is
	 * @throws IOException
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
	 * @param ci
	 * 		{@link ConnectionInformation} to add the lib file to
	 * @param md5hashForLibFile
	 * 		map of known filenames and their MD5 hashes
	 * @param libFileStreams
	 * 		map of filenames to the {@link InputStream}
	 * @param inputStream
	 * 		the {@link InputStream} of he lib file
	 * @param filename
	 * 		for which the MD5 is
	 * @throws IOException
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
	 * @param ci
	 * 		{@link ConnectionInformation} to add the file to
	 * @param md5hashForOtherFile
	 * 		map of known filenames and their MD5 hashes
	 * @param otherFileStreams
	 * 		map of filenames to the {@link InputStream}
	 * @param inputStream
	 * 		the {@link InputStream} of he lib file
	 * @param filename
	 * 		for which the MD5 is
	 * @throws IOException
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
	 * Creates a Server Object Mapper
	 *
	 * @return an object mapper that does not encrypt values
	 */
	public static ObjectMapper getRemoteObjectMapper() {
		ObjectMapper objectMapper = getObjectMapper();
		objectMapper.addMixIn(ValueProviderParameterImpl.class, ValueProviderParameterImpl.UnencryptedValueMixIn.class);
		return objectMapper;
	}

	/**
	 * Creates an ObjectMapper that doesn't close output streams
	 *
	 * @return a preconfigured ObjectMapper
	 */
	private static ObjectMapper getObjectMapper(){
		JsonFactory jsonFactory = new JsonFactory();
		// Don't close underlying streams
		jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		return new ObjectMapper(jsonFactory);
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
