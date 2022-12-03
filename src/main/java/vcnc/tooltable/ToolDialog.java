package vcnc.tooltable;

// A dialog box for setting the items in the tool table. This displays a ToolTable
// item.
//
// BUG: when you change a value in the table, you must hit return/enter before
// clicking on OK to close the dialog. This is very annoying and confusing to the
// user.
//
// BUG: No longer used.

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;


public class ToolDialog extends JDialog implements ActionListener {
	
	private ToolTurret turret = null;
	private ToolTableModel theModel = null;
	
	
	public ToolDialog(ToolTurret tools) {

    super(new JFrame(),"Tool Turret",true);   
    
    this.turret = tools;
    
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
 

    this.theModel = new ToolTableModel(tools);
    ToolTable theTable = new ToolTable(theModel,tools);
    
    JScrollPane tablePane = new JScrollPane(theTable);
    this.getContentPane().add(tablePane,"Center");
    
    JTextArea fixup = new JTextArea(
    		"\nFor now, because it's a lot of trouble to make this look slick, "+
    		"\nunder \"Type\" use 1 for endmills, 2 for drills and 3 for ballmills.\n"+
    		"Ignore the Image column. Use tool diameter for D (not radius).\n\n"+
    		"Since this is an imaginary machine, and all tools are assumed to\n"+
    		"have the same length, the H value should (probably) remain zero.\n");
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
      	// and put it into this.turret so that the caller can see his choices.
      	for (int i = 0; i < turret.theTools.length; i++)
      		{
      			// Tool type.
      			Integer iValue = (Integer) theModel.getValueAt(i+1,1);
      			int type = iValue;
      			
      			if (type == 1)
      				{
      					// Endmill
      					turret.theTools[i] = new Endmill(turret.theTools[i].D);
      				}
      			else if (type == 2)
      				{
      					// Drill
      					turret.theTools[i] = new Drill(turret.theTools[i].D);
      				}
      			else if (type == 3)
      				{
      					// Ballmill.
      					turret.theTools[i] = new Ballmill(turret.theTools[i].D);
      				}
      			
      			// Get the D and H values after this. These are stored in the base-class 
      			// Tool, and changing these above overwrites the old D and H.
      			Double d = (Double) theModel.getValueAt(i+1,3);
      			turret.theTools[i].D = d;
      			turret.theTools[i].diameter = d;
      			
      			d = (Double) theModel.getValueAt(i+1,4);
      			turret.theTools[i].H = d;
      		}
      }
    
    this.dispose();
  }
}
