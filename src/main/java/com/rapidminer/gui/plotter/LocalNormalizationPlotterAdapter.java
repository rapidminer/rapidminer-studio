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
package com.rapidminer.gui.plotter;

import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterSettingsChangedListener;
import com.rapidminer.gui.plotter.settings.ListeningJCheckBox;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class LocalNormalizationPlotterAdapter extends LabelRotatingPlotterAdapter {

	private static final long serialVersionUID = -232182954939212825L;

	public static final String PARAMETER_LOCAL_NORMALIZATION = "local_normalization";

	private final ListeningJCheckBox localNormalizationBox;

	private boolean isLocalNormalized;

	/**
	 * @param settings
	 */
	public LocalNormalizationPlotterAdapter(final PlotterConfigurationModel settings) {
		super(settings);

		localNormalizationBox = new ListeningJCheckBox("_" + PARAMETER_LOCAL_NORMALIZATION, "Local Normalization", false);
		localNormalizationBox
				.setToolTipText("Indicates if a local normalization for each dimension should be performed or not.");
		localNormalizationBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				settings.setParameterAsBoolean(PARAMETER_LOCAL_NORMALIZATION, localNormalizationBox.isSelected());
			}
		});

	}

	@Override
	public List<ParameterType> getAdditionalParameterKeys(InputPort inputPort) {
		List<ParameterType> types = super.getAdditionalParameterKeys(inputPort);
		types.add(new ParameterTypeBoolean(PARAMETER_LOCAL_NORMALIZATION,
				"Indicates if values should be normalized for each dimension between 0 and 1.", false));
		return types;
	}

	@Override
	public void setAdditionalParameter(String key, String value) {
		super.setAdditionalParameter(key, value);
		if (key.equals(PARAMETER_LOCAL_NORMALIZATION)) {
			isLocalNormalized = Boolean.parseBoolean(value);
			updatePlotter();
		}
	}

	@Override
	public List<PlotterSettingsChangedListener> getListeningObjects() {
		List<PlotterSettingsChangedListener> list = super.getListeningObjects();
		list.add(localNormalizationBox);
		return list;
	}

	public boolean isLocalNormalized() {
		return isLocalNormalized;
	}

	protected JComponent getLocalNormalizationComponent() {
		return localNormalizationBox;
	}
}
