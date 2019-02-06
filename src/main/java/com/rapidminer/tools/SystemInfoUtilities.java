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
package com.rapidminer.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Helper class to get system information.
 *
 * @author Nils Woehler
 */
public final class SystemInfoUtilities {

	private static final int TENTH_POWER_OF_TWO = 1024;
	private static final Logger LOGGER = Logger.getLogger(SystemInfoUtilities.class.getSimpleName());

	/**
	 * Available JVM architectures.
	 */
	public enum JVMArch {
		/**
		 * 32 bit
		 */
		THIRTY_TWO,
		/**
		 * 64 bit
		 */
		SIXTY_FOUR,
	}

	/**
	 * Possible operating systems.
	 */
	public enum OperatingSystem {
		/**
		 * Windows
		 */
		WINDOWS,
		/**
		 * Apple OSX
		 */
		OSX,
		/**
		 * Any Unix derivat
		 */
		UNIX,
		/**
		 * Any Solaris derivat
		 */
		SOLARIS,
		/**
		 * Unknown other operating system.
		 */
		OTHER,
	}

	/**
	 * Utility class constructor.
	 */
	private SystemInfoUtilities() {
		throw new AssertionError();
	}

	private static OperatingSystemMXBean getOperatingSystemBean() {
		return ManagementFactory.getOperatingSystemMXBean();
	}

	/**
	 * @return the current max heap memory size in MB
	 */
	public static long getMaxHeapMemorySize() {
		long maxMem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
		maxMem /= TENTH_POWER_OF_TWO;
		maxMem /= TENTH_POWER_OF_TWO;
		return maxMem;
	}

	/**
	 * @return the number of processors
	 */
	public static int getNumberOfProcessors() {
		return getOperatingSystemBean().getAvailableProcessors();
	}

	public static Integer getJavaVersion() {
		return Integer.parseInt(System.getProperty("java.version").substring(0, 3).replace(".", ""));
	}

	/**
	 * @return the current {@link JVMArch}
	 */
	public static JVMArch getJVMArchitecture() {
		if (getOperatingSystemBean().getArch().contains("64")) {
			return JVMArch.SIXTY_FOUR;
		} else {
			return JVMArch.THIRTY_TWO;
		}
	}

	/**
	 * Returns total physical memory in MB. If it is run in a 32-bit JVM it may return only a
	 * maximum of 4GB even if more memory is available.
	 *
	 * @return the amount of physical memory in MB
	 * @throws IOException
	 *             if something goes wrong
	 */
	public static Long getTotalPhysicalMemorySize() throws IOException {
		OperatingSystemMXBean operatingSystemBean = getOperatingSystemBean();

		long memory = 0L;

		// if the system bean is an implementation by sun, we are almost done
		try {
			memory = ((com.sun.management.OperatingSystemMXBean) operatingSystemBean).getTotalPhysicalMemorySize();
		} catch (Throwable t) {  // NOPMD
			// // fallback because sun implementation is not available
			switch (getOperatingSystem()) {
				case OSX:
					memory = readOSXTotalMemory();
					break;
				case WINDOWS:
					memory = readWindowsTotalMemory();
					break;
				case UNIX:
					memory = readUnixTotalMemory();
					break;
				case SOLARIS:
					memory = readSolarisTotalMemory();
					break;
				case OTHER:
				default:
					memory = readOtherTotalMemory();
					break;
			}
		}

		memory /= TENTH_POWER_OF_TWO; // kbyte
		memory /= TENTH_POWER_OF_TWO; // mbyte
		return memory;
	}

	/**
	 * Returns free physical memory in MB. If it is run in a 32-bit JVM it may return only a maximum
	 * of 2GB even if more memory is available.
	 * <p>
	 * <strong>Attention:</strong> The returned value is dependant on the OS. For example on Linux
	 * you might see less memory than there is actually available due to the OS using it for disk
	 * caching. That memory is not actually blocked because the OS would release it if requested,
	 * however this method will return the free memory which does NOT contain the ram used for disk
	 * caching. Use with care!
	 * </p>
	 *
	 * @return the free phsycial memory in MB or <code>-1</code> if we cannot determine it
	 * @since 6.0.004
	 *
	 */
	public static Long getFreePhysicalMemorySize() {
		OperatingSystemMXBean operatingSystemBean = getOperatingSystemBean();

		long memory = 0L;

		// if the system bean is an implementation by sun, we are almost done
		try {
			memory = ((com.sun.management.OperatingSystemMXBean) operatingSystemBean).getFreePhysicalMemorySize();
		} catch (Throwable t) { // NOPMD
			// fallback due to sun implementation not being available
			// in this case we behave as before 6.0.004 where we did not take free memory into
			// account
			return -1L;
		}

		memory /= TENTH_POWER_OF_TWO; // kbyte
		memory /= TENTH_POWER_OF_TWO; // mbyte
		return memory;
	}

