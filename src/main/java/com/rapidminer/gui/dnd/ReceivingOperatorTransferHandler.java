/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.gui.dnd;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.UnknownParameterInformation;
import com.rapidminer.operator.internal.ProcessEmbeddingOperator;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.operator.nio.file.LoadFileOperator;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.studio.io.gui.internal.DataImportWizardBuilder;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.Tools;


/**
 * Transfer handler that supports dragging and dropping operators and workflow annotations.
 *
 * @author Simon Fischer, Marius Helf, Michael Knopf, Marco Boeck
 *
 */
public abstract class ReceivingOperatorTransferHandler extends OperatorTransferHandler {

	private static final long serialVersionUID = 5355397064093668659L;

	private final List<DataFlavor> acceptableFlavors = new LinkedList<>();

	public ReceivingOperatorTransferHandler() {
		acceptableFlavors.add(TransferableOperator.LOCAL_TRANSFERRED_OPERATORS_FLAVOR);
		acceptableFlavors.add(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR);
		acceptableFlavors.add(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR);
		acceptableFlavors.add(TransferableAnnotation.LOCAL_OPERATOR_ANNOTATION_FLAVOR);
		acceptableFlavors.add(TransferableAnnotation.LOCAL_PROCESS_ANNOTATION_FLAVOR);
		acceptableFlavors.add(DataFlavor.javaFileListFlavor);
		acceptableFlavors.add(DataFlavor.stringFlavor);
	}

	/**
	 * Drops the operator at the given location. The location may be null, indicating that this is a
	 * paste.
	 */
	protected abstract boolean dropNow(List<Operator> newOperators, Point loc);

	protected abstract boolean dropNow(WorkflowAnnotation anno, Point loc);

	protected abstract boolean dropNow(String processXML);

	protected abstract void markDropOver(Point dropPoint);

	protected abstract boolean isDropLocationOk(List<Operator> operator, Point loc);

	protected abstract void dropEnds();

	protected abstract Process getProcess();

