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

package edu.cmu.tetrad.session;


/**
 * Basic implementation of SessionListener with empty methods.
 *
 * @author Joseph Ramsey
 */
public class SessionAdapter implements SessionListener {

    /**
     * Indicates that a node has been added.
     */
    public void nodeAdded(final SessionEvent event) {
    }

    /**
     * Indicates that a node has been removed.
     */
    public void nodeRemoved(final SessionEvent event) {
    }

    /**
     * Indicates that a parent has been added to a node. Note that this implies
     * a child is added to the parent.
     */
    public void parentAdded(final SessionEvent event) {
    }

    /**
     * Indicates that a parent has been removed from a node. Note that this
     * implies a child is removed from the parent.
     */
    public void parentRemoved(final SessionEvent event) {
    }

    /**
     * Indicates that a model has been created for a node.
     */
    public void modelCreated(final SessionEvent event) {
    }

    /**
     * Indicates that a model has been destroyed for a node.
     */
    public void modelDestroyed(final SessionEvent event) {
    }

    /**
     * Indicates that the createModel method has been called but there is more
     * than one model consistent with the parents, so a choice has to be made.
     */
    public void modelUnclear(final SessionEvent event) {
    }

    /**
     * Indicates that a new execution of a simulation edu.cmu.tetrad.study has begun. (Some
     * parameter objects need to be reset for every execution.
     */
    public void executionStarted(final SessionEvent event) {
    }

    /**
     * Indicates that the repetition of some node has changed.
     */
    public void repetitionChanged(final SessionEvent event) {
    }

    /**
     * Indicates that the model is contemplating adding an edge (but hasn't
     * yet).
     */
    public void addingEdge(final SessionEvent event) {
    }
}





