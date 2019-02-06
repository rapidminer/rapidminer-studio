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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Factory;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.renderers.Renderer.EdgeLabel;


/**
 * The graph model creator for similarity measurements.
 *
 * @author Ingo Mierswa
 */
public class SimilarityGraphCreator extends GraphCreatorAdaptor {

	private Factory<String> edgeFactory = new Factory<String>() {

		int i = 0;

		@Override
		public String create() {
			return "E" + i++;
		}
	};

	private JSlider distanceSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 1000, 100) {

		private static final long serialVersionUID = -6931545310805789589L;

		@Override
		public Dimension getMinimumSize() {
			return new Dimension(40, (int) super.getMinimumSize().getHeight());
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(40, (int) super.getPreferredSize().getHeight());
		}

		@Override
		public Dimension getMaximumSize() {
			return new Dimension(40, (int) super.getMaximumSize().getHeight());
		}
	};

	private Graph<String, String> graph;

	private DistanceMeasure measure;

	private ExampleSet exampleSet;

	private Map<String, String> edgeLabelMap = new HashMap<>();

	private Map<String, Double> edgeStrengthMap = new HashMap<>();

	private Map<String, List<Pair<String, Double>>> adjacencyMap = new HashMap<>();

	private DefaultObjectViewer objectViewer;

	public SimilarityGraphCreator(DistanceMeasure measure, ExampleSet exampleSet) {
		this.measure = measure;
		this.exampleSet = exampleSet;
		objectViewer = new DefaultObjectViewer(exampleSet);
	}

	@Override
	public Graph<String, String> createGraph() {
		graph = new UndirectedSparseGraph<String, String>();

		Attribute id = exampleSet.getAttributes().getId();
		if (id != null) {
			for (Example example : exampleSet) {
				graph.addVertex(example.getValueAsString(id));
			}
			addEdges();
		}
		return graph;
	}

	@Override
	public String getEdgeName(String id) {
		return edgeLabelMap.get(id);
	}

	@Override
	public String getVertexName(String id) {
		return id;
	}

	@Override
	public String getVertexToolTip(String id) {
		return createTooltip(id);
	}

	/**
	 * Returns the label offset. In most case, using -1 is just fine (default offset). Some tree
	 * like graphs might prefer to use 0 since they manage the offset themself.
	 */
	@Override
	public int getLabelOffset() {
		return -1;
	}

	@Override
	public int getNumberOfOptionComponents() {
		return 2;
	}

	@Override
	public JComponent getOptionComponent(final GraphViewer<?, ?> viewer, int index) {
		if (index == 0) {
			return new JLabel("Number of Edges:");
		} else if (index == 1) {
			this.distanceSlider.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					if (!distanceSlider.getValueIsAdjusting()) {
						addEdges();
						viewer.updateLayout();
					}
				}
			});
			return distanceSlider;
		} else {
			return null;
		}
	}

	private void addEdges() {
		// remove old edges if available
		Iterator<String> e = edgeLabelMap.keySet().iterator();
		while (e.hasNext()) {
			graph.removeEdge(e.next());
		}
		edgeLabelMap.clear();
		adjacencyMap.clear();

		boolean isDistance = measure.isDistance();
		Attribute id = exampleSet.getAttributes().getId();
		List<SortableEdge> sortableEdges = new LinkedList<SortableEdge>();
		for (int i = 0; i < exampleSet.size(); i++) {
			Example example = exampleSet.getExample(i);
			for (int j = i + 1; j < exampleSet.size(); j++) {
				Example comExample = exampleSet.getExample(j);
				if (isDistance) {
					sortableEdges.add(new SortableEdge(example.getValueAsString(id), comExample.getValueAsString(id), null,
							measure.calculateDistance(example, comExample), SortableEdge.DIRECTION_INCREASE));
				} else {
					sortableEdges.add(new SortableEdge(example.getValueAsString(id), comExample.getValueAsString(id), null,
							measure.calculateSimilarity(example, comExample), SortableEdge.DIRECTION_DECREASE));
				}
			}
		}

		Collections.sort(sortableEdges);

		int numberOfEdges = distanceSlider.getValue();
		int counter = 0;
		double minStrength = Double.POSITIVE_INFINITY;
		double maxStrength = Double.NEGATIVE_INFINITY;
		Map<String, Double> strengthMap = new HashMap<String, Double>();
		for (SortableEdge sortableEdge : sortableEdges) {
			if (counter > numberOfEdges) {
				break;
			}

			String idString = edgeFactory.create();
			graph.addEdge(idString, sortableEdge.getFirstVertex(), sortableEdge.getSecondVertex(), EdgeType.UNDIRECTED);
			edgeLabelMap.put(idString, Tools.formatIntegerIfPossible(sortableEdge.getEdgeValue()));

			List<Pair<String, Double>> edgesFirstList = adjacencyMap.get(sortableEdge.getFirstVertex());
			if (edgesFirstList == null) {
				edgesFirstList = new ArrayList<>();
				adjacencyMap.put(sortableEdge.getFirstVertex(), edgesFirstList);
			}
			edgesFirstList.add(new Pair<>(sortableEdge.getSecondVertex(), sortableEdge.getEdgeValue()));
			List<Pair<String, Double>> edgesSecondList = adjacencyMap.get(sortableEdge.getSecondVertex());
			if (edgesSecondList == null) {
				edgesSecondList = new ArrayList<>();
				adjacencyMap.put(sortableEdge.getSecondVertex(), edgesSecondList);
			}
			edgesSecondList.add(new Pair<>(sortableEdge.getFirstVertex(), sortableEdge.getEdgeValue()));

			double strength = sortableEdge.getEdgeValue();

			minStrength = Math.min(minStrength, strength);
			maxStrength = Math.max(maxStrength, strength);

			strengthMap.put(idString, strength);

			counter++;
		}

		for (Entry<String, Double> entry : strengthMap.entrySet()) {
			edgeStrengthMap.put(entry.getKey(), (entry.getValue() - minStrength) / (maxStrength - minStrength));
		}
	}

	/** Returns true. */
	@Override
	public boolean showEdgeLabelsDefault() {
		return true;
	}

	/** Returns false. */
	@Override
	public boolean showVertexLabelsDefault() {
		return false;
	}

	@Override
	public double getEdgeStrength(String id) {
		Double value = edgeStrengthMap.get(id);
		if (value == null) {
			return 1.0d;
		} else {
			if (Double.isNaN(value)) {
				return 1.0d;
			} else {
				return value;
			}
		}
	}

	/** Returns the shape of the edges. */
	@Override
	public int getEdgeShape() {
		return EDGE_SHAPE_QUAD_CURVE;
	}

	@Override
	public Object getObject(String id) {
		return id;
	}

	@Override
	public GraphObjectViewer getObjectViewer() {
		return objectViewer;
	}

	@Override
	public EdgeLabel<String, String> getEdgeLabelRenderer() {
		return new TreeModelEdgeLabelRenderer<String, String>();
	}

	/**
	 * Create the tooltip for a node.
	 *
	 * @param id
	 *            the id of the node for which to create the tooltip
	 * @return the HTML-formatted tooltip string
	 */
	private String createTooltip(String id) {
		StringBuilder sb = new StringBuilder();

		sb.append("<html><div style=\"font-size: 10px; font-family: 'Open Sans'\">");
		sb.append("<p style=\"font-size: 110%; text-align: center; font-family: 'Open Sans Semibold'\"><b>" + id
				+ "</b><hr NOSHADE style=\"color: '#000000'; width: 95%; \"/></p><br/>");
		sb.append("Top adjacent items:&nbsp;"
				+ SwingTools.transformToolTipText(formatAdjacencyMap(adjacencyMap.get(id)), false, 200, false, false));
		sb.append("</div></html>");

		return sb.toString();
	}

	/**
	 * Displays the rule premise/conclusion list contents nicely formatted for humans.
	 *
	 * @param dependencyList
	 *            the list of premises/conclusions of the rule to format
	 * @return the formatted string
	 */
	private String formatAdjacencyMap(List<Pair<String, Double>> list) {
		StringBuilder sb = new StringBuilder();

		if (list == null) {
			sb.append('-');
			return sb.toString();
		}

		int counter = 0;
		for (Pair<String, Double> edge : list) {
			sb.append("<br/>");
			sb.append(edge.getFirst());
			sb.append(' ');
			sb.append('-');
			sb.append(' ');
			sb.append("<i>");
			sb.append(Tools.formatNumber(edge.getSecond(), 2));
			sb.append("</i>");
			if (counter++ >= 2) {
				break;
			}
		}

		return sb.toString();
	}
}
