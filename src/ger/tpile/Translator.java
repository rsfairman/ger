package ger.tpile;

import static ger.tpile.SType.*;

/*

The main workhorse. It allows one to transpile to varying extents. 

*/

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Hashtable;

import ger.WizCompile;
import ger.persist.Persist;
import ger.tpile.Statement;
import ger.tpile.lex.Lexer;
import ger.tpile.parse.DataCircular;
import ger.tpile.parse.DataMove;
import ger.tpile.parse.DataSubProg;
import ger.tpile.parse.DataSubroutineCall;
import ger.tpile.parse.DataTLO;
import ger.tpile.parse.DataWizard;
import ger.tpile.parse.Parser;
import ger.util.LLNode;
import ger.util.LList;
import ger.wizard.WizardBase;


public class Translator {
	
  // Each of these corresponds to a particular layer of the translator. 
  // It's tempting to make these an enum type, but the fact that they're
  // *ordered* allows the code to be briefer since it allows cases to fall
  // through more easily.
  // 
  // The way these are ordered is a little artificial. Certain tasks must 
  // occur after other tasks, but some minor reordering would be OK.
  // One factor is what helps the user to debug. For example, wizards are done 
  // early since it seems most likely for there to be a bug (in the user's 
  // code) related to how wizard output is affected by other things.
  
  public static final int ThruLexer = -1;
  public static final int ThruParser = 0;
  public static final int ThruDirectives = 1;
  public static final int ThruSubProgs = 2;
  public static final int ThruWizards = 3;
  public static final int ThruUnits = 4;
  public static final int ThruWorkOffsets = 5;
  public static final int ThruPolar = 6;
  public static final int ThruIncremental = 7;
  public static final int ThruCutterComp = 8;
  
  // This could be set to ThruCutterComp or gotten rid of entirely, but
  // it's good for clarity of intent.
  // BUG: Maybe get rid of this? "Everything" doesn't include Prerender.
  // Maybe rename.
  public static final int ThruEverything = 1000;
  
  // Mostly for testing. The user won't want to see the code that goes
  // to the renderr.
  // BUG: Or will he? It might be useful to have code that consists of
  // nothing but linear moves.
  public static final int ThruPrerender = 2000;
  
  
	// The wip (Work In Progress) represents the series of statements,
	// as digested so far. Sometimes I call this the AST (Abstract Syntax Tree),
  // although that's a grandiose label for something this trivial.
	private static LList<Statement> wip = null;
	
	
	// State of the machine....
	// These are public so that they are visible to wizards.
	
	// Whether in G00 (rapid travel) or G01 (normal travel) mode.
	// Really, this doesn't matter for translation, but it doesn't hurt
	// to track it and it helps as a reminder of what's really going on.
	public static boolean rapidTravel = false;
	
	// When in normal (not rapid) travel mode, the feed rate persists.
	public static double curFeedRate = -1.0;
	
  // Whether the machine is *currently* working in inches (true) or mm (false).
  public static boolean curInch = true;

  // Which plane is current with G17/18/19.
  public static AxisChoice curAxis = AxisChoice.XY;

  // Whether currently in polar coordinates mode (G15/16).
  public static boolean usingPolar = false;

  // Whether in incremental (or absolute) mode.
  public static boolean usingIncremental = false;
  
  // Whether currently using cutter comp.
  public static boolean usingCutterComp = false;
  
  // If cutter comp is in force, whether left or right.
  public static boolean leftCutterComp = false;
  
  // The location of the PRZ, relative to machine zero, is given by these 
  // coordinates. Remember that machine zero is defined to be the location of 
  // the tool immediately before the first line of code executes. So, the
  // command 
  // G00 X0 Y0 Z0
  // will move the tool to (xPRZ,yPRZ,zPRZ) in terms of the machine coordinates
  // and those are the coordinates that should appear in the translated output
  // for the move.
  public static double xPRZ = 0.0;
  public static double yPRZ = 0.0;
  public static double zPRZ = 0.0;
	
  // The current position of the cutter, relative to machine zero. Effectively,
  // this is relative to the location of the tool immediately before the first
  // line of the program.
  public static double xCur = 0.0;
  public static double yCur = 0.0;
  public static double zCur = 0.0;
  
  // This also notes the maximum extent of travel in each axis. These values
  // are for the center of the cutter, not considering the outer edges of
  // the cutter.
  // This will be useful when rendering. This starts off with all values set
  // to 0.0 since that's where every program starts: machine zero.
  public static double xMin = 0.0;
  public static double xMax = 0.0;
  public static double yMin = 0.0;
  public static double yMax = 0.0;
  public static double zMin = 0.0;
  public static double zMax = 0.0;
  
	
	
  ///////////////////////////////////////////////////////////////////
  //
  // Code for various initial checks that need to be noted or skipped
	// in a fairly dumb way.
  //
  ///////////////////////////////////////////////////////////////////
  

  private static boolean handleSkippable(LLNode<Statement> curNode) {
  
    // Return true to skip over any single statement (without removing it from
    // the wip) that doesn't affect the translation but that could affect a
    // simulation, or that might be important to a physical machine, like M07 
    // for 'coolant on', etc. Note that this does skip M06 (tool change). It
    // *will* affect translation, but not until we reach the cutter comp stage.

    if (curNode == null) return false;
    
    Statement cur = curNode.data;
    
    // Skip over any errors that were inserted at some earlier stage.
    if (cur.type == ERROR) return true;
    
    if (cur.type == M00) return true;
    if (cur.type == M01) return true;
    if (cur.type == M03) return true;
    if (cur.type == M04) return true;
    if (cur.type == M05) return true;
    if (cur.type == M06) return true;
    if (cur.type == M07) return true;
    if (cur.type == M08) return true;
    if (cur.type == M09) return true;
    if (cur.type == M40) return true;
    if (cur.type == M41) return true;
    if (cur.type == M48) return true;
    if (cur.type == M49) return true;

    // Note that TLO commands G43 and G44 are not skipped. It may be 
    // necessary to convert any units given as arguments to inch/mm.
    if (cur.type == G49) return true;
    
    return false;
  }

  private static boolean noteCutterComp(Statement cmd) {
    
    if (cmd.type == G41)
      {
        usingCutterComp = true;
        leftCutterComp = true;
        
        // To tell the caller to skip this statement (this method handled it).
        return true;
      }
    if (cmd.type == G42)
      {
        usingCutterComp = true;
        leftCutterComp = false;
        return true;
      }
    if (cmd.type == G40)
      {
        usingCutterComp = false;
        return true;
      }
    
    // Not this method's problem. Someone else handles it.
    return false;
  }

  private static boolean noteAxis(Statement cmd) {
  
    // Check for G17/18/19, for setting the plane and change the machine state.
    // Return true if one of these was present.
    if (cmd.type == G17)
      {
        curAxis = AxisChoice.XY;
        return true;
      }
    if (cmd.type == G18)
      {
        curAxis = AxisChoice.ZX;
        return true;
      }
    if (cmd.type == G19)
      {
        curAxis = AxisChoice.YZ;
        return true;
      }
    
    return false;
  }
  
  private static boolean noteTravelMode(Statement cmd) {
    
    if (cmd.type == G00)
      {
        rapidTravel = true;
        return true;
      }
    if (cmd.type == G01)
      {
        rapidTravel = false;
        return true;
      }
    
    return false;
  }
  
  private static boolean noteUnits(Statement cmd) {
    
    // If this is a G20/21, note it in the state and return true.
    if (cmd.type == G20)
      {
        curInch = true;
        return true;
      }
    if (cmd.type == G21)
      {
        curInch = false;
        return true;
      }
    
    return false;
  }
  
  private static boolean notePolar(Statement cmd) {
    
    // If this is a G15/16, note it in the state, and return true.
    if (cmd.type == G15)
      {
        usingPolar = false;
        return true;
      }
    if (cmd.type == G16)
      {
        usingPolar = true;
        return true;
      }
    
    return false;
  }
  
