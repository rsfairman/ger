package vcnc;

/*

This controls everything and is kicked off by the Main class. Effectively,
the main() for the entire program is this class's constructor.

*/

//BUG: Vector? It's not thread-safe.
import java.util.Vector;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.Font;
import java.awt.MenuBar;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MenuShortcut;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;

import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import vcnc.ui.TabMgmt.GInputTab;
import vcnc.ui.TabMgmt.LexerTab;
import vcnc.ui.TabMgmt.ParserTab;
import vcnc.ui.TabMgmt.StaticWindow;
import vcnc.ui.TabMgmt.TabbedType;
import vcnc.ui.TabMgmt.TypedDisplayItem;
import vcnc.ui.TabbedPaneDnD.TabbedPaneDnD;
import vcnc.util.FileIOUtil;
import vcnc.util.LoadOrSaveDialog;
import vcnc.util.ChoiceDialogRadio;
import vcnc.tpile.MachineState;
import vcnc.tpile.Translator;
import vcnc.tpile.lex.Lexer;
import vcnc.workoffsets.WorkOffsetDialog;


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
    
    // BUG: I tried SCROLL_TAB_LAYOUT, but WRAP_TAB_LAYOUT seems better for 
    // the user. Also, the DnD stuff for tabs assumes (I think) WRAP_TAB_LAYOUT.
    theTabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
    
    this.getContentPane().add(theTabs);
    
    this.pack();
    this.setVisible(true);
    
    AllWindows.add(this);
  }
  
  public void doNew() {
  	
    // Create a new empty tab for inputting G-code.
    GInputTab textPanel = new GInputTab("");
    theTabs.addTab("New",textPanel);
  }

  public void doOpen() {
    
    // Open some G-code...
    //
    // BUG: Would be nice to restrict what the user can open.
    
    String[] choice = LoadOrSaveDialog.getLoadChoice("Open...");
    if (choice == null)
      return;
    
    String fileString = FileIOUtil.loadFileToString(choice[0],choice[1]);
    GInputTab textPanel = new GInputTab(fileString);
    theTabs.addTab(choice[1],textPanel);
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
    GInputTab textPanel = new GInputTab(fileString);
    theTabs.addTab(fname,textPanel);
  }
  
  public void doClose() {
  	
  	// Close file associated with top-most tab.
    int curTab = theTabs.getSelectedIndex();
    theTabs.removeTabAt(curTab);
  }
  
  public void saveToFile() {
    
    
    // BUG: Modify...
    // The user might want to save in various formats,
    // and the contents of the tab could vary.
    

    int curTabIndex = theTabs.getSelectedIndex();
    if (curTabIndex < 0)
        // No tabs exist.
        return;

    Component curTabComponent = theTabs.getComponentAt(curTabIndex);
    
    TypedDisplayItem curTabTyped = (TypedDisplayItem) curTabComponent;
    if (curTabTyped.type() != TabbedType.G_INPUT)
      // Only makes sense for input G-code.
      return;

    GInputTab gCodeTab = (GInputTab) curTabComponent;
    
    String[] choice = LoadOrSaveDialog.getSaveChoice("Save Text File to...");
    if (choice == null)
      return;
    
    FileIOUtil.saveStringAsAscii(choice[0],choice[1],gCodeTab.getTextArea().getText());
    
    // BUG: Fix so that the tab's name changes when the file name changes.
  }
  
  
  private void doTestLexer() {
  	
  	// Run the lexer, and display the tokens in a new tab.
    // BUG: This shouldn't appear in production -- or probably not. Very few
    // users will care about this. OTOH, it might help them to understand
    // why their program fails and debug the problem, just like the output 
    // of the other layers.
    
    int curTabIndex = theTabs.getSelectedIndex();
    if (curTabIndex < 0)
        // No tabs exist.
        return;

    Component curTabComponent = theTabs.getComponentAt(curTabIndex);
    TypedDisplayItem curTabTyped = (TypedDisplayItem) curTabComponent;
    if (curTabTyped.type() != TabbedType.G_INPUT)
      // Only makes sense for input G-code.
      return;
    
    GInputTab gCodeTab = (GInputTab) curTabComponent;
    String fullyDigested = Lexer.digestAll(gCodeTab.getTextArea().getText());
    
    // Create a new tab, considering the possibility that this might be
    // a re-do and the tab already exists.
    if (gCodeTab.lexOut != null)
      {
        LexerTab lexerOutput = gCodeTab.lexOut;
        lexerOutput.theText.setText(fullyDigested);
      }
    else
      {
        LexerTab lexerOutput = new LexerTab(fullyDigested,gCodeTab);
        theTabs.addTab(theTabs.getTitleAt(curTabIndex) + ": Lexer",lexerOutput);
        gCodeTab.lexOut = lexerOutput;
      }
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
  
  
  public void doSimplify(int layer) throws OutOfMemoryError {
  	
  	// Run some number of layers of the transpiler. The given layer is where 
    // to stop the translation. layer == -1 means to take the output of the 
    // Parser, layer == -2 means to run the wizard converter, layer == 0 means 
    // to use the output of Layer00, etc., to the final layer.
    //
    // BUG: For historical reasons, the numbers are messed up.
    // Do Lexer, then Parser, then LayerPre, then Layer00, etc.
    // BUG: Rename so that LayerPre becomes Layer00, and then bump up the
    // other layer numbers. This will require editing comments.
    // Maybe better to name the layers somehow: LayerWiz, LayerUnits, etc.,
    // but then it's not so obvious in what order they are invoked.
  	
    // Look at the top-most tab, but only use it if it's input G-code.
    int curTabIndex = theTabs.getSelectedIndex();
    if (curTabIndex < 0)
        // No tabs exist.
        return;

    Component curTabComponent = theTabs.getComponentAt(curTabIndex);
    TypedDisplayItem curTabTyped = (TypedDisplayItem) curTabComponent;
    if (curTabTyped.type() != TabbedType.G_INPUT)
      // Only makes sense for input G-code.
      return;
    
    GInputTab gCodeTab = (GInputTab) curTabComponent;
    
  	// BUG: Get rid of exception?
  	
  	try {
  	  String fullyDigested = 
  	      Translator.digestAll(gCodeTab.getTextArea().getText(),layer);

      if (gCodeTab.parseOut != null)
        {
          ParserTab parseOutput = gCodeTab.parseOut;
          parseOutput.theText.setText(fullyDigested);
        }
      else
        {
          ParserTab parseOutput = new ParserTab(fullyDigested,gCodeTab);
          theTabs.addTab(theTabs.getTitleAt(curTabIndex) + ": Parser",parseOutput);
          gCodeTab.parseOut = parseOutput;
        }
    
  	} catch (Exception e) {
      JOptionPane.showMessageDialog(this,e.getMessage());
      return;
  	}
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
  
  public void doWorkOffsets() { //throws OutOfMemoryError {
  	
  	// Bring up a table for work offsets. This is a JTable, something like the
    // one used for the tool table, but much simpler.
  	WorkOffsetDialog theDialog = new WorkOffsetDialog(MachineState.workOffsets);
  	theDialog.setVisible(true);
  }
  
  
  public void doSetMaterial() {
  	
  	// Specify the shape and dimensions of the billet along with the location
  	// of the PRZ and the starting position of the tool.
    //
    // Because there are such a variety of physical machines, it is diffiult 
    // to do more than this. The tool table might move in X and Y, or only in 
    // one axis, or not at all. Where is the tool turret, etc.
    //
    // So, there is a relatively limited number of ways this could be done.
    // The billet is a cube, with the axes of the cube aligned with the axes 
    // of the machine.
    // 
    // * By default, the cube is exactly as tall as it needs to be allow
    //   the deepest cut to "go through" and it is as large (xy) as it needs
    //   to be so that any cut uses the full width of the cutter.
    //   
    //   The PRZ is the lower-left corner of this billet, and Z=0 is the
    //   point at which the cutter starts to remove material.
    //   
    //   You can also set the thickness of the material and allow the
    //   material to be taller than Z=0.
    // 
    // * OR you can specify the XYZ dimensions of the billet. Then state the
    //   PRZ relative to the top-lower-left corner of the billet.
    
    // Use a dialog with a few buttons.
  	// I can think of only a few standard shapes: rectangular block, cylinder and hexagon.
  	// Other shapes seem too uncommon to make them a standard choice. I do want the user
  	// to be able to take the output of a previous program and use it as the input
  	// for a new program. The most efficient way to do this (memory-wise) is to allow
  	// the user to load some G-code that's run before anything else. This G-code won't be
  	// visible to the user. 
  	
  	// BUG: For now, just assume a rectangular block.
  	// BUG: I don't think that the initial tool really matters. I can just make sure
  	// that Z is above the PRZ.

    String[] labels = new String[9];
    labels[0] = "Material Length (x)";
    labels[1] = "PRZ (x)";
    labels[2] = "Initial Tool (x)";
    labels[3] = "Material Depth (y)";
    labels[4] = "PRZ (y)";
    labels[5] = "Initial Tool (y)";
    labels[6] = "Material Height (z)";
    labels[7] = "PRZ (z)";
    labels[8] = "Initial Tool (z)";
    
    String[] initial = new String[9];
//    initial[0] = Double.toString(materialX);
//    initial[1] = Double.toString(przX);
//    initial[2] = Double.toString(X0);
//    initial[3] = Double.toString(materialY);
//    initial[4] = Double.toString(przY);
//    initial[5] = Double.toString(Y0);
//    initial[6] = Double.toString(materialZ);
//    initial[7] = Double.toString(przZ);
//    initial[8] = Double.toString(Z0); 
   
    TextInputDialog inputDialog = new TextInputDialog("Set Starting Material",
    		labels,initial,10,3,
    		"\nGive the dimensions of a rectangular block, then the location of the PRZ "+
    		"relative to the top lower left corner of the block,\n"+
    		"followed by the initial position of the tool. The initial tool position should "+
    		"be given relative to the PRZ. All values should be\n"+
    		"given in the units chosen previously under the menu choice Settings->Inch/MM.\n\n"+
    		"You can cancel and not provide any of these numbers and the system will try to "+
    		"chose reasonable values automatically.\n"+
    		"Most of the time, the numbers chosen automatically will be fine.\n");
    String[] inputs = inputDialog.getDoubleInputs();
   
   
    if (inputs == null)
    	// User canceled.
    	return;
    
    // Note what was given.
//    materialX = Double.parseDouble(inputs[0]);
//    materialY = Double.parseDouble(inputs[3]);
//    materialZ = Double.parseDouble(inputs[6]);
//
//    przX = Double.parseDouble(inputs[1]);
//    przY = Double.parseDouble(inputs[4]);
//    przZ = Double.parseDouble(inputs[7]);
//
//    X0 = Double.parseDouble(inputs[2]);
//    Y0 = Double.parseDouble(inputs[5]);
//    Z0 = Double.parseDouble(inputs[8]);
  }
  
  
  public void doInchOrMM() throws OutOfMemoryError {
  	
  	// Whether the rendering process works in inches or millimeters.
  	String[] choiceText = new String[2];
    choiceText[0] = "Use inches internally";
    choiceText[1] = "Use millimeters interally";
    
    int initial = 0;
    if (MachineState.machineInchUnits == true)
    	initial = 0;
    else
    	initial = 1;
    
    ChoiceDialogRadio choiceDialog = new ChoiceDialogRadio("Inches or mm?",choiceText,initial);
    int choice = choiceDialog.getChoice();
    
    if (choice == -1)
    	// User canceled.
    	return;
    
    if (choice == 0)
    	{
    		// Use inches
    	  MachineState.machineInchUnits = true;
//    		this.scale = 1000.0;
    	}
    else
    	{
    		// User chose millimeters.
    	  MachineState.machineInchUnits = false;
//    		this.scale = 40.0;
    	}
  }
  
  /*
  public void doScale() throws OutOfMemoryError {
  	
  	// Scaling factor on the rendering. Effectively, this is the resolution of
  	// the table. All three axes have the same resolution.
    String[] labels = new String[1];
    labels[0] = "Scale";
    
    String[] initial = new String[1];
    initial[0] = Double.toString(this.scale); 
   
    TextInputDialog inputDialog = new TextInputDialog("Input Machine Resolution",
    		labels,initial,10,
    		"\nGive the number of machine steps per unit length. 1,000 is\n"+
    		"a common value when using inches; 50 is common when\n"+
    		"using millimeters. Numbers much larger than these require\n"+
    		"more memory, and the program will be faster if smaller\n" +
    		"values are used, though the accuracy will be reduced.\n");
    String[] inputs = inputDialog.getDoubleInputs();
   
   
    if (inputs == null)
    	// User canceled.
    	return;
    
    // Note what was given.
    this.scale = Double.parseDouble(inputs[0]);
  }
  */
  
  public void doToggleLineNumbers() {
  
    // Provided that the from tab of the window has G-code (the TabbedType
    // is G_INPUT), toggle display of the line numbers.

    int curTabIndex = theTabs.getSelectedIndex();
    if (curTabIndex < 0)
        // No tabs exist.
        return;
    
    Component curTabComponent = theTabs.getComponentAt(curTabIndex);
    
    TypedDisplayItem curTabTyped = (TypedDisplayItem) curTabComponent;
    if (curTabTyped.type() != TabbedType.G_INPUT)
      // Only makes sense for input G-code.
      return;

    GInputTab gCodeTab = (GInputTab) curTabComponent;
    gCodeTab.toggleLineNumbers();
  }
  
  public MainWindow doNewWindow() {
    
    // Create a new window, like this one, with no tabs.
    
    // BUG: Who owns this thing?
    MainWindow mw = new MainWindow();
    return mw;
  }
  
  private void doTabToWindow() {
    
    // Move the current tabbed item to its own window. The contents of this
    // window isn't dynamic. Whatever is in this window is fixed for all time.
    int curTab = theTabs.getSelectedIndex();
    Component c = theTabs.getComponentAt(curTab);
    String title = theTabs.getTitleAt(curTab);
    
    System.out.println("type: " +c.getClass().getName());
    
    if (c instanceof GInputTab)
      {
        GInputTab git = (GInputTab)  c;
        JTextArea ta = git.duplicate();
        JScrollPane sp = new JScrollPane();
        
        ta.setEditable(false);

        Font theFont = new Font("Monospaced",Font.PLAIN,12);
        ta.setFont(theFont);
        
        sp.setViewportView(ta);
        
        StaticWindow w = new StaticWindow(title,sp);
      }
    else
      System.out.println("Unknown type in doTabToWindow()!");
  }
  
  private void doGetVersion() throws OutOfMemoryError {

    JOptionPane.showMessageDialog(this,
        "This is version 0.03 of November 20, 2022.");
		
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
	  	else if (e.getActionCommand().equals("Test Lexer"))
	      doTestLexer();
      else if (e.getActionCommand().equals("Test Parser"))
        doSimplify(-1);
      else if (e.getActionCommand().equals("Test Wizards"))
        doSimplify(-2);
	  	else if (e.getActionCommand().equals("Simplify 00"))
	      doSimplify(0);
	  	else if (e.getActionCommand().equals("Simplify 01"))
	      doSimplify(1);
	  	else if (e.getActionCommand().equals("Simplify 02"))
	      doSimplify(2);
	  	else if (e.getActionCommand().equals("Simplify 03"))
	      doSimplify(3);
	  	else if (e.getActionCommand().equals("Simplify 04"))
	      doSimplify(4);
	  	else if (e.getActionCommand().equals("Simplify 05"))
	      doSimplify(5);
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
//	  	else if (e.getActionCommand().equals("Tool Table"))
//	  		doToolTable();
	  	else if (e.getActionCommand().equals("Work Offsets"))
	  		doWorkOffsets();
	  	else if (e.getActionCommand().equals("Inch/MM"))
	  		doInchOrMM();
//	  	else if (e.getActionCommand().equals("Scale"))
//	  		doScale();
	  	else if (e.getActionCommand().equals("Uncut Shape"))
	  		doSetMaterial();
	  	else if (e.getActionCommand().equals("Toggle Line Numbers"))
        doToggleLineNumbers();
	  	else if (e.getActionCommand().equals("New Window"))
          doNewWindow();
      else if (e.getActionCommand().equals("Close Window"))
        this.windowClosing(null);
	  	else if (e.getActionCommand().equals("Tab to Window"))
	  	  doTabToWindow();
	  	else if (e.getActionCommand().equals("Get Version"))
	  		doGetVersion();
	  		
  		
  	} catch (OutOfMemoryError err) {
  		JOptionPane.showMessageDialog(this,"Out of memory.\n"
  				+"Close some windows and try again.\n"
  				+"If that doesn't work, then restart the program.");
  	}
  }
  
  private void initMenus() {
    
    // BUG: This uses old style AWT menus. They work, but maybe Swing menus
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
    
    theMenu = new Menu("Test",false);
    theMenu.add(new MenuItem("Test Lexer",new MenuShortcut(KeyEvent.VK_L,false)));
    theMenu.add(new MenuItem("Test Parser",new MenuShortcut(KeyEvent.VK_P,false)));
    theMenu.add(new MenuItem("Test Wizards",new MenuShortcut(KeyEvent.VK_W,false)));
    theMenu.add(new MenuItem("Simplify 00"));
    theMenu.add(new MenuItem("Simplify 01"));
    theMenu.add(new MenuItem("Simplify 02"));
    theMenu.add(new MenuItem("Simplify 03"));
    theMenu.add(new MenuItem("Simplify 04"));
    theMenu.add(new MenuItem("Simplify 05"));
//    theMenu.add(new MenuItem("Test Pulses"));
//    theMenu.add(new MenuItem("Test Voxel Frame"));
    theMenu.addActionListener(this);
    theMenuBar.add(theMenu);
    
    theMenu = new Menu("Render",false);
//    theMenu.add(new MenuItem("2D Display"));
//    theMenu.add(new MenuItem("3D Display"));
    theMenu.add(new MenuItem("New Display"));
    theMenu.addActionListener(this);
    theMenuBar.add(theMenu);
    
    theMenu = new Menu("Settings",false);
//    theMenu.add(new MenuItem("Uncut Shape"));
//    theMenu.add(new MenuItem("Tool Table"));
    theMenu.add(new MenuItem("Work Offsets"));
    theMenu.add(new MenuItem("Inch/MM"));
//    theMenu.add(new MenuItem("Scale"));
    theMenu.addActionListener(this);
    theMenuBar.add(theMenu);

    
    theMenu = new Menu("Window",false);
    theMenu.add(new MenuItem("Toggle Line Numbers"));
    theMenu.add(new MenuItem("New Window"));
    theMenu.add(new MenuItem("Close Window"));
    theMenu.add(new MenuItem("Tab to Window"));
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


