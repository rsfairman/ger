package vcnc.tpile;

import vcnc.Statement;
import vcnc.tpile.lex.Lexer;

/*

Essentially a front-end to the transpiler. It allows one to transpile to
varying levels/layers.

BUG: THis is a mess because the layers are being refactored.


*/

import vcnc.tpile.parse.Parser;


public class Translator {
	
  // Each of these corresponds to a particular layer of the translator. 
  // ToLxx means "To Layer whatever."
  // BUG: Are these values worth making into a type? Adds verbosity.
  public static final int ToLxL = -4;
  public static final int ToLxP = -3;
  public static final int ToL0A = -2;
  public static final int ToL0B = -1;
  public static final int ToL00 = 0;
  public static final int ToL01 = 1;
  public static final int ToL02 = 2;
  public static final int ToL03 = 3;
  public static final int ToL04 = 4;
  public static final int ToL05 = 5;
  
	// One of the values above, set by the constructor.
	private int depth = ToLxL;
	
	// Only one of these will actually be used: whichever layer is being
	// translate to.
//	private Parser  P = null;
//	private Layer0A Lmd = null;; // md = machine directives
//	private Layer0B Lwiz = null;
//	private Layer00 L00 = null;
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
		// class is through a static method.
		this.depth = depth;
		
		CodeBuffer buf = new CodeBuffer(gCode);
		
		switch (depth)
			{
			  case ToLxL : // Lexer is a special case; nothing to allocate.
			               break;
//				case ToLxP : P = new Parser(gCode);
//				             break;
//				case ToL0A : Lmd = new Layer0A(buf);
//				             break;
//				case ToL0B : Lwiz = new Layer0B(buf);
//                     break;
//				case ToL00 : L00 = new Layer00(buf);
//									   break;
				case ToL01 : L01 = new Layer01(buf);
									   break;
				case ToL02 : L02 = new Layer02(buf);
									   break;
				case ToL03 : L03 = new Layer03(buf,0.0,0.0,0.0);
								     break;
				case ToL04 : L04 = new Layer04(buf,0.0,0.0,0.0);
									   break;
				case ToL05 : L05 = new Layer05(buf,0.0,0.0,0.0);
									   break;
				default : 
				  // This *really* shouldn't happen.
				  System.err.println("Error in the code: unknown Translator depth."); 
				  System.exit(0);
			}
	}
	
	private String nextStatement() throws Exception {
		
	  // Returns the next Statement in String form.
	  // Note that this is not used for Lexer since there are no Statements.
	  
	  // BUG: Try to get rid of exceptions
	  
		Statement cmd = null;
		
		switch (depth)
			{
//				case ToLxP : cmd = P.getStatement(); break;
//				case ToL0A : cmd = Lmd.nextStatement(); break;
//				case ToL0B : cmd = Lwiz.nextStatement(); break;
//				case ToL00 : cmd = L00.nextStatement(); break;
				case ToL01 : cmd = L01.nextStatement(); break;
				case ToL02 : cmd = L02.nextStatement(); break;
				case ToL03 : cmd = L03.nextStatement(); break;
				case ToL04 : cmd = L04.nextStatement(); break;
				case ToL05 : cmd = L05.nextStatement(); break;
				
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
    // the translation process up to the layer given to the constructor,
    // specified using one of the static final values above. The given
    // depth should be one of the constants defined above.
    // 
    // It's tempting to make this private and have the caller access through
    // a specific method, like toLexer(), toParser(), etc., but this seems
    // easier to use.
    
    if (depth == ToLxP)
      return Parser.digestAll(gcode);
    else if (depth == ToL0A)
      return Layer0A.digestAll(gcode);
    else if (depth == ToL0B)
      return Layer0B.digestAll(gcode);
    else if (depth == ToL00)
      return Layer00.digestAll(gcode);
    
    // BUG: Get rid of throwing exception?
    Translator trans = new Translator(depth,gcode);
    
    // The lexer is a special case because it doesn't involve Statment objects.
    if (trans.depth == ToLxL)
      // Special case
      return Lexer.digestAll(gcode);
    
    
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
