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
package com.rapidminer.gui.viewer.metadata.actions;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.viewer.metadata.model.AbstractAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.DateTimeAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.MetaDataStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.NominalAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.NumericalAttributeStatisticsModel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;

import java.awt.event.ActionEvent;
import java.util.List;


/**
 * The action copies all meta data into the clipboard, separating each column via <code>\t</code> so
 * pasting into for example Excel is possible.
 * 
 * @author Marco Boeck
 * 
 */
public class CopyAllMetaDataToClipboardAction extends ResourceAction {

	private static final long serialVersionUID = 6979404131032484600L;

	/** the {@link MetaDataStatisticsModel} model from which the meta data should be copied */
	MetaDataStatisticsModel model;

	/**
	 * Creates a new {@link CopyAllMetaDataToClipboardAction} instance.
	 * 
	 * @param model
	 */
	public CopyAllMetaDataToClipboardAction(MetaDataStatisticsModel model) {
		super(true, "meta_data_stats.copy_all_metadata");
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		StringBuilder sb = new StringBuilder();
		for (AbstractAttributeStatisticsModel statModel : model.getOrderedAttributeStatisticsModels()) {
			// append general stats like name, type, missing values
			sb.append(statModel.getAttribute().getName());
			appendTab(sb);

			String valueTypeString = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(statModel.getAttribute().getValueType());
			valueTypeString = valueTypeString.replaceAll("_", " ");
			valueTypeString = String.valueOf(valueTypeString.charAt(0)).toUpperCase() + valueTypeString.substring(1);
			sb.append(valueTypeString);
			appendTab(sb);

			sb.append(Tools.formatIntegerIfPossible(statModel.getNumberOfMissingValues()));
			appendTab(sb);

			// if construction is shown, also add it
			if (statModel.isShowConstruction()) {
				String construction = statModel.getConstruction();
				construction = construction == null ? "-" : construction;
				sb.append(construction);
				appendTab(sb);
			}

			// append value type specific stuff
			if (NumericalAttributeStatisticsModel.class.isAssignableFrom(statModel.getClass())) {
				sb.append(((NumericalAttributeStatisticsModel) statModel).getAverage());
				appendTab(sb);

				sb.append(((NumericalAttributeStatisticsModel) statModel).getDeviation());
				appendTab(sb);

				sb.append(((NumericalAttributeStatisticsModel) statModel).getMinimum());
				appendTab(sb);

				sb.append(((NumericalAttributeStatisticsModel) statModel).getMaximum());
			} else if (NominalAttributeStatisticsModel.class.isAssignableFrom(statModel.getClass())) {
				int count = 0;
				List<String> valueStrings = ((NominalAttributeStatisticsModel) statModel).getValueStrings();
				for (String valueString : valueStrings) {
					sb.append(valueString);
					if (count < valueStrings.size() - 1) {
						sb.append(", ");
					}

					count++;
				}
			} else if (DateTimeAttributeStatisticsModel.class.isAssignableFrom(statModel.getClass())) {
				sb.append(((DateTimeAttributeStatisticsModel) statModel).getDuration());
				appendTab(sb);

				sb.append(((DateTimeAttributeStatisticsModel) statModel).getFrom());
				appendTab(sb);

				sb.append(((DateTimeAttributeStatisticsModel) statModel).getUntil());
			}

			// next row for next attribute
			sb.append(System.lineSeparator());
		}

		Tools.copyStringToClipboard(sb.toString());
	}

	/**
	 * Appends a tabulator symbol to the given {@link StringBuilder}.
	 * 
	 * @param sb
	 */
	private static void appendTab(StringBuilder sb) {
		sb.append("\t");
	}

}
