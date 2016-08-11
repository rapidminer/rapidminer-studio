/**
 * Copyright (C) 2001-2016 RapidMiner GmbH
 */
package com.rapidminer.security;

import java.awt.AWTPermission;
import java.io.FilePermission;
import java.net.SocketPermission;
import java.net.URLPermission;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.PropertyPermission;
import java.util.logging.Level;
import java.util.logging.LoggingPermission;

import javax.sound.sampled.AudioPermission;
import javax.xml.bind.DatatypeConverter;

import com.rapidminer.RapidMiner;
import com.rapidminer.operator.ScriptingOperator;
import com.rapidminer.security.internal.InternalPluginClassLoader;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.tools.plugin.PluginClassLoader;


/**
 * This class is responsible for restricting access to certain capabilities of Java for
 * {@link Plugin}s. Only extensions that are signed by us and would pass the JarVerifier check are
 * granted trusted access, i.e. they will get the same permissions as our core code. Untrusted
 * extensions will get a limited set of permissions.
 *
 * @author Marco Boeck
 * @since 7.2
 */
public final class PluginSandboxPolicy extends Policy {

	/** The key pair algorithm for our signed extensions */
	private static final String KEY_ALGORITHM = "RSA";

	/** The Base64 encoded public key which is used to verify our signed extensions */
	private static final String KEY_B64_ENCODED = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvyoyYgZ0jYHlPOh2mGvvvXl6FS4X"
			+ "t3FaCsnn1IglbbDYM9eXcWgeD6I/4mM3t6XsAsyzSDLRxagCM869lYknxjff0xMdA5aekqPe0vx4yqR9QK369u3lbGMaNvylwhg5vCTWn2vZ"
			+ "anxWScOfVW6yDxEjgEHJvMiMzZkGNklYC3ULBCkHfIrih5hO83k5FileuUWDNO4BrLrawmjo9AmYksPVOMmd4/DtDpnehpLy0hQtjBJsz61h"
			+ "AGVDnPGpvbsW0rjFAjE4fR5+4RwUNo+SsD/44Jc8bui5seVH5vZuTj02XokybGR4BikrqvJZ4rHe4OGowl8uIr9sEN/+0eIJXQIDAQAB";

	/** Our public key used to verify the certificates */
	private static PublicKey key;

	static {
		try {
			KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
			X509EncodedKeySpec spec = new X509EncodedKeySpec(DatatypeConverter.parseBase64Binary(KEY_B64_ENCODED));
			key = factory.generatePublic(spec);
		} catch (GeneralSecurityException e) {
			key = null;
			// no log service available yet, so use syserr
			System.err.println(
					"Failed to initialize public key to verify extension certificates. Revoking permissions for all extensions!");
			e.printStackTrace();
		}
	}

	@Override
	public PermissionCollection getPermissions(ProtectionDomain domain) {
		if (isInternalPlugin(domain)) {
			// used e.g. by Radoop for external library loading
			return createAllPermissions();
		} else if (isUnsignedPlugin(domain)) {
			return createUnsignedPermissions((PluginClassLoader) domain.getClassLoader());
		} else if (isUnknown(domain)) {
			return createUnknownSourcePermissions();
		} else if (isGroovyScript(domain)) {
			return createGroovySourcePermissions();
		} else {
			return createAllPermissions();
		}
	}

	/**
	 * Checks whether the given domain belongs to a special internal extension or not.
	 *
	 * @param domain
	 *            the domain in question, never {@code null}
	 * @return {@code true} if the domain belongs to a special internal extension; {@code false}
	 *         otherwise
	 *
	 */
	private static boolean isInternalPlugin(ProtectionDomain domain) {
		// everything not loaded by the internal plugin classloader is not a special internal plugin
		return domain.getClassLoader() instanceof InternalPluginClassLoader;
	}

	/**
	 * Checks whether the given domain belongs to an unsigned extension or not.
	 *
	 * @param domain
	 *            the domain in question, never {@code null}
	 * @return {@code true} if the domain belongs to an unsigned extension; {@code false} otherwise
	 *
	 */
	private static boolean isUnsignedPlugin(ProtectionDomain domain) {
		// everything not loaded by the plugin classloader is no plugin
		if (!(domain.getClassLoader() instanceof PluginClassLoader)) {
			return false;
		}

		// if the public key could not be initialized, we treat all plugins as unsafe
		if (key == null) {
			return true;
		}

		// some sanity checks
		if (domain.getCodeSource() == null) {
			return true;
		}
		// special case for SNAPSHOT version: grant all permissions for all extensions
		if (RapidMiner.getVersion().isSnapshot()) {
			return false;
		}
		if (domain.getCodeSource().getCertificates() == null) {
			// if no certificate: unsigned permissions only
			return true;
		}

		try {
			verifyCertificates(domain.getCodeSource().getCertificates());
			// signed by us, we are good at this point as we found a valid certificate
			return false;
		} catch (GeneralSecurityException e) {
			// invalid certificate
			LogService.getRoot().log(Level.WARNING, "Invalid certificate for " + domain.getCodeSource().getLocation());
			return true;
		} catch (Exception e) {
			// some other error during certificate verification
			LogService.getRoot().log(Level.WARNING,
					"Error verifying certificate for " + domain.getCodeSource().getLocation(), e);
			return true;
		}
	}

	/**
	 * Checks whether the given domain is the one we create for the {@link ScriptingOperator}. If
	 * so, restrict what it can do.
	 *
	 * @param domain
	 *            the domain in question, must not be {@code null}
	 * @return {@code true} if the domain is a groovy script; {@code false} otherwise
	 *
	 */
	private static boolean isGroovyScript(ProtectionDomain domain) {
		if (domain.getCodeSource().getLocation() != null
				&& domain.getCodeSource().getLocation().getPath().contains(ScriptingOperator.GROOVY_DOMAIN)) {
			return true;
		}
		return false;
	}

