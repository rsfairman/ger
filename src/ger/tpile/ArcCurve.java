package ger.tpile;

import static ger.tpile.SType.*;

/*

This is used in a few ways:
* To verify that an arc move (G02/03) is geometrically possible. For example,
  the given radius could be too small for the given end-points.
* To convert a G02/03 to a standard form that uses I/J/K instead of a radius.
* There are various methods here for calculations related to cutter comp.
* As a place for arbitrary computations about arcs.

The business about different planes (G17/18/19) can be confusing. See curve()
for how each axis choice is parameterized -- which one is sine and which one
is cosine. 

BUG: Maybe one way to untangle the issue of reference plane, and minimize tests
for each case, is a standardized method of transforming to/from the easier to
understand XY case.

BUG: This should be broken up. Parts are purely geometric, and have nothing
to do with G-code, parts are more tailored to rendering, and parts are more
tailored to G-code.

*/

import java.util.ArrayList;

import ger.tpile.Statement;
import ger.tpile.parse.DataCircular;
import ger.tpile.parse.StatementData;


public class ArcCurve extends ToolCurve {

  // As set by G17/18/19.
  public AxisChoice axis;

  // Direction of travel (CW or CCW).
  public boolean cw;
  
  public double radius;
  
  // The center of the circle, in absolute terms. This isn't necessarily the
  // geometric center; it's the center with respect to the motion of the tool.
  // Thus, if cutting a helical arc in the XY-plane, then cz is irrelevant.
  public double cx;
  public double cy;
  public double cz;
  
  // The angles of the start and end points -- a0 is the angle of (x0,y0)
  // and a1 is the angle of (x1,y1). Thus,
  // x0 = cx + radius * cos(a0),
  // y0 = cy + radius * sin(a0),
  // and so forth. If we are not in the XY-plane, then
  // the angle is defined in the obvious way, using (z,x) or (y,z).
  // Both of these values are in the range [0,2pi], so whether a particular
  // angle is "between" a0 and a1 will depend on whether the arc is
  // CW or CCW. Se angleOnArc().
  public double a0;
  public double a1;
  
  // This is used to recreate the statement from which this arc was created.
  public double feedRate;
  
  // These are used to determine the maximum extent of travel.
  // Call noteMaxMin() to set these.
  public double xMin = Double.MAX_VALUE;
  public double xMax = -Double.MAX_VALUE;
  public double yMin = Double.MAX_VALUE;
  public double yMax = -Double.MAX_VALUE;
  public double zMin = Double.MAX_VALUE;
  public double zMax = -Double.MAX_VALUE;
  

  public  ArcCurve() {
    // Do nothing.
    // BUG: Make this private to force use of the factory?
  }
  
  public static ArcCurve factory(double x0,double y0,double z0,
      AxisChoice axis,Statement cmd) throws Exception {
    ;
    // Pass the start-point of the arc as (x0,y0,z0), together with the
    // reference plane and the Statement that specifies the arc.
    //
    // NOTE: I would prefer that this thing not throw, but it does seem
    // easiest.
    //
    // BUG: Should this be a factory and not a constructor?
    if ((cmd.type != G02) && (cmd.type != G03))
      return null;

    ArcCurve answer = new ArcCurve();
    answer.axis = axis;
    if (cmd.type == G02)
      answer.cw = true;
    else
      answer.cw = false;
    
    // BUG: Change to just 'c' for variable name.
    DataCircular theState = (DataCircular) cmd.data;
    
    // From the ToolCurve super-class: the start-point.
    answer.x0 = x0;
    answer.y0 = y0;
    answer.z0 = z0;
    
    // And the end-point of the arc.
    answer.x1 = x0;
    if (theState.xDefined) answer.x1 = theState.X;
    answer.y1 = y0;
    if (theState.yDefined) answer.y1 = theState.Y;
    answer.z1 = z0;
    if (theState.zDefined) answer.z1 = theState.Z;
    
    // Determine the radius and center of the arc.
    if (theState.rDefined == true)
      {
        answer.radius = theState.R;
        
        // Determining the center is trickier. This might throw for
        // geometric reasons.
        answer.calcCenter();
        
        // Now that we know the center, make sure the radius is positive.
        // Negative radius is used to de...
        answer.radius = Math.abs(answer.radius);
      }
    else
      {
        // The center is given by I/J/K. Conceptually simpler.
        if (axis == AxisChoice.XY)
          {
            answer.cx = answer.x0;
            if (theState.iDefined) 
              answer.cx = x0 + theState.I;
            answer.cy = y0;
            if (theState.jDefined) 
              answer.cy = y0 + theState.J;
            
            // Not relevant, but note anyway.
            answer.cz = z0;
            
            // The center, as given in the code, might not be geometrically
            // possible -- the distance to each of the two end-points could
            // be slightly different. We use the larger of the two values,
            // but we only allow them to be slightly different. We have
            // to allow *some* difference since the exactly correct value
            // might be an irrational number.
            double rad0 = Math.sqrt(
                (answer.cx-x0)*(answer.cx-x0) + (answer.cy-y0)*(answer.cy-y0));
            double rad1 = Math.sqrt(
                (answer.cx-answer.x1)*(answer.cx-answer.x1) + 
                (answer.cy-answer.y1)*(answer.cy-answer.y1));
              
            // Make sure that these two radii are within 1% of each other.
            // Since I compare to the sum of the radii, I allow 2% of that.
            // I'm not sure if this is likely to be a problem. Maybe it
            // should be tighter.
            // NOTE: Ideally, when the arc is converted to cutter moves, this
            // possible (likely!) rounding error should be considered. The 
            // crucial thing is that the cutter end up at the final end-point.
            if (Math.abs(rad0-rad1) / (rad0+rad1) > 0.02)
              throw new Exception(
                  "given end-points of arc not possible for the given center");
            
            // Use the larger of the two values for the radius.
            if (rad0 > rad1)
              answer.radius = rad0;
            else
              answer.radius = rad1;
          }
        else if (axis == AxisChoice.ZX)
          {
            // Here, the y-value is the odd man out.
            answer.cx = x0;
            if (theState.iDefined) 
              answer.cx = x0 + theState.I;
            answer.cy = y0;
            answer.cz = z0;
            if (theState.kDefined) 
              answer.cz = z0 + theState.K;
            
            double rad0 = Math.sqrt(
                (answer.cx-x0)*(answer.cx-x0) + (answer.cz-z0)*(answer.cz-z0));
            double rad1 = Math.sqrt(
                (answer.cx-answer.x1)*(answer.cx-answer.x1) + 
                (answer.cz-answer.z1)*(answer.cz-answer.z1));
            if (Math.abs(rad0-rad1) / (rad0+rad1) > 0.02)
              throw new Exception(
                  "given end-points of arc not possible for the given center");
            
            if (rad0 > rad1) answer.radius = rad0; else answer.radius = rad1;
          }
        else
          {
            // Must be YZ-plane and X is the odd one out.
            answer.cx = x0;
            answer.cy = y0;
            if (theState.jDefined)
              answer.cy = y0 + theState.J;
            answer.cz = z0;
            if (theState.kDefined) 
              answer.cz = z0 + theState.K;
            
            double rad0 = Math.sqrt(
                (answer.cy-y0)*(answer.cy-y0) + (answer.cz-z0)*(answer.cz-z0));
            double rad1 = Math.sqrt(
                (answer.cy-answer.y1)*(answer.cy-answer.y1) + 
                (answer.cz-answer.z1)*(answer.cz-answer.z1));
            if (Math.abs(rad0-rad1) / (rad0+rad1) > 0.02)
              throw new Exception(
                  "given end-points of arc not possible for the given center");
            
            if (rad0 > rad1) answer.radius = rad0; else answer.radius = rad1;
          }
      }
    
    // Determine a0 and a1.
    answer.setAngles();
    
    answer.feedRate = theState.F;
    
    return answer;
  }
  
