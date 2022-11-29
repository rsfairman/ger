package vcnc.tpile;

import vcnc.tpile.parse.Circular;
import vcnc.tpile.parse.StateData;
import vcnc.tpile.parse.Statement;


public class ArcCurve extends ToolCurve {


  public AxisChoice axis;
  
  // Due to the way G-code works, this may be negative *briefly* when the
  // constructor is getting things set up. See calcCenter(), below.
  public double radius;
  
  // Direction of travel (CW or CCW).
  public boolean cw;
  
  // The center of the circle. This isn't necessarily the
  // geometric center. It's the center with respect to the motion
  // of the tool. Thus, if cutting a helical arc in the XY-plane, then
  // the z-value is irrelevant.
  public double cx;
  public double cy;
  public double cz;
  
  // The angles of the start and end points -- a0 is the angle of (x0,y0)
  // and a1 is the angle of (x1,y1). Thus,
  // x0 = cx + radius * cos(a1),
  // y0 = cy + radius * sin(a1),
  // and so forth. If we are not in the XY-plane, then
  // the angle is defined in the obvious way, using (z,x) or (y,z).
  public double a0;
  public double a1;
  
  // The only reason I need this is to properly recreate the statement from
  // which this curve was created.
  public double feedRate;
  
  
  public  ArcCurve() {
    // Do nothing
  }

