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

package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.util.NumberFormatUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

/**
 * Plots a ROC plot.
 *
 * @author Frank Wimbely, Joseph Ramsey
 */
class RocPlot extends JPanel implements PropertyChangeListener {

    /**
     * The stored size of this component.
     */
    private Dimension size;

    /**
     * The background color of this component.
     */
    private final Color backgroundColor = Color.WHITE;

    /**
     * Color the axes are drawn in for graphs.
     */
    private final Color boundaryColor = Color.DARK_GRAY;

    /**
     * Color the title is drawn in.
     */
    private final Color titleColor = Color.DARK_GRAY;

    /**
     * Color the plot is drawn in.
     */
    private final Color plotColor = Color.BLUE;

    /**
     * The rectangle within which the graph is to be plotted.
     */
    private Rectangle plotRect;

    /**
     * The insets of the graph from the boundaries of this component.
     */
    private Insets graphInsets = new Insets(25, 25, 25, 25);

    /**
     * The title of this graph, which is drawn at the top of the graph.
     */
    private String title = "ROC Plot";

    /**
     * The font used for bold text.
     */
    private final Font fontBold = new Font("Serif", Font.BOLD, 12);

    /**
     * The font used for titles.
     */
    private final Font titleFont = new Font("Serif", Font.BOLD, 18);

    /**
     * The font metrics for the title font.
     */
    private final FontMetrics titleFm;

    /**
     * The font metrics of the non-bold font.
     */
    private final FontMetrics fm;

    /**
     * The data defining the ROC curve
     */
    private final double[][] points;

    /**
     * A String to be printed on the plot.
     */
    private final String info;

    /**
     * Constructs a power graph using the given model.  The argument points
     * contains the coordinates of x and y for each point.  I.e. points[i][0] is
     * the x coordinate and points[i][1] is the y coordinate of the ith point.
     */
    public RocPlot(final double[][] points, final String title, final String info) {
        this.points = points;
        this.info = info;
        /*
      The font used non-bold text.
     */
        final Font font = new Font("Serif", Font.PLAIN, 12);
        this.fm = getFontMetrics(font);
        this.titleFm = getFontMetrics(this.titleFont);
        setFont(font);

        if (title != null) {
            this.title = title;
        }
    }

    /**
     * Calculates the rectangle in which the graph is plotted.  Labels for the
     * graph are drawn in the margin.
     *
     * @param size the size of this component with respect to which the plot
     *             rectangle is calculated.
     */
    private void calcPlotRect(final Dimension size) {
        final int left = this.graphInsets.left;
        final int top = this.graphInsets.top;
        final int width = size.width - this.graphInsets.left - this.graphInsets.right;
        final int height = size.height - this.graphInsets.top - this.graphInsets.bottom;

        this.plotRect = new Rectangle(left, top, width, height);
    }

    /**
     * Draws the boundary of the plot rectangle.
     *
     * @param g the graphics context.
     */
    private void drawBoundary(final Graphics g) {
        final Rectangle r = getPlotRect();
        g.setColor(this.boundaryColor);
        g.drawRect(r.x, r.y, r.width, r.height);
    }

    /**
     * This method draws tick marks for the x axis.  The x axis represents pi_e,
     * the true probability of effect in treatment, and ranges in value from
     * pi_c to 1.0.
     *
     * @param g the graphics context.
     */
    private void drawXTickMarks(final Graphics g) {
        final Rectangle plotRect = getPlotRect();
        final int yPos = plotRect.y + plotRect.height;
        final NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();

        for (double d = 0.0; d <= 1.0; d += 0.1) {
            final int xPos = getXPos(d);

            g.setColor(this.boundaryColor);
            g.drawLine(xPos, yPos, xPos, yPos - 10);
            g.drawString(nf.format(d), xPos - 3, yPos + 12);
        }
    }

    /**
     * Draws tick marks for the y axis.
     *
     * @param g the graphics context.
     */
    private void drawYTickMarks(final Graphics g) {
        final Rectangle plotRect = getPlotRect();
        final int xPos = plotRect.x;
        final NumberFormat nf = NumberFormat.getInstance();

        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);

