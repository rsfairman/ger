package vcnc.ui.TabbedPaneDnD;

/*

Basically a sub-listener of TabDragGestureListener. This is the actual
handler used by that class. Conceptually, it's associated with the 
source for the DnD.

*/

import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;

import vcnc.MainWindow;

class TabDragSourceListener implements DragSourceListener {
  
  @Override public void dragEnter(DragSourceDragEvent e) {
    
    e.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
  }

  @Override public void dragExit(DragSourceEvent e) {
    
    e.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
  }

  @Override public void dragOver(DragSourceDragEvent e) {
    
    // Nothing to do.
  }

  @Override public void dragDropEnd(DragSourceDropEvent e) {
     
     MainWindow.setAllGlassInvisible();
  }

  @Override public void dropActionChanged(DragSourceDragEvent e) {
    // not needed
  }
}
