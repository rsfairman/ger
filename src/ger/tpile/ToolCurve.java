package ger.tpile;


import ger.tpile.Statement;
import ger.tpile.parse.StatementData;


abstract public class ToolCurve {

  // Every curve has a start point (x0,y0,z0) and an end point (x1,y1,z1).
  public double x0 = 0.0;
  public double y0 = 0.0;
  public double z0 = 0.0;
  public double x1 = 0.0;
  public double y1 = 0.0;
  public double z1 = 0.0;
  
  
  public ToolCurve() {
    // Do nothing.
  }

  // Returns a point on the curve, given a parametric value.
  // This assumes that sub-classes will implement a standard parametrization
  // that maps [0,1] --> R^3.
  public abstract CartCoord curve(double t);
  
  // Offset this curve to take cutter comp into account.
  public abstract void offset(boolean ccLeft,double radius);

  // Trim off the end of this curve, up to the parametric position given by t.
  // This assumes a standard way of parameterizing the curve, [0,1] --> R^3.
  // We are not simply chopping off this curve at p.t1 -- that's why we need
  // curve B. The tool must be allowed to move along A until it just touches
  // curve B. The point at which it touches is where this curve is cut off.
  // Both curves are assumed to have already been offset to take CC into account.
  // BUG: I think I fixed this, but the whole idea that you need to adjust
  // the position of trimming based on where the tool is firt tangent
  // to curve B is wrong. Being tangent to both curves is inherent in the fact
  // that the point, p, was obtained by intersecting *offset* curves.
  // So, I don't think I need the B arguement or the toolRadius. In fact,
  // the only thing I need is p.t1.
  public abstract void trimEnd(ParamPair p,ToolCurve B,double toolRadius);
  
  // Take this ToolCurve, and convert it to either a MoveState or a
  // CircularState. That's what the interpreter works with.
  // BUG: Do I really need this? Can't it just be part of toStatement()?
  public abstract StatementData toState();
  
