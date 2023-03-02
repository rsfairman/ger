package ger.ui.TabbedPaneDnD;

/*

Used with a TabbedPaneDnD. This acts like temporary transparent pane in
which the tabs are dragged. This is the "arena" in which DnD is managed. 

*/

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;


// BUG: This must be visible (public class) because it is referenced once in
// MainWindow. It would be better to avoid that so that only *one* class in
// this package is visible outside the package -- namely, TabbedpaneDnD.

public class GhostGlassPane extends JComponent {
  
  public final TabbedPaneDnD tabbedPane;
  
  // The lineRect is the vertical line to indicate where the tab is
  // about to be dropped. It has width, so it a rectangle.
  private final Rectangle lineRect = new Rectangle();
  private final Color lineColor = new Color(0, 100, 255);
  
  // Current location of the mouse.
  private final Point location = new Point();
  
  // The ghosty image being dragged.
  private transient BufferedImage draggingGhost;

  
  protected GhostGlassPane(TabbedPaneDnD tabbedPane) {
    
    super();
    this.tabbedPane = tabbedPane;
    setOpaque(false);
    // [JDK-6700748]
    // Cursor flickering during D&D when using CellRendererPane with validation - Java Bug System
    // https://bugs.openjdk.java.net/browse/JDK-6700748
    // setCursor(null);
  }

  public void setTargetRect(int x, int y, int width, int height) {
    
    // Call this to update where the tab is about to be dropped.
    lineRect.setBounds(x, y, width, height);
  }

  public void setImage(BufferedImage draggingImage) {
    
    // Provide an image to be dragged. When this is null, there is no
    // dragging happening.
    this.draggingGhost = draggingImage;
  }

  public void setPoint(Point pt) {
    
    // Call this every time the location of the mouse changes (the drag-to
    // location).
    this.location.setLocation(pt);
  }

  @Override public void setVisible(boolean v) {
    
    // Call this with true to "activate" the glass pane, and false when
    // the DnD is done.
    super.setVisible(v);
    
    if (v == false)
      {
        // Setting the rectangle to (0,0,0,0) is harmless, but irrelevant.
        setTargetRect(0, 0, 0, 0);
        setImage(null);
      }
  }

  @Override protected void paintComponent(Graphics g) {
    
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f));
    
    if (draggingGhost != null)
      {
        double xx = location.getX() - draggingGhost.getWidth(this) / 2d;
        double yy = location.getY() - draggingGhost.getHeight(this) / 2d;
        g2.drawImage(draggingGhost, (int) xx, (int) yy, this);
      }
    
    g2.setPaint(lineColor);
    g2.fill(lineRect);
    g2.dispose();
  }
}
