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
package com.rapidminer.gui.viewer.collection;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.rapidminer.gui.actions.StoreInRepositoryAction;
import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.actions.export.PrintableComponentContainer;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.GroupedModel;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.operator.learner.meta.MetaModel;


/**
 * Can be used to display the models of a ContainerModel.
 *
 * @author Ingo Mierswa, Nils Woehler
 */
public class CollectionViewer extends JPanel implements PrintableComponentContainer {

	private static final long serialVersionUID = -322963469866592863L;

	/** The currently used visualization component. */
	private Component current;

	private final IOObject collection;

	public CollectionViewer(final GroupedModel model, final IOContainer container) {
		this((IOObject) model, container);
	}

	public CollectionViewer(final MetaModel model, final IOContainer container) {
		this((IOObject) model, container);
	}

	public CollectionViewer(IOObjectCollection<IOObject> collection, final IOContainer container) {
		this((IOObject) collection, container);
	}

	private CollectionViewer(IOObject collection, final IOContainer container) {
		this.collection = collection;
		constructPanel(container);
	}

	private void constructPanel(final IOContainer container) {
		this.current = null;

		setLayout(new BorderLayout());

		int size;
		IOObject first = null;
		if (collection instanceof GroupedModel) {
			size = ((GroupedModel) collection).getNumberOfModels();
			if (size > 0) {
				first = ((GroupedModel) collection).getModel(0);
			}
		} else if (collection instanceof MetaModel) {
			size = ((MetaModel) collection).getModels().size();
			if (size > 0) {
				first = ((MetaModel) collection).getModels().get(0);
			}
		} else if (collection instanceof IOObjectCollection) {
			size = ((IOObjectCollection<?>) collection).size();
			if (size > 0) {
				first = ((IOObjectCollection<?>) collection).getElement(0, false);
			}
		} else {
			size = 1;
			first = collection;
		}

		switch (size) {
			case 0:
				current = new JLabel("No elements in this collection");
				add(current, BorderLayout.CENTER);
				break;
			case 1:
				IOObject currentObject = first;
				current = ResultDisplayTools.createVisualizationComponent(currentObject, container,
						currentObject instanceof ResultObject ? ((ResultObject) currentObject).getName() : currentObject
								.getClass().getName());

				add(current, BorderLayout.CENTER);
				break;
			default:
				final JTree tree = new JTree(new CollectionTreeModel(new CollectionTreeElement(collection)));
				tree.setCellRenderer(new CollectionTreeCellRenderer(collection));
				// enables a PopupMenu to store single IOOBjects out of the collection
				tree.addMouseListener(new MouseAdapter() {

					@Override
					public void mouseClicked(MouseEvent e) {
						showPopup(e);
					}

					@Override
					public void mousePressed(MouseEvent e) {
						showPopup(e);
					}

					@Override
					public void mouseReleased(MouseEvent e) {
						showPopup(e);
					}

					private void showPopup(MouseEvent e) {
						if (e.getButton() == MouseEvent.BUTTON3) {
							int row = tree.getRowForLocation(e.getX(), e.getY());
							if (row < 0) {
								return;
							}
							tree.setSelectionInterval(row, row);
							if (e.isPopupTrigger()) {
								IOObject currentObject = ((CollectionTreeElement) tree.getLastSelectedPathComponent())
										.getIOObject();
								if (currentObject != collection
										&&  // prevent recursive trees
										!(currentObject instanceof IOObjectCollection)
										&& !(currentObject instanceof GroupedModel) && !(currentObject instanceof MetaModel)) {
									JPopupMenu menu = new JPopupMenu();
									menu.add(new JMenuItem(new StoreInRepositoryAction(currentObject)));
									menu.show(tree, e.getX(), e.getY());
								}
							}
						}
					}

				});
				tree.addTreeSelectionListener(new TreeSelectionListener() {

					@Override
					public void valueChanged(TreeSelectionEvent e) {
						if (e.getPath() != null && e.getPath().getLastPathComponent() != null) {
							if (current != null) {
								remove(current);
							}
							IOObject currentObject = ((CollectionTreeElement) e.getPath().getLastPathComponent())
									.getIOObject();
							if (currentObject != collection
									&&  // prevent recursive trees
									!(currentObject instanceof IOObjectCollection)
									&& !(currentObject instanceof GroupedModel) && !(currentObject instanceof MetaModel)) {
								current = ResultDisplayTools.createVisualizationComponent(currentObject, container,
										currentObject instanceof ResultObject ? ((ResultObject) currentObject).getName()
												: currentObject.getClass().getName());
							} else {
								current = new ResourceLabel("collectionviewer.select_leaf");
								((JLabel) current).setHorizontalAlignment(SwingConstants.CENTER);
								((JLabel) current).setIcon(SwingTools.createIcon("16/information.png"));
							}
							add(current, BorderLayout.CENTER);
							revalidate();
						}
					};
				});

				JScrollPane listScrollPane = new ExtendedJScrollPane(tree);
				listScrollPane.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(0, 0, 0, 1, Colors.TAB_BORDER),
						BorderFactory.createEmptyBorder(10, 0, 0, 0)));
				add(listScrollPane, BorderLayout.WEST);

				// select first model
				tree.setSelectionRow(0);
				break;
		}
	}

	/**
	 * @return the Component which displays the actual result
	 */
	public Component getResultViewComponent() {
		return current;
	}

	@Override
	public List<PrintableComponent> getPrintableComponents() {
		if (current != null) {
			return PrintingTools.findExportComponents(current);
		}
		return Collections.emptyList();
	}

}
