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
package com.rapidminer.tools.abtesting;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.xml.bind.DatatypeConverter;

import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.tools.LogService;


/**
 * Returns a installation specific identifier
 * based on either the email address, the user name or, a hardware address
 *
 * @author Jonas Wilms-Pfau
 * @since 8.2
 */
final class IdentifierProvider {

	private IdentifierProvider(){
		throw new AssertionError("Utility class");
	}

	/**
	 * Returns an identifier based on the first available from the following:
	 *
	 * <ul>
	 *     <li>Email Address</li>
	 *     <li>Username and OS Version</li>
	 *     <li>MAC Address</li>
	 *     <li>Empty String</li>
	 * </ul>
	 *
	 * @return an identifier
	 */
	public static long getIdentifier(){
		return ("" + getIdentifierString()).hashCode();
	}

	/**
	 * Returns a seed based on the first available from the following:
	 *
	 * <ul>
	 *     <li>Email Address</li>
	 *     <li>Username and OS Version</li>
	 *     <li>MAC Address</li>
	 *     <li>Empty string</li>
	 * </ul>
	 *
	 * @return a installation specific string
	 */
	private static String getIdentifierString() {
		try {
			return getEmail();
		} catch (Exception e) {
			LogService.getRoot().log(Level.FINEST, "com.rapidminer.tools.abtesting.IdentifierProvider.email_retrieval_failed", e);
		}

		try {
			return getUserAndOSVersion();
		} catch (Exception e) {
			LogService.getRoot().log(Level.FINEST, "com.rapidminer.tools.abtesting.IdentifierProvider.system_properties_retrieval_failed", e);
		}

		try {
			return getHardwareAddress();
		} catch (Exception e) {
			LogService.getRoot().log(Level.FINEST, "com.rapidminer.tools.abtesting.IdentifierProvider.mac_address_retrieval_failed", e);
		}

		return "";
	}

	/**
	 * Tries to receive the email address of the current license user
	 *
	 * @return the users email address
	 * @throws NullPointerException if RapidMiner is not initialized
	 * @throws InvalidResultException if no license email was retrieved
	 */
	static String getEmail() {
		String email =  ProductConstraintManager.INSTANCE.getActiveLicense().getLicenseUser().getEmail();
		if (email == null || email.trim().isEmpty()) {
			throw new InvalidResultException("license user email must not be null or empty");
		}
		return email.toLowerCase(Locale.ENGLISH);
	}

	/**
	 * Tries to get the mac address of the first device in alphabetic order, should be eth or en in most cases.
	 * <p>
	 * Warning: this method can take seconds to execute
	 *
	 * @return the first found hardware address
	 * @throws SocketException
	 * 		if an I/O error occurs.
	 * @throws InvalidResultException
	 * 		if no non-loopback network device exists.
	 * @throws NullPointerException
	 * 		if no network device exists.
	 */
	static String getHardwareAddress() throws SocketException {
		//Use hardware addresses as seed
		Stream<NetworkInterface> sortedStream = Collections.list(NetworkInterface.getNetworkInterfaces()).stream().filter(i -> {
			try {
				return i.getHardwareAddress() != null;
			} catch (SocketException e) {
				return false;
			}
		}).sorted(Comparator.comparing(NetworkInterface::getName));
		Optional<NetworkInterface> result = sortedStream.findFirst();
		if (!result.isPresent()) {
			throw new InvalidResultException("No non-loopback network interface exists");
		}
		return DatatypeConverter.printHexBinary(result.get().getHardwareAddress());
	}

	/**
	 * Returns the user.name and os.version {@link System#getProperty System properties} concatenated
	 *
	 * @return the user name and os version
	 * @throws  SecurityException  if a security manager exists and its
	 *             <code>checkPropertyAccess</code> method doesn't allow
	 *             access to user.name or os.version
	 * @throws InvalidResultException if user name is null or empty
	 */
	static String getUserAndOSVersion() {
		String user = System.getProperty("user.name");
		String osVersion = System.getProperty("os.version");
		if (user == null || user.trim().isEmpty()) {
			throw new InvalidResultException("user.name must not be null or empty");
		}
		return user + osVersion;
	}

	/**
	 * Exception thrown for invalid results
	 *
	 * @author Jonas Wilms-Pfau
	 * @since 8.2
	 */
	static class InvalidResultException extends RuntimeException {

		/**
		 * Constructs a new InvalidResultException with the given message
		 *
		 * @param message
		 * 		the detail message
		 * @see RuntimeException#RuntimeException(String)
		 */
		InvalidResultException(String message) {
			super(message);
		}
	}

}
