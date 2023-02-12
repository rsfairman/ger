package vcnc;

// Annotations:
// BUG is used for things that definitely further attention, or at least
// further thought.
// NOTE is used for things that aren't quite bugs, but that could present
// problems down the road or that may not be obvious.

// PATH FORWARD (which may change!)
// * Finish the various layers, pretty much as they're set up.
//   Implement the relevant GUI bits.
// * Change the way layers are applied so that each statement
//   flows through all layers (except cutter comp?) before the next 
//   statement is considered.
// * Move wizard expansion to the point immediately before application
//   of cutter comp.
// * Get going on rendering. Start with a 2D array with z-buffer.
// * Allow for non-standard cutters. Probably need a bezier path language
//   to do this well.



// Get this to work well up through Layer00, including wizards.
// Strip out (or somehow make less accessible) anything after Layer00.
//
// Test (again) compiling wizards and the simple wizard I have.
// Don't worry about anything other than G00, G01, G02, G03. Really,
// everything should just pass through. In fact, for wizards, just do G0/01.

// Test
// * GUI conversion (layer00) DONE
// * Compiling wizards from CLI DONE
// * Compiling wizards on the fly DONE
// * Translating code from CLI with wizards DONE
// * Translating code from GUI with wizards DONE
// * Persistent data DONE
// * Do G20/21 work? NOT RELEVANT.
// * Use jar file. DONE



