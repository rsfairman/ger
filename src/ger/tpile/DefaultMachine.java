package ger.tpile;

/*

The "factory settings" for the simulated machine -- basically the state
of things prior to each G-code run.

*/


import ger.tooltable.ToolTurret;
import ger.workoffsets.WorkOffsets;


public class DefaultMachine {

  // The tool turret. This is fixed throughout the life of each G-code
  // script, after any initial adjustments made by machine directives before
  // the opening O-code.
  public static ToolTurret turret = new ToolTurret(20);
  
  // The values to be used for G56, etc.
  public static WorkOffsets workOffsets = new WorkOffsets();
  
  // Whether the machine works internally with inches (true) or mm (false).
  public static boolean inchUnits = true;
  
  // In a few places we need to ensure that points are different and rounding
  // error can make that uncertain. If two points in the plane are within
  // this distance, then they are considered to be the same.
  // BUG: I *might* want to let the user change this (but I hope not).
  public static double identicallyClose = 1e-6;
  
  // And sometimes we need to allow more leeway.
//  public static double fairlyClose = 2e-3;
  
  // This has nothing to do with "the machine," but here is the most natural
  // place for it since it's a program-wide global setting.
  public static double scale = 0.005;
}




