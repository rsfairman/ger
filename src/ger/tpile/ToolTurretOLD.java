package ger.tpile;

/*

Holds the specification of the contents of the tool turret.
This is set by a dialog from the main window.

BUG: This is in flux. The java version and the C++ version were a little
different. I was doing something clever(ish) in the C++ version to help
with the rendering process. See toolspec.h/cpp, and the various "spec"
files for different cutter types. 

BUG: Don't get side-tracked on the tool table yet. The tool table
matters most when rendering. It has very little to do with transpiling --
but not nothing. It matters for things like cutter comp and TLO.
 
*/

public class ToolTurretOLD {

  // The number of tools in the turret (or slots anyway; they might be empty).
  public static int TableSize = 20;

  // The possible types of tool. A "Drill" is any sort of tool with a pointy
  // tip. The program does not distinguish between drills and mills with
  // a pointy end.
  public static int Endmill = 0;
  public static int Ballmill = 1;
  public static int Drill = 2;
  public static int CenterDrill = 3;
  
  // This is here strictly for use in the tool turret dialog, when the
  // user chooses a drill, he then chooses an angle, and this value
  // is used in that situation.
  //public static int UserChoice = 4;

  // One of the values above
//  public static int[] tool = new int[TableSize];
  
  // The length of the tool. 
  // BUG: For an abstract machine, this may be irrelevant.
  // I'm not sure if this is ever used at all.
//  public static double[] toolLength = new double[TableSize];
  
  // Actual tool diameter.
  public static double[] toolDiameter = new double[TableSize];
  
  // Register values.
  public static double[] hRegister = new double[TableSize];
  
  // The angle of the tip of the tool for drill-like tools (type Drill above).
  // These values are given in degrees.
//  public static double[] angle = new double[TableSize];
  
  // Each tool has a set of data used when making cuts. These data
  // structures are not initialized unless the user calls for it.
  // So, before using one of these, call prepTool() for the appropriate
  // index. 
//  ToolSpec *cutter[TableSize];
}
