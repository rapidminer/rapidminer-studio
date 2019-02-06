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

import java.util.Collection;
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

	private final Map<String, String> toolTipInfos = new HashMap<>();

	private final List<String> nodeList = new LinkedList<>();

	private final List<String> edgeList = new LinkedList<>();

	private final Map<String, List<String>> asPremise = new HashMap<>();

	private final Map<String, List<String>> asConclusion = new HashMap<>();

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
				toolTipInfos.put(conjunctionNode, createTooltip(rule, ruleIndex));
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
			return createTooltip(id);
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
	public JComponent getOptionComponent(final GraphViewer<?, ?> viewer, int index) {
		if (index == 0) {
			this.viewer = (GraphViewer<String, String>) viewer;
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

	/**
	 * Create the tooltip for a rule.
	 *
	 * @param id
	 *            the id of a rule for which to create the tooltip
	 * @return the HTML-formatted tooltip string
	 */
	private String createTooltip(String id) {
		StringBuilder sb = new StringBuilder();
		sb.append(
				"<html><div style=\"font-size: 10px; font-family: 'Open Sans'\"><p style=\"font-size: 110%; text-align: center; font-family: 'Open Sans Semibold'\"><b>"
						+ id + "</b><hr NOSHADE style=\"color: '#000000'; width: 95%; \"/></p><br/>");
		sb.append("Premise:&nbsp;"
				+ (asPremise.get(id) == null || asPremise.get(id).size() == 0 ? "-" : formatDependencies(asPremise.get(id)))
				+ "<br/>");
		sb.append("Conclusion:&nbsp;" + (asConclusion.get(id) == null || asConclusion.get(id).size() == 0 ? "-"
				: formatDependencies(asConclusion.get(id))));
		sb.append("</div></html>");

		return sb.toString();
	}

	/**
	 * Create the tooltip for a rule.
	 *
	 * @param rule
	 *            the rule for which to create the tooltip
	 * @param ruleIndex
	 *            the index of the rule
	 * @return the HTML-formatted tooltip string
	 */
	private String createTooltip(AssociationRule rule, int ruleIndex) {
		StringBuilder sb = new StringBuilder();

		sb.append("<html><div style=\"font-size: 10px; font-family: 'Open Sans'\">");
		sb.append("<p style=\"font-size: 110%; text-align: center; font-family: 'Open Sans Semibold'\"><b>Rule " + ruleIndex
				+ "</b><hr NOSHADE style=\"color: '#000000'; width: 95%; \"/></p>");
		sb.append(SwingTools.transformToolTipText(
				formatDependencies(rule.getPremiseItems()) + " \u2794 " + formatDependencies(rule.getConclusionItems()),
				false, 200, false, false) + "<br/>");
		sb.append("Support:&nbsp;" + Tools.formatNumber(rule.getTotalSupport(), 2) + "<br/>");
		sb.append("Confidence:&nbsp;" + Tools.formatNumber(rule.getConfidence(), 2) + "<br/>");
		sb.append("Lift:&nbsp;" + Tools.formatNumber(rule.getLift(), 2) + "<br/>");
		sb.append("Gain:&nbsp;" + Tools.formatNumber(rule.getGain(), 2) + "<br/>");
		sb.append("Conviction:&nbsp;" + Tools.formatNumber(rule.getConviction(), 2) + "<br/>");
		sb.append("Laplace:&nbsp;" + Tools.formatNumber(rule.getLaplace(), 2) + "<br/>");
		sb.append("Ps:&nbsp;" + Tools.formatNumber(rule.getPs(), 2));
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
	private static String formatDependencies(Iterator<Item> dependencyIterator) {
		StringBuilder sb = new StringBuilder();

		while (dependencyIterator.hasNext()) {
			sb.append("<i>");
			sb.append(dependencyIterator.next());
			sb.append("</i>");
			if (dependencyIterator.hasNext()) {
				sb.append(',').append(' ');
			}
		}

		return sb.toString();
	}

	/**
	 * Displays the rule premise/conclusion list contents nicely formatted for humans.
	 *
	 * @param dependencyList
	 *            the list of premises/conclusions of the rule to format
	 * @return the formatted string
	 */
	private static String formatDependencies(Collection<String> dependencyList) {
		StringBuilder sb = new StringBuilder();

		int size = dependencyList.size();
		for (String item : dependencyList) {
			sb.append("<i>");
			sb.append(item);
			sb.append("</i>");
			if (--size > 0) {
				sb.append(',').append(' ');
			}
		}

		return sb.toString();
	}
}
