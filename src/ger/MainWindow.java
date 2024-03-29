package ger;

/*

This controls everything and is kicked off by the Main class. Effectively,
the main() for the entire program is this class's constructor if being
run as a GUI.

*/

//BUG: Vector? It's not thread-safe.
import java.util.Vector;

import java.io.File;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.MenuBar;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;

import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JButton;

import ger.graphics.Bogus;
import ger.graphics.Bogus2;
import ger.graphics.RenderFlat;
import ger.graphics.Renderer;

import ger.persist.Persist;

import ger.tpile.DefaultMachine;
import ger.tpile.Statement;
import ger.tpile.Translator;
import ger.tpile.RenderPacket;
import ger.tpile.lex.Lexer;

import ger.ui.TabMgmt.CodeTextArea;
import ger.ui.TabbedPaneDnD.TabbedPaneDnD;

import ger.unittest.UnitTests;

import ger.util.ChoiceDialogRadio;
import ger.util.FileIOUtil;
import ger.util.LList;
import ger.util.LoadOrSaveDialog;


public class MainWindow extends JFrame 
    implements ActionListener, WindowListener {
  
  private TabbedPaneDnD theTabs = null;
  
  // A list of all MainWindow objects (including this one, obviously).
  // It's needed for DnD so that the ghost pane can be turned on whenever
  // a DnD starts.
  private static Vector<MainWindow> AllWindows = new Vector<>(); 
  
	
  public MainWindow() {
    
  	super("Ger");
  	
  	this.addWindowListener(this);
  	
    this.setSize(300,500);
    this.setPreferredSize(new Dimension(600,500));
    initMenus();
    
    this.theTabs = new TabbedPaneDnD();
    
    // BUG: This doesn't look so great. Maybe a different L&F?
    
    // NOTE: I tried SCROLL_TAB_LAYOUT, but WRAP_TAB_LAYOUT seems better for 
    // the user. Also, the DnD stuff for tabs assumes (I think) WRAP_TAB_LAYOUT.
    theTabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
    
    this.getContentPane().add(theTabs);
    
    this.pack();
    this.setVisible(true);
    
    AllWindows.add(this);
  }
  
  private void doNew() {
  	
    // Create a new empty tab for inputting G-code.
    CodeTextArea textPanel = new CodeTextArea("");
    theTabs.addTab("New",textPanel);
  }

  private void doOpen() {
    
    // Open some G-code...
    //
    // BUG: Would be nice to restrict what the user can open.
    
    String[] choice = LoadOrSaveDialog.getLoadChoice("Open...");
    if (choice == null)
      return;
    
    String fileString = FileIOUtil.loadFileToString(choice[0],choice[1]);
    if (fileString == null)
      return;
    
    CodeTextArea textPanel = new CodeTextArea(fileString);
    theTabs.addTab(choice[1],textPanel);
    theTabs.setSelectedComponent(textPanel);
  }

  public void doOpen(String dir,String fname) {
    
    // BUG: for testing
    // Put back the actual "open file" code below.
    // Actually, this is only used for testing. I could get rid of this
    // entirely.
    
    // Open some G-code...
    //
    // BUG: Would be nice to restrict what the user can open.
    
//    String[] choice = LoadOrSaveDialog.getLoadChoice("Open...");
//    if (choice == null)
//      return;
    
    String fileString = FileIOUtil.loadFileToString(dir,fname);
    CodeTextArea textPanel = new CodeTextArea(fileString);
    theTabs.addTab(fname,textPanel);
  }
  
  private void doClose() {
  	
  	// Close file associated with top-most tab.
    int curTab = theTabs.getSelectedIndex();
    theTabs.removeTabAt(curTab);
  }
  
  private void saveToFile() {
    
    
    // BUG: Modify...
    // The user might want to save in various formats,
    // and the contents of the tab could vary.
    

    int curTabIndex = theTabs.getSelectedIndex();
    if (curTabIndex < 0)
        // No tabs exist.
        return;

    Component curTabComponent = theTabs.getComponentAt(curTabIndex);
    
    if (curTabComponent instanceof CodeTextArea == false)
      // Can only save text to a file.
      return;
    
    CodeTextArea gCodeTab = (CodeTextArea) curTabComponent;
    
    String[] choice = LoadOrSaveDialog.getSaveChoice("Save Text File to...");
    if (choice == null)
      return;
    
    FileIOUtil.saveStringAsAscii(choice[0],choice[1],gCodeTab.getTextArea().getText());
    
    // BUG: Fix so that the tab's name changes when the file name changes.
  }
  
  /*
  private void doTestPulses()  throws OutOfMemoryError {
  	
  	// Run the interpreter to get pulses and print them out.
  	// This will make a monstrously long list; it's only useful for debugging.
  	

  	// Set up the source for the code.
  	TextBuffer theText = codePane.getFrontTextBuffer();
  	if (theText == null)
  		// No code open.
  		return;
  	
  	Interpreter interp = null;
  	try {
  		interp = new Interpreter(theText,true,scale,materialX,materialY,materialZ,
  				przX,przY,przZ,X0,Y0,Z0,turret,workOffsets);
  	} catch (Exception e) {
  		JOptionPane.showMessageDialog(this,e.getMessage());
  		return;
  	}
  	
  	int count = 0;
  	
  	int test = 0;

  	StringBuffer theBuffer = new StringBuffer();
  	
  	try {
  		CNCSignal theSignal = interp.getSignal();
  		
  		while (theSignal != null)
  			{
		  		switch (theSignal.signal)
		  			{
	  				case CNCSignal.XPLUS	:
  						theBuffer.append(count+ "\t+1\t0\t0\n");
  						break;
	  				case CNCSignal.XMINUS	:
	  					theBuffer.append(count+ "\t-1\t0\t0\n");
  						break;
	  				case CNCSignal.YPLUS	:
	  					theBuffer.append(count+ "\t0\t1\t0\n");
  						break;
	  				case CNCSignal.YMINUS	:
	  					theBuffer.append(count+ "\t0\t-1\t0\n");
  						break;
	  				case CNCSignal.ZPLUS	:
	  					theBuffer.append(count+ "\t0\t0\t+1\n");
  						break;
	  				case CNCSignal.ZMINUS	:
	  					theBuffer.append(count+ "\t0\t0\t-1\n");
  						break;
		  			}
		  		test = 0;
		  		theSignal = interp.getSignal();
		  		test = 1;
		  		++count;
  			}
  	} catch (Exception e) {
  		JOptionPane.showMessageDialog(this,e.getMessage());
  		return;
  	}

  	codePane.createTab(theBuffer.toString(),"Motor Pulses");
  }
  */
  
  /*
  private void doTestVoxelFrame()  throws OutOfMemoryError {
  	
  	// The interpreter generates pulses, and these pulses are used to
  	// create a toolpaths which is rendered as a set of voxels.
  	//
  	// This is useful mostly for testing. 

  	// Set up the source for the code.
  	TextBuffer theText = codePane.getFrontTextBuffer();
  	if (theText == null)
  		// No code open.
  		return;

  	Interpreter interp = null;
  	try {
  		interp = new Interpreter(theText,true,scale,materialX,materialY,materialZ,
  				przX,przY,przZ,X0,Y0,Z0,turret,workOffsets);
  	} catch (Exception e) {
  		JOptionPane.showMessageDialog(this,e.getMessage());
  		return;
  	}
  	
  	// Run the interpreter to generate the tool path.
  	ArrayList<VoxelStep> path = new ArrayList<VoxelStep>();
  	
  	try {
  		CNCSignal theSignal = interp.getSignal();
  		
  		while (theSignal != null)
  			{
  				// Skip any null signals.
  				if (theSignal.signal != CNCSignal.NULL)
  					{
  						VoxelStep step = new VoxelStep();
  						switch ((int) theSignal.signal)
	  		  			{
	  		  				case CNCSignal.XPLUS	: step.axis = VoxelStep.X;
	  		  																step.move = 1;
	  		  																break;
	  		  				case CNCSignal.XMINUS	: step.axis = VoxelStep.X;
	  																			step.move = -1;
	  																			break;
	  		  				case CNCSignal.YPLUS	: step.axis = VoxelStep.Y;
	  		  																step.move = 1;
	  		  																break;
	  		  				case CNCSignal.YMINUS	: step.axis = VoxelStep.Y;
	  		  																step.move = -1;
	  		  																break;
	  		  				case CNCSignal.ZPLUS	: step.axis = VoxelStep.Z;
	  		  																step.move = 1;
	  		  																break;
	  		  				case CNCSignal.ZMINUS	: step.axis = VoxelStep.Z;
	  		  																step.move = -1;
	  		  																break;
	  		  			}
  						
				  		path.add(step);
  					}
  				
		  		theSignal = interp.getSignal();
  			}
  	} catch (Exception e) {
  		JOptionPane.showMessageDialog(this,e.getMessage());
  		return;
  	}
  	
  	ToolPath fullPath = new ToolPath();
  	fullPath.path = path.toArray(new VoxelStep[0]);
  	
  	// Tool path complete. Render it.
//  	System.out.println("Tool path elements: " +fullPath.path.length);
  	
  	// Invoke a VoxelFrameWindow from Swing.
  	StartVFW starter = new StartVFW(new VoxelSet(fullPath));
  	SwingUtilities.invokeLater(starter);
  }
  */
  
  
  private void doSimplify(int layer) {
    
  	// Run some number of layers of the transpiler. The given layer is where 
    // to stop the translation. Translator defines certain values to be used
    // as the layer count: Translator.ToL00, etc.
  	
    // Look at the top-most tab, but only use it if it's input G-code.
    int curTabIndex = theTabs.getSelectedIndex();
    if (curTabIndex < 0)
        // No tabs exist, so there's nothing to translate.
        return;

    Component curTabComponent = theTabs.getComponentAt(curTabIndex);
    
    if (curTabComponent instanceof CodeTextArea == false)
      // Translation only makes sense for text.
      return;
    
    CodeTextArea gCodeTab = (CodeTextArea) curTabComponent;
    
    String fullyDigested = 
  	      Translator.digest(gCodeTab.getTextArea().getText(),layer);
    
    
    // BUG: Check...when tabs are closed, is the memory fully reclaimed?
    // The code to handle the x-icon on tabs as a button is buried deeply,
    // so not sure if the memory is reclaimed.
    
    CodeTextArea outputTab = new CodeTextArea(fullyDigested);
    
    String subName = null;
    switch (layer) {
      case Translator.ThruLexer       : subName = "Lexer";
                                        break;
      case Translator.ThruParser      : subName = "Parser";
                                        break;
      case Translator.ThruDirectives  : subName = "Directives";
                                        break;
      case Translator.ThruSubProgs    : subName = "Sub-programs";
                                        break;
      case Translator.ThruWizards     : subName = "Wizards";
                                        break;
      case Translator.ThruUnits       : subName = "Units";
                                        break;
      case Translator.ThruWorkOffsets : subName = "Work Offsets";
                                        break;
      case Translator.ThruPolar       : subName = "Polar";
                                        break;
      case Translator.ThruIncremental : subName = "Incremental";
                                        break;
      case Translator.ThruCutterComp  : subName = "Cutter Comp";
                                        break;
      case Translator.ThruEverything  : subName = "Translated";
                                        break;
      case Translator.ThruPrerender   : subName = "Prerender";
                                        break;
                                        
      default : System.err.println("MainWindow.doSimplify() fell through.");
                subName = "Error";
    }
      
    theTabs.addTab(theTabs.getTitleAt(curTabIndex) + ": " +subName,outputTab);

  }
  
  /*
  public void do2DDisplayDA() throws OutOfMemoryError {
  	
  	// Parse G-code to a 1D array of shorts. Display this in 2D by shading pixels
  	// according to how deep they are.
  	// DA = Depth Array.
  	// This is meant primarily as a benchmark. This should be about as fast as
  	// anything could be.
  	//
  	// The surface of the object is represented by a set of shorts, one short for every
  	// square 1/1000th of an inch. So a 1 inch by 1 inch object requires 2meg of memory.
  	
  	long t00 = System.currentTimeMillis();
  	
  	// Set up the source for the code.
  	TextBuffer theText = codePane.getFrontTextBuffer();
  	if (theText == null)
  		// No code open.
  		return;

  	Interpreter interp = null;
  	try {
  		interp = new Interpreter(theText,true,scale,materialX,materialY,materialZ,
  				przX,przY,przZ,X0,Y0,Z0,turret,workOffsets);
  	} catch (Exception e) {
  		JOptionPane.showMessageDialog(this,e.getMessage());
  		return;
  	}
  	
  	// Run the interpreter to generate the tool path.
  	ArrayList<Short> path = new ArrayList<Short>();
  	
  	try {
  		CNCSignal theSignal = interp.getSignal();
  		while (theSignal != null)
  			{
		  		path.add(theSignal.signal);
		  		theSignal = interp.getSignal();
  			}
  	} catch (Exception e) {
  		JOptionPane.showMessageDialog(this,e.getMessage());
  		return;
  	}
  	
  	long t01 = System.currentTimeMillis();
  	long d0 = t01 -t00;
//  	System.out.println("Tool path elements: " +path.size());
//  	System.out.println("Time to fully parse to pulses: " +d0);
  	
  	Cut2D theCut = new Cut2D(path,(int)(scale*turret.getLargest())+50);
  	
  	long t1 = System.currentTimeMillis();
  	theCut.makeCut(path,turret.convertTurret(this.scale));
  	long t2 = System.currentTimeMillis();
  	long d = t2 - t1;
//  	System.out.println("Millis to cut: " +d);
  	
  	Start2DDisplay starter = new Start2DDisplay(theCut);
  	SwingUtilities.invokeLater(starter);
  }
  */
  /*
  private void do3DDisplayLINE() throws OutOfMemoryError {
  	
  	// Parse G-code to a 1D array of shorts in the form of a depth array (DA). Show
  	// this in 3D by drawing each line of the DA to the screen, taking into account 
  	// the possibility that one line may obscure another.
  	

  	// Set up the source for the code.
  	TextBuffer theText = codePane.getFrontTextBuffer();
  	if (theText == null)
  		// No code open.
  		return;
  	
  	Interpreter interp = null;
  	try {
  		interp = new Interpreter(theText,true,scale,materialX,materialY,materialZ,
  				przX,przY,przZ,X0,Y0,Z0,turret,workOffsets);
  	} catch (Exception e) {
  		JOptionPane.showMessageDialog(this,e.getMessage());
  		return;
  	}
  	  	
  	// Run the interpreter to generate the path.
  	ArrayList<Short> path = new ArrayList<Short>();
  	
  	try {
  		CNCSignal theSignal = interp.getSignal();
  		while (theSignal != null)
  			{
		  		path.add(theSignal.signal);
		  		theSignal = interp.getSignal();
  			}
  	} catch (Exception e) {
  		JOptionPane.showMessageDialog(this,e.getMessage());
  		return;
  	}
  	
  	Cut2D theCut = new Cut2D(path,(int)(scale*turret.getLargest())+50);
  	
  	long t1 = System.currentTimeMillis();
  	theCut.makeCut(path,turret.convertTurret(this.scale));
  	long t2 = System.currentTimeMillis();
  	long d = t2 - t1;
//  	System.out.println("Millis to cut: " +d);
  	
  	// Here is where this method differs -- the method of rendering.
  	Start3DDisplay starter = new Start3DDisplay(theCut);
  	SwingUtilities.invokeLater(starter);
  }
  */
  /*
  private void makeCut(short[][] da,SignalArray sig) {
  	
  	// Given a set of signals, cut the da. Some of these signals might be things like
  	// a tool change or change in the spindle state.
  	
  	// First, see if this is a non-cutting type of action -- change to the spindle
  	// state (on/off/speed), tool change, etc.
  	
  	// Ignore changes to the spindle.
  	if ((sig.signals.length == 1) && (
  			(sig.signals[0] == CNCSignal.SPCW) || 
  			(sig.signals[0] == CNCSignal.SPCCW) ||
  			(sig.signals[0] == CNCSignal.SPOFF)))
  		return;
  	
  	// The remaining cases are a linear move, an arc move, OR the possibility of
  	// a linear move followed by a tool change.
  	// BUG: I should probably have the interpreter strip the tool change out into
  	// a separate return value to eliminate this special case here.
  	
  	int lastStep = sig.signals.length - 1;
  	if ((sig.signals[lastStep] >= CNCSignal.T01) && 
  			(sig.signals[lastStep] <= CNCSignal.T20))
  		// yes, there's a tool change.
  		lastStep = lastStep - 1;
  		
  	// Handle the actual cuts.
  	// As it turns out, IT DOESN'T MATTER whether this is a linear cut, an arc
  	// or what. Since each step is made strictly in one of the three axes at a time,
  	// the cases are all handled the same way. This code would even work for something
  	// like cutting along a bezier curve, or any other shape.
  	//
  	// This is still confusing to understand. Suppose that at each stage of the process,
  	// the DA is completely "cut" for every step up to that point.
  	// Case 1, the move is in either X or Y with Z constant. Here, we just need to
  	// add the "cusp" along the forward edge of the tool's motion, and there are only
  	// four choices, and they're all sort of the same thing; it's just a question of
  	// orientation.
  	// Case 2, the move is in the upward Z direction. In this case, there is nothing
  	// to do since the cut at lower Z value was already completed.
  	// Case 3, the move is in the downward Z direction. We must cut a complete circle
  	// down to the new depth.
	  //
  	// WAIT: the above is true for an endmill, but not for ball mill or a drill.
  	// In those two cases there is not such a great savings over doing the full
  	// circle with every move. Cases 2 and 3 are the same (no cut or full circle
  	// cut); it's case 1 that's different. Here, we must copy half of the circle
  	// to the new position -- the half of the circle on the side in which the
  	// tool has moved.
  	//
  	// SO AS IT TURNS OUT, it would be faster to handle different cuts (linear or arc)
  	// seperately based on the type of tool. For instance, a linear cut using a ball
  	// mill would be most efficiently done by tracing a series of lines at different 
  	// depths. Cutting an arc would be done by cutting a series of one pixel wide
  	// arcs at various depths, etc.
  	//
  	// THUS, I am not sure that it makes sense to pursue a reimplementation of this
  	// portion of the Java code. I could certainly make it somewhat faster, but 
  	// the whole point of this exercise was to refamiliarlize myself with what
  	// I had done before.
  	//
  	// The more important bottleneck is the interactive rendering.
  	
  	
  	if (lastStep == sig.signals.length - 2)
  		{
  			// The final signal was for a tool change.
  			
//  			handle it.
  		}
  	
  	
  	
  }

  private void doNewDisplay() throws OutOfMemoryError {
  	
  	// The method of display, and means of getting it, that I am aiming for in this 
  	// new version. It's very similar to what was used in an earlier version, but this
  	// should be faster and more efficient, and (I hope) look nicer.
  	
  	System.out.println("This doesn't work....See comments in MainWindow.makeCut().");
  	
  	if (true) return;
  	
  	// Set up the source for the code.
  	TextBuffer theText = codePane.getFrontTextBuffer();
  	if (theText == null)
  		// No code open.
  		return;
  	
  	// Allocate the DA.
  	// For now, this is hard-coded as a 4 inch by 4 inch surface.
  	// I am using shorts rather than floats. These have a range of -32767 to +32768.
  	// Divide the values that appear here by 1,000 to get the true value in inches.
  	// BUG: I am using shorts for now because I think they will be faster, and I want
  	// to see how fast they are. Eventually, I may want to use floats.
  	short[][] da = new short[4096][4096];
  	
  	Interpreter interp = null;
  	try {
  		interp = new Interpreter(theText,true,scale,materialX,materialY,materialZ,
  				przX,przY,przZ,X0,Y0,Z0,turret,workOffsets);
  	} catch (Exception e) {
  		JOptionPane.showMessageDialog(this,e.getMessage());
  		return;
  	}

  	// Run the interpreter to generate the path.
  	ArrayList<Short> path = new ArrayList<Short>();
  	
  	try {
  		
  		// Get the signals for the next line of G-code. This could be something
  		// like a tool change too. Note that this does not pass up whether we are in
  		// rapid (G0) mode or feed rates. It does return whether the spindle is on
  		// or off.
  		SignalArray theSignals = interp.nextStatementToSignals();
  		
  		// When this is null, we have reached the EOF.
  		while (theSignals != null)
  			{
  				makeCut(da,theSignals);
  				theSignals = interp.nextStatementToSignals();
  			}
  		
  		
  	} catch (Exception e) {
  		JOptionPane.showMessageDialog(this,e.getMessage());
  		return;
  	}
  }
  
  public void doToolTable() throws OutOfMemoryError {
  	
  	// Bring up the tool table.
  	
//  	I think that the thing to do is use a JTable. Make the left column have a popup menu with
//  	the usual tools, then a picture of the tool, then D and H. Later I can let the user
//  	double click on the picture to bring up an editor to create oddball tools.
//  	
//  	For JTables, see old machine, then my docs, then jbprobject/OLD STUFF, !large progs, flow
//  	The progtrd program has some too, as does portopt, bopt and balancer
  	
  	ToolDialog theDialog = new ToolDialog(turret);
  	theDialog.setVisible(true);
  	theDialog = null;
  	
//  	System.out.println("after dialog");
//  	for (int i = 0; i < turret.theTools.length; i++)
//  		System.out.println(i+ " has D = " +turret.theTools[i].D+ " and diam = " +turret.theTools[i].diameter);
  	
  	// The results are automatically put into this.turret by the TooLDialog class.
  	// See ToolDialog.actionPerformed() for what happens when the user clicks "OK".
  }
  */
  
  
  private void doMachineSetup() {
  
    // Machine-wide settings: tool table, work offsets table, inch/mm 
    // choice, etc.
    //
    // Because these things are tied together (particularly the inch/mm 
    // choice with the others), all of these are handled by a single dialog.
    // This should make it clear to the user that these choices are a
    // conceptual unit.
    MachineSetupDialog dlog = new MachineSetupDialog(this);
    dlog.show();
    
    
  }
  
  private void doChangeMachine() {
    
    // Choose a .ger directory. This has the effect of choosing a particular
    // tool table, set of wizards, etc.
    
    // Make sure that they understand what they're doing.
    // If the current .ger directory being used is *not* the default, then 
    // don't bother asking. Of course, if the user went to a non-default
    // directory, then came back to the default, then this will ask again,
    // which may be mildly annoying to the user. The problem is that the
    // alternative is to store another persistent thing (did the user turn
    // off this message permanently using some kind of checkbox?) and that is 
    // to be avoided.
    if (Persist.usingDefault() == true)
      {
        JOptionPane sure = new JOptionPane(
            "Are you sure that you want to change the preferences directory?\n" +
             "Unless you want to store different machine setups, using the\n" +
             "default preferences directory should be sufficient.",
             JOptionPane.QUESTION_MESSAGE,JOptionPane.YES_NO_OPTION);
        JDialog dlog = sure.createDialog(this,"Check");
        dlog.show();
        
        // Result is Integer type (or null).
        Object selected = sure.getValue();
        if (selected == null)
          // User closed the dialog without really answering. Treat as "no."
          return;
        
        if ((Integer) selected == 1)
          // An explicit "no."
          return;
      }
    
    // Got here, so the user really does want to change the preferences dir.
    JFileChooser chooser = new JFileChooser(".");
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    
    int returnVal = chooser.showOpenDialog(this);
    if(returnVal != JFileChooser.APPROVE_OPTION)
      // Must have cancelled.
      return;
    
    File theDir = chooser.getSelectedFile();
    
    System.out.println("You chose to open this path: " +
            theDir.getAbsolutePath());
    
    Persist.setGerLocation(theDir.getAbsolutePath());
    
    //Persist.gerDir = theDir.getAbsolutePath();
  }
  
  private void doToggleLineNumbers() {
  
    // Provided that the from tab of the window has G-code (the TabbedType
    // is G_INPUT), toggle display of the line numbers.
    
    // BUG: This only works for input G-code (as intended), and it should
    // be grayed out or something for other types of tab.
    
    int curTabIndex = theTabs.getSelectedIndex();
    if (curTabIndex < 0)
        // No tabs exist.
        return;
    
    Component curTabComponent = theTabs.getComponentAt(curTabIndex);
    
    if (curTabComponent instanceof CodeTextArea == false)
      // Line numbers only make sense for text.
      return;
    
    CodeTextArea gCodeTab = (CodeTextArea) curTabComponent;
    gCodeTab.toggleLineNumbers();
  }
  
  public MainWindow doNewWindow() {
    
    // Create a new window, like this one, with no tabs.
    // public because the program starts by calling this from main().
    MainWindow mw = new MainWindow();
    return mw;
  }
  
  private void doGetVersion() throws OutOfMemoryError {

    JOptionPane.showMessageDialog(this,
        "This is version 0.017 of February 25, 2023.");
		
  }
  
  private void doSetScale() {
    
    // Change the value in DefaultMachine.scale used when rendering.
    
    // BUG: maybe there should be an outside "get floating-point value"
    // dialog class. Then I could avoid the use of anonymous action listener
    // and things would generally be tidier.
    JDialog dlog = new JDialog(this,"Set Scale...");
    dlog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
    
    dlog.getContentPane().setLayout(new BorderLayout(10,10));
    
    // NOTE: The scale is expressed in absolute terms. Maybe it would be more
    // natural for the scale to be in the range 1-50 (or 100 or whatever),
    // with the idea that, whatever the machine units may be, then the
    // scale is adjusted by this factor. So, if it's an inch machine, and
    // this value is set to 20, then each "step" is 0.020.
    JTextArea msg = new JTextArea(
        "Number of inches or mm to be used per 'step' when\n" +
        "rendering. Smaller numbers provide higher resolution\n" +
        "but images are slower to generate. Anything smaller\n" +
        "than 0.001 (when working in inches) or 0.025 (when\n" +
        "working in mm) is overkill and 10 or 20 times those\n" +
        "values is typically a reasonable choice.\n");
    msg.setBackground(dlog.getBackground());
    
    JPanel inputPanel = new JPanel();
    inputPanel.setLayout(new BoxLayout(inputPanel,BoxLayout.Y_AXIS));
    inputPanel.add(msg);
    
    JTextField inputText = new JTextField(Double.toString(DefaultMachine.scale)); 
    inputPanel.add(inputText);
    
    dlog.getContentPane().add(inputPanel,"Center");
    
    JPanel butPanel = new JPanel();
    JButton but = new JButton("OK"); 
    but.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
//        System.out.println("ok");
        String s = inputText.getText();
        double v = 0.0;
        try {
          v = Double.parseDouble(s);
        } catch (Exception ex) {
          // BUG: Fix so that the user can't type an invalid string.
          // Also, require a value in the range 0.001 to 0.10 (or something).
          return;
        }
        
        DefaultMachine.scale = v;
        dlog.dispose();
      }
    });
    butPanel.add(but);

    but = new JButton("Cancel"); 
    but.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
