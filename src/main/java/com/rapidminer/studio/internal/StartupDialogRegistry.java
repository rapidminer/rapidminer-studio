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
package com.rapidminer.studio.internal;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.studio.internal.StartupDialogProvider.ToolbarButton;


/**
 * Registry for a {@link StartupDialogProvider}.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
public enum StartupDialogRegistry {
	INSTANCE;

	private StartupDialogProvider provider;

	/**
	 * Registers the provider and disregards all previously registered {@link StartupDialogProvider}
	 * s.
	 *
	 * @param provider
	 *            the provider to register
	 */
	public void register(StartupDialogProvider provider) {
		this.provider = provider;
	}

	/**
	 * Shows the startup dialog provided by the registered provider with the tab of the startButton
	 * opened.
	 *
	 * @param startButton
	 *            the toolbar button to preselect
	 * @throws NoStartupDialogRegistreredException
	 *             if no {@link StartupDialogProvider} is registered
	 */
	public void showStartupDialog(final ToolbarButton startButton) throws NoStartupDialogRegistreredException {
		if (provider == null) {
			throw new NoStartupDialogRegistreredException();
		} else {
			SwingTools.invokeLater(() -> provider.show(startButton));
		}
	}
}
