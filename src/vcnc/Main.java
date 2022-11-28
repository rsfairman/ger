package vcnc;

//NEXT:
//  
//* Start with the lexer and work up to the last layer. As you reach things
//  that need some kind of UI thing (like the tool table), write the
//  necessary Swing.
// don't forget to update the dev manual.
//
// I think the lexer is good...see about how to do unit tests...
// 
//* work offsets table and persistence framework



/*
12345678901234567890123456789012345678901234567890123456789012345678901234567890

Compiling

Obviously, Eclipse is the easy way, with no additional tools needed.

If the source files are organized in packages (in the usual way Eclipse does 
it), then one can cd to the primary directory -- vcnc in my case, where there's
a bin directory for the .class files and a src directory that contains the
packages, with vcnc as the top-level package name. Then say

javac -cp src -d bin src/vcnc/Main.java
 
and it creates all the .class files, exactly as Eclipse would do.



Version History

v01

A revival of the vcnc program I worked on years ago. There's a summary of
what went down in the earlier versions in lab.tex.

It appears from the comments in the Qt version that I did fix some non-trivial
bugs in the original Java. It does not look like my intent was to do anything
significantly different in the Qt version though.

The initial goal is to implement the basic transpiler from "full" G-code to 
G-code that consists of nothing but linear moves and circular arcs. What 
I'm doing is copying stuff from qt dev/.../interp (C++ source) and from 
revived (Java source), doing so bit by bit. 

Set up the UI so that there is an editable panel on the left, with the
input G-code, and an output panel on the right. The output might be the
transpiled code, or a 3D rendering -- eventually. On second thought, I should
put everything in a distinct tab, and let the user break out the tabs
into multiple columns if he likes. So, each tab (and its contents) can
appear exactly once in the window. By default the window shows one set of
tabs, but the user can drag tabs to the right to create another set of tabs.

v02

First, I got rid of various sub-versions of the drag & drops stuff that were
created as that was figured out. There were a couple of packages called 'ex01',
and so forth, plus TabbedPaneDnD01 and 02t. Next, I took the DnD code that I
am using, and cleaning it up; it was full of crufty test code and the like.

After a hiatus, to push things forward a bit, I will bring the various layers
on line, rather than tweaking some of the lower level framework issues. 
It looks like qt dev/vcnc14 has the latest conceptual version, but it is in
C++, which makes it fiddly to port. The ancient/revived version is the most
recent java version, and I will use that for reference, but I did make some
important and bug-fixes in the C++ version, so I will copy over the C++ and
port it back to java.

All the various layers have been ported back to Java, and the thing compiles
and runs, even if I am sure there are bugs galore, and there are various bits
and pieces that I have not reimplemented properly (like the tool table). 
I also put in place the basic framework for wizards.

v03

Much cleaning up, with various files and classes deleted. This was done, in
part, as the development manual was written/updated.

Switched to a different method of displaying line numbers -- much better. The
Swing code being used (TextLineNumber) is a tad more complicated, but it's
easier to use. See GInputTab for how it is used.

v04

The Swing aspect is far from done, but certain aspects are much cleaner,
so I deleted various test files, particularly those in vcnc.ui.LineNumbers.
I also removed the components package, which was just some junky example code
from Oracle.

Somehow I had two classes for global state variables. I got rid of 
vcnc.MachineGlobals and folded it into vcnc.transpile.MachineState. 

Added a unit test framework. It can be run from the main() method instead
of launching the GUI.






 



I've dragged all the files from revived/main over. In fact, these are the
packages in the 'revived' Java project. An 'X' means that I took everything
from it, and copied into the new project.

        da3ddisplay
        flatimage
        graphics
        interpreter
        lexer
     X  main
        parser
        rsf -- lots of stuff, take as needed
        tooltable
     X  ui
        voxelframe
        workoffsets









Looking at the comments for main.cpp in the C++/Qt version, I am up to 
about v05.



I need to make some decisions about "home" and coordinate systems.
For example, G28 is "return to home" but what does "home" mean?

Add a WorkOffset command. 

Great input for tool table. By default, use tool 1.





TODO:

* When the last window is closed (x icon), the entire program should quit.
  
* Change the way the parser works a bit. Certain codes should just pass
  through.
  
* Problem when you close tabs (or entire windows). They somehow seem to
  remain on "the list."

* I need to insert the G/M/whatever wizard interpreter after the lexer
  and before the parser. Some odd G-code might expect certain arguments
  and the parser needs to know what those are.
 
* Idea: certain G-codes, like G81 for peck drilling, are very specific to
  the controller. I do not want this thing to interpret these; let them pass
  through the interpreter untouched. OTOH, I can imagine that this would be
  frustrating to some users. What's needed is a way to allow the user to
  define certain G-codes for HIS machine. This is a bit like externally
  defined wizards, but with a more restricted syntax. Ideally, we should allow
  them to define a function, together with a choice of the layer of 
  transpilation at which the translation is injected. 
  
  In fact, choosing the layer of injection would be good for ordinary wizards
  too. 

* After using and exercising things a bit, come back to the TabbedPaneDnD
  stuff and clean up some more. I might even put things back into fewer files.
  There's really only one front-end class.
  
  Really, Only one class in the TabbedPaneDnD package should be public.

* What about text size and fonts in general?

* Track file dirty

* Make the lexer output nicer too, with column labels. OTOH, you couldn't
  then parse the lexers output -- or would have to strip off the first line.
  
  It uses TextBuffer (and TextGetter?). Clean those up too.

* How to report errors?
  Maybe assume that if a layer completes, then there were no errors
  in that layer. So, we do the lexer. If any errors arise at any point, then
  halt the entire thing, print out the error message and go to the relevant
  line.

* Double-click or something to make tabs into their own windows.
  
  See https://docs.oracle.com/javase/tutorial/uiswing/components/tabbedpane.html
  I want close exes, and some kind of "expand" icon.

* I am kind of dumping stuff into random places. Give the packages better names.

* 

*

*/

