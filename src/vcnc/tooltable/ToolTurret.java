package vcnc.tooltable;

// This holds a record of the contents of a tool turret.

// BUG: This is (mostly) the old java version. Use the version ported from 
// C++, found in vcnc.tpile.ToolTurret. I've set things up to compile
// using this version, but the C++ stuff should be migrated to here 


public class ToolTurret {
	
  // BUG: Added to make it compile...not right.
  // Do I want this to be fixed? The constructor is not that way.
  public static int TableSize = 20;
  
	public Tool[] theTools = null;

  // Register values.
	// BUG: Added to make it compile...not right. static ???
  public static double[] hRegister = new double[TableSize];
  public static double[] toolDiameter = new double[TableSize];
  
	
	public ToolTurret(int size) {
	  size = TableSize;
		this.theTools = new Tool[size];
		for (int i = 0; i < size; i++)
		  theTools[i] = new Tool();
	}
	

	public ToolTurret convertTurret(double scale) {
		
		// Convert this ToolTurret object, whose values are given in inches or
		// mm, to the same thing, but where the values are in machine steps; e.g., 
		// convert 0.250 inches to 250 steps.
		ToolTurret answer = new ToolTurret(this.theTools.length);
		
//		for (int i = 0; i < answer.theTools.length; i++)
//			System.out.println(i+ " has D = " +this.theTools[i].D+ " and diam = " +this.theTools[i].diameter);
		
		
		for (int i = 0; i < answer.theTools.length; i++)
			answer.theTools[i] = this.theTools[i].convertTool(scale);
		
		return answer;
	}
	
	public double getLargest() {
		
		// Return the diameter of the largest tool.
		double answer = Double.MIN_VALUE;
		
		for (int i = 0; i < theTools.length; i++)
			{
				if (theTools[i].diameter > answer)
					answer = theTools[i].diameter;
			}
		
		return answer;
	}
}
