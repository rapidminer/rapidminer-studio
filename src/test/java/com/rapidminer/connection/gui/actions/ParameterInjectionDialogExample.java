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
package com.rapidminer.connection.gui.actions;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.connection.gui.model.MyConnectionParameterModel;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderImpl;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.tools.ParameterService;


/**
 * Helper Dialog to manually test the {@link InjectParametersAction} Dialog
 */
public class ParameterInjectionDialogExample extends JDialog {

	public static final String TESTTYPE = "TESTTYPE";
	public static final int VP_AMOUNT = 6;

	public static void main(String[] args) {
		ParameterService.init();
		ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_FONT_CONFIG, "Standard fonts");
		try {
			UIManager.setLookAndFeel(new RapidLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		JDialog dialog = new ParameterInjectionDialogExample();
		dialog.setVisible(true);
	}

	private ParameterInjectionDialogExample() {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(new Dimension(300, 300));
		setLayout(new BorderLayout());
		add(new JButton(new InjectParametersAction(this, TESTTYPE, this::setupParameters, setupValueProviders(), this::save)), BorderLayout.CENTER);
	}

	private void save(List<ConnectionParameterModel> connectionParameterModels) {
		try {
			System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(connectionParameterModels));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	private List<ConnectionParameterModel> setupParameters() {
		List<ConnectionParameterModel> list = new ArrayList<>();
		for (int i = 1; i < 23; i++) {
			list.add(new MyConnectionParameterModel(null, "group " + i, "parameter " + i, "empty", false, "ValPro " + i % VP_AMOUNT, (i % 4) > 0));
		}
		return list;
	}

	private List<ValueProvider> setupValueProviders() {
		List<ValueProvider> list = new ArrayList<>();
		for (int i = 1; i < VP_AMOUNT; i++) {
			list.add(new ValueProviderImpl("ValPro " + i, TESTTYPE));
		}
		return list;
	}

}