  private static boolean noteIncremental(Statement cmd) {
    
    // If this is a G90/91, note it in the state, and return true.
    if (cmd.type == G90)
      {
        usingIncremental = false;
        return true;
      }
    if (cmd.type == G91)
      {
        usingIncremental = true;
        return true;
      }
    
    return false;
  }
  
  private static boolean initialChecks(LLNode<Statement> curNode) {
    
    // Deal with a bunch of similar cases that must be handled each time
    // through doLayers(). None of these things are to be removed from the
    // wip; they are simply skipped over and left in place.
    
    // Commands that have no effect, but need to remain.
    if (handleSkippable(curNode) == true) return true;
    
    // Travel mode (G00 or G01).
    if (noteTravelMode(curNode.data) == true) return true;

    // Not something handled here.
    return false;
  }
  

  private static boolean removedChecks(LLNode<Statement> curNode) {
    
    // As above, but for things that *are* removed from the wip.
    if (curNode == null) return false;
    if (curNode.data == null) return false;
    
    // G20/21 for inch/mm.
    if (noteUnits(curNode.data) == true) return true;
    
    // Polar and absolute modes are mutually exclusive, and that messes
    // things up a little. If the user attempts to go into polar+absolute
    // mode, then the statement is converted to an ERROR and it is *not*
    // to be removed from the wip. However, the mode change *is* noted, 
    // and the moves that eventually result are likely to be gibberish.
    boolean polarChange = notePolar(curNode.data);     // G15/16
    boolean incChange = noteIncremental(curNode.data); // G90/91
    
    if ((polarChange == true) || (incChange == true))
      {
        // One of the modes changes. Was it in error?
        if ((usingIncremental == false) && (usingPolar == true))
          {
            // No can do. Change to an ERROR and do not remove from the wip.
            curNode.data.makeError(
                "may not be in polar and absolute mode at the same time");
            return false;
          }
        
        // Did change, and was permissible. So remove the statement.
        return true;
      }
    
    // The choice of axis (G17/8/19) and cutter comp are messy for similar
    // reasons. Cutter comp is only allowed in the XY-plane (G17).
    // Note that this is one reason cutter comp is tracked at an early stage
    // (before ThruCutterComp); another reason is that you can't change the
    // PRZ while using cutter comp.
    boolean compChange = noteCutterComp(curNode.data);
    boolean axisChange = noteAxis(curNode.data);
    
    if ((compChange == true) || (axisChange == true))
      {
        // One of them changed. In a permissible way?
        if ((usingCutterComp == true) && (curAxis != AxisChoice.XY))
          {
            curNode.data.makeError("cutter comp requires the XY-plane");
            return false;
          }
        
        return true;
      }
    
    // Whatever the statement is, leave it there.
    return false;
  }
	
  
  ///////////////////////////////////////////////////////////////////
  //
  // Code to check for various errors in the code.
  // These tend to be more sophisticated problems than (say) syntax.
  //
  ///////////////////////////////////////////////////////////////////
  

  private static boolean checkArcError(Statement cmd) {
    
    // Check whether there are any inconsistencies in the given Statement,
    // assuming it is a G02/03 for an arc move.
    // 
    // Return true if there is an error; false otherwise. If there was
    // an error, then the statement is changed to the corresponding error.
    if ((cmd.type != G02) && (cmd.type != G03))
      return false;
    
    // Not allowed to do G02/03 in polar mode.
    if (usingPolar == true)
      {
        cmd.makeError(
            "circular interpolation not allowed with polar coordinates");
        return true;
      }
    
    // Make sure that the arc specification is consistent with the choice
    // of plane. For instance, you can't use a K-value with G17.
    DataCircular theMove = (DataCircular) cmd.data;
    if (curAxis == AxisChoice.XY)
      {
        if (theMove.kDefined == true)
          {
            cmd.makeError("may not use K in the XY-plane (G17)");
            return true;
          }
      }
    else if (curAxis == AxisChoice.ZX)
      {
        if (theMove.jDefined == true)
          {
            cmd.makeError("may not use J in the ZX-plane (G18)");
            return true;
          }
      }
    else
      {
        // Must be the YZ-plane.
        if (theMove.iDefined == true)
          {
            cmd.makeError("may not use I in the YZ-plane (G19)");
            return true;
          }
      }
    
    // No errors.
    return false;
  }
  
  private static boolean checkForFeedRate(Statement cmd) {
    
    // A move in normal travel (not rapid) requires a positive feed rate.
    // Return true if there is an error, after changing cmd to an ERROR.
    if (cmd.type != MOVE)
      return false;
    
    DataMove m = (DataMove) cmd.data;
    
    if (rapidTravel == true)
      {
        // Feed rate is known: at "rapid" speed. If the user *did* give
        // a feed rate, then consider it an error.
        if (m.fDefined == true)
          {
            cmd.makeError("feed rate given in rapid travel mode");
            return true;
          }
        
        return false;
      }
    
    // Got here, so a normal (not rapid) move. There must be a feed rate.
    if ((m.fDefined == true) && (m.fValue > 0.0))
      // The move specifies the feed rate explicitly.
      // It's tempting to update storedFeedRate here, but the units may change.
      return false;
    
    if (curFeedRate <= 0.0)
      {
        cmd.makeError("move requires a feed rate");
        return true;
      }
    
    return false;
  }
  
  private static boolean checkPRZChange(Statement cmd) {
    
    // If the cmd is one of the commands that changes the PRZ (G52, etc.),
    // and we are in certain modes where that's not allowed, then convert
    // cmd to an error statement.
    if ((cmd.type != G52) && (cmd.type != G54) &&
        (cmd.type != G55) && (cmd.type != G56) &&
        (cmd.type != G57) && (cmd.type != G58) &&
        (cmd.type != G59) && (cmd.type != G92))
      return false;
    
    // Got here, so check.
    if (usingPolar == true)
      {
        cmd.makeError("changing the PRZ is not allowed while in polar mode");
        return true;
      }
    if (usingIncremental == true)
      {
        cmd.makeError(
            "changing the PRZ is not allowed while in incremental mode");
        return true;
      }
    if (usingCutterComp == true)
      {
        cmd.makeError(
            "changing the PRZ is not allowed while using cutter comp");
        return true;
      }
    
    return false;
  }
  
  private static boolean errorChecks(LLNode<Statement> curNode) {
    
    // Look for various problems; if one is found, then the corresponding
    // statement is changed to ERROR type (with a message) and return true.
    if (curNode == null) return false;
    if (curNode.data == null) return false;
    Statement cmd = curNode.data;
    
    // Certain syntax/geometry errors.
    if (checkArcError(cmd) == true) return true;
    
    // You can't move if there's no feed rate.
    if (checkForFeedRate(cmd) == true) return true;
    
    // Can't change the PRZ while in certain modes.
    if (checkPRZChange(cmd) == true) return true;
    
    return false;
  }
  
  
  ///////////////////////////////////////////////////////////////////
  //
  // Handle change in units, inch vs. mm.
  //
  ///////////////////////////////////////////////////////////////////

  
  private static double toNatural(double u) {
    
    // Convert the given coordinate value to the coordinate that's natural to 
    // the machine. E.g., if this is a metric machine, but we are currently in
    // inch mode, then inches must be converted to millimeters.
    
    if (DefaultMachine.inchUnits == curInch)
      // Nothing to do. 
      return u;
    
    if (DefaultMachine.inchUnits == true)
      // This is an inch machine, but u is in millimeters.
      return u / 25.40;
    
    // Else, this is a metric machine, but u is in inches.
    return u * 25.40;
  }
  
