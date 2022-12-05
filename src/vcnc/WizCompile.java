package vcnc;

/*

Static methods for compiling wizard code.

Compiling things at run-time is awkward, on several counts. The actual
compilation is not that hard, provided it's a fairly simple case.
But providing a decent UI to the user for the edit-debug-run cycle is not
really reasonable to do here (this isn't an IDE), so the user is
expected to compile via the CLI.

The basic idea is taken from old FigPut code (version 17, when it still
used Java). See also
http://www.java2s.com/Tutorials/Java/Java_Utilities/How_to_compile_Java_source_code_and_run_it_dynamically.htm
and
https://stackoverflow.com/questions/21544446/how-do-you-dynamically-compile-and-load-external-java-classes
and
https://stackoverflow.com/questions/12173294/compile-code-fully-in-memory-with-javax-tools-javacompiler?noredirect=1&lq=1

Other approaches are JANINO and the Apache JCI.

*/

import java.io.File;

import java.net.URL;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import vcnc.persist.Persist;


public class WizCompile {
  
  public static boolean compile(String wName) {
    
    // Compile a wizard with the given name. It is assumed that the
    // relevant .java file is found in the .ger directory.
    // 
    // Return true on success.
    
//    System.err.println("attempting compile");
    
    // The path to the .java file (we hope).
    File sourceFile = new File(Persist.getGerLocation() +File.separator
        +wName+ ".java");
    
    if (sourceFile.exists() == false)
      {
        System.err.println("Unable to find " +sourceFile.toString());
        return false;
      }
    
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    
    // What's done below is fairly uncomplicated. Another way to do this is
    // by calling compiler.getTask(). That would be done something like the
    // following -- although I have not debugged this.
    //
    //    StandardJavaFileManager fileManager = 
    //        compiler.getStandardFileManager(null,null,null);
    //    
    //    fileManager.setLocation(StandardLocation.CLASS_PATH,
    //        Arrays.asList(new File(
    //            "C:\\Users\\rsf\\Documents\\Work Area\\vcnc\\bin")));
    //
    //    List<String> optionList = new ArrayList<String>();
    //    optionList.add("-classpath");
    //    optionList.add(
    //        "C:\\Users\\rsf\\Documents\\Work Area\\vcnc\\bin");
    //    answer = compiler.getTask(null,fileManager,null,optionList,null,
    //        fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile)))
    //        .call();
    //    fileManager.close();
    
    // Instead of the above, what follows is much simpler.
    
    // In production this will be running from a JAR file, but not while
    // under development. We need to determine the classpath (JAR file or
    // directory).
    // BUG: Hard-coded class name.
    // BUG: This entire approach of parsing the url of Main.class seems brittle.
    String url = ClassLoader.getSystemResource("vcnc/Main.class").toString();
    
    // The url will start with either 'file:' or 'jar:', depending on the
    // source from which the program was launched.
//    System.out.println("the url: " +url);
    
    String cp = null;
    if (url.startsWith("file") == true)
      // The class path is the url String, without the 'file:/' (6 characters)
      // at the front and without '/vcnc/Main.class' (16 characters) at the end.
      cp = url.substring(6,url.length() - 16);
    else
      // Must start with 'jar:file:/' (10 characters) and we strip off the 
      // '!/vcnc/Main.class' at the end (17 characters). 
      cp = url.substring(10,url.length() - 17);
    
//    System.err.println("will try " +cp);
    
    // Go ahead and set things to stdout/err when running from the CLI,
    // but not as a GUI.
    int result = -1;
    if (Main.runningAsCLI() == true)
      result = compiler.run(null,System.out,System.err,
          "-cp","C:\\Users\\rsf\\Documents\\WorkArea\\vcnc\\bin",
          Persist.getGerLocation() + File.separator + wName + ".java");
    else
      result = compiler.run(null,null,null,
        "-cp","C:\\Users\\rsf\\Documents\\WorkArea\\vcnc\\bin",
        Persist.getGerLocation() + File.separator + wName + ".java");
      
    if (result != 0)
      // Compilation failure.
      return false;
      
    
//    System.err.println("done compile attempt");
    
    return false;
  }

}