/*
12345678901234567890123456789012345678901234567890123456789012345678901234567890

Compiling:
----------

Eclipse is the easy way, with no additional tools needed.

If the source files are organized in packages (in the usual way Eclipse does 
it), then one can cd to the primary directory -- vcnc in my case, where there's
a bin directory for the .class files and a src directory that contains the
packages, with vcnc as the top-level package name. Then say

javac -cp src -d bin src/vcnc/Main.java
 
and it creates all the .class files, exactly as Eclipse would do, sending
them to a separate bin directory.

How to Jarify:
--------------

Again, Eclipse makes this easy. Just "Export" it as a "Runnable JAR File."

Or, you can do it more directly by cd-ing to the bin directory, where all the
.class files are (as above). The manifest file should be there too, consisting 
of one line: 
Main-Class: vcnc.Main

Now type

jar cvmf manifest ger.jar vcnc

and you get the jar file in the bin directory. In fact, I find this easier
than using Eclipse. 

To run, say

java -jar ger.jar



Version History
---------------

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
set as a resulttable package, most of which is a mess. I just wanted a
placeholder for the Swing aspects of the UI.

Created Layer0A, with the intent of breaking up the wizard/machine directive
stuff into two layers. Layer0A is done(ish); it handles machine directives.
Worked on LayerPre (which has been renamed to Layer0B) to handle actual
wizards. This requires loading a class that many not have been known at
compile-time, and it may require compiling a .java file to the .class file.

v08

Clean up some of the mess in Layer0B now that the essence is there. A bunch
of tests related to run-time compilation were removed.

Created a jar and there is now CLI access. Compiling wizards is most easily 
done (for me, as the developer, and for the user) with a CLI. While I'm at it,
letting the user translate G-code via the CLI is an easy thing, as is running 
the unit tests. See Main.handleCLI() below.

I confused myself with regard to complications introduced by the expansion
of wizards. There's nothing in the code about this issue -- other than a 
comment at the head of Layer00 -- but I wasted some time.

Made some modest changes to Parser and Layer00 to make the tracking of
sub-program calls clearer.

I sort of abandoned this version to reorganize the code. Instead of having
each layer feed up to the next one statement at a time, have them process
the entire set of statements, then pass all of those statements up to
the next layer in one lump.

So, what's in here is a mess and won't compile or work properly.

v09

The goal is now for each layer to consume the *entire* output of the previous
layer, process it, and pass the result of its transformation applied to the
entire program to the next layer. This is hoggish of memory, but certain
difficulties should go away, particularly how sub-programs and wizards are
managed. It should also reduce some of the plumbing that links the layers.

This feels (and is) hoggish, but it should be fine. The feed rate is likely
to average something like 10 IPM (inches per minute). If each line moves the
tool 0.050 inches, then 100,000 lines are a total "path traveled" of 5,000
inches. At 10 IPM, that's 500 minutes, which is a long time for a program
to run. This is a crude estimate, but it's the right order of magnitude.
A program of 100,000 lines is something like 4 megabytes. There's plenty
of memory.

This change requires an extensive refactoring, from the Lexer on up.

I've started the refactoring in this version, but it's a mess. It's
sort of (but not really) refactored up through Layer00.

v10

Clean up the junk left over from refactoring Lexer, Parser, Layer0A, Layer0B
and Layer00: commented out junk, useless classes, etc. Lots of stuff was
removed. The refactoring through Layer00 should be complete, though not
tested.

I also began changing from the use of a single Statement class throughout
the series of layers to a set of different classes of statement, where these
different kinds of statement become more limited as the layers proceed. This 
has value in that it uses the compiler and type-checking to ensure that things 
are being done correctly, and it documents in a clearer way which G-codes are 
in play at any given stage. This involved many small edits, but no major 
conceptual change.

Some changes to the way wizards are to be specified by the user. They can
now be expressed somewhat more briefly (less typing) at modest cost in the
internal complexity of the wizard framework (invisible to the user).

Plus...much fiddling to get things into a more usable form...basically, 
resolving many small errors (or not so small).

This version "works" as far as it goes (through Layer00 for M98/M99) and
should be complete and bug-free (but has not been extensively tested).

v11

Get Layer01 working for G20/G21 (inches/mm)... Attempting this made me
realize that the change to how layers work was a mistake. In much earlier
versions, each layer acted like a middle-man; it took a statement from a lower
layer, digested it, and passed it to the next layer. Then I changed it so that
each layer digests the entire input G-code, and passes the entire result
to the next layer. That doesn't work, even for something as simple as inch/mm
conversion. How that conversion is done depends on whether you are in polar 
coordinate mode and which is the current reference plane (AxisChoice). You 
can't do the inch/mm conversion unless you've completely digested everything,
noting any changes to state as you go.

That said, the buffers used between layers prior to v08 were awkward -- sort
of clever, but also fiddly. Going forward, a linked list makes sense. The
Parser will produce a linked list of statements of the complete program. 
Each layer may insert or remove statements (or modify them in place). Note
that the "layers" will no longer be separate classes, but something more
like methods in a single class.

Unfortunately, java.util.LinkedList is stupid. A linked list is the right
way to do this, but java.util.LinkedList doesn't allow for access to the
nodes/links themselves. I tried ArrayList, but without "nodes," dealing
with sub-programs is difficult. You want to return from a sub-program to a
specific point in the program, but you can't do it by line number (i.e.,
index in the array) since they change. It *might* be possible to use ArrayList
by relying on the idea that an object can serve as the reference to the 
(sort-of) node, but the resulting code is not very clear. Bottom line: I 
wrote my own linked list (and node) class. See vcnc.util.LList and LLNode.

v12

I had a break while I did a bit of playing around with Haskell on the 
"yatzee problem." The idea of using Haskell for the transpiling part of the 
task is mildly tempting, but mixing languages seems like a bad idea. There's
also the problem of how to allow users to define wizards. It's one thing for
people to use Java, but something else entirely to expect them to use Haskell.
It *is* possible to call Java from Haskell, including Swing -- see 
https://www.tweag.io/blog/2017-09-15-inline-java-tutorial/
but it seems like a big headache for a modest project like this.

First thing: go back to the original Statement type. The idea of the various
St-whatever types was good in theory, in that it helps to enforce and make
clear where you are in the process, but it's difficult to work with.

This is pretty clean up through Translator.ThruUnits -- the translator; the
GUI has some uglier bits. This is the most basic things, like G20/21 and
it can expands simple wizards (linear moves only).  

Slowly trashing old code too as things are refactored...

v13

Came to some decisions about things like the work offsets table and tool
length offset (TLO). The bottom line is that these things rely too much on the
specifics of any particular physical machine. Instead of trying to unravel
all the various possibilities, minimize their effects. For example, TLO really
doesn't matter unless you're on a very specific physical machine setup, so 
don't even try to simulate it.

Small tweaks to parsing the G52 command in the parser and added the G92
command to the parser. Made the UI aspect of the work offsets table work
properly, including making it persist. Then added bit to translator to
handle these commands with PRZ.

Cut a version here for not particular reason other than I wanted to store
a copy before making more changes.

v14

Have (in v13) made various changes so that the main translation loop is tidier.
Now it's time to nail down more of the individual cases.

Polar coordinates done.
Needed to add a bit for handling changes to PRZ.
Handling incremental coordinates too.

Started on the last thing to do, before cutter comp, which is tracking the 
cutter position after the statement has been brought to a form where all
coordinates are in absolute terms. 

Changed the Lexer/Token in a minor way so that EOF is now unicode \u0000
instead of '*'. The "correct" solution is for the Tokens to be typed,
either with an enum or by sub-classes, but that's bloaty.

Got rid of the character count field in the Token objects. In theory, these
could be used for better error reporting, but they were being almost entirely
ignored. G-code is so simple that pointing a person to a specific character
in an error message isn't needed, and could just confusing things since
the character index might not match up with where a person (as opposed to the
lexer/parser) thinks it occurs. The character count was just noise.

There was also a field left over in Token.endCount that was used for dealing
with sub-programs. Under the new scheme, that's no longer needed. This
required some modest edits in Lexer and Parser, and the returnChar and 
returnLine fields of DataSubroutineCall are no longer needed for the same 
reason.

Some top-left cleanup around the interface to the translator, and how
it interfaces to the parser and lexer -- mostly due to the fact that we've
refactored to use linked lists.

I have done *some* debugging of this, but nothing extensive.

v15

Although cutter comp isn't done (or ever started) and wizards could be more
powerful, *and* what exists hasn't really been debugged, it seems the time
to implement the DA-based (2.5-D) viewer. Eyeballing text isn't the best way
to debug sometimes.







 
 
 



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









TODO:

* When the last window is closed (x icon), the entire program should quit.

* The way in which the tabs of code/translations are managed is far from
  ideal. The GInputTab objects point to the possible translations of the 
  output -- which is what I want. The problem is that when tabs are closed,
  the data associated with that tab doesn't disappear. I've found a sort of
  workaround, but it's nasty. What you want is a data structure representing
  these associations that is not directly tied to what's happening in the
  Swing UI.
    
* Currently, the output text includes the original line numbers as an
  N-code. There's value to that when debugging so that you can see the 
  original line that led to what you're looking at, but it will usually
  be unnecessary and could confuse the target machine. Add a way to turn
  that off and on.
  
* What about text size and fonts in general?

* Track file dirty

* It is tempting to use a MetaPost-like language to specify bezier curves.
  I wrote a parser and interpreter for that once, and it could be brought over.
  So, have a wizard that takes a single string as argument like
  Bezier "(x1,y1)..(x2,y2)"

*/