  private static void convertLineUnits(Statement cmd) {
    
    // Convert units for linear moves to the machine standard.
    // Polar coordinate mode can make this tricky.
//    
//    System.out.println("machine inch is " +MachineState.machineInchUnits);
//    System.out.println("current inch is " +curInch);
    
    DataMove theMove = (DataMove) cmd.data;
    
    if (theMove.fDefined == true)
      theMove.fValue = toNatural(theMove.fValue);
    
    if (usingPolar == false)
      {
        if (theMove.xDefined == true)
          theMove.xValue = toNatural(theMove.xValue);
        if (theMove.yDefined == true)
          theMove.yValue = toNatural(theMove.yValue);
        if (theMove.zDefined == true)
          theMove.zValue = toNatural(theMove.zValue);
      }
    else
      {
        // In polar coordinates. The plane chosen (G17/18/19) determines which
        // of the variables need to be changed. Don't change the angle, 
        // which is in degrees.
        switch (curAxis)
          {
            case XY : // X = radius, Y = angle.
                      if (theMove.xDefined == true)
                        theMove.xValue = toNatural(theMove.xValue);
                      if (theMove.zDefined == true)
                        theMove.zValue = toNatural(theMove.zValue);
                      break;
            case ZX : // Z = radius, X = angle.
                      if (theMove.yDefined == true)
                        theMove.yValue = toNatural(theMove.yValue);
                      if (theMove.zDefined == true)
                        theMove.zValue = toNatural(theMove.zValue);
                      break;
            case YZ : // Y = radius, Z = angle.
                      if (theMove.xDefined == true)
                        theMove.xValue = toNatural(theMove.xValue);
                      if (theMove.yDefined == true)
                        theMove.yValue = toNatural(theMove.yValue);
                      break;
          }
      }
  }
  
  private static void convertArcUnits(Statement cmd) {
    
    // Adjust arcs to be in the standard units: inches or mm.
    DataCircular theMove = (DataCircular) cmd.data;
    if (theMove.xDefined == true)
      theMove.X = toNatural(theMove.X);
    if (theMove.yDefined == true)
      theMove.Y = toNatural(theMove.Y);
    if (theMove.zDefined == true)
      theMove.Z = toNatural(theMove.Z);
  
    if (theMove.rDefined == true)
      theMove.R = toNatural(theMove.R);
    
    if (theMove.iDefined == true)
      theMove.I = toNatural(theMove.I);
    if (theMove.jDefined == true)
      theMove.J = toNatural(theMove.J);
    if (theMove.kDefined == true)
      theMove.K = toNatural(theMove.K);
    
    if (theMove.F > 0.0)
      theMove.F = toNatural(theMove.F);
  }
  
  private static void convertTLOUnits(Statement cmd) {
    
    DataTLO tlo = (DataTLO) cmd.data;
    if (tlo.hasZ == true)
      tlo.zValue = toNatural(tlo.zValue);
  }
  
  private static void convertUnits(Statement cmd) {
    
    // Convert cmd to machine units (inch/mm). Obviously, linear and arc moves
    // must be converted, but other things need conversion too.
    if (DefaultMachine.inchUnits == curInch)
      // Nothing to do.
      return;
    
    if (cmd.type == MOVE)
      convertLineUnits(cmd);
    
    if ((cmd.type == G02) || (cmd.type == G03))
      convertArcUnits(cmd);
    
    if ((cmd.type == G43)|| (cmd.type == G44))
      convertTLOUnits(cmd);
      
    // This reuses the code for linear moves, and that's fine because
    // G52 and G92 may only appear outside of polar (and incremental) mode.
    if ((cmd.type == G52) || (cmd.type == G92))
      convertLineUnits(cmd);
  }
  
  
  ///////////////////////////////////////////////////////////////////
  //
  // Cutter comp stuff
  //
  ///////////////////////////////////////////////////////////////////
  
  
  private static void updateMaxMin(Statement cmd) {
    
    // Update the maximum extent of tool travel.
    
    // BUG: This considers only the center of the cutter, not the edges.
    // I do want to track this alone, but will also want to track with
    // cutter edges considered after cutter comp is done.
    
    // BUG: As things stand, this is called by handleCutterComp().
    // Really, this isn't needed unless you are rendering, so maybe it should
    // be done there. OTOH, I suppose that the user might want this just for 
    // information. Maybe these values should be added as a comment to the 
    // output text?
    
    if (cmd.type == MOVE)
      {
        // For linear moves, the initial position doesn't need to be
        // considered, only where the tool ends up.
        DataMove m = (DataMove) cmd.data;
        if (m.xDefined == true)
          {
            if (m.xValue > xMax) xMax = m.xValue;
            if (m.xValue < xMin) xMin = m.xValue;
          }
        if (m.yDefined == true)
          {
            if (m.yValue > yMax) yMax = m.yValue;
            if (m.yValue < yMin) yMin = m.yValue;
          }
        if (m.zDefined == true)
          {
            if (m.zValue > zMax) zMax = m.zValue;
            if (m.zValue < zMin) zMin = m.zValue;
          }
      }
    else if ((cmd.type == G02) || (cmd.type == G03))
      {
        // Arc moves are messier.
        ArcCurve c = null;
        try {
          c = ArcCurve.factory(xCur,yCur,zCur,curAxis,cmd);
        } catch (Exception e) {
          // This should be impossible. Problematic stuff was filtered
          // out earlier (converted to an ERROR statement).
          System.err.println("Unexpected arc error.");
          e.printStackTrace();
          return;
        }
        
        c.noteMaxMin();
        
        DataCircular dc = (DataCircular) cmd.data;
        if (dc.xDefined == true)
          {
            if (c.xMax > xMax) xMax = c.xMax;
            if (c.xMin < xMin) xMin = c.xMin;
          }
        if (dc.yDefined == true)
          {
            if (c.yMax > yMax) yMax = c.yMax;
            if (c.yMin < yMin) yMin = c.yMin;
          }
        if (dc.zDefined == true)
          {
            if (c.zMax > zMax) zMax = c.zMax;
            if (c.zMin < zMin) zMin = c.zMin;
          }
      }
    
  }
  
  private static void updatePos(Statement cmd) {
    
    // Update xCur, yCur, zCur to the end-point of any tool move.
    // Similar to finalStep().
    
    // BUG: feed rate is irrelevant at this stage?
    if (cmd.type == MOVE)
      {
        DataMove m = (DataMove) cmd.data;
        if (m.fDefined == true) curFeedRate = m.fValue;
        if (m.xDefined == true) xCur = m.xValue;
        if (m.yDefined == true) yCur = m.yValue;
        if (m.zDefined == true) zCur = m.zValue;
      }
    else if ((cmd.type == G02) || (cmd.type == G03))
      {
        DataCircular a = (DataCircular) cmd.data;
        if (a.F > 0.0) curFeedRate = a.F;
        if (a.xDefined == true) xCur = a.X;
        if (a.yDefined == true) yCur = a.Y;
        if (a.zDefined == true) zCur = a.Z;
      }
  }
  
  private static void handleCutterComp() {
    
    // Step through the wip, and modify for cutter comp. It is assumed
    // that all coordinates are given in absolute cartesian terms, relative 
    // to machine zero in machine units.
    // This also notes the max/min moves for the cutter in each axis.
    
    // BUG: Not done.
    
    // BUG: Currently tracking max/min location of the cutter's center.
    // I should also (not instead of) track the location of the edge of the
    // cutter. But I can't do that till I handle cutter comp.
    

    LLNode<Statement> curNode = wip.head;
    while (curNode != null)
      {
        Statement cmd = curNode.data;
        
        // Do this again (or something like it) *after* adjusting for
        // cutter comp.
        // BUG: No, it doesn't need to be done twice, and I don't see
        // any reason (?) to track this without considering the tool
        // radius. Then again, it may matter for some kind of optimization
        // at the rendering step. Not sure. 
        updateMaxMin(cmd);
        
        
        
        updatePos(cmd);
        
        curNode = curNode.next;
      }
    
  }
  
  
  ///////////////////////////////////////////////////////////////////
  //
  // Stuff to change PRZ: G52, G54-59 and G92.
  //
  ///////////////////////////////////////////////////////////////////
  
  
  private static void useWorkOffset(int i) {
    xPRZ = DefaultMachine.workOffsets.offset[i][0];
    yPRZ = DefaultMachine.workOffsets.offset[i][1];
    zPRZ = DefaultMachine.workOffsets.offset[i][2];
  }
  
