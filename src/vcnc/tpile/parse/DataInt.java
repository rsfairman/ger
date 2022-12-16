package vcnc.tpile.parse;

/*

Use this when a Statement requires a single additional int value.

BUG: Don't like this. It's a "type without a type." The individual use-cases
should have their own types, even if they are functionally identical.
 

*/


public class DataInt extends StatementData {

	public int value;
	
	public DataInt(int v) {
		this.value = v;
	}
}
