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
package com.rapidminer.gui.graphs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;

import org.apache.commons.collections15.Factory;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.viewer.AssociationRuleFilter;
import com.rapidminer.gui.viewer.AssociationRuleFilterListener;
import com.rapidminer.operator.learner.associations.AssociationRule;
import com.rapidminer.operator.learner.associations.AssociationRules;
import com.rapidminer.operator.learner.associations.Item;
import com.rapidminer.tools.Tools;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;


/**
 * Creates a graph model for a set of association rules.
 *
 * @author Ingo Mierswa
 */
public class AssociationRulesGraphCreator extends GraphCreatorAdaptor implements AssociationRuleFilterListener {

	private final Factory<String> edgeFactory = new Factory<String>() {

		int i = 0;

		@Override
		public String create() {
			return "E" + i++;
		}
	};

	private final AssociationRules rules;

	private final Map<String, String> toolTipInfos = new HashMap<String, String>();

	private final List<String> nodeList = new LinkedList<String>();

	private final List<String> edgeList = new LinkedList<String>();

	private final Map<String, List<String>> asPremise = new HashMap<String, List<String>>();

	private final Map<String, List<String>> asConclusion = new HashMap<String, List<String>>();

	private final AssociationRuleFilter filter;

	private DirectedSparseGraph<String, String> graph;

	private GraphViewer<String, String> viewer;

	public AssociationRulesGraphCreator(AssociationRules rules) {
		this.rules = rules;
		this.filter = new AssociationRuleFilter(rules);
		this.filter.setBorder(BorderFactory.createTitledBorder("Filter"));
		this.filter.addAssociationRuleFilterListener(this);
	}

	@Override
	public Graph<String, String> createGraph() {
		graph = new DirectedSparseGraph<String, String>();
		boolean[] allFilter = new boolean[rules.getNumberOfRules()];
		for (int i = 0; i < allFilter.length; i++) {
			allFilter[i] = true;
		}
		addRuleNodes(allFilter);
		return graph;
	}

	private void addRuleNodes(boolean[] filter) {
		Iterator<String> e = edgeList.iterator();
		while (e.hasNext()) {
			graph.removeEdge(e.next());
		}

		Iterator<String> n = nodeList.iterator();
		while (n.hasNext()) {
			graph.removeVertex(n.next());
		}

		edgeList.clear();
		nodeList.clear();

		toolTipInfos.clear();
		asPremise.clear();
		asConclusion.clear();

		int ruleIndex = 1;
		for (int r = 0; r < rules.getNumberOfRules(); r++) {
			if (filter[r]) {
				AssociationRule rule = rules.getRule(r);

				// define conjunction node
				String conjunctionNode = "Rule " + ruleIndex + " (" + Tools.formatNumber(rule.getTotalSupport()) + " / "
						+ Tools.formatNumber(rule.getConfidence()) + ")";
				toolTipInfos.put(
						conjunctionNode,
						"<html><b>Rule " + ruleIndex + "</b><br>"
								+ SwingTools.addLinebreaks(rule.toPremiseString() + " --> " + rule.toConclusionString())
								+ "<br><b>Support:</b> " + rule.getTotalSupport() + "<br><b>Confidence:</b> "
								+ rule.getConfidence() + "<br><b>Lift:</b> " + rule.getLift() + "<br><b>Gain:</b> "
								+ rule.getGain() + "<br><b>Conviction:</b> " + rule.getConviction() + "<br><b>Laplace:</b> "
								+ rule.getLaplace() + "<br><b>Ps:</b> " + rule.getPs() + "</html>");
				nodeList.add(conjunctionNode);

				// add premise nodes
				Iterator<Item> p = rule.getPremiseItems();
				while (p.hasNext()) {
					Item premiseItem = p.next();
					String edgeId = edgeFactory.create();
					edgeList.add(edgeId);
					nodeList.add(premiseItem.toString());
					graph.addEdge(edgeId, premiseItem.toString(), conjunctionNode);
					List<String> premiseList = asPremise.get(premiseItem.toString());
					if (premiseList == null) {
						premiseList = new LinkedList<String>();
						asPremise.put(premiseItem.toString(), premiseList);
					}
					premiseList.add("Rule " + ruleIndex);
				}

				// add conclusion nodes
				Iterator<Item> c = rule.getConclusionItems();
				while (c.hasNext()) {
					Item conclusionItem = c.next();
					String edgeId = edgeFactory.create();
					edgeList.add(edgeId);
					nodeList.add(conclusionItem.toString());
					graph.addEdge(edgeId, conjunctionNode, conclusionItem.toString());
					List<String> conclusionList = asConclusion.get(conclusionItem.toString());
					if (conclusionList == null) {
						conclusionList = new LinkedList<String>();
						asConclusion.put(conclusionItem.toString(), conclusionList);
					}
					conclusionList.add("Rule " + ruleIndex);
				}
			}
			ruleIndex++;
		}
	}

	/** Returns true for rule nodes. */
	@Override
	public boolean isBold(String id) {
		return toolTipInfos.get(id) == null;
	}

	/** Returns true for rule nodes. */
	@Override
	public boolean isLeaf(String id) {
		return toolTipInfos.get(id) != null;
	}

	/** Returns null. */
	@Override
	public String getVertexToolTip(String id) {
		String toolTip = toolTipInfos.get(id);
		if (toolTip != null) {
			return toolTip;
		} else {
			return "<html><b>Item:</b> " + id + "<br><b>Premise in Rules:</b> "
					+ (asPremise.get(id) == null || asPremise.get(id).size() == 0 ? "none" : asPremise.get(id))
					+ "<br><b>Conclusion in Rules:</b> "
					+ (asConclusion.get(id) == null || asConclusion.get(id).size() == 0 ? "none" : asConclusion.get(id))
					+ "</html>";
		}
	}

	@Override
	public String getEdgeName(String id) {
		return null;
	}

	@Override
	public String getVertexName(String id) {
		return id;
	}

	/**
	 * Returns the label offset. In most case, using -1 is just fine (default offset). Some tree
	 * like graphs might prefer to use 0 since they manage the offset themself.
	 */
	@Override
	public int getLabelOffset() {
		return -1;
	}

	/** Returns false. */
	@Override
	public boolean showEdgeLabelsDefault() {
		return false;
	}

	/** Returns false. */
	@Override
	public boolean showVertexLabelsDefault() {
		return true;
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
	public int getNumberOfOptionComponents() {
		return 1;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JComponent getOptionComponent(final GraphViewer viewer, int index) {
		if (index == 0) {
			this.viewer = viewer;
			return filter;
		} else {
			return null;
		}
	}

	@Override
	public void setFilter(boolean[] filter) {
		addRuleNodes(filter);
		viewer.updateLayout();
	}
}