  // Similar to toState(), but also fills in the type.
  public abstract void toStatement(Statement cmd);
  public abstract Statement toStatement();

  
//  public static double dist(double x1,double y1,double x2,double y2) {
//    
//    // To save typing.
//    return Math.sqrt((x1-y1)*(x1-y1) + (x2-y2)*(x2-y2));
//  }
  
//double ToolCurve::dist(double u0,double v0,double u1,double v1)
//{
//  // Static method that returns the distance between two points in the XY-plane.
//  return sqrt((u1-u0)*(u1-u0) + (v1-v0)*(v1-v0));
//}

  
  public static boolean intersectArcXYArcXY(ParamPair p,ArcCurve arcA,ArcCurve arcB) {
    
    // See intersect(). This handles the case where A and B are arcs that
    // are both in the XY-plane.
    // 
    // It turns out that this may be the hardest/messiest case.
    // If you just write out the two equations in their natural form, it
    // is a mess! If you use the two parametric forms, it's even worse
    // because of the sine and cosine. The trick seems to be to translate
    // and rotate the coordinate axes before trying to find a solution.
    // The idea is to bring yourself to a situation where the two
    // equations are given by
    // x^2 + y^2 = R^2 and (x-d)^2 + y^2 = r^2.
    // That is, the first circle is centered at (0,0) and the second
    // is centered at (d,0). It's relatively easy to find the intersection
    // of these two circles:
    // y^2 = R^2 - x^2, so that
    // (x-d)^2 + R^2 - x^2 = r^2
    // x^2 -2xd + d^2 + R^2 - x^2 = r^2
    // -2xd + d^2 + R^2 = r^2
    // -2xd = r^2 - d^2 - R^2.
    // x = (1/2d) ( R^2 + d^2 - r^2).
    // 
    // Perform (or think in terms of) a translation that brings A to
    // a position centered at (0,0), and where B "comes along." In other
    // words, the points of the two circles are the original points,
    // minus (A.cx,A.cy). B is then centered at (B.cx - A.cx,B.cy-A.cy).
    // We now rotate both circles around the origin to bring
    // B.cy-A.cy to zero. Let alpha be this angle:
    // alpha = atan2(B.cy-A.cy,B.cx-A.cx).
    //
    // Now we obtain the two (at most) points of intersecton, undo the
    // rotation, then undo the translation, and we have the point(s)
    // we care about. We still have to check whether these points
    // are on the arcs A and B (and not just on the circles).
    // 
    // Think of A as the the circle that's at (0,0).
    
    // First, we need the value of d used in the derivation above.
    // This is just the distance from (0,0) to the center of B
    // after translation -- put another way, it's the distance between the
    // centers of the two circles: (B.cx - A.cx,B.cy-A.cy).
    double d = Math.sqrt((arcB.cx - arcA.cx) * (arcB.cx - arcA.cx) +
                    (arcB.cy - arcA.cy) * (arcB.cy - arcA.cy));
    
    // This is the value x = (1/2d) ( R^2 + d^2 - r^2 ).
    // So, arcA is the circle that's been translated to the origin, the one with
    // radius equal to R (not r).
    double preX = (arcA.radius * arcA.radius + d*d - arcB.radius * arcB.radius)
        / (2.0 * d);
    
    double discriminant = arcA.radius*arcA.radius - preX*preX;
    if (discriminant < 0.0)
      // They don't touch at all.
      return false;
    
    // The value of y, before undoing the rotation and translation.
    double preY = Math.sqrt(discriminant);
    
    // So the point(s) of intersection are (preX,+/- preY) before un-rotating and
    // un-translating. Do the un-rotate. As noted, the angle of rotation was
    // alpha = atan2(B.cy-A.cy,B.cx-A.cx).
    // In fact, when we rotate to bring B to the X-axis, we rotate by -alpha,
    // so we must rotate by alpha to "unrotate." See my book, p. 53. We need
    // cos(alpha) = cos(atan2(B.cy-A.cy,B.cx-A.cx)) 
    //     = (B.cx-A.cx) / d
    // where d = distance between the two original centers, and
    // sin(alpha) = sin(atan2(B.cy-A.cy,B.cx-A.cx)) 
    //     = (B.cy-A.cy) / d,
    // You can get these two identities from by book (p. 301) or draw a picture.
    // Now, apply the rotation matrix. Remember, there are two possible points.
    double sinalpha = (arcB.cy - arcA.cy) / d;
    double cosalpha = (arcB.cx - arcA.cx) / d;
    
    double pt1x = preX * cosalpha - preY * sinalpha;
    double pt1y = preX * sinalpha + preY * cosalpha;
    
    double pt2x = preX * cosalpha + preY * sinalpha;
    double pt2y = preX * sinalpha + preY * cosalpha;
    
    // The two points of intersection, prior to un-translating are
    // (pt1x,pt1y) and (pt2x,pt2y), which may be the same point.
    // Now untranslate -- just add back (A.cx,A.cy).
    pt1x += arcA.cx;
    pt1y += arcA.cy;
    pt2x += arcA.cx;
    pt2y += arcA.cy;
    
    // We now have the point(s) of intersection. See if they are on
    // the actual arcs, not merely on the entire circle. To do this, we
    // must convert these points to angles.
    // The 0.0 values here are OK -- the z-coordinate is irrelevant in the XY-plane.
    double angleA1 = arcA.ptToAngle(pt1x,pt1y,0.0);
    double angleA2 = arcA.ptToAngle(pt2x,pt2y,0.0);
    double angleB1 = arcB.ptToAngle(pt1x,pt1y,0.0);
    double angleB2 = arcB.ptToAngle(pt2x,pt2y,0.0);
              
    // Now angleA1 and angleA2 are the angles of the two points
    // of intersection, as points on circle A. We need to know whether
    // either or both of these is in the range A.a0 to A.a1, bearing
    // in mind that the arc could be CW or CCW. Likewise for the
    // angles of the points on curveB. The point(s) must be on
    // *both* arcs to be a valid point of intersection.
    boolean pt1Valid = true;
    boolean pt2Valid = true;
    
    pt1Valid = arcA.angleOnArc(angleA1);
    pt2Valid = arcA.angleOnArc(angleA2);
    
    if (pt1Valid == true)
      pt1Valid = arcB.angleOnArc(angleB1);
    if (pt2Valid == true)
      pt2Valid = arcB.angleOnArc(angleB2);
    
    // We may be done.
    if ((pt1Valid == false) && (pt2Valid == false))
      // No intersection.
      return false;
    
    // Convert the two points to parameteric positions, assuming they
    // are on both of the arcs (are valid points of intersection).
    double paramA1 = 0.0;
    double paramA2 = 0.0;
    double paramB1 = 0.0;
    double paramB2 = 0.0;
    
    if (pt1Valid == true)
      {
        paramA1 = arcA.angleToParametric(angleA1);
        paramB1 = arcB.angleToParametric(angleB1);
      }
    if (pt2Valid == true)
      {
        paramA2 = arcA.angleToParametric(angleA2);
        paramB2 = arcB.angleToParametric(angleB2);
      }
    
    // We have the angles of the points (angleA1, angleA2, angleB1 and
    // angleB2); we only need to convert them using the parametric
    // equation a0 + (a1-a0) t. The problem (see ArcCurve.curve()) is
    // that there are four separate cases of these equation, depending
    // on whether the arc is CW or CCW and whether a1>a0 or a1<a0.
    
    // If only one of these points is valid, then we are home free.
    if ((pt1Valid == true) && (pt2Valid == false))
      {
        p.t1 = paramA1;
        p.t2 = paramB1;
        return true;
      }
    if ((pt1Valid == false) && (pt2Valid == true))
      {
        p.t1 = paramA2;
        p.t2 = paramB2;
        return true;
      }
    
    // Got here, so there are two seemingly valid points of intersection,
    // but we only want to return one of them. Choose the one that's 
    // closest to the end of curve A/start of curve B. Actually, I only check
    // which one is closest to the end of A (has the higher parametric
    // argument). One situation in which this could arise is where
    // the two points are actually the same point -- the arcs are merely
    // tangent -- but that's handled by this code too.
    if (paramA1 > paramA2)
      {
        p.t1 = paramA1;
        p.t2 = paramB1;
      }
    else
      {
        p.t1 = paramA2;
        p.t2 = paramB2;
      }
    
    // We are done with the case where A & B are both arcs!
    return true;
  }

