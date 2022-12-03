package vcnc;

/*

Used with the machine setup dialog for the workoffsets table.

*/

import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import vcnc.tpile.MachineState;

import vcnc.workoffsets.WorkOffsets;
import vcnc.workoffsets.WOTable;
import vcnc.workoffsets.WOTableModel;



class WorkOffsetsTab extends JScrollPane {
  

  private WorkOffsets theOffsets = null;
  private WOTableModel theModel = null;
  
  
  public WorkOffsetsTab() {
    
    //JTextArea test = new JTextArea("blah blah");
//    this.getViewport().add(test);
    
    theOffsets = MachineState.workOffsets;
    

    //this.getContentPane().setLayout(new BorderLayout(10,10));
//    this.setLayout(new BorderLayout(10,10));
//    JPanel buttonPanel = new JPanel();
    
//    JButton but = new JButton("OK");
//    but.addActionListener(this);
//    buttonPanel.add(but);
    
//    but = new JButton("Cancel");
//    but.addActionListener(this);
//    buttonPanel.add(but);
    
    //this.getContentPane().add(buttonPanel,"South");
//    this.add(buttonPanel,"South");
    
    this.theModel = new WOTableModel(theOffsets);
    WOTable theTable = new WOTable(theModel);
    
//    JScrollPane tablePane = new JScrollPane(theTable);
    this.getViewport().add(theTable);
    //this.getContentPane().add(tablePane,"Center");
    
//    JTextArea fixup = new JTextArea(
//        "\nEnter the coordinates relative to the PRZ.\n");
//    fixup.setBorder(BorderFactory.createEmptyBorder(0,30,5,5));
//    this.getContentPane().add(fixup,"North");
//    fixup.setBackground((new JLabel()).getBackground());
//    fixup.setEditable(false);

//    this.pack();
  }

}