  public void calcCenter() throws Exception {
    
    // BUG: Does this need to be public?
    
    // Used in the case where the center is given implicitly from a given
    // radius -- and *not* with I/J/K. It determines the center (cx,cy,cz)
    // from the start-point, end-point, radius and orientation (cw or ccw).
    // The axis is also needed since this isn't really the center; it's only
    // the "center" as far as the G-code is concerned. For instance, in the 
    // XY-plane (G17), you might have a change in Z (for a helical cut), but 
    // the Z-coordinate is irrelevant for the notion of center intended here.
    //
    // Consider the G17 (XY-plane) case. 
    // We know that the arc passes through two points, (x0,y0) and (x1,y1).
    // Let (cx,cy) be the center; this is what we want to determine.
    // The circles is then
    // (x-cx)^2 + (y-cy)^2 = r^2
    // We have two equations, one for (x0,y0) and one for (x1,y1), and
    // the two unknowns, cx and cy. Solving this is too messy to describe
    // here. See devman.tex. The gist is that we connect the end-points
    // with a chord, take the perpendicular and that line must pass through
    // the center.
    //
    // Something confusing about this is the use of negative radius.
    // See Smid, p. 253. It's a little surprising that the geometry works
    // out as it does, but you *can* have two valid arcs with the same
    // end-points, the same radius and the correct orientation (cw or ccw).
    // One of these subtends more than 180 degrees and one subtends less.
    // Use negative radius when subtending more than 180 degrees.
    //
    // The code below is written as though we are in the XY-plane (G17).
    // Rather than type the same code three times, swap the variables
    // around to make everything "look like" G17, then unswap the variables
    // after the calculation. Conceptually, these are the cases:
    // XY (G17) -- as written
    // ZX (G18) -- x becomes z and y becomes x
    // YZ (G19) -- x becomes y and y becomes z
    switch (this.axis)
      {
        case XY : break;
          
        case ZX :
          // swap x0 and z0.
          double t = x0; x0 = z0; z0 = t;
          
          // swap x1 and z1
          t = x1; x1 = z1; z1 = t;
                  
          // Careful: these swaps are really with x since we just swapped x 
          // with z.
          // swap y0 and z0
          t = y0; y0 = z0; z0 = t;
          
          // swap y1 and z1.
          t = y1; y1 = z1; z1 = t;
          break;
          
        case YZ : 
          // swap x0 and y0.
          t = x0; x0 = y0; y0 = t;
          
          // swap x1 and y1.
          t = x1; x1 = y1; y1 = t;        
          
          // Again, be careful. Swap y0 and z0.
          t = y0; y0 = z0; z0 = t;
                  
          // Swap y1 and z1.
          t = y1; y1 = z1; z1 = t;
          break;
      }
    
    // Remember, radius may be negative:
    double r = Math.abs(radius);
    
    // As defined in devman.tex; the slope of the chord formed by the 
    // end-points is dy/dx.
    double dx = x0-x1;
    double dy = y0-y1;
    double mx = (x0+x1)/2.0;
    double my = (y0+y1)/2.0;
    
    // Distance between the end-points of the arc.
    double q = Math.sqrt(dx*dx + dy*dy);

    // The radius must be at least as large as q/2 for it to "reach."
    // NOTE: I might (?) want a little slop here in case the radius was
    // intended to be equal to q/2, but rounding error makes it a hair larger.
    if (r < q/2)
      throw new Exception("radius too small for the given end-points");
    
    // Before going any further make sure that the two end-points are 
    // different. Remember, in this method, we are trying to determine the
    // center based on the end-points and the radius, *not* using I/J/K values.
    // We can't cut a complete circle based on the radius. With only a single
    // point on the circle and the radius, there's no way to determine 
    // the center. Or, it could also happen that the end-points are simply
    // too close to one another to be sensible.
    if (q < DefaultMachine.identicallyClose)
      throw new Exception(
          "a complete circle requires an explicit center, not the radius");
    
    // Here are the two possible centers.
    double rootterm = Math.sqrt(r*r - q*q/4.0);
    double c1x = mx + (dy/q) * rootterm;
    double c1y = my - (dx/q) * rootterm;
    double c2x = mx - (dy/q) * rootterm;
    double c2y = my + (dx/q) * rootterm;
    
    // Which of these two centers is the right one depends on whether we
    // are moving CW or CCW. Use the center that gives the shorter arc 
    // (subtends the smaller angle) when the given radius is positive, and 
    // the arc that subtends more than 180 degrees when the radius is 
    // negative. That's how G-code works. See Smid, p. 253.
    // It is tempting to get rid of the use of negative radius, and require
    // that the user give I/J/K, but that would probably upset some people.
    
    // The angles subtended relative to the two possible centers:
    double subtend1 = subtend(Math.atan2(y0-c1y,x0-c1x),
        Math.atan2(y1-c1y,x1-c1x),cw);
    double subtend2 = subtend(Math.atan2(y0-c2y,x0-c2x),
        Math.atan2(y1-c2y,x1-c2x),cw);
    
    if (((subtend1 < subtend2) && (radius > 0.0)) || 
        ((subtend1 > subtend2) && (radius < 0.0)))
      {
        this.cx = c1x;
        this.cy = c1y;
      }
    else
      {
        this.cx = c2x;
        this.cy = c2y;
      }
    
    // Un-swap back to normal. Need to swap the centers too, and for the ZX 
    // and YZ cases, we need to unswap in the reverse order in which the 
    // swaps were originally done.
    switch (this.axis)
      {
        case XY : break;
        case ZX : 
          double t = y0; y0 = z0; z0 = t;
          t = y1; y1 = z1; z1 = t;
          t = x0; x0 = z0; z0 = t;
          t = x1; x1 = z1; z1 = t;
          t = cx; cx = cz; cz = t;
          t = cy; cy = cx; cx = t;
          break;
          
        case YZ : 
          t = y0; y0 = z0; z0 = t;
          t = y1; y1 = z1; z1 = t;
          t = x0; x0 = y0; y0 = t;
          t = x1; x1 = y1; y1 = t;
          t = cy; cy = cx; cx = t;
          t = cz; cz = cx; cx = t;
          break;
      }
  }
  
