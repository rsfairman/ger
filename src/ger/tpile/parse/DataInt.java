package ger.tpile.parse;

/*

Use this when a Statement requires a single additional int value.

NOTE: Don't like this because it's a "type without a type." The individual 
use-cases should have their own types, even if they are functionally identical.
Of course, that's a lot of messing around for pedantic reasons.

*/


public class DataInt extends StatementData {

	public int value;
	
	public DataInt(int v) {
		this.value = v;
	}

  public DataInt deepCopy() {
    
    DataInt answer = new DataInt(this.value);
    return answer;
  }
}
