package ger;

/*

Used with the machine setup dialog for the work offsets table.

*/

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import ger.tpile.DefaultMachine;
import ger.workoffsets.WOTable;
import ger.workoffsets.WOTableModel;
import ger.workoffsets.WorkOffsets;



class WorkOffsetsTab extends JScrollPane {
  

  private WorkOffsets theOffsets = null;
  private WOTableModel theModel = null;
  
  
  public WorkOffsetsTab(Color bColor) {
    
    //JTextArea test = new JTextArea("blah blah");
//    this.getViewport().add(test);
//    this.setBackground(bColor);
    
    theOffsets = DefaultMachine.workOffsets.deepCopy();
    

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
    this.getViewport().setBackground(bColor);
    //this.getContentPane().add(tablePane,"Center");
    
//    JTextArea fixup = new JTextArea(
//        "\nEnter the coordinates relative to the PRZ.\n");
//    fixup.setBorder(BorderFactory.createEmptyBorder(0,30,5,5));
//    this.getContentPane().add(fixup,"North");
//    fixup.setBackground((new JLabel()).getBackground());
//    fixup.setEditable(false);

//    this.pack();
  }
  
  public void extract() {
    
    // Called when the user hits "OK" to pull the data out of the dialog
    // and make it a permanent part of the initial machine settings.
    theModel.extract();
  }

}
