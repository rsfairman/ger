package vcnc.tpile.parse;

// Use this when a Statement requires a single additional int value. 
//
// BUG: I'm not sure if this used anymore.


public class IntState extends StateData {

	public int value;
	
	public IntState(int v) {
		this.value = v;
	}
}