  public static boolean intersectLineLine(ParamPair p,LineCurve lineA,LineCurve lineB) {
    
    // See intersect(). This handles the case where both curves are line segments.
    //
    // Let the two curves be parameterized by
    // x0 + (x1-x0)t, y0 + (y1-y0)t 
    // and
    // u0 + (u1-u0)s, v0 + (v1-v0)s
    // Solve for (t,s):
    // x0 + (x1-x0)t = u0 + (u1-u0)s implies that
    // t = [ 1 / (x1-x0) ] [ u0 + (u1-u0)s - x0 ]
    // Plug that into
    // y0 + (y1-y0)t = v0 + (v1-v0)s
    // and solve for s:
    // y0 + (y1-y0) [ 1 / (x1-x0) ] [ u0 + (u1-u0)s - x0 ] = v0 + (v1-v0)s.
    // Set m1 = (y1-y0)/(x1-x0). Then 
    // y0 + m1 [ u0 + (u1-u0)s - x0 ] = v0 + (v1-v0)s
    // y0 + m1 (u0-x0) + m1 (u1-u0)s = v0 + (v1-v0)s
    // y0 - v0 + m1 (u0-x0) = (v1-v0)s - m1 (u1-u0)s
    // y0 - v0 + m1 (u0-x0) = [ v1-v0 - m1 (u1-u0) ] s
    // s = [ y0 - v0 + m1 (u0-x0) ] / [ v1-v0 - m1 (u1-u0) ]
    // 
    // There are some special cases though.
    // If the denominator of the s-expression is zero, then
    // the two lines are parallel (the slopes are the same) and
    // there is no intersection.
    // If the denominator of the t-expression is zero, then
    // line A is a vertical line, and we must use a different
    // equation. In this case, A is parameterized by
    // x0 and y0 + (y1-y0)t (the x-coordinate is always x0).
    // We do the same basic thing as before, but start by using
    // the x-coordinate to solve for s (instead of t):
    // x0 = u0 + (u1-u0)s implies that
    // s = (x0-u0)/(u1-u0).
    // Plug that into
    // y0 + (y1-y0)t = v0 + (v1-v0)s
    // and solve for t:
    // y0 + (y1-y0)t = v0 + (v1-v0)(x0-u0)/(u1-u0)
    // (y1-y0)t = v0 - y0 + (v1-v0)(x0-u0)/(u1-u0)
    // t = (v0-y0)/(y1-y0) + [ (v1-v0)(x0-u0) ] / [ (u1-u0)(y1-y0) ]
    // or
    // t = [ v0 - y0 + (v1-v0)(x0-u0)/(u1-u0) ] / (y1-y0)
    // Obviously, before doing this, I should check whether
    // B is also a vertical line. Note also that if y1 = y0, then
    // line B degenerates to a point. This should have been filtered
    // out at an earlier stage of the interpreter -- moves that don't 
    // move the tool at all.
    //
    // Once you have s and t, I still need to check whether these values
    // fall in the range [0,1] since I am interested in whether the
    // two line *segments* intersect.
    //
    // The possibility of parallel lines is strange. Obviously, if they
    // are parallel and not co-incident, then there is no intersection.
    // If they are the same line, then I also report that there is no
    // intersection. I don't think this can arise anyway, given that
    // these lines came from offsetting tool paths.
    
    // First, check whether A is a vertical line.
    if (lineA.x0 == lineA.x1)
      {
        // Yes, it's vertical. See if B is also vertical.
        if (lineB.x0 == lineB.x1)
          // Yes, both vertical. As noted above, we assume that
          // they do not intersect, ignoring the possibility that
          // they are the same line.
          return false;
        
        // A is vertical, but B is not.
        double s = (lineA.x0 - lineB.x0) / (lineB.x1 - lineB.x0);
        
        double t = (lineB.y1 - lineB.y0) * (lineA.x0 - lineB.x0);
        t /= lineB.x1 - lineB.x0;
        t += lineB.y0 - lineA.y0;
        t /= lineA.y1 - lineA.y0;
        
        if ((s >= 0.0) && (s <= 1.0) && (t >= 0.0) && (t <= 1.0))
          {
            // Yes, the segments intersect.
            p.t1 = t;
            p.t2 = s;
            return true;
          }
        else
          // No intersection.
          return false;
      }
    
    // Got here, so line A is not vertical. 
    // Make sure that the two lines do not have the same slope.
    double m1 = (lineA.y1 - lineA.y0) / (lineA.x1 - lineA.x0);
    double m2 = (lineB.y1 - lineB.y0) / (lineB.x1 - lineB.x0);
    
    if (m1 == m2)
      // Yes, parallel. As above, we consider identical lines to
      // be non-intersecting.
      return false;
    
    // OK...the standard case.
    double sNumer = m1 * (lineB.x0-lineA.x0) + lineA.y0 - lineB.y0;
    double sDenom = lineB.y1 - lineB.y0 - m1 * (lineB.x1 - lineB.x0);
    double s = sNumer / sDenom;
    
    double t = lineB.x0 + (lineB.x1 - lineB.x0) * s - lineA.x0;
    t /= lineA.x1 - lineA.x0;
    
    if ((s >= 0.0) && (s <= 1.0) && (t >= 0.0) && (t <= 1.0))
      {
        // Yes, the segments intersect.
        p.t1 = t;
        p.t2 = s;
        return true;
      }
    else
      // No intersection.
      return false;
  }

