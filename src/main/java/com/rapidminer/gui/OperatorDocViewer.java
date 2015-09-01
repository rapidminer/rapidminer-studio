/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.rapidminer.Process;
import com.rapidminer.gui.actions.RefreshHelpTextFromWikiAction;
import com.rapidminer.gui.actions.ShowHelpTextAction;
import com.rapidminer.gui.actions.ShowHelpTextInBrowserAction;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.NetTools;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.documentation.ExampleProcess;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * Displays HTML help text for an operator
 *
 * @author Simon Fischer
 *
 */
public class OperatorDocViewer extends JPanel implements Dockable, ProcessEditor {

	private static final long serialVersionUID = 1L;

	static {
		NetTools.init();
	}

	private final ExtendedHTMLJEditorPane editor = new ExtendedHTMLJEditorPane("text/html", "<html></html>") {

		private static final long serialVersionUID = 1L;

		@Override
		public void installDefaultStylesheet() {
			HTMLEditorKit hed = new HTMLEditorKit();
			StyleSheet css = hed.getStyleSheet();
			css.addRule("body {font-family:Sans;font-size:12pt}");
			css.addRule("h4 {margin-bottom:2px; margin-top:2ex; padding-left:4px; padding:0; color:#446699; font-size:16pt}");
			css.addRule("p {margin-top:0; margin-bottom:2ex; padding:0}");
			css.addRule("dt {font-weight:bold;}");
			css.addRule("ul.ports {margin-top:0; margin-bottom:1ex; list-style-image:url("
					+ Tools.getResource("icons/help/circle.png") + "); }");
			css.addRule("ul li {padding-bottom:1ex}");
			css.addRule("ul.param_dep {margin-top:0; margin-bottom:1ex; list-style-type:none; list-style-image:none; }");
			css.addRule("li ul li {padding-bottom:0}");
			css.addRule("hr {color:red; background-color:red}");
			css.addRule("table {font-style:italic;}");

			Document doc = hed.createDefaultDocument();
			editor.setDocument(doc);

		};
	};

	private Operator displayedOperator;

	private String displayedOperatorDescName;

	public String getDisplayedOperatorDescName() {
		return displayedOperatorDescName;
	}

