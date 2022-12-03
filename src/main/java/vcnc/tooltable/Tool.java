package vcnc.tooltable;

// To hold the definition for a single tool. For the most part, this does very
// little other than serve as a base class for different types of tool.
//
// My hope is that I can define all of the methods that I will need for *any*
// tool here and let this class be extended/overridden in particular cases.


public class Tool {
	
	// For an abstract machine this is often irrelevant.
	public double length = 1.0;
	
	// For oddly shaped tools this won't tell the complete story. It's only a defining
	// feature for endmills and ballmills, but even for something like an
	// ogee bit, letting this be the diameter of a bounding cylinder makes sense.
	public double diameter = 0.250;
	
	// These are the values for D and H that appear in the tool table.
	public double D = 0.250;
	public double H = 0.000;
	
	public Tool convertTool(double scale) {
		
		// To convert the values defining a tool from inches or mm to machine
		// steps.
		Tool answer = new Tool();
		
		answer.length = this.length * scale;
		answer.diameter = this.diameter * scale;
		answer.D = this.D * scale;
		answer.H = this.H * scale;
		
		return answer;
	}
	
}
