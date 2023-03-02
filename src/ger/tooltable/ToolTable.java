package ger.tooltable;

// Use this to show the tool table. This is a list of turret postions, with
// specifications for each tool. There will be a number, a picture, the D and H 
// values and, in the future, some way of creating oddly shaped tools. 
//
// It's clear what the D-value should mean. It's just the diameter of the
// tool. However, for oddly shaped tools, it may be unclear what should be used
// for the D value, but that's the user's problem and the nature of the problem
// is clear. 
//
// Exactly what to do with H-values is less clear. In the real world, this is 
// supposed to be the length of the tool beyond the base of the turret. I could
// either assume that H=0 puts the tip of the tool in the plane z=0, or I could
// assume that the user has input some actual length for the tool, and that H
// should be chosen to match this length (or not). The latter is more realistic,
// but it will be annoying in practice. One advantage of the latter is that
// it may catch bugs in the user's code. A compromise is to default to assuming
// that H=0 is what the user wants, but to allow him to require that the actual
// tool length and H be specified.
//
// 

// FOR NOW, rather than spend a lot of time on this, make it simple.
// There are only three types of tool (endmill, ballmill and drill), along with
// their diameters. If I want to get fancy, then the drill has an angle.
// It doesn't make sense to create a gui for this.	

import javax.swing.JTable;


public class ToolTable extends JTable {
  
  ToolTableModel		theModel = null;
  
  public ToolTable(ToolTableModel theModel,ToolTurret turret) {
  	
   	super(theModel);
   	
    this.theModel = theModel;
  }
  
//  public TableCellRenderer getCellRenderer(int row,int column) {
//  	
//    if (row == 0)
//      return super.getCellRenderer(row,column);
//      
//    if (column == theModel.SiColumn)
//      return new DoubleRenderer((Double) theModel.getValueAt(row,column),this,3);
//    if (column == theModel.uiColumn || column == theModel.giColumn)
//      return new DoubleRenderer((Double) theModel.getValueAt(row,column),this,0);
//    if ((column == 1) && (row == theModel.pRow || row == theModel.qRow))
//      return new ScienceRendererFancy((Double) theModel.getValueAt(row,column),this,3);
//    else
//     return super.getCellRenderer(row,column);
//  }
  

}
