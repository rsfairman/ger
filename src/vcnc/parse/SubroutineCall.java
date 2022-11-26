package vcnc.parse;

// To specify the nature of a call to a subroutine.

public class SubroutineCall extends StateData {
	
	 // Which subroutine to call. This is the program number: O[###].
	public int programNumber = 0;
	
  // Number of times to call the subroutine, from L[###].
	public int invocations = 1;
	
	// This is the character than follows the subroutine call. It's the
	// point to which the program should return; the first character that
	// follows M98 P[###] L[###].
	public int returnIndex = -1;
}
