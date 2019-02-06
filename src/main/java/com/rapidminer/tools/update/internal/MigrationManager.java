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
package com.rapidminer.tools.update.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;


/**
 * Updates of RM Studio sometimes require specific changes to parts of the installation. With this class it is possible
 * to execute these changes only if the specific version update happened.
 *
 * @author Andreas Timm
 * @since 9.1.0
 */
public final class MigrationManager {

	/**
	 * The {@link MigrationStep} gets all the information which is required to check if the update is necessary and
	 * run the update itself.
	 */
	static class MigrationStep {

		/**
		 * Name of the step for information through logs
		 */
		private String name;

		/**
		 * Minimal version from which the changes are possible
		 */
		private VersionNumber fromVersion;

		/**
		 * The target version of this migration must be at most the Studio version.
		 */
		private VersionNumber toVersion;

		/**
		 * The code that will be executed
		 */
		private Runnable runnable;

		/**
		 * {@link MigrationStep} holds all the information for migration
		 *
		 * @param name
		 * 		of the step, will be shown in the logs
		 * @param fromVersion
		 * 		minimal version for the migration to make sense and be executed, may be null for any
		 * @param toVersion
		 * 		only run the migration if at least the target version is reached
		 * @param runnable
		 * 		the actual migration
		 */
		public MigrationStep(String name, VersionNumber fromVersion, VersionNumber toVersion, Runnable runnable) {
			this.name = Objects.requireNonNull(name);
			this.fromVersion = fromVersion;
			this.toVersion = Objects.requireNonNull(toVersion);
			this.runnable = Objects.requireNonNull(runnable);
		}

		/**
		 * Minimal version for the migration
		 *
		 * @return a {@link VersionNumber} or null
		 */
		private VersionNumber getFromVersion() {
			return fromVersion;
		}

		/**
		 * Minimal Studio version for the migration
		 *
		 * @return a {@link VersionNumber}
		 */
		private VersionNumber getToVersion() {
			return toVersion;
		}

		/**
		 * Execute the step safely, will catch any error and just log it.
		 */
		private void runSafe() {
			try {
				LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.update.internal.MigrationManager.starting", name);
				runnable.run();
				LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.update.internal.MigrationManager.success", name);

			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(), "com.rapidminer.tools.update.internal.MigrationManager.failure", name), e);
			}
		}


	}

	/**
	 * List of all possible migration steps.
	 */
	private static List<MigrationStep> steps = new ArrayList<>();

	/**
	 * Migration of the parameter "disconnect_on_disable" is done here
	 *
	 * @since 9.1.0
	 */
	private static final MigrationStep REMOVE_DISCONNECT_ON_DISABLE_SETTING = new MigrationStep("Migrate disconnect on disable setting", null,
			new VersionNumber(9, 1, 0), () -> {
		if (RapidMiner.getExecutionMode().canAccessFilesystem() && !RapidMiner.getExecutionMode().isHeadless()) {
			File userConfigFile = FileSystemService.getUserConfigFile(ParameterService.RAPIDMINER_CONFIG_FILE_NAME);
			if (userConfigFile != null) {
				Properties properties = new Properties();
				// read the user config for RM Studio
				try (InputStream in = new FileInputStream(userConfigFile)) {
					properties.load(in);
					final String disconnectOnDisableKey = "rapidminer.gui.disconnect_on_disable";
					String disconnectOnDisableValue = properties.getProperty(disconnectOnDisableKey);
					if (disconnectOnDisableValue != null) {
						// Migration strategy:
						// if disconnect_on_disable was set to true, set the new parameters disable_op_conn_behavior and delete_op_conn_behavior to drop
						// else set disable_op_conn_behavior to keep and delete_op_conn_behavior to bridge
						if (Boolean.parseBoolean(disconnectOnDisableValue)) {
							if (!properties.containsKey(RapidMinerGUI.PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR)) {
								properties.setProperty(RapidMinerGUI.PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR, RapidMinerGUI.PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR_VALUES[0]);
							}
							if (!properties.containsKey(RapidMinerGUI.PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR)) {
								properties.setProperty(RapidMinerGUI.PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR, RapidMinerGUI.PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR_VALUES[0]);
							}
						} else {
							if (!properties.containsKey(RapidMinerGUI.PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR)) {
								properties.setProperty(RapidMinerGUI.PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR, RapidMinerGUI.PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR_VALUES[RapidMinerGUI.PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR_DEFAULT_VALUE]);
							}
							if (!properties.containsKey(RapidMinerGUI.PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR)) {
								properties.setProperty(RapidMinerGUI.PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR, RapidMinerGUI.PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR_VALUES[RapidMinerGUI.PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR_DEFAULT_VALUE]);
							}
						}
						properties.remove(disconnectOnDisableKey);
					}
					storeProperties(userConfigFile, properties);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
	});

	static {
		steps.add(REMOVE_DISCONNECT_ON_DISABLE_SETTING);
	}

	/**
	 * Util class, not to be instantiated.
	 */
	private MigrationManager() {
		throw new UnsupportedOperationException("Util class, not to be instantiated!");
	}

	/**
	 * Calculate the necessary migration and execute it. Will be executed in the order of appearance in the list steps.
	 *
	 * @param fromVersion
	 * 		the  version that was last in use before the update
	 * @param toVersion
	 * 		the current version
	 */
	public static void doMigrate(VersionNumber fromVersion, VersionNumber toVersion) {
		if (fromVersion == null || toVersion == null) {
			return;
		}

		getNecessaryMigrationSteps(fromVersion, toVersion).forEach(MigrationStep::runSafe);
	}

	/**
	 * Find the necessary migration steps.
	 *
	 * @param fromVersion
	 * 		version that was in use previously
	 * @param toVersion
	 * 		current version
	 * @return Stream of the steps that need to be run now
	 */
	static List<MigrationStep> getNecessaryMigrationSteps(VersionNumber fromVersion, VersionNumber toVersion) {
		if (fromVersion == null || toVersion == null) {
			return Collections.emptyList();
		}
		return steps.stream().filter(migrationStep -> (migrationStep.getFromVersion() == null || fromVersion.isAtLeast(migrationStep.getFromVersion()))
				&& migrationStep.getToVersion().isAtMost(toVersion) && migrationStep.getToVersion().isAbove(fromVersion)).collect(Collectors.toList());
	}

	/**
	 * Store properties in this file
	 *
	 * @param outfile
	 * 		where to store the properties
	 * @param properties
	 * 		the properties to be stored
	 */
	private static void storeProperties(File outfile, Properties properties) {
		try (FileOutputStream fos = new FileOutputStream(outfile); BufferedOutputStream out = new BufferedOutputStream(fos)) {
			properties.store(out, "");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
