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
package com.rapidminer.test;

import junit.framework.TestCase;

import org.junit.Test;

import Jama.Matrix;

import com.rapidminer.operator.learner.functions.kernel.rvm.util.SECholeskyDecomposition;

/**
 * Test our SE99-based cholesky decomposition.
 * 
 * @author Piotr Kasprzak
 *
 */
public class SECDTest extends TestCase {

	private SECholeskyDecomposition cd;
	
	private Matrix L, PTR;
	
	@Test
	public void testPaperExample1() {
		
		double[][] A = {{  1890.3, -1705.6, -315.8,  3000.3},
						{ -1705.6,  1538.3,  284.9, -2706.6},
						{  -315.8,   284.9,   52.5,  -501.2},
						{  3000.3, -2706.6, -501.2,  4760.8}};
		
		cd = new SECholeskyDecomposition(A);
		L = cd.getL();
		PTR = cd.getPTR();
		L.toString();
		
		Matrix LTL = PTR.times((L.times(L.transpose())).times(PTR.transpose()));
		LTL.toString();
	}

	@Test
	public void testPaperExample2() {
		
		double[][] A = {{ 14.8253, -6.4243,   7.8746,  -1.2498, 10.2733, 10.2733},
						{ -6.4243, 15.1024,  -1.1155,  -0.2761, -8.2117, -8.2117},
						{  7.8746, -1.1155,  51.8519, -23.3482, 12.5902, 12.5902},
						{ -1.2498, -0.2761, -23.3482,  22.7967, -9.8958, -9.8958},
						{ 10.2733, -8.2117,  12.5902,  -9.8958, 21.0656, 21.0656},
						{ 10.2733, -8.2117,  12.5902,  -9.8958, 21.0656, 21.0656}};

		cd = new SECholeskyDecomposition(A);
		L = cd.getL();
		PTR = cd.getPTR();
		L.toString();
		
		Matrix LTL = PTR.times((L.times(L.transpose())).times(PTR.transpose()));
		LTL.toString();
	}
		
	@Test	
	public void testSwapping() {
		
		double[][] A = new double[4][4];
		
		A[0][0] = 00;
		A[0][1] = 01;
		A[0][2] = 02;
		A[0][3] = 03;
		
		A[1][0] = 10;
		A[1][1] = 11;
		A[1][2] = 12;
		A[1][3] = 13;
		
		A[2][0] = 20;
		A[2][1] = 21;
		A[2][2] = 22;
		A[2][3] = 23;
		
		A[3][0] = 30;
		A[3][1] = 31;
		A[3][2] = 32;
		A[3][3] = 33;

//		SECholeskyDecomposition.swapRowsAndColumns(A, 1, 2, true, 0);
	}
}
