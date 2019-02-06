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
package com.rapidminer.example.set;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.ExampleSetFactory;


/**
 * Test the constructor of SplittedExampleSet that composes partitions.
 *
 * @author Gisa Schaefer
 */
public class SplittedExampleSetCompositionTest {

	ExampleSet es = ExampleSetFactory.createExampleSet(new double[][] { { 0 }, { 1 }, { 2 }, { 3 }, { 4 }, { 5 } });

	@Test
	public void compositionTest1PartionSelected() {
		Partition outer = new Partition(new int[] { 1, 0, 0, 2, 0, 2 }, 3);

		SplittedExampleSet outerEs = new SplittedExampleSet(es, outer);
		outerEs.selectSingleSubset(0);

		Partition inner = new Partition(new int[] { 1, 0, 1 }, 2);

		SplittedExampleSet innerEsComposed = new SplittedExampleSet(outerEs, inner, true);
		SplittedExampleSet innerEsOld = new SplittedExampleSet(outerEs, inner);

		assertEquals(innerEsOld.size(), innerEsComposed.size());
		assertEquals(innerEsOld.getNumberOfSubsets(), innerEsComposed.getNumberOfSubsets());

		Attribute att = innerEsOld.getAttributes().get("att1");

		for (int i = 0; i < innerEsOld.size(); i++) {
			assertEquals(innerEsOld.getExample(i).getValue(att), innerEsComposed.getExample(i).getValue(att), 1e-15);
		}

		innerEsComposed.selectSingleSubset(1);
		innerEsOld.selectSingleSubset(1);

		for (int i = 0; i < innerEsOld.size(); i++) {
			assertEquals(innerEsOld.getExample(i).getValue(att), innerEsComposed.getExample(i).getValue(att), 1e-15);
		}
	}

	@Test
	public void compositionTest2PartitionsSelected() {
		Partition outer = new Partition(new int[] { 1, 0, 0, 2, 0, 2 }, 3);
		SplittedExampleSet outerEs = new SplittedExampleSet(es, outer);
		outerEs.selectAllSubsetsBut(0);

		Partition inner = new Partition(new int[] { 1, 0, 1 }, 2);

		SplittedExampleSet innerEsComposed = new SplittedExampleSet(outerEs, inner, true);
		SplittedExampleSet innerEsOld = new SplittedExampleSet(outerEs, inner);

		assertEquals(innerEsOld.size(), innerEsComposed.size());
		assertEquals(innerEsOld.getNumberOfSubsets(), innerEsComposed.getNumberOfSubsets());

		Attribute att = innerEsOld.getAttributes().get("att1");

		for (int i = 0; i < innerEsOld.size(); i++) {
			assertEquals(innerEsOld.getExample(i).getValue(att), innerEsComposed.getExample(i).getValue(att), 1e-15);
		}

		innerEsComposed.selectAllSubsetsBut(1);
		innerEsOld.selectAllSubsetsBut(1);

		for (int i = 0; i < innerEsOld.size(); i++) {
			assertEquals(innerEsOld.getExample(i).getValue(att), innerEsComposed.getExample(i).getValue(att), 1e-15);
		}
	}

	@Test
	public void compositionTestSizeNotMatching() {
		Partition outer = new Partition(new int[] { 1, 0, 0, 2, 0, 2 }, 3);
		SplittedExampleSet outerEs = new SplittedExampleSet(es, outer);
		Partition inner = new Partition(new int[] { 1, 0, 1 }, 2);

		SplittedExampleSet innerEsComposed = new SplittedExampleSet(outerEs, inner, true);
		SplittedExampleSet innerEsOld = new SplittedExampleSet(outerEs, inner);

		assertEquals(innerEsOld.size(), innerEsComposed.size());
		assertEquals(innerEsOld.getNumberOfSubsets(), innerEsComposed.getNumberOfSubsets());

		Attribute att = innerEsOld.getAttributes().get("att1");

		for (int i = 0; i < innerEsOld.size(); i++) {
			assertEquals(innerEsOld.getExample(i).getValue(att), innerEsComposed.getExample(i).getValue(att), 1e-15);
		}
	}

	@Test
	public void compositionWithPartitionComposition() {
		Partition outer = new Partition(new int[] { 1, 0, 0, 2, 0, 2 }, 3);

		SplittedExampleSet outerEs = new SplittedExampleSet(es, outer);
		outerEs.selectSingleSubset(0);

		Partition inner = new Partition(new int[] { 1, 0, 1 }, 2);

		SplittedExampleSet innerEsComposed = new SplittedExampleSet(outerEs, inner, true);

		Partition outer2 = (Partition) outer.clone();
		outer2.clearSelection();
		outer2.selectSubset(0);
		Partition inner2 = (Partition) inner.clone();

		Partition composition = Partition.compose(outer2, inner2);

		SplittedExampleSet composedPartitionSet = new SplittedExampleSet(es, composition);

		assertEquals(composedPartitionSet.size(), innerEsComposed.size());
		assertEquals(composedPartitionSet.getNumberOfSubsets(), innerEsComposed.getNumberOfSubsets());

		Attribute att = composedPartitionSet.getAttributes().get("att1");

		for (int i = 0; i < composedPartitionSet.size(); i++) {
			assertEquals(composedPartitionSet.getExample(i).getValue(att), innerEsComposed.getExample(i).getValue(att),
					1e-15);
		}

	}

}
