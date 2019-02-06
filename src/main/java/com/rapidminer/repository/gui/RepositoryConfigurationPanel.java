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
package com.rapidminer.repository.gui;

import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;

import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;


/**
 * Panel to configure a repository.
 * 
 * @author Simon Fischer
 * 
 */
public interface RepositoryConfigurationPanel {

	/**
	 * (Asynchronously) creates a new repository and adds it to the {@link RepositoryManager}.
	 * 
	 * @throws RepositoryException
	 */
	public void makeRepository() throws RepositoryException;

	/** Configures the UI elements to show the properties defined by the given repository. */
	public void configureUIElementsFrom(Repository repository);

	/**
	 * Configures given repository with the values entered into the dialog.
	 * 
	 * @return true if configuration is ok
	 */
	public boolean configure(Repository repository);

	/** Returns the actual component. */
	public JComponent getComponent();

	/** This button should be disabled when invalid values are entered. */
	public void setOkButton(JButton okButton);

	/**
	 * Additional buttons that will be shown left of the cancel button. If no additional buttons
	 * should be added an empty list should be returned.
	 */
	public List<AbstractButton> getAdditionalButtons();

}
