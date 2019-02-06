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
package com.rapidminer.operator.ports.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.quickfix.AttributeSelectionQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;


/**
 * This precondition might be used to ensure that a number of attributes is contained in the
 * exampleSet at the given port. If the attribute(s) are not contained, only a warning will be
 * given. Since this Precondition does not register errors beside the warning, it might be used in
 * addition to the ExampleSetPrecondition.
 *
 * An implementation of the AttributeNameProvider might be used to provide the names of attribute
 * unknown during creation time.
 *
 * @author Sebastian Land
 *
 */
public class AttributeSetPrecondition extends AbstractPrecondition {

	public static abstract class AttributeNameProvider {

		public abstract String[] getRequiredAttributeNames();
	}

	private static class ParameterAttributeNameProvider extends AttributeNameProvider {

		private final ParameterHandler handler;
		private final String[] parameterKeys;

		public ParameterAttributeNameProvider(ParameterHandler handler, String... parameterKeys) {
			this.handler = handler;
			this.parameterKeys = parameterKeys;
		}

		@Override
		public String[] getRequiredAttributeNames() {
			ArrayList<String> names = new ArrayList<String>();
			for (String key : parameterKeys) {
				try {
					names.add(handler.getParameterAsString(key));
				} catch (UndefinedParameterError e) {
				}
			}
			return names.toArray(new String[names.size()]);
		}

		public ParameterHandler getHandler() {
			return this.handler;
		}

		public String[] getParameterKeys() {
			return this.parameterKeys;
		}
	}

	private static class ParameterListAttributeNameProvider extends AttributeNameProvider {

		private final ParameterHandler handler;
		private final String parameterKey;
		private final int entry;

		public ParameterListAttributeNameProvider(ParameterHandler handler, String parameterKey, int entry) {
			this.handler = handler;
			this.parameterKey = parameterKey;
			this.entry = entry;
		}

		@Override
		public String[] getRequiredAttributeNames() {
			try {
				if (handler.isParameterSet(parameterKey)) {
					List<String[]> parameterList = handler.getParameterList(parameterKey);
					String[] attributeNames = new String[parameterList.size()];
					int i = 0;
					for (String[] pair : parameterList) {
						attributeNames[i] = pair[entry];
						i++;
					}
					return attributeNames;
				}
			} catch (UndefinedParameterError e) {
			}
			return new String[0];
		}

		public ParameterHandler getHandler() {
			return this.handler;
		}

		public String getParameterKey() {
			return this.parameterKey;
		}
	}

	private final String[] requiredAttributes;
	private final AttributeNameProvider requiredNameProvider;
	private final int requiredAttributesType;

	public AttributeSetPrecondition(InputPort inputPort, String... requiredAttributeNames) {
		this(inputPort, null, requiredAttributeNames);
	}

	public AttributeSetPrecondition(InputPort inputPort, AttributeNameProvider attributeNameProvider,
			String... requiredAttributeNames) {
		this(inputPort, attributeNameProvider, Ontology.VALUE_TYPE, requiredAttributeNames);
	}

	public AttributeSetPrecondition(InputPort inputPort, AttributeNameProvider attributeNameProvider,
			int typeOfRequiredAttributes, String... requiredAttributeNames) {
		super(inputPort);
		this.requiredAttributes = requiredAttributeNames;
		this.requiredNameProvider = attributeNameProvider;
		this.requiredAttributesType = typeOfRequiredAttributes;
	}

	@Override
	public void assumeSatisfied() {
		getInputPort().receiveMD(new ExampleSetMetaData());
	}

	@Override
	public void check(MetaData metaData) {
		if (metaData != null) {
			if (metaData instanceof ExampleSetMetaData) {
				ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
				// checking attribute names
				checkAttributeNames(requiredAttributes, emd);

				// checking provider name
				if (requiredNameProvider != null) {
					checkAttributeNames(requiredNameProvider.getRequiredAttributeNames(), emd);
				}
				makeAdditionalChecks(emd);
			}
		}
	}

