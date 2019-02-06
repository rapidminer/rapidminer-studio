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
package de.dfki.madm.paren.operator.learner.functions.neuralnet;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.gui.actions.export.AbstractPrintableIOObjectPanel;
import com.rapidminer.gui.graphs.GraphViewer;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.learner.functions.neuralnet.InnerNode;
import com.rapidminer.operator.learner.functions.neuralnet.InputNode;
import com.rapidminer.operator.learner.functions.neuralnet.Node;
import com.rapidminer.report.Renderable;
import com.rapidminer.tools.Tools;


/**
 * Visualizes the improved neural net. The nodes can be selected by clicking. The next tool tip will
 * then show the input weights for the selected node.
 *
 * @author Ingo Mierswa, Syed Atif Mehdi
 */
public class AutoMLPImprovedNeuralNetVisualizer extends AbstractPrintableIOObjectPanel implements MouseListener, Renderable {

	private static final long serialVersionUID = 1L;

	private static final int ROW_HEIGHT = 36;

	private static final int LAYER_WIDTH = 150;

	private static final int MARGIN = 30;

	private static final int NODE_RADIUS = 24;

	private static final Font LABEL_FONT = GraphViewer.VERTEX_PLAIN_FONT;

	private static final Font LAYER_FONT = GraphViewer.VERTEX_BOLD_FONT;

	private static final Color NODE_ALMOST_WHITE = new Color(233, 233, 233);

	private AutoMLPImprovedNeuralNetModel neuralNet;

	private int selectedLayerIndex = -1;

	private int selectedRowIndex = -1;

	private double maxAbsoluteWeight = Double.NEGATIVE_INFINITY;

	private String key = null;

	private int keyX = -1;

	private int keyY = -1;

	private String[] attributeNames;

	private Map<Integer, List<Node>> layers = new LinkedHashMap<Integer, List<Node>>();

	public AutoMLPImprovedNeuralNetVisualizer(AutoMLPImprovedNeuralNetModel neuralNet, String[] attributeNames) {
		super(neuralNet, "automlp_improved_neural_net");
		this.neuralNet = neuralNet;
		this.attributeNames = attributeNames;
		addMouseListener(this);

		// calculate maximal absolute weight and store layers
		this.maxAbsoluteWeight = Double.NEGATIVE_INFINITY;

		// add input layer
		List<Node> inputNodes = new ArrayList<Node>();
		for (InputNode inputNode : this.neuralNet.getInputNodes()) {
			inputNodes.add(inputNode);
		}
		this.layers.put(0, inputNodes);

		// add hidden layers
		for (InnerNode innerNode : this.neuralNet.getInnerNodes()) {
			// max weight
			double[] weights = innerNode.getWeights();
			for (double w : weights) {
				this.maxAbsoluteWeight = Math.max(this.maxAbsoluteWeight, Math.abs(w));
			}

			// layer size
			int layerIndex = innerNode.getLayerIndex();
			if (layerIndex != Node.OUTPUT) {
				layerIndex++;
				List<Node> layer = layers.get(layerIndex);
				if (layer == null) {
					layer = new ArrayList<Node>();
					layers.put(layerIndex, layer);
				}
				layer.add(innerNode);
			}
		}

		// add output layer
		int trueLayerIndex = this.layers.size();
		List<Node> outputNodes = new ArrayList<Node>();
		for (InnerNode innerNode : this.neuralNet.getInnerNodes()) {
			int layerIndex = innerNode.getLayerIndex();
			if (layerIndex == Node.OUTPUT) {
				outputNodes.add(innerNode);
			}
		}
		this.layers.put(trueLayerIndex, outputNodes);
	}

	@Override
	public Dimension getPreferredSize() {
		int maxRows = -1;
		for (Map.Entry<Integer, List<Node>> entry : layers.entrySet()) {
			int layerIndex = entry.getKey();
			int nodes = entry.getValue().size();
			if (layerIndex != Node.OUTPUT) {
				nodes++;
			}
			maxRows = Math.max(maxRows, nodes);
		}
		return new Dimension(layers.size() * LAYER_WIDTH + 2 * MARGIN, maxRows * ROW_HEIGHT + 2 * MARGIN);
	}

	@Override
	public void paint(Graphics graphics) {
		graphics.clearRect(0, 0, getWidth(), getHeight());
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, getWidth(), getHeight());

		Dimension dim = getPreferredSize();
		int height = dim.height;

		Graphics2D g = (Graphics2D) graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(LABEL_FONT);
		Graphics2D translated = (Graphics2D) g.create();
		translated.translate(MARGIN, MARGIN);

