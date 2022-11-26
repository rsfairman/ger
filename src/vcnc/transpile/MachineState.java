package vcnc.transpile;

/*

Certain "Global" settings for the machine. Some of these are more fixed, like
the work offsets table, and some change, like whether in absolute or 
incremental mode. Also, some of these settings are only relevant to certain
layers. For example, at some point (Layer01?), all units that appear in
the G-code are converted to the machine units (inches or mm) so that
curInch is no longer relevant. Likewise, Layer04 converts everything to
absolute coordinates, making the absolute field irrelevant.

*/

public class MachineState {
  
  // Whether the machine works internally with inches (true) or mm (false).
  public static boolean machineInchUnits = true;
  
  // Whether the machine is *currently* working inches (true) or mm (false).
  public static boolean curInch = true;
  
  // Whether currently in polar coordinates mode (G15/16).
  public static boolean usingPolar = false;
  
  // Which plane is current with G17/18/19.
  public static AxisChoice curAxis = AxisChoice.XY;

  // Whether currently in absolute mode (true) or incremental mode (false).
  public static boolean absolute = true;

  // If there is any kind of change to the reference frame, then this is where
  // it is noted. When the machine starts, these are all zero. The entries in 
  // WorkOffsets or a value given to G52 could have an effect here. Any 
  // coordinate that occurs in a program after G52 or G54...G59 should be 
  // interpreted relative to this new reference frame. 
  //
  // Thus, to go to machine coordinates, take the coordinate given by the user
  // and *subtract* the offset value. TLO mode is similar, except that only the 
  // offsetZ value should be non-zero. It's a bit more complicated than that 
  // though. The user might invoke G52 (say), then TLO, then cancel TLO. So, 
  // the TLO must be kept seperately. The offsetZ value will have the tloValue
  // included if the machine is in TLO mode.
  public static double offsetX = 0.0;
  public static double offsetY = 0.0;
  public static double offsetZ = 0.0;
  
  // The tool turret. This is fixed throughout the life of each G-code
  // script, after any initial adjustements before the O-number statement.
  public static ToolTurret turret = null;
  
  // Whether TLO is being applied and the amount. Positive means that the tool
  // moves upward while in TLO mode. TLO mode can be invoked in either a 
  // positive or a negative sense, using either G43 or G44, but noting that 
  // seperately is not necessary. The sign of tloValue is enough.
  public static boolean TLO = false;
  public static double tloValue = 0.0;

  // Whether cutter comp is currently in force.
  public static boolean cutterComp = false;
  
  // Tool positions, in absolute coordinates (inch or mm, according to 
  // machineInch). This is updated after every Statement (if the tool moves).
  // BUG: Not using, but I suspect that I could replace the passing around
  // of X0/Y0/Z0 between the contructors of the various layers this way.
  // It comes down to the assumption that every program *always* starts with
  // the tool at (0,0,0).
//  double toolX;
//  double toolY;
//  double toolZ;
}