	/**
	 * Checks whether the given domain is unknown. In that case, restrict everything!
	 *
	 * @param domain
	 *            the domain in question, may be {@code null}
	 * @return {@code true} if the domain is unknown; {@code false} otherwise
	 *
	 */
	private static boolean isUnknown(ProtectionDomain domain) {
		if (domain == null || domain.getCodeSource() == null) {
			return true;
		}
		return false;
	}

	/**
	 * Create permission for unsigned extensions.
	 *
	 * @param loader
	 *            the plugin class loader of the unsigned extensions
	 * @return the permissions, never {@code null}
	 */
	private static PermissionCollection createUnsignedPermissions(final PluginClassLoader loader) {
		final Permissions permissions = new Permissions();

		permissions.add(new RuntimePermission("shutdownHooks"));

		permissions.add(new PropertyPermission("*", "read"));

		AccessController.doPrivileged(new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				String userHome = System.getProperty("user.home");
				String tempDir = System.getProperty("java.io.tempdir");
				String pluginKey = loader.getPluginKey();

				// delete access to the general temp directory
				permissions.add(new FilePermission(tempDir + "/-", "read, write, delete"));

				// extensions can only delete files in their own subfolder of the
				// .RapidMiner/extensions/workspace folder
				if (pluginKey != null) {
					String pluginFolder = pluginKey;

					permissions.add(new FilePermission(userHome + "/.RapidMiner/extensions", "read"));
					permissions.add(new FilePermission(userHome + "/.RapidMiner/extensions/workspace", "read"));
					permissions.add(new FilePermission(userHome + "/.RapidMiner/extensions/workspace/" + pluginFolder,
							"read, write"));
					permissions.add(new FilePermission(userHome + "/.RapidMiner/extensions/workspace/" + pluginFolder + "/-",
							"read, write, delete"));
				}

				// unfortunately currently we have to give all location permissons to read/write
				// files to not block extensions that add "Read/Write xyz" operators
				permissions.add(new FilePermission("<<ALL FILES>>", "read, write"));
				return null;
			}
		});

		addCommonPermissions(permissions);

		return permissions;
	}

	/**
	 * Create permission for unknown sources.
	 *
	 * @return the permissions, never {@code null}
	 */
	private static PermissionCollection createUnknownSourcePermissions() {
		Permissions permissions = new Permissions();
		// empty permissions, not allowed to do anything
		return permissions;
	}

	/**
	 * Create permission for groovy scripts of the {@link ScriptingOperator}.
	 *
	 * @return the permissions, never {@code null}
	 */
	private static PermissionCollection createGroovySourcePermissions() {
		Permissions permissions = new Permissions();

		// grant some permissions because the script is something the user himself created
		permissions.add(new PropertyPermission("*", "read, write"));
		permissions.add(new FilePermission("<<ALL FILES>>", "read, write, delete"));

		addCommonPermissions(permissions);

		return permissions;
	}

	/**
	 * Create permission for our trusted code. No restrictions are applied
	 *
	 * @return the permissions, never {@code null}
	 */
	private static PermissionCollection createAllPermissions() {
		Permissions permissions = new Permissions();
		permissions.add(new AllPermission());
		return permissions;
	}

	/**
	 * Adds a couple of common permissions for both unsigned extensions as well as Groovy scripts.
	 *
	 * @param permissions
	 *            the permissions object which will get the permissions added to it
	 */
	private static void addCommonPermissions(Permissions permissions) {
		permissions.add(new AudioPermission("play"));
		permissions.add(new AWTPermission("listenToAllAWTEvents"));
		permissions.add(new AWTPermission("setWindowAlwaysOnTop"));
		permissions.add(new AWTPermission("watchMousePointer"));
		permissions.add(new LoggingPermission("control", ""));
		permissions.add(new SocketPermission("*", "connect, listen, accept, resolve"));
		permissions.add(new URLPermission("http://-", "*:*"));
		permissions.add(new URLPermission("https://-", "*:*"));

		// because random Java library calls use sun classes which may or may not do an acess check,
		// we have to grant access to all of them
		// this is a very unfortunate permission and I would love to not have it
		// so if at any point in the future this won't be necessary any longer, remove it!!!
		permissions.add(new RuntimePermission("accessClassInPackage.sun.*"));

		permissions.add(new RuntimePermission("accessDeclaredMembers"));
		permissions.add(new RuntimePermission("getenv.*"));
		permissions.add(new RuntimePermission("getFileSystemAttributes"));
		permissions.add(new RuntimePermission("readFileDescriptor"));
		permissions.add(new RuntimePermission("writeFileDescriptor"));
		permissions.add(new RuntimePermission("queuePrintJob"));
	}

	/**
	 * Verify the given certificates and see if at least one was signed by us.
	 *
	 * @param certificates
	 *            the array of certificates to check
	 * @throws GeneralSecurityException
	 *             if no certificate could be verified, will throw the last exception that occured
	 *             during verification of all certificates. Can be {@code null}
	 */
	private static void verifyCertificates(Certificate[] certificates) throws GeneralSecurityException {
		GeneralSecurityException lastException = null;
		boolean verified = false;
		for (Certificate certificate : certificates) {
			try {
				certificate.verify(key);
				verified = true;
				break;
			} catch (GeneralSecurityException e) {
				lastException = e;
			}
		}
		if (!verified) {
			throw lastException;
		}
	}
}
