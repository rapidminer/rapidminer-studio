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
package com.rapidminer.repository;

import org.w3c.dom.Element;

import com.rapidminer.repository.gui.NewRepositoryDialog;
import com.rapidminer.repository.gui.RepositoryConfigurationPanel;
import com.rapidminer.tools.XMLException;


/**
 *
 * A factory that knows how to create instances of custom {@link Repository} implementations. The
 * user is able to create new repositories via the {@link RepositoryConfigurationPanel}. Once the
 * repository has been created its configuration is stored via XML. When starting RapidMiner the
 * repository is recreated from XML via {@link #fromXML(Element)}. Register a factory at the
 * {@link CustomRepositoryRegistry} during extension initialization.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public interface CustomRepositoryFactory {

	/**
	 * @return {@code true} if the radio button for this repository should be enabled when opening
	 *         the {@link NewRepositoryDialog}, {@code false} otherwise
	 */
	boolean enableRepositoryConfiguration();

	/**
	 * @return the {@link RepositoryConfigurationPanel} for this factory. The method is called only
	 *         once per opened {@link NewRepositoryDialog}.
	 */
	RepositoryConfigurationPanel getRepositoryConfigurationPanel();

	/**
	 * Recreates a {@link Repository} instance from XML.
	 *
	 * @param element
	 *            the XML element specified by {@link #getXMLTag()}
	 * @return the recreated repository instance
	 * @throws RepositoryException
	 *             in case the repository creation failed
	 * @throws XMLException
	 *             in case of invalid XML
	 */
	Repository fromXML(Element element) throws RepositoryException, XMLException;

	/**
	 * @return the XML tag used to store repository instances.
	 */
	String getXMLTag();

	/**
	 * @return the I18N base key. It is used in the {@link NewRepositoryDialog} to label the radio
	 *         button for the repository (e.g. the key 'custom_repo' would need such a GUI
	 *         properties entry:
	 *         <p>
	 *         gui.action.custom_repo.label=Custom repo)
	 */
	String getI18NKey();

	/**
	 * @return the actual {@link Repository} implementation class
	 */
	Class<? extends Repository> getRepositoryClass();

}
