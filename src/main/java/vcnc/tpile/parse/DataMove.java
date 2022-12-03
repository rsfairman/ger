package vcnc.tpile.parse;

/*

Use this for a move statement. This is also used for I,J,K settings, where
X, Y and Z become I, J and K.
NOTE the presence of the F-value here for feed rate.

*/

public class DataMove extends StatementData {
	
	public boolean xDefined = false;
	public boolean yDefined = false;
	public boolean zDefined = false;
	public boolean fDefined = false;
	
	public double xValue = 0.0;
	public double yValue = 0.0;
	public double zValue = 0.0;
	public double fValue = 0.0;
	
	public String toString() {
		return xValue+ ", " +yValue+ ", " +zValue+ ", " +fValue;
	}
	
	public DataMove() {
		// Do nothing.
	}
	
	public DataMove(boolean xd,boolean yd,boolean zd,boolean fd,
			double x,double y,double z,double f) {
		
		this.xDefined = xd;
		this.yDefined = yd;
		this.zDefined = zd;
		this.fDefined = fd;
		this.xValue = x;
		this.yValue = y;
		this.zValue = z;
		this.fValue = f;
	}
}
