package ger.workoffsets;

// The JTable in which work offsets are displayed.

import javax.swing.JTable;

public class WOTable extends JTable {
  
  WOTableModel		theModel = null;
  
  public WOTable(WOTableModel theModel) {
  	
   	super(theModel);
   	
    this.theModel = theModel;
  }
}
