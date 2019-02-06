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
package com.rapidminer.gui.graphs;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.text.StyleContext;

import org.apache.commons.collections15.Transformer;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.graphs.actions.ZoomInAction;
import com.rapidminer.gui.graphs.actions.ZoomOutAction;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJToolBar;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.report.Renderable;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;

import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.MultiLayerTransformer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.ConstantDirectionalEdgeValueTransformer;
import edu.uci.ics.jung.visualization.decorators.DirectionalEdgeArrowTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.renderers.BasicVertexRenderer;
import edu.uci.ics.jung.visualization.renderers.EdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.renderers.VertexLabelRenderer;
import edu.uci.ics.jung.visualization.util.Animator;
import edu.uci.ics.jung.visualization.util.ArrowFactory;


/**
 * The basic graph viewer component for graph display.
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class GraphViewer<V, E> extends JPanel implements Renderable, PrintableComponent {

	private static final long serialVersionUID = -7501422172633548861L;

	private static final Color EDGE_LIGHT_GRAY = new Color(230, 230, 230);

	private static final Color LEAF_BACKGROUND = new Color(230, 230, 230);

	private static final Color BORDER_INVIS = new Color(0, 0, 0, 0);

	public static final Color NODE_SELECTED = Colors.TEXT_HIGHLIGHT_BACKGROUND;

	public static final Color NODE_BACKGROUND = new Color(230, 230, 230);

	public static final Color NODE_ON_PATH = NODE_SELECTED;

	public static final Font EDGE_FONT = FontTools.getFont("Open Sans", Font.PLAIN, 12);

	public static final Font EDGE_FONT_ON_PATH = FontTools.getFont("Open Sans Semibold", Font.BOLD, 13);

	public static final Font VERTEX_BOLD_FONT = FontTools.getFont("Open Sans Semibold", Font.BOLD, 14);

	public static final Font VERTEX_PLAIN_FONT = FontTools.getFont("Open Sans Semibold", Font.BOLD, 14);

	private VisualizationViewer<V, E> vv;

	private transient Layout<V, E> layout;

	private transient GraphCreator<V, E> graphCreator;

	private LayoutSelection<V, E> layoutSelection;

	private transient ScalingControl scaler = new CrossoverScalingControl();

	private transient SingleDefaultGraphMouse<V, E> graphMouse;

	private boolean showEdgeLabels = true;

	private boolean showVertexLabels = true;

	private transient JSplitPane objectViewerSplitPane;

	public GraphViewer(final GraphCreator<V, E> graphCreator) {
		try {
			if (!RapidMiner.getExecutionMode().isHeadless()) {
				graphMouse = new SingleDefaultGraphMouse<>(1 / 1.1f, 1.1f);
			}
		} catch (HeadlessException e) {
		}

		this.graphCreator = graphCreator;

		setLayout(new BorderLayout());

		Graph<V, E> graph = graphCreator.createGraph();
		this.layoutSelection = new LayoutSelection<>(this, graph);
		this.layoutSelection.putClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK, true);
		this.layoutSelection.setPreferredSize(new Dimension(200, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		this.layout = new ISOMLayout<>(graph);
		vv = new VisualizationViewer<V, E>(layout) {

			private static final long serialVersionUID = 8247229781249216143L;

			private boolean initialized = false;

			/**
			 * Necessary in order to re-change layout after first painting (starting position and
			 * size).
			 */
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				if (!initialized) {
					initialized = true;
					updateLayout();
					if (objectViewerSplitPane != null) {
						objectViewerSplitPane.setDividerLocation(0.9);
					}
				}
			}
		};
		vv.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		vv.getRenderingHints().put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		vv.getRenderingHints().put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		vv.setBorder(BorderFactory.createEmptyBorder());
		vv.setBackground(Colors.WHITE);
		SwingTools.disableClearType(vv);

		// === design ===

		setupRendering(graphCreator);

		// === end of design ===

		// === main panel ===

		if (graphCreator.getObjectViewer() == null) {
			vv.setBorder(BorderFactory.createMatteBorder(10, 0, 5, 10, Colors.WHITE));
			add(vv, BorderLayout.CENTER);
		} else {
			JComponent viewer = graphCreator.getObjectViewer().getViewerComponent();
			if (viewer != null) {
				viewer.setBorder(null);
				objectViewerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
				objectViewerSplitPane.setResizeWeight(1);
				objectViewerSplitPane.add(vv, 0);
				objectViewerSplitPane.add(viewer, 1);
				objectViewerSplitPane.setBorder(BorderFactory.createMatteBorder(10, 0, 5, 10, Colors.WHITE));
				add(objectViewerSplitPane, BorderLayout.CENTER);
			} else {
				vv.setBorder(BorderFactory.createMatteBorder(10, 0, 5, 10, Colors.WHITE));
				add(vv, BorderLayout.CENTER);
			}
		}

		// === control panel ===

		Component controlPanel = createControlPanel();
		JScrollPane sp = new ExtendedJScrollPane(controlPanel);
		sp.setBorder(null);
		add(sp, BorderLayout.WEST);

		this.showEdgeLabels = !graphCreator.showEdgeLabelsDefault();
		togglePaintEdgeLabels();
		this.showVertexLabels = !graphCreator.showVertexLabelsDefault();
		togglePaintVertexLabels();

		this.layoutSelection.setLayout();
	}

	public LayoutSelection<V, E> getLayoutSelection() {
		return this.layoutSelection;
	}

	private JComponent createControlPanel() {
		// === mouse behaviour ===
		if (graphMouse != null) {
			vv.setGraphMouse(graphMouse);
		}

		vv.addGraphMouseListener(new GraphMouseListener<V>() {

			@Override
			public void graphClicked(V vertex, MouseEvent arg1) {
					if (graphCreator.getObjectViewer() != null) {
						vv.getPickedVertexState().clear();
						vv.getPickedVertexState().pick(vertex, true);
						graphCreator.getObjectViewer().showObject(graphCreator.getObject(vertex));
					}
				}

			@Override
			public void graphPressed(V arg0, MouseEvent arg1) {}

			@Override
			public void graphReleased(V vertex, MouseEvent arg1) {}
		});

		JPanel controls = new JPanel();
		controls.setOpaque(true);
		controls.setBackground(Colors.WHITE);
		controls.setBorder(BorderFactory.createMatteBorder(10, 5, 5, 10, Colors.WHITE));
		GridBagLayout gbLayout = new GridBagLayout();
		controls.setLayout(gbLayout);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(4, 4, 4, 4);
		c.weightx = 1;
		c.weighty = 0;

		// zooming
		JToolBar zoomBar = new ExtendedJToolBar();
		zoomBar.setLayout(new FlowLayout(FlowLayout.CENTER));
		zoomBar.setBorder(BorderFactory.createTitledBorder("Zoom"));
		zoomBar.setBackground(Colors.WHITE);
		JButton zoomIn = new JButton(new ZoomInAction(this, IconSize.SMALL));
		zoomIn.setContentAreaFilled(false);
		zoomIn.setText("");
		zoomBar.add(zoomIn);
		JButton zoomOut = new JButton(new ZoomOutAction(this, IconSize.SMALL));
		zoomOut.setContentAreaFilled(false);
		zoomOut.setText("");
		zoomBar.add(zoomOut);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gbLayout.setConstraints(zoomBar, c);
		controls.add(zoomBar);

		// layout selection
		c.gridwidth = GridBagConstraints.REMAINDER;
		gbLayout.setConstraints(layoutSelection, c);
		controls.add(layoutSelection);

		// show node labels
		JCheckBox nodeLabels = new JCheckBox("Node Labels", graphCreator.showVertexLabelsDefault());
		nodeLabels.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				togglePaintVertexLabels();
			}
		});
		c.gridwidth = GridBagConstraints.REMAINDER;
		gbLayout.setConstraints(nodeLabels, c);
		controls.add(nodeLabels);

		// show edge labels
		JCheckBox edgeLabels = new JCheckBox("Edge Labels", graphCreator.showEdgeLabelsDefault());
		edgeLabels.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				togglePaintEdgeLabels();
			}
		});
		c.gridwidth = GridBagConstraints.REMAINDER;
		gbLayout.setConstraints(edgeLabels, c);
		controls.add(edgeLabels);

		// option components
		for (int i = 0; i < graphCreator.getNumberOfOptionComponents(); i++) {
			JComponent optionComponent = graphCreator.getOptionComponent(this, i);
			if (optionComponent != null) {
				c.gridwidth = GridBagConstraints.REMAINDER;
				gbLayout.setConstraints(optionComponent, c);
				controls.add(optionComponent);
			}
		}

		JLabel filler = new JLabel();
		c.weighty = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gbLayout.setConstraints(filler, c);
		controls.add(filler);

		return controls;
	}

	public void updateLayout() {
		changeLayout(this.layout, this.layoutSelection.getAnimate(), 0, 0);
	}

	public void changeLayout(final Layout<V, E> newLayout, boolean animate, int desiredWidth, int desiredHeight) {
		if (newLayout != null) {
			MultiLayerTransformer transformer = vv.getRenderContext().getMultiLayerTransformer();
			double scale = transformer.getTransformer(Layer.VIEW).getScale();

			// set desired size
			if (desiredWidth > 0 && desiredHeight > 0) {
				vv.setSize(desiredWidth, desiredHeight);
				try {
					newLayout.setSize(new Dimension(desiredWidth, desiredHeight));
				} catch (Exception e) {
					// do nothing - some layouts do not support setting of size
				}
			} else {
				int layoutWidth = (int) (vv.getWidth() / scale);
				int layoutHeight = (int) (vv.getHeight() / scale);
				try {
					newLayout.setSize(new Dimension(layoutWidth, layoutHeight));
				} catch (Exception e) {
					// do nothing - some layouts do not support setting of size
				}
			}

			if (layout == null) {
				// initial layout --> no transition possible!
				vv.setGraphLayout(newLayout);
			} else {
				// No transition possible if no edges in graph!
				if (animate) {
					if (newLayout.getGraph().getEdgeCount() > 0 || newLayout.getGraph().getVertexCount() > 0) {
						try {
							LayoutTransition<V, E> lt = new LayoutTransition<>(vv, layout, newLayout);
							Animator animator = new Animator(lt);
							animator.start();
						} catch (Throwable e) {
							// any error --> no transition possible!
							vv.setGraphLayout(newLayout);
						}
					}
				} else {
					vv.setGraphLayout(newLayout);
				}
			}
			this.layout = newLayout;

			vv.scaleToLayout(this.scaler);

			double viewX = transformer.getTransformer(Layer.VIEW).getTranslateX();
			double viewY = transformer.getTransformer(Layer.VIEW).getTranslateX();
			double scaleViewX = viewX * scale;
			double scaleViewY = viewY * scale;
			transformer.getTransformer(Layer.VIEW).translate(-scaleViewX, -scaleViewY);

			Thread changeToStaticThread = new Thread() {

				@Override
				public void run() {
					try {
						sleep(10000);
					} catch (InterruptedException e) {
						// do nothing
					}
					// change to static
					if (GraphViewer.this.layout.equals(newLayout)) { // still the same layout?
						vv.setGraphLayout(new StaticLayout<>(layout.getGraph(), layout));
					}
				}
			};
			changeToStaticThread.start();
		}
	}

	/**
	 * VertexLabel is not parameterized in Jung. In order to avoid to make all things unchecked, the
	 * default label position setting is done in this method.
	 */
	private void setDefaultLabelPosition() {
		vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
	}

	public void zoomIn() {
		scaler.scale(vv, 1.1f, vv.getCenter());
	}

	public void zoomOut() {
		scaler.scale(vv, 1 / 1.1f, vv.getCenter());
	}

	private void togglePaintEdgeLabels() {
		setPaintEdgeLabels(!showEdgeLabels);
	}

	public void setPaintEdgeLabels(boolean showEdgeLabels) {
		this.showEdgeLabels = showEdgeLabels;
		if (this.showEdgeLabels) {
			vv.getRenderContext().setEdgeLabelTransformer(new Transformer<E, String>() {

				@Override
				public String transform(E object) {
					return graphCreator.getEdgeName(object);
				}
			});
		} else {
			vv.getRenderContext().setEdgeLabelTransformer(new Transformer<E, String>() {

				@Override
				public String transform(E object) {
					return null;
				}
			});
		}
		vv.repaint();
	}

	private void togglePaintVertexLabels() {
		setPaintVertexLabels(!showVertexLabels);
	}

	public void setPaintVertexLabels(boolean showVertexLabels) {
		this.showVertexLabels = showVertexLabels;
		if (this.showVertexLabels) {
			Renderer.Vertex<V, E> vertexRenderer = graphCreator.getVertexRenderer();
			if (vertexRenderer != null) {
				vv.getRenderer().setVertexRenderer(vertexRenderer);
			}
			vv.getRenderContext().setVertexShapeTransformer(new ExtendedVertexShapeTransformer<>(graphCreator));
			vv.getRenderContext().setVertexLabelTransformer(new Transformer<V, String>() {

				@Override
				public String transform(V object) {
					return graphCreator.getVertexName(object);
				}
			});
		} else {
			vv.getRenderer().setVertexRenderer(new BasicVertexRenderer<V, E>());
			vv.getRenderContext().setVertexShapeTransformer(new BasicVertexShapeTransformer<V>());
			vv.getRenderContext().setVertexLabelTransformer(new Transformer<V, String>() {

				@Override
				public String transform(V object) {
					return null;
				}
			});
		}
		vv.repaint();
	}

	public GraphCreator<V, E> getGraphCreator() {
		return this.graphCreator;
	}

	public Component getVisualizationComponent() {
		return vv;
	}

	@Override
	public void prepareRendering() {
		// this dirty hack was copied from the AbstractGraphRenderer
		// this method is NOT used by Studio, only by the reporting extension
		try {
			// necessary to give layout (thread!) time to finish its work before reporting
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// do nothing
		}
	}

	@Override
	public void finishRendering() {}

	@Override
	public int getRenderHeight(int preferredHeight) {
		int height = vv.getHeight();
		if (height < 1) {
			height = preferredHeight;
		}
		return height;
	}

	@Override
	public int getRenderWidth(int preferredWidth) {
		int width = vv.getWidth();
		if (width < 1) {
			width = preferredWidth;
		}
		return width;
	}

	@Override
	public void render(Graphics graphics, int width, int height) {
		vv.setSize(width, height);
		vv.setBorder(BorderFactory.createEmptyBorder());
		changeLayout(this.layout, false, width, height);
		vv.paint(graphics);
	}

	public List<ParameterType> getParameterTypes() {
		return new LinkedList<>();
	}

	@Override
	public Component getExportComponent() {
		return vv;
	}

	@Override
	public String getExportName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.cards.result_view.graph_view.title");
	}

	@Override
	public String getIdentifier() {
		return null;
	}

	@Override
	public String getExportIconName() {
		return I18N.getGUIMessage("gui.cards.result_view.graph_view.icon");
	}

	/**
	 * Setup rendering for tree models.
	 *
	 * @param graphCreator
	 *            the creator instance
	 */
	private void setupRendering(final GraphCreator<V, E> graphCreator) {
		// ## edge layout ##
		// EDGE SHAPE
		int edgeShapeType = graphCreator.getEdgeShape();
		switch (edgeShapeType) {
			case GraphCreator.EDGE_SHAPE_LINE:
				vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<V, E>());
				break;
			case GraphCreator.EDGE_SHAPE_QUAD_CURVE:
				vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.QuadCurve<V, E>());
				break;
			case GraphCreator.EDGE_SHAPE_CUBIC_CURVE:
				vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.CubicCurve<V, E>());
				break;
			case GraphCreator.EDGE_SHAPE_BENT_LINE:
				vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.BentLine<V, E>());
				break;
			case GraphCreator.EDGE_SHAPE_WEDGE:
				vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Wedge<V, E>(5));
				break;
			default:
				vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<V, E>());
				break;
		}

		// EDGE FONT
		vv.getRenderContext().setEdgeFontTransformer(new Transformer<E, Font>() {

			@Override
			public Font transform(E edge) {
				if (graphCreator.isEdgeOnSelectedPath(vv.getPickedVertexState().getPicked(), edge)) {
					return EDGE_FONT_ON_PATH;
				} else {
					return EDGE_FONT;
				}
			}
		});

		// EDGE COLORS
		vv.getRenderContext().setEdgeDrawPaintTransformer(new Transformer<E, Paint>() {

			@Override
			public Paint transform(E edge) {
				if (graphCreator.isEdgeOnSelectedPath(vv.getPickedVertexState().getPicked(), edge)) {
					return NODE_ON_PATH;
				} else {
					return EDGE_LIGHT_GRAY;
				}
			}
		});
		vv.getRenderContext().setArrowDrawPaintTransformer(new Transformer<E, Paint>() {

			@Override
			public Paint transform(E edge) {
				if (graphCreator.isEdgeOnSelectedPath(vv.getPickedVertexState().getPicked(), edge)) {
					return NODE_ON_PATH;
				} else {
					return EDGE_LIGHT_GRAY;
				}
			}
		});
		vv.getRenderContext().setArrowFillPaintTransformer(new Transformer<E, Paint>() {

			@Override
			public Paint transform(E edge) {
				if (graphCreator.isEdgeOnSelectedPath(vv.getPickedVertexState().getPicked(), edge)) {
					return NODE_ON_PATH;
				} else {
					return EDGE_LIGHT_GRAY;
				}
			}
		});

		vv.getRenderContext().setEdgeArrowTransformer(new DirectionalEdgeArrowTransformer<V, E>(8, 7, 0));

		// only change edge thickness for tree models for now
		if (graphCreator instanceof TreeModelGraphCreator) {
			vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<E, Stroke>() {

				@Override
				public Stroke transform(E edge) {
					float edgeStrength = Math.max(0.4f, 10f * (float) graphCreator.getEdgeStrength(edge));
					return new BasicStroke(edgeStrength);
				}

			});
			int length = 8;
			int width = 7;
			int notch = 0;
			vv.getRenderContext().setEdgeArrowTransformer(new DirectionalEdgeArrowTransformer<V, E>(length, width, notch) {

				@Override
				public Shape transform(Context<Graph<V, E>, E> context) {
					// yes, the order is correct and differs from the transformer constructor order
					float edgeStrength = (float) graphCreator.getEdgeStrength(context.element);
					float arrowWidth = Math.max(7f, 24f * edgeStrength);
					float arrowLength = Math.max(8f, 16f * edgeStrength);
					return ArrowFactory.getNotchedArrow(arrowWidth, arrowLength, notch);
				}
			});
		}

		// EDGE LABEL POSITION
		vv.getRenderContext()
				.setEdgeLabelClosenessTransformer(new ConstantDirectionalEdgeValueTransformer<V, E>(0.5d, 0.5d));
		int labelOffset = graphCreator.getLabelOffset();
		if (labelOffset >= 0) {
			vv.getRenderContext().setLabelOffset(labelOffset);
		}

		// EDGE LABELS
		vv.getRenderContext().setEdgeLabelTransformer(new Transformer<E, String>() {

			@Override
			public String transform(E object) {
				return graphCreator.getEdgeName(object);
			}
		});
		// EDGE LABEL RENDERER
		Renderer.EdgeLabel<V, E> edgeLabelRenderer = graphCreator.getEdgeLabelRenderer();
		if (edgeLabelRenderer != null) {
			vv.getRenderer().setEdgeLabelRenderer(edgeLabelRenderer); // renderer...
		}
		vv.getRenderContext().setEdgeLabelRenderer(new EdgeLabelRenderer() { // ...context!

			private JLabel renderer = new JLabel();

			@Override
			public <T> Component getEdgeLabelRendererComponent(JComponent parent, Object value, Font font,
					boolean isSelected, T edge) {
				renderer.setFont(font);
				if (graphCreator.isEdgeLabelDecorating()) {
					renderer.setOpaque(false);
					renderer.setBackground(new Color(0, 0, 0, 0));
				}
				renderer.setText(value.toString());
				return renderer;
			}

			/** Let the graph model decide. */
			@Override
			public boolean isRotateEdgeLabels() {
				return graphCreator.isRotatingEdgeLabels();
			}

			/** Does nothing. */
			@Override
			public void setRotateEdgeLabels(boolean rotate) {}
		});

		// ## vertex layout ##

		// VERTEX FONT
		vv.getRenderContext().setVertexFontTransformer(new Transformer<V, Font>() {

			@Override
			public Font transform(V vertex) {
				if (graphCreator.isBold(vertex)
						|| graphCreator.isVertexOnSelectedPath(vv.getPickedVertexState().getPicked(), vertex)) {
					return VERTEX_BOLD_FONT;
				} else {
					return VERTEX_PLAIN_FONT;
				}
			}
		});
		// VERTEX NAME
		vv.getRenderContext().setVertexLabelTransformer(new Transformer<V, String>() {

			@Override
			public String transform(V object) {
				return graphCreator.getVertexName(object);
			}
		});
		// VERTEX FILL PAINT
		Transformer<V, Paint> paintTransformer = graphCreator.getVertexPaintTransformer(vv);
		if (paintTransformer == null) {
			paintTransformer = new Transformer<V, Paint>() {

				@Override
				public Paint transform(V vertex) {
					if (vv.getPickedVertexState().isPicked(vertex)) {
						return NODE_SELECTED;
					} else if (graphCreator.isVertexOnSelectedPath(vv.getPickedVertexState().getPicked(), vertex)) {
						return NODE_ON_PATH;
					} else {
						if (graphCreator.isLeaf(vertex)) {
							return LEAF_BACKGROUND;
						} else {
							return NODE_BACKGROUND;
						}
					}
				}
			};
		}
		vv.getRenderContext().setVertexFillPaintTransformer(paintTransformer);

		// VERTEX DRAW PAINT
		vv.getRenderContext().setVertexDrawPaintTransformer(new Transformer<V, Paint>() {

			@Override
			public Paint transform(V vertex) {
				if (vv.getPickedVertexState().isPicked(vertex)) {
					if (graphCreator.isLeaf(vertex)) {
						return BORDER_INVIS;
					} else {
						return BORDER_INVIS;
					}
				} else {
					if (graphCreator.isLeaf(vertex)) {
						return BORDER_INVIS;
					} else {
						return BORDER_INVIS;
					}
				}
			}
		});
		// VERTEX TOOL TIP
		this.vv.setVertexToolTipTransformer(new Transformer<V, String>() {

			@Override
			public String transform(V vertex) {
				return graphCreator.getVertexToolTip(vertex);
			}
		});
		// VERTEX SHAPE
		vv.getRenderContext().setVertexShapeTransformer(new ExtendedVertexShapeTransformer<>(graphCreator));

		// VERTEX RENDERER
		Renderer.Vertex<V, E> vertexRenderer = graphCreator.getVertexRenderer();
		if (vertexRenderer != null) {
			vv.getRenderer().setVertexRenderer(vertexRenderer);
		}

		// VERTEX LABEL RENDERER
		setDefaultLabelPosition();
		// custom renderer?
		Renderer.VertexLabel<V, E> customVertexLabelRenderer = graphCreator.getVertexLabelRenderer();
		if (customVertexLabelRenderer != null) {
			vv.getRenderer().setVertexLabelRenderer(customVertexLabelRenderer);
		}

		// context
		vv.getRenderContext().setVertexLabelRenderer(new VertexLabelRenderer() {

			private JLabel label = new JLabel();

			@Override
			public <T> Component getVertexLabelRendererComponent(JComponent parent, Object object, Font font,
					boolean isSelection, T vertex) {
				label.setFont(font);
				if (object != null) {
					label.setText(object.toString());
				} else {
					label.setText("");
				}
				return label;
			}

		});
	}
}
