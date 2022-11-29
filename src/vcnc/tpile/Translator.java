package vcnc.tpile;

import vcnc.tpile.parse.Parser;
import vcnc.tpile.parse.Statement;

/*

Essentially a front-end to the transpiler. It allows one to transpile to
varying levels/layers.

*/

//import tooltable.ToolTurret;
//import ui.TextBuffer;
//import workoffsets.WorkOffsets;
//import parser.Statement;
//import parser.Parser;


public class Translator {
	
	// Set by the constructor. It indicates which of the layers below we are to
	// consider.
	private int depth = -2;
	
	private Parser  P = null;
	private LayerPre pre = null;
	private Layer00 L00 = null;
	private Layer01 L01 = null;
	private Layer02 L02 = null;
	private Layer03 L03 = null;
	private Layer04 L04 = null;
	private Layer05 L05 = null;
	
	public Translator(
	    int depth,
	    TextBuffer theText
	    //boolean inch,double scale,
			//double materialX,double materialY,double materialZ,
			//double przX,double przY,double przZ,
			//double X0,double Y0,double Z0,
			//ToolTurret turret,WorkOffsets offsets
			) throws Exception {
		;
		// This is just like invoking the Interpreter, but add an interger for the 
		// layer you want. depth = 0 gives the output of Layer00, etc. depth == -1
		// gives the parser and depth == 5 gives the output of Layer05, which is the
		// same as the input to the interpreter and is the final layer.
		this.depth = depth;
		
		switch (depth)
			{
				case -1	: P = new Parser(theText);
									break;
				case -2 : pre = new LayerPre(theText);
				          break;
				case 0	: L00 = new Layer00(theText);
									break;
				case 1	: L01 = new Layer01(theText);
									break;
				case 2  : L02 = new Layer02(theText);
									break;
				case 3  : L03 = new Layer03(theText,0.0,0.0,0.0);
									break;
				case 4  : L04 = new Layer04(theText,0.0,0.0,0.0);
									break;
				case 5  : L05 = new Layer05(theText,0.0,0.0,0.0);
									break;
				default : 
					throw new Exception("Unknown interpreter depth requested");
			}
	}
	
	public String nextStatement() throws Exception {
		
		Statement cmd = null;
		
		switch (depth)
			{
				case -1 : cmd = P.getStatement(); break;
				case -2 : cmd = pre.nextStatement(); break;
				case 0	:	cmd = L00.nextStatement(); break;
				case 1	:	cmd = L01.nextStatement(); break;
				case 2	: cmd = L02.nextStatement(); break;
				case 3	: cmd = L03.nextStatement(); break;
				case 4	: cmd = L04.nextStatement(); break;
				case 5	: cmd = L05.nextStatement(); break;
				
				// BUG: Shouldn't happen
				default : System.err.println("fell through in Translator");
			}
		
		if (cmd == null)
			return null;
		if (cmd.type == Statement.EOF)
			return null;
		
		return cmd.toString();
	}
}