	private static String executeMemoryInfoProcess(String... command) throws IOException {
		ProcessBuilder procBuilder = new ProcessBuilder(command);
		Process process = procBuilder.start();

		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
		BufferedReader br = new BufferedReader(isr);
		try {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				} else {
					return line;
				}
			}
		} catch (IOException e1) {   // NOPMD
			throw e1;
		} finally {
			br.close();
		}
		throw new IOException("Could not read memory process output for command " + Arrays.toString(command));
	}

	/**
	 * @return total memory in bytes
	 */
	private static Long readWindowsTotalMemory() throws IOException {
		String[] command = "wmic OS get TotalVisibleMemorySize /Value".split(" ");
		// Output should be something like 'TotalVisibleMemorySize=8225260'
		String line = executeMemoryInfoProcess(command);
		// convert it to bytes
		return Long.parseLong(line.substring(line.indexOf('=') + 1)) * TENTH_POWER_OF_TWO;
	}

	/**
	 * @return total memory in bytes
	 */
	private static Long readOSXTotalMemory() throws IOException {
		String[] command = "sysctl -a | grep hw.memsize".split(" ");
		String line = executeMemoryInfoProcess(command);
		return Long.parseLong(line.substring(line.indexOf(':') + 2));
	}

	/**
	 * @return total memory in bytes
	 */
	private static Long readSolarisTotalMemory() throws IOException {
		String[] command = "prtconf | grep Memory".split(" ");

		// output should be something like 'Memorysize: 8192 Megabytes'
		String line = executeMemoryInfoProcess(command);

		line = line.substring(line.indexOf(':') + 2); // shorten output to '8192 Megabytes'
		line = line.substring(0, line.indexOf(' ')); // shorten to just '8192'
		return Long.parseLong(line) * TENTH_POWER_OF_TWO * TENTH_POWER_OF_TWO;
	}

	/**
	 * @return total memory in bytes
	 */
	private static Long readUnixTotalMemory() throws IOException {
		String[] command = "grep MemTotal /proc/meminfo".split(" ");
		// should output something like 'MemTotal: 12297204 kB'
		String line = executeMemoryInfoProcess(command);

		line = line.substring(line.indexOf(':') + 1).trim(); // shorten to '12297204 kB'
		line = line.substring(0, line.indexOf(' ')); // shorten to just '12297204'
		return Long.parseLong(line) * TENTH_POWER_OF_TWO;
	}

	/**
	 * @return total memory in bytes
	 */
	private static Long readOtherTotalMemory() throws IOException {
		throw new IOException("Not yet implemented"); // TODO implement
	}

	public static String getOperatingSystemName() {
		return getOperatingSystemBean().getName();
	}

	public static String getOperatingSystemVersion() {
		return getOperatingSystemBean().getVersion();
	}

	/**
	 * @return the current operating system
	 */
	public static OperatingSystem getOperatingSystem() {
		String systemName = getOperatingSystemName().toLowerCase();
		if (isWindows(systemName)) {
			return OperatingSystem.WINDOWS;
		} else if (isMac(systemName)) {
			return OperatingSystem.OSX;
		} else if (isUnix(systemName)) {
			return OperatingSystem.UNIX;
		} else if (isSolaris(systemName)) {
			return OperatingSystem.SOLARIS;
		} else {
			return OperatingSystem.OTHER;
		}
	}

	private static boolean isWindows(String osName) {
		return osName.indexOf("win") >= 0;
	}

	private static boolean isMac(String osName) {
		return osName.indexOf("mac") >= 0;
	}

	private static boolean isUnix(String osName) {
		return osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0 || osName.indexOf("aix") > 0;
	}

	private static boolean isSolaris(String osName) {
		return osName.indexOf("sunos") >= 0;
	}

	/**
	 * Uses the logger to write current environment infos to the log (with {@link Level#INFO}).
	 */
	public static void logEnvironmentInfos() {
		if (LOGGER.isLoggable(Level.INFO)) {
			LOGGER.log(Level.INFO, "Operating system: " + getOperatingSystemName() + ", Version: "
					+ getOperatingSystemVersion());
			LOGGER.log(Level.INFO, "Number of logical processors: " + getNumberOfProcessors());
			LOGGER.log(Level.INFO, "Java version: " + getJavaVersion());
			LOGGER.log(Level.INFO, "JVM Architecture: " + getJVMArchitecture());
		}
		try {
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.log(Level.INFO, "Maxmimum physical memory available for JVM: " + getTotalPhysicalMemorySize() + "mb");
			}
		} catch (IOException e) {
			if (LOGGER.isLoggable(Level.WARNING)) {
				LOGGER.log(Level.WARNING, "Could not detect total physical memory.");
			}
		}
		if (LOGGER.isLoggable(Level.INFO)) {
			LOGGER.log(Level.INFO, "Free physical memory available for JVM: " + getFreePhysicalMemorySize() + "mb");
		}
	}

	/**
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		logEnvironmentInfos();
	}
}
