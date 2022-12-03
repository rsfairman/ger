package vcnc;

//NEXT:
//  
//* Start with the lexer and work up to the last layer. As you reach things
//  that need some kind of UI thing (like the tool table), write the
//  necessary Swing.
//
// don't forget to update the dev manual.
//
// I think the lexer is good
// 
// parser too, although I'm sure errors will be uncovered in each of these 
// layers as the next layer is created. 
// 
// 
// Look at the "pre" layer for wizards. Rename the layers?
// 
//* work offsets table and persistence framework. Easiest is probably some 
//  kind of hidden .ger file or directory for settings.



/*
12345678901234567890123456789012345678901234567890123456789012345678901234567890

Compiling:

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
Note that vcnc.transpile has been renamed (as of v05) to vcnc.tpile.  

Added a unit test framework. It can be run from the main() method instead
of launching the GUI.

Rewrote things to make TextBuffer simpler and not based directly on a 
JTextArea. This simplifies a variety of things, including the Lexer (and
will simplify the higher layers eventually).  

v05

Clean up and throw away much useless code related to changes made in the
previous version.

The whole TextGetter/TextBuffer thing was overkill. They've been combined to a 
single class and it no longer does any buffering on it's own. It just uses 
String.charAt(). 

Moved some of the files around so that all the code related to transpiling
is in vcnc.tpile, whether directly under it, or in a sub-package.

v06

Renamed the single TextBuffer class to CodeBuffer for clarity.

Simplified the primary switch statement in Parser so that the error cases
are all handled together -- much briefer now.

Got rid of the idea of letting unknown, but potentially meaningful, commands
pass through the translator unchanged. The idea was that something like 
G83 (peck drilling) might exist on a particular physical machine and I don't
want to strip it out of the program or flag it as an error since the user may
want to use it. Even weirder would be something like G84.3 for left-handed
tapping. If the command and its quirks is unknown to me, then it's
difficult to parse. In theory, I could just take everything from 'G83' (or 
whatever) to the EOL and call that a command and assume that it's valid on 
the target machine. But my impression is that some of these commands differ so
much from one physical machine to another that this is only going to confuse
things. It will also be impossible to render what these commands do as a 
3D image.

One solution to this problem is to define anything like this as a wizard.
For example, instead of 'G83', the user would say something like 'Peck'
and the wizard would convert to more basic G-codes -- which is what something
like a FANUC must be doing anyway. Another solution would be to provide a
selection of physical machines that the program is able to simulate and then 
implement the various codes available on each machine, but that is a huge 
research project. A middle ground, that I was considering and may eventually
do, is to allow the user to define things like G84.3 or any G-code that is
at all non-standard. They can define it however they like, and it would be
implemented basically like wizards. What's needed to allow that is detecting
these "pseudo-wizards" since wizards are currently detected based on starting
with two letters (so, not G-number). This wouldn't be that hard.

Renamed various classes related to the data associated with statements.
These are the classes that extend StateData, which is now called StatementData
to make it clear that it's not about "state" particularly.

v07

The program needs a certain amount of persistent data: things like the tool
table or the available wizards, that should remain fixed every time the program
is launched. The amount of data could end up being pretty extensive, so the
most flexible way is to use a .ger directory. See vcnc.persist.Persist. 

This persistent data should go together, so it makes sense for it to all be
set as a result of a single dialog. In particular, it should be clear to the
user that the inch/mm choice affects the tool and work offsets tables.

So...reorganized the WorkOffsets stuff, mostly with regard to the UI, and
added the vcnc.tooltable package, most of which is a mess.








 



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


DOWN THE ROAD

*** It is tempting to use a MetaPost-like language to specify bezier curves.
I wrote a parser and interpreter for that once, and it could be brought over.
So, have a wizard that takes a single string as argument like
Bezier "(x1,y1)..(x2,y2)"

*** 








TODO:

* When the last window is closed (x icon), the entire program should quit.
  
* Change the way the parser works a bit. Certain codes should just pass
  through.
  
* Problem when you close tabs (or entire windows). They somehow seem to
  remain on "the list."

* I need to insert the G/M/whatever wizard interpreter after the lexer
  and before the parser. Some odd G-code might expect certain arguments
  and the parser needs to know what those are.

* What about text size and fonts in general?

* Track file dirty

* How to report errors?
  Maybe assume that if a layer completes, then there were no errors
  in that layer. So, we do the lexer. If any errors arise at any point, then
  halt the entire thing, print out the error message and go to the relevant
  line.


*/

// BUG: Should I name all these packages ger.whatever instead of vcnc.whatever?
// Ger *is* supposed to be the name of the program.


import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.util.prefs.Preferences;

import vcnc.persist.Persist;
import vcnc.unittest.UnitTests;


public class Main {
  
  

  private static void createAndShowGUI() {
  	
    // Create and set up the window.
  	MainWindow mainWindow = new MainWindow();
  	
  	// BUG: Testing
//  	MainWindow other = mainWindow.doNewWindow();
  	
/*
  	String dir = "C:\\Users\\rsf\\Documents\\WorkArea\\vcnc\\ancient\\qt dev\\vcnc\\vcnc14\\test suite";

    mainWindow.doOpen(dir,"layer00.txt");
    mainWindow.doOpen(dir,"layer01.txt");
    mainWindow.doOpen(dir,"layer02.txt");

    other.doOpen(dir,"layer03.txt");
    other.doOpen(dir,"layer04.txt");
    other.doOpen(dir,"layer05.txt");
*/

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
    System.out.println("Running unit tests...");
    
    UnitTests.testLexer();
//    UnitTests.testParser();
    
    System.out.println("Unit tests complete.");
    
  }

  public static void main(String[] args) {
    
    // Load any machine settings from the .ger directory to MachineState.
    Persist.loadSettings();
    
    // There are two ways to run the program: normally, as a gui; or to
    // do unit tests. Comment out one or the other.
    // 
    // It wouldn't be hard to take a command-line argument to do these
    // tests, but it's more trouble than it's worth.
    
    // The usual way to run the program:
    guiMain();
    
    // Or, run a series of unit tests:
//    testMain();

  }
  

}

