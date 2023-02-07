package vcnc.tpile.parse;

/*

Use this for O-codes (to indicate start of subprograms). 

*/


public class DataSubProg extends StatementData {

	public int progNumber = 0;
	
	
	public DataSubProg deepCopy() {
	  
	  DataSubProg answer = new DataSubProg();
	  answer.progNumber = this.progNumber;
	  return answer;
	}
}
