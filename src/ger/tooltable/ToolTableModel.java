package ger.tooltable;

import javax.swing.table.AbstractTableModel;
import javax.swing.JComboBox;

// BUG: I don't want to to deal with all of the complicated details for this right now.
// I want to use a JComboBox for the tool type (endmill, ballmill, drill, and maybe
// a few others), but to do that I need to define a CellRenderer for the column.

public class ToolTableModel extends AbstractTableModel {
  
  public Object[][] data = null;
  
  public String[] rowLabels = null;
  
  public String[] columnLabels = new String[] {
    "Type",
    "Image",
    "D",
    "H"
  };

  String[] toolTypes = {
  		"Endmill",
  		"Ballmill",
  		"Drill"
  };
  
  final static int   minNumRows = 12;
  private int  numRows = 1;
  private int  numCols = 5;
  
  
  public ToolTableModel(ToolTurret turret) {
    
  	this.numRows = turret.theTools.length + 1;
    data = new Object[numRows][numCols];
    
    rowLabels = new String[turret.theTools.length];
    for (int i = 0; i < turret.theTools.length; i++)
    	rowLabels[i] = Integer.toString(i+1);
    
    data[0][0] = new String();
    for (int i = 0; i < columnLabels.length; i++)
      data[0][i+1] = columnLabels[i];
    for (int i = 0; i < rowLabels.length; i++)
      data[i+1][0] = rowLabels[i];
    
    for (int i = 0; i < turret.theTools.length; i++)
    	{
    		// BUG: This is temporary. I'm using 1 for endmill, 2 for drill and 3 for ballmill.
    		int value = -1;
    		if (turret.theTools[i] instanceof Endmill)
    			value = 1;
    		if (turret.theTools[i] instanceof Drill)
    			value = 2;
    		else if (turret.theTools[i] instanceof Ballmill)
    			value = 3;
    		
    		data[i+1][1] = new Integer(value);
    		data[i+1][3] = new Double(turret.theTools[i].D);
    		data[i+1][4] = new Double(turret.theTools[i].H);
    	}
    
//    data[2][1] = new JComboBox(toolTypes);
    
    
  }
	
 	public void refresh() {
//  	System.out.println("Called ToolTableModel.refresh().");
  }
  
  public boolean isCellEditable(int row,int col) { 
    
  	if (row == 0)
  		return false;
  	if ((col == 1) || (col == 3) || (col == 4))
  		return true;
  	else
  		return false;
  }
 	
 	public void setValueAt(Object theValue,int row,int col) {
  	
    // It looks like this generally comes in as a string, even for numbers.
    // But booleans come in as Boolean.
    
 		try {
 			// theValue should be a string. See if it parses to a number, either an
 			// integer or a double, depending on the column.
 			if (col == 1)
 				{
 					// Tool type. Should be 1, 2 or 3.
 					int value = Integer.parseInt((String) theValue);
 					if ((value > 0) && (value < 4))
 						data[row][col] = new Integer(value);
 					return;
 				}
 			
 			if ((col == 3) || (col == 4))
 				{
 					// Changing either the D or H column. These are doubles.
 					double value = Double.parseDouble((String) theValue);
 					data[row][col] = new Double(value);
 				}
 			
 		} catch (Exception e) {
 			// Ignore these. They are typically parsing errors.
 			;
 		}
    
    fireTableDataChanged();
	}
 	
 	public Object getValueAt(int row,int col) { 
    //System.out.println("Getting " + row + " " +col);
    return data[row][col]; }
  public int getRowCount() { return data.length; }
  public int getColumnCount() { return data[0].length; }
  public String getColumnName(int col) { return null; }
  public Class getColumnClass(int col) { return data[0][col].getClass(); }
  
}





