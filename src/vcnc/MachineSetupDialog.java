package vcnc;

/*

To take user input for all the settings for the machine: inch/mm, tool table,
work offsets, etc.

BUG: Not only is this functionally not done, it is visibly ugly. 
And it should go in a ui pacakge?

*/

import java.awt.Frame;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.GridBagConstraints;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JRadioButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SpringLayout;

import vcnc.persist.Persist;
import vcnc.tpile.MachineState;

import javax.swing.JTabbedPane;


class MachineSetupDialog extends JDialog implements ActionListener {

  private static float grayLevel = 0.6f;
  private static Color myGray = new Color(grayLevel,grayLevel,grayLevel);

  // The user's choices in the dialog are stored here while the dialog
  // is open. It's a copy of the relevant items in the MachineState global,
  // and is needed in case the user cancels his changes.
  
  class TempMachineState {
   
   boolean machineInchUnits = true;
     
  }
  
  private TempMachineState tempState = new TempMachineState();
  
  
  public MachineSetupDialog(Frame parent) {
    
    super(parent,"Machine Setup");
    this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
    
    // Copy the relevant items from the static MachineSettings to the 
    // temporary copy used here.
    // BUG: Not doing tool turret or workoffsets yet. Those classes
    // will need some kind of deep copy method.
    tempState.machineInchUnits = MachineState.machineInchUnits;
    
    // The GUI layout...
    // BUG: Looks like I need to set the background colors for all the
    // individual JPanels.
    //this.setBackground(myGray);
    this.getContentPane().setBackground(myGray);
    
    
    // A pair of linked radio buttons for inch/mm. The choice comes with 
    // a reminder. Layout here is annoyingly layered with sub-panels.
    // This is partly due to the fact that the JTextArea for the reminder
    // should be centered, and that's easiest with GridBagLayout.
    JPanel unitPanel = new JPanel();
    unitPanel.setLayout(new GridBagLayout());
    GridBagConstraints gridCon = new GridBagConstraints();
    gridCon.gridx = GridBagConstraints.VERTICAL;
    
    JTextArea msg = new JTextArea(
        "\nThe inch/mm choice applies to all values that appear in the\n" +
        "tool table or the work offsets table. The simulated machine\n" +
        "also starts in the given mode (inch or mm, for G20 or G21)\n" +
        "and it will translate to code expressed in those units.\n");
    msg.setEditable(false);
    msg.setBackground(myGray);
    unitPanel.add(msg,gridCon);
    
    JPanel radioPanel = new JPanel();
    radioPanel.setLayout(new BoxLayout(radioPanel,BoxLayout.Y_AXIS));
    
    ButtonGroup radioGroup = new ButtonGroup();
    
    JRadioButton inchBut = new JRadioButton("inch",true);
    inchBut.addActionListener(this);
    JRadioButton mmBut = new JRadioButton("mm",false);
    mmBut.addActionListener(this);

    radioPanel.add(inchBut);
    radioPanel.add(mmBut);
    
    radioGroup.add(inchBut);
    radioGroup.add(mmBut);
    
    unitPanel.add(radioPanel,gridCon);
    this.getContentPane().add(unitPanel,"North");
    
    // Set the correct choice: inch or mm.
    if (tempState.machineInchUnits == false)
      mmBut.setSelected(true);
    // else defaults to inch
    
    // DONE with the inch/mm area.
    
    // Now, the primary area for input: the tables.
    JTabbedPane theTabs = new JTabbedPane();
    theTabs.add("Work Offsets",new WorkOffsetsTab());
    theTabs.add("Tool Table",new ToolTableTab());
    this.getContentPane().add(theTabs,"Center");
    
    // The OK/Cancel buttons.
    JPanel exitPanel = new JPanel();
    
    JButton but = new JButton("OK");
    but.addActionListener(this);
    exitPanel.add(but);
    
    but = new JButton("Cancel");
    but.addActionListener(this);
    exitPanel.add(but);
    
    this.getContentPane().add(exitPanel,"South");
    
    
    
//    this.getContentPane().setLayout(new SpringLayout());
    
    
    
    
//    this.setSize(300,500);
//    this.setPreferredSize(new Dimension(600,500));
    
    this.pack();
    
    
  }
  
  public void actionPerformed(ActionEvent e) {
     
    if (e.getActionCommand().equals("OK"))
      {
        // User wants to save his changes. 
        
        /*
        // Pull everything out of the table
        // and put it into this.theOffsets so that the caller can see his choices.
        for (int i = 0; i < theOffsets.offset.length; i++)
          {
            Double d = (Double) theModel.getValueAt(i+1,1);
            theOffsets.offset[i][0] = d;

            d = (Double) theModel.getValueAt(i+1,2);
            theOffsets.offset[i][1] = d;

            d = (Double) theModel.getValueAt(i+1,3);
            theOffsets.offset[i][2] = d;
          }
          */
        
        // Make the temporary choices permanent.
        // BUG: Not doing tool turret or work offsets. Note that a deep
        // copy will not be needed in this case.
        MachineState.machineInchUnits = this.tempState.machineInchUnits;
        
        // And save the changes to disk.
        Persist.save();
        
        this.dispose();
      }
    else if (e.getActionCommand().equals("Cancel"))
      // Forget the whole thing...
      this.dispose();
    else if (e.getActionCommand().equals("inch"))
      this.tempState.machineInchUnits = true;
    else if (e.getActionCommand().equals("mm"))
        this.tempState.machineInchUnits = false;
    
    
  }

}