  private static boolean changePRZ(Statement cmd) {
    
    // Return true iff the cmd does adjust the PRZ and should be removed
    // from the wip.
    if ((cmd.type != G52) && (cmd.type != G54) &&
        (cmd.type != G55) && (cmd.type != G56) &&
        (cmd.type != G57) && (cmd.type != G58) &&
        (cmd.type != G59) && (cmd.type != G92))
      return false;
    
    if (cmd.type == G52)
      {
        // The (x,y,z) value given with the command, expressed relative to
        // machine zero, becomes the new PRZ.
        DataMove move = (DataMove) cmd.data;
        xPRZ = move.xValue;
        yPRZ = move.yValue;
        zPRZ = move.zValue;
        return true;
      }

    if (cmd.type == G92)
      {
        // Set a new PRZ so that the (x,y,z) value given with the command
        // becomes the current location. Put another way: imagine a move
        // to the given (x,y,z) under the current PRZ, then do a bare G92
        // so that the location we just moved to becomes (0,0,0).
        
        // The value given is relative to the current tool position. Express
        // that position relative to machine zero.
        DataMove move = (DataMove) cmd.data;
        double x = move.xValue + xCur;
        double y = move.yValue + yCur;
        double z = move.zValue + zCur;
        
        // This (x,y,z) should be (0,0,0) under the new PRZ.
        xPRZ = x;
        yPRZ = y;
        zPRZ = z;
        
        return true;
      }

    if (cmd.type == G54)
      {
        useWorkOffset(0);
        return true;
      }
    if (cmd.type == G55)
      {
        useWorkOffset(1);
        return true;
      }
    if (cmd.type == G56)
      {
        useWorkOffset(2);
        return true;
      }
    if (cmd.type == G57)
      {
        useWorkOffset(3);
        return true;
      }
    if (cmd.type == G58)
      {
        useWorkOffset(4);
        return true;
      }
    if (cmd.type == G59)
      {
        useWorkOffset(5);
        return true;
      }
    
    // Somehow (?) fell through.
    return false;
  }

  private static boolean handlePRZ(Statement cmd) {
    
    // First, the PRZ might be changing due to G52, etc.
    if (changePRZ(cmd) == true) return true;
    
    // Adjust any motion commands to be given relative to machine zero,
    // whatever the PRZ may be. This is different than the distinction
    // between absolute and incremental coordinates. So, this may change
    // MOVE, G02 and G03.
    // But this is done only while in absolute mode.
    if (usingIncremental == true) return false;
    
    if (cmd.type == MOVE)
      {
        DataMove m = (DataMove) cmd.data;
        if (m.xDefined == true) m.xValue += xPRZ;
        if (m.yDefined == true) m.yValue += yPRZ;
        if (m.zDefined == true) m.zValue += zPRZ;
      }
    else if ((cmd.type == G02) || (cmd.type == G03))
      {
        // G02 and G03 take the form
        // G02 X## Y## Z## I## J## K## R## F##
        // and may use I/J/K or R to specify the radius, but not both.
        // The tool moves from its current location to the given (X,Y,Z)
        // and I/J/K (or R) is used to determine the radius. Recall that
        // the IJK values are inherently relative (or incremental). They
        // indicate the position of the center of the arc, measured from
        // the cutter's starting point.
        DataCircular c = (DataCircular) cmd.data;
        if (c.xDefined == true) c.X += xPRZ;
        if (c.yDefined == true) c.Y += yPRZ;
        if (c.zDefined == true) c.Z += zPRZ;
      }
    
    return false;
  }
  
  
  ///////////////////////////////////////////////////////////////////
  //
  // Code for conversion from polar coordinates.
  //
  ///////////////////////////////////////////////////////////////////
  
  
  private static void convertPolar(LLNode<Statement> curNode) {
    
    // If in polar mode, convert the given command to cartesian coordinates.
    // Note that errorChecks() should have caught use of things like G02/03
    // while in polar mode. What appears here should be either a linear
    // move or something that can be ignored.
    if (usingPolar == false) return;
    
    Statement cmd = curNode.data;
    if (cmd.type != MOVE) return;
    
    DataMove m = (DataMove) cmd.data;
    
    // Polar coordinate are only allowed while in incremental mode, and
    // that requirement was checked earlier. Requiring incremental mode
    // simplifies things a lot, and it seems more natural for the user.
    // In (much) earlier versions of the program, I allowed polar and
    // absolute modes to mix. It could be done, but it's messy and it's
    // hard to imagine a legitimate use for it. The big issue is that
    // there is no sensible machine zero to use as the origin of the 
    // polar coordinate system.
    //
    // Another attitude would be to say that going into polar coordinate mode
    // implicitly puts you in incremental mode, and you don't need to put a 
    // G91 into the code. That would work too, but it does seem worth requiring
    // the user to put in a G90 as a reminder that things are different.
    
    // These are just different enough that being DRY isn't worth it.
    if (curAxis == AxisChoice.XY)
      {
        // X = radius, Y = angle. 
        if (m.xDefined == false)
          {
            curNode.data.makeError("no radius (X) given");
            return;
          }
        
        double radius = m.xValue;
        
        double angle = 0.0;
        if (m.yDefined == true)
          angle = m.yValue;
        
        angle = angle * Math.PI / 180.0;
        
        double x = radius * Math.cos(angle);
        double y = radius * Math.sin(angle);
        
        // Change the underling DataMove to reflect this.
        m.xDefined = true;
        m.xValue = x;
        m.yDefined = true;
        m.yValue = y;
      }
    else if (curAxis == AxisChoice.ZX)
      {
        // Z = radius, X = angle. 
        if (m.zDefined == false)
          {
            curNode.data.makeError(
                "no radius (Z) given -- in G18 mode (ZX-plane)");
            return;
          }
        
        double radius = m.zValue;
        
        double angle = 0.0;
        if (m.xDefined == true)
          angle = m.xValue;
        
        angle = angle * Math.PI / 180.0;
        
        double z = radius * Math.cos(angle);
        double x = radius * Math.sin(angle);
        
        m.zDefined = true;
        m.zValue = z;
        m.xDefined = true;
        m.xValue = x;
      }
    else
      {
        // Must be the AxisChoice.YZ case. Y = radius, Z = angle.
        if (m.yDefined == false)
          {
            curNode.data.makeError(
                "no radius (Y) given -- in G19 mode (YZ-plane)");
            return;
          }
        
        double radius = m.yValue;
        
        double angle = 0.0;
        if (m.zDefined == true)
          angle = m.zValue;
        
        angle = angle * Math.PI / 180.0;
        
        double y = radius * Math.cos(angle);
        double z = radius * Math.sin(angle);
        
        m.yDefined = true;
        m.yValue = y;
        m.zDefined = true;
        m.zValue = z;
      }
  }
  
  
  ///////////////////////////////////////////////////////////////////
  //
  // Code for conversion from incremental to absolute coordinates.
  //
  ///////////////////////////////////////////////////////////////////
  
  
  private static void convertIncremental(LLNode<Statement> curNode) {
  
    // Convert all statements to absolute coordinates.
    // The things that may need to be translated are: MOVE, G02 and G03. 
    // Note that G52 and G54-59 are *not* translated. G92 isn't either since
    // it is inherently an incremental concept.
    
    // Obviously, we only convert from incremental coordinates if we are
    // in incremental mode.
    if (usingIncremental == false) return;
    
    if (curNode == null) return;
    if (curNode.data == null) return;
    
    Statement cmd = curNode.data;
    
    if (cmd.type == MOVE)
      {
        DataMove m = (DataMove) cmd.data;
        if (m.xDefined == true)  m.xValue += xCur;
        if (m.yDefined == true)  m.yValue += yCur;
        if (m.zDefined == true)  m.zValue += zCur;
      }
    else if ((cmd.type == G02) || (cmd.type == G03))
      {
        // G02 and G03 take the form
        // G02 X## Y## Z## I## J## K## R## F##
        // and may use I/J/K or R to specify the radius, but not both.
        // The tool moves from its current location to the given (X,Y,Z)
        // and I/J/K (or R) is used to determine the radius. Recall that
        // the IJK values are inherently relative (or incremental). They
        // indicate the position of the center of the arc, measured from
        // the cutter's starting point.
        DataCircular c = (DataCircular) cmd.data;
        if (c.xDefined == true) c.X += xCur;
        if (c.yDefined == true) c.Y += yCur;
        if (c.zDefined == true) c.Z += zCur;
      }
  }
  
  
  ///////////////////////////////////////////////////////////////////
  //
  // Code to deal with wizards.
  //
  ///////////////////////////////////////////////////////////////////
  
  
  private static WizardBase attemptClassLoad(String cName) {
    
    // Attempt to load the given class. If that's possible, then load it and
    // return it. Otherwise, return null.
    //
    // IMPORTANT. For this to work, the module-info file must include the line
    // exports vcnc.wizard
    // I'm a little unclear why this is, but the gist seems to be that it's
    // because the *.class file being loaded is outside the usual directory
    // hierarchy (or the JaR file) and the class loader needs some kind (?)
    // of special permission 
    //
    // BUG: the package name 'wizard_test' is hard-coded.
    
    // Try loading "normally," without messing with the class loader.
    // This should work if the wizard class is "built in" and was compiled 
    // as part of the original ger framework.
    try {
      // Package name, 'wizard_test', followed by class name.
      Class<?> loaded = Class.forName("wizard_test." +cName);
//      Class<?> loaded = Class.forName(cName);
      
      WizardBase answer = (WizardBase) loaded.newInstance();
      return answer;
      
    } catch (ClassNotFoundException e) {
      // BUG: To stderr for debugging.
      System.err.println("Not found: " +e);
    } catch (IllegalAccessException e) {
      System.err.println("Bad access: " +e);
    } catch (InstantiationException e) {
      System.err.println("Bad instance: " +e);
    }
    
    // Not a normal "built in" wizard. See if it's available as a class
    // file in the .ger directory.
    try {
      Path p = Paths.get(Persist.getGerLocation());
      URL url = p.toUri().toURL();
      
      System.out.println("url: " +url);
      
      URLClassLoader loader = new URLClassLoader(new URL[] {url});
      Class<?> loaded = Class.forName("wizard_test." +cName,true,loader);
      
      WizardBase answer = (WizardBase) loaded.newInstance();
      try {
        loader.close();
      } catch (IOException e) {
        // Really shouldn't happen.
        System.err.println("Strange error: " +e.getMessage());
        e.printStackTrace();
      }
      return answer;
      
    } catch (MalformedURLException e) {
      // BUG: This one really shouldn't happen. The gerDir must be a valid
      // directory, hence a valid URL.
      System.err.println("Malformed URL: " +Persist.getGerLocation());
    } catch (ClassNotFoundException e) {
      // BUG: To stderr for debugging.
      System.err.println("Not found: " +e);
    } catch (IllegalAccessException e) {
      System.err.println("Bad access: " +e);
    } catch (InstantiationException e) {
      System.err.println("Bad instance: " +e);
    }
    
    // Got here, so the class is unknown.
    return null;
  }

