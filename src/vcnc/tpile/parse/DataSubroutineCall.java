package vcnc.tpile.parse;

// To specify the nature of a call to a subroutine.

public class DataSubroutineCall extends StatementData {
	
	 // Which subroutine to call. This is the program number: O[###].
	public int programNumber = 0;
	
  // Number of times to call the subroutine, from L[###].
	public int invocations = 1;
	
	
	public DataSubroutineCall deepCopy() {
	  
	  DataSubroutineCall answer = new DataSubroutineCall();
	  answer.programNumber = this.programNumber;
	  answer.invocations = this.invocations;
	  return answer;
	}
}
