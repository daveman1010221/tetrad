///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.gene.tetrad.gene.history;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the Polynomial class.
 *
 * @author Joseph Ramsey jdramsey@andrew.cmu.edu
 */
public class TestPolynomial extends TestCase {

    /**
     * Standard constructor for JUnit test cases.
     */
    public TestPolynomial(final String name) {
        super(name);
    }

    /**
     * Tests to make sure that null parent throw an exception.
     */
    public void testConstruction() {
        final PolynomialTerm term0 = new PolynomialTerm(1.0, new int[]{0});
        final PolynomialTerm term1 = new PolynomialTerm(1.0, new int[]{1});
        final PolynomialTerm term2 = new PolynomialTerm(1.0, new int[]{2, 3});

        final List terms = new ArrayList();
        terms.add(term0);
        terms.add(term1);
        terms.add(term2);

        final Polynomial p = new Polynomial(terms);

        System.out.println(p);
    }

    /**
     * Test the evaluation of terms.
     */
    public void testEvaluation() {
        final PolynomialTerm term0 = new PolynomialTerm(1.0, new int[]{0});
        final PolynomialTerm term1 = new PolynomialTerm(1.0, new int[]{1});
        final PolynomialTerm term2 = new PolynomialTerm(1.0, new int[]{2, 3});

        final List terms = new ArrayList();
        terms.add(term0);
        terms.add(term1);
        terms.add(term2);

        final Polynomial p = new Polynomial(terms);

        final double[] values = {1.0, 2.0, 3.0, 4.0};

        TestCase.assertEquals(15.0, p.evaluate(values), 0.00001);
    }

    /**
     * This method uses reflection to collect up all of the test methods from
     * this class and return them to the test runner.
     */
    public static Test suite() {

        // Edit the name of the class in the parens to match the name
        // of this class.
        return new TestSuite(TestPolynomial.class);
    }
}





