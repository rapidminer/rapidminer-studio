/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
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
package com.rapidminer.template.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.Transient;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.ui.EditorPaneUI;
import com.rapidminer.gui.processeditor.ExtendedProcessEditor;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ListHoverHelper;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.ButtonDecotrator;
import com.rapidminer.gui.tools.components.LinkButton;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.template.Step;
import com.rapidminer.template.Template;
import com.rapidminer.template.TemplateController;
import com.rapidminer.template.TemplateManager;
import com.rapidminer.template.TemplateState;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.tools.update.internal.UpdateManagerRegistry;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * Tabbed view to allow the user to walk through a {@link Template}. A card layout is used to
 * display the individual steps. All logic is delegated to a {@link TemplateController}.
 *
 * @author Simon Fischer
 *
 */
public class TemplateView extends JPanel implements Dockable {

	private static final long serialVersionUID = 1L;

	private Component dataTab;
	private Component resultsTab;
	private CardLayout cardLayout = new CardLayout();
	private JPanel cards = new JPanel(cardLayout);
	private TemplateController controller = new TemplateController();;

	public TemplateView(MainFrame mainFrame) {
		setLayout(new BorderLayout(0, 15));
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Navigation on top
		JPanel navigationBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
		navigationBar.setBackground(Color.WHITE);
		Component templateTab = makeNavigationButton("template");
		navigationBar.add(templateTab);
		dataTab = makeNavigationButton("data");
		dataTab.setEnabled(false);
		navigationBar.add(dataTab);
		resultsTab = makeNavigationButton("results");
		resultsTab.setEnabled(false);
		navigationBar.add(resultsTab);

		final JLabel titleLabel = new JLabel(" ");
		titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 0));
		titleLabel.setFont(titleLabel.getFont().deriveFont(18f).deriveFont(Font.BOLD));
		controller.getModel().addObserver(new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				if (TemplateState.OBSERVER_EVENT_TEMPLATE.equals(arg)) {
					Template template = controller.getModel().getTemplate();
					if (template == null) {
						titleLabel.setText(" ");
						titleLabel.setIcon(null);
					} else {
						titleLabel.setText(template.getTitle());
					}
				}
			}
		});
		navigationBar.add(titleLabel);
		add(navigationBar, BorderLayout.NORTH);

		// Step 1: Template Selection
		JPanel step1Panel = new JPanel(new GridBagLayout());
		// JPanel step1Panel = new JPanel(new BorderLayout(10, 0));
		step1Panel.setBackground(Color.WHITE);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;

		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.ipadx = 10;
		c.insets = new Insets(0, 10, 0, 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = GridBagConstraints.REMAINDER;

		// Step 1
		// template list
		final JList<Template> templatesList = new JList<>(TemplateManager.getInstance().getAllTemplates()
				.toArray(new Template[TemplateManager.getInstance().getAllTemplates().size()]));
		templatesList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		templatesList.setVisibleRowCount(2);
		templatesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		templatesList.setCellRenderer(new TemplateCellRenderer());

		JPanel listWrapper = new JPanel(new GridBagLayout());
		listWrapper.add(templatesList);
		listWrapper.setOpaque(false);

		JScrollPane listScrollPane = new ExtendedJScrollPane(listWrapper);
		listScrollPane.getViewport().setOpaque(false);
		listScrollPane.setOpaque(false);
		listScrollPane.setBorder(BorderFactory.createCompoundBorder(
				new RoundTitledBorder(1, I18N.getMessage(I18N.getGUIBundle(), "gui.label.template.select_template.label")),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		step1Panel.add(listScrollPane, c);

		templatesList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				// only act on left-click
				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				Template selectedTemplate = templatesList.getSelectedValue();
				if (selectedTemplate == null) {
					// deselecting a selected template
					return;
				}
				if (selectedTemplate.getRequiredExtensions() != null && selectedTemplate.getRequiredExtensions().length > 0) {
					final List<String> missingExtensions = new ArrayList<>();
					for (String extensionId : selectedTemplate.getRequiredExtensions()) {
						Plugin ext = Plugin.getPluginByExtensionId(extensionId);
						if (ext == null) {
							missingExtensions.add(extensionId);
						}
					}

					if (!missingExtensions.isEmpty()) {
						int confirm = SwingTools.showConfirmDialog(
								"template.install_missing_extensions",
								ConfirmDialog.YES_NO_OPTION,
								selectedTemplate.getRequiredExtensionNames() != null ? selectedTemplate
										.getRequiredExtensionNames() : missingExtensions.toString());
						if (confirm == ConfirmDialog.YES_OPTION) {
							try {
								UpdateManagerRegistry.INSTANCE.get().installSelectedPackages(missingExtensions);
							} catch (URISyntaxException | IOException e1) {
								SwingTools.showSimpleErrorMessage("Unable to connect to the RapidMiner Marketplace.", e1);
							}
						}
						templatesList.getSelectionModel().clearSelection();
						return;
					}
				}

				controller.setTemplate(selectedTemplate);
				showCard("data");
			}
		});
		ListHoverHelper.install(templatesList);
		controller.getModel().addObserver(new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				if (TemplateState.OBSERVER_EVENT_TEMPLATE.equals(arg)) {
					Template template = controller.getModel().getTemplate();
					if (template != null) {
						dataTab.setEnabled(true);
					} else {
						dataTab.setEnabled(false);
					}
				} else if (TemplateState.OBSERVER_EVENT_RESULTS.equals(arg)) {
					boolean isResultsAvailable = controller.getModel().getResults() != null;
					if (isResultsAvailable) {
						showCard("results");
					}
					resultsTab.setEnabled(isResultsAvailable);
				}
			}
		});
		cards.add(step1Panel, "template");

		// Step 2: Data
		JPanel step2Panel = new JPanel(new GridBagLayout());
		step2Panel.setBackground(Color.WHITE);
		// Help view
		c.gridwidth = GridBagConstraints.RELATIVE;
		ExtendedHTMLJEditorPane dataHelpViewer = makeHelpTextViewer("-", Step.DATA);

		// Data area
		c = new GridBagConstraints();
		c.weightx = 0;
		c.weighty = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.ipadx = 10;
		c.insets = new Insets(0, 10, 0, 10);
		c.fill = GridBagConstraints.BOTH;

		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.RELATIVE;
		step2Panel.add(new InputDataArea(controller), c);
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		step2Panel.add(makeWestScrollPane(dataHelpViewer), c);
		cards.add(step2Panel, "data");

		// Step 3: Results
		JPanel step3Panel = new JPanel(new GridBagLayout());
		step3Panel.setBackground(Color.WHITE);
		c = new GridBagConstraints();
		c.weightx = 0;
		c.weighty = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.ipadx = 10;
		c.insets = new Insets(0, 0, 0, 10);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		ExtendedHTMLJEditorPane resultsHelpViewer = makeHelpTextViewer("-", Step.RESULTS);
		JButton showProcess = new JButton(new ResourceAction(true, "template.show_process") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				showProcess(controller, null);
			}
		});
		ButtonDecotrator.decorateAsLinkButton(showProcess);
		JButton exportHTML = new JButton(new ExportAsHtmlAction(controller));
		ButtonDecotrator.decorateAsLinkButton(exportHTML);
		JComponent buttonBox = new Box(BoxLayout.Y_AXIS);
		buttonBox.add(showProcess);
		buttonBox.add(exportHTML);
		JComponent resultsHelpPane = makeWestScrollPane(resultsHelpViewer, buttonBox);
		step3Panel.add(resultsHelpPane, c);

		JScrollPane resultsPanel = new ExtendedJScrollPane(new ResultsDashboard(controller));
		resultsPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		resultsPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		resultsPanel.setBackground(Color.WHITE);
		resultsPanel.getViewport().setBackground(Color.WHITE);
		resultsPanel.setBorder(new RoundTitledBorder(6, I18N.getMessage(I18N.getGUIBundle(),
				"gui.label.template.analytical_results.label")));

		c.weightx = 1;
		c.gridwidth = GridBagConstraints.RELATIVE;
		step3Panel.add(resultsPanel, c);
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		step3Panel.add(resultsHelpPane, c);
		cards.add(step3Panel, "results");

		cardLayout.show(cards, "template");
		add(cards, BorderLayout.CENTER);

		mainFrame.addExtendedProcessEditor(new ExtendedProcessEditor() {

			@Override
			public void setSelection(List<Operator> selection) {}

			@Override
			public void processUpdated(Process process) {}

			@Override
			public void processChanged(Process process) {
				controller.getModel().setProcessOpened(false);
			}

			@Override
			public void processViewChanged(Process process) {}
		});

		// prevent vldocking popup menu
		setComponentPopupMenu(null);
		setInheritsPopupMenu(false);

		showCard("template");
	}

	private JComponent makeWestScrollPane(JComponent resultsHelpViewer) {
		return makeWestScrollPane(resultsHelpViewer, null);
	}

	private JComponent makeWestScrollPane(final JComponent dataHelpViewer, final JComponent bottomComponent) {
		JPanel panel = new JPanel(new BorderLayout()); // GridBagLayout());
		ExtendedJScrollPane dataHelpScrollPane = new ExtendedJScrollPane(dataHelpViewer) {

			private static final long serialVersionUID = 1L;

			@Override
			@Transient
			public Dimension getMinimumSize() {
				return new Dimension(320, 400);
			}

			@Override
			@Transient
			public Dimension getPreferredSize() {
				return new Dimension(320, 800);
			}

			@Override
			@Transient
			public Dimension getMaximumSize() {
				return new Dimension(320, 2000);
			}
		};
		dataHelpScrollPane.setBackground(Color.WHITE);
		dataHelpScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		panel.add(dataHelpScrollPane, BorderLayout.CENTER);

		if (bottomComponent != null) {
			bottomComponent.setBackground(Color.WHITE);
			panel.add(bottomComponent, BorderLayout.SOUTH);
		}

		panel.setBackground(Color.WHITE);
		panel.setBorder(BorderFactory.createEmptyBorder());

		return panel;
	}

	private ExtendedHTMLJEditorPane makeHelpTextViewer(String initialMessage, Step forStep) {
		TemplateHelpView view = new TemplateHelpView(controller.getModel(), initialMessage, forStep);
		// dirty hack to remove the ugly cut/copy/paste popup which is added in the earliest
		// possible place, the EditorPaneUI look&feel
		// implementation..
		MouseListener toRemove = null;
		for (MouseListener l : view.getMouseListeners()) {
			if (EditorPaneUI.class.getPackage().equals(l.getClass().getPackage())) {
				toRemove = l;
			}
		}
		view.removeMouseListener(toRemove);
		return view;
	}

	private HashMap<String, LinkButton> navButtons = new HashMap<>();

	private void showCard(String cardName) {
		cardLayout.show(cards, cardName);
		for (LinkButton b : navButtons.values()) {
			b.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.WHITE));
			b.setMargin(new Insets(0, 0, 4, 20));
		}
		navButtons.get(cardName).setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.DARK_GRAY));
	}

	private Component makeNavigationButton(final String key) {
		final LinkButton linkButton = new LinkButton(new ResourceActionAdapter("template.navstep." + key) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				showCard(key);
			}
		}, true) {

			private static final long serialVersionUID = 1L;

			{
				StyleSheet css = ((HTMLEditorKit) getEditorKit()).getStyleSheet();
				css.addRule("a {text-decoration:none; color:#666666; }");
			}
		};
		if (navButtons.isEmpty()) {
			// first? Mark
			linkButton.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.DARK_GRAY));
		}
		navButtons.put(key, linkButton);
		return linkButton;
	}

	/**
	 * @param postOpenTask
	 *            to be executed after open on EDT.
	 */
	public static void showProcess(final TemplateController controller, final Runnable postOpenTask) {
		new ProgressThread("template.saving_project") {

			@Override
			public void run() {
				final Process process;
				try {
					process = controller.save(getProgressListener());
				} catch (MalformedRepositoryLocationException | RepositoryException | IOException | XMLException e) {
					SwingTools.showSimpleErrorMessage("template.failed_to_save_project", e);
					return;
				}
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						RapidMinerGUI.getMainFrame().setProcess(process, true);
						controller.getModel().setProcessOpened(true);
						if (controller.getModel().getTemplate() != null) {
							ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_TEMPLATE,
									controller.getModel().getTemplate().getName(), "opened_process");
						}
						if (postOpenTask != null) {
							postOpenTask.run();
						}
					}
				});
			}
		}.start();
	}

	public static final String TEMPLATES_DOCK_KEY = "templates";
	private final DockKey DOCK_KEY = new ResourceDockKey(TEMPLATES_DOCK_KEY);

	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
		DOCK_KEY.setCloseEnabled(false);
		DOCK_KEY.setFloatEnabled(false);
		DOCK_KEY.setAutoHideEnabled(false);
		DOCK_KEY.setMaximizeEnabled(false);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}
}
