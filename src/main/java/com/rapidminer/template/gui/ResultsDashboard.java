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
package com.rapidminer.template.gui;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.Perspectives;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.plotter.PlotterConfigurationSettings;
import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.renderer.AbstractGraphRenderer;
import com.rapidminer.gui.renderer.Renderer;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkButton;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.gui.tour.BubbleWindow;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tour.ComponentBubbleWindow;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.report.Renderable;
import com.rapidminer.report.Reportable;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.template.TemplateController;
import com.rapidminer.template.TemplateState;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;


/**
 * Displays the results of the process in a 2x2 grid.
 * 
 * @author Simon Fischer
 * 
 */
public class ResultsDashboard extends JPanel implements PrintableComponent {

	private static final int CELL_HEIGHT = 350;

	private static final int CELL_WIDTH = 250;

	private static final long serialVersionUID = 1L;

	/** Client property used in the component wrappers for the bubble title. */
	private static final Object CLIENT_PROPERTY_TITLE = "tempalte.title";

	/** Client property used in the component wrappers for the bubble main text. */
	private static final Object CLIENT_PROPERTY_MAIN_TEXT = "tempalte.main_text";

	/** Client property used to specify the alignment of the bubble. */
	private static final Object CLIENT_PROPERTY_BUBBLE_LOCATION = "template.bubble_location";

	/** Feature flag to disable tooltip windows. */
	private static final boolean USE_TOOLTIPS = false;

	private final TemplateController controller;

