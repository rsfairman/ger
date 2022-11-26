package vcnc.ui.TabbedPaneDnD;

/*

The front-end for a kind of JTabbedPane in which you can drag the tabs
around to reorder them. The tabs also have close boxes and can detect
double-clicks.

For the most part, it should appear to the user no different than a normal
JTabbedPane.

I took the basic framework from github (MIT License), but it's been changed
extensively. I took out various things I don't care about (like
scrolling tabs, or tabs to the side), added things I do care about, like
a tab-close button and the ability to DnD to other windows, and generally 
reorganized to make it easier to follow. In v01 of the main program there
are various version of this code leading up to what is here; I took those
versions out of the main stream of development.

The entire DnD process is confusing. Here's what's going on.

This class (TabbedPaneDnD) registers to listeners in its constructor:
a TabDropTargetListener and a TabDragGestureListener. We have

TabDragGestureListener implements the DragGestureListener interface.
There is also a DragGestureRecognizer, but we use the default recognizer
when TabDragGestureListener is registered (in the constructor). I'm not
sure *exactly* how the default recognizer determines when it's recognized
an appropriate gesture, but it seems to consider any click-and-drag
activity with the mouse to be a valid gesture. Once it has "recognized" the
gesture, it sends it on to TabGestureListener.dragGestureRecognized(),
which is the only method of the interface. So that's how things get started.

When TabDragGestureListener.dragGestureRecognized() hears about an event,
it receives a DragGestureEvent, and that event must be told that the
drag is starting, by calling DragGestureEvent.startDrag(), which takes
a TabTransferable object and a DragSourceListener. The TabTransferable 
implements Transferable, and it carries the information needed by the 
recipient of the DnD to receive the drop; in our case, this information is 
minimal: knowing the TabbedPaneDnD involved is enough since we are moving 
the currently selected tab, whatever it is.

The TabDragSourceListener that is the other argument to 
DragGestureEvent.startDrag() implement DragSourceListener. The dragDropEnd()
method of this listener is where the source for the event (the TabbedPaneDnd)
is supposed to hear that the DnD is finished. It also hears about mouse enters
and exits on the source, but these don't do much other than tell Swing
that the nature of the drop has changed (e.g., you can't drop once the mouse
exits the window).
 
The other listener registered in the constructor is a TabDropTargetListener.
This implements DropTargetListener and is something like a mouse listener.
It also hears about enters and exits, and does more to manage the behavior
of the target based on where the drop is currently hovering. The crucial
method here is TabDropTargetListener.drop() since this is where the
drop is made and finalized.

In summary,

mouse activity -> default DragGestureRecognizer -> TabDragGestureListener ->
    DragGestureEvent -> TabTransferable and TabDragSourceListener ->
    TabDropTargetListener

So the steps are: (1) TabGestureListener hears about it and gets everyone
who might care about it ready; (2) the TabDragSourceListener and
TabDropTargetListener hear about enters and exits relaated to potential
drops; (3) eventually, TabDragSourceListener and TabDropTargetListener hear
about the actual drop, if any, or the fact that the drop failed or aborted.

The ghost pane (GhostGlassPane) is where things happen. Using these is a
standard technique for various scenarios. When the DnD starts, the ghost pane
is made visible, and is made invisible when the event is complete. 

*/


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;


public class TabbedPaneDnD extends JTabbedPane {
  
  // The width of the line used to indicate where a tab is about
  // to be dropped. 3 is OK, but a little small I think, and 5 is too much.
  // The color of this line is set in GhostGlassPane.
  private static final int LINE_SIZE = 4;
  
  public final GhostGlassPane glassPane = new GhostGlassPane(this);
  