  public static double subtend(double a1,double a2,boolean cw) {
    
    // Return the angle subtended between a1 and a2, given in radians,
    // for traveling along an arc CW (cw == true) or CCW (cw == false). 
    // This always returns a positive number in [0,2 pi]. This is as you
    // go from the position at angle a1 to the position at angle a2.
    //
    // This is confusing. If you start at a1 and move CW to a2, with
    // a2 < a1, then you want the result to be negative. You moved through
    // a positive angle of a1-a2, but you were moving CW, which is contrary
    // the usual way angles are measured, so it should be negative.
    //
    // BUG: This should go in some kind of general geometry utility class.
    double answer = 0.0;
    if (cw == true)
      answer = 2.0 * Math.PI - (a2-a1);
    else
      answer = a2-a1;
    
    if (answer < 0.0) 
      answer += 2.0 * Math.PI;
    if (answer > 2.0 * Math.PI) 
      answer -= 2.0 * Math.PI;
    
    return answer;
  }

  public void setAngles() {
    
    // Determine a0 and a1. We have
    // x0 = cx + radius * cos(a0),
    // y0 = cy + radius * sin(a0),
    // and so forth, in the obvious way.
    // BE CAREFUL. If the two end-points are the same, then a0 and a1 will
    // be the same.
    // BUG: This (and other things) probably belong in a general "circle
    // utilities" class.
    if (axis == AxisChoice.XY)
      {
        a0 = Math.atan2(y0-cy,x0-cx);
        a1 = Math.atan2(y1-cy,x1-cx);
      }
    else if (axis == AxisChoice.ZX)
      {
        // Order is z, then x, just like it's normally x, then y.
        a0 = Math.atan2(x0-cx,z0-cz);
        a1 = Math.atan2(x1-cx,z1-cz);
      }
    else
      {
        // Axis is YZ.
        a0 = Math.atan2(z0-cz,y0-cy);
        a1 = Math.atan2(z1-cz,y1-cy);
      }
    
    if (a0 < 0.0) a0 += 2.0 * Math.PI;
    if (a1 < 0.0) a1 += 2.0 * Math.PI;
  }

  
  public boolean angleOnArc(double angle) {
    
    // Return true iff the given angle is "between" a0 and a1 so that 
    // it is on the arc. The quotes are appropriate because it's not
    // simply a matter of checking whether a0 <= angle <= a1 due to
    // issues of CW versus CCW and the fact that it's all modulo 2pi.
    // 
    // BUG: Should this be in a separate utility class?
    boolean answer = true;
    
    if (cw == false)
      {
        // This is the normal sense of rotation. Think of a1 as being
        // larger than a0.
        double tempa1 = a1;
        if (tempa1 < a0) 
          tempa1 += 2.0 * Math.PI;
        
        // The test is confusing since we want the angle to be
        // between a0 and tempa1, modulo 2pi. If the angle is between the
        // two values, as is, then we're good. By construction, if the angle 
        // is larger than a1, then it's not on the arc. If the angle is smaller
        // than a0, then the point could still be on the arc if the angle
        // plus 2pi is less than a1.
        if (angle > tempa1)
          answer = false;
        else
          {
            // if angle > a0, then we're good and answer remains true.
            if (angle < a0)
              {
                // The weird case.
                if (angle * 2.0*Math.PI > tempa1)
                  answer = false;
              }
          }
      }
    else
      {
        // The reverse sense of rotation. Think of a1 as being smaller than a0.
        // It seems (to me) most natural to make a0 larger by 2pi. This is
        // similar to the case above. We must have the angle larger than a1 and
        // smaller than a0, but the comparison is done modulo 2pi.
        double tempa0 = a0;
        if (tempa0 < a1) 
          tempa0 += 2.0*Math.PI;
        
        if (angle > tempa0)
          answer = false;
        else
          {
            if (angle < a1)
              {
                if (angle + 2.0*Math.PI > tempa0)
                  answer = false;
              }
          }
      }
    
    return answer;
  }
  