//        System.out.println("cancel");
        // BUG: Make the escape key work as cancel.
        dlog.dispose();
        return;
      }
    });
    butPanel.add(but);
    
    dlog.getContentPane().add(butPanel,"South");
    
    // Some space around things. In the past, I've used "struts," but
    // a rigid area is less ambiguous. I don't know why you can't reuse the
    // fill value on the East and West, but you need two separate values.
    Component fill = Box.createRigidArea(new Dimension(100,15));
    dlog.getContentPane().add(fill,"North");
    fill = Box.createRigidArea(new Dimension(20,10));
    dlog.getContentPane().add(fill,"East");
    fill = Box.createRigidArea(new Dimension(20,10));
    dlog.getContentPane().add(fill,"West");
    
    dlog.pack();
    dlog.setVisible(true);
   
    
  }
  
  private void doBogusRender() {
    
    // Bogus test, to see if the pipes are connected.
    
    // This only works when the command is given to an input G-code tab,
    // not any kind of output tab.
    int curTabIndex = theTabs.getSelectedIndex();
    if (curTabIndex < 0)
        // No tabs exist, so there's nothing to translate.
        return;

    Component curTabComponent = theTabs.getComponentAt(curTabIndex);
    if (curTabComponent instanceof CodeTextArea == false)
      // Can only save text to a file.
      return;
    
    CodeTextArea gCodeTab = (CodeTextArea) curTabComponent;
        
    theTabs.addTab(theTabs.getTitleAt(curTabIndex)+": Bogus",new Bogus());
  }
  
  private void doBogus2() {
    
    // Want to test using a monochrome raster.
    
    // This only works when the command is given to an input G-code tab,
    // not any kind of output tab.
    int curTabIndex = theTabs.getSelectedIndex();
    if (curTabIndex < 0)
        // No tabs exist, so there's nothing to translate.
        return;

    Component curTabComponent = theTabs.getComponentAt(curTabIndex);
    if (curTabComponent instanceof CodeTextArea == false)
      // Can only save text to a file.
      return;
    
    // It's input code. Digest it fully, but we don't care about the result
    // as a String. We want it as an an AST.
    CodeTextArea gCodeTab = (CodeTextArea) curTabComponent;
    
    theTabs.addTab(theTabs.getTitleAt(curTabIndex)+": Bogus 2",new Bogus2());
  }
  
  private void doFlatPathRender() {
    
    // To render in the simplest possible way, in 2D monochrome.
    // If the tool goes below z=0, then it cuts, and we only track the
    // tool path, not the cutter diameter.
    
    // BUG: if these methods of rendering proliferate, it might make sense
    // to do something like doSimplify(), where all the methods are brought
    // under a single function call.
    
    // BUG: Another problem is that the tab name that's appended should
    // have a suffix, like '-1', '-2', etc. since the user could render and
    // rerender the same file, getting different output each time.
    // If he does this a few times without closing the tabs, then there 
    // could be an out of memory error. Not sure what to do about this.
    // A *single* (and separate) render output window would be one solution. 
    
    // This only works when the command is given to an input G-code tab,
    // not any kind of output tab.
    int curTabIndex = theTabs.getSelectedIndex();
    if (curTabIndex < 0)
        // No tabs exist, so there's nothing to translate.
        return;

    Component curTabComponent = theTabs.getComponentAt(curTabIndex);
    if (curTabComponent instanceof CodeTextArea == false)
      // Can only save text to a file.
      return;
    
    // It's input code. Digest it fully, but we don't care about the result
    // as a String. We want it as an an AST.
    CodeTextArea gCodeTab = (CodeTextArea) curTabComponent;
    
    // BUG: Need to check for errors in the G-code!
    RenderPacket rPacket = Translator.preRender(
        gCodeTab.getTextArea().getText());
    
    // BUG: Size is hard-coded. Seems OK?
    int rSize = Renderer.getFlatPathSize(rPacket);
    if (rSize > 100000000)
      {
        // More than 100 million pixels, which is 10,000 x 10,000.
        int reply = JOptionPane.showConfirmDialog(
            null,
            "The scale may be smaller than necessary.\n" +
            "Continue, even though the image may be so\n" +
            "large that it's hard to work with, and you\n" +
            "may run out of memory?",
            "Continue?",JOptionPane.YES_NO_OPTION,JOptionPane.PLAIN_MESSAGE);
        
        if (reply == JOptionPane.NO_OPTION)
          return;
      }
    
    
    
    
    
    // BUG: Try to catch out of memory exceptions?
    RenderFlat rendered = Renderer.flatPathRender(rPacket);
    
    theTabs.addTab(theTabs.getTitleAt(curTabIndex)+": Flat Path",rendered);
    theTabs.setSelectedComponent(rendered);
    rendered.requestFocusInWindow();
  }
  
  public void actionPerformed(ActionEvent e) {
    
  	try {
  		
	  	if (e.getActionCommand().equals("New"))
	      doNew();
	  	else if (e.getActionCommand().equals("Open"))
	      doOpen();
	  	else if (e.getActionCommand().equals("Close Tab"))
	      doClose();
	  	else if (e.getActionCommand().equals("Save"))
	      saveToFile();
	  	else if (e.getActionCommand().equals("Quit"))
	      System.exit(0);
      else if (e.getActionCommand().equals("Unit Tests"))
        UnitTests.testAll();
      else if (e.getActionCommand().equals("Thru Lexer"))
        doSimplify(Translator.ThruLexer);
      else if (e.getActionCommand().equals("Thru Parser"))
        doSimplify(Translator.ThruParser);
      else if (e.getActionCommand().equals("Thru Directives"))
        doSimplify(Translator.ThruDirectives);
      else if (e.getActionCommand().equals("Thru Sub-programs"))
        doSimplify(Translator.ThruSubProgs);
      else if (e.getActionCommand().equals("Thru Wizards"))
        doSimplify(Translator.ThruWizards);
      else if (e.getActionCommand().equals("Thru Units"))
        doSimplify(Translator.ThruUnits);
      else if (e.getActionCommand().equals("Thru Offsets"))
        doSimplify(Translator.ThruWorkOffsets);
      else if (e.getActionCommand().equals("Thru Polar"))
        doSimplify(Translator.ThruPolar);
      else if (e.getActionCommand().equals("Thru Incremental"))
        doSimplify(Translator.ThruIncremental);
      else if (e.getActionCommand().equals("Thru Cutter Comp"))
        doSimplify(Translator.ThruCutterComp);
      else if (e.getActionCommand().equals("Thru Prerender"))
        doSimplify(Translator.ThruPrerender);
//      else if (e.getActionCommand().equals("Thru Cutter Comp"))
//        doSimplify(Translator.ThruCutterComp);
//    else if (e.getActionCommand().equals("Test Pulses"))
//    doTestPulses();
//  else if (e.getActionCommand().equals("Test Voxel Frame"))
//    doTestVoxelFrame();
//	  	else if (e.getActionCommand().equals("2D Display"))
//	      do2DDisplayDA();
//	  	else if (e.getActionCommand().equals("3D Display"))
//	      do3DDisplayLINE();
//	  	else if (e.getActionCommand().equals("New Display"))
//	      doNewDisplay();
      else if (e.getActionCommand().equals("Scale..."))
        doSetScale();
      else if (e.getActionCommand().equals("Bogus Test"))
        doBogusRender();
      else if (e.getActionCommand().equals("Bogus2"))
        doBogus2();
      else if (e.getActionCommand().equals("Flat Path"))
        doFlatPathRender();
	  	else if (e.getActionCommand().equals("Machine Settings..."))
	  		doMachineSetup();
      else if (e.getActionCommand().equals("Change Machine..."))
        doChangeMachine();
	  	else if (e.getActionCommand().equals("Toggle Line Numbers"))
        doToggleLineNumbers();
	  	else if (e.getActionCommand().equals("New Window"))
          doNewWindow();
      else if (e.getActionCommand().equals("Close Window"))
        this.windowClosing(null);
	  	else if (e.getActionCommand().equals("Get Version"))
	  		doGetVersion();
	  		
  		
  	} catch (OutOfMemoryError err) {
  	  // BUG: Is this still relevant? 
  		JOptionPane.showMessageDialog(this,"Out of memory.\n"
  				+"Close some windows and try again.\n"
  				+"If that doesn't work, then restart the program.");
  	}
  }
  
  private void initMenus() {
    
    // NOTE: This uses old style AWT menus. They work, but maybe Swing menus
    // would be better.
    
    MenuBar theMenuBar = new MenuBar();

    Menu theMenu = new Menu("File",false);
    theMenu.add(new MenuItem("New",new MenuShortcut(KeyEvent.VK_N,false)));
    theMenu.add(new MenuItem("Open",new MenuShortcut(KeyEvent.VK_O,false)));
    theMenu.add(new MenuItem("Close Tab",new MenuShortcut(KeyEvent.VK_C,false)));
    theMenu.add(new MenuItem("Save",new MenuShortcut(KeyEvent.VK_S,false)));
    theMenu.add(new MenuItem("Quit",new MenuShortcut(KeyEvent.VK_Q,false)));
    theMenu.addActionListener(this);
    theMenuBar.add(theMenu);
    
    theMenu = new Menu("Translate",false);
    // Having the unit tests here makes it easier to run the symbolic debugger.
    if (Main.DEBUG == true) theMenu.add(new MenuItem("Unit Tests",new MenuShortcut(KeyEvent.VK_U,false)));
    if (Main.DEBUG == true) theMenu.add(new MenuItem("Thru Lexer"));
    theMenu.add(new MenuItem("Thru Parser"));
    theMenu.add(new MenuItem("Thru Directives"));
    theMenu.add(new MenuItem("Thru Sub-programs"));
    theMenu.add(new MenuItem("Thru Wizards"));
    theMenu.add(new MenuItem("Thru Units"));
    theMenu.add(new MenuItem("Thru Offsets"));
    theMenu.add(new MenuItem("Thru Polar"));
    theMenu.add(new MenuItem("Thru Incremental"));
    theMenu.add(new MenuItem("Thru Cutter Comp"));
    theMenu.add(new MenuItem("Thru Prerender"));
//    theMenu.add(new MenuItem("Test Pulses"));
//    theMenu.add(new MenuItem("Test Voxel Frame"));
    theMenu.addActionListener(this);
    theMenuBar.add(theMenu);
    
    theMenu = new Menu("Render",false);
    theMenu.add(new MenuItem("Scale..."));
    theMenu.addSeparator();
//    theMenu.add(new MenuItem("2D Display"));
//    theMenu.add(new MenuItem("3D Display"));
    theMenu.add(new MenuItem("Bogus Test"));
    theMenu.add(new MenuItem("Bogus2"));
    theMenu.add(new MenuItem("Flat Path"));
    theMenu.addActionListener(this);
    theMenuBar.add(theMenu);
    
    theMenu = new Menu("Settings",false);
    theMenu.add(new MenuItem("Machine Settings..."));
    theMenu.add(new MenuItem("Change Machine..."));
    theMenu.addActionListener(this);
    theMenuBar.add(theMenu);

    
    theMenu = new Menu("Window",false);
    theMenu.add(new MenuItem("Toggle Line Numbers"));
    theMenu.add(new MenuItem("New Window"));
    theMenu.add(new MenuItem("Close Window"));
    theMenu.addActionListener(this);
    theMenuBar.add(theMenu);
    
    theMenu = new Menu("Version",false);
    theMenu.add(new MenuItem("Get Version"));
    theMenu.addActionListener(this);
    theMenuBar.add(theMenu);
    
    this.setMenuBar(theMenuBar);
  }
  
  public static void setAllGlassVisibile() {
    
    // Fire up the glass panes for all the windows. 
    // This is called when a DnD starts.
    for (MainWindow w : AllWindows)
      w.theTabs.setGlassVisibility(true);
  }
  
  public static void initAllGlass(TabbedPaneDnD src) {
    
    // When a drag & drop starts, all the ghost panes need to have the ghosty
    // draggable image ready. This image comes from a single source, given
    // as the argument.
    BufferedImage image = src.getImageForGhosting();
    for (MainWindow w : AllWindows)
      {
        w.theTabs.glassPane.setImage(image);
      }
  }
  
  public static void setAllGlassInvisible() {
    
    // Close down the glass planes for all the windows. Called when DnD ends.
    for (MainWindow w : AllWindows)
      w.theTabs.setGlassVisibility(false);
  }
  
  public static void nobodyDragging() {
    
    for (MainWindow w : AllWindows)
      w.theTabs.setNotDragging();
  }
  
  // We don't care about most of these.
  public void windowActivated(WindowEvent e) { ;; }
  public void windowClosed(WindowEvent e) { ;; }
  public void windowDeactivated(WindowEvent e) { ;; }
  public void windowDeiconified(WindowEvent e) { ;; }
  public void windowIconified(WindowEvent e) { ;; }
  public void windowOpened(WindowEvent e) { ;; }

  public void windowClosing(WindowEvent e) {
    
    // This is called before the window has actually closed; it's possible
    // to abort the close operation. In fact, the window will not close
    // unless it's done here; the call is more like a request to close.
    
    // BUG: Eventually, I should check for dirty flags, but just close for now.
    //this.setVisible(false);
    
    // Before disposing, remove the reference to this window from the 
    // global list of windows.
    AllWindows.remove(this);
    
    this.dispose();
  }

  
}


