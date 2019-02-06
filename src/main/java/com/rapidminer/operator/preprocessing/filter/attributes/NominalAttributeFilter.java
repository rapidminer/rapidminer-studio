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
package com.rapidminer.operator.preprocessing.filter.attributes;

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.parameter.ParameterHandler;


/**
 * This class implements the condition if an attribute is nominal. All non-nominal attributes will
 * be removed.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class NominalAttributeFilter extends AbstractAttributeFilterCondition {

	@Override
	public MetaDataInfo isFilteredOutMetaData(AttributeMetaData attribute, ParameterHandler handler) {
		return attribute.isNominal() ? MetaDataInfo.NO : MetaDataInfo.YES;
	}

	@Override
	public ScanResult beforeScanCheck(Attribute attribute) throws UserError {
		if (!attribute.isNominal()) {
			return ScanResult.REMOVE;
		} else {
			return ScanResult.KEEP;
		}
	}
}
