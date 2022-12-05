package vcnc.workoffsets;

/*

Holds the work offsets associated with G55, G56, etc.
These values are given relative to the PRZ. Since this is an imaginary machine,
this choice is arbitrary.

BUG: This doesn't belong in this package, and the other things belong in
some vcnc.ui package.

*/

public class WorkOffsets {
	
	// These are for G55, G56, G57, G58 and G59. So, the three offsets for G55
	// are at offset[0], and the three for G59 are at offset[4].
	public double[][] offset = new double[5][3];
}