  private static WizardBase compileAndLoad(String cName) {
    
    // Although this is called *compile* and load, compiling here should
    // be unusual since that should have been done earlier (by the user,
    // from the CLI). Allowing for compilation here is more of a nicety in 
    // case the user forgot (and he was fortunate enough to have error-free 
    // Java code).
    // Note that WizCompile checks whether running as CLI or GUI.
    WizCompile.compile(cName);
    return attemptClassLoad(cName);
  }
  
  private static ArrayList<Statement> expandWizard(LLNode<Statement> node) {
    
    // Convert the given wizard statement to a series of straight G-code
    // statements. If w is not a wizard statement, then return null.
    // If there's a problem, return an ArrayList consisting of
    // a single error statement. Or, the wizard might have an internal problem,
    // and *it* might return a mixture of valid G-code and error statements.
    if (node == null) return null;
    Statement s = node.data;
    if (s == null) return null;
    if (s.type != WIZARD) return null;
    
    DataWizard wiz = (DataWizard) s.data;
    
    //System.out.println("Attempting standard load");
    
    WizardBase theWizard = attemptClassLoad(wiz.cmd);
    
    if (theWizard == null)
      theWizard = compileAndLoad(wiz.cmd);
    
    if (theWizard == null)
      {
        // Still null, so not a known class. Return an error Statement.
        ArrayList<Statement> answer = new ArrayList<>(1);
        Statement e = formError(s,"Unknown wizard: " +wiz.cmd);
        answer.add(e);
        return answer;
      }
    
    System.out.println("class loaded");
    
    // Got here, so the class is known and loaded. Run it.
    return theWizard.execute(wiz.args);
  }
  
  
  ///////////////////////////////////////////////////////////////////
  //
  // Code related to sub-programs
  //
  ///////////////////////////////////////////////////////////////////
  
  
  private static LList<Statement> deepCopy(
      LLNode<Statement> start,LLNode<Statement> end) {
    ;
    // Do a deep copy between the given nodes (inclusive) of the wip.
    // 
    // NOTE: I'm not happy with this since this is the kind of thing that
    // should be done within the LList<T> class, but I couldn't see any
    // way to do it given Java's type system. See LList.insertAfter().
    LList<Statement> answer = new LList<>();
    
    answer.head = new LLNode<Statement>();
    if (start.data != null)
      answer.head.data = start.data.deepCopy();
    
    if (start == end)
      {
        // Special case of a one-element list.
        answer.tail = answer.head;
        return answer;
      }
    
    LLNode<Statement> prevDest = answer.head;
    LLNode<Statement> curSrc = start.next;
    while (curSrc != end)
      {
        LLNode<Statement> newNode = new LLNode<>();
        prevDest.next = newNode;
        newNode.prev = prevDest;
        
        if (curSrc.data != null)
          newNode.data = curSrc.data.deepCopy();
        
        prevDest = prevDest.next;
        curSrc = curSrc.next;
      }
    
    LLNode<Statement> newNode = new LLNode<>();
    prevDest.next = newNode;
    newNode.prev = prevDest;
    if (end.data != null)
      newNode.data = end.data.deepCopy();
    answer.tail = newNode;
    
    return answer;
  }
  
