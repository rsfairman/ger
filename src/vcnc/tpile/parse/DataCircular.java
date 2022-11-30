package vcnc.tpile.parse;

// Used with circular interpolation.

public class DataCircular extends StatementData {
	
	// Circular interpolation can be specified based on I/J/K (relative) or with 
  // X/Y/Z (absolute) or with the radius.
	public boolean rDefined = false;
	public boolean xDefined = false;
	public boolean yDefined = false;
	public boolean zDefined = false;
	public boolean iDefined = false;
	public boolean jDefined = false;
	public boolean kDefined = false;
	
	public double R = -1.0;
	public double X = 0.0;
	public double Y = 0.0;
	public double Z = 0.0;
	public double I = 0.0;
	public double J = 0.0;
	public double K = 0.0;
	
	// There may also be a feed rate. Ignore this if it is negative.
	public double F = -1.0;
}
