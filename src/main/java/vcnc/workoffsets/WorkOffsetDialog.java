package vcnc.workoffsets;

// Shows a simple JTable for work offsets (G55, G56, etc).

// BUG: No longer used.

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;



public class WorkOffsetDialog extends JDialog implements ActionListener {

	private WorkOffsets theOffsets = null;
	private WOTableModel theModel = null;
	
  
	public WorkOffsetDialog(WorkOffsets theOffsets) {

	  super(new JFrame(),"Work Offsets",true);	
		this.theOffsets = theOffsets;

    // North is text area; South is Okay/Cancel.
    this.getContentPane().setLayout(new BorderLayout(10,10));
    JPanel buttonPanel = new JPanel();
    
    JButton but = new JButton("OK");
    but.addActionListener(this);
    buttonPanel.add(but);
    
    but = new JButton("Cancel");
    but.addActionListener(this);
    buttonPanel.add(but);
    
    this.getContentPane().add(buttonPanel,"South");
    
    this.theModel = new WOTableModel(theOffsets);
    WOTable theTable = new WOTable(theModel);
    
    JScrollPane tablePane = new JScrollPane(theTable);
    this.getContentPane().add(tablePane,"Center");
    
    JTextArea fixup = new JTextArea(
    		"\nEnter the coordinates relative to the PRZ.\n");
    fixup.setBorder(BorderFactory.createEmptyBorder(0,30,5,5));
    this.getContentPane().add(fixup,"North");
    fixup.setBackground((new JLabel()).getBackground());
    fixup.setEditable(false);

    this.pack();
	}

	public void actionPerformed(ActionEvent e) {
	   
    if (e.getActionCommand().equals("OK"))
      {
        // User wants to save his changes. Pull everything out of the table
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
      }
    
    this.dispose();
  }
}
