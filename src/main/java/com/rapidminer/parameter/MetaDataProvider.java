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
package com.rapidminer.parameter;

import com.rapidminer.operator.ports.MetaDataChangeListener;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * Many parameter types depend on {@link MetaData} arriving at the operator they belong to. This
 * meta data may be different each time the editor component is shown. To that end, the parameter
 * type can use this interface to query the current meta data.
 * 
 * GUI components can also add listeners to be informed upon change.
 * 
 * @author Simon Fischer
 * */
public interface MetaDataProvider {

	/** Returns the meta data currently avaiable. */
	public MetaData getMetaData();

	public void addMetaDataChangeListener(MetaDataChangeListener l);

	public void removeMetaDataChangeListener(MetaDataChangeListener l);

}
