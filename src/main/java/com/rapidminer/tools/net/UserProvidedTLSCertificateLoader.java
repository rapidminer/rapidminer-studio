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
package com.rapidminer.tools.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.PlatformUtilities;


/**
 * Loads additional X.509 certificates from .RapidMiner/cacerts
 * <p>
 * The certificates are loaded from both the jre cacerts file as well as from the .RapidMiner/cacerts folder. Loading is
 * only done once on startup.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.1
 */
public enum UserProvidedTLSCertificateLoader {
	INSTANCE;

	private static final String CERTS_FOLDER = "cacerts";
	private static final String X509 = "X.509";
	private static final String USER_CERT_PREFIX = "user_cert_";
	private static final String JRE_CERT_PREFIX = "jre_cert_";
	private static final char SEPARATOR = '_';
	private static final String TMP_PREFIX = "cacerts-rm";
	private static final String TMP_SUFFIX = "jks";

	/**
	 * We can only access the LogService if
	 * <ol>
	 * <li>PlatformUtilities are initialized</li>
	 * <li>We have Filesystem access</li>
	 * </ol>
	 */
	private final Logger logger;

	UserProvidedTLSCertificateLoader() {
		if (RapidMiner.getExecutionMode().canAccessFilesystem()) {
			PlatformUtilities.initialize();
			logger = LogService.getRoot();
		} else {
			logger = Logger.getLogger(UserProvidedTLSCertificateLoader.class.getName());
		}
		reloadCertificates();
	}

	/**
	 * Initializes the TrustManager
	 *
	 * @see #reloadCertificates()
	 */
	public void init() {
		// init happens in the constructor
	}

	/**
	 * Reloads the certificates from the .RapidMiner/cacerts folder and merges them with the provided jvm certificates
	 */
	private void reloadCertificates() {
		try {
			Path certsPath = FileSystemService.getUserRapidMinerDir().toPath().resolve(CERTS_FOLDER);
			Map<String, Certificate> userCertificates = readCertificates(certsPath);
			if (userCertificates.isEmpty()) {
				// No need to do anything here
				return;
			}
			TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			// init with default keystore (or "javax.net.ssl.trustStore" if configured)
			factory.init((KeyStore) null);
			// Extract the default certificates
			userCertificates.putAll(extractCertificates(factory.getTrustManagers()));
			// Create new keystore, containing all certificates
			KeyStore keystore = createKeyStore(userCertificates);
			String password = UUID.randomUUID().toString();
			Path keystoreFile = Files.createTempFile(TMP_PREFIX, TMP_SUFFIX);
			keystoreFile.toFile().deleteOnExit();
			try (OutputStream out = Files.newOutputStream(keystoreFile)) {
				keystore.store(out, password.toCharArray());
			}
			// Set the new trustStore and trustStorePassword
			System.setProperty("javax.net.ssl.trustStore", keystoreFile.toAbsolutePath().toString());
			System.setProperty("javax.net.ssl.trustStorePassword", password);
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Loading of user provided certificates failed.", e);
		}
	}

	/**
	 * Creates a new java keystore, containing the given certificates
	 *
	 * @param certificates
	 * 		the certificates that the keystore should contain
	 * @return a new in-memory {@link KeyStore} containing the certificates
	 * @throws KeyStoreException
	 * 		if no Provider supports a KeyStoreSpi implementation for the default type, if the keystore has not been
	 * 		initialized, or the given alias already exists and does not identify an entry containing a trusted certificate,
	 * 		or this operation fails for some other reason.
	 * @throws CertificateException
	 * 		if any of the certificates in the keystore could not be loaded
	 * @throws NoSuchAlgorithmException
	 * 		if the algorithm used to check the integrity of the keystore cannot be found
	 * @throws IOException
	 * 		if there is an I/O or format problem with the keystore data. If the error is due to an incorrect
	 * 		ProtectionParameter (e.g. wrong password) the cause of the IOException should be an UnrecoverableKeyException
	 */
	private KeyStore createKeyStore(Map<String, Certificate> certificates) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		keystore.load(null);
		for (Map.Entry<String, Certificate> certificate : certificates.entrySet()) {
			keystore.setCertificateEntry(certificate.getKey(), certificate.getValue());
		}
		return keystore;
	}

	/**
	 * Extracts the X509Certificate from the given TrustManagers
	 *
	 * @param trustManagers
	 * 		trust managers to extract the certificates from
	 * @return Map containing the certificates
	 */
	private Map<String, X509Certificate> extractCertificates(TrustManager... trustManagers) {
		Map<String, X509Certificate> certificates = new HashMap<>();
		int certCount = 0;
		for (TrustManager manager : trustManagers) {
			if (manager instanceof X509TrustManager) {
				for (X509Certificate x509Certificate : ((X509TrustManager) manager).getAcceptedIssuers()) {
					String name = JRE_CERT_PREFIX + certCount++;
					certificates.put(name, x509Certificate);
				}
			}
		}
		return certificates;
	}

	/**
	 * Reads the certificates from the given path
	 *
	 * @param certFolder
	 * 		the folder containing the certificates
	 * @return a map containing all names and certificates
	 */
	private Map<String, Certificate> readCertificates(Path certFolder) throws IOException, CertificateException {
		if (!certFolder.toFile().exists()) {
			return Collections.emptyMap();
		}

		Map<String, Certificate> result = new ConcurrentHashMap<>();
		CertificateFactory factory = CertificateFactory.getInstance(X509);
		Predicate<Path> isDirectory = Files::isDirectory;
		try (Stream<Path> files = Files.walk(certFolder, FileVisitOption.FOLLOW_LINKS)) {
			AtomicInteger globalCount = new AtomicInteger(0);
			files.filter(isDirectory.negate()).forEach(path -> {
				String filename = path.getFileName().toString();
				// Filenames might occur multiple times in the file tree
				int fileNumber = globalCount.getAndIncrement();
				try (InputStream fileInputStream = Files.newInputStream(path)) {
					Collection<? extends Certificate> certificates = factory.generateCertificates(fileInputStream);
					int certCount = 0;
					for (Certificate certificate : certificates) {
						String name = USER_CERT_PREFIX + fileNumber + SEPARATOR + certCount++ + SEPARATOR + filename;
						result.put(name, certificate);
					}
					logger.info("Loaded " + certCount + " certificate" + (certCount != 1 ? "s" : "") + " from " + filename);
				} catch (IOException | CertificateException e) {
					logger.log(Level.WARNING, "Could not load certificates from " + filename, e);
				}
			});
		}

		return result;
	}

}
