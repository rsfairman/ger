package vcnc.workoffsets;

import javax.swing.table.AbstractTableModel;


public class WOTableModel extends AbstractTableModel {
  
  public Object[][] data = null;
  
  public String[] rowLabels = new String[] {
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
  
  private int  numRows = 6;
  private int  numCols = 4;
  
  
  public WOTableModel(WorkOffsets theOffsets) {
    
    data = new Object[numRows][numCols];
    
    data[0][0] = new String();
    for (int i = 0; i < columnLabels.length; i++)
      data[0][i+1] = columnLabels[i];
    for (int i = 0; i < rowLabels.length; i++)
      data[i+1][0] = rowLabels[i];
    
    for (int i = 1; i < numRows; i++)
    	{
    		data[i][1] = theOffsets.offset[i-1][0];
    		data[i][2] = theOffsets.offset[i-1][1];
    		data[i][3] = theOffsets.offset[i-1][2];
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
    
 		if ((row == 0) || (col == 0))
 			return;
 		
 		try {
 			double value = Double.parseDouble((String) theValue);
 			data[row][col] = new Double(value);
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
