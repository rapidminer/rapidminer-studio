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
package com.rapidminer.gui.tools;

import java.util.Locale;
import java.util.regex.Pattern;


/**
 * Contains information about the different parts of a version number.
 *
 * @author Ingo Mierswa, Marcel Michel
 */
public class VersionNumber implements Comparable<VersionNumber> {

	/**
	 * @deprecated use {@link VersionNumberException} instead
	 */
	@Deprecated
	public class VersionNumberExcpetion extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public VersionNumberExcpetion(String message) {
			super(message);
		}
	}

	/**
	 * Exception for malformed VersionNumber String representations including further information
	 * @since 8.0
	 */
	public class VersionNumberException extends VersionNumberExcpetion {
		public VersionNumberException(String message) {
			super(message);
		}
	}

	private static final String CLASSIFIER_TAG = "-";

	private static final String ALPHA_TAG = "alpha";

	private static final String BETA_TAG = "beta";

	private static final String RELEASE_CANDIDATE = "rc";

	private static final String SNAPSHOT_TAG = "snapshot";

	private int majorNumber;

	private int minorNumber;

	private int patchLevel;

	private final String classifier;

	// comparing BETA and BETA1 should be considered equal, using this helper therefore
	private static final Pattern patternVersionClassifierOne = Pattern.compile("-[a-z]+1");

	/**
	 * Constructs a new VersionNumber object with the given versionString. The versionString should
	 * use the format major.minor.patchlevel-classifier. Examples: 6.0 or 6.0.005-SNAPSHOT. Throws a
	 * {@link VersionNumberException} if the given versionString is malformed.
	 */
	public VersionNumber(String versionString) {
		String version = versionString.toLowerCase().trim();
		String[] numbers;
		int classifierIndex = version.indexOf(CLASSIFIER_TAG);
		if (classifierIndex >= 0) {
			numbers = version.substring(0, classifierIndex).split("\\.");
		} else {
			numbers = version.split("\\.");
		}

		// extract major, minor and patch level version
		if (numbers.length > 0) {
			try {
				majorNumber = Integer.parseInt(numbers[0]);
			} catch (NumberFormatException e) {
				throw new VersionNumberException("Malformed major version!");
			}
		} else {
			throw new VersionNumberException("No major version given!");
		}
		if (numbers.length > 1) {
			try {
				minorNumber = Integer.parseInt(numbers[1]);
			} catch (NumberFormatException e) {
				throw new VersionNumberException("Malformed minor version!");
			}
		} else {
			minorNumber = 0;
		}
		if (numbers.length > 2) {
			try {
				patchLevel = Integer.parseInt(numbers[2]);
			} catch (NumberFormatException e) {
				throw new VersionNumberException("Malformed patch level!");
			}
		} else {
			patchLevel = 0;
		}
		if (classifierIndex >= 0) {
			classifier = CLASSIFIER_TAG + version.substring(classifierIndex + 1);
		} else {
			classifier = null;
		}
	}

	/**
	 * Constructs a new VersionNumber object with the given major and minor version.
	 */
	public VersionNumber(int majorNumber, int minorNumber) {
		this(majorNumber, minorNumber, 0, null);
	}

	/**
	 * Constructs a new VersionNumber object with the given major, minor version and patch level.
	 */
	public VersionNumber(int majorNumber, int minorNumber, int patchLevel) {
		this(majorNumber, minorNumber, patchLevel, null);
	}

	/**
	 * Constructs a new VersionNumber object with the given major, minor version, patch level and
	 * classifier. Note: A {@link #CLASSIFIER_TAG} will be added as prefix to the given classifier.
	 */
	public VersionNumber(int majorNumber, int minorNumber, int patchLevel, String classifier) {
		this.majorNumber = majorNumber;
		this.minorNumber = minorNumber;
		this.patchLevel = patchLevel;
		if (classifier != null) {
			this.classifier = CLASSIFIER_TAG + classifier;
		} else {
			this.classifier = null;
		}
	}

	/**
	 * @deprecated Use {@link #VersionNumber(int, int, int, String)} instead.
	 */
	@Deprecated
	public VersionNumber(int majorNumber, int minorNumber, int patchLevel, boolean alpha, int alphaNumber, boolean beta,
						 int betaNumber) {
		this.majorNumber = majorNumber;
		this.minorNumber = minorNumber;
		this.patchLevel = patchLevel;
		if (alpha) {
			this.classifier = CLASSIFIER_TAG + ALPHA_TAG + (alphaNumber >= 2 ? alphaNumber : "");
		} else if (beta) {
			this.classifier = CLASSIFIER_TAG + BETA_TAG + (betaNumber >= 2 ? betaNumber : "");
		} else {
			this.classifier = null;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (classifier == null ? 0 : normalize(classifier).hashCode());
		result = prime * result + majorNumber;
		result = prime * result + minorNumber;
		result = prime * result + patchLevel;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		VersionNumber other = (VersionNumber) obj;

		if (majorNumber != other.majorNumber) {
			return false;
		}
		if (minorNumber != other.minorNumber) {
			return false;
		}
		if (patchLevel != other.patchLevel) {
			return false;
		}
		return this.compareClassifier(other) == 0;
	}

	/**
	 * Returns if this number is at least as high as the given arguments.
	 */
	public boolean isAtLeast(int major, int minor, int buildNumber) {
		return this.compareTo(new VersionNumber(major, minor, buildNumber)) >= 0;
	}

	/**
	 * Returns if this number is at least as high as the given version number object.
	 */
	public boolean isAtLeast(VersionNumber other) {
		return this.compareTo(other) >= 0;
	}

	/**
	 * Returns <code>true</code> if this number is at most as high as the given arguments.
	 */
	public boolean isAtMost(int major, int minor, int buildNumber) {
		return this.compareTo(new VersionNumber(major, minor, buildNumber)) <= 0;
	}

	/**
	 * Returns <code>true</code> if this VersionNumber is at most as high as the given argument.
	 */
	public boolean isAtMost(VersionNumber other) {
		return this.compareTo(other) <= 0;
	}

	/**
	 * Returns <code>true</code> if this VersionNumber is above (greater than) the given argument.
	 */
	public boolean isAbove(VersionNumber other) {
		return !isAtMost(other);
	}

	@Override
	public int compareTo(VersionNumber o) {
		if(o == null) {
			return 1;
		}
		int index = Double.compare(this.majorNumber, o.majorNumber);
		if (index != 0) {
			return index;
		} else {
			index = Double.compare(this.minorNumber, o.minorNumber);
			if (index != 0) {
				return index;
			} else {
				index = Double.compare(this.patchLevel, o.patchLevel);
				if (index != 0) {
					return index;
				}
				return compareClassifier(o);
			}
		}
	}

	/**
	 * SNAPSHOT < ALPHA == ALPHA1 < ALPHA2 < ALPHA200 < BETA == BETA1 < BETA2 < BETA300 < RC == RC1 < RC2 < RC400
	 *
	 * @param o VersionNumber to be classifier-compared to this VersionNumbers classifier
	 * @return
	 */
	private int compareClassifier(VersionNumber o) {
		if (classifier != null) {
			if (o.classifier != null) {
				if (isSnapshot()) {
					if (o.isSnapshot()) {
						return 0;
					} else {
						return -1;
					}
				}
				if(o.isSnapshot()) {
					return 1;
				}
				String normalizedClassifier = normalize(classifier);
				String normalizedOtherClassifier = normalize(o.classifier);
				return normalizedClassifier.compareTo(normalizedOtherClassifier);
			}
			return -1;
		} else {
			if (o.classifier != null) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	/**
	 * create a lowercase version that changes beta1 to beta or alpha1 to alpha etc
	 *
	 * @param input
	 * @return
	 */
	private String normalize(String input) {
		String output = input.toLowerCase();
		if (patternVersionClassifierOne.matcher(output).matches()) {
			output = output.substring(0, output.length() - 1);
		}
		return output;
	}

	@Override
	public String toString() {
		return majorNumber + "." + minorNumber + "." + "000".substring((patchLevel + "").length()) + patchLevel
				+ (classifier != null ? classifier.toUpperCase(Locale.ENGLISH) : "");
	}

	/**
	 * Assembles the String representation to be a minimal representation without adding leading zeros to the patchlevel
	 * @return a minimal String representation of this VersionNumber
	 */
	public String getShortLongVersion() {
		return majorNumber + "." + minorNumber + "." + patchLevel
				+ (classifier != null ? classifier.toUpperCase(Locale.ENGLISH) : "");
	}

	/**
	 * Returns the RapidMiner version in the format major.minor.patchlevel-classifier, with 3 digits
	 * for patchlevel. Example: 6.0.005-SNAPSHOT
	 */
	public String getLongVersion() {
		return toString();
	}

	/**
	 * Return the RapidMiner short version in the format major.minor. Example: 6.0
	 */
	public String getShortVersion() {
		return majorNumber + "." + minorNumber;
	}

	/**
	 * Return the RapidMiner major version.
	 */
	public int getMajorNumber() {
		return majorNumber;
	}

	/**
	 * Return the RapidMiner minor version.
	 */
	public int getMinorNumber() {
		return minorNumber;
	}

	/**
	 * Return the RapidMiner path level.
	 */
	public int getPatchLevel() {
		return patchLevel;
	}

	/**
	 * @return <code>true</code> if the current version is a development build (i.e. it has a classifier named SNAPSHOT
	 * or ALPHA or BETA or RC).
	 */
	public final boolean isDevelopmentBuild() {
		return isSnapshot() || isPreview(ALPHA_TAG) || isPreview(BETA_TAG) || isPreview(RELEASE_CANDIDATE);
	}

	/**
	 * @return {@code true} if the current version is a snapshot build (exactly if it has a classifier named SNAPSHOT).
	 */
	public final boolean isSnapshot() {
		return isPreview(SNAPSHOT_TAG);
	}

	private boolean isPreview(String tagName) {
		if (classifier != null) {
			String lowerCase = classifier.toLowerCase(Locale.ENGLISH);
			if (lowerCase.contains(CLASSIFIER_TAG)) {
				String suffix = lowerCase.substring(lowerCase.lastIndexOf(CLASSIFIER_TAG) + 1);
				return suffix.startsWith(tagName);
			} else {
				return false;
			}
		}
		return false;
	}
}
