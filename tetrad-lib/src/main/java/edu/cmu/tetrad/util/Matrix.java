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

package edu.cmu.tetrad.util;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.FastMath;
import org.ejml.simple.SimpleMatrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;

/**
 * Wraps the Apache math3 linear algebra library for most uses in Tetrad. Specialized uses will still have to use the
 * library directly. One issue is that we need to be able to represent empty matrices gracefully; this case is handled
 * separately and incorporated into the class.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
public class Matrix implements TetradSerializable {
    @Serial
    private static final long serialVersionUID = 23L;

    /**
     * The Apache math3 matrix.
     */
    private final RealMatrix apacheData;

    /**
     * The number of rows.
     */
    private int m;

    /**
     * The number of columns.
     */
    private int n;

    /**
     * <p>Constructor for Matrix.</p>
     *
     * @param data an array of  objects
     */
    public Matrix(double[][] data) {
        if (data.length == 0) {
            this.apacheData = new Array2DRowRealMatrix();
        } else {
            this.apacheData = org.apache.commons.math3.linear.MatrixUtils.createRealMatrix(data);
        }

        this.m = data.length;
        this.n = this.m == 0 ? 0 : data[0].length;
    }

    /**
     * <p>Constructor for Matrix.</p>
     *
     * @param data a {@link org.apache.commons.math3.linear.RealMatrix} object
     */
    public Matrix(RealMatrix data) {
        this.apacheData = data;

        this.m = data.getRowDimension();
        this.n = data.getColumnDimension();
    }

    /**
     * <p>Constructor for Matrix.</p>
     *
     * @param m a int
     * @param n a int
     */
    public Matrix(int m, int n) {
        if (m == 0 || n == 0) {
            this.apacheData = new Array2DRowRealMatrix();
        } else {
            this.apacheData = org.apache.commons.math3.linear.MatrixUtils.createRealMatrix(m, n);
        }

        this.m = m;
        this.n = n;
    }

    /**
     * <p>Constructor for Matrix.</p>
     *
     * @param m a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix(Matrix m) {
        this(m.apacheData.copy());
    }

    /**
     * <p>identity.</p>
     *
     * @param rows a int
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public static Matrix identity(int rows) {
        Matrix m = new Matrix(rows, rows);
        for (int i = 0; i < rows; i++) m.set(i, i, 1);
        return m;
    }

    /**
     * <p>sparseMatrix.</p>
     *
     * @param m a int
     * @param n a int
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public static Matrix sparseMatrix(int m, int n) {
        return new Matrix(new OpenMapRealMatrix(m, n).getData());
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public static Matrix serializableInstance() {
        return new Matrix(0, 0);
    }

    /**
     * <p>assign.</p>
     *
     * @param matrix a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public void assign(Matrix matrix) {
        if (this.apacheData.getRowDimension() != matrix.getNumRows() || this.apacheData.getColumnDimension() != matrix.getNumColumns()) {
            throw new IllegalArgumentException("Mismatched matrix size.");
        }

        for (int i = 0; i < this.apacheData.getRowDimension(); i++) {
            for (int j = 0; j < this.apacheData.getColumnDimension(); j++) {
                this.apacheData.setEntry(i, j, matrix.get(i, j));
            }
        }
    }

    /**
     * <p>getNumColumns.</p>
     *
     * @return a int
     */
    public int getNumColumns() {
        return this.n;
    }

    /**
     * <p>diag.</p>
     *
     * @return a {@link edu.cmu.tetrad.util.Vector} object
     */
    public Vector diag() {
        double[] diag = new double[this.apacheData.getRowDimension()];

        for (int i = 0; i < this.apacheData.getRowDimension(); i++) {
            diag[i] = this.apacheData.getEntry(i, i);
        }

        return new Vector(diag);
    }

    /**
     * <p>getSelection.</p>
     *
     * @param rows an array of  objects
     * @param cols an array of  objects
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix getSelection(int[] rows, int[] cols) {
        Matrix m = new Matrix(rows.length, cols.length);

        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < cols.length; j++) {
                m.set(i, j, this.apacheData.getEntry(rows[i], cols[j]));
            }
        }

        return m;

//        if (rows.length == 0 || cols.length == 0) {
//            return new Matrix(rows.length, cols.length);
//        }
//
//        RealMatrix subMatrix = this.apacheData.getSubMatrix(rows, cols);
//        return new Matrix(subMatrix.getData());
    }

    /**
     * <p>copy.</p>
     *
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix copy() {
        if (zeroDimension()) return new Matrix(getNumRows(), getNumColumns());
        return new Matrix(this.apacheData.copy());
    }

    /**
     * <p>getColumn.</p>
     *
     * @param j a int
     * @return a {@link edu.cmu.tetrad.util.Vector} object
     */
    public Vector getColumn(int j) {
        if (zeroDimension()) {
            return new Vector(getNumRows());
        }

        return new Vector(this.apacheData.getColumn(j));
    }

    /**
     * <p>times.</p>
     *
     * @param m a {@link edu.cmu.tetrad.util.Matrix} object
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix times(Matrix m) {
        if (this.zeroDimension() || m.zeroDimension())
            return new Matrix(this.getNumRows(), m.getNumColumns());
        else {
            return new Matrix(this.apacheData.multiply(m.apacheData));
        }
    }

    /**
     * <p>times.</p>
     *
     * @param v a {@link edu.cmu.tetrad.util.Vector} object
     * @return a {@link edu.cmu.tetrad.util.Vector} object
     */
    public Vector times(Vector v) {
        if (v.size() != this.apacheData.getColumnDimension()) {
            throw new IllegalArgumentException("Mismatched dimensions.");
        }

        double[] y = new double[this.apacheData.getRowDimension()];

        for (int i = 0; i < this.apacheData.getRowDimension(); i++) {
            double sum = 0.0;

            for (int j = 0; j < this.apacheData.getColumnDimension(); j++) {
                sum += this.apacheData.getEntry(i, j) * v.get(j);
            }

            y[i] = sum;
        }

        return new Vector(y);
    }

    /**
     * <p>toArray.</p>
     *
     * @return an array of  objects
     */
    public double[][] toArray() {
        return this.apacheData.getData();
    }

    /**
     * <p>Getter for the field <code>apacheData</code>.</p>
     *
     * @return a {@link org.apache.commons.math3.linear.RealMatrix} object
     */
    public RealMatrix getApacheData() {
        return this.apacheData;
    }

    /**
     * <p>get.</p>
     *
     * @param i a int
     * @param j a int
     * @return a double
     */
    public double get(int i, int j) {
        return this.apacheData.getEntry(i, j);
    }

    /**
     * <p>like.</p>
     *
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix like() {
        return new Matrix(this.apacheData.getRowDimension(), this.apacheData.getColumnDimension());
    }

    /**
     * <p>set.</p>
     *
     * @param i a int
     * @param j a int
     * @param v a double
     */
    public void set(int i, int j, double v) {
        this.apacheData.setEntry(i, j, v);
    }

    /**
     * <p>getRow.</p>
     *
     * @param i a int
     * @return a {@link edu.cmu.tetrad.util.Vector} object
     */
    public Vector getRow(int i) {
        if (zeroDimension()) {
            return new Vector(getNumColumns());
        }

        return new Vector(this.apacheData.getRow(i));
    }

    /**
     * <p>getPart.</p>
     *
     * @param i a int
     * @param j a int
     * @param k a int
     * @param l a int
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix getPart(int i, int j, int k, int l) {
        return new Matrix(this.apacheData.getSubMatrix(i, j, k, l));
    }

    /**
     * Returns the inverse of the matrix. If the matrix is not square, an exception is thrown. If the matrix is singular,
     * an exception is thrown.
     *
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     * @throws org.apache.commons.math3.linear.SingularMatrixException if any.
     */
    public Matrix inverse() throws SingularMatrixException {
        if (!isSquare()) throw new IllegalArgumentException("This matrix is not square; cannot invert.");

        if (getNumRows() == 0) {
            return new Matrix(0, 0);
        }

        return new Matrix(org.apache.commons.math3.linear.MatrixUtils.inverse(this.apacheData, 1e-10));
    }

    /**
     * Returns the Moore-Penrose pseudoinverse of the matrix.
     *
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix pseudoinverse() {
        if (zeroDimension()) return new Matrix(getNumColumns(), getNumRows());
        SingularValueDecomposition svd = new SingularValueDecomposition(this.apacheData);
        RealMatrix pseudoinverse = svd.getSolver().getInverse();
        return new Matrix(pseudoinverse);
    }

    /**
     * <p>assignRow.</p>
     *
     * @param row     a int
     * @param doubles a {@link edu.cmu.tetrad.util.Vector} object
     */
    public void assignRow(int row, Vector doubles) {
        this.apacheData.setRow(row, doubles.toArray());
    }

    /**
     * <p>assignColumn.</p>
     *
     * @param col     a int
     * @param doubles a {@link edu.cmu.tetrad.util.Vector} object
     */
    public void assignColumn(int col, Vector doubles) {
        this.apacheData.setColumn(col, doubles.toArray());
    }

    /**
     * <p>trace.</p>
     *
     * @return a double
     */
    public double trace() {
        return this.apacheData.getTrace();
    }

    /**
     * <p>det.</p>
     *
     * @return a double
     */
    public double det() {
        return new LUDecomposition(this.apacheData, 1e-6D).getDeterminant();
    }

    /**
     * <p>transpose.</p>
     *
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix transpose() {
        if (zeroDimension()) return new Matrix(getNumColumns(), getNumRows());
        return new Matrix(this.apacheData.transpose());
    }

    /**
     * <p>equals.</p>
     *
     * @param m         a {@link edu.cmu.tetrad.util.Matrix} object
     * @param tolerance a double
     * @return a boolean
     */
    public boolean equals(Matrix m, double tolerance) {
        for (int i = 0; i < this.apacheData.getRowDimension(); i++) {
            for (int j = 0; j < this.apacheData.getColumnDimension(); j++) {
                if (FastMath.abs(this.apacheData.getEntry(i, j) - m.apacheData.getEntry(i, j)) > tolerance) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * <p>isSquare.</p>
     *
     * @return a boolean
     */
    public boolean isSquare() {
        return getNumRows() == getNumColumns();
    }

    /**
     * <p>isSymmetric.</p>
     *
     * @param tolerance a double
     * @return a boolean
     */
    public boolean isSymmetric(double tolerance) {
        return MatrixUtils.isSymmetric(this.apacheData.getData(), tolerance);
    }

    /**
     * <p>minus.</p>
     *
     * @param mb a {@link edu.cmu.tetrad.util.Matrix} object
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix minus(Matrix mb) {
        if (mb.getNumRows() == 0 || mb.getNumColumns() == 0) return this;
        return new Matrix(this.apacheData.subtract(mb.apacheData));
    }

    /**
     * <p>norm1.</p>
     *
     * @return a double
     */
    public double norm1() {
        return this.apacheData.getNorm();
    }

    /**
     * <p>plus.</p>
     *
     * @param mb a {@link edu.cmu.tetrad.util.Matrix} object
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix plus(Matrix mb) {
        if (mb.getNumRows() == 0 || mb.getNumColumns() == 0) return this;
        return new Matrix(this.apacheData.add(mb.apacheData));
    }

    /**
     * <p>rank.</p>
     *
     * @return a int
     */
    public int rank() {
        SingularValueDecomposition singularValueDecomposition = new SingularValueDecomposition(this.apacheData);
        return singularValueDecomposition.getRank();
    }

    /**
     * <p>getNumRows.</p>
     *
     * @return a int
     */
    public int getNumRows() {
        return this.m;
    }

    /**
     * <p>scalarMult.</p>
     *
     * @param scalar a double
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix scalarMult(double scalar) {
        Matrix newMatrix = copy();
        for (int i = 0; i < getNumRows(); i++) {
            for (int j = 0; j < getNumColumns(); j++) {
                newMatrix.set(i, j, get(i, j) * scalar);
            }
        }

        return newMatrix;
    }

    /**
     * <p>sqrt.</p>
     *
     * @return a {@link edu.cmu.tetrad.util.Matrix} object
     */
    public Matrix sqrt() {
        SingularValueDecomposition svd = new SingularValueDecomposition(this.apacheData);
        RealMatrix U = svd.getU();
        RealMatrix V = svd.getV();
        double[] s = svd.getSingularValues();
        for (int i = 0; i < s.length; i++) s[i] = 1.0 / s[i];
        RealMatrix S = org.apache.commons.math3.linear.MatrixUtils.createRealMatrix(s.length, s.length);
        for (int i = 0; i < s.length; i++) S.setEntry(i, i, s[i]);
        RealMatrix sqrt = U.multiply(S).multiply(V);
        return new Matrix(sqrt);
    }

    /**
     * <p>sum.</p>
     *
     * @param direction a int
     * @return a {@link edu.cmu.tetrad.util.Vector} object
     */
    public Vector sum(int direction) {
        if (direction == 1) {
            Vector sums = new Vector(getNumColumns());

            for (int j = 0; j < getNumColumns(); j++) {
                double sum = 0.0;

                for (int i = 0; i < getNumRows(); i++) {
                    sum += this.apacheData.getEntry(i, j);
                }

                sums.set(j, sum);
            }

            return sums;
        } else if (direction == 2) {
            Vector sums = new Vector(getNumRows());

            for (int i = 0; i < getNumRows(); i++) {
                double sum = 0.0;

                for (int j = 0; j < getNumColumns(); j++) {
                    sum += this.apacheData.getEntry(i, j);
                }

                sums.set(i, sum);
            }

            return sums;
        } else {
            throw new IllegalArgumentException("Expecting 1 (sum columns) or 2 (sum rows).");
        }
    }

    /**
     * <p>zSum.</p>
     *
     * @return a double
     */
    public double zSum() {
        return new DenseDoubleMatrix2D(this.apacheData.getData()).zSum();
    }

    private boolean zeroDimension() {
        return getNumRows() == 0 || getNumColumns() == 0;
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        if (getNumRows() == 0) {
            return "Empty";
        } else {
            return MatrixUtils.toString(toArray());
        }
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
     * Reads the object from the specified ObjectInputStream. This method is used during deserialization to restore the
     * state of the object.
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