  public void noteMaxMin() {
    
    // Set the various max/min values: this.xMin, etc.
    // This needs some trivial calculus, as discussed in devman.tex.
    // Extrema are at either an end-point or where the derivative
    // of the parameterized arc is zero.
    
    // BUG: Will need another version of this to take the cutter
    // diameter into account. Or maybe that can be done by adding to the
    // values this method produces.
    
    xMin = x0;
    xMax = x0;
    yMin = y0;
    yMax = y0;
    zMin = z0;
    zMax = z0;
    
    if (x1 > xMax) xMax = x1;
    if (x1 < xMin) xMin = x1;
    if (y1 > yMax) yMax = y1;
    if (y1 < yMin) yMin = y1;
    if (z1 > zMax) zMax = z1;
    if (z1 < zMin) zMin = z1;
    
    // Now consider whether an extrema is along the arc rather than at
    // an end-point.
    if (axis == AxisChoice.XY)
      { 
        // Extreme x may be at an integer multiple of pi.
        if (angleOnArc(0.0) == true)
          {
            // Same as xtest = cx + radius * Math.cos(0.0);
            double xtest = cx + radius;
            if (xtest > xMax) xMax = xtest;
            if (xtest < xMin) xMin = xtest;
          }
        if (angleOnArc(Math.PI) == true)
          {
            // Same as xtest = cx + radius * Math.cos(Math.PI);
            double xtest = cx - radius;
            if (xtest > xMax) xMax = xtest;
            if (xtest < xMin) xMin = xtest;
          }
        
        // Extreme y may be at pi/2 or 3pi/2.
        if (angleOnArc(Math.PI/2.0) == true)
          {
            // Same as ytest = cy + radius * Math.sin(pi/2);
            double ytest = cy + radius;
            if (ytest > yMax) yMax = ytest;
            if (ytest < yMin) yMin = ytest;
          }

        if (angleOnArc(3.0*Math.PI/2.0) == true)
          {
            // Same as ytest = cy + radius * Math.sin(3 pi/2);
            double ytest = cy - radius;
            if (ytest > yMax) yMax = ytest;
            if (ytest < yMin) yMin = ytest;
          }
      }
    else if (axis == AxisChoice.ZX)
      {
        // Ignore y. The z-value uses cosine for parameterization.
        if (angleOnArc(0.0) == true)
          {
            double ztest = cz + radius;
            if (ztest > zMax) zMax = ztest;
            if (ztest < zMin) zMin = ztest;
          }
        if (angleOnArc(Math.PI) == true)
          {
            double ztest = cz - radius;
            if (ztest > zMax) zMax = ztest;
            if (ztest < zMin) zMin = ztest;
          }
        
        if (angleOnArc(Math.PI/2.0) == true)
          {
            double xtest = cx + radius;
            if (xtest > xMax) xMax = xtest;
            if (xtest < xMin) xMin = xtest;
          }

        if (angleOnArc(3.0*Math.PI/2.0) == true)
          {
            double xtest = cx - radius;
            if (xtest > xMax) xMax = xtest;
            if (xtest < xMin) xMin = xtest;
          }
      }
    else
      {
        // In the YZ-plane. Ignore x. y uses cosine.
        if (angleOnArc(0.0) == true)
          {
            double ytest = cy + radius;
            if (ytest > yMax) yMax = ytest;
            if (ytest < yMin) yMin = ytest;
          }
        if (angleOnArc(Math.PI) == true)
          {
            double ytest = cy - radius;
            if (ytest > yMax) yMax = ytest;
            if (ytest < yMin) yMin = ytest;
          }
        
        if (angleOnArc(Math.PI/2.0) == true)
          {
            double ztest = cz + radius;
            if (ztest > zMax) zMax = ztest;
            if (ztest < zMin) zMin = ztest;
          }

        if (angleOnArc(3.0*Math.PI/2.0) == true)
          {
            double ztest = cz - radius;
            if (ztest > zMax) zMax = ztest;
            if (ztest < zMin) zMin = ztest;
          }
      }
  }
  
  public ArrayList<LineCurve> toLineSegs(double err) {
    
    // Convert this to a series of LineCurve line segments, using err to
    // limit the error of the approximation. See the bit about "Arc 
    // Approximation" in devman.tex. For simplicity, this approximates the
    // error by 
    // alpha < sqrt{2 e / r}
    // where alpha is the angle subtended, e is the maximum error (the err
    // argument) and r is the radius. This is approximate because it uses
    // the Taylor series; unless err is relatively large it's quite accurate.
    ArrayList<LineCurve> answer = new ArrayList<>();
    
    // Total radians subtended by the arc, and angle subtended by each segment.
    double span = subtend(a0,a1,cw);
    double alpha = Math.sqrt(2.0 * err / radius);

//    System.out.println("span = " +span);
//    System.out.println("alpha = " +alpha);
    
    // The number of line segments. For simplicity, always round up and the 
    // error will be small enough even if you might end up with one more
    // segment than is strictly necessary. 
    int n = (int) (span / alpha) + 1;
    
//    System.out.println("n arcs = " +n);
    
    // Generate the line segements.
    CartCoord b = new CartCoord(x0,y0,z0);
    for (int i = 0; i < n; i++)
      {
        // Next point of parameterization.
        double t = (double) (i+1) / (double) n;
        
        CartCoord e = curve(t);
        
        // Feed rate (last argument) is irrelevant.
        answer.add(new LineCurve(b.x,b.y,b.z,e.x,e.y,e.z,0.0));
        
        // End end (e) becomes the beginning (b) of the line segment.
        b = e;
      }
    
    return answer;
  }
  