// BUG: Should I name all these packages ger.whatever instead of vcnc.whatever?
// Ger *is* supposed to be the name of the program.

import java.io.File;
import java.nio.file.Path;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import vcnc.persist.Persist;
import vcnc.tpile.Translator;
import vcnc.unittest.UnitTests;
import vcnc.util.FileIOUtil;


public class Main {
  
  // Whether this is being run as a CLI (or GUI).
  private static boolean asCLI = false;
  
  
  public static boolean runningAsCLI() {
    return asCLI;
  }

  private static void createAndShowGUI() {
  	
    // Create and set up the window.
  	MainWindow mainWindow = new MainWindow();
  	
  	
  	
  	
  	// BUG: Testing
  	MainWindow other = mainWindow.doNewWindow();
  	
  	String dir = "C:\\Users\\rsf\\Documents\\WorkArea\\vcnc\\ancient\\qt dev\\vcnc\\vcnc14\\test suite";

//    mainWindow.doOpen(dir,"layer00.txt");
//    mainWindow.doOpen(dir,"layer01.txt");
//    mainWindow.doOpen(dir,"layer02.txt");
//
//    other.doOpen(dir,"layer03.txt");
//    other.doOpen(dir,"layer04.txt");
//    other.doOpen(dir,"layer05.txt");
  	
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

  public static void testMain(String path) {
    
    // To run the units tests.
    // You can run any or all of these, and the test of each layer may
    // consist of several individual tests.
    UnitTests.setDir(path);
    UnitTests.testAll();
    
  }
  
  private static boolean handleCLI(String[] args) {

    // Parse any arguments and act accordingly. If there are no arguments or
    // the only argument is 'gui', then return true immediately. Otherwise,
    // parse the arguments, act accordingly and return false.
    // 
    // The permitted arguments are
    // * 'gui' (or no arguments) to launch as a GUI program.
    // * 'test' to run a suite of tests, with the path.
    // * 'change' to switch to a different .ger directory. Provide the
    //   path to the desired .ger directory. If not path is provided, it
    //   will report the current path.
    // * 'translate' to run a single input g-code file and send the
    //   output to stdout.
    // * 'compile' takes a wizard name. The associated .java file must be in 
    //   the .ger directory. This is slightly redundant since 'translate' 
    //   will attempt to compile too, provided there's an uncompiled wizard 
    //   referenced by the G-code.
    
    if ((args == null) || (args.length == 0))
      return true;
    
    if ((args.length == 1) && (args[0].equals("gui")))
      return true;
    
    // Got here, so we're running strictly with CLI.
    Main.asCLI = true;
    
    
    if ((args.length == 2) && (args[0].equals("test")))
      {
        // Run unit tests. First, where are they?
        File f = new File(args[1]);
        
        if (f.exists() == false)
          System.err.println("No such directory.");
        else if (f.isDirectory() == false)
          System.err.println("That is not a directory.");
        else
          testMain(f.getAbsolutePath());
        
        return false;
      }
    
    if ((args.length == 2) && (args[0].equals("change")))
      {
        // Change the .ger directory.
        File f = new File(args[1]);
        
        if (f.exists() == false)
          System.err.println("No such directory.");
        else if (f.isDirectory() == false)
          System.err.println("That is not a directory.");
        else
          {
            System.out.println("Changed to " +f.getAbsolutePath());
            Persist.setGerLocation(f.getAbsolutePath());
          }
        
        return false;
      }
    
    if ((args.length == 1) && (args[0].equals("change")))
      {
        // Report the current path to the .ger directory.
        System.out.println(Persist.getGerLocation());
        
        return false;
      }
    
    if ((args.length == 2) && (args[0].equals("compile")))
      {
        WizCompile.compile(args[1]);
        return false;
      }
    
    if ((args.length == 2) && (args[0].equals("translate")))
      {
        File f = new File(args[1]);
        if (f.exists() == false)
          {
            System.err.println("No such file.");
            return false;
          }
        
        // BUG: Implemented only up to a particular layer (as far as has
        // been debugged).
        
        // BUG: Annoying exception to be eliminated?
        try {
          
        String inCode = FileIOUtil.loadFileToString(".",args[1]);
        String outCode = Translator.digest(inCode,Translator.ThruEverything);
        System.out.println(outCode);
        
        } catch (Exception e) {
          System.err.println("Problem: " +e.getMessage());
        }
        
        return false;
      }
    
    System.err.println("Unexpected arguments...");
    return false;
  }

  public static void main(String[] args) {
    
    // Load any machine settings from the .ger directory to MachineState.
    Persist.reload();
    
    if (handleCLI(args) == true)
      guiMain();
    
    // else, fall off; it was dealt with by handleCLI().
    
    
    
    // For testing...
    
    
//    WizCompile.compile("SimpleWiz2");
    

//    System.exit(0);
//    guiMain();
    
//    testMain();

  }
  

}

