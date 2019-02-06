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
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJComboBox;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.visualization.dependencies.TransitionGraph;
import com.rapidminer.tools.ObjectVisualizerService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.renderers.Renderer.EdgeLabel;


/**
 * The graph model creator for transition graphs.
 *
 * @author Ingo Mierswa
 */
public class TransitionGraphCreator extends GraphCreatorAdaptor {

	private static class SourceId implements Comparable<SourceId> {

		private final String id;

		private final String label;

		public SourceId(String id, String label) {
			this.id = id;
			this.label = label;
		}

		public String getId() {
			return id;
		}

		@Override
		public String toString() {
			return label;
		}

		@Override
		public int compareTo(SourceId o) {
			return this.label.compareTo(o.label);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			SourceId other = (SourceId) obj;
			if (label == null) {
				if (other.label != null) {
					return false;
				}
			} else if (!label.equals(other.label)) {
				return false;
			}
			return true;
		}

	}

	private final Factory<String> edgeFactory = new Factory<String>() {

		int i = 0;

		@Override
		public String create() {
			return "E" + i++;
		}
	};

	private final JSlider edgeSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 1000, 100) {

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

	private final JComboBox<SourceId> sourceFilter;

	private final JSpinner numberOfHops;

	private Graph<String, String> graph;

	private final Attribute sourceAttribute;

	private final Attribute targetAttribute;

	private Attribute strengthAttribute;

	private Attribute typeAttribute;

	private final String nodeDescription;

	private final ExampleSet exampleSet;

	private final Map<String, String> edgeLabelMap = new HashMap<>();

	private final Map<String, Double> edgeStrengthMap = new HashMap<>();

	private final Map<String, String> vertexLabelMap = new HashMap<>();

	private Map<String, List<Pair<String, Double>>> adjacencyMap = new HashMap<>();

	private final DefaultObjectViewer objectViewer;

	public TransitionGraphCreator(TransitionGraph transitionGraph, ExampleSet exampleSet) {
		this.sourceAttribute = exampleSet.getAttributes().get(transitionGraph.getSourceAttribute());
		this.targetAttribute = exampleSet.getAttributes().get(transitionGraph.getTargetAttribute());
		if (transitionGraph.getStrengthAttribute() != null) {
			this.strengthAttribute = exampleSet.getAttributes().get(transitionGraph.getStrengthAttribute());
		}
		if (transitionGraph.getTypeAttribute() != null) {
			this.typeAttribute = exampleSet.getAttributes().get(transitionGraph.getTypeAttribute());
		}
		this.exampleSet = exampleSet;
		this.nodeDescription = transitionGraph.getNodeDescription();

		SortedSet<SourceId> sourceNames = new TreeSet<SourceId>();
		for (Example example : exampleSet) {
			Object id = example.getValue(sourceAttribute);
			if (sourceAttribute.isNominal()) {
				id = example.getValueAsString(sourceAttribute);
			}
			String description = getNodeDescription(id);
			if (description == null) {
				sourceNames.add(new SourceId(id.toString(), id.toString()));
			} else {
				sourceNames.add(new SourceId(id.toString(), description));
			}

		}

		sourceFilter = new ExtendedJComboBox<>(200);
		sourceFilter.putClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK, true);
		sourceFilter.setPreferredSize(
				new Dimension(sourceFilter.getPreferredSize().width, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		sourceFilter.addItem(new SourceId("None", "None"));
		for (SourceId sourceId : sourceNames) {
			sourceFilter.addItem(sourceId);
		}

		this.numberOfHops = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
		this.numberOfHops.setPreferredSize(
				new Dimension(this.numberOfHops.getPreferredSize().width, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));

		if (exampleSet.getAttributes().getId() != null) {
			objectViewer = new DefaultObjectViewer(exampleSet);
		} else {
			objectViewer = null;
		}
	}

	@Override
	public Graph<String, String> createGraph() {
		graph = new DirectedSparseGraph<String, String>();
		updateGraph();
		return graph;
	}

	@Override
	public String getEdgeName(String id) {
		return edgeLabelMap.get(id);
	}

	@Override
	public String getVertexName(String id) {
		String storedName = vertexLabelMap.get(id);
		if (storedName == null) {
			return id;
		} else {
			return storedName;
		}
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
		return 6;
	}

	@Override
	public JComponent getOptionComponent(final GraphViewer<?, ?> viewer, int index) {
		if (index == 0) {
			return new JLabel("Source Filter:");
		} else if (index == 1) {
			sourceFilter.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					updateGraph();
					viewer.updateLayout();
				}
			});
			return sourceFilter;
		} else if (index == 2) {
			return new JLabel("Number of Hops:");
		} else if (index == 3) {
			this.numberOfHops.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					updateGraph();
					viewer.updateLayout();
				}
			});
			return numberOfHops;

		} else if (index == 4) {
			return new JLabel("Number of Edges:");
		} else if (index == 5) {
			this.edgeSlider.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					if (!edgeSlider.getValueIsAdjusting()) {
						updateGraph();
						viewer.updateLayout();
					}
				}
			});
			return edgeSlider;
		} else {
			return null;
		}
	}

	private void updateGraph() {
		// remove old edges if available
		Iterator<String> e = edgeLabelMap.keySet().iterator();
		while (e.hasNext()) {
			graph.removeEdge(e.next());
		}
		edgeLabelMap.clear();
		edgeStrengthMap.clear();
		adjacencyMap.clear();

		// remove old vertices if available
		Iterator<String> v = vertexLabelMap.keySet().iterator();
		while (v.hasNext()) {
			graph.removeVertex(v.next());
		}
		vertexLabelMap.clear();

		String sourceFilterName = null;
		if (sourceFilter.getSelectedIndex() > 0) {
			sourceFilterName = ((SourceId) sourceFilter.getSelectedItem()).getId();
		}

		List<SortableEdge> sortableEdges = new LinkedList<SortableEdge>();
		if (sourceFilterName == null) {
			for (Example example : exampleSet) {
				String source = example.getValueAsString(sourceAttribute);
				String target = example.getValueAsString(targetAttribute);

				double strength = 1.0d;
				if (strengthAttribute != null) {
					strength = example.getValue(strengthAttribute);
				}

				String type = null;
				if (typeAttribute != null) {
					type = example.getValueAsString(typeAttribute);
				}

				String edgeName = null;
				if (type != null) {
					edgeName = type;
				} else {
					edgeName = strength + "";
				}

				sortableEdges.add(new SortableEdge(source, target, edgeName, strength, SortableEdge.DIRECTION_INCREASE));
			}
		} else {
			List<String> sources = new LinkedList<String>();
			sources.add(sourceFilterName);
			int hop = 1;
			int maxHops = (Integer) numberOfHops.getValue();

			do {
				List<String> newSources = new LinkedList<String>();
				for (String currentSourceFilterName : sources) {
					for (Example example : exampleSet) {
						String source = example.getValueAsString(sourceAttribute);
						if (currentSourceFilterName != null) {
							if (!currentSourceFilterName.equals(source)) {
								continue;
							}
						}

						String target = example.getValueAsString(targetAttribute);

						double strength = 1.0d;
						if (strengthAttribute != null) {
							strength = example.getValue(strengthAttribute);
						}

						String type = null;
						if (typeAttribute != null) {
							type = example.getValueAsString(typeAttribute);
						}

						String edgeName = null;
						if (type != null) {
							edgeName = type;
						} else {
							edgeName = strength + "";
						}

						sortableEdges
								.add(new SortableEdge(source, target, edgeName, strength, SortableEdge.DIRECTION_INCREASE));

						newSources.add(target);
					}
				}
				sources.clear();
				hop++;
				if (hop > maxHops) {
					sources = null;
				} else {
					sources = newSources;
				}
			} while (sources != null);
		}

		Collections.sort(sortableEdges);

		// determine used vertices
		Set<String> allVertices = new HashSet<String>();
		int numberOfEdges = edgeSlider.getValue();
		int counter = 0;
		for (SortableEdge sortableEdge : sortableEdges) {
			if (counter > numberOfEdges) {
				break;
			}

			allVertices.add(sortableEdge.getFirstVertex());
			allVertices.add(sortableEdge.getSecondVertex());

			counter++;
		}

		// add all used vertices to graph
		for (String vertex : allVertices) {
			graph.addVertex(vertex);

			String description = getNodeDescription(vertex);
			if (description == null) {
				vertexLabelMap.put(vertex, vertex);
			} else {
				vertexLabelMap.put(vertex, description);
			}
		}

		counter = 0;
		double minStrength = Double.POSITIVE_INFINITY;
		double maxStrength = Double.NEGATIVE_INFINITY;
		Map<String, Double> strengthMap = new HashMap<String, Double>();
		for (SortableEdge sortableEdge : sortableEdges) {
			if (counter > numberOfEdges) {
				break;
			}

			String idString = edgeFactory.create();
			graph.addEdge(idString, sortableEdge.getFirstVertex(), sortableEdge.getSecondVertex(), EdgeType.DIRECTED);
			edgeLabelMap.put(idString, Tools.formatNumber(sortableEdge.getEdgeValue(), 2));

			List<Pair<String, Double>> edgesFirstList = adjacencyMap.get(sortableEdge.getFirstVertex());
			if (edgesFirstList == null) {
				edgesFirstList = new ArrayList<>();
				adjacencyMap.put(sortableEdge.getFirstVertex(), edgesFirstList);
			}
			Pair<String, Double> pair = new Pair<>(sortableEdge.getSecondVertex(), sortableEdge.getEdgeValue());
			addIfKeyNotYetExists(edgesFirstList, pair);
			List<Pair<String, Double>> edgesSecondList = adjacencyMap.get(sortableEdge.getSecondVertex());
			if (edgesSecondList == null) {
				edgesSecondList = new ArrayList<>();
				adjacencyMap.put(sortableEdge.getSecondVertex(), edgesSecondList);
			}
			pair = new Pair<>(sortableEdge.getFirstVertex(), sortableEdge.getEdgeValue());
			addIfKeyNotYetExists(edgesSecondList, pair);

			double strength = sortableEdge.getEdgeValue();

			minStrength = Math.min(minStrength, strength);
			maxStrength = Math.max(maxStrength, strength);

			strengthMap.put(idString, strength);

			counter++;
		}

		for (Entry<String, Double> entry : strengthMap.entrySet()) {
			edgeStrengthMap.put(entry.getKey(),
					(strengthMap.get(entry.getKey()) - minStrength) / (maxStrength - minStrength));
		}
	}

	private String getNodeDescription(Object vertexId) {
		if (nodeDescription != null) {
			ObjectVisualizer visualizer = ObjectVisualizerService.getVisualizerForObject(exampleSet);

			if (visualizer != null) {
				if (visualizer.isCapableToVisualize(vertexId)) {
					StringBuffer resultString = new StringBuffer();
					int currentIndex = 0;
					int startIndex = nodeDescription.indexOf("%{", currentIndex);
					while (startIndex >= currentIndex) {
						int endIndex = nodeDescription.indexOf("}", startIndex);
						if (endIndex >= startIndex) {
							String fieldName = nodeDescription.substring(startIndex + 2, endIndex);
							String fieldValue = visualizer.getDetailData(vertexId, fieldName);
							resultString.append(nodeDescription.substring(currentIndex, startIndex));
							if (fieldValue != null) {
								resultString.append(fieldValue);
							} else {
								resultString.append("?");
							}
							currentIndex = endIndex + 1;
						} else {
							resultString.append(nodeDescription.substring(startIndex));
							currentIndex = nodeDescription.length();
						}

						startIndex = nodeDescription.indexOf("%{", currentIndex);
					}

					if (currentIndex < nodeDescription.length()) {
						resultString.append(nodeDescription.substring(currentIndex));
					}

					return resultString.toString();
				}
			}
		}
		return null;
	}

	@Override
	public Transformer<String, Paint> getVertexPaintTransformer(VisualizationViewer<String, String> viewer) {
		return new Transformer<String, Paint>() {

			@Override
			public Paint transform(String name) {
				if (viewer.getPickedVertexState().isPicked(name)) {
					return GraphViewer.NODE_SELECTED;
				} else if (sourceFilter.getSelectedIndex() > 0
						&& ((SourceId) sourceFilter.getSelectedItem()).getId().equals(name)) {
					return GraphViewer.NODE_ON_PATH;
				} else {
					return GraphViewer.NODE_BACKGROUND;
				}
			}
		};
	}

	/** Returns true. */
	@Override
	public boolean showEdgeLabelsDefault() {
		return true;
	}

	/** Returns true. */
	@Override
	public boolean showVertexLabelsDefault() {
		return true;
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

	/**
	 * Adds the given {@link Pair} if the key of the pair does not yet exist in any pair of the
	 * given list.
	 * 
	 * @param list
	 *            the list which must not contain the key of the given pair
	 * @param pair
	 *            the pair to potentially insert
	 */
	private static void addIfKeyNotYetExists(List<Pair<String, Double>> list, Pair<String, Double> pair) {
		if (pair.getFirst() == null) {
			return;
		}

		for (Pair<String, Double> p : list) {
			if (pair.getFirst().equals(p.getFirst())) {
				return;
			}
		}

		list.add(pair);
	}
}
