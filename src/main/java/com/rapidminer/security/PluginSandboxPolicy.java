/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.security;

import java.awt.AWTPermission;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.net.URLPermission;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
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
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.license.StudioLicenseConstants;
import com.rapidminer.operator.ScriptingOperator;
import com.rapidminer.security.internal.InternalPluginClassLoader;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
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

	/** Internal permission for {@link RuntimePermission}s */
	public static final String RAPIDMINER_INTERNAL_PERMISSION = "accessClassInPackage.rapidminer.internal";

	/** The key pair algorithm for our signed extensions */
	private static final String KEY_ALGORITHM = "RSA";

	/** The Base64 encoded public key which is used to verify our signed extensions */
	private static final String KEY_B64_ENCODED = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvyoyYgZ0jYHlPOh2mGvvvXl6FS4X"
			+ "t3FaCsnn1IglbbDYM9eXcWgeD6I/4mM3t6XsAsyzSDLRxagCM869lYknxjff0xMdA5aekqPe0vx4yqR9QK369u3lbGMaNvylwhg5vCTWn2vZ"
			+ "anxWScOfVW6yDxEjgEHJvMiMzZkGNklYC3ULBCkHfIrih5hO83k5FileuUWDNO4BrLrawmjo9AmYksPVOMmd4/DtDpnehpLy0hQtjBJsz61h"
			+ "AGVDnPGpvbsW0rjFAjE4fR5+4RwUNo+SsD/44Jc8bui5seVH5vZuTj02XokybGR4BikrqvJZ4rHe4OGowl8uIr9sEN/+0eIJXQIDAQAB";

	/**
	 * the system property which can be set to {@code true} to enforce plugin sandboxing even on
	 * SNAPSHOT versions
	 */
	private static final String PROPERTY_SECURITY_ENFORCED = "com.rapidminer.security.enforce";

	/** Our public key used to verify the certificates */
	private static PublicKey key;

	/**
	 * if {@code true}, plugin sandboxing is enforced even on SNAPSHOT versions
	 */
	private static volatile Boolean enforced;

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

	@Override
	public PermissionCollection getPermissions(CodeSource codesource) {
		// This is a workaround for the following bug
		// https://bugs.openjdk.java.net/browse/JDK-8014008
		// return modifiable empty permissions, to avoid manipulation of read only permissions
		for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
			if ("sun.rmi.server.LoaderHandler".equals(element.getClassName())
					&& ("loadClass".equals(element.getMethodName()) || "loadProxyClass".equals(element.getMethodName()))) {
				return new Permissions();
			}
		}
		// return unmodifiable Policy.UNSUPPORTED_EMPTY_COLLECTION
		return super.getPermissions(codesource);
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
		// unless security is enforced via system property
		if (RapidMiner.getVersion().isSnapshot() && !isSecurityEnforced()) {
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

		if (ProductConstraintManager.INSTANCE.isInitialized()) {
			boolean isAllowed = ProductConstraintManager.INSTANCE.getActiveLicense()
					.getPrecedence() >= StudioLicenseConstants.UNLIMITED_LICENSE_PRECEDENCE
					|| ProductConstraintManager.INSTANCE.isTrialLicense();
			boolean isEnabled = Boolean.parseBoolean(
					ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_ADDITIONAL_PERMISSIONS));
			if (isAllowed && isEnabled) {
				permissions.add(new ReflectPermission("suppressAccessChecks"));
				permissions.add(new ReflectPermission("newProxyInPackage.*"));
				permissions.add(new AWTPermission("accessClipboard"));
				permissions.add(new RuntimePermission("createClassLoader"));
				permissions.add(new RuntimePermission("getClassLoader"));
				permissions.add(new RuntimePermission("setContextClassLoader"));
				permissions.add(new RuntimePermission("enableContextClassLoaderOverride"));
				permissions.add(new RuntimePermission("closeClassLoader"));
				permissions.add(new RuntimePermission("modifyThread"));
				permissions.add(new RuntimePermission("stopThread"));
				permissions.add(new RuntimePermission("modifyThreadGroup"));
				permissions.add(new RuntimePermission("loadLibrary.*"));
				permissions.add(new RuntimePermission("getStackTrace"));
				permissions.add(new RuntimePermission("setDefaultUncaughtExceptionHandler"));
				permissions.add(new RuntimePermission("preferences"));
				permissions.add(new RuntimePermission("setFactory"));
				permissions.add(new PropertyPermission("*", "write"));
			}
		}

		permissions.add(new RuntimePermission("shutdownHooks"));

		permissions.add(new PropertyPermission("*", "read"));

		AccessController.doPrivileged(new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				String userHome = System.getProperty("user.home");
				String tmpDir = System.getProperty("java.io.tmpdir");
				String pluginKey = loader.getPluginKey();

				// delete access to the general temp directory
				permissions.add(new FilePermission(tmpDir, "read, write"));
				permissions.add(new FilePermission(tmpDir + "/-", "read, write, delete"));

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
		if (ProductConstraintManager.INSTANCE.isInitialized()) {
			if (ProductConstraintManager.INSTANCE.getActiveLicense()
					.getPrecedence() >= StudioLicenseConstants.UNLIMITED_LICENSE_PRECEDENCE
					|| ProductConstraintManager.INSTANCE.isTrialLicense()) {
				return createAllPermissions();
			}
		}

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

	/**
	 * Checks whether the system property {@value #PROPERTY_SECURITY_ENFORCED} is set to
	 * {@code true}. This property is used to enable the full plugin sandbox security even on
	 * SNAPSHOT versions.
	 *
	 * @return {@code true} if the system property is set to 'true', {@code false} otherwise
	 */
	private static boolean isSecurityEnforced() {
		// no need to synchronize, if this is entered multiple times it's fine
		if (enforced == null) {
			enforced = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

				@Override
				public Boolean run() {
					return Boolean.parseBoolean(System.getProperty(PROPERTY_SECURITY_ENFORCED));
				}
			});

			if (enforced) {
				LogService.getRoot().log(Level.INFO,
						"Plugin sandboxing enforced via '" + PROPERTY_SECURITY_ENFORCED + "' property.");
			}
		}

		return enforced;
	}
}
