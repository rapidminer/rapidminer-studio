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
package com.rapidminer.gui.properties;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeSuggestion;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.SuggestionProvider;
import com.rapidminer.tools.ParameterService;


/**
 * Quick testing possibility for the updated {@link com.rapidminer.gui.properties.celleditors.value.AbstractSuggestionBoxValueCellEditor AbstractSuggestionBoxValueCellEditor}.
 * Shows a simple frame that contains two {@link ParameterTypeSuggestion} parameters.
 * One {@link SuggestionProvider} that is used has a 2 second delay to show the loading gif and how the popup size changes.
 * The other one caches its results and can be reset through the "<strong>T</strong>est-><strong>R</strong>eset" menu.
 *
 * @author Jan Czogalla
 * @since 9.2.0
 */
public class SuggestionComboBoxTestField {

	@SuppressWarnings("squid:S2925")
	public static void main(String[] args) {
		ParameterService.init();
		ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_FONT_CONFIG, "Standard fonts");
		SuggestionProvider<String> slowProvider = (op, pl) -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// ignore
			}
			return Arrays.asList("a", "b", "c");
		};
		List<ParameterType> parameterTypes = new ArrayList<>();
		parameterTypes.add(new ParameterTypeSuggestion("reloading_parameter",
				"A simple slow loading suggestion list that loads every time",
				slowProvider));
		List<String> cache = new ArrayList<>();
		parameterTypes.add(new ParameterTypeSuggestion("caching_parameter",
				"A simple slow loading suggestion list that caches its results",
				(SuggestionProvider<String> ) (op, pl) -> {
					if (cache.isEmpty()) {
						cache.addAll(slowProvider.getSuggestions(op, pl));
					}
					return cache;
				})
		);
		Parameters parameters = new Parameters(parameterTypes);

		JFrame frame = new JFrame("Test suggestion combo box");
		frame.add(new GenericParameterPanel(parameters));
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Test");
		menu.setMnemonic('T');
		JMenuItem reset = menu.add(new AbstractAction("Reset") {
			@Override
			public void actionPerformed(ActionEvent e) {
				cache.clear();
			}
		});
		reset.setMnemonic('R');
		menuBar.add(menu);
		frame.setJMenuBar(menuBar);

		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(300, 200));
		frame.pack();
		frame.setVisible(true);
	}
}
