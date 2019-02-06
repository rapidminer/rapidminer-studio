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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.MacroHandler;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.gui.wizards.ConfigurationWizardCreator;
import com.rapidminer.tools.LogService;


/**
 * This parameter type will lead to a GUI element which can be used as initialization for a sort of
 * operator configuration wizard.
 *
 * @author Ingo Mierswa
 */
public class ParameterTypeConfiguration extends ParameterType {

	private static final long serialVersionUID = -3512071671355815277L;

	public static final String PARAMETER_DEFAULT_CONFIGURATION_NAME = "configure_operator";

	private Class<? extends ConfigurationWizardCreator> wizardCreatorClass;

	private transient ConfigurationListener wizardListener;

	private Map<String, String> parameters = null;

	public Object[] wizardConstructionArguments;

	public ParameterTypeConfiguration(Class<? extends ConfigurationWizardCreator> wizardCreatorClass,
			ConfigurationListener wizardListener) {
		this(wizardCreatorClass, null, wizardListener);
	}

	public ParameterTypeConfiguration(Class<? extends ConfigurationWizardCreator> wizardCreatorClass,
			Map<String, String> parameters, ConfigurationListener wizardListener) {
		this(wizardCreatorClass, parameters, wizardListener, null);

	}

	public ParameterTypeConfiguration(Class<? extends ConfigurationWizardCreator> wizardCreatorClass,
			Map<String, String> parameters, ConfigurationListener wizardListener, Object[] constructorArguments) {
		super(PARAMETER_DEFAULT_CONFIGURATION_NAME, "Configure this operator by means of a Wizard.");
		this.wizardCreatorClass = wizardCreatorClass;
		this.parameters = parameters;
		this.wizardListener = wizardListener;
		this.wizardConstructionArguments = constructorArguments;

		setPrimary(true);
	}

	/**
	 * Returns a new instance of the wizard creator. If anything does not work this method will
	 * return null.
	 */
	public ConfigurationWizardCreator getWizardCreator() {
		ConfigurationWizardCreator creator = null;
		try {
			if (wizardConstructionArguments == null) {
				creator = wizardCreatorClass.newInstance();
			} else {
				// if arguments were given: Use appropriate constructor
				Class<?>[] classes = new Class[wizardConstructionArguments.length];
				for (int i = 0; i < classes.length; i++) {
					classes[i] = wizardConstructionArguments[i].getClass();
				}
				try {
					for (Constructor<?> constructor : wizardCreatorClass.getConstructors()) {
						boolean fits = true;
						for (int i = 0; i < classes.length; i++) {
							Class<?>[] constructorParameter = constructor.getParameterTypes();
							if (i >= constructorParameter.length || !constructorParameter[i].isAssignableFrom(classes[i])) {
								fits = false;
								break;
							}
						}
						if (fits) {
							creator = (ConfigurationWizardCreator) constructor.newInstance(wizardConstructionArguments);
							break;
						}
					}
				} catch (SecurityException e) {
					creator = wizardCreatorClass.newInstance();
				} catch (IllegalArgumentException e) {
					creator = wizardCreatorClass.newInstance();
				} catch (InvocationTargetException e) {
					creator = wizardCreatorClass.newInstance();
				}
			}
			// this is ensured to be non null
			Objects.requireNonNull(creator);
			creator.setParameters(parameters);
		} catch (InstantiationException e) {
			// LogService.getGlobal().log("Problem during creation of wizard: " + e.getMessage(),
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.parameter.ParameterTypeConfiguration.problem_during_creation_of_wizard", e.getMessage());
		} catch (IllegalAccessException e) {
			// LogService.getGlobal().log("Problem during creation of wizard: " + e.getMessage(),
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.parameter.ParameterTypeConfiguration.problem_during_creation_of_wizard", e.getMessage());
		}
		return creator;
	}

	public ConfigurationListener getWizardListener() {
		return wizardListener;
	}

	/** Returns null. */
	@Override
	public Object getDefaultValue() {
		return null;
	}

	/** Does nothing. */
	@Override
	public void setDefaultValue(Object value) {}

	@Override
	public String getRange() {
		return null;
	}

	/**
	 * Returns an empty string since this parameter cannot be used in XML description but is only
	 * used for GUI purposes.
	 */
	@Override
	public String getXML(String indent, String key, String value, boolean hideDefault) {
		return "";
	}

	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public Element getXML(String key, String value, boolean hideDefault, Document doc) {
		return null;
	}

	@Override
	public String substituteMacros(String parameterValue, MacroHandler mh) {
		return parameterValue;
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {
		throw new UnsupportedOperationException("XML Definition not supported for this type");
	}
}
