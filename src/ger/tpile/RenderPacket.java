package ger.tpile;

import ger.util.LList;

/*

This information generated from input G-code that is needed to render an
image.

BUG: Need to include more stuff, like the tool table.

*/

public class RenderPacket {
  
  // The code after translation to a form that consists of nothing but MOVE
  // and M06 statements.
  public LList<Statement> code = null;

  // The maximum extent of travel in each axis. This includes the cutter
  // diameter.
  // BUG: Does it?
  // BUG: Is this actually needed? Seems not, except perhaps as information
  // for the user. It is needed when the edge values are determined, but 
  // not (?) here.
//  public double xMin = 0.0;
//  public double xMax = 0.0;
//  public double yMin = 0.0;
//  public double yMax = 0.0;
//  public double zMin = 0.0;
//  public double zMax = 0.0;

  // The size of the billet, assumed to be rectangular, in inches or mm.
  // These are the coordinates of the edges, so the width runs from
  // xLeft to xRight, and xLeft will often be negative. Likewise,
  // yBot will often be negative. The zTop value will generally be zero,
  // and zBot will usually be negative so that abs(zBot) is the thickness.
  public double xLeft = 0.0;
  public double xRight = 0.0;
  public double yBot = 0.0;
  public double yTop = 0.0;
  public double zBot = 0.0;
  public double zTop = 0.0;
  
  
  
}
