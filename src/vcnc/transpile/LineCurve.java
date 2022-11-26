package vcnc.transpile;

// BUG: I should rename these classes, LineCurve, ToolCurve, ArcCurve to
// CurveLine, CurveArc and CurveTool so that they more obviously go together.

/*

Represents a tool move along a line segment. Note that it is based on
the ToolCurve class. In many respects, what this represents is simply
a line segment, never mind the fact that it's in the context of
the CNC problem.

*/

import vcnc.parse.Statement;
import vcnc.parse.StateData;
import vcnc.parse.MoveState;


public class LineCurve extends ToolCurve {

  // This is needed to properly implement toState and toStatement().
  private double feedRate;

  public LineCurve() {
    
    // Do nothing.
  }

  public LineCurve(double x0, double y0, double z0, MoveState data) {
    
    // The new line starts at (x0,y0,z0) and ends at the point given by the
    // data argument.
    
    // Copy over the start-point.
    this.x0 = x0;
    this.y0 = y0;
    this.z0 = z0;
    
    // Pull out the end-point.
    x1 = x0;
    y1 = y0;
    z1 = z0;
    
    if (data.xDefined) x1 = data.xValue;
    if (data.yDefined) y1 = data.yValue;
    if (data.zDefined) z1 = data.zValue;
    if (data.fDefined) feedRate = data.fValue; else feedRate = -1.0;
  }

  public LineCurve(double x0,double y0,double z0,
      double x1,double y1,double z1,double feedRate) {
        
    this.x0 = x0;
    this.y0 = y0;
    this.z0 = z0;
    
    this.x1 = x1;
    this.y1 = y1;
    this.z1 = z1;
    
    this.feedRate = feedRate;
  }

  public CartCoord curve(double t) {
    
    // Return the point given by the parametric equation
    // (x,y,z) = (x0,y0,z0) + t (x1-x0,y1-y0,z1-z0); t should be in [0,1].
    CartCoord answer = new CartCoord();
    
    answer.x = x0 + t*(x1-x0);
    answer.y = y0 + t*(y1-y0);
    answer.z = z0 + t*(z1-z0);
    
    return answer;
  }

  public void offset(boolean ccLeft,double radius) {
      
    // Offset this curve to take cutter comp into account
    //
    // NOTE: In the Java version I tried to do things involving the choice of plane,
    // and I don't think that was right; it just doesn't make sense to try to change
    // the meaning of CC based on the reference plane. The tool should be "to the
    // side of" the move, and the sense in which being "to the side of" the line
    // has nothing to do with the choice of reference plane.
    //
    // Suppose that the line is from (x0,y0) to (x1,y1).
    // Define v = (x1-x0,y1-y0) so that v points from (x0,y0) to (x1,y1).
    // Let q be the length of v so that v/q has length 1. Now rotate
    // v/q through -90 degees to get w = (1/q)(y1-y0,x0-x1) -- in particular,
    // w dot v = 0. The offset points are (x0,y0) +/- w * r and
    // (x1,y1) +/- w * r, where r is the tool radius. Whether to choose plus or 
    // minus depends on whether we're doing left or right cutter comp.
    // 
    // In particular, the adjustment done here has no effect on z0 or z1.
    
    // Length of line segment.
    double q = Math.sqrt((x0-x1)*(x0-x1) + (y0-y1)*(y0-y1));
    
    // If q is zero, then the move is entirely in the z direction and
    // there's nothing to do.
    if (q == 0.0)
      return;
    
    double wx = (y1-y0)/q;
    double wy = (x0-x1)/q;
    
    if (ccLeft == true)
      {
        x0 = x0 - wx * radius;
        x1 = x1 - wx * radius;
        y0 = y0 - wy * radius;
        y1 = y1 - wy * radius;
      }
    else
      {
        x0 = x0 + wx * radius;
        x1 = x1 + wx * radius;
        y0 = y0 + wy * radius;
        y1 = y1 + wy * radius;
      }
  }