	public OperatorDocViewer() {
		super();

		setLayout(new BorderLayout());
		scrollPane = new ExtendedJScrollPane(editor);
		scrollPane.setBorder(null);

		setSelection(Collections.<Operator> emptyList());
		editor.installDefaultStylesheet();
		editor.setEditable(false);

		addToolBar();

		add(scrollPane, BorderLayout.CENTER);

		editor.addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (!RMUrlHandler.handleUrl(e.getDescription())) {
						if (e.getDescription().startsWith("show_example_")) {
							int index = Integer.parseInt(e.getDescription().substring("show_example_".length()));
							ExampleProcess example = getDisplayedOperator().getOperatorDescription()
									.getOperatorDocumentation().getExamples().get(index);
							RapidMinerGUI.getMainFrame().setProcess(example.getProcess(), true);
						}
					}
					if (e.getDescription().startsWith("/wiki/index.php?title=")) {
						String op = e.getDescription().split("=")[1];
						op = op.replaceAll(" ", "_");
						op = op.toLowerCase();
						op = op.replaceAll("\\(", "");
						op = op.replaceAll("\\)", "");
						op = op.replaceAll("-", "_");
						OperatorDescription opDesc = OperatorService.getOperatorDescription(op);
						if (opDesc != null) {
							displayedOperatorDescName = opDesc.getName();
							showDocumentation(opDesc);
						}
					}
				}
			}
		});
	}

	private void addToolBar() {
		ViewToolBar toolBar = new ViewToolBar();

		this.showHelpTextAction = new ShowHelpTextAction(this);
		JToggleButton toggleShowHelpTextButton = this.showHelpTextAction.createToggleButton();
		toolBar.add(new JButton(new ShowHelpTextInBrowserAction(true, "rapid_doc_bot_importer_showInBrowser", null, this)),
				ViewToolBar.LEFT);
		toolBar.add(new JButton(new RefreshHelpTextFromWikiAction(true, "rapid_doc_bot_importer_refresh", null, this)),
				ViewToolBar.RIGHT);
		toolBar.add(toggleShowHelpTextButton, ViewToolBar.RIGHT);

		add(toolBar, BorderLayout.NORTH);
	}

	public JEditorPane getEditor() {
		return editor;
	}

	public Operator getDisplayedOperator() {
		return displayedOperator;
	}

	public OperatorDocViewer getCurrentOperatorDocViewerObject() {
		return this;
	}

	public void setDisplayedOperator(Operator operator) {
		if (operator != null
				&& !operator.getOperatorDescription().isDeprecated()
				&& (this.displayedOperator == null || this.displayedOperator != null
				&& !operator.getOperatorDescription().getName().equals(this.displayedOperatorDescName))) {
			this.displayedOperator = operator;
			this.displayedOperatorDescName = this.displayedOperator.getOperatorDescription().getName();
			showDocumentation(operator.getOperatorDescription());
		}
	}

	private void showDocumentation(final OperatorDescription opDesc) {
		if (opDesc == null) {
			return;
		}
		if (showHelpTextAction.isSelected() && !OperatorDocLoader.hasCache(opDesc)) {
			showLoadScreen();
		}
		new SwingWorker<String, Void>() {

			@Override
			protected String doInBackground() {
				return OperatorDocLoader.loadOperatorDocumentation(showHelpTextAction.isSelected(),
						showHelpTextAction.isSelected(), opDesc);
			}

			@Override
			protected void done() {
				try {
					String toShow = get();
					JEditorPane editor = getEditor();
					editor.setText(toShow);
					editor.setCaretPosition(0);
				} catch (Exception e) {
					SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", e, true,
							new Object[] { e.getMessage() });
				}
			}
		}.execute();
	}

	protected void showHelptext() {
		editor.setEditable(false);
		if (displayedOperator == null) {
			editor.setText("<html></html>");
		} else {
			String doc = makeOperatorDocumentation(displayedOperator);
			editor.setText(doc);
			editor.setCaretPosition(0);
		}
	}

	protected static String makeOperatorDocumentation(Operator displayedOperator) {
		OperatorDescription descr = displayedOperator.getOperatorDescription();
		StringBuilder buf = new StringBuilder("<html>");
		buf.append("<table cellpadding=0 cellspacing=0><tr><td>");

		String iconName = "icons/24/" + displayedOperator.getOperatorDescription().getIconName();
		URL resource = Tools.getResource(iconName);
		if (resource != null) {
			buf.append("<img src=\"" + resource + "\"/> ");
		}

		buf.append("</td><td style=\"padding-left:4px;\">");
		buf.append("<h2>" + descr.getName());

		if (descr.getProvider() != null) {
			String operatorPluginName = descr.getProvider().getName();
			if (operatorPluginName != null && operatorPluginName.length() > 0) {
				buf.append("<small style=\"font-size:70%;color:#5F5F5F;font-weight:normal;\"> (" + operatorPluginName
						+ ")</small>");
			}
		}

		buf.append("</h2>");
		buf.append("</td></tr></table>");
		buf.append("<hr noshade=\"true\"/><br/>");
		buf.append("<h4>Synopsis</h4>");
		buf.append("<p>");
		buf.append(descr.getShortDescription());
		buf.append("</p>");
		buf.append("</p><br/>");
		buf.append("<h4>Description</h4>");
		String descriptionText = descr.getLongDescriptionHTML();
		if (descriptionText != null) {
			if (!descriptionText.trim().startsWith("<p>")) {
				buf.append("<p>");
			}
			buf.append(descriptionText);
			if (!descriptionText.trim().endsWith("</p>")) {
				buf.append("</p>");
			}
			buf.append("<br/>");
		}
		appendPortsToDocumentation(displayedOperator.getInputPorts(), "Input", null, buf);
		appendPortsToDocumentation(displayedOperator.getOutputPorts(), "Output", "outPorts", buf);
		Parameters parameters = displayedOperator.getParameters();
		if (parameters.getKeys().size() > 0) {
			buf.append("<h4>Parameters</h4><dl>");
			for (String key : parameters.getKeys()) {
				ParameterType type = parameters.getParameterType(key);
				if (type == null) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.OperatorDocViewer.unkown_parameter_key",
							new Object[] { displayedOperator.getName(), key });
					continue;
				}
				buf.append("<dt>");

				if (type.isExpert()) {
					buf.append("<i>");
				}

				buf.append(makeParameterHeader(type));

				if (type.isExpert()) {
					buf.append("</i>");
				}
				buf.append("</dt><dd style=\"padding-bottom:10px\">");
				// description
				buf.append(" ");
				buf.append(type.getDescription() + "<br/><font color=\"#777777\" size=\"-2\">");
				if (type.getDefaultValue() != null) {
					if (!type.toString(type.getDefaultValue()).equals("")) {
						buf.append(" Default value: ");
						buf.append(type.toString(type.getDefaultValue()));
						buf.append("<br/>");
					}
				}
				if (type.isExpert()) {
					buf.append("Expert parameter<br/>");
				}
				// conditions
				if (type.getDependencyConditions().size() > 0) {
					buf.append("Depends on:<ul class=\"param_dep\">");
					for (ParameterCondition condition : type.getDependencyConditions()) {
						buf.append("<li>");
						buf.append(condition.toString());
						buf.append("</li>");
					}
					buf.append("</ul>");
				}
				buf.append("</small></dd>");
			}
			buf.append("</dl>");
		}

		if (!descr.getOperatorDocumentation().getExamples().isEmpty()) {
			buf.append("<h4>Examples</h4><ul>");
			int i = 0;
			for (ExampleProcess exampleProcess : descr.getOperatorDocumentation().getExamples()) {
				buf.append("<li>");
				buf.append(exampleProcess.getComment());
				buf.append(makeExampleFooter(i));
				buf.append("</li>");
				i++;
			}
			buf.append("</ul>");
		}

		buf.append("</html>");
		return buf.toString();
	}

	protected static Object makeExampleFooter(int exampleIndex) {
		return "<br/><a href=\"show_example_" + exampleIndex + "\">Show example process</a>.";
	}

	protected String makeSynopsisHeader() {
		return "<h4>Synopsis</h4>";
	}

	protected String makeDescriptionHeader() {
		return "<h4>Description</h4>";
	}

	protected static String makeParameterHeader(ParameterType type) {
		return type.getKey().replace('_', ' ');
	}

	private static void appendPortsToDocumentation(Ports<? extends Port> ports, String title, String ulClass,
			StringBuilder buf) {
		if (ports.getNumberOfPorts() > 0) {
			buf.append("<h4>" + title + "</h4><ul class=\"ports\">");
			for (Port port : ports.getAllPorts()) {
				if (ulClass != null) {
					buf.append("<li class=\"" + ulClass + "\"><strong>");
				} else {
					buf.append("<li><strong>");
				}
				buf.append(port.getName());
				buf.append("</strong>");
				if (port.getDescription() != null && port.getDescription().length() > 0) {
					buf.append(": ");
					buf.append(port.getDescription());
				}
				buf.append("</li>");
			}
			buf.append("</ul><br/>");
		}
	}

	@Override
	public void setSelection(List<Operator> selection) {
		if (selection.isEmpty()) {
			setDisplayedOperator(null);
		} else {
			setDisplayedOperator(selection.get(0));
		}
	}

	public static final String OPERATOR_HELP_DOCK_KEY = "operator_help";
	private final DockKey DOCK_KEY = new ResourceDockKey(OPERATOR_HELP_DOCK_KEY);

	private JScrollPane scrollPane;

	private ShowHelpTextAction showHelpTextAction;

	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	@Override
	public void processChanged(Process process) {}

	@Override
	public void processUpdated(Process process) {}

	public static OperatorDocViewer instantiate() {
		return new OperatorDocViewer();
	}

	public ShowHelpTextAction getShowHelpTextAction() {
		return showHelpTextAction;
	}

	private static String LOADING_TEXT_FROM_RAPIDWIKI = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
			+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" dir=\"ltr\" lang=\"en\" xml:lang=\"en\">"
			+ "<head>"
			+ "<table cellpadding=0 cellspacing=0>"
			+ "<tr><td>"
			+ "<img src=\""
			+ SwingTools.getIconPath("48/hourglass.png")
			+ "\" /></td>"
			+ "<td width=\"5\">"
			+ "</td>"
			+ "<td>"
			+ "Please stand by while loading from RapidWiki..." + "</td></tr>" + "</table>" + "</head>" + "</html>";

	public void showLoadScreen() {
		this.editor.setText(LOADING_TEXT_FROM_RAPIDWIKI);
	}

	public void refresh() {
		if (displayedOperator != null) {
			showDocumentation(displayedOperator.getOperatorDescription());
		}
	}
}
