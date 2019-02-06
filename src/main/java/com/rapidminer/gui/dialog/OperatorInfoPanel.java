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
package com.rapidminer.gui.dialog;

import java.awt.BorderLayout;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceTabbedPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * An info panel for operators. Shows all important meta data about an operator like name, group,
 * expected input and delivered output. In case of an operator chain the desired numbers of inner
 * operators are also shown. In contrast to the info screen {@link OperatorInfoScreen} this panel
 * can not handle user comments and is mainly used for short informations like those displayed in
 * the {@link NewOperatorDialog}.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class OperatorInfoPanel extends JPanel {

	private static final long serialVersionUID = 3610550973456646750L;
	public static final Icon WARNING_ICON = SwingTools.createIcon("16/sign_warning.png");

	public OperatorInfoPanel(OperatorDescription description) {
		if (description == null) {
			add(new JLabel("No operator selected!"));
		} else {
			Operator operator = null;
			try {
				operator = description.createOperatorInstance();
			} catch (Exception e) {
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.gui.dialog.OperatorInfoPanel.creating_operator_error", e.getMessage()), e);
				throw new RuntimeException(e);
			}

			setLayout(new BorderLayout());
			JLabel label;

			label = new JLabel("<html><b>" + description.getName() + "</b><br/>Group: " + description.getGroupName()
					+ "</html>");
			label.setIcon(operator.getOperatorDescription().getIcon());
			label.setBorder(BorderFactory.createEmptyBorder(8, 16, 24, 12));
			label.setIconTextGap(12);
			add(label, BorderLayout.NORTH);

			ResourceTabbedPane tabs = new ResourceTabbedPane("operator_info_panel");
			if (description.isDeprecated()) {
				JPanel panel = new JPanel(new BorderLayout());
				panel.add(OperatorInfoScreen.createDeprecationInfoPanel(operator), BorderLayout.NORTH);
				panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
				tabs.addTabI18N("deprecation", panel);
			}

			JEditorPane descriptionLabel = new ExtendedHTMLJEditorPane("text/html", "");
			StyleSheet css = ((HTMLEditorKit) descriptionLabel.getEditorKit()).getStyleSheet();
			css.addRule("p { margin : 0pt; font-family : sans-serif; font-size : 9px; font-style : normal; }");
			descriptionLabel.setToolTipText("The description of this operator");
			descriptionLabel.setEditable(false);
			descriptionLabel.setBackground(this.getBackground());
			String descriptionString = operator.getOperatorDescription().getLongDescriptionHTML();
			descriptionLabel.setText("<p>" + descriptionString + "</p>");
			descriptionLabel.setCaretPosition(0);
			JScrollPane spD = new ExtendedJScrollPane(descriptionLabel);
			spD.setBorder(null);
			tabs.addTabI18N("description", spD);

			if (operator instanceof CapabilityProvider) {
				JPanel panel = new JPanel(new BorderLayout());
				panel.add(OperatorInfoScreen.createCapabilitiesPanel(operator), BorderLayout.NORTH);
				panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
				JScrollPane spC = new ExtendedJScrollPane(panel);
				spC.setBorder(null);
				tabs.addTabI18N("capabilities", spC);
			}

			if (operator.getInputPorts().getNumberOfPorts() > 0 || operator.getOutputPorts().getNumberOfPorts() > 0) {
				JPanel panel = new JPanel(new BorderLayout());
				panel.add(
						OperatorInfoScreen.createPortsDescriptionPanel("input_ports", "output_ports",
								operator.getInputPorts(), operator.getOutputPorts()), BorderLayout.NORTH);
				panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
				JScrollPane spP = new ExtendedJScrollPane(panel);
				spP.setBorder(null);
				tabs.addTabI18N("ports", spP);
			}

			if (operator instanceof OperatorChain) {
				OperatorChain chain = (OperatorChain) operator;

				for (ExecutionUnit subprocess : chain.getSubprocesses()) {
					JPanel panel = new JPanel(new BorderLayout());
					panel.add(
							OperatorInfoScreen.createPortsDescriptionPanel("inner_sources", "inner_sinks",
									subprocess.getInnerSources(), subprocess.getInnerSinks()), BorderLayout.NORTH);
					panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
					JScrollPane spC = new ExtendedJScrollPane(panel);
					spC.setBorder(null);
					tabs.addTabI18N("subprocess", spC, subprocess.getName());
				}
			}

			add(tabs, BorderLayout.CENTER);
		}
	}
}