// BUG: Should I name all these packages ger.whatever instead of vcnc.whatever?
// Ger *is* supposed to be the name of the program.


import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import vcnc.unittest.UnitTests;


public class Main {

  private static void createAndShowGUI() {
  	
    // Create and set up the window.
  	MainWindow mainWindow = new MainWindow();
  	
  	// BUG: Testing
  	MainWindow other = mainWindow.doNewWindow();
  	
  	String dir = "C:\\Users\\rsf\\Documents\\WorkArea\\vcnc\\ancient\\qt dev\\vcnc\\vcnc14\\test suite";

    mainWindow.doOpen(dir,"layer00.txt");
    mainWindow.doOpen(dir,"layer01.txt");
    mainWindow.doOpen(dir,"layer02.txt");

    other.doOpen(dir,"layer03.txt");
    other.doOpen(dir,"layer04.txt");
    other.doOpen(dir,"layer05.txt");
  	
  }
	
  public static void guiMain() {
    
    // Run this normally.
    
    /*
    It seems like the default ("Metal") L&F actually looks better,
    but that may have to do with the background colors I'm using.
    For now, ignore this.
    try {
      
      // Set System L&F
//      UIManager.setLookAndFeel(
//          UIManager.getSystemLookAndFeelClassName());
      
      // This is what happens by default.
      UIManager.setLookAndFeel(
          UIManager.getCrossPlatformLookAndFeelClassName());
    } 
    catch (UnsupportedLookAndFeelException e) {
     // handle exception
    }
    catch (ClassNotFoundException e) {
     // handle exception
    }
    catch (InstantiationException e) {
     // handle exception
    }
    catch (IllegalAccessException e) {
     // handle exception
    }
    */
    
//  System.out.println("here");
  
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          createAndShowGUI();
        }
      });
  }

  public static void testMain() {
    
    // To run the units tests.
    // You can run any or all of these, and the test of each layer may
    // consist of several individual tests.
    UnitTests.testLexer();
    
    
  }

  public static void main(String[] args) {
    
    // There are two ways to run the program: normally, as a gui; or to
    // do unit tests. Comment out one or the other.
    // 
    // It wouldn't be hard to take a command-line argument to do these
    // tests, but it's more trouble than it's worth.
    
    // The usual way to run the program:
//    guiMain();
    
    // Or, run a series of unit tests:
    testMain();

  }
  

}

