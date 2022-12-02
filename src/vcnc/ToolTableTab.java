package vcnc;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

/*

Used with the machine setup dialog for the tool table.

*/

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import vcnc.tooltable.ToolTable;
import vcnc.tooltable.ToolTableModel;
import vcnc.tooltable.ToolTurret;
import vcnc.tpile.MachineState;
import vcnc.workoffsets.WOTable;
import vcnc.workoffsets.WOTableModel;
import vcnc.workoffsets.WorkOffsets;


class ToolTableTab extends JScrollPane {
  

  private ToolTurret turret = null;
  private ToolTableModel theModel = null;
  
  
  public ToolTableTab() {
    
    //JTextArea test = new JTextArea("blah blah");
//    this.getViewport().add(test);
    
    turret = MachineState.turret;

    //this.theModel = new ToolTableModel(tools);
    this.theModel = new ToolTableModel(turret);
    //ToolTable theTable = new ToolTable(theModel,tools);
    ToolTable theTable = new ToolTable(theModel,turret);
    
    
//    JScrollPane tablePane = new JScrollPane(theTable);
//    this.getContentPane().add(tablePane,"Center");
    
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
