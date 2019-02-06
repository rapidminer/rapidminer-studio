/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui;

/**
 * An interface which GUI components that either do long running calculations and/or need to free resources after the UI is no
 * longer shown, should implement.
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public interface CleanupRequiringComponent {

	/**
	 * Called when the GUI is no longer needed, e.g. when it has been closed by the user. Use this
	 * to both stop any still outstanding calculations and do any clean-up necessary.
	 * <p>This method may be called on the EDT, so make sure this returns almost instantly! If you need to do lengthy
	 * clean-ups (e.g. file access or even HTTP connection access), create a {@link
	 * com.rapidminer.gui.tools.ProgressThread} and do the clean-up in it instead.</p>
	 */
	void cleanUp();
}
