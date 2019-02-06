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
 * Always returns the same meta data. Listeners are ignored because meta data never changes.
 * 
 * @author Simon Fischer
 * 
 */
public class StaticMetaDataProvider implements MetaDataProvider {

	private final MetaData metaData;

	public StaticMetaDataProvider(MetaData metaData) {
		this.metaData = metaData;
	}

	@Override
	public MetaData getMetaData() {
		return metaData;
	}

	@Override
	public void addMetaDataChangeListener(MetaDataChangeListener l) {
		// nothing to do: meta data does not change
	}

	@Override
	public void removeMetaDataChangeListener(MetaDataChangeListener l) {
		// nothing to do: meta data does not change
	}

}
