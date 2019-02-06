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
import com.rapidminer.example.Example;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;

import java.util.List;


/**
 * This interface must be implemented by classes implementing an AttributeFilterCondition for the
 * AttributeFilter operator.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public interface AttributeFilterCondition {

	public static enum ScanResult {
		REMOVE, KEEP, UNCHECKED;

		public ScanResult invert(boolean invert) {
			switch (this) {
				case KEEP:
					return (invert) ? REMOVE : KEEP;
				case REMOVE:
					return (invert) ? KEEP : REMOVE;
				default:
					return UNCHECKED;
			}
		}
	};

	/**
	 * Initializes the condition before checking anything. If checking depends on parameters, their
	 * values might be retrieved in this method.
	 * 
	 * @throws UserError
	 * @throws ConditionCreationException
	 *             TODO
	 */
	public void init(ParameterHandler operator) throws UserError, ConditionCreationException;

	/**
	 * This method tries to check if the given attribute is contained, removed from the resulting
	 * operation or if the result is unpredictable.
	 * 
	 * @param attribute
	 *            the meta data of the attribute
	 * @param parameterHandler
	 *            to get the value of the defined parameters
	 * @return
	 * @throws ConditionCreationException
	 */
	public MetaDataInfo isFilteredOutMetaData(AttributeMetaData attribute, ParameterHandler parameterHandler)
			throws ConditionCreationException;

	/**
	 * Indicates if this filter needs a data scan, i.e. an invocation of the check method for each
	 * example.
	 */
	public boolean isNeedingScan();

	/**
	 * Indicates that this filter needs a full data scan and can evaluate its condition only after
	 * the full scan has been performed. If this method returns true, isNeedingScan must have
	 * returned true either.
	 */
	public boolean isNeedingFullScan();

	/**
	 * This method initializes this condition and resets all counters. It returns REMOVE, if the
	 * attribute can be removed without checking examples. If it has been removed, no checking
	 * during examples will occur. If it returns UNCHECKED, this Attribute Filter needs a full check
	 * and hence the attribute cannot be deleted or kept. Distinguishing this is important, because
	 * of the inverting, which otherwise might remove attributes although they only have been kept
	 * for later checking.
	 * 
	 * @param attribute
	 *            this is the attribute, the filter will have to check for.
	 * @throws ConditionCreationException
	 */
	public ScanResult beforeScanCheck(Attribute attribute) throws UserError;

	/**
	 * This method checks the given example. During this method the filter might check data to
	 * decide if attribute should be filtered out. If the condition needs a full scan before it can
	 * decide, this result is ignored.
	 */
	public ScanResult check(Attribute attribute, Example example);

	/**
	 * This method has to be invoked after a full scan has been performed if the isNeedingFullScan
	 * method returns true.
	 * 
	 * @return This method has to be restricted to return KEEP or REMOVED, but not unchecked
	 */
	public ScanResult checkAfterFullScan();

	/**
	 * This method is used to get parameters needed by this AttributeFilter
	 * 
	 * @param handler
	 *            the parameter handler for defining dependencies
	 */
	public List<ParameterType> getParameterTypes(ParameterHandler operator, InputPort inPort, int... valueTypes);

}
