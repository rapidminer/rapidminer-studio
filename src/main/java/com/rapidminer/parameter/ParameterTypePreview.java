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

import com.rapidminer.MacroHandler;
import com.rapidminer.gui.wizards.PreviewCreator;
import com.rapidminer.gui.wizards.PreviewListener;
import com.rapidminer.tools.LogService;

import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * This parameter type will lead to a GUI element which can be used as initialization for a results
 * preview. This might be practical especially for complex operators which often also provide a
 * configuration wizard.
 * 
 * @author Ingo Mierswa
 */
public class ParameterTypePreview extends ParameterType {

	private static final long serialVersionUID = 6538432700371374278L;

	private Class<? extends PreviewCreator> previewCreatorClass;

	private transient PreviewListener previewListener;

	public ParameterTypePreview(Class<? extends PreviewCreator> previewCreatorClass, PreviewListener previewListener) {
		this("preview", "Shows a preview for the results which will be achieved by the current configuration.",
				previewCreatorClass, previewListener);
	}

	public ParameterTypePreview(String parameterName, String description,
			Class<? extends PreviewCreator> previewCreatorClass, PreviewListener previewListener) {
		super(parameterName, description);
		this.previewCreatorClass = previewCreatorClass;
		this.previewListener = previewListener;
	}

	/**
	 * Returns a new instance of the wizard creator. If anything does not work this method will
	 * return null.
	 */
	public PreviewCreator getPreviewCreator() {
		PreviewCreator creator = null;
		try {
			creator = previewCreatorClass.newInstance();
		} catch (InstantiationException e) {
			// LogService.getGlobal().log("Problem during creation of previewer: " + e.getMessage(),
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.parameter.ParameterTypePreview.problem_during_creation_of_previewer", e.getMessage());
		} catch (IllegalAccessException e) {
			// LogService.getGlobal().log("Problem during creation of previewer: " + e.getMessage(),
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.parameter.ParameterTypePreview.problem_during_creation_of_previewer", e.getMessage());
		}
		return creator;
	}

	public PreviewListener getPreviewListener() {
		return previewListener;
	}

	/** Returns null. */
	@Override
	public Object getDefaultValue() {
		return null;
	}

	/** Does nothing. */
	@Override
	public void setDefaultValue(Object defaultValue) {}

	/** Returns null. */
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
}