  public static boolean intersectArcXYLine(ParamPair p,ArcCurve arcA,LineCurve lineB) {
    
    // See intersect(). This is messy; for the arc-to-arc case, you can
    // translate and rotate the circles to put them into a much simpler
    // form. I could do something similar here, but it really doesn't
    // simplify things much. So, we start with
    // (x-cx)^2 + (y-cy)^2 = r^2 and x = x0 + (x1-x0)t, y = y0 + (y1-y0)t
    // Subtitute the parametric equations for the line into the equation
    // for the circle, and solve for t:
    // (x0 + (x1-x0)t - cx)^2 + (y0 + (y1-y0)t - cy)^2 = r^2
    // [ (x0-cx) + (x1-x0)t ]^2 + [ (y0-cy) + (y1-y0)t ]^2 = r^2
    // Set nx = x0-cx, ny = y0-cy, dx = x1-x0, dy = y1-y0:
    // [ nx + dx t ]^2 + [ ny + dy t ]^2 = r^2
    // nx^2 + 2nx dx t + dx^2 t^2 + ny^2 + 2ny dy t + dy^2 t^2 = r^2
    // (dx^2 + dy^2) t^2 + 2(nx dx + ny dy) t + nx^2 + ny^2 - r^2 = 0
    // Apply the quadratic formula.
    
    double nx = lineB.x0 - arcA.cx;
    double ny = lineB.y0 - arcA.cy;
    double dx = lineB.x1 - lineB.x0;
    double dy = lineB.y1 - lineB.y0;
    
    double a = dx*dx + dy*dy;
    double b = 2.0 * (nx*dx + ny*dy);
    double c = nx*nx + ny*ny - arcA.radius*arcA.radius;
    
    double disc = b*b - 4.0 * a * c;
    if (disc < 0.0)
      // Term under sqrt is negative; no intersection.
      return false;
    
    double t1 = (-b + Math.sqrt(disc)) / (2.0 * a);
    double t2 = (-b - Math.sqrt(disc)) / (2.0 * a);
    
    // We have the parameterization relative to the line, and we need
    // it for the arc as well. Also, neither of these points may be on the actual
    // arc or line segment.
    boolean pt1Valid = true;
    boolean pt2Valid = true;
    
    // See if the points are on the line segment.
    if ((t1 > 1.0) || (t1 < 0.0))
      pt1Valid = false;
    if ((t2 > 1.0) || (t2 < 0.0))
      pt2Valid = false;
    
    // While we're checking whether the points are on the arc, get the
    // parametric value of the position of the point on the arc.
    double arct1 = 0.0;
    double arct2 = 0.0;
    if (pt1Valid == true)
      {
        // See if pt1 is on the arc.
        CartCoord linePt = lineB.curve(t1);
        double angle = arcA.ptToAngle(linePt.x,linePt.y,0.0);
        pt1Valid = arcA.angleOnArc(angle);
        if (pt1Valid == true)
          arct1 = arcA.angleToParametric(angle);
      }
    
    if (pt2Valid == true)
      {
        // See if pt2 is on the arc.
        CartCoord linePt = lineB.curve(t2);
        double angle = arcA.ptToAngle(linePt.x,linePt.y,0.0);
        pt2Valid = arcA.angleOnArc(angle);
        if (pt2Valid == true)
          arct2 = arcA.angleToParametric(angle);
      }
    
    if ((pt1Valid == false) && (pt2Valid == false))
      // Intersection not on the two curves.
      return false;
    
    // See if only one is valid.
    if ((pt1Valid == false) && (pt2Valid == true))
      {
        p.t1 = arct2;
        p.t2 = t2;
        return true;
      }
    else if ((pt1Valid == true) && (pt2Valid == false))
      {
        p.t1 = arct1;
        p.t2 = t1;
        return true;
      }
    
    // Got here, so they are both valid. Take the solution that is closer to 
    // the start of B -- the smaller value.
    if (t1 > t2)
      {
        p.t1 = arct2;
        p.t2 = t2;
      }
    else
      {
        p.t1 = arct1;
        p.t2 = t1;
      }
    
    return true;
  }