  public CartCoord curve(double t) {
    
    // Return the point of the curve given by the parametric equation.
    // In the natural case, the parametric equation is
    // x = cx + r cos( a0 + (a1-a0) t)
    // y = cy + r sin( a0 + (a1-a0) t)
    // The "natural case" is the one moving CCW, where a1 > a0. It helps to
    // think of four seperate cases (draw some pictures):
    // case 1 -- CCW with a1 > a0
    //   The natural case. The equation is as above.
    // case 2 -- CCW with a1 < a0
    //   The equation above is OK, but you need to add 2pi to the a1-a0 term: 
    //   a0 + (a1-a0+2pi) t
    //   The point is that you want the value to rise from a0 to an even larger
    //   number, even though sine and cosine "roll the number over" since they
    //   work modulo 2pi.
    // case 3 -- CW with a1 > a0.
    //   Adjust the equation to read
    //   a0 + (a1-a0-2pi) t
    //   Here, we want the value to fall from a0 to a1, possibly crossing the x-axis.
    //   It may help to think of this as a0 + (a1-2pi - a0) t, the idea being that
    //   you subtract 2pi from a1 to "back it up" to be "before" a0.
    // case 4 -- CW with a1 < a0
    //   The equation is fine as is, but you do have to think about it.
    //
    // In general, when thinking about these cases, try not to focus on the 
    // angle aspect. Think of how this would work if we were talking about the 
    // ordinary case of linear interpolation between two scalar values.
    //
    // At the same time, we need to move linearly in the Z-axis, which is easy.
    // Also, it all depends on the reference plane: XY, ZX or YZ.
    //
    // NOTE: this method is inverted by a combination of ptToAngle() and 
    // angleToParametric(). See also angleOnArc().
    
    CartCoord answer = new CartCoord(0,0,0);
    
    // The case where it's a complete circle is special.
    // In that case, we will have a1 == a0 because the two end-points
    // are the same. See setAngles().
    double endAngle = a1;
    double delta;
    
    // NOTE: are these *really* equal? Rounding error?
    if (a0 == a1)
      {
        // In the special case of a full circle.
        if (cw == false)
          delta = 2.0 * Math.PI;
        else
          delta = -2.0 * Math.PI;
      }
    else
      {
        // The usual case of an arc that's only a portion of a circle.
        if ((cw == false) && (a1 < a0))
          // case 2.
          endAngle += 2.0 * Math.PI;
        else if ((cw == true) && (a1 > a0))
          // case 3
          endAngle -= 2.0 * Math.PI;
        
        delta = endAngle - a0;
      }
    
    switch (axis)
      {
        case XY : answer.x = cx + radius * Math.cos(a0+ delta*t);
                  answer.y = cy + radius * Math.sin(a0 + delta*t);
                  answer.z = z0 + (z1-z0) * t;
                  break;
        case ZX : answer.x = cx + radius * Math.sin(a0 + delta*t);
                  answer.y = y0 + (y1-y0)*t;
                  answer.z = cz + radius * Math.cos(a0 + delta*t);
                  break;
        case YZ : answer.x = x0 + (x1-x0)*t;
                  answer.y = cy + radius * Math.cos(a0 + delta*t);
                  answer.z = cz + radius * Math.sin(a0 + delta*t);
                  break;
      }
    
    return answer;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  // BUG: I pulled this out of old code and I'm not so sure about the stuff
  // below. I think the constructors should all be private so that only
  // the factory() is available.
  // In fact, I'm not sure I need anything other than the factory method.
  // Also: *should* I use a factory here? Maybe a constructor would be fine.
  
  

  public ArcCurve(double x0,double y0,double z0,AxisChoice axis,Statement cmd) 
      throws Exception {
    ;
    
    this.axis = axis;
    if (cmd.type == G02)
      cw = true;
    else
      cw = false;
    
    DataCircular theState = (DataCircular) cmd.data;
    
    this.x0 = x0;
    this.y0 = y0;
    this.z0 = z0;
    
    this.x1 = x0;
    if (theState.xDefined) this.x1 = theState.X;
    this.y1 = y0;
    if (theState.yDefined) this.y1 = theState.Y;
    this.z1 = z0;
    if (theState.zDefined) this.z1 = theState.Z;
    
    // Determine the radius and center of the arc.
    if (theState.rDefined == true)
      {
        // Be careful. This could be negative. See calcCenter().
        this.radius = theState.R;
        
        // Determining the center is trickier. This might throw.
        calcCenter();
        
        // Now that we know the center, make sure the radius is positive.
        if (this.radius < 0.0) this.radius *= -1.0;
      }
    else
      {
        // The center is given by I/J/K.
        if (axis == AxisChoice.XY)
          {
            cx = x0;
            if (theState.iDefined) 
              cx = x0 + theState.I;
            cy = y0;
            if (theState.jDefined) 
              cy = y0 + theState.J;
            
            // Not relevant, but note anyway.
            cz = z0;
            
            // The center, as given in the code, might not be geometrically
            // possible -- the distance to each of the two end-points could
            // be slightly different. We use the larger of the two values,
            // but we only allow them to be slightly different.
            double rad0 = Math.sqrt((cx-x0)*(cx-x0) + (cy-y0)*(cy-y0));
            double rad1 = Math.sqrt((cx-x1)*(cx-x1) + (cy-y1)*(cy-y1));
            
            // Make sure that these two radii are within 1% of each other.
            // Since I compare to the sum of the radii, I allow 2% of that.
            if (Math.abs(rad0-rad1) / (rad0+rad1) > 0.02)
              throw new Exception(
                  "given end-points of arc not possible for the given center");
            
            // Use the larger of the two values for the radius.
            if (rad0 > rad1)
              radius = rad0;
            else
              radius = rad1;
          }
        else if (axis == AxisChoice.ZX)
          {
            // Here, the y-value is the odd man out.
            cx = x0;
            if (theState.iDefined) 
              cx = x0 + theState.I;
            cy = y0;
            cz = z0;
            if (theState.kDefined) 
              cz = z0 + theState.K;
            
            double rad0 = Math.sqrt((cx-x0)*(cx-x0) + (cz-z0)*(cz-z0));
            double rad1 = Math.sqrt((cx-x1)*(cx-x1) + (cz-z1)*(cz-z1));
            if (Math.abs(rad0-rad1) / (rad0+rad1) > 0.02)
              throw new Exception(
                  "given end-points of arc not possible for the given center");
            
            if (rad0 > rad1) radius = rad0; else radius = rad1;
          }
        else
          {
            // Must be YZ-plane and X is the odd one out.
            cx = x0;
            cy = y0;
            if (theState.jDefined)
              cy = y0 + theState.J;
            cz = z0;
            if (theState.kDefined) 
              cz = z0 + theState.K;
            
            double rad0 = Math.sqrt((cy-y0)*(cy-y0) + (cz-z0)*(cz-z0));
            double rad1 = Math.sqrt((cy-y1)*(cy-y1) + (cz-z1)*(cz-z1));
            if (Math.abs(rad0-rad1) / (rad0+rad1) > 0.02)
              throw new Exception(
                  "given end-points of arc not possible for the given center");
            
            if (rad0 > rad1) radius = rad0; else radius = rad1;
          }
      }
    
    setAngles();
    
    this.feedRate = theState.F;
  }

  public ArcCurve(double x0,double y0,double z0,double x1,double y1,double z1,
                   AxisChoice axis,boolean cw,double radius,double feedRate) 
             throws Exception {
    ;

    // As above, although slightly simpler since the radius is given.
    // Wallow negative radius.
    this.axis = axis;
    this.cw = cw;
    
    this.x0 = x0;
    this.y0 = y0;
    this.z0 = z0;
    
    this.x1 = x1;
    this.y1 = y1;
    this.z1 = z1;
    
    this.radius = radius;
    
    // This might throw, but we do not catch it here.
    calcCenter();
    if (this.radius < 0.0) this.radius *= -1.0;
    
    setAngles();
    
    this.feedRate = feedRate;
  }
  
  public double angleToParametric(double angle) {
    
    // Given an angle of a point on the arc, perhaps has given by
    // ptToAngle(), return the parametric location of the point.
    // This amounts to the inverse of curve(), below. This doesn't
    // consider whether the given angle is on the arc itself, so
    // the result may be outside the range [0,1]. It may be best
    // to call angleOnArc() to check this before calling this method.
    //
    // There are four separate cases, depending on whether the arc
    // is CW or CCW and whether a1>a0 or a1<a0. Actually, two of
    // these cases collapse into one.
    
    if (((cw == false) && (a1 > a0)) || ((cw == true) && (a1 < a0)))
      // Normal case: inverting alpha = a0 + (a1-a0) t.
      return (angle - a0) / (a1 - a0);
    else if (cw == false)
      // and a1 < a0. Inverting alpha = a0 + (a1-a0+2pi) t.
      return (angle - a0) / (a1 - a0 + 2.0*Math.PI);
    else
      // Only case left: cw = true and a1 > a0. Inverting
      // alpha = a0 + (a1-a0-2pi) t.
      return (angle - a0) / (a1 - a0 - 2.0*Math.PI);
  }

  
  public double ptToAngle(double x,double y,double z) {
    
    // Convert the given point, which should be a point on the arc
    // of this circle, to an angle. This is the same angle that
    // is used in the parameterization. In fact, the point doesn't
    // have to be on the circle; if it is not, then the value given
    // is that angle at which the point occurs, as thought the point
    // were projected to the circle along the ray from the given
    // point to the center of the circle.
    //
    // One of the three coordinates above is irrelevant to the calculation.
    // If the arc is in the XY-plane, then z is irrelevant, etc.
    //
    // This method doesn't consider whether the point is on the arc
    // itself (whether the angle is in the correct range between a0 and a1).
    // 
    // The result is in the range [0,2pi].
    double answer = 0.0;
    if (axis == AxisChoice.XY)
      answer = Math.atan2(y-cy,x-cx);
    else if (axis == AxisChoice.ZX)
      answer = Math.atan2(x-cx,z-cz);
    else
      // YZ-plane
      answer = Math.atan2(z-cz,y-cy);
    
    if (answer < 0.0)
      answer += 2.0 * Math.PI;
    
    return answer;
  }

  public void offset(boolean ccLeft,double toolRadius) {
    
    // Offset this curve to take cutter comp into account
    
    // Similar to the line case. We do care about the choice of plane
    // here, but only in a simple way.
    //double radius = 0.0;
    
    if (axis == AxisChoice.XY)
      { 
        // We have the radius of the arc before CC. Adjust it for CC.
        if (((ccLeft == true) && (cw == true)) || ((ccLeft == false) && (cw == false)))
          radius += toolRadius;
        else
          radius -= toolRadius;
        
        // Adjust the endpoints of the arc.
        x0 = cx + radius * Math.cos(a0);
        y0 = cy + radius * Math.sin(a0);
        
        x1 = cx + radius * Math.cos(a1);
        y1 = cy + radius * Math.sin(a1);
      }
    else
      {
        // I think I can treat these two cases (ZX-plane or YZ-plane) the same way.
        // This is treated a lot like a line in the XY-plane. In fact, that's
        // exactly what we do.
        LineCurve tempLine = new LineCurve(x0,y0,z0,x1,y1,z1,-1.0);
        tempLine.offset(ccLeft,toolRadius);
        
        // Copy the x/y values from tempLine to this arc, but first we
        // must offset the location of the center by a corresponding amount.
        double deltaX = tempLine.x0 - x0;
        double deltaY = tempLine.y0 - y0;
        
        cx += deltaX;
        cy += deltaY;
        
        // Now just copy over the x/y values.
        x0 = tempLine.x0;
        y0 = tempLine.y0;
        
        x1 = tempLine.x1;
        y1 = tempLine.y1;
      }
  }

  public void trimEnd(ParamPair p,ToolCurve B,double toolRadius) {
    
    // Trim off the tail end of the curve (near x1/y1/z1). We do need curve B.
    // We want to trim this curve so that it ends at p.t1, except that it should
    // end, not exactly at p.t1, but at the point along its length where the 
    // tool just touches curve B. Basically, we have to start at this curve's
    // position at p.t1, and "back up" until we are toolRadius away from curve B 
    // at its position p.t2.
    // BUG: See toolcurve.h. I don't think I *do* need B or toolRadius.
    //  double t = findPointAtDistance(p,B,toolRadius);
    
    //Point pt = curve(t);
    CartCoord pt = curve(p.t1);
    this.x1 = pt.x;
    this.y1 = pt.y;
    this.z1 = pt.z;
        
    // The need for this is why this entire method can't be defined in ToolCurve.
    setAngles();
  }

  public StatementData toState() {
      
    // This always produces a center based on I/J/K, no matter how this curve
    // was originally given (so, not using radius).
    // BUG: Can't this just be made part of toStatement()?
    DataCircular theState = new DataCircular();
    
    theState.rDefined = false;
    theState.F = this.feedRate;
    
    theState.xDefined = true;
    theState.X = x1;
    theState.yDefined = true;
    theState.Y = y1;
    theState.zDefined = true;
    theState.Z = z1;
    
    // Remember: the i/j/k values are relative, not absolute.
    if (axis == AxisChoice.XY)
      {
        theState.iDefined = true;
        theState.I = cx - x0;
        theState.jDefined = true;
        theState.J = cy - y0;
        theState.kDefined = false;
      }
    else if (axis == AxisChoice.ZX)
      {
        theState.iDefined = true;
        theState.I = cx - x0;
        theState.jDefined = false;
        theState.kDefined = true;
        theState.K = cz - z0;
      }
    else
      {
        // In YZ-plane
        theState.iDefined = false;
        theState.jDefined = true;
        theState.J = cy - y0;
        theState.kDefined = true;
        theState.K = cz - z0;
      }
    
    return theState;
  }
  
  public void toStatement(Statement cmd) {
  
    // BUG: Bogus needed by old code.
    
  }
  
  public Statement toStatement() {
  
    // Convert this object to a G02 or G03. The point is for the result to
    // be expressed using I/J/K, and *not* the radius.
    Statement answer = null;
    if (this.cw == true)
      answer = new Statement(G02);
    else
      answer = new Statement(G03);
    
    
    // BUG: Call this something else, like c for curve or a for arc.
    DataCircular theState = new DataCircular();
    
    theState.rDefined = false;
    theState.F = this.feedRate;
    
    theState.xDefined = true;
    theState.X = x1;
    theState.yDefined = true;
    theState.Y = y1;
    theState.zDefined = true;
    theState.Z = z1;
    
    // Remember: the i/j/k values are relative, not absolute.
    if (axis == AxisChoice.XY)
      {
        theState.iDefined = true;
        theState.I = cx - x0;
        theState.jDefined = true;
        theState.J = cy - y0;
        theState.kDefined = false;
      }
    else if (axis == AxisChoice.ZX)
      {
        theState.iDefined = true;
        theState.I = cx - x0;
        theState.jDefined = false;
        theState.kDefined = true;
        theState.K = cz - z0;
      }
    else
      {
        // In YZ-plane
        theState.iDefined = false;
        theState.jDefined = true;
        theState.J = cy - y0;
        theState.kDefined = true;
        theState.K = cz - z0;
      }
    
    answer.data = theState;
    return answer;
  }

  /*
  public void getSignals(CNCSignalArray *theSignals,double stepSize) 
{
  // Fill in theSignals.cncSignals and numSignals based on this line.
  // The step size is the magnitude of the move resulting from a single pulse.
  // It is assumed that cndSignals is initially NULL.
  //
  // IMPORTANT: there could be odd little rounding errors that may accumulate
  // if the step size is relatively large. For that reason, the last thing
  // this method does is reset the x1/y1/z1 values to be equal to the exact
  // postion to which the last pulse brings the tool. The caller should note
  // this position so that it knows where the tool is really at.
  //
  // The strategy here is different than what I did for lines. This case is 
  // done by walking along the arc in very small steps. When the walk
  // has moved far enough in any direction, then a signal is generated.
  // No doubt, there is a smarter way to do this....
  
  // Rather than try to use some kind of dynamic array, allocate an
  // array that's sure to be larger than what we need, then copy
  // only what we need over at the end. So allocate enough space for a full
  // circle. For a full circle in the XY-plane, we need enough signals to
  // travel the diameter twice in the x-direction and twice in the y-direction,
  // or four times the diameter. I add 10% fudge to be sure, then add the travel
  // in the z-axis.
  // BUG: In fact, I think I could figure out exactly how many elements there
  // will be in the array, but I am lazy. I would do this by looking at the
  // number of octants (quadrants?) over which the arc travels.
  int maxSigSize = 1.1 * 4.0 * 2.0 * radius / stepSize;
  if (axis == XY)
    maxSigSize += round(fabs(z1-z0)/stepSize);
  else if (axis == ZX)
    maxSigSize += round(fabs(y1-y0)/stepSize);
  else
    // YZ-plane
    maxSigSize += round(fabs(x1-x0)/stepSize);
  
  // This is the temporary holding place for the signals.
  short* sigs = new short[maxSigSize];
  int sigIndex = 0;
  
  Point P = curve(0.0);
//  int oldx =  round(P.x / stepSize);
//  int oldy =  round(P.y / stepSize);
//  int oldz =  round(P.z / stepSize);
  
  // This is a bit clever. oldx/y/z is the value as of the most recently
  // emitted pulse and prevx/y/z is the value as of the previous step
  // along the parameterized curve. We need to distinguish these two cases
  // because we want to reduce the step size if it's too big (so we need
  // to know the previous step along the parameterization), but we also
  // need to track the distance of the step since the most recent pulse
  // (so that we know when to send out a new pulse).
//  double oldx =  P.x / stepSize;
//  double oldy =  P.y / stepSize;
//  double oldz =  P.z / stepSize;

  double oldx =  P.x;
  double oldy =  P.y;
  double oldz =  P.z;
  
  double prevx = oldx;
  double prevy = oldy;
  double prevz = oldz;
  
//  qDebug() << "arc param steps:";
//  for (double t = 0; t <= 1.0; t += 0.05)
//    {
//      P = curve(t);
//      qDebug() << "\t" << t << "\t" << P.x << "\t" << P.y << "\t" << P.z;
//    }
  
  
  
  // We begin by taking steps that are only this fraction of total arc.
  // If the arc is large, then the pStep will be reduced below.
  double pStep = 0.001;
  double t = pStep;
  
  bool done = false;
  
  while (done == false)
    {
      P = curve(t);
      
//      int newx = round(P.x / stepSize);
//      int newy = round(P.y / stepSize);
//      int newz = round(P.z / stepSize);
      
      double newx = P.x;
      double newy = P.y;
      double newz = P.z;
        
      // Make sure that the step size wasn't too large.
      //if ((fabs(newx - prevx) > 0.5) || (fabs(newy - prevy) > 0.5) || (fabs(newz - prevz) > 0.5))
      if ((fabs(newx - prevx) > stepSize/2.0) || 
          (fabs(newy - prevy) > stepSize/2.0) || 
          (fabs(newz - prevz) > stepSize/2.0))
        {
          // Parametrization step size is too big. Go back and start over.
          t -= pStep;
          pStep /= 2.0;
        }
      else
        {
          // All is well, see if we moved far enough to generate a signal.
          //if ((fabs(newx-oldx) >= 1.0) || (fabs(newy-oldy) >= 1.0) || (fabs(newz-oldz) >= 1.0))
          if ((fabs(newx-oldx) >= stepSize) || (fabs(newy-oldy) >= stepSize) || (fabs(newz-oldz) >= stepSize))
            {
              if (newx - oldx >= stepSize)
                {
                  sigs[sigIndex] = CNCSignalArray::XPLUS;
                  ++sigIndex;
                  
                  // BUG: (?) Adding stepSize (which is about 0.001) repeatedly
                  // like this could lead to rounding errors. There are several ways
                  // to reduce this. I could track the total number of pulses sent out in
                  // each axis and set oldx to that number times the stepSize -- one
                  // multiplication should have less error that adding the steps
                  // individually. Another strategy would be to convert all positions
                  // to whole numbers at some earlier stage. This means that the entire
                  // interpreter layer, which goes from statements of G-code to pulses,
                  // would work in integer arithmetic rather than floating point.
                  oldx += stepSize;
                }
              else if (newx - oldx <= -stepSize)
                {
                  sigs[sigIndex] = CNCSignalArray::XMINUS;
                  ++sigIndex;
                  oldx -= stepSize;
                }
              if (newy - oldy >= stepSize)
                {
                  sigs[sigIndex] = CNCSignalArray::YPLUS;
                  ++sigIndex;
                  oldy += stepSize;
                }
              else if (newy - oldy <= -stepSize)
                {
                  sigs[sigIndex] = CNCSignalArray::YMINUS;
                  ++sigIndex;
                  oldy -= stepSize;
                }
              if (newz - oldz >= stepSize)
                {
                  sigs[sigIndex] = CNCSignalArray::ZPLUS;
                  ++sigIndex;
                  oldz += stepSize;
                }
              else if (newz - oldz <= -stepSize)
                {
                  sigs[sigIndex] = CNCSignalArray::ZMINUS;
                  ++sigIndex;
                  oldz -= stepSize;
                }
            }
          
          prevx = newx;
          prevy = newy;
          prevz = newz;
        }
      
      if (t == 1.0)
        done = true;
      
      t += pStep;
      if (t > 1.0)
        t = 1.0;
      
//      if (t > 0.01)
//      qDebug() << " t = " << t;
    }
  
  // Copy the data over to theSignals, and delete the temporary holding area.
  theSignals->cncSignals = new short[sigIndex];
  theSignals->numSignals = sigIndex;
  for (int i = 0; i < sigIndex; i++)
    theSignals->cncSignals[i] = sigs[i];
  delete sigs;
  
  // Update the x1/y1/z1 values to reflect the pulses that were actually
  // sent out. Because we kept track of the oldx/y/z values in the loop
  // above, we just use those.
  // BUG: As noted above, rounding errors could have accumulated.
  x1 = oldx;
  y1 = oldy;
  z1 = oldz;
}

*/
}