		Graphics2D synapsesG = (Graphics2D) translated.create();
		paintSynapses(synapsesG, height);
		synapsesG.dispose();

		Graphics2D nodeG = (Graphics2D) translated.create();
		paintNodes(nodeG, height);
		nodeG.dispose();

		translated.dispose();

		// key
		if (key != null) {
			// line.separator does not work for split, transform and use \n
			key = Tools.transformAllLineSeparators(key);
			String[] lines = key.split("\n");
			double maxWidth = Double.NEGATIVE_INFINITY;
			double totalHeight = 0.0d;
			for (String line : lines) {
				Rectangle2D keyBounds = g.getFontMetrics().getStringBounds(line, g);
				maxWidth = Math.max(maxWidth, keyBounds.getWidth());
				totalHeight += keyBounds.getHeight();
			}
			totalHeight += (lines.length - 1) * 3;

			Rectangle frame = new Rectangle(keyX - 4, keyY, (int) maxWidth + 8, (int) totalHeight + 6);
			g.setColor(Color.WHITE);
			g.fill(frame);
			g.setColor(Color.BLACK);
			g.draw(frame);
			g.setColor(Color.BLACK);
			int xPos = keyX;
			int yPos = keyY;
			for (String line : lines) {
				Rectangle2D keyBounds = g.getFontMetrics().getStringBounds(line, g);
				yPos += (int) keyBounds.getHeight();
				g.drawString(line, xPos, yPos);
				yPos += 3;
			}
		}
	}

	private void paintSynapses(Graphics2D g, int height) {
		for (int i = 1; i < layers.size(); i++) {
			int layerIndex = i;
			List<Node> layer = this.layers.get(layerIndex);

			int offset = layerIndex == layers.size() - 1 ? 0 : 1; // last layer no threshold
			int outputY = height / 2 - (layer.size() + offset) * ROW_HEIGHT / 2;
			for (Node node : layer) {
				if (node instanceof InnerNode) {
					Node[] inputNodes = node.getInputNodes();
					double[] weights = ((InnerNode) node).getWeights();

					int inputY = height / 2 - (inputNodes.length + 1) * ROW_HEIGHT / 2;

					for (int j = 0; j < inputNodes.length; j++) {
						float weight = 1.0f - (float) (Math.abs(weights[j + 1]) / this.maxAbsoluteWeight);
						Color color = new Color(weight, weight, weight);
						g.setColor(color);
						g.drawLine(NODE_RADIUS / 2, inputY + NODE_RADIUS / 2, NODE_RADIUS / 2 + LAYER_WIDTH,
								outputY + NODE_RADIUS / 2);
						inputY += ROW_HEIGHT;
					}

					// draw threshold
					float weight = 1.0f - (float) (Math.abs(weights[0]) / this.maxAbsoluteWeight);
					Color color = new Color(weight, weight, weight);
					g.setColor(color);
					g.drawLine(NODE_RADIUS / 2, inputY + NODE_RADIUS / 2, NODE_RADIUS / 2 + LAYER_WIDTH,
							outputY + NODE_RADIUS / 2);
				}
				outputY += ROW_HEIGHT;
			}
			g.translate(LAYER_WIDTH, 0);
			layerIndex++;
		}
	}

	private void paintNodes(Graphics2D g, int height) {
		for (Map.Entry<Integer, List<Node>> entry : layers.entrySet()) {
			int layerIndex = entry.getKey();
			List<Node> layer = entry.getValue();
			int nodes = layer.size();
			if (layerIndex < layers.size() - 1) {
				nodes++;
			}

			String layerName = null;
			if (layerIndex == 0) {
				layerName = "Input";
			} else if (layerIndex == layers.size() - 1) {
				layerName = "Output";
			} else {
				layerName = "Hidden " + layerIndex;
			}

			g.setColor(Color.BLACK);
			g.setFont(LAYER_FONT);
			Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(layerName, g);
			g.drawString(layerName, (int) (-1 * stringBounds.getWidth() / 2 + NODE_RADIUS / 2), 0);
			int yPos = height / 2 - nodes * ROW_HEIGHT / 2;
			for (int r = 0; r < nodes; r++) {
				Shape node = new Ellipse2D.Double(0, yPos, NODE_RADIUS, NODE_RADIUS);
				if (layerIndex == 0 || layerIndex == layers.size() - 1) {
					if (r < nodes - 1 || layerIndex == layers.size() - 1) {
						g.setPaint(SwingTools.makeYellowPaint(NODE_RADIUS, NODE_RADIUS));
					} else {
						g.setPaint(NODE_ALMOST_WHITE);
					}
				} else {
					if (r < nodes - 1) {
						g.setPaint(SwingTools.makeBluePaint(NODE_RADIUS, NODE_RADIUS));
					} else {
						g.setPaint(NODE_ALMOST_WHITE);
					}
				}
				g.fill(node);
				if (layerIndex == this.selectedLayerIndex && r == this.selectedRowIndex) {
					g.setColor(Color.RED);
				} else {
					g.setColor(Color.BLACK);
				}
				g.draw(node);
				yPos += ROW_HEIGHT;
			}
			g.translate(LAYER_WIDTH, 0);
			layerIndex++;
		}
	}

	private void setKey(String key, int keyX, int keyY) {
		this.key = key;
		this.keyX = keyX;
		this.keyY = keyY;
		repaint();
	}

	private void setSelectedNode(int layerIndex, int rowIndex, int xPos, int yPos) {
		this.selectedLayerIndex = layerIndex;
		this.selectedRowIndex = rowIndex;

		if (this.selectedLayerIndex < 0 || this.selectedRowIndex < 0) {
			setKey(null, -1, -1);
			return;
		}

		if (layerIndex == 0) { // input layer
			if (rowIndex >= 0 && rowIndex < this.attributeNames.length) {
				setKey(this.attributeNames[rowIndex], xPos, yPos);
			} else {
				if (rowIndex == this.attributeNames.length) {
					setKey("Threshold Node", xPos, yPos);
				} else {
					setKey(null, -1, -1);
				}
			}
		} else {
			List<Node> currentLayer = layers.get(selectedLayerIndex);
			if (rowIndex >= 0 && rowIndex < currentLayer.size()) {
				StringBuffer toolTip = new StringBuffer("Weights:" + Tools.getLineSeparator());
				Node node = currentLayer.get(this.selectedRowIndex);
				if (node instanceof InnerNode) {
					InnerNode innerNode = (InnerNode) node;
					double[] weights = innerNode.getWeights();
					for (int w = 1; w < weights.length; w++) {
						toolTip.append(Tools.formatNumber(weights[w]) + Tools.getLineSeparator());
					}
					toolTip.append(Tools.formatNumber(weights[0]) + " (Threshold)");
				}
				setKey(toolTip.toString(), xPos, yPos);
			} else {
				setKey("Threshold Node", xPos, yPos);
			}
		}

		repaint();
	}

	private void checkMousePos(int xPos, int yPos) {
		int x = xPos - MARGIN;
		int y = yPos - MARGIN;
		int layerIndex = x / LAYER_WIDTH;
		int layerMod = x % LAYER_WIDTH;
		boolean layerHit = layerMod > 0 && layerMod < NODE_RADIUS;
		if (layerHit && layerIndex >= 0 && layerIndex < this.layers.size()) {
			List<Node> layer = layers.get(layerIndex);
			int rows = layer.size();
			if (layerIndex < layers.size() - 1) {
				rows++;
			}
			int yMargin = getPreferredSize().height / 2 - rows * ROW_HEIGHT / 2;
			if (y > yMargin) {
				for (int i = 0; i < rows; i++) {
					if (y > yMargin && y < yMargin + NODE_RADIUS) {
						if (this.selectedLayerIndex == layerIndex && this.selectedRowIndex == i) {
							setSelectedNode(-1, -1, -1, -1);
						} else {
							setSelectedNode(layerIndex, i, xPos, yPos);
						}
						return;
					}
					yMargin += ROW_HEIGHT;
				}
			}
		}
		setSelectedNode(-1, -1, -1, -1);
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {
		int xPos = e.getX();
		int yPos = e.getY();
		checkMousePos(xPos, yPos);
	}

	@Override
	public void prepareRendering() {}

	@Override
	public void finishRendering() {}

	@Override
	public int getRenderHeight(int preferredHeight) {
		int height = getPreferredSize().height;
		if (height < 1) {
			height = preferredHeight;
		}
		if (preferredHeight > height) {
			height = preferredHeight;
		}
		return height;
	}

	@Override
	public int getRenderWidth(int preferredWidth) {
		int width = getPreferredSize().width;
		if (width < 1) {
			width = preferredWidth;
		}
		if (preferredWidth > width) {
			width = preferredWidth;
		}
		return width;
	}

	@Override
	public void render(Graphics graphics, int width, int height) {
		setSize(width, height);
		paint(graphics);
	}

	@Override
	public Component getExportComponent() {
		return this;
	}
}