	public ResultsDashboard(final TemplateController controller) {
		super(new GridBagLayout());
		this.controller = controller;
		setBackground(Color.WHITE);
		getState().addObserver(new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				if (TemplateState.OBSERVER_EVENT_RESULTS.equals(arg) && getState().getResults() != null) {
					showResults();
				}
			}
		});
	}

	@Override
	public Component getExportComponent() {
		return this;
	}

	@Override
	public String getExportName() {
		return controller.getModel().getTemplate().getTitle();
	}

	@Override
	public String getIdentifier() {
		return null;
	}

	@Override
	public String getExportIconName() {
		return I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label.templates.icon");
	}

	private void showResults() {
		removeAll();
		IOObject[] results = getState().getResults();
		if (results != null) {
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(20, 20, 20, 20);
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 1;
			int col = 0;
			List<JComponent> allWrappers = new ArrayList<>();
			for (Properties props : getState().getTemplate().getResultPlotterSettings()) {
				col++;
				boolean multicolumn = props.containsKey("colspan");
				if ((col >= 2) || multicolumn) {
					c.gridwidth = GridBagConstraints.REMAINDER;
					col = 0;
				} else {
					c.gridwidth = GridBagConstraints.RELATIVE;
				}
				JComponent wrapper = makeReport(results, props);
				if (hasBubble(wrapper)) {
					allWrappers.add(wrapper);
				}
				if (multicolumn) {
					wrapper.putClientProperty(CLIENT_PROPERTY_BUBBLE_LOCATION, BubbleWindow.AlignedSide.BOTTOM);
				} else {
					switch (col) {
						case 1:
							wrapper.putClientProperty(CLIENT_PROPERTY_BUBBLE_LOCATION, BubbleWindow.AlignedSide.RIGHT);
							break;
						case 0:
							wrapper.putClientProperty(CLIENT_PROPERTY_BUBBLE_LOCATION, BubbleWindow.AlignedSide.LEFT);
							break;
						default:
							wrapper.putClientProperty(CLIENT_PROPERTY_BUBBLE_LOCATION, BubbleWindow.AlignedSide.BOTTOM);
							break;
					}
				}
				add(wrapper, c);
			}
			addBubbles(allWrappers);
		}
	}

	private JComponent makeReport(IOObject[] results, Properties resultPlotterSettings) {
		String indexString = getResultProperty(resultPlotterSettings, TemplateController.RESULT_PROPERTY_INDEX);
		final int index;
		try {
			index = Integer.parseInt(indexString);
		} catch (NumberFormatException e) {
			return new JLabel("Illegal result index: " + indexString);
		}
		if (index > results.length) {
			return new JLabel("Illegal result index: " + index);
		}
		final IOObject ioo = results[index - 1];
		String reportableType = RendererService.getName(ioo.getClass());
		String rendererName = getResultProperty(resultPlotterSettings, TemplateController.RESULTS_PROPERTY_RENDERER);

		// special case: renderer_name=Text. If that is the case, look at the format specified by
		// text=
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(Color.WHITE);
		final String title = getResultProperty(resultPlotterSettings, TemplateController.RESULT_PROPERTY_TITLE);
		if (title != null) {
			JLabel label = new JLabel("<html><strong>" + title + "</strong></html>");
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
			label.setBackground(Color.WHITE);
			label.setOpaque(false);
			wrapper.add(label, BorderLayout.NORTH);
		}
		String description = getResultProperty(resultPlotterSettings, TemplateController.RESULT_PROPERTY_DESCRIPTION);
		if (description != null) {
			JLabel label = new JLabel("<html><div>" + description + "</div></html>") {

				private static final long serialVersionUID = 1L;

				@Override
				public Dimension getPreferredSize() {
					return new Dimension(CELL_WIDTH, 40);
				}

				@Override
				public Dimension getMinimumSize() {
					return new Dimension(150, 10);
				}

				@Override
				public Dimension getMaximumSize() {
					return new Dimension(CELL_WIDTH, CELL_HEIGHT);
				}
			};
			label.setBackground(Color.WHITE);
			label.setForeground(Color.GRAY);
			label.setOpaque(false);
			wrapper.add(label, BorderLayout.SOUTH);
		}
		final String tooltip = getResultProperty(resultPlotterSettings, TemplateController.RESULT_PROPERTY_TOOLTIP);
		String linkedOperator = getResultProperty(resultPlotterSettings, TemplateController.RESULT_PROPERTY_LINKED_OPERATOR);
		if (TemplateController.RESULT_RENDERER_NAME_TEXT.equals(rendererName)) {
			String html = "<html><div style=\"max-width:" + (CELL_WIDTH - 10) + "px;\">"
					+ getResultProperty(resultPlotterSettings, "text") + "</div></html>";
			JComponent textComponent;
			boolean scrollable = resultPlotterSettings.containsKey("scrollable");
			if (scrollable) {
				ExtendedHTMLJEditorPane pane = new ExtendedHTMLJEditorPane("text/html", html);
				JScrollPane scrollPane = new JScrollPane(pane) {

					private static final long serialVersionUID = 1L;

					@Override
					public Dimension getPreferredSize() {
						return new Dimension(CELL_WIDTH, CELL_HEIGHT);
					}

					@Override
					public Dimension getMinimumSize() {
						return new Dimension(150, 150);
					}

					@Override
					public Dimension getMaximumSize() {
						return new Dimension(CELL_WIDTH, CELL_HEIGHT);
					}
				};
				pane.installDefaultStylesheet();
				scrollPane.setBorder(null);
				scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
				scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				textComponent = scrollPane;
			} else {
				JLabel label = new JLabel(
						"<html><style type=\"text/css\">.result-text-highlight {color: #ff6600;font-size:24px;margin:10px;font-weight: 600;}</style><div style=\"max-width:"
								+ (CELL_WIDTH - 10)
								+ "px;\">"
								+ getResultProperty(resultPlotterSettings, "text")
								+ "</div></html>") {

					private static final long serialVersionUID = 1L;

					@Override
					public Dimension getPreferredSize() {
						return new Dimension(CELL_WIDTH, CELL_HEIGHT);
					}

					@Override
					public Dimension getMinimumSize() {
						return new Dimension(150, 150);
					}

					@Override
					public Dimension getMaximumSize() {
						return new Dimension(CELL_WIDTH, Integer.MAX_VALUE);
					}
				};
				label.setVerticalAlignment(SwingConstants.TOP);
				textComponent = label;
			}
			wrapper.add(textComponent, BorderLayout.CENTER);
		} else if (TemplateController.RESULT_RENDERER_NAME_TABLE.equals(rendererName)) {
			final ExampleSetTable table = new ExampleSetTable((ExampleSet) ioo, false);
			table.getTableHeader().setReorderingAllowed(false);
			table.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseReleased(MouseEvent e) {
					mouseAction(e);
				}

				@Override
				public void mousePressed(MouseEvent e) {
					mouseAction(e);
				}

				@Override
				public void mouseClicked(MouseEvent e) {
					mouseAction(e);
				}

				private void mouseAction(MouseEvent e) {
					if (e.isPopupTrigger()) {
						JPopupMenu popup = new JPopupMenu();
						popup.add(new ExportExcelAction(controller.getModel(), index));
						popup.show(table, e.getX(), e.getY());
					}
				}
			});
			addOpenHandlers(table, ioo, null, null);
			decorateWithTooltip(table, tooltip, linkedOperator);
			JScrollPane tablePane = new JScrollPane(table) {

				private static final long serialVersionUID = 1L;

				@Override
				public Dimension getPreferredSize() {
					return new Dimension(CELL_WIDTH, CELL_HEIGHT);
				}

				@Override
				public Dimension getMinimumSize() {
					return new Dimension(150, 150);
				}

				@Override
				public Dimension getMaximumSize() {
					return new Dimension(CELL_WIDTH, CELL_HEIGHT);
				}
			};
			tablePane.setBackground(Color.WHITE);
			tablePane.setBorder(null);
			tablePane.getViewport().setBackground(Color.WHITE);
			wrapper.add(tablePane, BorderLayout.CENTER);
		} else {
			List<Renderer> renderers = RendererService.getRenderers(reportableType);
			// check if selected renderer is available
			Renderer selectedRenderer = null;
			for (Renderer renderer : renderers) {
				if (renderer.getName().equals(rendererName)) {
					selectedRenderer = renderer;
					break;
				}
			}
			if (selectedRenderer == null) {
				LogService.getRoot().log(Level.WARNING, "Unknown renderer " + rendererName + " for " + reportableType);
				return new JLabel("Unknown renderer '" + rendererName + "'");
			}

			for (Entry<Object, Object> prop : resultPlotterSettings.entrySet()) {
				selectedRenderer.getParameters().setParameter(prop.getKey().toString(),
						TemplateController.expandMacros(prop.getValue().toString(), controller.getModel()));
			}

			Reportable reportable = selectedRenderer.createReportable(ioo, new IOContainer(), 300, 300); // size
																											// does
																											// not
																											// matter
			if (reportable instanceof Renderable) {
				final Renderable renderable = (Renderable) reportable;
				JComponent chartPanel = null;
				if (selectedRenderer instanceof AbstractGraphRenderer) {
					// Hack for fixing the broken renderable of decision trees etc.
					for (PrintableComponent printableComponent : PrintingTools.findExportComponents(selectedRenderer
							.getVisualizationComponent(ioo, null))) {
						Component graphPanel = printableComponent.getExportComponent();
						((JComponent) graphPanel).setBorder(BorderFactory.createLineBorder(SwingTools.RAPIDMINER_ORANGE));
						chartPanel = new JPanel(new BorderLayout()) {

							private static final long serialVersionUID = 1L;

							@Override
							public Dimension getPreferredSize() {
								return new Dimension(CELL_WIDTH, CELL_HEIGHT);
							}

							@Override
							public Dimension getMinimumSize() {
								return new Dimension(150, 150);
							}

							@Override
							public Dimension getMaximumSize() {
								return new Dimension(CELL_WIDTH, CELL_HEIGHT);
							}
						};
						((JPanel) chartPanel).add(graphPanel, BorderLayout.CENTER);
						decorateWithTooltip((JComponent) graphPanel, tooltip, linkedOperator);
					}
				}
				if (chartPanel == null) {
					chartPanel = new JPanel() {

						private static final long serialVersionUID = 1L;

						@Override
						protected void paintComponent(java.awt.Graphics g) {
							renderable.render(g, getWidth(), getHeight());
						};

						@Override
						public Dimension getPreferredSize() {
							return new Dimension(CELL_WIDTH, CELL_HEIGHT);
						}

						@Override
						public Dimension getMinimumSize() {
							return new Dimension(150, 150);
						}

						@Override
						public Dimension getMaximumSize() {
							return new Dimension(CELL_WIDTH, CELL_HEIGHT);
						}
					};
					decorateWithTooltip(chartPanel, tooltip, linkedOperator);
				}
				wrapper.add(chartPanel, BorderLayout.CENTER);
				HashMap<String, String> settingsAsMap = new HashMap<>();
				for (Entry<Object, Object> entry : resultPlotterSettings.entrySet()) {
					settingsAsMap.put((String) entry.getKey(),
							TemplateController.expandMacros((String) entry.getValue(), controller.getModel()));
				}
				addOpenHandlers(chartPanel, ioo, rendererName, settingsAsMap);
			} else {
				wrapper.add(new JLabel("Cannot display reportables of " + reportable.getClass()));
			}
		}
		wrapper.putClientProperty(CLIENT_PROPERTY_TITLE, title);
		wrapper.putClientProperty(CLIENT_PROPERTY_MAIN_TEXT, tooltip);
		return wrapper;
	}

	private String getResultProperty(Properties properties, String key) {
		String value = properties.getProperty(key);
		if (value != null) {
			value = TemplateController.expandMacros(value, controller.getModel());
		}
		return value;
	}

	/**
	 * Adds a bubble to the component showing a title, main text, and "Got it" button which opens
	 * the successor. The first bubble will automatically become visible once the first component
	 * becomes visible.
	 */
	private void addBubbles(final List<JComponent> components) {
		if (components.isEmpty()) {
			return;
		}
		final JComponent wrapper = components.get(0);
		final AncestorListener l = new AncestorListener() {

			@Override
			public void ancestorRemoved(AncestorEvent event) {}

			@Override
			public void ancestorMoved(AncestorEvent event) {}

			@Override
			public void ancestorAdded(AncestorEvent event) {
				createBubble(components, 0).setVisible(true);
				wrapper.removeAncestorListener(this);
			}
		};
		wrapper.addAncestorListener(l);
	}

	/**
	 * Returns true iff a bubble should be added for this container, i.e. if the client property
	 * {@link #CLIENT_PROPERTY_MAIN_TEXT} is set.
	 */
	private boolean hasBubble(JComponent component) {
		return component.getClientProperty(CLIENT_PROPERTY_MAIN_TEXT) != null;
	}

	/**
	 * Creates, but does not show, a bubble attached to the startIndex-th component in the list.
	 * Automatically opens the successor when the user clicked "Got it".
	 */
	private ComponentBubbleWindow createBubble(final List<JComponent> components, final int startIndex) {
		final JComponent wrapper = components.get(startIndex);
		scrollRectToCenter(wrapper, wrapper.getBounds());
		BubbleWindow.AlignedSide alignment = (AlignedSide) wrapper.getClientProperty(CLIENT_PROPERTY_BUBBLE_LOCATION);
		if (alignment == null) {
			alignment = AlignedSide.BOTTOM;
		}
		final ComponentBubbleWindow bubble = new ComponentBubbleWindow(wrapper, RapidMinerGUI.getMainFrame(), alignment,
				"bla", null, new JButton[] { new JButton(new ResourceAction("template.got_it") {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						// Close button
						Component source = (Component) e.getSource();
						Container parentContainer = SwingUtilities.getAncestorOfClass(BubbleWindow.class, source);
						if (parentContainer != null) {
							((BubbleWindow) parentContainer).killBubble(true);
						}
						// show next
						if (startIndex + 1 < components.size()) {
							createBubble(components, startIndex + 1).setVisible(true);
						} else {
							((JComponent) wrapper.getParent()).scrollRectToVisible(new Rectangle(wrapper.getSize()));
						}
					}
				}) });
		bubble.setHeadline((String) wrapper.getClientProperty(CLIENT_PROPERTY_TITLE));
		bubble.setMainText((String) wrapper.getClientProperty(CLIENT_PROPERTY_MAIN_TEXT));
		bubble.pack();
		bubble.setVisible(true);
		return bubble;
	}

	private void scrollRectToCenter(Container wrapper, Rectangle wrapperBounds) {
		JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, wrapper);
		if (viewport != null) {
			Rectangle viewRect = viewport.getViewRect();
			wrapperBounds.setLocation(wrapperBounds.x - viewRect.x, wrapperBounds.y - viewRect.y);

			int centerX = (viewRect.width - wrapperBounds.width) / 2;
			int centerY = (viewRect.height - wrapperBounds.height) / 2;
			if (wrapperBounds.x < centerX) {
				centerX = -centerX;
			}
			if (wrapperBounds.y < centerY) {
				centerY = -centerY;
			}
			wrapperBounds.translate(centerX, centerY);
			viewport.scrollRectToVisible(wrapperBounds);
		} else {
			((JComponent) wrapper).scrollRectToVisible(wrapperBounds);
		}
	}

	private void decorateWithTooltip(final JComponent comp, final String text, final String linkedOperator) {
		if (!USE_TOOLTIPS) {
			return;
		}
		if (text == null) {
			return;
		}
		new ToolTipWindow(new TipProvider() {

			private final Object TIP = new Object();
			private final Object NO_TIP = new Object();

			@Override
			public String getTip(Object id) {
				return text;
			}

			@Override
			public Object getIdUnder(Point point) {
				if (comp.getBounds().contains(point)) {
					return TIP;
				} else {
					return NO_TIP;
				}
			}

			@Override
			public Component getCustomComponent(Object id) {
				if (linkedOperator != null) {
					return new LinkButton(new ResourceAction("template.show_operator", linkedOperator) {

						private static final long serialVersionUID = 1L;

						@Override
						public void actionPerformed(ActionEvent e) {
							Runnable selectOperatorTask = new Runnable() {

								@Override
								public void run() {
									MainFrame mainFrame = RapidMinerGUI.getMainFrame();
									mainFrame.selectOperator(mainFrame.getProcess().getOperator(linkedOperator));
									mainFrame.getPerspectives().showPerspective(Perspectives.DESIGN);
								}
							};
							if (!controller.getModel().isProcessOpened()) {
								TemplateView.showProcess(controller, selectOperatorTask);
							} else {
								selectOperatorTask.run();
							}
						}
					}, true);
				} else {
					return null;
				}
			}
		}, comp).setOnlyWhenFocussed(false);
		;
	}

	/**
	 * Adds listeners so this {@link IOObjectEntry} will be opened when the component is
	 * double-clicked.
	 */
	private void addOpenHandlers(Component comp, final IOObject ioo, final String rendererName,
			final Map<String, String> plotterSettings) {
		comp.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					if (rendererName != null) {
						ioo.setUserData(ResultDisplayTools.IOOBJECT_USER_DATA_KEY_RENDERER, rendererName);
					}
					if (plotterSettings != null) {
						String plotterName = plotterSettings.get("plotter");
						Map<String, String> clone = new HashMap<>(plotterSettings);
						if (plotterName != null) {
							// Classic plotter magic: Key "bars_plot_column" turns into
							// "_plot_column"
							ioo.setUserData(PlotterConfigurationSettings.IOOBJECT_USER_DATA_PLOTTER_KEY, plotterName);
							for (Entry<String, String> entry : plotterSettings.entrySet()) {
								if (entry.getKey().startsWith(plotterName.toLowerCase())) {
									clone.put(entry.getKey().substring(plotterName.length()), entry.getValue());
								}
							}
						}
						ioo.setUserData(PlotterConfigurationSettings.IOOBJECT_USER_DATA_SETTINGS_KEY, clone);
					}
					RapidMinerGUI.getMainFrame().getResultDisplay().showResult((ResultObject) ioo);
				}
			};
		});
	}

	private TemplateState getState() {
		return controller.getModel();
	}

}