  public static boolean intersectLineArcXY(ParamPair p,LineCurve lineA,ArcCurve arcB) {
    
    // Just as intersectArcXYLine(), but the roles of A and B are reversed.
    // It's not worth the effort (?) to try to combine the two methods.
    
    double nx = lineA.x0 - arcB.cx;
    double ny = lineA.y0 - arcB.cy;
    double dx = lineA.x1 - lineA.x0;
    double dy = lineA.y1 - lineA.y0;
    
    double a = dx*dx + dy*dy;
    double b = 2.0 * (nx*dx + ny*dy);
    double c = nx*nx + ny*ny - arcB.radius*arcB.radius;
    
    double disc = b*b - 4.0 * a * c;
    if (disc < 0.0)
      // Term under sqrt is negative; no intersection.
      return false;
    
    double t1 = (-b + Math.sqrt(disc)) / (2.0 * a);
    double t2 = (-b - Math.sqrt(disc)) / (2.0 * a);
    
    // Check if the points are on the two curves.
    boolean pt1Valid = true;
    boolean pt2Valid = true;
    
    if ((t1 > 1.0) || (t1 < 0.0))
      pt1Valid = false;
    if ((t2 > 1.0) || (t2 < 0.0))
      pt2Valid = false;
    
    double arct1 = 0.0;
    double arct2 = 0.0;
    if (pt1Valid == true)
      {
        CartCoord linePt = lineA.curve(t1);
        double angle = arcB.ptToAngle(linePt.x,linePt.y,0.0);
        pt1Valid = arcB.angleOnArc(angle);
        if (pt1Valid == true)
          arct1 = arcB.angleToParametric(angle);
      }
    
    if (pt2Valid == true)
      {
        CartCoord linePt = lineA.curve(t2);
        double angle = arcB.ptToAngle(linePt.x,linePt.y,0.0);
        pt2Valid = arcB.angleOnArc(angle);
        if (pt2Valid == true)
          arct2 = arcB.angleToParametric(angle);
      }
    
    if ((pt1Valid == false) && (pt2Valid == false))
      // Intersection not on the two curves.
      return false;
    
    // See if only one is valid.
    if ((pt1Valid == false) && (pt2Valid == true))
      {
        p.t1 = t2;
        p.t2 = arct2;
        return true;
      }
    else if ((pt1Valid == true) && (pt2Valid == false))
      {
        p.t1 = t1;
        p.t2 = arct1;
        return true;
      }
    
    // Got here, so they are both valid. Take the solution that is closer to 
    // the end of A -- the larger value. This is different than the other version.
    if (t1 > t2)
      {
        p.t1 = t1;
        p.t2 = arct1;
      }
    else
      {
        p.t1 = t2;
        p.t2 = arct2;
      }
    
    return true;
  }

