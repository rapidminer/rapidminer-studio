/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.OperatorDocumentationBrowser;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.FixedWidthLabel;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;


/**
 * An info screen for operators. Shows all important meta data about an operator like name, group,
 * expected input and delivered output. In case of an operator chain the desired numbers of inner
 * operators are also shown.
 *
 * @author Ingo Mierswa, Tobias Malbrecht, Sebastian Land
 */
public class OperatorInfoScreen extends ButtonDialog {

	private static final long serialVersionUID = -6566133238783779634L;

	private final transient Operator operator;

	public OperatorInfoScreen(Operator operator) {
		// TODO: externalize strings and icon names
		super(ApplicationFrame.getApplicationFrame(), "operator_info", ModalityType.APPLICATION_MODAL, new Object[] {});
		this.operator = operator;
		setTitle(getTitle());  // must be executed after setting member field

		JTabbedPane tabs = new JTabbedPane();

		OperatorDocumentationBrowser browser = new OperatorDocumentationBrowser();
		browser.setDisplayedOperator(operator);

		final JPanel overviewPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0;
		c.insets = new Insets(0, 0, 0, 0);

		if (operator.getOperatorDescription().isDeprecated()) {
			final JPanel deprecatedPanel = new JPanel(new BorderLayout());
			final JLabel label = new JLabel(SwingTools.createIcon("24/sign_warning.png"));
			label.setHorizontalTextPosition(SwingConstants.CENTER);
			label.setVerticalTextPosition(SwingConstants.BOTTOM);
			label.setText("<html><b>Depreceated!</b></html>");
			label.setPreferredSize(new Dimension(180, 50));
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 40));
			deprecatedPanel.add(label, BorderLayout.WEST);
			deprecatedPanel.add(createDeprecationInfoPanel(operator), BorderLayout.CENTER);
			deprecatedPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
					BorderFactory.createEmptyBorder(4, 4, 4, 4)));
			overviewPanel.add(deprecatedPanel, c);
		}

		if (operator instanceof CapabilityProvider) {
			JPanel learnerPanel = new JPanel(new BorderLayout());
			JLabel label = new JLabel(SwingTools.createIcon("24/briefcase2.png"));
			label.setHorizontalTextPosition(SwingConstants.CENTER);
			label.setVerticalTextPosition(SwingConstants.BOTTOM);
			label.setText("Capabilities");
			label.setPreferredSize(new Dimension(180, 50));
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 40));
			learnerPanel.add(label, BorderLayout.WEST);

			learnerPanel.add(createCapabilitiesPanel(operator), BorderLayout.CENTER);
			learnerPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
					BorderFactory.createEmptyBorder(4, 4, 4, 4)));

			overviewPanel.add(learnerPanel, c);
		}

		// ports
		if (operator.getInputPorts().getNumberOfPorts() > 0 || operator.getOutputPorts().getNumberOfPorts() > 0) {
			JPanel portPanel = new JPanel(new BorderLayout());
			JLabel label = new JLabel(SwingTools.createIcon("24/plug.png"));
			label.setHorizontalTextPosition(SwingConstants.CENTER);
			label.setVerticalTextPosition(SwingConstants.BOTTOM);
			label.setText("Ports");
			label.setPreferredSize(new Dimension(180, 50));
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 40));
			portPanel.add(label, BorderLayout.WEST);

			portPanel.add(
					createPortsDescriptionPanel("input_ports", "output_ports", operator.getInputPorts(),
							operator.getOutputPorts()), BorderLayout.CENTER);
			portPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
					BorderFactory.createEmptyBorder(4, 4, 4, 4)));

			overviewPanel.add(portPanel, c);
		}

		if (operator instanceof OperatorChain) {
			OperatorChain chain = (OperatorChain) operator;

			for (ExecutionUnit subprocess : chain.getSubprocesses()) {
				JPanel subprocessPanel = new JPanel(new BorderLayout());
				JLabel label = new JLabel(SwingTools.createIcon("24/elements_tree.png"));
				label.setHorizontalTextPosition(SwingConstants.CENTER);
				label.setVerticalTextPosition(SwingConstants.BOTTOM);
				label.setText(subprocess.getName());
				label.setPreferredSize(new Dimension(180, 50));
				label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 40));
				subprocessPanel.add(label, BorderLayout.WEST);

				subprocessPanel.add(
						createPortsDescriptionPanel("inner_sources", "inner_sinks", subprocess.getInnerSources(),
								subprocess.getInnerSinks()), BorderLayout.CENTER);
				subprocessPanel.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
						BorderFactory.createEmptyBorder(4, 4, 4, 4)));

				overviewPanel.add(subprocessPanel, c);
			}
		}

		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		overviewPanel.add(new JPanel(new BorderLayout()), c);
		final JScrollPane overviewPane = new ExtendedJScrollPane(overviewPanel);
		overviewPane.setBorder(null);
		overviewPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		overviewPane.getViewport().addComponentListener(new ComponentListener() {

			@Override
			public void componentHidden(ComponentEvent e) {}

			@Override
			public void componentMoved(ComponentEvent e) {}

			@Override
			public void componentResized(ComponentEvent e) {
				// transfer width to contained panel
				overviewPanel.setPreferredSize(new Dimension((int) overviewPane.getViewport().getExtentSize().getWidth(),
						(int) overviewPanel.getPreferredSize().getHeight()));
			}

			@Override
			public void componentShown(ComponentEvent e) {}

		});
		tabs.add("Overview", overviewPane);
		tabs.add("Description", browser);
		layoutDefault(tabs, NORMAL, makeCloseButton());
	}

	@Override
	protected Icon getInfoIcon() {
		return operator.getOperatorDescription().getLargeIcon();
	}

	@Override
	protected String getInfoText() {
		return "<html><b>"
				+ operator.getOperatorDescription().getName()
				+ "</b>"
				+ (operator.getOperatorDescription().getGroup().equals("") ? "" : "<br/>Group: "
						+ operator.getOperatorDescription().getGroupName()) + "</html>";
	}

	@Override
	public String getTitle() {
		if (operator != null) {
			return super.getTitle() + ": " + operator.getOperatorDescription().getName();
		}
		return super.getTitle();
	}

	public static JPanel createPortsDescriptionPanel(String inKey, String outKey, Ports<? extends Port> inputPorts,
			Ports<? extends Port> outputPorts) {
		int numberOfInputPorts = inputPorts.getNumberOfPorts();
		int numberOfOutputPorts = outputPorts.getNumberOfPorts();
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(GAP / 2, 0, GAP / 2, 0);
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.weighty = 1;

		final JPanel panel = new JPanel(layout);
		final Icon inIcon;
		final Icon outIcon;
		{
			JPanel rowPanel = new JPanel(new GridLayout(1, 2));
			JLabel label = new ResourceLabel(inKey);
			label.setText("<html><i>" + label.getText() + "</i></html>");
			inIcon = label.getIcon();
			label.setIcon(null);
			rowPanel.add(label);
			label = new ResourceLabel(outKey);
			label.setText("<html><i>" + label.getText() + "</i></html>");
			outIcon = label.getIcon();
			label.setIcon(null);
			rowPanel.add(label);
			panel.add(rowPanel, c);
		}
		final LinkedList<FixedWidthLabel> labels = new LinkedList<FixedWidthLabel>();
		for (int i = 0; i < Math.max(numberOfInputPorts, numberOfOutputPorts); i++) {
			JPanel rowPanel = new JPanel(new GridLayout(1, 2));
			if (i < numberOfInputPorts) {
				Port port = inputPorts.getPortByIndex(i);
				FixedWidthLabel label = new FixedWidthLabel(rowPanel.getWidth() / 2, port.getName());
				label.setIcon(inIcon);
				label.setText(// (numberOfInputPorts > 1 ? ("<em>" + (i + 1) + ":</em> ") : "") +
				port.getName() + (port.getDescription().equals("") ? "" : " (" + port.getDescription() + ")"));
				labels.add(label);
				rowPanel.add(label);
			} else {
				rowPanel.add(new JLabel());
			}
			if (i < numberOfOutputPorts) {
				Port port = outputPorts.getPortByIndex(i);
				FixedWidthLabel label = new FixedWidthLabel(rowPanel.getWidth() / 2, port.getName());
				label.setIcon(outIcon);
				label.setText(// (numberOfOutputPorts > 1 ? ("<em>" + (i + 1) + ":</em> ") : "") +
				port.getName() + (port.getDescription().equals("") ? "" : " (" + port.getDescription() + ")"));
				labels.add(label);
				rowPanel.add(label);
			} else {
				rowPanel.add(new JLabel());
			}
			panel.add(rowPanel, c);
		}
		panel.addComponentListener(new ComponentListener() {

			@Override
			public void componentHidden(ComponentEvent e) {}

			@Override
			public void componentMoved(ComponentEvent e) {}

			@Override
			public void componentResized(ComponentEvent e) {
				// transfer to labels in panel
				for (FixedWidthLabel label : labels) {
					label.setWidth((int) (panel.getWidth() / 2.2));
				}
			}

			@Override
			public void componentShown(ComponentEvent e) {}
		});
		return panel;
	}

	public static JPanel createDeprecationInfoPanel(Operator operator) {
		final JPanel panel = new JPanel(new BorderLayout());
		final FixedWidthLabel info = new FixedWidthLabel(200, operator.getOperatorDescription().getDeprecationInfo());
		panel.add(info, BorderLayout.CENTER);
		panel.addComponentListener(new ComponentListener() {

			@Override
			public void componentHidden(ComponentEvent e) {}

			@Override
			public void componentMoved(ComponentEvent e) {}

			@Override
			public void componentResized(ComponentEvent e) {
				info.setWidth(panel.getWidth());
			}

			@Override
			public void componentShown(ComponentEvent e) {}
		});
		return panel;
	}

	public static JPanel createCapabilitiesPanel(Operator operator) {
		CapabilityProvider capabilityProvider = (CapabilityProvider) operator;
		int length = OperatorCapability.values().length;
		GridLayout layout = new GridLayout(length / 2, 2);
		layout.setHgap(GAP);
		layout.setVgap(GAP);
		JPanel capabilitiesPanel = new JPanel(layout);
		for (OperatorCapability capability : OperatorCapability.values()) {
			JLabel capabilityLabel = new JLabel(capability.getDescription());
			try {
				if (capabilityProvider.supportsCapability(capability)) {
					capabilityLabel.setIcon(SwingTools.createIcon("16/ok.png"));
				} else {
					capabilityLabel.setIcon(SwingTools.createIcon("16/error.png"));
				}
			} catch (Exception e) {
				break;
			}
			capabilitiesPanel.add(capabilityLabel);
		}
		return capabilitiesPanel;
	}

}
