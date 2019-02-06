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
package com.rapidminer.operator.nio;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.rapidminer.RapidMiner;
import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.example.Attributes;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;


/**
 * 
 * @author Simon Fischer
 * 
 */
public class ImportWizardUtils {

	public static void showErrorMessage(String resource, String message, Throwable exception) {
		SwingTools.showSimpleErrorMessage("importwizard.io_error", exception, resource, message);
	}

	public static int getPreviewLength() {
		try {
			return Integer
					.parseInt(Objects.requireNonNull(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS)));
		} catch (NumberFormatException | NullPointerException e) {
			return 100;
		}
	}

	/**
	 * Sets the parameters of the given operator according to the provided {@link DataSource#getMetadata()}.
	 *
	 * @param dataSource
	 * 		the datasource
	 * @param operator
	 * 		the operator
	 * @throws DataSetException
	 * 		if datasource contains no meta data
	 * @since 9.0.0
	 */
	public static void setMetaData(DataSource dataSource, Operator operator) throws DataSetException {
		if (dataSource == null) {
			throw new IllegalArgumentException("dataSource must not be null!");
		}
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null!");
		}

		// set meta data
		DataSetMetaData metaData = dataSource.getMetadata();

		DateFormat dateFormat = metaData.getDateFormat();
		if (dateFormat instanceof SimpleDateFormat) {
			operator.setParameter(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT, ((SimpleDateFormat) dateFormat).toPattern());
		}

		List<String[]> metaDataList = new LinkedList<>();

		int index = 0;
		for (ColumnMetaData cmd : metaData.getColumnMetaData()) {
			String[] tuple = new String[4];
			tuple[0] = cmd.getName();
			tuple[1] = String.valueOf(!cmd.isRemoved());

			// convert ColumnType to Ontology
			int type;
			switch (cmd.getType()) {
				case BINARY:
					type = Ontology.BINOMINAL;
					break;
				case DATE:
					type = Ontology.DATE;
					break;
				case DATETIME:
					type = Ontology.DATE_TIME;
					break;
				case INTEGER:
					type = Ontology.INTEGER;
					break;
				case REAL:
					type = Ontology.REAL;
					break;
				case TIME:
					type = Ontology.TIME;
					break;
				case CATEGORICAL:
				default:
					type = Ontology.POLYNOMINAL;
					break;
			}
			tuple[2] = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(type);

			String role = cmd.getRole();
			if (role == null) {
				// no role -> attribute
				role = Attributes.ATTRIBUTE_NAME;
			}
			tuple[3] = role;
			String encodedTuple = ParameterTypeTupel.transformTupel2String(tuple);
			metaDataList.add(new String[] { String.valueOf(index++), encodedTuple });
		}
		operator.setParameter(AbstractDataResultSetReader.PARAMETER_META_DATA, ParameterTypeList.transformList2String(metaDataList));

		operator.setParameter(AbstractDataResultSetReader.PARAMETER_ERROR_TOLERANT, String.valueOf(metaData.isFaultTolerant()));
	}

}