  public static boolean intersect(ParamPair p,ToolCurve A,ToolCurve B,
      double toolRadius) throws Exception {
    ;
    // This is a static method.
    
    // See below (*way* below). The comments at the beginning are not accurate.
    // I chose to do things a different way -- algebraically.
    // 
    // Find the intersection of A and B, and report the position as the
    // parametric coordinates of the points, one parametric coordinate for
    // each curve. This is done using "boxy" intersection, which is just subdividing
    // each curve into segments, finding the bounding cube for each segment, then 
    // looking for intersections. This is brute force, but easy to understand
    // and less likely to give strange behavior.
    //
    // This *could* be done algebraically, but it is hideous, particularly the
    // case where a line intersects a circle....Actually, it's not *that* bad.
    // We have a circle given by
    // (x-cx)^2 + (y-cy)^2 - r^2 = 0
    // and a line segment parameterized by
    // x = x0 + (x1-x0)t; y = y0 + (y1-y0) t.
    // Subsitute the parameterization of the line into the equation for the
    // circle, and solve for t. It's not *that* bad.
    //
    // So, it could all be done algebraically, but there are a couple of reasons that
    // I would rather not. First, it would require that I do things like test
    // the type of objects (is the ToolCurve a LineCurve or an ArcCurve), and
    // that will make the code a ugglier. Second, if I ever introduce
    // other curves, then this may become a lot harder. Also, it would lead to
    // an explosion of different cases since each pair of possible curve types
    // needs to be handled with a distinct formula. That (I suppose) is one
    // reason that Knuth uses *only* cubic curves in metapost -- there can
    // be only a single case for any calculation like this.
    //
    // You could have multiple points of intersection, and we want the one
    // that's "near" the end of curve A and the start of curve B. That's what
    // the toolRadius is for. The point that we want should be in the neighborhood
    // of toolRadius from the ends of the curves. This helps to avoid the spurious
    // intersections that may arise, say if A is an arc, and B is a line that
    // crosses the arc twice.
    //
    // Remember, A and B are curves that have been offset by the tool radius.
    // The end of A and the beginning of B were the same point before the two
    // curves were offset. Offsetting either moved the curves apart so that there
    // is no intersection, or it made them cross over each other.
    //
    // In the Java version, I tried to be clever by subdividing based on
    // the arc length of the curve. In this version, I just subdivide the parametric
    // domain [0,1], and assume that there's no radical shrinking or stretching in
    // the parameterization of the curve, [0,1] -. R^3. For the curves I'm dealing
    // with (arcs and lines), that is reasonable.
    //
    // The other simplifying assumption is that we only care about the "intersection"
    // in the XY-plane. I put intersection in quotes because this method will
    // detect an intersection if one curve crosses under the other without actually
    // touching. Put another way, I am looking for the (honest) intersection of A and 
    // B after the two curves have been projected to the XY-plane. Since cutter comp 
    // only applies in the XY-plane this is exactly what I want.
    //
    // This version uses a method much different than what I did in Java. It relies
    // on the ability to measure the distance from a point to a curve. That is,
    // given an arbitrary point in the plane, you have a method of determining the distance
    // from that point to the closest point on a curve. This has the advantage of being
    // something easy to determine algebraically for the cases I am considering here,
    // and for strange curves where you can't find the derivative, you could do
    // it by some kind of subdivision. Now, walk from the end of curve A, taking very
    // small steps at first, and the distance to B should be decreasing since you are 
    // walking closer to B. Eventually, you will cross the point of intersection and
    // the distance to B will be increasing. The gist is that you are minimizing
    // the function distanceToCurveB(t), where t is the parametrization of a position
    // on curve A. I tried this method in an earlier version of the Java code,
    // but I abandoned it for some reason. It really seems like the best idea.
    //
    // We begin by bracketing the search area. We know that the point of intersection
    // (if any) is near the end (t=1.0) of curve A. We walk back along A, taking
    // very small steps at first, until we cross over to the other side of B.
    //
    // NEVER MIND. I AM GOING TO BITE THE BULLET AND DO IT ALGEBRAICALLY.
    // The problem is that when you try to do this numerically (at least by
    // any of the methods that I have thought of), there are too many odd
    // geometric situations that can arise, and I'm never 100% confident
    // that I've thought of them all.
    // 
    // Here are a few comments about another idea that I decided not to implement...
    // this is the "boxy subdivision" idea that I used in the final Java version.
    // 
    // Getting the subdivision underway is a pain. First, we need to determine how far
    // out along each curve to look. We don't want to subdivide the entire curve
    // because one of the curves might loop back and we wouldn't find the point we
    // want, which is near the end of the curve. 
    //
    // For each curve, we to bracket the "boxy subdivision" at a point that is 
    // 1.50*toolRadius from the end-point of that curve. We want this value (1.50) to
    // be large enough to allow for some "wiggling" of the curve, but not so large that
    // we could end up with weird behavior due to the possibility that the curve bends
    // back on itself. I think (?) that the extreme case is one where the original "curve"
    // is a circle of radius zero. Then the offset curve is a circle of radius equal to
    // the tool radius.
    //
    // Finding these start points is a two stage process. First, we need to bracket
    // the search area -- find upper and lower bounds on the range of parameters in
    // which to look for the start point. Once we have the bracket, we subdivide
    // this range to find the start point.
    //
    // Note that I am starting at a point *very* close to the end-point, so that
    // I am slowing "creeping" out from the end to find the point I want. If I
    // started somewhere closer to the center of the curve, there's a risk that
    // strange things might happen -- e.g., the curve might wiggle around and this
    // point could be close to the endpoint.
    
    // AGAIN, NEVER MIND (MOST OF) THE ABOVE. I AM DOING THIS ALGEBRAICALLY.
    // So we need to know the types of the objects. There are three cases:
    // line-line intersection, circle-circle intersection and line-circle intersection.
    //
    // Remember, as of v09 I may assume that all arcs are in the XY-plane. That
    // simplifies things considerably compared to v08.
    
    if (A instanceof ArcCurve)
      {
        // Before proceeding, check that we are in XY-plane mode. If we are not,
        // then arcA can be treated more like a line segment.
        ArcCurve arcA = (ArcCurve) A;
        if (arcA.axis != AxisChoice.XY)
          // This really shouldn't happen!
          throw new Exception("Somehow arc A is not in the XY plane!");
          
        // Got here, so A is an arc in the XY-plane.
        if (B instanceof ArcCurve)
          {
            // As above, we must check that B is in the XY-plane. We are allowing
            // the user to change planes while in cutter comp mode, so this could
            // happen. 
            ArcCurve arcB = (ArcCurve) B;
            
            if (arcB.axis != AxisChoice.XY)
              // This should be impossible.
              throw new Exception("Somehow Arc B is not in the XY plane!");
            
            // They are both arcs in the XY-plane.
            return intersectArcXYArcXY(p,arcA,arcB);
          }
        else
          {
            // A = arc, B = line (and we know that A is in the XY-plane).
            arcA = (ArcCurve) A;
            LineCurve lineB = (LineCurve) B;
            return intersectArcXYLine(p,arcA,lineB);
          }
      }
    else
      {
        // A is NOT an ArcCurve; it is a LineCurve.
        
        if (B instanceof ArcCurve)
          {
            // A = line, B = arc, but B might not be in the XY-plane.
            LineCurve lineA = (LineCurve) A;
            ArcCurve arcB = (ArcCurve) B;
            
            // First check that B is in the XY-plane. 
            if (arcB.axis != AxisChoice.XY)
              // This should be impossible
              throw new Exception("Somehow arc B is not in the XY-plane!");
            
            // So A is a line and B is an arc in the XY-plane.
            return intersectLineArcXY(p,lineA,arcB);
          }
        else
          {
            LineCurve lineA = (LineCurve) A;
            LineCurve lineB = (LineCurve) B;
            
            return intersectLineLine(p,lineA,lineB);
          }
      }
  }


//double ToolCurve::findPointAtDistance(ParamPair &p,ToolCurve *B,double toolRadius)
//{
//  // Given a point on this curve, at p.t1, and a point on curve B,
//  // at p.t2, at which the two curves intersect, this finds the parameter of 
//  // the point along this curve at which the distance from the point to 
//  // B(p.t2) is equal to toolRadius.
//  //
//  // I don't see an obvious way to do this algebraically. We want to find
//  // the point along this curve -- call it A -- at which the tool is tangent
//  // to 
//  
//  
//  
//  
//  
//  // We could do this by recursive subdivision or some other numerical method.
//  // We want to find the point on A (earlier than A(p.t1)) at which the distance
//  // to the point of intersection is equal to the tool radius. This is fairly
//  // fool-proof for a numerical minimization routine.
//  //
//  // In fact, this isn't at all difficult to do algebraically. If this curve
//  // is a line, it's almost trivial
//  
//  
//  
//   //qDebug() << "not implemented";
//}



}
