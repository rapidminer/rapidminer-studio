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
package com.rapidminer.tools.config;

import java.util.Map;

import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.config.gui.ConfigurableDialog;


/**
 *
 * <p>
 * <strong> Please ALWAYS extends {@link AbstractConfigurable} instead of implementing this
 * interface directly. Reason is that this interface was not changed for compatibility reasons and
 * the {@link AbstractConfigurable} contains vital methods. </strong>
 * </p>
 *
 * Interface describing objects which can be listed and configured through a
 * {@link ConfigurableDialog}.
 *
 * For every {@link Configurable} localized information has to be saved in the resource file. See
 * {@link AbstractConfigurator#getTypeId()} for the <code>TYPE_ID</code>.
 * <p>
 * The required GUI i18n keys are:<br/>
 * <code>
 * gui.configurable.TYPE_ID.name<br/>
 * gui.configurable.TYPE_ID.description<br/>
 * gui.configurable.TYPE_ID.icon<br/>
 * 
 * gui.dialog.configuration.TYPE_ID.title<br/>
 * gui.dialog.configuration.TYPE_ID.icon<br/>
 * gui.dialog.configuration.TYPE_ID.message<br/>
 * 
 * gui.action.configuration.TYPE_ID.label<br/>
 * gui.action.configuration.TYPE_ID.icon<br/>
 * gui.action.configuration.TYPE_ID.mne<br/>
 * gui.action.configuration.TYPE_ID.tip<br/>
 * 
 * gui.configuration.TYPE_ID.list<br/>
 * gui.configuration.TYPE_ID.configuration
 * </code>
 * </p>
 *
 * @author Simon Fischer, Dominik Halfkann
 *
 */
public interface Configurable {

	/** Sets the user defined unique name. */
	public void setName(String name);

	/** Gets the user defined unique name. */
	public String getName();

	/**
	 * Sets the given parameters.
	 *
	 * @see #getParameters()
	 */
	public void configure(Map<String, String> parameterValues);

	/**
	 * The parameter values representing this Configurable.
	 *
	 * @see #configure(Map)
	 */
	public Map<String, String> getParameters();

	/**
	 * Returns the ID of this configurable in case it was retrieved from RapidMiner Server. This ID
	 * must be used when editing and saving a configurable.
	 *
	 * @see #getSource()
	 * @return -1 if this configurable was not loaded from RapidMiner Server
	 */
	public int getId();

	/**
	 * Called when loading and creating configurables.
	 *
	 * @see #getId()
	 */
	public void setId(int id);

	/**
	 * If this configurable was loaded from a RapidMiner Server instance, this is the connection it
	 * was loaded from. May be null for local entries.
	 *
	 * @see #getId()
	 */
	public RemoteRepository getSource();

	/** Set when this configurable was loaded from a RapidMiner Server instance. */
	public void setSource(RemoteRepository source);

	/**
	 * Gets the user defined short info which will be shown in the list on the left
	 */
	public String getShortInfo();

	/** Sets the parameter value for the given key **/
	public void setParameter(String key, String value);

	/** Gets the parameter value for the given key **/
	public String getParameter(String key);

	/**
	 * Compares the name and the parameter values of this Configurable with a given Configurable
	 **/
	public boolean hasSameValues(Configurable comparedConfigurable);

	/**
	 * Checks if the Configurable is empty (has no values/only empty values/default values)
	 *
	 * @param configurator
	 *            The configurator to resolve the default values from
	 * @deprecated Use {@link AbstractConfigurable#isEmptyOrDefault(AbstractConfigurator)} instead.
	 **/
	@Deprecated
	public boolean isEmptyOrDefault(Configurator<? extends Configurable> configurator);

	/** Returns the type id of the corresponding {@link Configurator}. */
	public String getTypeId();

}
