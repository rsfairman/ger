package vcnc.unittest;

/*

Various static methods to run the tests.

Each layer of the translator, starting with the lexer, has list of input
test files below. Each test consists of running some G-code through the
transpiler up to the given layer. The output of this run is compared to a
static reference file, with all comparisons done with Strings.

Each of the input files is ordinary G-code. Most of them should transpile
normally -- although maybe what it would do on a physical machine is dumb --
but some are intentionally filled with errors. 

Although it's possible to automate the running of these tests, the tests
themselves must obviously be created by hand. Each test appears in an input 
file with a name like 'lex_test_id.in'. This runs through the transpiler and 
the output is saved to 'lex_test_id.out', which is then compared to 
'lex_test_id.ref'. If the two files are different, then there may be a problem.
Sometimes, they'll be different, even though there is no error because the code
was changed -- and changed correctly. When this happens, manually copy 
'lex_test_id.out' to 'lex_test_id.ref' for future tests.

BUG: As these tests proliferate, it might make sense to put them in 
sub-directories of unit_tests.

*/

import java.io.File;

import vcnc.util.FileIOUtil;

import vcnc.tpile.Translator;


public class UnitTests {

  // The directory where all the test files (inputs and outputs) are stored.
  //
  // I had thought to do this relative to the classpath, but this doesn't
  // seem to work: System.getProperty("java.class.path"). It's an empty
  // string, which is weird. There are other ways to get the classpath
  // using the ClassLoader, but that seems too fiddly.
  //
  // BUG: This is set up to make it easy to run these tests more easily from
  // Eclipse if I want. The default here is where the file are ordinarily
  // stored, but you can change with setDir(), below.
  // Obviously, none of this will be in the production version anyway.
  private static String testDir = System.getProperty("user.dir") + 
      File.separator + "unit_tests";
  
  // Each test has an entry here. There's one array for each battery of tests;
  // that is, for each layer. These are the "base names," and the testing
  // code will add the appropriate suffix as necessary: '.ref', '.in' or
  // '.out'.
  private static String[] lexTests = {
      "lex_test_01"
  };
  
  // And the same idea for the other layers. The idea of "layers" is sort of
  // bogus as of v11, but it makes some sense when testing. These layers
  // correspond roughly to the degree of translation specified by
  // Translator.ThruParser, .ThruSubProgs, etc.
    
  // For the ThruParser stage.
  private static String[] parseTests = {
       "parse_test_01"
      ,"parse_test_02" 
      ,"parse_test_03" 
  };

  // For the ThruDirectives stage.
  private static String[] directiveTests = {
      "directive_test_01"
  };
  
  // For the ThruSubProgs stage.
  private static String[] subprogTests = {
       "subprog_test_01"
      ,"subprog_test_02"
      ,"subprog_test_03"
      ,"subprog_test_04"
      ,"subprog_test_05"
      ,"subprog_test_06"
      ,"subprog_test_07"
      ,"subprog_test_08"
      ,"subprog_test_09"
      ,"subprog_test_10"
  };

  // This is for the ThruUnits stage. Yes, the name is unfortunate, given that 
  // it appears in the code for unit tests.
  // Several things happen in this layer: choosing a reference plane
  // (G17/18/19), inch/mm (G20/21), polar coords (G15/16) and a check
  // for correct syntax and geometry for G02/03. While we're at it, throw
  // in some tests for skippable stuff, like M02 and M41 since that happens
  // in this stage too.
  private static String[] unitsTests = {
      "units_test_01"
     ,"units_test_02"
  };
  
  // For the wizard layer
  private static String[] wizardTests = {
      "wizard_test_01"
//     ,"wizard_test_02"
  };
  
  
  
  public static void setDir(String dir) {
    testDir = dir;
  }


  public static void runTests(String[] infiles,int layer) {
    
    // Common code to run the various tests. The infiles are the names
    // of the files to run, and layer is one of Translator.ThruLexer, etc.

    for (int i = 0; i < infiles.length; i++)
      {
        String inString = 
            FileIOUtil.loadFileToString(testDir,infiles[i]+".in");

        try {
          
          if (inString == null)
            {
              System.err.println("Skipping " +infiles[i]+
                  " because it does not exist!");
              continue;
            }
        
          System.out.println("Testing " + infiles[i]);
          String outString = Translator.digest(inString,layer);

          FileIOUtil.saveStringAsAscii(
              testDir,infiles[i] + ".out",outString);
          
          String refString = 
              FileIOUtil.loadFileToString(testDir,infiles[i] + ".ref");
          
          if (refString == null)
            System.err.println("Failure for " +infiles[i]+ 
                " because .ref file does not exist.");
          else if (refString.equals(outString) == false)
            System.err.println("Unit test failed for " +infiles[i]);
          
        } catch (Exception e) {
          System.err.println(
              "Parsing exception with: " +infiles[i]+ ": "+ e.getMessage());
          e.printStackTrace();
        }
      }
  }
  
  
  public static void testAll() {

    System.out.println("Running unit tests...");

    runTests(lexTests,Translator.ThruLexer);
    runTests(parseTests,Translator.ThruParser);
    runTests(directiveTests,Translator.ThruDirectives);
    runTests(subprogTests,Translator.ThruSubProgs);
    runTests(unitsTests,Translator.ThruUnits);
    runTests(wizardTests,Translator.ThruWizards);
    
    System.out.println("Unit tests complete.");
  }

  
}
