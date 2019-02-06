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

import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizard;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.operator.nio.model.WizardState;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * All new data import wizards should inherit from this class. It provides the common steps
 * (annotations, meta data, saving) for all import wizards.
 *
 * @author Simon Fischer
 * @deprecated was replaced by the {@link com.rapidminer.studio.io.gui.internal.DataImportWizard}
 */
@Deprecated
public abstract class AbstractDataImportWizard extends DataImportWizard {

	private static final long serialVersionUID = 1L;

	private final WizardState state;
	private final AbstractDataResultSetReader reader;

	private RepositoryLocation preselectedLocation;

	public AbstractDataImportWizard(AbstractDataResultSetReader reader, RepositoryLocation preselectedLocation, String key,
			Object... arguments) throws OperatorException {
		super(key, arguments);
		this.reader = reader;
		this.preselectedLocation = preselectedLocation;
		DataResultSetFactory factory = makeFactory(reader);
		state = new WizardState(reader, factory);
		logStats("start");
	}

	/**
	 * Logs an event for this import type (CSV, Excel, ...) with the
	 * {@link ActionStatisticsCollector}. Event type can be passed as arg, e.g. "start", "complete",
	 * "error".
	 */
	protected void logStats(String arg) {
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_IMPORT,
				getReader() != null ? getReader().getOperatorDescription().getKey() : getClass().getSimpleName(), arg);
	}

	/**
	 * Creates a {@link DataResultSetFactory} for the {@link AbstractDataResultSetReader} given in
	 * the constructor.
	 */
	protected abstract DataResultSetFactory makeFactory(AbstractDataResultSetReader reader) throws OperatorException;

	/**
	 * Same as {@link #addCommonSteps()} where the store step will be added only if
	 * {@link #getReader()} is null.
	 */
	protected void addCommonSteps() {
		addCommonSteps(getReader() == null);
	}

	/**
	 *
	 * @param includeStoreStep
	 *            If true, a step to store the data in the repository will be added.
	 */
	protected void addCommonSteps(boolean includeStoreStep) {
		addStep(new AnnotationDeclarationWizardStep(getState()));
		addStep(new MetaDataDeclarationWizardStep(getState()));
		if (includeStoreStep) {
			addStep(new StoreDataWizardStep(this, getState(),
					preselectedLocation != null ? preselectedLocation.getAbsoluteLocation() : null, true));
		}
	}

	@Override
	public void cancel() {
		super.cancel();
		getState().getDataResultSetFactory().close();
		logStats("cancelled");
	}

	public WizardState getState() {
		return state;
	}

	public AbstractDataResultSetReader getReader() {
		return reader;
	}

	@Override
	public void finish() {
		super.finish();
		if (reader != null) { // we are configuring an operator
			state.getTranslationConfiguration().setParameters(reader);
			state.getDataResultSetFactory().setParameters(reader);
			getState().getDataResultSetFactory().close();
		}
		logStats("finish");
	}
}