  private static boolean subProgramLayer() {
    
    // This filters out calls to sub-programs (M98, M99), which has the effect
    // of making the program longer -- maybe *much* longer. Because G-code 
    // doesn't have variables or loops, this is conceptually a matter of
    // inserting some known number of copies of the sub-programs. 
    //
    // This also removes O-codes from the stream. After M98/99 have been 
    // unraveled, O-codes serve no purpose. It's tempting to put the main 
    // O-code back (at the head of the file) since the user's physical CNC 
    // machine may require one, but he can always put it back.
    //
    // It removes M30 from the stream too. Explicitly ending the program
    // no longer serves any purpose. The program simply runs off the end
    // of itself. For the same reason, EOF is eliminated too.
    // 
    // Do this immediately after machine directives, and before any other 
    // layer.
    //
    // This makes a complete pass through the G-code to look ahead and find
    // sub-programs and must be called as a separate thing, not as one of a
    // series of per-statement digestive steps.
    // 
    // Reusing an O-code is such a dramatic error that there's really no
    // point in trying to continue. Likewise for an unexpected M99.
    // Return false if that happens; true otherwise.
    
    // Begin by creating a lookup table showing where all subprograms begin 
    // (where the O-code appears). The key is the program number and the value
    // is the node in the wip at which that program number appears.
    Hashtable<Integer,LLNode<Statement>> subprogs = new Hashtable<>();

//    System.out.println("Entering: ");
//    System.out.println(wipToString());
    
    // Create the table of sub-programs.
    LLNode<Statement> curNode = wip.head;
    while ((curNode != null) && (curNode.data.type != EOF))
      {
        Statement s = curNode.data;
        if (s.type == PROG)
          {
            DataSubProg o = (DataSubProg) s.data;
            
            if (subprogs.containsKey(o.progNumber))
              {
                // Program numbers must be unique.
                // Replace this reuse with an error statement.
                curNode.data.makeError(
                    "Reuse of program number " +o.progNumber);
                
                // There's no point in continuing, and difficult to do anyway.
                return false;
              }
            
            // Note the entry is the statement *after* the O-code.
            subprogs.put(o.progNumber,curNode.next);
          }
        
        curNode = curNode.next;
      }
    
//    System.out.println("subprogs table: ");
//    for (Integer ocode : subprogs.keySet())
//      {
//        curNode = subprogs.get(ocode);
//        Statement data = curNode.data;
//        System.out.println("  O" +ocode+  " at " +data.lineNumber);
//      }
    

    // For each entry in the table of sub-programs, create another table
    // that points to the return point for each sub-program. This is the
    // Statement immediately before the M99. Below, when we scan through the
    // code, looking for M98s, and substitute in the sub-programs, this is
    // what we need. Insert the block of code noted here, then scan again,
    // looking for further sub-programs being called by what was just inserted.
    Hashtable<Integer,LLNode<Statement>> subends = new Hashtable<>();
    for (Integer ocode : subprogs.keySet())
      {
        // The first statement after the opening O-value.
        curNode = subprogs.get(ocode);
        
//        System.out.println("looking for " +ocode);
        
        // Scan through until we find M99 (return). This also accepts M30, in
        // which case the M30 ends both the subprogram and the program as a
        // whole.
        while ((curNode != null) && (curNode.data.type != EOF))
          {
            Statement s = curNode.data;
            if ((s.type == M99) || (s.type == M30))
              {
                // Found it. We want the statement *before* this one.
//                System.out.println("inserting end for " +ocode);
                subends.put(ocode,curNode.prev);
                break;
              }
            
            if (s.type == PROG)
              {
                // We hit another O-value. The M99 is definitely missing.
                // Insert an error statement after the opening O-value.
                curNode = subprogs.get(ocode);
                Statement err = formError(curNode.data,
                    "missing M99 to end sub-program (or M30 on main program)");
                wip.insertAfter(curNode,err);
                
                // Again, difficult thing from which to recover.
                return false;
              }
            
            curNode = curNode.next;
          }
        
        // Maybe there was no M99 or M30. Act like there was an M30 just
        // before EOF.
        if ((curNode == null) || (curNode.data.type == EOF))
          subends.put(ocode,wip.tail);
      }

    
//    System.out.println("subends table: ");
//    for (Integer ocode : subends.keySet())
//      {
//        curNode = subends.get(ocode);
//        Statement data = curNode.data;
//        System.out.println("  O" +ocode+  " at " +data.lineNumber);
//      }
//
//    System.out.println("Will start: ");
//    System.out.println(wipToString());
    
    // There is no stack as such, and we can't really even determine the
    // depth of calls -- not easily -- because it's based on substituting
    // subprogram code without really tracking the "returns." Even so, we
    // want to catch possible infinite recursion errors. The user might get
    // mixed up and have (say) O100 call O200, which calls back to O100.
    // The best we can easily do is count how many times we've made any
    // substitution. There's no clear point at which this count is certain
    // to be an error, but any more than this is pretty likely to be an error!
    // BUG: Maybe this should be setable by the user. A number like 100
    // is more reasonable. In rare cases, he might want more, but we risk
    // out-of-memory errors instead of sensible error messages with a large 
    // value. 
    int callDepth = 0;
    final int MaxCallDepth = 5000;
    
    // Walk through the entire statement stream, substituting as we go.
    curNode = wip.head;
    while (curNode != null)
      { 
        Statement cur = curNode.data;

//        System.out.println("Next run through: ");
//        System.out.println("At " +cur.toString());
//        System.out.println(wipToString());
       
        
        if ((cur.type == M30) || (cur.type == EOF))
          {
            // Chop everything off from here on. Anything that follows
            // isn't reachable.
            wip.truncateAt(curNode);
            break;
          }
        
        if (cur.type == PROG)
          {
            // O-codes are irrelevant in later layers. Remove it from the wip.
//            wip.remove(curNode);
//            curNode = curNode.next;
            curNode = wip.removeGet(curNode);
            continue;
          }
        
        if (cur.type == M98)
          {
            // Call a sub-program.
            ++callDepth;
//            System.out.println("call depth: " +callDepth);
            
            if (callDepth > MaxCallDepth)
              { 
                Statement err = formError(cur,
                    "too many sub-programs; circularity?");
                wip.insertAfter(curNode,err);
                return false;
              }
            
            // Make the call(s).
            DataSubroutineCall call = (DataSubroutineCall) cur.data;
            if (subprogs.containsKey(call.programNumber) == false)
              {
                
//                System.out.println("Failed to find O-" +call.programNumber);
                
                // Attempt to call a sub-program that was never defined.
                Statement err = formError(cur,"no such sub-program was defined");
                wip.insertAfter(curNode,err);
                curNode = curNode.next.next;
                continue;
              }
            
            // Look to subprogs and subends for the block of code to be
            // inserted some number of times. The curNode points to the M98
            // statement and the insertion is done after that, but don't
            // remove the M98 yet; remove it after the insertions.
            LLNode<Statement> subStart = subprogs.get(call.programNumber);
            LLNode<Statement> subEnd = subends.get(call.programNumber);
            
//            System.out.println("inserting sub-program from");
//            System.out.println(subStart.data.toString());
//            System.out.println("to");
//            System.out.println(subEnd.data.toString());
            
            
            // This is annoying. What we insert needs to be a deep copy
            // of the original code so that you don't get circular weirdness.
            // And it has to be a new copy for each insertion.
            // Note also that, since these copies are completely identical,
            // there is no point in trying to insert each new bit after a
            // previously inserted bit. Just do multiple insertions after
            // curNode.
            for (int i = 0; i < call.invocations; i++)
              {
                LList<Statement> freshCopy = deepCopy(subStart,subEnd);
                wip.insertAfter(curNode,freshCopy.head,freshCopy.tail);
              }
            
//            System.out.println("After insertion, total is");
//            System.out.println(wipToString());
            
            // Remove the M98.
            curNode = wip.removeGet(curNode);
            continue;
          }
        
        if (cur.type == M99)
          {
            // These should not appear in the stream, and we're basically
            // hosed if they do.
            Statement err = formError(curNode.data,"Unexpected M99");
            wip.insertAfter(curNode,err);
            return false;
          }
        
        // Nothing that's handled here. Advance to the next statement.
        curNode = curNode.next;
      }
    
    // All went well.
    return true;
  }
  
  
  ///////////////////////////////////////////////////////////////////
  //
  // Code related to machine directives.
  //
  ///////////////////////////////////////////////////////////////////

  
	private static Statement handleMachineDirective(Statement wizard) {

    // The given statement is known to be a wizard. Either: 
    // * Act on it and return null (null since the statement was "consumed"
	  //   and should be removed from the wip).
    // * Determine that there's an error and return an error statement.
    //   Using genuine wizards (not machine directives) before the O-code is
    //   such an error.
    DataWizard wizData = (DataWizard) wizard.data;
    
    // Remember, this only accepts "machine directives," not genuine wizards.
    if (wizData.cmd.equals("Billet"))
      {
        // BUG: Handle this case...and add others.
        // For now, just consume this and ignore it.
        System.err.println("Billet directive not handled yet.");
        return null;
      }
    else if (wizData.cmd.equals("SetUnits"))
      {
        // Allow the user to indicate, before the program starts, what the
        // units are. This is a little silly since the program could include
        // G20 or G21, but maybe somebody would want this and it's an easy
        // test for the code. On second thought, it's not entirely silly.
        // A person might normally want their machine to be an "inch" machine,
        // but they want to run some code (maybe that someone gave them) that
        // assumes a "mm" machine.
        if (wizData.args.size() != 1)
          return formError(wizard,"SetUnits has the wrong number of arguments");
        
        Object arg = wizData.args.get(0);
        
        if (arg instanceof String == false)
          return formError(wizard,"SetUnits takes 'inch' or 'mm' as argument");  
        
        String choice = (String) arg;
       
        if (choice.equals("inch"))
          DefaultMachine.inchUnits = true;
        else
          DefaultMachine.inchUnits = false;
      }
    else
      {
        // Something unexpected that shouldn't occur before the O-statement.
        // Could be a typo, or could be an attempt to use a genuine wizard
        // before it's permitted.
        return formError(wizard,
            "unexpected " +wizData.cmd+ " before O (program number)");
      }
    
    // Got here, so the machine directive wizard was properly handled, and
    // therefore consumed from the stream.
    return null;
	}
	
