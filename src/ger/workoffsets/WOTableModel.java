package ger.workoffsets;

// BUG: Absolute mess

import javax.swing.table.AbstractTableModel;

import ger.tpile.DefaultMachine;


public class WOTableModel extends AbstractTableModel {
  
  // Annoying, but it's easiest to have two copies of the same data.
  // It's not really the same either, since data is an array of Strings.
  // No doubt, there's a better way to do this.
  public Object[][] data = null;
  private WorkOffsets theOffsets = null;
  
  
  public String[] rowLabels = new String[] {
      "G54",
      "G55",
  		"G56",
  		"G57",
  		"G58",
  		"G59"
  };
  
  public String[] columnLabels = new String[] {
    "X",
    "Y",
    "Z"
  };
  
  private int  numRows = 7;
  private int  numCols = 4;
  
  
  public WOTableModel(WorkOffsets theOffsets) {
    
    this.theOffsets = theOffsets;
    
    data = new Object[numRows][numCols];
    
    data[0][0] = new String();
    for (int i = 0; i < columnLabels.length; i++)
      data[0][i+1] = columnLabels[i];
    for (int i = 0; i < rowLabels.length; i++)
      data[i+1][0] = rowLabels[i];
    
    for (int i = 1; i < numRows; i++)
    	{
    		data[i][1] = Double.toString(theOffsets.offset[i-1][0]);
    		data[i][2] = Double.toString(theOffsets.offset[i-1][1]);
    		data[i][3] = Double.toString(theOffsets.offset[i-1][2]);
    	}
  }
	
 	public void refresh() {
//  	System.out.println("Called ToolTableModel.refresh().");
  }
  
  public boolean isCellEditable(int row,int col) { 
    
  	if ((row == 0) || (col == 0))
  		return false;
  	else
  		return true;
  }
 	
 	public void setValueAt(Object theValue,int row,int col) {
  	
    // It looks like this generally comes in as a string, even for numbers.
    // But booleans come in as Boolean.

//    System.out.println("Setting " + row + " " +col+ " to " +theValue);
    
 		if ((row == 0) || (col == 0))
 			return;
 		
 		try {
 			double value = Double.parseDouble((String) theValue);
// 			data[row][col] = new Double(value);
 			data[row][col] = theValue;
 		} catch (Exception e) {
 			// Ignore these. They are typically parsing errors, and
 		  // we don't want to take it anyway.
 		  // BUG: The UI is crappy. Should tab from cell to cell better
 		  // and not let the user even type non-numbers.
// 			System.out.println("odd parse error");
 			;
 		}
    
    fireTableDataChanged();
	}
 	
 	public Object getValueAt(int row,int col) { 
//    System.out.println("Getting " + row + " " +col+ " as " +data[row][col]);
    return data[row][col]; }
  public int getRowCount() { return data.length; }
  public int getColumnCount() { return data[0].length; }
  public String getColumnName(int col) { return null; }
  public Class getColumnClass(int col) { return data[0][col].getClass(); }
  
  public void extract() {
    
    // Called when the user hits "OK" to pull the data out of the dialog
    // and make it a permanent part of the initial machine settings.
    // This reaches right out and modifies the machine settings global.
    for (int i = 0; i < WorkOffsets.Rows; i++)
      {
        for (int j = 0; j < WorkOffsets.Cols; j++)
          {
            try {

//              System.out.println("setting at " + i + " " +j+ " from " 
//                    + data[i+1][j+1]);
              
              theOffsets.offset[i][j] = Double.parseDouble((String)data[i+1][j+1]);

            
//              System.out.println("setting at " + i + " " +j+ " to " 
//                    +theOffsets.offset[i][j]+ " from " 
//                    + data[i+1][j+1]);
            } catch (Exception e) {
              // Ignore these. They shouldn't be (?) possible.
//              System.out.println("odd extraction parse error");
              ;
              ;
            }
          }
      }
    
    DefaultMachine.workOffsets.takeCopy(theOffsets);
  }

}
