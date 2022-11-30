package vcnc.tpile;

/*

Essentially a front-end to the transpiler. It allows one to transpile to
varying levels/layers.


*/

import vcnc.tpile.parse.Parser;
import vcnc.tpile.parse.Statement;


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
	
	// BUG: Can I get rid of the exception here?
	
	private Translator(
	    int depth,
	    String gCode
	    //boolean inch,double scale,
			//double materialX,double materialY,double materialZ,
			//double przX,double przY,double przZ,
			//double X0,double Y0,double Z0,
			//ToolTurret turret,WorkOffsets offsets
			) throws Exception {
		;
		
		// Note that this is a private constructor. The only way to access this
		// class is through the static digestAll() method.
		// 
		// This is just like invoking the Interpreter (BUG: Interpreter no longer
		// exists? Was this the pulse generator?), but add an integer for the 
		// layer you want. depth = 0 gives the output of Layer00, etc. depth == -1
		// gives the parser and depth == 5 gives the output of Layer05, which is the
		// same as the input to the interpreter and is the final layer.
		this.depth = depth;
		
		CodeBuffer buf = new CodeBuffer(gCode);
		
		switch (depth)
			{
				case -1	: P = new Parser(buf);
									break;
				case -2 : pre = new LayerPre(buf);
				          break;
				case 0	: L00 = new Layer00(buf);
									break;
				case 1	: L01 = new Layer01(buf);
									break;
				case 2  : L02 = new Layer02(buf);
									break;
				case 3  : L03 = new Layer03(buf,0.0,0.0,0.0);
									break;
				case 4  : L04 = new Layer04(buf,0.0,0.0,0.0);
									break;
				case 5  : L05 = new Layer05(buf,0.0,0.0,0.0);
									break;
				default : 
				  // This *really* shouldn't happen.
				  System.err.println("Error in the code: unknown Translator depth."); 
				  System.exit(0);
			}
	}
	
	private String nextStatement() throws Exception {
		
	  // Returns the next Statement in String form.
	  
	  // BUG: Try to get rid of exceptions
	  
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
				
				default : 
				  System.err.println("fell through in Translator");
				  System.exit(0);
			}
		
		if (cmd == null)
			return null;
		if (cmd.type == Statement.EOF)
			return null;
		
		return cmd.toString();
	}
	
  public static String digestAll(String gcode,int depth) throws Exception {
    
    // Run all of the G-code that was provided to the constructor through
    // the translation process up to the layer given to the constructor.
    
    // BUG: Get rid of throwing exception?
    Translator trans = new Translator(depth,gcode);
    
    StringBuffer theBuffer = new StringBuffer();
    
    String smnt = trans.nextStatement();
    while (smnt != null)
      {
        theBuffer.append(smnt);
        theBuffer.append("\n");
        smnt = trans.nextStatement();
      }
    
    return theBuffer.toString();
  }
}