	private static void machineDirectiveLayer() {
	  
	  // Layer0A comes immediately after the Parser and acts on certain 
	  // "machine directives" that must appear before the O-code. These are 
	  // for things like setting the billet dimensions or changing the work 
	  // offsets table or tool table in way that is independent of the 
	  // persistent machine setup.  
	  //
	  // To the user, syntactically, these are a lot like wizards, and they come
	  // in that way from the parser. In essence, any "wizard" that occurs before
	  // the O-code must be one of these directives, and any "wizard" that comes 
	  // after the O-code is a wizard in the usual sense.
	  //
	  // Unlike most layers, this makes a complete pass through the gcode --
	  // really, just a pass up to the first occurrence of an O-code -- to
	  // remove and act on all machine directives.
	  // 
    // BUG: None of these directives are actually handled, though "Billet"
    // will get filtered out as a valid directive and the arguments are checked
    // for "SetUnits." Actually, maybe SetUnits is done (at least for now).

    LLNode<Statement> curNode = wip.head;
    while ((curNode != null) && (curNode.data.type != EOF))
      {
        Statement cur = curNode.data;
        
        if (cur.type == PROG)
          // Hit an O-statement, so this layer is done.
          return;
         
        if (cur.type != WIZARD)
          {
            // Just a plain old statement, not a wizard directive.
            curNode = curNode.next;
            continue;
          }
        
        // The statement is a wizard, hopefully a machine directive. 
        // This returns either null or an error statement.
        Statement result = handleMachineDirective(cur);
        if (result != null)
          {
            // Must have been an error, and it replaces the original statement.
            curNode.data = result;
            curNode = curNode.next;
          }
        else
          {
            // Properly handled, so remove the directive statement and do 
            // *not* advance the index.
//            wip.remove(curNode);
//            curNode = curNode.next;
            curNode = wip.removeGet(curNode);
          }
      }
	}

  
  ///////////////////////////////////////////////////////////////////
  //
  // Code related to converting "prerendering." This converts the 
	// G-code to an even simpler form, with nothing but linear moves.
  //
  ///////////////////////////////////////////////////////////////////

	
  private static void preRender() {
    
    // Modify the wip to a form suitable for rendering. It becomes nothing
    // but a series of MOVE Statements, possibly with M06 (tool change).
    // 
    // NOTE: It is tempting to leave other (harmless and irrelevant) Statements
    // in the output. At some point in the future, I might want these to
    // do things like look for rapid moves in the material (would want G00 and
    // G01 to see that) or any number of other thing that might be simulated
    // to check for "bad things." It seems better to limit and tailor this 
    // method to a particular case and worry about dealing with those other
    // things when the need arises.
    //
    // NOTE: Another thing that's tempting is to take the scale into account
    // here so that all coordinates are in multiples of the scale. But this
    // would give up a small amount of accuracy when voxeling. It would also 
    // make what is done here more complicated.
    LLNode<Statement> curNode = wip.head;
    while (curNode != null)
      {
        Statement s = curNode.data;
        
        // The reference plane is one of the few things that matters *and* is 
        // removed from the wip.
        boolean skip = noteAxis(s);
        if (skip == true)
          {
            curNode = wip.removeGet(curNode);
            continue;
          }
        
        // These remain, untouched.
        if (s.type == M06)
          {
            curNode = curNode.next;
            continue;
          } 
        
        // Need to note where a MOVE sends the tool, and it remains
        // untouched too.
        if (s.type == MOVE)
          {
            DataMove m = (DataMove) s.data;
            if (m.xDefined == true) xCur = m.xValue;
            if (m.yDefined == true) yCur = m.yValue;
            if (m.zDefined == true) zCur = m.zValue;
            
            curNode = curNode.next;
            continue;
          }
        
        // The main task is converting arcs to line segments.
        if ((s.type == G02) || (s.type == G03))
          {
            ArcCurve c = null;
            try {
              c = ArcCurve.factory(xCur,yCur,zCur,curAxis,s);
            } catch (Exception e) {
              // This should be impossible. Problematic stuff was filtered
              // out earlier (converted to an ERROR statement).
              System.err.println("Unexpected arc error.");
              e.printStackTrace();
              return;
            }
            
            
            // BUG: The accuracy/error should not be hard coded.
            // This is another thing that might make sense as a machine
            // directive.
            
            ArrayList<LineCurve> segs = c.toLineSegs(0.001);
            
            // Remove curNode and replace with segs. Convert LineCurves to
            // Statements.
            ArrayList<Statement> segStatements = new ArrayList<>();
            for (LineCurve lc : segs)
              segStatements.add(lc.toStatement());
            
            // This is inefficient since curNode will point to the first of the 
            // newly inserted items and this loop will need to skip over 
            // everything that was just inserted. Since the newly inserted/
            // substituted statements will be digested, the current x/y/z
            // will be updated.
            curNode = wip.replaceAtGet(curNode,segStatements);
            
            continue;
          }  
        
        // Everything else is filtered out.
        curNode = wip.removeGet(curNode);
      }
  }
	
	
  ///////////////////////////////////////////////////////////////////
  //
  // Main entry-point and high-level management.
  //
  ///////////////////////////////////////////////////////////////////
	
  
	private static void finalStep(LLNode<Statement> curNode) {
	  
	  // The last step of the primary digestive loop (before cutter comp).
	  // An arc is brought to standard from (I/J/K, not radius) and note
	  // where the statement takes the cutter.  At this point, all coordinates
	  // are in absolute terms, relative to machine zero.
	  if (curNode == null) return;
	  if (curNode.data == null) return;
	  Statement cmd = curNode.data;
	  
    // Convert an arc move to a standard form that uses I/J/K instead
    // of the radius.
    if ((cmd.type == G02) || (cmd.type == G03))
      {
        // This thing throws if there is a geometric problem.
        // NOTE: I'm not keen on that, but it seems simplest.
        ArcCurve c = null;
        try {
          c = ArcCurve.factory(xCur,yCur,zCur,curAxis,cmd);
        } catch (Exception e) {
          
          // Geometry is wrong.
          curNode.data.makeError(e.getMessage());
          return;
        }
        
        // Arc is geometrically valid. Express it in standard form.
        curNode.data = c.toStatement();
        
        // And note where the tool ends up.
        DataCircular a = (DataCircular) curNode.data.data;
        if (a.F > 0.0) curFeedRate = a.F;
        if (a.xDefined == true) xCur = a.X;
        if (a.yDefined == true) yCur = a.Y;
        if (a.zDefined == true) zCur = a.Z;
        
        return;
      }
    
	  if (cmd.type == MOVE)
	    {
	      DataMove m = (DataMove) cmd.data;
	      if (m.fDefined == true) curFeedRate = m.fValue;
        if (m.xDefined == true) xCur = m.xValue;
        if (m.yDefined == true) yCur = m.yValue;
        if (m.zDefined == true) zCur = m.zValue;
	      return;
	    }
	}
	
  private static void resetMachine() {
    
    // Call this before a translation so that the machine starts off in its
    // default state. Note that we use Persist.reload() to go all the way back
    // to the settings stored in the .ger directory. A previous run may have
    // changed the values in MachineState -- for example a call to the
    // SetUnits directive made by a previous run or a change to the tool table.
    
    // BUG: This is done several times. As things stand, it goes to the disk
    // each time. Set up Persist so that it stores the settings.
    Persist.reload();
    
    rapidTravel = false;
    curInch = DefaultMachine.inchUnits;
    curAxis = AxisChoice.XY;
    usingPolar = false;
    usingIncremental = false;
    curFeedRate = -1.0;
    usingCutterComp = false;

    xPRZ = 0.0;
    yPRZ = 0.0;
    zPRZ = 0.0;

    xCur = 0.0;
    yCur = 0.0;
    zCur = 0.0;
  }