	private void checkAttributeNames(String[] requiredNames, ExampleSetMetaData emd) {
		for (String attributeName : requiredNames) {
			if (attributeName != null && attributeName.length() > 0) {
				if (!attributeName.contains("%{")) {
					MetaDataInfo attInfo = emd.containsAttributeName(attributeName);
					if (attInfo == MetaDataInfo.NO) {
						QuickFix fix = null;
						try {
							fix = getQuickFix(emd);
						} catch (UndefinedParameterError e) {
						}
						Severity sev = Severity.WARNING;
						if (emd.getAttributeSetRelation() == SetRelation.EQUAL) {
							sev = Severity.ERROR;
						}
						if (fix != null) {
							createError(sev, Collections.singletonList(fix), "missing_attribute", attributeName);
						} else {
							createError(sev, "missing_attribute", attributeName);
						}
					} else if (attInfo == MetaDataInfo.YES) {
						AttributeMetaData amd = emd.getAttributeByName(attributeName);
						if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), requiredAttributesType)) {
							QuickFix fix = null;
							try {
								fix = getQuickFix(emd);
							} catch (UndefinedParameterError e) {
							}
							if (fix != null) {
								createError(Severity.ERROR, Collections.singletonList(fix), "attribute_has_wrong_type",
										amd.getName(), Ontology.VALUE_TYPE_NAMES[requiredAttributesType]);
							} else {
								createError(Severity.ERROR, "attribute_has_wrong_type", amd.getName(),
										Ontology.VALUE_TYPE_NAMES[requiredAttributesType]);
							}
						}
					}
				}
			}
		}
	}

	protected final String[] getRequiredNames() {
		int length = requiredAttributes.length;
		String[] providedNames = requiredNameProvider.getRequiredAttributeNames();
		if (requiredNameProvider != null) {
			length += providedNames.length;
		}
		String[] result = new String[length];
		for (int i = 0; i < requiredAttributes.length; i++) {
			result[i] = requiredAttributes[i];
		}
		for (int i = 0; i < providedNames.length; i++) {
			result[i + requiredAttributes.length] = providedNames[i];
		}
		return result;
	}

	/** Can be implemented by subclasses in order to specify quickfixes. */
	public QuickFix getQuickFix(ExampleSetMetaData emd) throws UndefinedParameterError {
		if (requiredNameProvider != null) {
			if (requiredNameProvider instanceof ParameterAttributeNameProvider) {
				ParameterAttributeNameProvider provider = (ParameterAttributeNameProvider) requiredNameProvider;
				if (provider.getParameterKeys().length == 1 && provider.getRequiredAttributeNames().length == 1) {
					return new AttributeSelectionQuickFix(emd, provider.getParameterKeys()[0], provider.getHandler(),
							provider.getRequiredAttributeNames()[0], requiredAttributesType);
				}
			}
		}
		return null;
	}

	/** Can be implemented by subclasses. */
	public void makeAdditionalChecks(ExampleSetMetaData emd) {}

	@Override
	public String getDescription() {
		return "<em>expects:</em> ExampleSet";
	}

	@Override
	public boolean isCompatible(MetaData input, CompatibilityLevel level) {
		return ExampleSet.class.isAssignableFrom(input.getObjectClass());
	}

	@Override
	public MetaData getExpectedMetaData() {
		return new ExampleSetMetaData();
	}

	public static AttributeNameProvider getAttributesByParameter(ParameterHandler handler, String... parameterKeys) {
		return new ParameterAttributeNameProvider(handler, parameterKeys);
	}

	/**
	 * Returns an AttributeNameProvider that can extract one column of a ParameterTypeList for use
	 * them as attribute names.
	 */
	public static AttributeNameProvider getAttributesByParameterListEntry(ParameterHandler handler, String parameterListKey,
			int entry) {
		return new ParameterListAttributeNameProvider(handler, parameterListKey, entry);
	}

}
