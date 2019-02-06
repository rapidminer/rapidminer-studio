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
/**
 * Contains the {@link ColumnarExampleTable} and the {@link Column}s used for it. Columns consist of
 * several Chunks.
 *
 * <p>
 * A {@link DoubleAutoColum} contains {@link DoubleAutoChunk}s where each can be either
 * {@link DoubleAutoDenseChunk} or {@link DoubleAutoSparseChunk}. It comes in two modes: either
 * {@link DataManagement#AUTO} or {@link DataManagement#MEMORY_OPTIMIZED}. If the mode is
 * {@link DataManagement#AUTO}, then the {@link DoubleAutoSparseChunk} contains a
 * {@link DoubleHighSparsityChunk}, otherwise it contains a {@link DoubleMediumSparsityChunk}.
 *
 * <p>
 * In mode {@link DataManagement#AUTO}, every {@link DoubleAutoChunk} starts as a
 * {@link DoubleAutoDenseChunk} which contains an array of maximal 2048 elements to store the added
 * values. If the 2048th element is added, the density of these elements is checked. If it is below
 * 1%, the chunk changes to a {@link DoubleAutoSparseChunk} with the calculated default value.
 * Otherwise, it stays a {@link DoubleAutoDenseChunk} but with internal array size as big as the
 * expected size. Such {@link DoubleAutoDenseChunk}s will not check their density again.
 * {@link DoubleAutoSparseChunk}s however continue to check their density. If it grows above 2%,
 * they change back to a {@link DoubleAutoDenseChunk}s with the full expected size.
 *
 * <p>
 * In mode {@link DataManagement#MEMORY_OPTIMIZED}, every {@link DoubleAutoChunk} starts as a
 * {@link DoubleAutoSparseChunk} containing a {@link DoubleMediumSparsityChunk} with default value
 * 0. If the density reaches over 50% with respect to the first 2048 values before inserting the
 * 2048th element, the chunk is changed back to {@link DoubleAutoDenseSparse} and its sparsity and
 * default value is again checked when the 2048th element is inserted. If the density reaches over
 * 55% when inserting an element (after the 2048th element was inserted), then the chunk is changed
 * back to {@link DoubleAutoDenseChunk} and stays dense.
 *
 * <p>
 * If the {@link DoubleAutoColumn#complete()} method is called before the 2048th element is
 * inserted, the chunk grows to the full expected size.
 *
 * <p>
 * Here an overview of the transitions:
 * <p>
 * <table summary="Transitions of Chunks">
 * <tr>
 * <th>Type A</th>
 * <th>Scenario</th>
 * <th>Type B</th>
 * </tr>
 *
 * <tr>
 * <td>
 *
 * {@link DoubleAutoDenseChunk} in mode {@link DataManagement#AUTO} with density {@code < 1%}
 *
 * </td>
 * <td>Insertion of 2048th element</td>
 * <td>
 *
 * {@link DoubleAutoSparseChunk} containing {@link DoubleHighSparsityChunk} in mode
 * {@link DataManagement#AUTO}
 *
 * </td>
 * </tr>
 *
 * <tr>
 * <td>
 *
 * {@link DoubleAutoDenseChunk} in mode {@link DataManagement#AUTO} with density {@code >= 1%}
 *
 * </td>
 * <td>Insertion of 2048th element</td>
 * <td>
 *
 * {@link DoubleAutoDenseChunk} in mode {@link DataManagement#AUTO} with the full ensured size
 *
 * </td>
 * </tr>
 *
 * <tr>
 * <td>
 *
 * {@link DoubleAutoSparseChunk} containing {@link DoubleHighSparsityChunk} in mode
 * {@link DataManagement#AUTO}
 *
 * </td>
 * <td>Insertion of non-default element making density > 2%</td>
 * <td>
 *
 * {@link DoubleAutoDenseChunk} in mode {@link DataManagement#AUTO} with the full ensured size
 *
 * </td>
 * </tr>
 *
 * <tr>
 * <td>
 *
 * {@link DoubleAutoSparseChunk} containing a {@link DoubleMediumDensityChunk} in mode
 * {@link DataManagement#MEMORY_OPTIMIZED}
 *
 * </td>
 * <td>Insertion of element element before the 2048th that grows density to over 50% wrt.
 * max(ensuredSize, 2048)</td>
 * <td>
 *
 * {@link DoubleAutoDenseChunk} in mode {@link DataManagement#MEMORY_OPTIMIZED} with space for
 * maximal 2048 values
 *
 * </td>
 * </tr>
 *
 * <tr>
 * <td>
 *
 * {@link DoubleAutoDenseChunk} in mode {@link DataManagement#MEMORY_OPTIMIZED} with density
 * {@code < 50%}
 *
 * </td>
 * <td>Insertion of 2048th element</td>
 * <td>
 *
 * {@link DoubleAutoSparseChunk} containing {@link DoubleMediumSparsityChunk} in mode
 * {@link DataManagement#MEMORY_OPTIMIZED}
 *
 * </td>
 * </tr>
 *
 * <tr>
 * <td>
 *
 * {@link DoubleAutoDenseChunk} in mode {@link DataManagement#MEMORY_OPTIMIZED} with density
 * {@code >= 50%}
 *
 * </td>
 * <td>Insertion of 2048th element</td>
 * <td>
 *
 * {@link DoubleAutoDenseChunk} in mode {@link DataManagement#MEMORY_OPTIMIZED} the full ensured
 * size
 *
 * </td>
 * </tr>
 *
 * <tr>
 * <td>
 *
 * {@link DoubleAutoSparseChunk} containing a {@link DoubleMediumDensityChunk} in mode
 * {@link DataManagement#MEMORY_OPTIMIZED}
 *
 * </td>
 * <td>Insertion of element element after the 2048th that grows density to over 55%</td>
 * <td>
 *
 * {@link DoubleAutoDenseChunk} in mode {@link DataManagement#MEMORY_OPTIMIZED} with full ensured
 * size
 *
 * </td>
 * </tr>
 *
 * <tr>
 * <td>
 *
 * {@link DoubleAutoDenseChunk} with fewer than 2048 elements inserted
 *
 * </td>
 * <td>Call of {@link DoubleAutoColumn#complete()}</td>
 * <td>
 *
 * {@link DoubleAutoDenseChunk} with full ensured size
 *
 * </td>
 * </tr>
 *
 * </table>
 *
 * <p>
 * A {@link IntegerAutoColumn} with its {@link IntegerAutoChunk}s works exactly the same except
 * that, instead of 50% below 2048 elements and 55% above, the threshold for changing back to dense
 * in mode {@link DataManagement#MEMORY_OPTIMIZED} is always 45%. Also, the threshold for going to
 * sparse is 40% instead of 50% in that mode.
 * <p>
 *
 * The columns and chunks with Incomplete instead of Auto in its name work analogously to the Auto
 * ones. The only difference is, that their dense chunks allocate always the full expected size
 * instead of only 2048 values first before the sparsity check.
 *
 * @author Gisa Schaefer
 *
 */
package com.rapidminer.example.table.internal;
