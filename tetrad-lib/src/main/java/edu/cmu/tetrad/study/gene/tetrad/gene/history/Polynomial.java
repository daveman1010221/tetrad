///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015, 2022 by Peter Spirtes, Richard        //
// Scheines, Joseph Ramsey, and Clark Glymour.                               //
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

package edu.cmu.tetrad.study.gene.tetrad.gene.history;

import edu.cmu.tetrad.util.TetradLogger;
import edu.cmu.tetrad.util.TetradSerializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a polynomial as a sum of a list of terms whose variables are identified as integers in the set {0, 1, 2,
 * ...}.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
public final class Polynomial implements TetradSerializable {
    @Serial
    private static final long serialVersionUID = 23L;

    /**
     * The terms of the polynomial.
     */
    private final List<PolynomialTerm> terms;

    //==============================CONSTRUCTOR===========================//

    /**
     * Constructs a polynomial from a list of terms.
     *
     * @param terms a {@link java.util.List} object
     */
    public Polynomial(List<PolynomialTerm> terms) {

        if (terms == null) {
            throw new NullPointerException("Terms list cannot be null.");
        }

        this.terms = new ArrayList<>(terms);
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @return a {@link edu.cmu.tetrad.study.gene.tetrad.gene.history.Polynomial} object
     */
    public static Polynomial serializableInstance() {
        return new Polynomial(new ArrayList<>());
    }

    //==============================PUBLIC METHODS========================//

    /**
     * Returns the number of terms.
     *
     * @return a int
     */
    public int getNumTerms() {
        return this.terms.size();
    }

    /**
     * Returns the coefficient.
     *
     * @param index a int
     * @return a {@link edu.cmu.tetrad.study.gene.tetrad.gene.history.PolynomialTerm} object
     */
    public PolynomialTerm getTerm(int index) {
        return this.terms.get(index);
    }

    /**
     * Finds the first term matching the given profile.
     *
     * @param variables an array of  objects
     * @return a {@link edu.cmu.tetrad.study.gene.tetrad.gene.history.PolynomialTerm} object
     */
    public PolynomialTerm findTerm(int[] variables) {
        for (PolynomialTerm term1 : this.terms) {
            if (term1.isVariableListEqual(variables)) {
                return term1;
            }
        }

        return null;
    }

    /**
     * Returns the highest variable index in any term.
     *
     * @return a int
     */
    public int getMaxIndex() {
        int max = 0;
        for (PolynomialTerm term1 : this.terms) {
            int termMax = term1.getMaxIndex();
            if (termMax > max) {
                max = termMax;
            }
        }
        return max;
    }

    /**
     * Evaluates the term.
     *
     * @param values an array of  objects
     * @return a double
     */
    public double evaluate(double[] values) {
        double sum = 0.0;
        for (PolynomialTerm term1 : this.terms) {
            sum += term1.evaluate(values);
        }
        return sum;
    }

    /**
     * Prints out a representation of the term.
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < this.terms.size(); i++) {
            buf.append(this.terms.get(i));
            if (i < this.terms.size() - 1) {
                buf.append(" + ");
            }
        }
        return buf.toString();
    }

    /**
     * Writes the object to the specified ObjectOutputStream.
     *
     * @param out The ObjectOutputStream to write the object to.
     * @throws IOException If an I/O error occurs.
     */
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        try {
            out.defaultWriteObject();
        } catch (IOException e) {
            TetradLogger.getInstance().log("Failed to serialize object: " + getClass().getCanonicalName()
                                           + ", " + e.getMessage());
            throw e;
        }
    }

    /**
     * Reads the object from the specified ObjectInputStream. This method is used during deserialization
     * to restore the state of the object.
     *
     * @param in The ObjectInputStream to read the object from.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class of the serialized object cannot be found.
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            in.defaultReadObject();
        } catch (IOException e) {
            TetradLogger.getInstance().log("Failed to deserialize object: " + getClass().getCanonicalName()
                                           + ", " + e.getMessage());
            throw e;
        }
    }
}





