package ger;

import java.awt.Color;


/*

Used with the machine setup dialog for the tool table.

*/

import java.awt.Color;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import ger.tooltable.ToolTable;
import ger.tooltable.ToolTableModel;
import ger.tooltable.ToolTurret;
import ger.tpile.DefaultMachine;
import ger.workoffsets.WOTable;
import ger.workoffsets.WOTableModel;
import ger.workoffsets.WorkOffsets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;


class ToolTableTab extends JScrollPane {
  

  private ToolTurret turret = null;
  private ToolTableModel theModel = null;
  
  
  public ToolTableTab(Color bColor) {
    
    //JTextArea test = new JTextArea("blah blah");
//    this.getViewport().add(test);
    
    turret = DefaultMachine.turret;

    //this.theModel = new ToolTableModel(tools);
    this.theModel = new ToolTableModel(turret);
    //ToolTable theTable = new ToolTable(theModel,tools);
    ToolTable theTable = new ToolTable(theModel,turret);
    
    
//    JScrollPane tablePane = new JScrollPane(theTable);
//    this.getContentPane().add(tablePane,"Center");

    this.getViewport().setBackground(bColor);
    
    JTextArea fixup = new JTextArea(
        "\nFor now, because it's a lot of trouble to make this look slick, "+
        "\nunder \"Type\" use 1 for endmills, 2 for drills and 3 for ballmills.\n"+
        "Ignore the Image column. Use tool diameter for D (not radius).\n\n"+
        "Since this is an imaginary machine, and all tools are assumed to\n"+
        "have the same length, the H value should (probably) remain zero.\n");
//    fixup.setBorder(BorderFactory.createEmptyBorder(0,30,5,5));
//    this.getContentPane().add(fixup,"North");
//    fixup.setBackground((new JLabel()).getBackground());
//    fixup.setEditable(false);

//    this.pack();
  }

}
