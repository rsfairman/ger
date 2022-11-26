package vcnc;

// Creates a dialog with short text boxes for user input and labels indicating what 
// to input.
//
// To use this, instantiate the class with a title for the dialog box, an array of 
// labels for the user inputs, and the minimum size that the user input boxes should 
// have. Call getInputs() to find out what the user did with the dialog. If 
// getInputs() returns null, then the user canceled. If the user chose "OK", then 
// getInputs() returns the text typed by the user for each field.

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class TextInputDialog extends JDialog implements ActionListener {
  
  private String[] results = null;
  private JTextField[] inputFields = null;
  
  // Whether all inputs must be parseable to doubles before this will return.
  private boolean forceDoubles = false;
  
  
  public TextInputDialog(String title,String[] labels,int minSize) {
   
    // This is a modal dialog.
  	// Even though the window has a close box, when you hit the close box it's
  	// the same as hitting the cancel button.
    super(new JFrame(),title,true);  
   
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
 
    // Create the labels and text input areas.
    JPanel inputPanel = new JPanel();
    inputFields = new JTextField[labels.length];
    inputPanel.setLayout(new GridLayout(labels.length,2,8,10));
    for (int i = 0; i < labels.length; i++)
      {
        JLabel theLabel = new JLabel(labels[i]);
        inputPanel.add(theLabel);
        
        // Make sure that the input fields have room for at least minSize characters.
        inputFields[i] = new JTextField(minSize);
        inputPanel.add(inputFields[i]);
      }
    this.getContentPane().add(inputPanel,"North");

    this.pack();
  }
  
  public TextInputDialog(String title,String[] labels,String[] initial,int minSize,
  		String instructions) {
  	
  	// As above, but this constructor allow shows a longer informative message
  	// at the top -- the instructions. It also requires that the initial contents
  	// of each input be given.
  	
    super(new JFrame(),title,true);  
   
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
    
    // Create the labels and text input areas.
    // I can't use a GridLayout for topPanel because it assumes that all cells of
    // the grid have the same size.
    JPanel topPanel = new JPanel();
    GridBagLayout gbLayout = new GridBagLayout();
    GridBagConstraints gbConst = new GridBagConstraints();
    topPanel.setLayout(gbLayout);
    
    JPanel inputPanel = new JPanel();
    inputPanel.setLayout(new GridLayout(labels.length,2,8,10));
    
    // This rigamarole is to center the column at the top.
    JTextArea msg = new JTextArea(instructions);
    msg.setMargin(new Insets(0,20,0,20));
    gbConst.gridwidth = GridBagConstraints.REMAINDER;
    gbLayout.setConstraints(msg,gbConst);
    topPanel.add(msg);
    topPanel.add(inputPanel);
    
    inputFields = new JTextField[labels.length];
    for (int i = 0; i < labels.length; i++)
      {
        JLabel theLabel = new JLabel(labels[i]);
        inputPanel.add(theLabel);
        
        // Make sure that the input fields have room for at least minSize characters.
        inputFields[i] = new JTextField(minSize);
        inputFields[i].setText(initial[i]);
        inputPanel.add(inputFields[i]);
      }
    
    // Make the background color of the instructions match the background of
    // everything else.
    msg.setBackground((new JLabel()).getBackground());
    msg.setEditable(false);
    
    this.getContentPane().add(topPanel,"Center");
    
    this.pack();
  }

  public TextInputDialog(String title,String[] labels,String[] initial,int minSize,
  											int columns,String instructions) {
  	
  	// This constructor allows a long informational message at the top
  	// and the text inputs are arranged in the given number of columns.
  	// The number of columns is in terms of the number of labels/input pairs.
  	// E.g., if labels.length = 6, and you want two rows of three, then columns
  	// should equal 3. Forcing a particular order here is a nuisance so the
  	// caller must give the labels in the order they want. This also requires that the
  	// initial contents of each window are given.
  	
    super(new JFrame(),title,true);  
   
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
    
    // Create the labels and text input areas.
    // I can't use a GridLayout for topPanel because it assumes that all cells of
    // the grid have the same size.
    JPanel topPanel = new JPanel();
    topPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    GridBagLayout gbLayout = new GridBagLayout();
    GridBagConstraints gbConst = new GridBagConstraints();
    topPanel.setLayout(gbLayout);
    
    JPanel inputPanel = new JPanel();
    int rows = labels.length / columns;
    if (rows * columns < labels.length)
    	rows += 1;
    inputPanel.setLayout(new GridLayout(rows,2*columns,8,10));
    
    // This rigamarole is to center the column at the top.
    JTextArea msg = new JTextArea(instructions);
    msg.setMargin(new Insets(0,20,0,20));
    gbConst.gridwidth = GridBagConstraints.REMAINDER;
    gbLayout.setConstraints(msg,gbConst);
    topPanel.add(msg);
    topPanel.add(inputPanel);
    
    inputFields = new JTextField[labels.length];
    for (int i = 0; i < labels.length; i++)
      {
        JLabel theLabel = new JLabel(labels[i]);
        inputPanel.add(theLabel);
        
        // Make sure that the input fields have room for at least minSize characters.
        inputFields[i] = new JTextField(minSize);
        inputFields[i].setText(initial[i]);
        inputPanel.add(inputFields[i]);
      }
    
    // Make the background color of the instructions match the background of
    // everything else.
    msg.setBackground((new JLabel()).getBackground());
    msg.setEditable(false);
    
    this.getContentPane().add(topPanel,"Center");
    
    this.pack();
  }

  public void actionPerformed(ActionEvent e) {
   
    if (e.getActionCommand().equals("OK"))
      {
        // Pull the text out of the JTextField items.
        results = new String[inputFields.length];
        for (int i = 0; i < inputFields.length; i++)
          results[i] = inputFields[i].getText();
        
        // If doubles are required, make sure that that's what was given.
        // If they were not given, then return from this method without calling
        // dispose().
        if (forceDoubles == true)
        	{
        		for (int i = 0; i < results.length; i++)
        			{
        				try {
        					double temp = Double.parseDouble(results[i]);
        				} catch (Exception ex) {
        					// Wouldn't parse.
        					return;
        				}
        			}
        	}
      }  
    else if (e.getActionCommand().equals("Cancel"))
      results = null;
    
    this.dispose();
  }

  public String[] getInputs() {
  	this.forceDoubles = false;
    this.setVisible(true);
    return results;
  }
  
  public String[] getDoubleInputs() {
  	
  	// As above, but this assumes that all results must be parseable to doubles.
  	this.forceDoubles = true;
    this.setVisible(true);
    return results;
  }

  public static void main(String[] args) {
   
    // An example of how to use this class.
    String[] labels = new String[2];
    labels[0] = "First Label";
    labels[1] = "Second Label";
   
    TextInputDialog inputDialog = new TextInputDialog("Input Text",labels,10);
    String[] inputs = inputDialog.getInputs();
   
    if (inputs == null)
      System.out.println("The user canceled");
    else
      {
        for (int i = 0; i < labels.length; i++)
          System.out.println("For \"" +labels[i]+ "\" the user input \"" +inputs[i]+ "\"");
      }
    
    System.exit(0);
  }
}