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
package com.rapidminer.gui.processeditor.results;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.List;

import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.rapidminer.Process;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.renderer.DefaultTextRenderer;
import com.rapidminer.gui.renderer.Renderer;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.report.Renderable;
import com.rapidminer.report.Reportable;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryLocation;


/**
 * Displays an overview of a single IOObject. Does not remember the IOObject itself.
 *
 * @author Simon Fischer
 *
 */
public class SingleResultOverview extends JPanel {

	private static final long serialVersionUID = 1L;

	private final JLabel title;
	private final Component main;
	private String repositoryLocation = null;
	private RepositoryLocation processFolderLocation = null;
	private SoftReference<ResultObject> ioObject;

	static final int MIN_HEIGHT = 300;
	static final int MIN_WIDTH = 300;

	private static final int MAX_RESULT_STRING_LENGTH = 2048;

	private final Action RESTORE_FROM_REPOSITORY = new ResourceAction("resulthistory.restore_data") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			final ProgressThread downloadProgressThread = new ProgressThread("download_from_repository") {

				@Override
				public void run() {
					try {
						RepositoryLocation location = null;
						if (RepositoryLocation.isAbsolute(repositoryLocation)) {
							location = new RepositoryLocation(repositoryLocation);
						} else {
							location = new RepositoryLocation(processFolderLocation, repositoryLocation);
						}
						Entry entry = location.locateEntry();
						if (entry instanceof IOObjectEntry) {
							IOObjectEntry data = (IOObjectEntry) entry;
							ResultObject result = (ResultObject) data.retrieveData(this.getProgressListener());
							result.setSource(data.getLocation().toString());
							RapidMinerGUI.getMainFrame().getResultDisplay().showResult(result);
						} else {
							SwingTools.showSimpleErrorMessage("cannot_fetch_data_from_repository", "Not an IOObject.");
						}
					} catch (Exception e1) {
						SwingTools.showSimpleErrorMessage("cannot_fetch_data_from_repository", e1);
					}
				}
			};
			downloadProgressThread.start();
		}
	};

	private final Action OPEN_DATA = new ResourceAction("resulthistory.open_data") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			ResultObject referenced = ioObject.get();
			if (referenced != null) {
				RapidMinerGUI.getMainFrame().getResultDisplay().showResult(referenced);
			}
		}
	};

	public SingleResultOverview(IOObject result, Process process, int resultIndex) {
		setLayout(null);
		setOpaque(false);
		MetaData metaData = MetaData.forIOObject(result, true);
		setBorder(new RapidBorder(ProcessDrawUtils.getColorFor(metaData), 15, 35));
		setBackground(Color.WHITE);

		if (result instanceof ResultObject) {
			this.ioObject = new SoftReference<ResultObject>((ResultObject) result);
		}

		if (process.getRootOperator().getSubprocess(0).getInnerSinks().getNumberOfPorts() > resultIndex) {
			InputPort resultPort = process.getRootOperator().getSubprocess(0).getInnerSinks().getPortByIndex(resultIndex);
			IOObject other = resultPort.getAnyDataOrNull();
			if (result == other) { // make sure result does not come from collecting unconnected
				// outputs, but is really
				// from inner sink
				if (process.getContext().getOutputRepositoryLocations().size() > resultIndex) {
					repositoryLocation = process.getContext().getOutputRepositoryLocations().get(resultIndex).toString();
					processFolderLocation = process.getRepositoryLocation() != null ? process.getRepositoryLocation()
							.parent() : null;
				}
			}
		}

		String name = result.getClass().getSimpleName();
		if (result instanceof ExampleSet) {
			main = makeMainLabel("<html>" + metaData.getDescription() + "</html>");
			name = ((ResultObject) result).getName();
		} else {
			name = RendererService.getName(result.getClass());
			List<Renderer> renderers = RendererService.getRenderers(name);
			if (renderers.isEmpty()) {
				main = makeTextRenderer(result);
			} else {
				Component component = null;
				for (Renderer renderer : renderers) {
					if (!(renderer instanceof DefaultTextRenderer)) {
						IOContainer dummy = new IOContainer();
						int imgWidth = MIN_WIDTH - 10;
						int imgHeight = MIN_HEIGHT - 65;
						Reportable reportable = renderer.createReportable(result, dummy, imgWidth, imgHeight);
						if (reportable instanceof Renderable) {
							Renderable renderable = (Renderable) reportable;
							renderable.prepareRendering();
							int preferredWidth = renderable.getRenderWidth(800);
							int preferredHeight = renderable.getRenderHeight(800);
							final BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
							Graphics2D graphics = (Graphics2D) img.getGraphics();
							graphics.setColor(Color.WHITE);
							graphics.fillRect(0, 0, imgWidth, imgHeight);
							double scale = Math.min((double) imgWidth / (double) preferredWidth, (double) imgHeight
									/ (double) preferredHeight);
							graphics.scale(scale, scale);
							renderable.render(graphics, preferredWidth, preferredHeight);
							component = new JPanel() {

								private static final long serialVersionUID = 1L;

								@Override
								protected void paintComponent(java.awt.Graphics g) {
									g.drawImage(img, 0, 0, null);
								}
							};
							break;
						}
					} else {
						component = makeTextRenderer(result);
						break;
					}
				}
				if (component == null) {
					main = makeTextRenderer(result);
				} else {
					main = component;
				}
			}
		}

		StringBuilder b = new StringBuilder();
		b.append("<html><strong>").append(name);
		b.append("</strong>");
		if (result.getSource() != null) {
			b.append(" (").append(result.getSource()).append(")");
		}
		if (repositoryLocation != null && !repositoryLocation.isEmpty()) {
			b.append("<br/><small>").append(repositoryLocation).append("</small>");
		} else {
			b.append("<br/><small>Result not stored in repository.</small>");
		}
		b.append("</html>");
		title = new JLabel(b.toString());
		add(title);
		title.setBounds(5, 0, MIN_WIDTH - 15, 40);

		add(main);
		main.setBounds(5, 45, MIN_WIDTH - 10, MIN_HEIGHT - 65);

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showContextMenu(e.getPoint());
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showContextMenu(e.getPoint());
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showContextMenu(e.getPoint());
				}
			}
		});

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				main.setBounds(5, 45, getWidth() - 10, getHeight() - 65);
			}
		});
	}

	private Component makeTextRenderer(IOObject result) {
		if (result instanceof ResultObject) {
			String resultString = ((ResultObject) result).toResultString();
			if (resultString.length() > MAX_RESULT_STRING_LENGTH) {
				resultString = resultString.substring(0, MAX_RESULT_STRING_LENGTH);
			}
			return makeMainLabel("<html><pre>" + resultString + "</pre></html>");
		} else {
			return makeMainLabel("No information available.");
		}
	}

	private Component makeMainLabel(String text) {
		JEditorPane label = new ExtendedHTMLJEditorPane("text/html", text);
		StyleSheet css = ((HTMLEditorKit) label.getEditorKit()).getStyleSheet();
		css.addRule("body {font-family:Sans;font-size:11pt}");
		css.addRule("h3 {margin:0; padding:0}");
		css.addRule("h4 {margin-bottom:0; margin-top:1ex; padding:0}");
		css.addRule("p  {margin-top:0; margin-bottom:1ex; padding:0}");
		css.addRule("ul {margin-top:0; margin-bottom:1ex; list-style-image: url("
				+ getClass().getResource("/com/rapidminer/resources/icons/modern/help/circle.png") + ")}");
		css.addRule("ul li {padding-bottom: 2px}");
		css.addRule("li.outPorts {padding-bottom: 0px}");
		css.addRule("ul li ul {margin-top:0; margin-bottom:1ex; list-style-image: url("
				+ getClass().getResource("/com/rapidminer/resources/icons/modern/help/line.png") + ")");
		css.addRule("li ul li {padding-bottom:0}");

		// label.setOpaque(false);
		label.setEditable(false);
		label.setBackground(Color.WHITE);
		// label.setVerticalTextPosition(SwingConstants.TOP);
		// label.setHorizontalTextPosition(SwingConstants.LEFT);

		JScrollPane pane = new JScrollPane(label);
		pane.setBackground(Color.WHITE);
		pane.setBorder(null);
		return pane;
	}

	protected void showContextMenu(Point point) {
		JPopupMenu menu = new JPopupMenu();
		boolean empty = true;
		if (repositoryLocation != null && !repositoryLocation.isEmpty()) {
			menu.add(RESTORE_FROM_REPOSITORY);
			empty = false;
		}
		if (ioObject != null && ioObject.get() != null) {
			menu.add(OPEN_DATA);
			empty = false;
		}
		if (!empty) {
			menu.show(this, (int) point.getX(), (int) point.getY());
		}
	}
}
