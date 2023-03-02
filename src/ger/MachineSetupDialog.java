package ger;

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

import ger.persist.Persist;
import ger.tpile.DefaultMachine;

import javax.swing.JTabbedPane;


class MachineSetupDialog extends JDialog implements ActionListener {

  // For some reason the background colors of the various parts don't seem to
  // match by default so they are explicitly set to this.
  private static float grayLevel = 0.9f;
  private static Color myGray = new Color(grayLevel,grayLevel,grayLevel);

  // The user's choices in the dialog are stored here while the dialog
  // is open. It's a copy of the relevant items in the MachineState global,
  // and is needed in case the user cancels his changes.
  
  // BUG: Get rid of this. Nice idea, but...
  class TempMachineState {
   
   boolean machineInchUnits = true;
   
  }
  
  private TempMachineState tempState = new TempMachineState();
  
  
  WorkOffsetsTab woTab = null;
  
  
  
  
  
  public MachineSetupDialog(Frame parent) {
    
    super(parent,"Machine Setup");
    this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
    
    // Copy the relevant items from the static MachineSettings to the 
    // temporary copy used here.
    Persist.reload();
    
    // BUG: Not doing tool turret or workoffsets yet. Those classes
    // will need some kind of deep copy method. Or will they?
    tempState.machineInchUnits = DefaultMachine.inchUnits;
    
    // The GUI layout...
    // NOTE: Looks like I need to set the background colors for all the
    // individual JPanels. Probably a mistake somewhere.
    //this.setBackground(myGray);
    this.getContentPane().setBackground(myGray);
    
    
    // A pair of linked radio buttons for inch/mm. The choice comes with 
    // a reminder. Layout here is annoyingly layered with sub-panels.
    // This is partly due to the fact that the JTextArea for the reminder
    // should be centered, and that's easiest with GridBagLayout.
    JPanel unitPanel = new JPanel();
    unitPanel.setBackground(myGray);
    unitPanel.setLayout(new GridBagLayout());
    GridBagConstraints gridCon = new GridBagConstraints();
    gridCon.gridx = GridBagConstraints.VERTICAL;
    
    JTextArea msg = new JTextArea(
        "\nThe inch/mm choice applies to all values that appear in the\n" +
        "tool table or the work offsets table. The simulated machine\n" +
        "also starts in the given mode (inch or mm, for G20 or G21)\n" +
        "and it will translate to code expressed in those units.\n\n" +
        "These changes persist over all future runs. Use machine\n" +
        "directives to make temporary per-program changes.\n");
    msg.setBackground(myGray);
    msg.setEditable(false);
    msg.setBackground(myGray);
    unitPanel.add(msg,gridCon);
    
    JPanel radioPanel = new JPanel();
    radioPanel.setBackground(myGray);
    radioPanel.setLayout(new BoxLayout(radioPanel,BoxLayout.Y_AXIS));
    
    ButtonGroup radioGroup = new ButtonGroup();
    
    JRadioButton inchBut = new JRadioButton("inch",true);
    inchBut.setBackground(myGray);
    inchBut.addActionListener(this);
    JRadioButton mmBut = new JRadioButton("mm",false);
    mmBut.setBackground(myGray);
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
//    theTabs.setBackground(myGray);
    
    this.woTab = new WorkOffsetsTab(myGray);
    theTabs.add("Work Offsets",woTab);
    theTabs.add("Tool Table",new ToolTableTab(myGray));
    this.getContentPane().add(theTabs,"Center");
    
    // The OK/Cancel buttons.
    JPanel exitPanel = new JPanel();
    exitPanel.setBackground(myGray);
    
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
        
        System.out.println("will save changes");
        
        // Pull everything out of the table and save it to the initial machine
        // settings.
        woTab.extract();
          
        
        // Make the temporary choices permanent.
        // BUG: Not doing tool turret yet. Note that a deep will be needed.
        DefaultMachine.inchUnits = this.tempState.machineInchUnits;
        
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
