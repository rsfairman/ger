% Various metapost macros I've come up with. Say
% input mymacros.mp
% before you say beginfig
%
% I modified this a bit from the version that appears with the astronomy
% book. I got rid of the 3d stuff... Actually, I don't think that the
% 3d2.mp file was used anyway.
%input "../3d/3d2.mp";

%
%
% I looks (?) like all variables declared in macros are global by
% default. I think that to use local variables in my macros,
% I need to bracket every macro with begingroup; and endgroup;
% Then all variable names used in the macro must first be declared with
% save name; This isn't really "declaring" the variable; it's just
% saying that the name should be considered to be local. You still need
% to say numeric name; or whatever.

def draw_angle(expr A,O,B,d) =
  % This draws an angle arc at distance d from O.
  draw (O + d*unitvector(A-O))
    { d*unitvector(A-O) rotated 90 }
    ..
    { d*unitvector(B-O) rotated 90 }
    (O + d*unitvector(B-O));
enddef;

vardef get_draw_angle(expr A,O,B,d) =
  
  % As above, but it returns the path so that it can be tweaked
  % before drawing it.
  (O + d*unitvector(A-O))
    { d*unitvector(A-O) rotated 90 }
    ..
    { d*unitvector(B-O) rotated 90 }
    (O + d*unitvector(B-O))
enddef;


vardef ellipse_part_path(expr startAngle,endAngle) =
  %
  % NOTE: because this returns a path, it must be defined as a vardef,
  % not a def (though not sure exactly why) and the last statement
  % does not have a semicolon.
  %
  % Creates and returns, as a path, a part of a circle. Specify the
  % start and end angle, in degrees. The center is always at origin (0,0).
  % Because this is a path, you can shift and rotate it to do things like
  % get a part of an ellipise.
  %
  % For example
  % thePath := ellipise_part_path(30,120);
  % draw thePath xscaled 20 yscaled 12 rotated 40;
  % will draw an ellipse with major axis 20, minor axis 12, rotated 40
  % degrees, and the part drawn will extend from 30 to 120 degrees from the
  % major axis.
  %
  % This is done very simply. If you have a series of points along an arc,
  % and you connect them with "..", then the bezier curve will be very close
  % to the given arc provided that you have enough points.
  % I just use 12 points every time. That's really more than enough for
  % small arcs, but that's OK.
  path ellipsepath;
  ellipsepath := (cosd startAngle,sind startAngle)
  for i=1 upto 11:
    .. (cosd (startAngle + (i /11) * (endAngle-startAngle)),
      sind (startAngle + (i/11) * (endAngle-startAngle)))
  endfor;
      
  ellipsepath
enddef;

def path_arc_midpoint(expr p) =

  % Returns the point that's the mid-point of the path, p.
  % This is done by arc-length, not time.
  point (arctime arclength(p)/2 of p) of p

enddef;

def path_trimmed(expr p,f) =

  % Given path p, return the subpath that makes up fraction f of the
  % path, measured by arc-length. Should have 0 < f < 1.
  begingroup;
    save alen;
    numeric alen;
    alen = arclength(p);

    save endtime;
    numeric endtime;
    endtime = arctime (f * alen) of p;
    
    (subpath (0,endtime) of p)
  endgroup;
enddef;

% I can't see exactly how the rotation macros in 3d.mp work, so I've defined
% my own rotation macros.
% Think of the page as being the (x,z) plane (x increases right and z
% increases up), with the y value increasing as you move away to the
% other side of the page.

def vec_rot_X(expr i,alpha) =
  
  % Rotate v about the x-axis by alpha degrees. i should be an integer so
  % that vec[i] is defined.
  save tempy;
  save tempz;
  tempy = vec[i]y;
  tempz = vec[i]z;
  vec[i]y := tempy * cosd(alpha) - tempz * sind(alpha);
  vec[i]z := tempy * sind(alpha) + tempz * cosd(alpha);
enddef;

def vec_rot_Y(expr i,alpha) =
  
  % Rotate v about the y-axis by alpha degrees.
  save tempx;
  save tempz;
  tempx = vec[i]x;
  tempz = vec[i]z;
  vec[i]x := tempx * cosd(alpha) + tempz * sind(alpha);
  vec[i]z := -tempx * sind(alpha) + tempz * cosd(alpha);
enddef;

def vec_rot_Z(expr i,alpha) =
  
  % Rotate v about the z-axis by alpha degrees.
  save tempx;
  save tempy;
  tempx = vec[i]x;
  tempy = vec[i]y;
  vec[i]x := tempx * cosd(alpha) - tempy * sind(alpha);
  vec[i]y := tempx * sind(alpha) + tempy * cosd(alpha);
enddef;