  private static Statement formError(Statement s,String msg) {
    
    // I had hoped not to need this, but there are a couple of cases where
    // it's easier or more appropriate. This happens when an error statement 
    // is inserted into the wip rather than replacing an existing statement.
    Statement answer = new Statement(UNKNOWN);
    answer.lineNumber = s.lineNumber;
    answer.makeError(msg);
    return answer;
  }
	
	private static void doLayers(int depth) {
	
	  // Translate up to the given depth. The output appears in this.wip.
    // The initial parsing to the wip was done by the caller.
	  if (depth <= ThruParser) return;

	  // The machine and its state are only relevant after parsing, but
	  // must be reset to "factory settings."
    resetMachine();
    
	  // The first couple of layers are handled as special cases. Machine 
	  // directives are simply acted upon and removed from the wip.
	  machineDirectiveLayer();
	  if (depth <= ThruDirectives) return;
	  
	  // And sub-programs are an even more special case because they require 
	  // infinite look-ahead.
	  // Removes M98/99, M30 and O-codes.
	  if (subProgramLayer() == false)
	    // For certain errors, like reusing an O-code, there is no recovery.
	    return;
	  if (depth <= ThruSubProgs) return;
	  
    // For the remaining cases, we take a single statement out of the wip, and 
	  // digest it to "final form." In some cases the statement is removed from
	  // the wip (typically while noting some state change); in other cases,
	  // the statement is changed somehow (e.g., change of units); or the
	  // statement is expanded to a large number of statements (wizards).
	  //
	  // Digesting things statement-by-statement means that any notion of
	  // "layers" or a particular order in which these items are applied is
	  // artificial. That said, certain statements must be handled together or
	  // in a certain order. For example, change of units (G20/21) is affected by
	  // the plane you are in (G17/18/19) and whether in polar coordinate 
	  // mode (G15/16). We must consider the code statement-by-statement and
	  // not layer-by-layer due to the way each statement can change the machine
	  // state for statements that follow.
	  LLNode<Statement> curNode = wip.head;
	  while (curNode != null)
      {
        // Check for mode changes and skippable statements. These are things
        // that remain in the wip.
        boolean skip = initialChecks(curNode);
        if (skip == true)
          {
            curNode = curNode.next;
            continue;
          }
        
        // These are things that *are* removed from the WIP, mostly 
        // mode changes.
        skip = removedChecks(curNode);
        if (skip == true)
          {
            curNode = wip.removeGet(curNode);
            continue;
          }
        
        // If there's a problem with the current statement, convert it to
        // type ERROR, with a message.
        skip = errorChecks(curNode);
        if (skip == true)
          {
            curNode = curNode.next;
            continue;
          }

        if (depth >= ThruWizards)
          {
            // Wizards are a messier case due to expansion.
            ArrayList<Statement> exp = expandWizard(curNode);
            if (exp != null)
              {
                // Yes, it was a wizard. Insert the expansion.
                curNode = wip.replaceAtGet(curNode,exp);
                continue;
              }
          }
        
        if (depth >= ThruUnits)
          // Convert to the default units for the machine. 
          convertUnits(curNode.data);
        
        if (depth >= ThruWorkOffsets)
          {
            // The various ways of changing PRZ: G52, G54-59 and G92.
            // This also converts any motion commands to take the PRZ
            // into account.
            skip = handlePRZ(curNode.data);
            if (skip == true)
              {
                // For this one, we remove it from the WIP. 
                curNode = wip.removeGet(curNode);
                continue;
              }
          }
        
        if (depth >= ThruPolar)
          convertPolar(curNode);
        
        if (depth >= ThruIncremental)
          convertIncremental(curNode);
        
        // Everything about the curNode statement is now in absolute 
        // cartesian coordinates, relative to machine zero, using machine
        // units. Last thing is to note where the cutter moves and
        // translate arcs to a standard form (explicit center, no radius).
        finalStep(curNode);
        
        curNode = curNode.next;
      }
	  
	  if (depth < ThruCutterComp)
	    return;
	  
	  // Because cutter comp requires look-ahead, it has to be done last,
	  // outside the loop above, after all the various moves have been settled.
	  // This should also be clearer to the user. If you allow wizards to get
	  // mixed up with cutter comp, then all kinds of edge-cases arise.
	  //
	  // At this point, most of the modes for the machine are irrelevant.
	  // Put another way, the machine is always using absolute cartesian
	  // coordinates, relative to machine zero, in the default units.
	  
	  // Reset the machine, and start over....
	  
	  // BUG: Don't forget to take any moves that don't actually move the
	  // tool out of the wip. 
	  
	  // One of the things this does, that is not directly related to
	  // Translation, is determine the max/min values seen in each axis.
	  // This will be used when the billet is drawn.
	  // BUG: I need to allow the user to change the billet location and
	  // dimensions.
	  // BUG: This will need to be modified (after I deal with cutter
	  // comp) to include the tool radius. For one thing, I want to provide
	  // the user with the default option of saying that the billet extends
	  // half an inch (say) beyond every cut.
	  
	  resetMachine();
	  handleCutterComp();
	  
	  
	  if (depth <= ThruCutterComp)
	    return;
	  
	  
	  // Must be doing the pre-rendering step too.
    resetMachine();
	  preRender();
	  
	  
	  
	}
	
	private static String wipToString() {
	  
	  // Convert the WIP to a set of statements suitable for printing.
	  // This is used to generate output to the user, and it's also
	  // handy for debugging.
    StringBuffer answer = new StringBuffer();

    LLNode<Statement> curNode = wip.head;
    while ((curNode != null) && (curNode.data.type != EOF))
      {
        answer.append(curNode.data.toString());
        answer.append("\n");
        
        curNode = curNode.next;
      }
    
    return answer.toString();
	}
	
	private static void digestToWIP(String gcode,int depth) {

	  // The depth may not equal ThruLexer since the output of the lexer
	  // does not consist of statements.
	  wip = Parser.process(gcode);
    
    // Maybe we are only parsing.
    if (depth == ThruParser) return;
    
    // Translate the code to simpler form. The result goes into the wip.
    doLayers(depth);
	}
	
  public static String digest(String gcode,int depth) {
    
    // Run all of the G-code provided through the translation process up 
    // to the given depth, as specified using one of the static final values 
    // above. Return a result suitable for display in a text editor.
    
    // The lexer is a special case...
    if (depth == ThruLexer)
      return Lexer.digestAll(gcode);
    
    digestToWIP(gcode,depth);
    
    return wipToString();
  }  

  public static RenderPacket preRender(String gcode) {
    
    // Return the wip in a form suitable for rendering. This returns nothing
    // but a series of MOVE Statements, possibly with M06 (tool change).
    // 
    // NOTE: It is tempting to leave other (harmless and irrelevant) Statements
    // in the output. At some point in the future, I might want these to
    // do things like look for rapid moves in the material (would want G00 and
    // G01 to see that) or any number of other thing that might be simulated
    // to check for "bad things." It seems better to limit and tailor this 
    // method to a particular case and worry about dealing with those other
    // things when the need arises.
    //
    // NOTE: Another thing that's tempting is to take the scale into account
    // here so that all coordinates are in multiples of the scale. But this
    // would give up a small amount of accuracy when voxeling. It would also 
    // make what is done here more complicated.
    digestToWIP(gcode,ThruPrerender);
    
    // This is the only outside access to the wip. Everything in this class
    // is static, including the wip. It is tempting to return a deep copy
    // of the wip to avoid accidental reuse of this variable, but it will
    // not be a problem. There are only two entry points, this method and
    // digest(), and both of them start by creating a new wip when they
    // call wip = Parser.process().
    RenderPacket answer = new RenderPacket();
    
    answer.code = wip;
    
//    answer.xMax = xMax;
//    answer.xMin = xMin;
//    answer.yMax = yMax;
//    answer.yMin = yMin;
//    answer.zMax = zMax;
//    answer.zMin = zMin;
    
    // BUG: Hard coded arbitrary numbers for the margins.
    answer.xLeft = -0.50;
    answer.xRight = xMax + 0.50;
    answer.yBot = -0.50;
    answer.yTop = yMax + 0.50;
    answer.zTop = 0.0;
    answer.zBot = -1.000;
    
    return answer;
  }
}