        for (double d = 0.0; d <= 1.0; d += 0.1) {
            final int yPos = getYPos(d);

            g.setColor(this.boundaryColor);
            g.drawLine(xPos, yPos, xPos + 10, yPos);

            final String str = nf.format(d);
            final int strWid = this.fm.stringWidth(str);
            final int strHgt = this.fm.getAscent();

            g.drawString(str, xPos - strWid - 5, yPos + strHgt / 2);
        }
    }

    /**
     * Draws the title of the graph.
     *
     * @param g the graphics context.
     */
    private void drawTitle(final Graphics g) {
        final int stringWidth = this.titleFm.stringWidth(this.title);
        final int stringHeight = this.titleFm.getHeight();

        g.setFont(this.titleFont);
        g.setColor(this.titleColor);
        g.drawString(this.title, this.plotRect.x + this.plotRect.width / 2 - stringWidth / 2,
                stringHeight);
    }

    /**
     * @return the stored plot rectangle.
     */
    private Rectangle getPlotRect() {
        return this.plotRect;
    }

    /**
     * @return the preferred size of this component.
     */
    public Dimension getPreferredSize() {
        return new Dimension(600, 600);
    }

    /**
     * @param x the value of x (between pi_c and 1.0).
     * @return a pixel x position which is the proportion x / (range from pi_e
     * to 1.0) from the left side of the unit square to the right.
     */
    private int getXPos(final double x) {
        final Rectangle r = getPlotRect();
        final double range = 1.0;
        return r.x + (int) ((x / range) * (double) this.plotRect.width);
    }

    /**
     * @param y a value between 0.0 and 1.0, inclusive.
     * @return a pixel y position which is the proportion x / 1.0 from the
     * bottom of the unit square to the top.
     */
    private int getYPos(final double y) {
        final Rectangle r = getPlotRect();
        return r.y + r.height - (int) (y * (double) r.height);
    }

    /**
     * Paints the "power" graph for each of the requested measures. Measures are
     * requested by selecting the appropriate checkboxes in the control panel.
     *
     * @param g the graphics context.
     */
    public void paint(final Graphics g) {
        final Dimension size = getSize();

        if ((this.size == null) || !this.size.equals(size)) {
            this.size = size;
            calcGraphInsets();
            calcPlotRect(size);
        }

        g.setColor(this.backgroundColor);
        g.fillRect(0, 0, getWidth(), getHeight());
        drawBoundary(g);
        drawXTickMarks(g);
        drawYTickMarks(g);
        drawTitle(g);
        drawXLabel(g);
        drawYLabel(g);

        g.setColor(this.plotColor);

        // Plot point or lines however you want. For lines, call this repeatedly
        // for relevant points.
        for (final double[] point : this.points) {
            final int x = getXPos(point[0]);
            final int y = getYPos(point[1]);

            //Draw an "x" at the point on the curve.
            g.drawLine(x - 2, y + 2, x + 2, y - 2);
            g.drawLine(x - 2, y - 2, x + 2, y + 2);

            //g.fillRect(getXPos(points[i][0]), getYPos(points[i][1]), 1, 1);
        }

        g.setColor(this.boundaryColor);
        //        g.drawString(info, size.width / 2, size.height / 2);
        g.drawString(this.info, getXPos(0.6), getYPos(0.2));
    }

    private void drawXLabel(final Graphics g) {
        g.setFont(this.fontBold);
        final Rectangle plotRect = getPlotRect();
        /*
      The x axis label.
     */
        final String xLabel = "False Positive Fraction";
        final int stringWidth = this.fm.stringWidth(xLabel);

        // where to begin drawing (the rotated image)
        final Point translate = new Point(
                plotRect.x + plotRect.width / 2 - stringWidth / 2,
                getSize().height - 8);

        g.setColor(this.boundaryColor);
        g.drawString(xLabel, translate.x, translate.y);
    }

    private void drawYLabel(final Graphics g) {
        g.setFont(this.fontBold);

        // # radians to rotate.
        final double theta = -Math.PI / 2;

        final Rectangle plotRect = getPlotRect();
        /*
      The y axis laabel.
     */
        final String yLabel = "True Positive Fraction";
        final int stringWidth = this.fm.stringWidth(yLabel);

        // where to begin drawing (the rotated image)
        final Point translate = new Point(this.fm.getAscent(),
                plotRect.y + (plotRect.height / 2 + stringWidth / 2));

        final Graphics2D g2 = (Graphics2D) g;
        final AffineTransform save = g2.getTransform();

        g2.translate(translate.x, translate.y);
        g2.rotate(theta);

        g2.setColor(this.boundaryColor);
        g2.drawString(yLabel, 0, 0);

        g2.setTransform(save);
    }

    private void calcGraphInsets() {
        final int top = this.titleFm.getHeight() + this.titleFm.getDescent() + 10;
        final int left = (int) (2.5 * this.fm.getHeight());
        final int bottom = 2 * this.fm.getHeight();
        final int right = (int) (2.5 * this.fm.getHeight());

        this.graphInsets = new Insets(top, left, bottom, right);
    }

    /**
     * Reacts to any property change event by repainting the graph.
     *
     * @param e the property change event.
     */
    public void propertyChange(final PropertyChangeEvent e) {
        repaint();
    }
}





