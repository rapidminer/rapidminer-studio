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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComponent;

import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterSettingsChangedListener;
import com.rapidminer.gui.plotter.settings.ListeningJCheckBox;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.I18N;


/**
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class LabelRotatingPlotterAdapter extends PlotterAdapter {

	private static final long serialVersionUID = -8622638833472714672L;

	public static final String PARAMETER_ROTATE_LABELS = "rotate_labels";

	private final ListeningJCheckBox rotateLabels;

	private boolean rotateLabelsFlag = false;

	public LabelRotatingPlotterAdapter(final PlotterConfigurationModel settings) {
		super(settings);
		rotateLabels = new ListeningJCheckBox("_" + PARAMETER_ROTATE_LABELS,
				I18N.getGUILabel("plotter_panel.rotate_labels.label"), false);
		rotateLabels.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setParameterAsBoolean(PARAMETER_ROTATE_LABELS, rotateLabels.isSelected());
			}
		});

	}

	protected abstract void updatePlotter();

	@Override
	public List<ParameterType> getAdditionalParameterKeys(InputPort inputPort) {
		List<ParameterType> types = super.getAdditionalParameterKeys(inputPort);
		types.add(new ParameterTypeBoolean(PARAMETER_ROTATE_LABELS,
				"Indicates if the domain axis labels should be rotated.", false));
		return types;
	}

	public boolean isLabelRotating() {
		return rotateLabelsFlag;
	}

	@Override
	public void setAdditionalParameter(String key, String value) {
		super.setAdditionalParameter(key, value);
		if (key.equals(PARAMETER_ROTATE_LABELS)) {
			rotateLabelsFlag = Boolean.parseBoolean(value);
			updatePlotter();
		}
	}

	@Override
	public List<PlotterSettingsChangedListener> getListeningObjects() {
		List<PlotterSettingsChangedListener> list = super.getListeningObjects();
		list.add(rotateLabels);
		return list;
	}

	protected JComponent getRotateLabelComponent() {
		return rotateLabels;
	}
}