  // The index of the tab currently being dragged.
  // Set by TabDragGestureListener and used by TabDropTargetListener.
  protected int dragTabIndex = -1;
  
  
  public TabbedPaneDnD() {
    
    super();
    
    // A name is irrelevant, but harmless.
    glassPane.setName("GlassPane");
    
    // Part of java.awt.dnd. A DropTarget accepts drops and a DragSource
    // initiates and handles the drag and drop.
    // The DragSource is "this" so that a click anywhere on the entire
    // area (tabs themselves or the contents of the tab areas) is a source
    // for dragging. The DropTarget is also "this".
    //
    // What's happening is that the TabDropTargetListener hears about the DnD, 
    // sort of like mouse moves (enter, exit, drop). TabDragGestureListener
    // acts like a filter as to whether it wants to accept and being a drag
    // operation.
    //
    // Note that the DropTarget value isn't held anywhere. We call new
    // and it's created and that's enough. I guess (?) it's held by the
    // glass pane and will be disposed from there. Same basic thing for
    // DragSource.
    //
    // BUG: Do I just want ACTION_MOVE? No COPY?
    new DropTarget(glassPane, DnDConstants.ACTION_COPY_OR_MOVE, 
        new TabDropTargetListener(), true);
    DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
        this, DnDConstants.ACTION_COPY_OR_MOVE, new TabDragGestureListener());
  }
  
  @Override public void addTab(String title,Component component) {
    
    // Like a normal addTab(), but with a close box.
    super.addTab(title,component);
    
    // Tabs are always added to the end of the list. Subtract 1 to get 
    // the *index*.
    int addedIndex = this.getTabCount() - 1;
    
    this.setTabComponentAt(addedIndex, new ButtonTabComponent(this));
  }
  
  public void setNotDragging() {
    this.dragTabIndex = -1;
  }

  protected int getTargetTabIndex(Point glassPt) {
    
    // Given a point relative to the glass pane, return the index of the
    // tab that is under this point -- or -1 if none. 
    
    Point tabPt = SwingUtilities.convertPoint(glassPane, glassPt, this);
 
    for (int i = 0; i < getTabCount(); i++)
      {
         Rectangle r = getBoundsAt(i);
         r.translate(-r.width / 2,0);
         if (r.contains(tabPt))
           return i;
       }
    
    // If there are no tabs in the target window, then this fails
    // since the tabcount is zero and there are no bounds for a nonexistent
    // tab. Kind of a special case.
    
    int tc = getTabCount();
    if (tc == 0)
      return -1;
    
     Rectangle r = getBoundsAt(tc - 1);
     r.translate(r.width / 2, 0);
     
     if (r.contains(tabPt) == true)
       return getTabCount(); 
     else
       return -1;
  }
  
  protected void dndAwayTab(int index,TabTransferPacket p) {
    
    // Remove the tab at the given index, as part of a DnD, filling p with 
    // the information needed by the drop target.
    
    // Get the body of the tab (the "component") and what appears in the tab
    // itself (the "tab component"). We don't care about other aspects of
    // tab, like the Icon, since they aren't used, but there is no insertTab()
    // method that doesn't take these, so I have to get them. Maybe using
    // null for these would be OK, but no harm in getting the actual values.
    p.noteTabSpecs(getComponentAt(index),getTabComponentAt(index),
        getTitleAt(index),getIconAt(index),getToolTipTextAt(index),
        isEnabledAt(index));
    
    remove(index);
  }
  
  protected void dndInsertTab(int index,TabTransferPacket p) {
    
    // Take the tab information provided in p and insert a new tab at the
    // given index.
    int count = this.getTabCount();
    
    if (index < 0)
      {
        // Can't "insert" the tab; it must be "added."
        dndAddTab(p);
        return;
      }
      
    insertTab(p.title,p.icon,p.cmp,p.tip,index);
    setEnabledAt(index,p.isEnabled);
    
    // When you drag and drop a disabled tab, it finishes enabled and 
    // selected.
    if (p.isEnabled == true)
      setSelectedIndex(index);
    
    this.setTabComponentAt(index, new ButtonTabComponent(this));
    
  }
  
  protected void dndAddTab(TabTransferPacket p) {
    
    // Much as above, but we start with addTab() rather than insertTab().
    // The tab is thus added to the "end" of the set of tabs. The way this is
    // used, this should only happen when the added tab is the first tab,
    // but this is set up so that it should work if it's being added to an
    // existing set.
    super.addTab(p.title,p.cmp);
    
    int addedIndex = this.getTabCount() - 1;
    this.setTabComponentAt(addedIndex, new ButtonTabComponent(this));
  }
  
  protected void convertTab(int prev, int next) {
    
    // Move a tab from the prev index position to the next index position.
    if (next < 0 || prev == next) 
      return;
    
    // Get the body of the tab (the "component") and what appears in the tab
    // itself (the "tab component"). We don't care about other aspects of
    // tab, like the Icon, since they aren't used, but there is no insertTab()
    // method that doesn't take these, so I have to get them. Maybe using
    // null for these would be OK, but no harm in getting the actual values.
    final Component cmp = getComponentAt(prev);
    final Component tab = getTabComponentAt(prev);
    final String title = getTitleAt(prev);
    final Icon icon = getIconAt(prev);
    final String tip = getToolTipTextAt(prev);
    
    // Note this so that we can keep it the same. 
    final boolean isEnabled = isEnabledAt(prev);
    
    // The target index -- where it will be inserted.
    int tgtIndex = prev > next ? next : next - 1;
    
    remove(prev);
    insertTab(title, icon, cmp, tip, tgtIndex);
    setEnabledAt(tgtIndex, isEnabled);
    
    // When you drag and drop a disabled tab, it finishes enabled and selected.
    if (isEnabled == true)
      setSelectedIndex(tgtIndex);
    
    // I have a component in all tabs (JLabel with an X to close the tab)
    // and when I move a tab the component disappear. I'm not sure why
    // this isn't part of insertTab().
    setTabComponentAt(tgtIndex, tab);
  }
  
  protected void initTargetLine(int next) {
    
    // Set the values for the "target line," which is the line indicating
    // where the tab would be dropped if the mouse is released. The next
    // value is the index of the tab next to which the line goes.
    //
    // BUG: This shouldn't be called "init" since it's used more often.
    
    // Whether the apparent target is the neighbor to a tab, and there
    // should be a vertical "drop line."
    boolean isSideNeighbor = true;
    if ((next < 0) || (dragTabIndex == next))
      isSideNeighbor = false;
    else if ((this.dragTabIndex >= 0) && (next - dragTabIndex == 1))
      isSideNeighbor = false;
    
    if (isSideNeighbor == false)
      {
        // This basically means not to show the "drop line" at all.
        glassPane.setTargetRect(0, 0, 0, 0);
        return;
      }
    
    // Rectangle for this tab, or null if no such tab.
    Rectangle b = getBoundsAt(Math.max(0, next - 1));
    if (b == null)
      return;
    
    Rectangle r = SwingUtilities.convertRectangle(this,b,glassPane);
    int a = Math.min(next, 1);
    glassPane.setTargetRect(r.x + r.width * a - LINE_SIZE / 2,r.y,
        LINE_SIZE, r.height);
  }
  
  public void setGlassVisibility(boolean vis) {
    
    getRootPane().setGlassPane(glassPane);
    this.glassPane.setVisible(vis);
  }
  
  public BufferedImage getImageForGhosting() {

    getRootPane().setGlassPane(glassPane);
    
    Component c = getTabComponentAt(dragTabIndex);
    
    Component theLabel = ((ButtonTabComponent) c).getLabel();
    
    JLabel lab1 = (JLabel) theLabel;
    JLabel lab2 = new JLabel(lab1.getText());
    
    Dimension d = theLabel.getPreferredSize();
    BufferedImage image = new BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = image.createGraphics();
    
    // For reasons I don't understand, having to do with Swing
    // internals, it *seems* (?) that paintComponent() changes the ownership
    // of the given Component. It's like it ceases to be part of the original
    // JTabbedPane (and the particular tab), and becomes part of glassPane.
    // The solution is to duplicate the original label and use that.
    // 
    // BUG: Could this lead to a memory leak?
    SwingUtilities.paintComponent(g2, lab2, glassPane, 0, 0, d.width, d.height);
    
    g2.dispose();
    
    return image;
  }
  
  public void initGlassPane(Point tabPt) {
    
    // Called by TabDragGestureListener.startDrag().
    // It sets up the glass pane and ghost image to be dragged.
    getRootPane().setGlassPane(glassPane);
    
    Component c = getTabComponentAt(dragTabIndex);
    Component theLabel = ((ButtonTabComponent) c).getLabel();
    
    JLabel lab1 = (JLabel) theLabel;
    JLabel lab2 = new JLabel(lab1.getText());
    
    
    Dimension d = theLabel.getPreferredSize();
    BufferedImage image = new BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = image.createGraphics();
    
    // For reasons I don't understand, having to do with Swing
    // internals, it *seems* (?) that paintComponent() changes the ownership
    // of the given Component. It's like it ceases to be part of the original
    // JTabbedPane (and the particular tab), and becomes part of glassPane.
    // The solution is to duplicate the original label and use that.
    // 
    // BUG: Could this lead to a memory leak?
    SwingUtilities.paintComponent(g2, lab2, glassPane, 0, 0, d.width, d.height);
    
    g2.dispose();
    
    glassPane.setImage(image);
    
    if (c != null)
      setTabComponentAt(dragTabIndex, c);
    
    Point glassPt = SwingUtilities.convertPoint(this, tabPt, glassPane);
    glassPane.setPoint(glassPt);
    glassPane.setVisible(true);
  }
}





