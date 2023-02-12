package vcnc.tpile;

/* 

The default settings for the machine. These are the settings the machine
should have when it's "turned on" -- the settings before every program run.
This should be reset to the values that persist on the disk (in the .ger
directory) before every run.

*/

import vcnc.workoffsets.WorkOffsets;
import vcnc.tooltable.ToolTurret;


public class DefaultMachine {

  // The tool turret. This is fixed throughout the life of each G-code
  // script, after any initial adjustments before the opening O-code.
  public static ToolTurret turret = new ToolTurret(20);
  
  // The values to be used for G56, etc.
  public static WorkOffsets workOffsets = new WorkOffsets();
  
  // Whether the machine works internally with inches (true) or mm (false).
  public static boolean inchUnits = true;
  
  // BUG: In the original code there's also a concept of 'scale'. I might
  // need that at some point. However, it's less about the machine, and 
  // more about 3D rendering.
  
  
  // In a few places we need to ensure that points are different and rounding
  // error can make that uncertain. If two points in the plane are within
  // this distance, then they are considered to be the same.
  // BUG: I *might* want to let the user change this (but I hope not).
  public static double identicallyClose = 1e-6;
  
  // And sometimes we need to allow more leeway.
//  public static double fairlyClose = 2e-3;
}




