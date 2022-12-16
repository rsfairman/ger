package vcnc.wizard;


/*

BUG: (or not) This doesn't put the machine into a default state before
the wizard is expanded. 


All wizard definitions must extend this base class.

The extending class must implement the definition() method, which is abstract.

Internally, the wizard is run by calling execute(), but the user won't need 
to know that.

There are several problems with the general concept of "wizards," with no
perfect solution. The person writing the wizard may want to know whether the
machine is in inch or mm mode, or whether polar coordinates are being used.
In fact, if the machine is in mm mode and he is expecting inch, then his
wizard will almost certainly do the wrong thing; similarly for polar
coordinates (what a mess that would be). If the machine were in relative
mode, and the wizard expects absolute, then that would be another disaster.

So, it seems clear that the machine must be put into "default state" before
a wizard is invoked. So, using the default units for the machine, and no polar
coordinates. Allow the plane (XY, ZX or YZ) to "float" since it's hard to
imagine writing (or wanting to write) a wizard that depends on that. Also
allow the G00/G01/G02/G03 mode to float since it's natural for the user to
adjust that all the time.

The ideas below are meant as background, but aren't directly relevant to
what is being done...

Whether in absolute or relative mode is another sticky problem. It's hard
to imagine wanting a wizard that is genuinely in absolute mode -- unless 
it's to be used for a single location on a single part, and then it's hard
to see the point of a wizard. In any case, if the user really wants absolute
mode (maybe his part involves some difficult calculation), then he could 
say G90 and get it.
 
Typically, wizards are things you want to do at a particular location, like
a bolt circle relative to the center of the circle, or a pocket relative
to the lower-left corner. So, it seems that wizards should run in absolute
mode, as though the origin has been changed to the location of the tool when 
the wizard begins. Allowing the user access to G91 (incremental mode) from
wizards seems like a disaster waiting to happen, and with access to a real
programming language, G91 seems less useful in any case. 

Do not consider cutter comp or TLO. It seems like these really are independent 
of wizards.

Bottom line...
Wizards always start off using the default units, cartesian coordinates and
in absolute mode, after changing the origin to the location of the tool when 
the wizard was invoked. The change to these defaults (if it is a change) must
be undone before the wizard returns.

So, calls to a wizard are sandwiched with additional pre and post codes:
G20/21 (to get into the correct inch/mm mode)
G15 (polar coordiantes off)
G90 (absolute mode)
G92 X0 Y0 Z0 (to reset the origin)
--- wizard code ---
G92 (saved position)*
G91 (if needed: back to incremental mode)
G16 (if needed: back to polar coordinates)
G20/21
 
* It is tempting to insert a move to bring the tool back to where it was
  when the wizard started, then undo the G92. The problem with this is that
  the tool could crash depending on the setup. It seems better to let the
  tool remain where it's left; it that leads to a crash, then the user needs
  to fix it. 


BUG: THE PROBLEM WITH THE IDEAS ABOVE IS THAT THERE'S NO CLEAR WAY TO DO WHAT 
THEY DESCRIBE.

If the wizards are expanded early (as the very first thing, after the parser),
then there's no way to "sandwich" the wizard expansion in a series of 
commands as above; e.g., we don't know whether the *current* units are inch or
mm because that layer hasn't yet been applied and the machine state hasn't
been updated accordingly.

We *could* provide a new command, call it G666, that means "push the current
state onto a stack then change to default state," and another command that 
means "pop the state off the stack and apply it." These two commands, that
are *not* to be accessible (or even known) to the user would be translated at
some later stage, when the machine state is available. 

A more fundamental difficulty is that a wizard might want to know the state
of the machine for some reason. The big example is knowing what the z-coordinate
is (in absolute terms). The tool could be anywhere when the wizard is called,
and you certainly need to know the Z-coordinate if you're going to cut a 
pocket (say) to a certain depth. When the wizard is expanded, it should ideally 
have access to everything about the machine.

This is an argument for expanding wizards later in the translation process.
We could apply the wizard expansion layer immediately before the cutter
comp (and TLO?) layer, but that would mean that wizards can't use the
commands that are "translated away" in the earlier layers -- which may not
be such a big deal.

Alternatively, these layers could be implemented so that the boundaries
between layers are "softer." Instead of passing all statements through 
layer 1, then all statements through layer 2, etc., take a single statement 
and pass it through all the layers, then pass the next statement through all 
the layers, etc. Then you would know everything about the machine state when
the wizard is expanded.

It is still that case that wizards must be expanded before cutter comp.
For one thing the cutter comp calculation requires examining transitions
from move to move. In any case, wizards have to come before cutter comp if
we ever want cutter comp to apply to a path generated by a wizard.






BUG: Add "makers" for other G-codes, like G02, etc.

*/

import java.util.ArrayList;

import vcnc.tpile.St0B;


abstract public class WizardBase {

  private static ArrayList<St0B> wizout = null;

  // Define the wizard here.
  abstract public void definition(ArrayList<Object> args);
  
  
  public ArrayList<St0B> execute(ArrayList<Object> args) {
    
    // Convert the commands that appear in definiton() to an array that
    // will be inserted into the larger G-code program.
    // The items in the args array consists of either Double or String objects.
    
    wizout = new ArrayList<>();
    definition(args);
    
    // BUG: Might make sense to insert a check here to make sure the user
    // didn't do anything stupid, like say
    // Move();
    // without setting any coordinates. OTOH, these things are checked
    // at later layers. OTOOH, it might be difficult to indicate the
    // nature of the error from that point in a way that makes it clear to
    // the user that the source of the problem is inside a wizard. 
    // Or just ignore it; it's not really an error; it might be mere sloppiness.
    
    return wizout;
  }
  
  public static void G00() {
    
    wizout.add(new St0B(St0B.G00));
    
    // BUG: What about line number and char number?
    // Maybe as arguments to constructor?
  }

  public static void G01() {
    
    wizout.add(new St0B(St0B.G01));
    
    // BUG: What about line number and char number?
    // Maybe as arguments to constructor?
  }
  
  public static MoveMaker Move() {
    
    // This is a little weird, but seems more natural and easier for the user.
    // To use this from WizardBase.execute(), say
    // Move().X(1.0).F(20.0);
    // or whatever. The point is that Move() (this method) returns a MoveMaker,
    // which can then be given the arguments.
    //
    // BUG: It would be nice if I could eliminate the need for Move(),
    // and be able to say something like X(14.0).F(3.0), but I don't see how.
    return new MoveMaker(wizout);
  }
}