  public void trimEnd(ParamPair p,ToolCurve B,double toolRadius) {
    
    // Trim off the tail end of the curve (near x1/y1/z1). We do need curve B.
    // We want to trim this curve so that it ends at p.t1, except that it should
    // end, not exactly at p.t1, but at the point along its length where the 
    // tool just touches curve B. Basically, we have to start at this curve's
    // position at p.t1, and "back up" until we are toolRadius away from curve B 
    // at its position p.t2.
    //
    // This is almost identical to what's in ArcCurve.
    //
    // BUG: See toolcurve.h. I don't think I need B or toolRadius.
  //  double t = findPointAtDistance(p,B,toolRadius);
    
    //Point pt = curve(t);
    CartCoord pt = curve(p.t1);
    this.x1 = pt.x;
    this.y1 = pt.y;
    this.z1 = pt.z;
  }

  public StateData toState() {
    
    // This allocates a new object. The caller must delete it.
    MoveState theState = new MoveState();
    
    theState.xDefined = true;
    theState.xValue = x1;
    theState.yDefined = true;
    theState.yValue = y1;
    theState.zDefined = true;
    theState.zValue = z1;
    
    if (this.feedRate > 0.0)
      {
        theState.fDefined = true;
        theState.fValue = feedRate;
      }
    else
      theState.fDefined = false;
    
    return theState;
  }

  public void toStatement(Statement cmd) {
    
  // Modify the given statement to be of the correct type, and with the correct "guts."
  cmd.type = Statement.MOVE;
  
//  if (cmd.data != null)
//    delete cmd.data;
  cmd.data = toState();
}

