package ger.ui.TabbedPaneDnD;

/*

Information that specifies the tab being transfered. It's the source
TabbedPaneDnD, plus the index of the tab. This is what is provided by
TabTransferable.getTransferData().

Then, in the drag & drop process, call noteTabSpecs() to store the information
needed for the transfer.

*/

import java.awt.Component;

import javax.swing.Icon;


class TabTransferPacket {
  
  // These are used by the DnD framework proper. 
  TabbedPaneDnD srcPane = null;
  int srcIndex = -1;
  
  // And these are filled in with the information that's actually moved.
  Component cmp;
  Component tab;
  String title;
  Icon icon;
  String tip;
  boolean isEnabled;
  
  
  public TabTransferPacket(TabbedPaneDnD srcPane,int srcIndex) {
    
    this.srcPane = srcPane;
    this.srcIndex = srcIndex;
  }
  
  public void noteTabSpecs(Component cmp,Component tab,String title,
      Icon icon, String tip,boolean isEnabled) {
    
    // When a tab is removed from its source, this is called to note the
    // tab's data so that it can be dropped elsewhere.
    this.cmp = cmp;
    this.tab = tab;
    this.title = title;
    this.icon = icon;
    this.tip = tip;
    this.isEnabled = isEnabled;
  }
}
