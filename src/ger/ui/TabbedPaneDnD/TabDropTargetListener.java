package ger.ui.TabbedPaneDnD;

/*

Registered by TabbedPaneDnD constructor. When a DnD finishes, this
gets the message that "something was dropped on you."

*/

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.util.Optional;


class TabDropTargetListener implements DropTargetListener {
  
  private static final Point HIDDEN_POINT = new Point(0, -1000);

  
  private static Optional<GhostGlassPane> getGhostGlassPane(Component c) {
    
    // BUG: Get rid of this horrible thing. It's just casting c to 
    // GhostGlassPane and it makes the code that uses it super verbose.
    Class<GhostGlassPane> clz = GhostGlassPane.class;
    return Optional.ofNullable(c).filter(clz::isInstance).map(clz::cast);
  }


  public void dragEnter(DropTargetDragEvent e) {
    
    getGhostGlassPane(e.getDropTargetContext().getComponent()).ifPresent(glassPane -> {
      Transferable t = e.getTransferable();
      DataFlavor[] f = e.getCurrentDataFlavors();
      if (t.isDataFlavorSupported(f[0])) { // && tabbedPane.dragTabIndex >= 0) {
        e.acceptDrag(e.getDropAction());
      } else {
        e.rejectDrag();
      }
    });
  }

  public void dragExit(DropTargetEvent e) {
    
    getGhostGlassPane(e.getDropTargetContext().getComponent()).
      ifPresent(glassPane -> {
        // XXX: glassPane.setVisible(false);
        glassPane.setPoint(HIDDEN_POINT);
        glassPane.setTargetRect(0, 0, 0, 0);
        glassPane.repaint();
      });
  }

  public void dropActionChanged(DropTargetDragEvent e) {
    // not needed
  }

  public void dragOver(DropTargetDragEvent e) {

    // This sets the vertical rectangle that indicates where the drop will
    // take place.
    Component c = e.getDropTargetContext().getComponent();
    
    getGhostGlassPane(c).ifPresent(glassPane -> {
      Point glassPt = e.getLocation();

      TabbedPaneDnD tabbedPane = glassPane.tabbedPane;
      tabbedPane.initTargetLine(tabbedPane.getTargetTabIndex(glassPt));
      
      glassPane.setPoint(glassPt);
      glassPane.repaint();
    });
  }

  public void drop(DropTargetDropEvent e) {

    System.out.println("target drop");
    
    Component c = e.getDropTargetContext().getComponent();
    
    if ((c instanceof GhostGlassPane) == false)
      return;
    
    GhostGlassPane glassPane = (GhostGlassPane) c;
      
    TabbedPaneDnD destPane = glassPane.tabbedPane;
    
    Transferable t = e.getTransferable();
    DataFlavor[] f = t.getTransferDataFlavors();
    
    TabTransferPacket p = null;
    try {
      
      // These "flavors" don't matter since there is only one flavor 
      // and I ignore it, but Swing won't accept a null argument.
      p = (TabTransferPacket) t.getTransferData(f[0]);
      
      // These exceptions *really* should be impossible.
    } catch (IOException ex) {
      System.out.println("Strange TabTransferPacket error: ");
      ex.printStackTrace();
    } catch (UnsupportedFlavorException ex) {
      System.out.println("Strange UnsupportedFlavorException error: ");
      ex.printStackTrace();
    }
      
    // prev is the old index of the tab, and next is the desired index.
    // Careful when moving within the same window though.
    int prev = p.srcIndex;
    int next = destPane.getTargetTabIndex(e.getLocation());
    System.out.println("initial prev and next: " + prev + " and " +next);
    
    // How the destination index is handled depends on whether we are 
    // dropping to a different window or the same window. The next
    // index doesn't take into account that the prev index will be moved
    // when working within the same window/set of tabs.
      
    if (t.isDataFlavorSupported(f[0]) == true) 
      {
        // BUG: Not sure testing isDataFlavorSupported necessary.
        
        // Make sure that the DnD actually moved the item.
        if (destPane == p.srcPane)
          {
            // DnD within a single window.
            if (prev != next)
              {
                if (prev < next)
                  next -= 1;
                
                // Remove from the source.
                p.srcPane.dndAwayTab(prev,p);
                
                // The target is the set of tabs in this window.
                destPane.dndInsertTab(next,p);
              }
          }
        else
          {
            // Window-to-window DnD.
            if (next < 0)
              {
                // Dropping a tab to a window with no tabs (yet) at all.
                p.srcPane.dndAwayTab(prev,p);
                destPane.dndAddTab(p);
              }
            else
              {
                // Adding to an existing set of tabs.
                p.srcPane.dndAwayTab(prev,p);
                destPane.dndInsertTab(next,p);
              }
          }
        
        e.dropComplete(true);
      } 
    else
      e.dropComplete(false);
      
    // BUG: Not sure if this is necessary since I make it invisible 
    // elsewhere too. See TabDragSourceListner.dragDropEnd().
    glassPane.setVisible(false);
  }
}