	// Drop Support
	@Override
	public boolean canImport(TransferSupport ts) {
		for (DataFlavor flavor : acceptableFlavors) {
			if (ts.isDataFlavorSupported(flavor)) {
				if (ts.isDrop()) {
					markDropOver(ts.getDropLocation().getDropPoint());
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean importData(TransferSupport ts) {
		if (!canImport(ts)) {
			return false;
		}
		DataFlavor acceptedFlavor = null;
		for (DataFlavor flavor : acceptableFlavors) {
			if (ts.isDataFlavorSupported(flavor)) {
				acceptedFlavor = flavor;
				break;
			}
		}
		if (acceptedFlavor == null) {
			dropEnds();
			return false; // cannot happen
		}

		Object transferData;
		try {
			transferData = ts.getTransferable().getTransferData(acceptedFlavor);
		} catch (Exception e1) {
			LogService.getRoot()
					.log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.error_while_accepting_drop"),
							e1);
			dropEnds();
			return false;
		}
		List<Operator> newOperators;
		if (acceptedFlavor.equals(DataFlavor.javaFileListFlavor)) {

			@SuppressWarnings("unchecked")
			final File file = ((List<File>) transferData).get(0);
			if (file.getName().toLowerCase().endsWith("." + RapidMiner.PROCESS_FILE_EXTENSION)) {
				// This is a process file
				try {
					Operator processEmbedder = OperatorService.createOperator(ProcessEmbeddingOperator.OPERATOR_KEY);
					processEmbedder.setParameter(ProcessEmbeddingOperator.PARAMETER_PROCESS_FILE, file.getAbsolutePath());
					newOperators = Collections.<Operator> singletonList(processEmbedder);
				} catch (Exception e) {
					SwingTools.showSimpleErrorMessage("cannot_create_process_embedder", e);
					dropEnds();
					return false;
				}
			} else {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						DataImportWizardBuilder importWizardBuilder = new DataImportWizardBuilder();
						importWizardBuilder.forFile(file.toPath()).build(RapidMinerGUI.getMainFrame()).getDialog()
								.setVisible(true);
					}
				});
				dropEnds();
				return true;
			}
		} else if (acceptedFlavor.equals(TransferableOperator.LOCAL_TRANSFERRED_OPERATORS_FLAVOR)) {
			// This is an operator
			if (transferData instanceof Operator[]) {
				newOperators = Arrays.asList((Operator[]) transferData);
			} else {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.expected_operator", acceptedFlavor);
				dropEnds();
				return false;
			}
		} else if (acceptedFlavor.equals(DataFlavor.stringFlavor)) {
			if (transferData instanceof String) {
				try {
					Process process = new Process(((String) transferData).trim());
					newOperators = process.getRootOperator().getSubprocess(0).getOperators();
				} catch (Exception e) {
					try {
						Document document = XMLTools.createDocumentBuilder()
								.parse(new InputSource(new StringReader((String) transferData)));
						NodeList opElements = document.getDocumentElement().getChildNodes();
						Operator newOp = null;
						for (int i = 0; i < opElements.getLength(); i++) {
							Node child = opElements.item(i);
							if (child instanceof Element) {
								Element elem = (Element) child;
								if ("operator".equals(elem.getTagName())) {
									newOp = new XMLImporter(null).parseOperator(elem, RapidMiner.getVersion(), getProcess(),
											new LinkedList<UnknownParameterInformation>());
									break;
								}
							}
						}
						if (newOp == null) {
							LogService.getRoot().log(Level.WARNING,
									"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.parsing_operator_from_clipboard_error",
									transferData);
							dropEnds();
							return false;
						}
						newOperators = Collections.singletonList(newOp);
					} catch (SAXParseException e1) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.gui.processeditor.XMLEditor.failed_to_parse_process");
						dropEnds();
						return false;
					} catch (Exception e1) {
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.parsing_operator_from_clipboard_error_exception",
										e1, transferData),
								e1);
						dropEnds();
						return false;
					}
				}
			} else {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.expected_string", acceptedFlavor);
				dropEnds();
				return false;
			}
		} else if (acceptedFlavor.equals(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR)) {
			if (transferData instanceof RepositoryLocation) {
				RepositoryLocation repositoryLocation = (RepositoryLocation) transferData;
				newOperators = Collections.singletonList(createOperator(repositoryLocation));
				if (newOperators == null) {
					return false;
				}
			} else {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.expected_repositorylocation",
						acceptedFlavor);
				dropEnds();
				return false;
			}
		} else if (acceptedFlavor.equals(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR)) {
			if (transferData instanceof RepositoryLocationList) {
				RepositoryLocationList repositoryLocationList = (RepositoryLocationList) transferData;
				newOperators = new LinkedList<>();
				for (RepositoryLocation loc : repositoryLocationList.getAll()) {
					List<Operator> list = Collections.singletonList(createOperator(loc));
					if (list == null) {
						return false;
					} else {
						newOperators.addAll(list);
					}
				}
			} else {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.expected_repositorylocationlist",
						acceptedFlavor);
				dropEnds();
				return false;
			}
		} else if (acceptedFlavor.equals(TransferableAnnotation.LOCAL_OPERATOR_ANNOTATION_FLAVOR)
				|| acceptedFlavor.equals(TransferableAnnotation.LOCAL_PROCESS_ANNOTATION_FLAVOR)) {
			newOperators = new LinkedList<>();
		} else {
			// cannot happen
			dropEnds();
			return false;
		}

		if (ts.isDrop()) {
			// drop
			Point loc = ts.getDropLocation().getDropPoint();
			boolean dropLocationOk = !ts.isDrop() || isDropLocationOk(newOperators, loc);
			if (!dropLocationOk) {
				dropEnds();
				return false;
			} else {
				if (ts.getDropAction() == MOVE) {
					for (Operator operator : newOperators) {
						operator.removeAndKeepConnections(newOperators);
					}
				}
				newOperators = Tools.cloneOperators(newOperators);

				boolean result;
				try {
					result = dropNow(newOperators, ts.isDrop() ? loc : null);
				} catch (RuntimeException e) {
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.error_in_drop", e), e);
					SwingTools.showVerySimpleErrorMessage("error_in_paste", e.getMessage(), e.getMessage());
					dropEnds();
					return false;
				}
				dropEnds();
				return result;
			}
		} else {
			// paste
			if (acceptedFlavor.equals(DataFlavor.stringFlavor)) {
				// handle XML String pasting differently
				boolean result;
				try {
					result = dropNow(String.valueOf(transferData));
				} catch (RuntimeException e) {
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.error_in_paste", e), e);
					SwingTools.showVerySimpleErrorMessage("error_in_paste", e.getMessage(), e.getMessage());
					dropEnds();
					return false;
				}
				dropEnds();
				return result;
			} else if (acceptedFlavor.equals(TransferableAnnotation.LOCAL_PROCESS_ANNOTATION_FLAVOR)
					|| acceptedFlavor.equals(TransferableAnnotation.LOCAL_OPERATOR_ANNOTATION_FLAVOR)) {
				boolean result;
				try {
					result = dropNow((WorkflowAnnotation) transferData, null);
				} catch (RuntimeException e) {
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.error_in_paste", e), e);
					SwingTools.showVerySimpleErrorMessage("error_in_paste", e.getMessage(), e.getMessage());
					dropEnds();
					return false;
				}
				dropEnds();
				return result;
			} else {
				// paste an existing Operator
				newOperators = Tools.cloneOperators(newOperators);
				boolean result;
				try {
					result = dropNow(newOperators, null);
				} catch (RuntimeException e) {
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.error_in_paste", e), e);
					SwingTools.showVerySimpleErrorMessage("error_in_paste", e.getMessage(), e.getMessage());
					dropEnds();
					return false;
				}
				dropEnds();
				return result;
			}
		}
	}

	/**
	 * Creates the operator to import from the given repository location.
	 *
	 * @param repositoryLocation
	 *            the location which should be imported
	 * @return the operator or {@code null}
	 */
	private Operator createOperator(RepositoryLocation repositoryLocation) {
		Entry entry;

		try {
			entry = repositoryLocation.locateEntry();
		} catch (Exception e) {
			// no valid entry
			return null;
		}

		String resolvedLocation;
		if (getProcess().getRepositoryLocation() != null) {
			resolvedLocation = repositoryLocation.makeRelative(getProcess().getRepositoryLocation().parent());
		} else {
			resolvedLocation = repositoryLocation.getAbsoluteLocation();
		}

		if (!(entry instanceof DataEntry)) {
			// can't handle non-data entries (like folders)
			return null;
		} else if (entry instanceof BlobEntry) {
			// create Retrieve Blob operator
			try {
				LoadFileOperator source = OperatorService.createOperator(LoadFileOperator.class);
				source.setParameter(LoadFileOperator.PARAMETER_REPOSITORY_LOCATION, resolvedLocation);
				source.setParameter(LoadFileOperator.PARAMETER_SOURCE_TYPE,
						String.valueOf(LoadFileOperator.SOURCE_TYPE_REPOSITORY));
				return source;
			} catch (OperatorCreationException e1) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.creating_repositorysource_error", e1), e1);
				return null;
			}
		} else if (entry instanceof ProcessEntry) {
			// create Execute Process operator
			try {
				Operator embedder = OperatorService.createOperator(ProcessEmbeddingOperator.OPERATOR_KEY);
				embedder.setParameter(ProcessEmbeddingOperator.PARAMETER_PROCESS_FILE, resolvedLocation);
				embedder.rename("Execute " + repositoryLocation.getName());
				return embedder;
			} catch (OperatorCreationException e1) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.creating_repositorysource_error", e1), e1);
				return null;
			}
		} else {
			// create Retrieve operator
			try {
				RepositorySource source = OperatorService.createOperator(RepositorySource.class);
				source.setParameter(RepositorySource.PARAMETER_REPOSITORY_ENTRY, resolvedLocation);
				source.rename("Retrieve " + repositoryLocation.getName());
				return source;
			} catch (OperatorCreationException e1) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler.creating_repositorysource_error", e1), e1);
				return null;
			}
		}
	}
}
