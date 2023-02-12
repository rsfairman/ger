package vcnc.workoffsets;

/*

Holds the work offsets associated with G54, G56, etc.
These values are given relative to machine zero.

BUG: This doesn't belong in this package, and the other things belong in
some vcnc.ui package.

*/

public class WorkOffsets {
	
  public final static int Rows = 6;
  public final static int Cols = 3;
  
	// These are for G54, G55, G56, G57, G58 and G59. So, the three offsets 
  // for G54 are at offset[0], and the three for G59 are at offset[5].
  // It's tempting to make this static, but we do need to make a copy
  // while the user has the option of modifying it.
	public double[][] offset = new double[Rows][Cols];
	
	
	public WorkOffsets() {
	  
	  // Java should fill this in with zeros, but make sure.
	  for (int i = 0; i < Rows; i++)
      {
        for (int j = 0; j < Cols; j++)
          offset[i][j] = 0.0;
      }
	}
	
	private static void doCopy(WorkOffsets src,WorkOffsets dest) {
	  
    for (int i = 0; i < Rows; i++)
      {
        for (int j = 0; j < Cols; j++)
          dest.offset[i][j] = src.offset[i][j];
      }
	}
	
	public WorkOffsets deepCopy() {
	  
	  WorkOffsets answer = new WorkOffsets();
	  doCopy(this,answer);
	  return answer;
	}
	
	public void takeCopy(WorkOffsets src) {
	  
	  // Copy from src to this.
	  doCopy(src,this);
	}
}
