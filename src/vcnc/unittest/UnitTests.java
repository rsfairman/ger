package vcnc.unittest;

/*

Various static methods to run the tests.

Each layer of the translator, starting with the lexer, has a method, and
each of these methods may run several tests. Each test consists of running
some G-code into the transpiler up to the given layer. The output of this
run is compared to a static reference file, with all comparisons done with
Strings.

Each of the input files is ordinary G-code. Most of them should transpile
normally -- although maybe what it would do on a physical machine is dumb --
but some are intentionally filled with errors. 

Although it's possible to automate the running of these tests, the tests
themselves must obviously be created "by hand." Each test appears in an input 
file with a name like 'lex_test_id.in'. This runs through the transpiler and 
the output is saved to 'lex_test_id.out', which is then compared to 
'lex_test_id.ref'. If the two files are different, then there may be a problem.
Sometimes, they'll be different, even though there is no error because the code
was changed -- and changed correctly. When this happens, manually copy 
'lex_test_id.out' to 'lex_test_id.ref' for future tests.

BUG: As these tests proliferate, it might make sense to put them in 
sub-directories of unit_tests, one sub-dir for each layer.

*/

import java.io.File;

import vcnc.util.FileIOUtil;

import vcnc.lex.Lexer;
import vcnc.lex.Token;

import vcnc.transpile.TextBuffer;


public class UnitTests {

  // The directory where all the test files (inputs and outputs) are stored.
  //
  // I had thought to do this relative to the classpath, but this doesn't
  // seem to work: System.getProperty("java.class.path"). It's an empty
  // string, which is weird. There are other ways to get the classpath
  // using the ClassLoader, but that seems too fiddly.
  //
  // Instead, the user.dir (aka, working directory) points to the root
  // of the entire project. This might not work if I these tests are run
  // from a jar file.
  private static String testDir = System.getProperty("user.dir") + 
      File.separator + "unit_tests";
  
  // Each test has an entry here. There's one array for each battery of tests;
  // that is, for each layer. These are the "base names," and the testing
  // code will add the appropriate suffix as necessary: '.ref', '.in' or
  // '.out'.
  private static String[] lexTests = {
      "lex_test_01"
  };
  
  
  public static void testLexer() {
    
    // Run all the lexer tests...
    for (int i = 0; i < lexTests.length; i++)
      {
        // Load an entire test file for input.
        String inString = 
            FileIOUtil.loadFileToString(testDir,lexTests[i]+".in");
        
        TextBuffer input = new TextBuffer(inString);
    
        // Run it though the lexer, sending the output to a buffer.
        StringBuffer theBuffer = new StringBuffer();
        
        Lexer theLexer = new Lexer(input);
        Token tok = theLexer.getToken();
        while (tok.letter != Token.EOF)
          {
            
            theBuffer.append(tok.lineNumber+ "\t");
            
            if (tok.letter == Token.EOL)
              theBuffer.append(";\n");
            else if (tok.letter == Token.WIZARD)
              theBuffer.append("extern\t" + tok.wizard + "\n");
            else if (tok.letter == Token.STRING)
              theBuffer.append("string\t" + tok.wizard + "\n");
            else if (tok.letter == Token.NUMBER)
              theBuffer.append("num\t" + tok.d + "\n");
            else if (tok.letter == Token.ERROR)
              theBuffer.append("error\t" + tok.error + "\n");
            else
              // Normal G-code
              theBuffer.append(tok.letter+ "\t" +tok.i+ "\t" + tok.d + "\n");
              
            tok = theLexer.getToken();
          }
        
        // Save the resulting output.
        FileIOUtil.saveStringAsAscii(
            testDir,lexTests[i] + ".out",theBuffer.toString());
        
        // Read the reference file.
        String refString = 
            FileIOUtil.loadFileToString(testDir,lexTests[i] + ".ref");
        
        // Compare the two Strings, and flag the problem, if there is one.
        if (refString.equals(theBuffer.toString()) == false)
          System.err.println("Unit test failed for " +lexTests[i]);
      }
      
  }
    
    
    

  
}