  /*
public void getSignals(CNCSignalArray *theSignals,double stepSize) 
{
  // Fill in theSignals.cncSignals and numSignals based on this line.
  // The step size is the magnitude of the move resulting from a single pulse.
  // It is assumed that theSignals->cncSignals is initially NULL.
  //
  // IMPORTANT: there could be odd little rounding errors that may accumulate
  // if the step size is relatively large. For that reason, the last thing
  // this method does is reset the x1/y1/z1 values to be equal to the exact
  // postion to which the last pulse brings the tool. The caller should note
  // this position so that it knows where the tool is really at.
  
  // These are the number of pulses that will be needed in each direction.
  int dx = round(fabs(x1 - x0) / stepSize);
  int dy = round(fabs(y1 - y0) / stepSize);
  int dz = round(fabs(z1 - z0) / stepSize);
  
  int totalSteps = dx + dy + dz;
  
  theSignals->numSignals = totalSteps;
  
  // Rarely (?) there might not be any move at all.
  if (totalSteps == 0)
    return;
  
  theSignals->cncSignals = new short[totalSteps];
  
  // We need to know whether each coordinate is increasing or decreasing.
  int signX = 1; if (x1 < x0) signX = -1;
  int signY = 1; if (y1 < y0) signY = -1;
  int signZ = 1; if (z1 < z0) signZ = -1;
  
  // We now how many pulses in each axis there will be overall. The question is 
  // the order in which the signals are given. If we could give fractional
  // pulses, then we would just count from 1 to totalSteps, and spit out
  // three fractional pulses, one for each axis. Instead, we keep track of how
  // many of these fractional pulses we would have liked to send out, and 
  // whichever of the three axes "needs it the most" is the one that gets a
  // whole pulse.
  
  // This is the portion of a step in each direction that should occur with each
  // step in the overall process -- the size of a fractional pulse.
  double targetRatioX = (double) dx / (double) totalSteps;
  double targetRatioY = (double) dy / (double) totalSteps;
  double targetRatioZ = (double) dz / (double) totalSteps;
  
  // These are the number of whole pulses that have actually gone out in each axis.
  int xCount = 0;
  int yCount = 0;
  int zCount = 0;
  
  for (int i = 0; i < totalSteps; i++)
    {
      // Pulse or not for each axis? If we could give out fractional pulses, then
      // we would want these pulse ratios to equal dx/totalSteps (or dy, dz) --
      // that is, the targetRatio defined above.
      // If we send out a pulse in X (say), then the ratio will become
      // (xCount+1)/(i+1); if we don't then the ratio will become xCount/(i+1),
      // etc. for the other axes. One of the axes will get a pulse; the question
      // is which one needs it the most -- would be furthest from the target ratio
      // if it doesn't get a pulse.
      //double pulseRatioX = (double) (xCount+1) / (double) (i+1);
      double noPulseRatioX = (double) (xCount) / (double) (i+1);
      //double pulseRatioY = (double) (yCount+1) / (double) (i+1);
      double noPulseRatioY = (double) (yCount) / (double) (i+1);
      //double pulseRatioZ = (double) (zCount+1) / (double) (i+1);
      double noPulseRatioZ = (double) (zCount) / (double) (i+1);
      
      
//       BUG: This is what I was doing in Java...not sure what I was thinking.
//       Get rid of this once I'm sure that what I have works.
//      // See how far each axis is from needing a pulse. Each of these is the difference
//      // between two values: the error of no pulse minus the error after we give a pulse.
//      // If any of these "need" values is positive, then that axis needs a pulse to
//      // bring the total value closer to the target.
//      double needX = Math.abs(noPulseRatioX - targetRatioX) - 
//                        Math.abs(pulseRatioX - targetRatioX); 
//      double needY = Math.abs(noPulseRatioY - targetRatioY) - 
//                        Math.abs(pulseRatioY - targetRatioY);
//      double needZ = Math.abs(noPulseRatioZ - targetRatioZ) - 
//                        Math.abs(pulseRatioZ - targetRatioZ); 
      
      
      // See which of the three axes needs it the most. This is the axis where
      // noPulseRatio is furthest from the targetRatio. Note that a particular axis
      // may be *above* the target ratio already (e.g., if it got a pulse last time), in
      // which case it *really* doesn't need one this time. Whichever one of these
      // is largest would have the greatest shortfall if it did not get a pulse.
      double needX = targetRatioX - noPulseRatioX;
      double needY = targetRatioY - noPulseRatioY;
      double needZ = targetRatioZ - noPulseRatioZ;
      
      // Give a pulse to the axis that needs it the most.
      if ((needX > needY) && (needX > needZ))
        {
          // x-axis needs it the most.
          ++xCount;
          if (signX > 0)
            theSignals->cncSignals[i] = CNCSignalArray::XPLUS;
          else
            theSignals->cncSignals[i] = CNCSignalArray::XMINUS;
        }
      else if (needY > needZ)
        {
          // y-axis needs it the most.
          ++yCount;
          if (signY > 0)
            theSignals->cncSignals[i] = CNCSignalArray::YPLUS;
          else
            theSignals->cncSignals[i] = CNCSignalArray::YMINUS;
        }
      else
        {
          // z-axis must need it the most.
          ++zCount;
          if (signZ > 0)
            theSignals->cncSignals[i] = CNCSignalArray::ZPLUS;
          else
            theSignals->cncSignals[i] = CNCSignalArray::ZMINUS;
        }
    }
  
  
  if (xCount != dx)
    qDebug() << "Send wrong number of x signals";
  if (yCount != dy)
    qDebug() << "Send wrong number of y signals";
  if (zCount != dz)
    qDebug() << "Send wrong number of z signals";
  
  // Adjust the x1/y1/z1 values to reflect what was actually done.
  // This reduces rounding errors as statements flow through the system.
  x1 = x0 + (signX * xCount * stepSize);
  y1 = y0 + (signY * yCount * stepSize);
  z1 = z0 + (signZ * zCount * stepSize);
  
}


*/
}


