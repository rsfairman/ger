package vcnc.ui.TabbedPaneDnD;

/*

Registered by TabbedPaneDnD constructor.
This is what listens for the initiation of a DnD.

*/

import java.awt.Point;
import java.awt.Component;

import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.InvalidDnDOperationException;

import javax.swing.JTabbedPane;
import javax.swing.plaf.metal.MetalTabbedPaneUI;

// BUG: Referring to the USER of the class is disastrously bad design.
import vcnc.MainWindow;


class TabDragGestureListener implements DragGestureListener {
  
  private final DragSourceListener handler = new TabDragSourceListener();

  
  @Override public void dragGestureRecognized(DragGestureEvent e) {
      
    Component theComp = e.getComponent();
    if (theComp == null)
      return;
    if ((theComp instanceof TabbedPaneDnD) == false)
      return;
    
    TabbedPaneDnD dnd = (TabbedPaneDnD) theComp;
    int t = dnd.getTabCount();
    if (t <= 0)
      return;
    
    startDrag(e,dnd);
  }

  private void startDrag(DragGestureEvent e, TabbedPaneDnD tabs) {
    
    Point tabPt = e.getDragOrigin();
    int idx = tabs.indexAtLocation(tabPt.x, tabPt.y);
    int selIdx = tabs.getSelectedIndex();
    
    // Always using WRAP_TAB_LAYOUT, but that's OK.
    boolean isTabRunsRotated = !(tabs.getUI() instanceof MetalTabbedPaneUI)
        && tabs.getTabLayoutPolicy() == JTabbedPane.WRAP_TAB_LAYOUT
        && idx != selIdx;
    
    // Set all windows so that they are *not* seeing themselves as dragging
    // a tab that belongs to them.
    MainWindow.nobodyDragging();
    
    // But this one *is* the source for dragging.
    tabs.dragTabIndex = isTabRunsRotated ? selIdx : idx;
    
    // I can't see why a tab would ever be disabled, but check anyway.
    if (tabs.dragTabIndex >= 0 && tabs.isEnabledAt(tabs.dragTabIndex))
      { 
        tabs.initGlassPane(tabPt);
        
        try {
          e.startDrag(DragSource.DefaultMoveDrop,
              new TabTransferable(tabs,tabs.dragTabIndex), handler);
        } catch (InvalidDnDOperationException ex) {
          throw new IllegalStateException(ex);
        }
        
        // And make the glass pane for *all* windows visible (and hence
        // responsive to being a DnD target).
        MainWindow.setAllGlassVisibile();
        MainWindow.initAllGlass(tabs);
      }
  }
}