  public ArcCurve(double x0,double y0,double z0,AxisChoice axis,Statement cmd) 
      throws Exception {
    ;
    
    this.axis = axis;
    if (cmd.type == Statement.G02)
      cw = true;
    else
      cw = false;
    
    Circular theState = (Circular) cmd.data;
    
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
  
  public void setAngles() {
    
    // Determine a0 and a1. We have
    // x0 = cx + radius * cos(a0),
    // y0 = cy + radius * sin(a0),
    // and so forth, in the obvious way.
    // BE CAREFUL. If the two end-points are the same, then a0 and a1 will
    // be the same.
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

  
  public double subtend(double a1,double a2,boolean cw) {
    
    // Return the angle subtended between a1 and a2, given in radians,
    // for traveling along an arc CW (cw == true) or CCW (cw == false). 
    // This always returns a positive number in [0,2 pi]. This is as you
    // go from the position at angle a1 to the position at angle a2.
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
  
  public void calcCenter() throws Exception {
    
    // Assuming that the x_i/y_i/z_i values and the radius are known (and the 
    // orientation, cw or ccw), determine the coordinates of the center. Also 
    // need the axis since this isn't really the center; it's only the "center"
    // as far as the G-code is concerned. For instance, in the XY-plane (G17), 
    // you might have a change in Z (a helical cut), but the Z-coordinate is 
    // irrelevant for the notion of center.
    //
    // There are two possible centers; which one is the right one depends on 
    // the orientation. Conceptually, you have two circles, one centered about 
    // (x0,y0) and the other about (x1,y1). They intersect at two places, and
    // these are the two possible centers. You could try to solve the problem 
    // by thinking of these two circles. The circles are given by
    // (x-x0)^2 + (y-y0)^2 = r^2 = (x-x1)^2 + (y-y1)^2   (**)
    // Now, solve for (x,y). But that is hairy. In fact, it's kind of a mystery
    // how I got the formulas below. I messed around with (**) and I think I
    // could reproduce the formulas below, but it's not easy. I think it 
    // requires some magically known substitutions. In particular, start with 
    // u = x - x1 and v = y - y1. The formulas do work in the sense that if you 
    // plug the two values I get for the center into the equation  (**), you 
    // do in fact get r^2. So they are right.
    
    // The code below is written as though we are in the XY-plane (G17).
    // Rather than type the same code three times, we swap the variables
    // around to make every thing "look like" G17, then unswap the variables
    // after the calculation. Conceptually, these are the cases:
    // XY (G17) -- as written
    // ZX (G18) -- x becomes z and y becomes x. This is strange but think
    //             of the fact that normally x comes before y. In the ZX plane,
    //             we want z to come before x.
    // YZ (G19) -- x becomes y and y becomes z.
    switch (this.axis)
      {
        case XY : 
          // Do nothing.
          break;
          
        case ZX :
          // swap x0 and z0.
          double t = x0;
          x0 = z0;
          z0 = t;
          
          // swap x1 and z1
          t = x1;
          x1 = z1;
          z1 = t;
                  
          // Careful: these swaps are really with x since we just swapped x 
          // with z.
          // swap y0 and z0
          t = y0;
          y0 = z0;
          z0 = t;
          
          // swap y1 and z1.
          t = y1;
          y1 = z1;
          z1 = t;
          break;
          
        case YZ : 
          // swap x0 and y0.
          t = x0;
          x0 = y0;
          y0 = t;
          
          // swap x1 and y1.
          t = x1;
          x1 = y1;
          y1 = t;        
          
          // Again, be careful. Swap y0 and z0.
          t = y0;
          y0 = z0;
          z0 = t;
                  
          // Swap y1 and z1.
          t = y1;
          y1 = z1;
          z1 = t;
          break;
      }
    
    // Before going any further make sure that the two end-points
    // are different. We're here, so we are trying to determine the center
    // based on the end-points and the radius. The point is that we can't
    // cut a complete circle this way. With only a single point on the
    // circle and the radius, there's no way to determine the center.
    if ((x0 == x1) && (y0 == y1))
      throw new Exception(
          "a complete circle requires an explicit center, not the radius");
    
    // Midpoint of the chord formed by the end-points of the arc.
    double midx = (x0+x1)/2.0;
    double midy = (y0+y1)/2.0;
    
    // Distance between the end-points of the arc.
    double q = Math.sqrt((x1-x0)*(x1-x0) + (y1-y0)*(y1-y0));
    
    if ((radius*radius - (q*q/4.0)) < 0.0)
      throw new Exception("radius too small for the given end-point");
    
    // Here are the two possible centers.
    // If it's a full circle and the start and end point are the same, then
    // q is zero -- a special case.
    double c1x;
    double c1y;
    double c2x;
    double c2y;
    if (q > 0.0)
      {
        c1x = midx + ((y1-y0)/q) * Math.sqrt(radius*radius - q*q/4.0);
        c1y = midy + ((x0-x1)/q) * Math.sqrt(radius*radius - q*q/4.0);
        c2x = midx - ((y1-y0)/q) * Math.sqrt(radius*radius - q*q/4.0);
        c2y = midy - ((x0-x1)/q) * Math.sqrt(radius*radius - q*q/4.0);
      }
    else
      {
        c1x = midx + radius;
        c1y = midy + radius;
        c2x = midx - radius;
        c2y = midy - radius;
      }
    
    // Which of these two centers is the right one depends on whether we
    // are moving CW or CCW. As I noted above, you can go from one end-point
    // of the arc to the other going CW or CCW using either center. This is
    // unexpected. As a machinist, it seems as though something like
    // G02 X## Y## R## is unambiguous, but it is not. Draw a picture, and 
    // you can see that it's true. I choose to use the center that gives 
    // the shorter arc (subtends the smaller angle) when the given radius
    // is positive, and the arc that subtends more than 180 degrees when the
    // radius is negative. That's how G-code works. See Smid, p. 253.
    
    // This is confusing. Think of (x2,y2) as the "reference axis"; it's like
    // the x-axis when you measure an angle normally.
    double subtend1 = subtend(Math.atan2(y0-c1y,x0-c1x),
        Math.atan2(y1-c1y,x1-c1x),cw);
    double subtend2 = subtend(Math.atan2(y0-c2y,x0-c2x),
        Math.atan2(y1-c2y,x1-c2x),cw);
    
    if (((subtend1 < subtend2) && (radius > 0.0)) || ((subtend1 > subtend2) && (radius < 0.0)))
      {
        this.cx = c1x;
        this.cy = c1y;
      }
    else
      {
        this.cx = c2x;
        this.cy = c2y;
      }
    
    // Un-swap back to normal. Need to swap the centers too.
    // For the ZX and YZ cases, we need to unswap in the reverse order in which
    // the swaps were originally done.
    switch (this.axis)
      {
        case XY : // Do nothing.
                  break;
        case ZX : 
          double t = y0;
          y0 = z0;
          z0 = t;
          
          t = y1;
          y1 = z1;
          z1 = t;
          
          t = x0;
          x0 = z0;
          z0 = t;
          
          t = x1;
          x1 = z1;
          z1 = t;
          
          t = cx;
          cx = cz;
          cz = t;
          
          t = cy;
          cy = cx;
          cx = t;
          break;
          
        case YZ : 
          t = y0;
          y0 = z0;
          z0 = t;
          
          t = y1;
          y1 = z1;
          z1 = t;
          
          t = x0;
          x0 = y0;
          y0 = t;
          
          t = x1;
          x1 = y1;
          y1 = t;
          
          t = cy;
          cy = cx;
          cx = t;
          
          t = cz;
          cz = cx;
          cx = t;
          break;
      }
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
  
  public boolean angleOnArc(double angle) {
    
    // Return true iff the given angle is "between" a0 and a1 so that 
    // it is on the arc. The quotes are appropriate because it's not
    // simply a matter of checking whether a0 <= angle <= a1 due to
    // issues of CW versus CCW and the fact that it's all modulo 2pi.
    // 
    // It is assumed that the given angle is in [0,2pi].
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

  public CartCoord curve(double t) {
    
    // Return the point of the curve given by the parametric equation.
    // In the natural case, the parametric equation is
    // x = cx + r cos( a0 + (a1-a0) t)
    // y = cy + r sin( a0 + (a1-a0) t)
    // The "natural case" is the one moving CCW, where a1 > a0. It helps to think
    // of four seperate cases (draw some pictures):
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
    // In general, when thinking about these cases, try not to focus on the angle
    // aspect. Think of how this would work if we were talking about the ordinary
    // case of linear interpolation between two scalar values.
    //
    // At the same time, we need to move linearly in the Z-axis, which is easy.
    // Also, it all depends on the reference plane: XY, ZX or YZ.
    //
    // NOTE: this method is inverted by a combination of ptToAngle() and 
    // angleToParametric(). See also angleOnArc().
    
    CartCoord answer = new CartCoord();
    
    // The case where it's a complete circle is special.
    // In that case, we will have a1 == a0 because the two end-points
    // are the same. See setAngles().
    double endAngle = a1;
    double delta;
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

  public StateData toState() {
      
    // This allocates a new object. The caller must delete it.
    // This always produces a center based on I/J/K, no matter how this curve
    // was originally given (so, no radius given).
    Circular theState = new Circular();
    
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
  
    // Modify the given statement to be of the correct type, and with the correct "guts."
    if (this.cw == true)
      cmd.type = Statement.G02;
    else
      cmd.type = Statement.G03;
    
//    if (cmd.data != NULL)
//      delete cmd.data;
    cmd.data = toState();
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
