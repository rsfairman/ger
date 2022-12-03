package vcnc.ui.TabbedPaneDnD;

/*

Needed by the DnD framework since this type is needed by DragGestureEvent.
The primary purpose is to specify what is being DnD-ed.
 
*/

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;


class TabTransferable implements Transferable {
  
  private static final String NAME = "test";
  private final TabbedPaneDnD tabbedPane;
  
  private final int tabIndex;
  
  
  protected TabTransferable(TabbedPaneDnD tabbedPane,int index) {
    this.tabbedPane = tabbedPane;
    this.tabIndex = index;
  }

  @Override public Object getTransferData(DataFlavor flavor) {
    
    return new TabTransferPacket(this.tabbedPane,this.tabIndex);
  }

  @Override public DataFlavor[] getTransferDataFlavors() {
    
    return new DataFlavor[] {
        new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME)};
  }

  @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
    
    return NAME.equals(flavor.getHumanPresentableName());
  }
}