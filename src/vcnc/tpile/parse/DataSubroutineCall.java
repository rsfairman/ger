package vcnc.tpile.parse;

// To specify the nature of a call to a subroutine.

public class DataSubroutineCall extends StatementData {
	
	 // Which subroutine to call. This is the program number: O[###].
	public int programNumber = 0;
	
  // Number of times to call the subroutine, from L[###].
	public int invocations = 1;
	
	// This is the character than follows the subroutine call. It's the
	// point to which the program should return; the first character that
	// follows M98 P[###] L[###].
	
	// BUG: Is this ever used??? It's set, but not read?
	
	public int returnChar = -1;
	
	// And the line number to which a subroutine call should return.
//	public int returnLine = -1;
}
