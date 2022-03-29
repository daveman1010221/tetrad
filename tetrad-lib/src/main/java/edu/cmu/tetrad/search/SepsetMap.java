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

package edu.cmu.tetrad.search;

import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.TetradSerializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Stores a map from pairs of nodes to separating sets--that is, for each unordered pair of nodes {node1, node2} in a
 * graph, stores a set of nodes conditional on which node1 and node2 are independent (where the nodes are considered as
 * variables) or stores null if the pair was not judged to be independent. (Note that if a sepset is non-null and empty,
 * that should means that the compared nodes were found to be independent conditional on the empty set, whereas if a
 * sepset is null, that should mean that no set was found yet conditional on which the compared nodes are independent.
 * So at the end of the search, a null sepset carries different information from an empty sepset.)</p> <p>We cast the
 * variable-like objects to Node to allow them either to be variables explicitly or else to be graph nodes that in some
 * model could be considered as variables. This allows us to use d-separation as a graphical indicator of what
 * independendence in models ideally should be.</p>
 *
 * @author Joseph Ramsey
 */
public final class SepsetMap implements TetradSerializable {
    static final long serialVersionUID = 23L;

    /**
     * @serial
     */
    private Map<Set<Node>, List<Node>> sepsets = new ConcurrentHashMap<>();
    private Map<Set<Node>, Double> pValues = new ConcurrentHashMap<>();
    private final Map<Node, HashSet<Node>> parents = new HashMap<>();
//    private Set<Set<Node>> correlations;
//    private boolean returnEmptyIfNotSet = false;

    //=============================CONSTRUCTORS===========================//

    public SepsetMap() {
    }

    public SepsetMap(final SepsetMap map) {
        this.sepsets = new HashMap<>(map.sepsets);
        this.pValues = new HashMap<>(map.pValues);
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     */
    public static SepsetMap serializableInstance() {
        return new SepsetMap();
    }

    //=============================PUBLIC METHODS========================//

    /**
     * Sets the sepset for {x, y} to be z. Note that {x, y} is unordered.
     */
    public void set(final Node x, final Node y, final List<Node> z) {
        final Set<Node> pair = new HashSet<>(2);
        pair.add(x);
        pair.add(y);
        if (z == null) {
            this.sepsets.remove(pair);
        } else {
            this.sepsets.put(pair, z);
        }
    }

//    public void setPValue(Node x, Node y, double p) {
//        Set<Node> pair = new HashSet<>(2);
//        pair.add(x);
//        pair.add(y);
//        pValues.put(pair, p);
//    }

    /**
     * Retrieves the sepset previously set for {a, b}, or null if no such set was previously set.
     */
    public List<Node> get(final Node a, final Node b) {
        final Set<Node> pair = new HashSet<>(2);
        pair.add(a);
        pair.add(b);

//        if (correlations != null && !correlations.contains(pair)) {
//            return Collections.emptyList();
//        }

//        if (/*returnEmptyIfNotSet && */ sepsets.get(pair) == null) {
//            return Collections.emptyList();
//        }

        return this.sepsets.get(pair);
    }

    public double getPValue(final Node x, final Node y) {
        final Set<Node> pair = new HashSet<>(2);
        pair.add(x);
        pair.add(y);

        return this.pValues.get(pair);
    }

    public void set(final Node x, final LinkedHashSet<Node> z) {
        if (this.parents.get(x) != null) {
            this.parents.get(x).addAll(z);
        } else {
            this.parents.put(x, z);
        }
    }

    public HashSet<Node> get(final Node x) {
        return this.parents.get(x) == null ? new HashSet<Node>() : this.parents.get(x);
    }

    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof SepsetMap)) {
            return false;
        }

        final SepsetMap _sepset = (SepsetMap) o;
        return this.sepsets.equals(_sepset.sepsets);
    }

    /**
     * Adds semantic checks to the default deserialization method. This method must have the standard signature for a
     * readObject method, and the body of the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from version to version. A readObject method of
     * this form may be added to any class, even if Tetrad sessions were previously saved out using a version of the
     * class that didn't include it. (That's what the "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for
     * help.
     */
    private void readObject(final ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        if (this.sepsets == null) {
            throw new NullPointerException();
        }
    }

    public int size() {
        return this.sepsets.keySet().size();
    }

    public String toString() {
        return this.sepsets.toString();
    }

    /**
     * //     * ( Sets the set of node pairs that are correlated. These are returned by the depth zero search of PC. This set
     * //     * must be complete; it will be assumed that the sepset for any node pair not in this set is the empty set.
     * //
     */
//    public void setCorrelations(Set<Set<Node>> pairs) {
//        this.correlations = pairs;
//    }

//    public boolean isReturnEmptyIfNotSet() {
//        return returnEmptyIfNotSet;
//    }
//
//    public void setReturnEmptyIfNotSet(boolean returnEmptyIfNotSet) {
//        this.returnEmptyIfNotSet = returnEmptyIfNotSet;
//    }
    public void addAll(final SepsetMap newSepsets) {
        this.sepsets.putAll(newSepsets.sepsets);
    }
}





