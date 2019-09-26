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
package com.rapidminer.gui.processeditor.results;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.rapidminer.Process;
import com.rapidminer.adaption.belt.TableViewingTools;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.renderer.DefaultTextRenderer;
import com.rapidminer.gui.renderer.Renderer;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.MultiSwingWorker;
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
import com.rapidminer.tools.ReferenceCache;
import com.rapidminer.tools.Tools;


/**
 * Displays an overview of a single IOObject. Does not remember the IOObject itself.
 *
 * @author Simon Fischer, Marco Boeck
 *
 */
public class SingleResultOverview extends JPanel {

	private static final int MAX_RESULT_STRING_LENGTH = 2048;

	private static final long serialVersionUID = 1L;

	private static final ReferenceCache<ResultObject> RESULT_OBJECT_CACHE = new ReferenceCache<>(20);

	private final JLabel title;
	private final Component main;
	private String repositoryLocation = null;
	private RepositoryLocation processFolderLocation = null;
	private ReferenceCache<ResultObject>.Reference ioObject;

	static final int MIN_HEIGHT = 300;
	static final int MIN_WIDTH = 300;

	private final Action RESTORE_FROM_REPOSITORY = new ResourceAction("resulthistory.restore_data") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
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
		public void loggedActionPerformed(ActionEvent e) {
			ResultObject referenced = ioObject.get();
			if (referenced != null) {
				RapidMinerGUI.getMainFrame().getResultDisplay().showResult(referenced);
			}
		}
	};

	private BufferedImage img;

	public SingleResultOverview(IOObject result, Process process, int resultIndex) {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(0, 0, 10, 0);
		gbc.anchor = GridBagConstraints.NORTHWEST;

		MetaData metaData = MetaData.forIOObject(result, true);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5),
				new RapidBorder(ProcessDrawUtils.getColorFor(metaData), RapidLookAndFeel.CORNER_DEFAULT_RADIUS, 35)));
		setBackground(Colors.WHITE);

		if (result instanceof ResultObject) {
			this.ioObject = RESULT_OBJECT_CACHE.newReference((ResultObject) result);
		}

		if (process.getRootOperator().getSubprocess(0).getInnerSinks().getNumberOfPorts() > resultIndex) {
			InputPort resultPort = process.getRootOperator().getSubprocess(0).getInnerSinks().getPortByIndex(resultIndex);
			IOObject other = resultPort.getRawData();
			if (result == other) { // make sure result does not come from collecting unconnected
				// outputs, but is really
				// from inner sink
				if (process.getContext().getOutputRepositoryLocations().size() > resultIndex) {
					repositoryLocation = process.getContext().getOutputRepositoryLocations().get(resultIndex).toString();
					processFolderLocation = process.getRepositoryLocation() != null
							? process.getRepositoryLocation().parent() : null;
				}
			}
		}

		String name = result.getClass().getSimpleName();
		if (TableViewingTools.isDataTable(result)) {
			main = makeMainLabel("<html>" + metaData.getDescription() + "</html>");
			name = ((ResultObject) result).getName();
		} else {
			name = RendererService.getName(result.getClass());
			List<Renderer> renderers = RendererService.getRenderersExcludingLegacyRenderers(name);
			if (renderers.isEmpty()) {
				main = makeTextRenderer(result);
			} else {
				Component component = null;
				for (Renderer renderer : renderers) {
					if (!(renderer instanceof DefaultTextRenderer)) {
						IOContainer dummy = new IOContainer();
						Reportable reportable = renderer.createReportable(result, dummy, 800, 500);
						if (reportable instanceof Renderable) {
							updatePreviewImage();
							component = new JPanel() {

								private static final long serialVersionUID = 1L;

								@Override
								protected void paintComponent(java.awt.Graphics g) {
									if (img != null) {
										g.drawImage(img, 0, 0, null);
									}
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
		add(title, gbc);

		main.setPreferredSize(new Dimension(MIN_WIDTH - 10, MIN_HEIGHT - 65));
		gbc.gridy += 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(main, gbc);

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

			private void showContextMenu(Point point) {
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
					menu.show(SingleResultOverview.this, (int) point.getX(), (int) point.getY());
				}
			}
		});

		addAncestorListener(new AncestorListener() {

			@Override
			public void ancestorRemoved(AncestorEvent event) {
				// not needed
			}

			@Override
			public void ancestorMoved(AncestorEvent event) {
				// not needed
			}

			@Override
			public void ancestorAdded(AncestorEvent event) {
				// update image (correct size is now known)
				updatePreviewImage();
			}
		});

	}

	/**
	 * Create a text renderer for this result.
	 *
	 * @param result
	 * @return
	 */
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

	/**
	 * Creates the main text representation of this result.
	 *
	 * @param text
	 * @return
	 */
	private Component makeMainLabel(String text) {
		JEditorPane label = new ExtendedHTMLJEditorPane("text/html", text);
		StyleSheet css = ((HTMLEditorKit) label.getEditorKit()).getStyleSheet();
		css.addRule("body {font-family:Sans;font-size:11pt}");
		css.addRule("h3 {margin:0; padding:0}");
		css.addRule("h4 {margin-bottom:0; margin-top:1ex; padding:0}");
		css.addRule("p  {margin-top:0; margin-bottom:1ex; padding:0}");
		css.addRule("ul {margin-top:0; margin-bottom:1ex; list-style-image: url("
				+ Tools.getResource("icons/help/circle.png") + ")}");
		css.addRule("ul li {padding-bottom: 2px}");
		css.addRule("li.outPorts {padding-bottom: 0px}");
		css.addRule("ul li ul {margin-top:0; margin-bottom:1ex; list-style-image: url("
				+ Tools.getResource("icons/help/line.png") + ")");
		css.addRule("li ul li {padding-bottom:0}");

		label.setEditable(false);
		label.setBackground(Colors.WHITE);

		JScrollPane pane = new JScrollPane(label);
		pane.setBackground(Colors.WHITE);
		pane.setBorder(null);
		return pane;
	}

	/**
	 * Updates the preview renderable image in a {@link MultiSwingWorker}.
	 */
	private void updatePreviewImage() {
		final IOObject result = ioObject != null ? ioObject.get() : null;
		if (result != null) {
			String name = RendererService.getName(result.getClass());
			final List<Renderer> renderers = RendererService.getRenderersExcludingLegacyRenderers(name);
			if (renderers.isEmpty()) {
				return;
			}

			MultiSwingWorker<Void, Void> sw = new MultiSwingWorker<Void, Void>() {

				@Override
				protected Void doInBackground() throws Exception {
					int width = Math.max(getSize().width, MIN_WIDTH);
					int height = Math.max(getSize().height, MIN_HEIGHT);
					for (Renderer renderer : renderers) {
						Reportable reportable = renderer.createReportable(result, new IOContainer(), 800, 600);
						if (reportable instanceof Renderable) {
							Renderable renderable = (Renderable) reportable;
							renderable.prepareRendering();
							int preferredWidth = renderable.getRenderWidth(800);
							int preferredHeight = renderable.getRenderHeight(600);

							img = new BufferedImage(preferredWidth, preferredHeight, BufferedImage.TYPE_INT_RGB);
							Graphics2D graphics = (Graphics2D) img.getGraphics();
							graphics.setColor(Colors.WHITE);
							graphics.fillRect(0, 0, 5000, 3000);
							double scale = Math.min((double) width / (double) preferredWidth,
									(double) height / (double) preferredHeight);
							graphics.scale(scale, scale);
							renderable.render(graphics, preferredWidth, preferredHeight);

							break;
						}
					}

					return null;
				}

				@Override
				public void done() {
					main.repaint();
				}
			};
			sw.start();
		}
	}
}
