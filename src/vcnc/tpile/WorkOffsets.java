package vcnc.tpile;

/*

Holds the values to be used with G55-G59. This is just a set of XYZ values, one
for each G-code. The values are set by a menu choice from the main window.

*/

public class WorkOffsets {
  
  // Each of these has three values: x, y and z.
  public static double[] g55 = new double[3];
  public static double[] g56 = new double[3];
  public static double[] g57 = new double[3];
  public static double[] g58 = new double[3];
  public static double[] g59 = new double[3];
}
