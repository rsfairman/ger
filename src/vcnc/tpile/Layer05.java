package vcnc.tpile;

/*

BUG: See Parser. I may need to use three buffers there for the Statements
to allow peeking forward AND back. That's what I did in the C++ version.
I copied a comment from the C++ to Parser.java about this. There may be
additional comments about the issue in the parser.cpp file.

BUG: Add yet another layer to remove codes that don't do anything when
rendering (coolant, spindle speed and the like). This means that I should be 
more careful in earlier layers about *not* stripping that stuff out.



Another layer to the interpreter.

Layer00 handles subroutine calls and returns. This removes M98 and M99 from
      the input stream, along with a bunch of things that I don't handle
      or that don't mean anything in a virtual context, like coolant on/off.
Layer01 converts all values to the standard units of the machine, inches or mm.
      G20 and G21 disappear from the statement stream. Also checks that G02/03
      are given correctly (e.g., no K-value when in G17/XY-plane mode).
Layer02 eliminates TLO and work offsets. It filters out G43, G44 and G49 for
      TLO, along with G52, G54...G59 for work offsets.
Layer03 eliminates polar coordinates. This removes G15 and G16 from the 
      statement stream. Also removes from the stream any linear moves that 
      don't actually move the tool.
Layer04 eliminates incremental coordinates so that everything is given in
      absolute terms -- except for the obvious way that I/J/K are used for
      the center of circles. Also checks that arcs make geometric sense.

Aside: I keep thinking that I should be able to get rid of G17, 18, 19 
(to choose a plane) at some earlier stage of processing, but I need these at
the final interpreter layer because they affect how G02 and G03 are treated.
BUG: Maybe what I should do it change the format of G02 and G03 so that the
plane is specified as part of the statement. That would clean up my code a bit,
but it would mean that the output of the final layer would have some format
(that I come up with) that is not consistent with standard G-code.

This layer handles cutter comp, which is definitely the trickiest thing.
The output of this layer consists of the most fundamental moves: lines and 
arcs with nothing fancy. When two offset curves do not intersect after being 
offset for cutter comp, this layer introduces an extra Statement to move the 
tool along an arc from one curve to the next. This requires that "spareStatement" 
be introduced as a field. See nextStatement(). BUG: Maybe I can get rid of that.

Here's an overview of how the cutter comp problem is dealt with. First, 
there are two buffers. Call them the "private" buffer and the "public"
buffer.

Suppose that you are in cutter comp, and have just read the first move 
statement after CC was invoked. You can't determine exactly how to perform 
that move because you have not yet seen the move that follows. What you can
do is jog the tool so that it is tangent to the start of this initial move.
Put that jogging move into the public buffer and hold onto the intial move
in the private buffer. Continue to read statements, putting them into a
buffer, until you see another move. At that point you can adjust the
initial move to take CC into account, then move that statement into the 
public buffer, along with any intervening statements you saw before the
second move (keep the second move in the private buffer).

So the code here is organized somewhat differently compared to the other
layers. The nextStatement() method, which is the where the higher layer
receives the result, looks at this public buffer. If there is a statement 
there that's ready, it gets passed to the higher layer. If there is no 
statement ready, then a "buffer populate" method is called. 

If you're not in CC mode, then the buffer populate method is pretty much 
identical to what is done in nextStatement() in the other layers -- in
fact there's very little to do other than passing the statements through.
If you *are* in CC mode, then the method first checks whether it has a
previous move that it's sitting on in its private buffer. This is a move
that must be adjusted for CC, and for which we are waiting to see the
next move so that the adjustment is possible. If there is no previous
move sitting there, then we aren't yet really "in" CC mode, and we can
just keep passing statements on to the public buffer until we see the
first move. So, assume that the buffer populate method has a move 
statement that it is sitting on. Begin by putting that (currently 
unadjusted) move into the public buffer -- it will be adjusted before this
method returns. Now it reads additional statements, putting them into the 
public buffer, until it hits the next move. It can now adjust the initial
move, and that move and all of the statements before the most recently
seen move are in the public buffer and ready to be passed up. The second
move that was just read is kept in the private buffer.

In practice, there aren't many of these intervening non-move statement
that might need to be buffered. I've gotten rid of almost all of them.
Even so, I will go with this framework since having it in place will
make it easier if I ever want to add statements to the system that would
fall into this category.

So, the "private buffer" is only a single statement, but the public buffer
can be much longer.

NOTE: One of the things that makes this messier than it seems like it should
be is that fact that the user might have done dumb things, like say G41
(to enter CC mode), then immediately say G40 to cancel it, without there
being any intervening moves. Dealing with G40 (CC cancel) when not in
CC mode is another dumb thing. I *could* introduce another layer prior to
this one to eliminate "dumb stuff" and that might simplify the code here
a bit.

Aside on using the ZX or YZ-plane with CC. I can see why the Tormach
disallows this. I think I made the right decision to permit it, but
it's kind of dumb, not to mention the fact that it complicates the code
a lot! It is hard to see what use this would have. About the only thing
I can come up is a helical cut where the arc is in the YZ-plane, but where
there is linear motion in the x-axis too. Think of cutting the along the
top half of a cylinder. I suppose that some user somewhere might want to
offset the tool to the side of this path...

In fact, as of v09, I do not allow G18 or G19 with cutter comp -- no oddball
planes! See my comments in main.cpp under v08.

BUG: This isn't so much a "bug" as a conceptual difficulty with the whole
idea of cutter comp, and it explains why some controllers look ahead
by more than one move.

Suppose that the G-code specifies two linear moves that come together at a
very acute angle (say 15 degrees), and that CC is being used on the 
inside/acute side of this path with a large tool (say one inch). The tool 
can't even come close to the point where the two lines meet. The problem
that the tool can't reach all the way into any acute angle is true for
any angle less than 180 degrees. This is a well-known problem and is fairly
obvious to anyone who has ever used a mill. In any case, some controllers
prohibit cutter comp on interior angles for just this reason. I chose not
to prohibit CC in these situations since I can see that a person might want
it. After all, the problem is not with CC; the problem is an unavoidable 
aspect of physical reality. Of course, prohibiting CC in these cases
eliminates the entire problem of finding the intersection of curves,
which would be a nice thing as a programmer -- dealing with intersections
is probably the messiest thing in the entire program.

What my code does NOT consider is the following. Consider the two lines
that meet at a 15 degree angle and a one inch tool. The entire end of the
two lines is sort of irrelevant to the tool's post-CC path. Imagine that
the "tip," where the lines meet, is chopped off and you put in another
linear move so that the path looks like a truncated triangle. Ideally, the
user would probably like the program to detect that the small line segment
at the end has no effect on the post-CC tool path and remove it from the
stream. In fact, my program will probably do strange things. Even worse,
imagine that, instead of a single small line segment between the two
longer lines, we have a series of tiny line segments that go between
the two longer lines (making a round cap, say). All of these little
line segments are irrelevant to the path that the user probably wants,
and my program will have the tool wandering all over the place. The only
way to fix this problem is to look all the way ahead to the last move
that takes place in CC mode since a problem like this could arise for
path 1 due to something that happens in path n, where n is arbitrarily
large.

In the abstract, I would like to solve this problem, but there really
is no good solution that is unambiguously the right one. For instance,
suppose that path 1 is a line, then paths 2 through n go out in an
innocuous way, and then path n+1 comes back and runs parallel to path 1
so that the tool would then obliterate the cut made due to path 1.
There's no solution in this situation. Either the user made a mistake or he
expects something very specific to happen that I can't predict.

In light of the fact that this is a simulator, I think I'm making the
right decision in not trying to come up with a "solution" to these
issues. Part of the reason for using a simulator is to check whether
your program does what you want. There are too many different ways that
controllers handle this situation, and any way that I choose will be 
misleading with respect to many real controllers. It's better for the user 
to see that strange things are happening in the simulator, which will tell 
him that he should think hard about just what the heck he is doing -- the
strange behavior I'm describing only arises in cases where the user is
asking the machine to do things that he really shouldn't ask it to do.
So, I take the attitude that when these issues arise, it's a bug in the
user's input, not a bug in my program.

What I *could* do is issue a warning whenever the user applies CC to
an angle of less than 180 degrees. Another small change I could make is
to disallow any line segment shorter than the tool diameter (say). But
I like the idea of letting the simulator act crazy. It informs the user
that they are trying to *do* something crazy.

*/

import java.util.ArrayDeque;

import vcnc.tpile.parse.DataCircular;
import vcnc.tpile.parse.DataMove;
import vcnc.tpile.parse.DataRegister;
import vcnc.tpile.parse.StatementData;
import vcnc.tpile.parse.Statement;


public class Layer05 {

  private Layer04 lowerLayer = null;
  
    
//    // The contents of the tool turret, along with values for D and H.
//    const ToolTurret *turret;
//    
//    // Which plane current active with G17/18/19.
//    // BUG: Why do we care about this??
//    AxisChoice axis;
//      
  // Feed rate to be used in normal mode.
  // When this is negative, we are in rapid mode (G00).
  // I care about this because any tool jogs that occur as we enter
  // or leave CC mode are at this rate.
  // BUG: OTOH, I don't think this needs to be handled here. It
  // could be dealt with at the next layer, and WILL be dealt with,
  // with absolutely no changes to the higher layer.
  // BUG: So there is no need to track this.
  private double feedRate;
     
  // Tool position. This must be updated as each statement is passed along.
  // "Passed along" means as each statement leaves this layer (with the
  // exception of spareStatement, for which the coordinates are updated when
  // the value of spareStatement is set).
  // This is the "real" tool positions; it's where the machine thinks the 
  // tool is.
  // BUG: Should be put into MachineState?  
  public double machX;
  public double machY;
  public double machZ;

  // The "user" position. This is the position as given by the user.
  // In other words, it's where the tool would be if CC had never been
  // turned on. It should be close to the machX/Y/Z position, differing
  // only by the tool radius. This is only relevant when in CC mode.
  // I called this (or something a lot like it) ccMachX/Y/Z in the Java version.
  // BUG: Is this really the best way?
  private double userX;
  private double userY;
  private double userZ;
    
  // To implement cutter comp, we must be able to peek ahead to the next
  // move, and there may be many intervening non-move statements that must
  // be buffered. This is the public buffer discussed above.
  //
  // This just holds pointers so that there are no memory management issues
  // for this class.
  // BUG: I am not checking how many items are in this queue. The possibility
  // is very remote, but a program could be written that "uses up" the buffers
  // in the parser (the StatementBuffer). If there are more than 
  // 2 * StatementBuffer.BufferSize statements that must be buffered here, then
  // you'll get gibberish because the parser will begin reusing statements
  // that are already in this buffer. At the moment, we are talking about
  // 100 statement, and the idea that there could be this many non-move statements
  // in a row is ridiculous.
  //
  // NOTE: In Qt/C++, this was a QQueue data structure, which is FIFO.
  // There are only two methods for this C++ object: enqueue and dequeue.
  // The closest (?) analogue in Java is the Queue inteface, but it is an
  // interface. Those most appropriate implementation seems to be ArrayDeque.
  // Since I want to use it in strictly FIFO fashion, I should limit myself
  // to the methods in the Queue interface -- add(), and poll() or remove().
  // There are lots of other methods that might be useful, but try to avoid
  // them.
//  QQueue<const Statement*> publicBuffer;
  ArrayDeque<Statement> publicBuffer = new ArrayDeque<>();
  
  // When cutter comp is in force, it can lead to "extra" statements beyond
  // those directly specified by the user. For instance, if the user says
  // G01 X1.000 Y1.000
  // X1.000 Y2.000
  // X2.000
  // (that is, a move up, then a move 90 degrees to the right), then simply
  // offsetting these two line segments by the tool radius produces two line
  // segments with no intersection. We need an "extra" statement to get from
  // one to the other.
  //
  // Another situation in which these extra statements are unavoidable is
  // when cutter comp is cancelled. In that case, you need to jog the tool
  // slightly to bring it to the normal (non-cutter comp adjusted) position.
  // 
  // IMPORTANT: this is a special case and the memory involved must be handled
  // by this class. In the case of the parser, where ordinary statements are
  // ultimately generated, the memory problem is dealt with by using buffers
  // that sit on the stack. I do the same thing here. That means that the caller
  // MAY NOT ASSUME THAT THE CONTENTS OF A PREVIOUS STATEMENT WON'T CHANGE if 
  // another call is made to nextStatement().
  // That's true of ordinary statements too, but the buffer used there is
  // so large that it's extremely unlikely to be an issue. For this spare
  // statement, the "buffer" is only one statement deep, so it's more of a 
  // concern.
  Statement spareStatement;
  
  // This is the "private buffer" discussed above. If we're in CC mode, then 
  // it's a move that we are sitting on until we see the next move and are 
  // able to adjust this one to take CC into account. This is a pointer into
  // a buffer at a lower layer, and should not be deleted.
  private Statement privateBuffer;
    
  // Whether cutter comp is on, and the current RADIUS in whatever units (inch or mm).
  // ccLeft is whether cutter comp is on the left (G41) or on the right (G42).
  private boolean cutterComp;
  private boolean ccLeft;
  private double toolRadius;
  
  // These annoying variables are needed to handle an oddball case in performOffset().
  // See the comments in that method. Basically, these are needed when we are in
  // CC mode and the tool moves in a strictly vertical line.
  double vertOffX;
  double vertOffY;
  boolean inOddballVertCase;
  
  // Needed in case reset() is called.
  public double X0;
  public double Y0;
  public double Z0;
    
    
    
  
  
  
  
  
  
  
   
   
  public Layer05(CodeBuffer theText,double X0,double Y0,double Z0) 
      throws Exception {
    ;
    
    this.lowerLayer = new Layer04(theText,X0,Y0,Z0);

    machX = X0;
    machY = Y0;
    machZ = Z0;
//    feedRate = -1.0;
//    axis = XY;
//    spareStatement.type = Statement::NIL;
//    privateBuffer = NULL;
    cutterComp = false;
    ccLeft = true;
    toolRadius = -1.0;
//    inOddballVertCase = false;
    
    this.X0 = X0;
    this.Y0 = Y0;
    this.Z0 = Z0;
  }
  
  public String formError(int lineNumber,String msg) {
    
    return "Error on line " +lineNumber+ ": " +msg;
  }
  
  private void changeToError(Statement cmd,String msg) {
    
    // Change the given statement to an ERROR with the given error msg.
    // BUG: It seems likely that this would be useful in other classes too.
    cmd.type = Statement.ERROR;
    cmd.error = formError(cmd.lineNumber,msg);
  }


  private void noteLineMachCoords(Statement cmd) {
    
    // Note where the tool goes due to the given line move.
    // If this is called when in CC mode, the line is assumed
    // to have been adjusted already.
    DataMove theMove = (DataMove) cmd.data;
    if (theMove.xDefined == true)
      machX = theMove.xValue;
    if (theMove.yDefined == true)
      machY = theMove.yValue;
    if (theMove.zDefined == true)
      machZ = theMove.zValue;
  //  if (theMove.fDefined == true)
  //    feedRate = theMove.fValue;
  }

  private void noteArcMachCoords(Statement cmd) {
    
    // Note where the tool goes due to the given arc move.
    // If this is called when in CC mode, the arc is assumed
    // to have been adjusted already.
    DataCircular theMove = (DataCircular) cmd.data;
    if (theMove.xDefined == true)
      machX = theMove.X;
    if (theMove.yDefined == true)
      machY = theMove.Y;
    if (theMove.zDefined == true)
      machZ = theMove.Z;
  //  if (theMove.F > 0.0)
  //    feedRate = theMove.F;
  }

  private void noteLineUserCoords(Statement cmd) {
    
    DataMove theMove = (DataMove) cmd.data;
    if (theMove.xDefined == true)
      userX = theMove.xValue;
    if (theMove.yDefined == true)
      userY = theMove.yValue;
    if (theMove.zDefined == true)
      userZ = theMove.zValue;
  //  if (theMove.fDefined == true)
  //    feedRate = theMove.fValue;
  }

  private void noteArcUserCoords(Statement cmd) {
    
    DataCircular theMove = (DataCircular) cmd.data;
    if (theMove.xDefined == true)
      userX = theMove.X;
    if (theMove.yDefined == true)
      userY = theMove.Y;
    if (theMove.zDefined == true)
      userZ = theMove.Z;
  //  if (theMove.F > 0.0)
  //    feedRate = theMove.F;
  }

  private void noteMachCoords(Statement cmd) {
    
    // If the given statement is a move (line or arc), then note
    // the terminal coordinates in machX/Y/Z. Ignore it if it's
    // not a move.
    if (cmd.type == Statement.MOVE)
      noteLineMachCoords(cmd);
    else if (cmd.type == Statement.G02)
      noteArcMachCoords(cmd);
    else if (cmd.type == Statement.G03)
      noteArcMachCoords(cmd);
  }

  private void noteUserCoords(Statement cmd) {
    
    // As with mach coordintes: userX/Y/Z instead of machX/Y/Z.
    if (cmd.type == Statement.MOVE)
      noteLineUserCoords(cmd);
    else if (cmd.type == Statement.G02)
      noteArcUserCoords(cmd);
    else if (cmd.type == Statement.G03)
      noteArcUserCoords(cmd);
  }

  private void setTerminalExtraMotion(Statement cmd,double newValue) {
    
    // Set the terminal value of the "extra" coordinate (i.e., Z when in the XY-plane)
    // to the given newValue in the given cmd. This is to handle the situation where
    // a move has been trimmed at an intersection point, but the move involved a
    // change in this extra coordinate. In such a case, the move in the extra
    // direction should go the full distance. See performOffset(), where this is called.
    //
    // Note that as of v09, which does not allow oddball places with CC, I no
    // longer have to worry about the axis.
    if (cmd.type == Statement.MOVE)
      {
        DataMove data = (DataMove) cmd.data;
        if (data.zDefined == true)
          data.zValue = newValue;
      }
    else
      {
        // Must be an arc move.
        DataCircular data = (DataCircular) cmd.data;
        if (data.zDefined == true)
          data.Z = newValue;
      }
  }

  private CartCoord getOffsetStart(Statement cmd) throws Exception {

    // If cmd is either a linear move or an arc move, this returns the
    // (absolute) position of the location of the beginning of the line
    // after taking cutter comp into account. The move is done using
    // the cutter comp settings in this.ccLeft, etc.
    // This is only used when CC is getting underway, when cmd is the
    // first move after a G41 or G42, so there is no previous move to
    // worry about.
    CartCoord answer = new CartCoord();
    
    if (cmd.type == Statement.MOVE)
      {
        LineCurve theLine = 
            new LineCurve(machX,machY,machZ,(DataMove) cmd.data);
        theLine.offset(ccLeft,toolRadius);
        answer.x = theLine.x0;
        answer.y = theLine.y0;
        answer.z = theLine.z0;
      }
    else
      {
        // This could throw if the center/radius are weird.
        ArcCurve theArc = 
            new ArcCurve(machX,machY,machZ,MachineState.curAxis,cmd);
        theArc.offset(ccLeft,toolRadius);
        answer.x = theArc.x0;
        answer.y = theArc.y0;
        answer.z = theArc.z0;
      }
      
    return answer;
  }

  private void performOffset(Statement nextMove) throws Exception {
    
    // Adjust privateBuffer, which must point to a line or arc move, for CC, with
    // nextMove as the move that follows it. As a side-effect, this may set
    // spareStatement to be a bridging move so that the tool is ready to start
    // cutting nextMove -- check whether spareStatement.type is equal to
    // Statement::NIL. The contents of nextMove are left unchanged.
    //
    // If nextMove is NULL, then there is no following move; for instance, the
    // user might have cancelled CC or we ran off the end of the program. In such
    // a case, the line or arc is pushed over by the tool radius, and the path
    // ends with the tool tangent to the end of the path.
    //
    // It is the caller's responsibility to track any changes to userX/Y/Z
    // and machX/Y/Z. This method doesn't change those values, but it does
    // assume that they are correct, meaning that they are up to date --
    // up to just before privateBuffer.
    //
    // This throws because the constructor for ArcCurve throws, and the 
    // exceptions are not caught here. The recovery from these errors would
    // be pretty bad in terms of the geometry. I make no attempt to keep the
    // userX/Y/Z or machX/Y/Z values updated when we see one of these problems.
    // Actually, that's less of an issue here, but it may be an issue at
    // higher levels.
    
    // Begin by forcing spareStatement to be "empty" since we may not
    // need it. Not sure why the compiler warns that this "has no effect."
    // It is certainly necessary. Instead of looking at this warning all the time,
    // I put this statement in a couple of places below.
    //spareStatement.type == Statement::NIL;
    
    // First, convert the two statements to ToolCurve objects. That's
    // where the code is that we need.
    ToolCurve curve1 = null;
    ToolCurve curve2 = null;
    
    if (privateBuffer.type == Statement.MOVE)
      curve1 = new LineCurve(userX,userY,userZ,(DataMove) privateBuffer.data);
    else
      curve1 = new ArcCurve(userX,userY,userZ,MachineState.curAxis,privateBuffer);
    
    if (nextMove != null)
      {
        // Note that curve2 starts where curve1 ends.
        if (nextMove.type == Statement.MOVE)
          curve2 = new LineCurve(curve1.x1,curve1.y1,curve1.z1,(DataMove) nextMove.data);
        else
          curve2 = new ArcCurve(curve1.x1,curve1.y1,curve1.z1,MachineState.curAxis,nextMove);
      }
    
    // Offset the two curves to take cutter comp into account.
    curve1.offset(ccLeft,toolRadius);
    
    if (inOddballVertCase == true)
      {
        // curve1 must be a vertical line. The last time through this method,
        // we noted where that line should be taken to be in terms of (x,y).
        // See the comments below.
        curve1.x0 = vertOffX;
        curve1.x1 = vertOffX;
        curve1.y0 = vertOffY;
        curve1.y1 = vertOffY;
        inOddballVertCase = false;
      }
    
    if (nextMove != null)
      curve2.offset(ccLeft,toolRadius);
    else
      {
        // We're done -- curve1 is all that's needed.
        // There is no nextMove; we must be leaving CC mode.
        StatementData newGuts = curve1.toState();
        privateBuffer.data = newGuts;
        
        spareStatement.type = Statement.NIL;
        return;
      }
    
    // Look for intersection.
    ParamPair p = new ParamPair();
    boolean foundit = ToolCurve.intersect(p,curve1,curve2,toolRadius);
    
    if (foundit == true)
      {
        // The curves intersect and they need to be trimmed, and *both*
        // curves should be trimmed, even nextMove, even though it's not ready
        // to go into publicBuffer. However, the idea of actively trimming 
        // nextMove doesn't make sense. Move statements specify where a move 
        // finishes, with the understanding that the move starts whereever the 
        // tool happens to be at. So, the previous move is not actually trimmed;
        // there's no point (and really no way to do it).
        curve1.trimEnd(p,curve2,toolRadius);
        
        // Replace the StateData (a MoveState or CircularState) in privateBuffer
        // with new StateData which is consistent with curve1. To do this, we 
        // must break const-ness.
        StatementData newGuts = curve1.toState();
        privateBuffer.data = newGuts;
        
        // There's a special case. If we are in the XY-plane, and curve1 involved
        // a change in the z-coordinate, then trimming curve1 has the effect of
        // stopping the z-motion before it's done. We don't want that. All motions
        // in the "extra" plane should go the full distance.
        setTerminalExtraMotion(privateBuffer,curve2.z0);
        
        spareStatement.type = Statement.NIL;
      }
    else
      {
        // No intersection of the curves, so we need a bridging move.
        // The first move (after the offset) isn't changed, but the guts
        // of privateBuffer must be changed to take the offset into account.
        // So, begin by adjusting the move before nextMove, in privateBuffer
        // to be along the offset version of curve1.
        StatementData newGuts = curve1.toState();
        privateBuffer.data = newGuts;
        
        // The bridge move (or "pivot"). Basically, we need these when the angle between
        // the user-moves (i.e., the moves without considering CC) lead to
        // a situation where the cutter must move more than 180 degrees around
        // a corner. If the move is less than 180, then there will be an
        // intersection.
        //
        // The bottom line is that the first move will end with the tool tangent
        // to the user-curve at its end-point, and we want to start the next move
        // with the tool tangent at that curve's start-point. The way to bridge
        // the two is with a pivot -- a small arc move.
        // We must specify whether this is a G02 (CW) or G03 (CCW) move. Knowing
        // this is easier than it seems. Draw some pictures. If the cutter is to the
        // left, then it's a CW move; and it's a CCW move if the cutter is to the right.
        // 
        // Let ToolCurve handle this since the code is already there.
        // This move is "extra," so there's no need for its own feed rate. It will
        // use the feed rate in force during the previous move.
        //
        // HOWEVER, there is a special case: nextMove may be a move strictly in the 
        // Z-direction with no changes in the (x,y) values. In this case there's
        // no need for a bridging move, but we do need to make some adjustments.
        // The tool will move to the end of the offset curve1 (as usual).
        // Then we must change nextMove to be offset to this position.
        // nextMove is a plunging or retracting move, so calling LineCurve.offset()
        // will leave it unchanged. Thus, the next time through here, when we are
        // trying to go from nextMove to the move that follow it, there will be
        // no way to know in what way *this* call to performOffset() treated nextMove.
        // See below, where the case is handled, for further comments.
        //
        // So, check whether we are in the special case where nextMove does not
        // change X or Y.
        boolean specialCase = false;
        if (nextMove.type == Statement.MOVE)
          {
            // curve2 must be a LineCurve, and we might be in the special case.
            if ((curve2.x0 == curve2.x1) && (curve2.y0 == curve2.y1))
              // Yes, only the z-coordinate changes and we are in the special case.
              specialCase = true;
          }
        
        if (specialCase == true)
          {
            // Again, there is no pivot, but we must adjust nextMove.
            // This is a quandry...When nextMove is seen the next time through this 
            // method, it will seen to be a vertical tool motion with (x,y) at the
            // location of user (not machine) coordinates. That is the KEY DIFFICULTY --
            // curves, before being offset, always start at userX,userY. Since these
            // are vertical lines, calling LineCurve.offset() will do nothing.
            // The only solution seems to a new class variable (used by this method
            // only). That way, when I see a vertical line the first time
            // this method is called (in privateBuffer), I can note that fact along 
            // with the (x,y) offset that we used for that vertical line. Then, the
            // next time we hit this method, we will know how to treat the vertical 
            // line (which is now in nextMove). That's what Layer05.vertOffX and
            // Layer05.vertOffY are for.
            //
            // So, note the (x,y) coordinates at which curve1 ends. These are where
            // nextMove must be considered to be the next time we see it.
            this.vertOffX = curve1.x1;
            this.vertOffY = curve1.y1;
            inOddballVertCase = true;
            
  //          qDebug() << "set vert oddball to " << vertOffX << " and " << vertOffY;
  //          qDebug() << "curve1 was " << privateBuffer.toString();
  //          qDebug() << "curve2 was " << nextMove.toString();
            
            
            
            
            
  //          MoveState* data = (MoveState*) nextMove.data;
  //          if (data.xDefined == true)
  //            data.xValue = curve1.x1;
  //          if (data.yDefined == true)
  //            data.yValue = curve1.y1;
            
            // This is no pivot move.
            spareStatement.type = Statement.NIL;
          }
        else
          {
            // Normal situation with an arc pivot.
            // In rare cases you could have the end of curve1 and the start or curve2
            // already coincident, in which case we don't need to do this.
            if ((curve1.x1 == curve2.x0) && (curve1.y1 == curve2.y0))
              spareStatement.type = Statement.NIL;
            else
              {
                ArcCurve pivot = new ArcCurve(curve1.x1,curve1.y1,curve1.z1,
                    curve2.x0,curve2.y0,curve2.z0,
                    MachineState.curAxis,ccLeft,toolRadius,-1.0);
                
                pivot.toStatement(spareStatement);
                
                // Move the pivot into spareStatement.
                if (ccLeft == true)
                  spareStatement.type = Statement.G02;
                else
                  spareStatement.type = Statement.G03;
                
//                if (spareStatement.data != null)
//                  delete spareStatement.data;
                spareStatement.data = pivot.toState();
                
                // Set the line number for this jogging move equal to the line number
                // of the previous move. Usually this is the object in privateBuffer,
                // but things like G17 could also be in publicBuffer. What we want is to
                // repeat the line number in the tail item of publicBuffer. In fact,
                // it's not so clear what the right thing to do is since one could argue
                // that the pivoting move "belongs to" the move in privateBuffer.
                // I chose to do it this way because it is disconcerting to see the line
                // numbers decline. A better solution would be to change the order
                // of the items in publicBuffer so that the pivot occurs immediately
                // after the move it comes after, but that's a pain.
                //spareStatement.lineNumber = privateBuffer.lineNumber;
                //spareStatement.lineNumber = publicBuffer.last().lineNumber;
                spareStatement.lineNumber = publicBuffer.peekLast().lineNumber;
              }
          }
      }
    
//    delete curve1;
//    delete curve2;
  }

  private void startCCMode(Statement enterCCStatement) throws Exception {
    
    // populateBuffer() just read a G41 or G42 (which is in 
    // enterCCStatement) and the publicBuffer must be empty. Read statements and 
    // post them to publicBuffer until we hit a move. When we do hit a move, 
    // adjust the G41/42 to be the jog needed to bring the tool to the start
    // position, and sit on this move in privateBuffer.
    // Note that we could run off the end of the program here if there
    // are *no* moves after the G41/42. Another special case is if the
    // user turns off CC (G40) before any moves.
    this.cutterComp = true;
    if (enterCCStatement.type == Statement.G41)
      this.ccLeft = true;
    else
      this.ccLeft = false;
    
    DataRegister reg = (DataRegister) enterCCStatement.data;
    if (reg.D == true)
      // Look in D register for diameter.
      this.toolRadius = MachineState.turret.toolDiameter[reg.regValue] / 2.0;
    else
      // Look in H register for diameter.
      this.toolRadius = MachineState.turret.hRegister[reg.regValue] / 2.0;
    
    // Note the tool location just before CC mode begins.
    userX = machX;
    userY = machY;
    userZ = machZ;
    
    // Start building up the publicBuffer. This will be changed into a tool jog.
//    publicBuffer.enqueue(enterCCStatement);
    publicBuffer.add(enterCCStatement);
    
    Statement cmd = lowerLayer.nextStatement();
    while ((cmd.type != Statement.MOVE) && 
           (cmd.type != Statement.G02) && 
           (cmd.type != Statement.G03))
      {
        // cmd is not a move (line or arc).
        if (cmd.type == Statement.EOF)
          {
            // Special case: ran off the end of the program without any moves at all.
            // Get rid of the G41/42 (enterCCStatement) from the buffer since
            // we won't be changing it to a tool jog, then put the EOF into the buffer.
            this.cutterComp = false;
            //(void) publicBuffer.dequeue();
            Statement trash = publicBuffer.remove();
            //publicBuffer.enqueue(cmd);
            publicBuffer.add(cmd);
            return;
          }
        
        if (cmd.type == Statement.G40)
          {
            // Special case: user cancelled CC before doing any moves.
            // This is kind of a pain. We shouldn't report the original
            // G41 or G42 in enterCCStatement, and we shouldn't report
            // this G40 either.
            // So, remove the enterCCStatement, and note that we are 
            // not in CC mode after all.
            this.cutterComp = false;
            //(void) publicBuffer.dequeue();
            Statement trash = publicBuffer.remove();
            return;
          }
        
        if ((cmd.type == Statement.G41) || (cmd.type == Statement.G42))
          // It's OK to proceed after this error. Effectively the statement will do nothing.
          changeToError(cmd,"already in cutter comp mode");
        
        // Ordinary situation.
        //publicBuffer.enqueue(cmd);
        publicBuffer.add(cmd);
        
        // Don't let the user apply G18 or G19 now that we are in CC mode.
        if ((cmd.type == Statement.G18) || (cmd.type == Statement.G19))
          // Again, it's OK to proceed.
          changeToError(cmd,"not allowed to use cutter comp with G18 or G19");
        
        cmd = lowerLayer.nextStatement();
      }
    
    // Got here so cmd is a move (line or arc). We transform the original enterCCStatement
    // to be a tool jog to begin CC mode, and then sit on the current cmd, 
    // without changing it, until we see another move statement.
    CartCoord startPt;
    try {
      startPt = getOffsetStart(cmd);
    } catch (Exception e) {
      // cmd must be a geometrically bogus arc move.
      changeToError(cmd,e.getMessage());
      
      // Go ahead and sit on cmd so that we can try to proceed.
      privateBuffer = cmd;
      return;
    }
    
    // We must break the const-ness of enterCCStatement to change it's meaning.
    // This statement is already in publicBuffer; we are just changing its
    // meaning.
    //Statement *breakit = (Statement*) enterCCStatement;
    enterCCStatement.type = Statement.MOVE;
//    if (breakit.data != NULL)
//      delete breakit.data;
    DataMove theMove = new DataMove();
    enterCCStatement.data = theMove;
    
    theMove.xDefined = true;
    theMove.xValue = startPt.x;
    theMove.yDefined = true;
    theMove.yValue = startPt.y;
    theMove.zDefined = true;
    theMove.zValue = startPt.z;
    
    theMove.fDefined = false;
    
    // The user coordinates haven't changed, but the jog move does move the tool.
    machX = startPt.x;
    machY = startPt.y;
    machZ = startPt.z;
    
    // Sit on the move we just read.
    privateBuffer = cmd;
  }

  private void populateBuffer() throws Exception {
    
    // This is like nextStatement() in the other layers, except that it
    // puts the resulting statements into publicBuffer.
    
    if (this.cutterComp == false)
      {
        // Not currently in CC mode. Just read statements, posting them to the buffer
        // until we hit a G41 or G42 to enter CC mode. If we do hit a G41 or G42, then
        // don't post it to the publicBuffer. But do continue reading and posting
        // statements until we hit the first move. At that point, we can post a jog
        // to bring the tool to where that move starts, but we sit on that initial
        // move in the privateBuffer. We do have to be a little careful because
        // the user might (stupidly) turn on CC, then turn it off without there
        // being any intervening moves.
        //
        // Also, when you hit a G41 or G42, we will need to go back and adjust
        // the contents of that statement to make it into the jog needed to
        // start the first move.
        Statement answer = null;
        
        // This loop is confusing. Bear in mind that most of the time this will
        // only put a single statement into publicBuffer. The only exception
        // is when a G41/42 is reached and we are entering CC mode.
        while (answer == null)
          {
            Statement cmd = lowerLayer.nextStatement();
            
            if (cmd.type == Statement.G40)
              // User cancelled CC, and we're not in CC mode! There's no
              // harm in this, but we must not pass it to the higher layer.
              continue;
            
            if ((cmd.type != Statement.G41) && (cmd.type != Statement.G42))
              {
                // The cmd has nothing to do with CC. Just pass it through.
                answer = cmd;
                
                // Before continuing, note the tool position and any changes to the axes.
                noteMachCoords(cmd);
                
                if (cmd.type == Statement.G17)
                  MachineState.curAxis = AxisChoice.XY;
                else if (cmd.type == Statement.G18)
                  MachineState.curAxis = AxisChoice.ZX;
                else if (cmd.type == Statement.G19)
                  MachineState.curAxis = AxisChoice.YZ;
                
                // answer is the next item to be posted to publicBuffer.
                break;
              }
            
            // Got here, so it must be a G41 or G42 and we are entering CC mode.
            // Make sure we are in the XY-plane.
            if (MachineState.curAxis != AxisChoice.XY)
              changeToError(cmd,"must be in the XY-plane to use cutter comp");
            else
              // We are in the XY-plane, so go ahead and start CC mode.
              startCCMode(cmd);
            
            // One weird special case: if the user says G41 or G42, then follows 
            // immediately with a G40 to cancel, then the public buffer is 
            // *still* empty and this loop must continue. So, don't break out
            // of this loop unless there really are statements ready in 
            // publicBuffer.
            if (publicBuffer.isEmpty() == false)
              break;
          }
        
        if (answer != null)
          // If answer is NULL, then we must have been dealing with a 
          // G41 or G42; othewise we're in the normal situation and we
          // need to note the statement just read.
//          publicBuffer.enqueue(answer);
          publicBuffer.add(answer);
        
        return;
      }
    
    // Got here, so we know that CC is on, and that there is a move statement
    // in privateBuffer. That statement was the one most recently read, and we need
    // another move (line or arc) so that we can adjust the first move. The
    // next call to lowerLayer.nextStatement() will give us the statment that
    // follows the one in privateBuffer. 
    //
    // First, we post the statement in privateBuffer to the public buffer. 
    // Then we read statements, posting them to public buffer, until we hit the
    // next move. Then we adjust the move in privateBuffer, to take CC into account,
    // perhaps adding a spare move (for going around corners). We put this just-read
    // move into the private buffer, and return.
    //
    // Eventually we will see a G40 (CC cancel). We just need to jog the tool out
    // to finish the move that's sitting in privateBufer.
    //
    // One tricky thing: the program might *end* while in CC mode. This could be
    // handled as though there *is* a G40 just before the program ends, complete
    // with the resulting tool jog. This jog isn't necessary (what's the point?),
    // but we do have to be looking for an EOF, even while in CC mode.
    
    // Put privateBuffer into publicBuffer, knowing that we will modify this
    // later.
    //publicBuffer.enqueue(privateBuffer);
    publicBuffer.add(privateBuffer);
    
    // Read statements, looking for G40 (CC cancel) or the next move,
    // putting any intermediate statements in publicBuffer.
    Statement nextMove = null;
    
    while (nextMove == null)
      {
        Statement cmd = lowerLayer.nextStatement();
        
        if (cmd.type == Statement.G40)
          {
            // CC cancel. There is no "next move."
            
            // When this process is complete, the machX/Y/Z will be equal to the
            // terminal coordinate of the first move (in privateBuffer), as 
            // given by the user, without the CC adjustment. We need to note
            // where the tool will go before the final move (in privateBuffer) 
            // is adjusted for CC.
            DataMove theMove = (DataMove) privateBuffer.data;
            double postX = userX;
            double postY = userY;
            double postZ = userZ;
            if (theMove.xDefined) postX = theMove.xValue;
            if (theMove.yDefined) postY = theMove.yValue;
            if (theMove.zDefined) postZ = theMove.zValue;
            
            // Push the previous move (in privateBuffer) out by the tool radius,
            // adjust the contents of cmd to jog the tool back to it's non-CC 
            // position, post that to publicBuffer and return.
            try {
              performOffset(null);
            } catch (Exception e) {
              // This will throw if the move in privateBuffer is a geometrically bad arc.
              // I'm not sure this could happen since this was probably caught earlier
              // when this cmd was the "next move."
              changeToError(privateBuffer,e.getMessage());
              
              // Continuing with what remains should be fine.
            }
            
            // performOffset() takes care of modifying the first move (in 
            // privateBuffer). Now modify the current statement, changing it 
            // from a G40 to a MOVE that jogs the tool back to machine coordinates.
            // To do this, we must break const-ness. This is why we needed
            // postX/Y/Z.
//            Statement *breakit = (Statement*) cmd;
            cmd.type = Statement.MOVE;
//            if (breakit.data != NULL)
//              delete breakit.data;
            theMove = new DataMove();
            cmd.data = theMove;
            
            // We move to the machine coordinates, as noted above.
            theMove.xDefined = true;
            theMove.xValue = postX;
            theMove.yDefined = true;
            theMove.yValue = postY;
            theMove.zDefined = true;
            theMove.zValue = postZ;
            theMove.fDefined = false;
            
            // And note the ultimate machine coords.
            machX = postX;
            machY = postY;
            machZ = postZ;
            
            //publicBuffer.enqueue(cmd);
            publicBuffer.add(cmd);
            
            this.cutterComp = false;
            
            return;
          }
        
        // Not a CC cancel. Make sure the user isn't trying to
        // enter CC mode while already in CC mode.
        if ((cmd.type == Statement.G41) || (cmd.type == Statement.G42))
          changeToError(cmd,"already in cutter comp mode");
        
        // Not allowed to use G18 or G19 in CC mode.
       if ((cmd.type == Statement.G18) || (cmd.type == Statement.G19))
         changeToError(cmd,"G18 and G19 are not allowed with cutter comp");
        
        if (cmd.type == Statement.EOF)
          {
            // This is much like cancelling, although we don't bother with the
            // bookkeeping since the program ends.
            try {
              performOffset(null);
            } catch (Exception e) {
              changeToError(privateBuffer,e.getMessage());
            }
  
            return;
          }
        
        if ((cmd.type == Statement.MOVE) || 
            (cmd.type == Statement.G02) ||
            (cmd.type == Statement.G03))
          // Found nextMove, which will end the loop.
          nextMove = cmd;
          
        // If nextMove is still NULL, then cmd was not a move and
        // should be passed through to publicBuffer.
        if (nextMove == null)
          //publicBuffer.enqueue(cmd);
          publicBuffer.add(cmd);
      }
    
    // We now have nextMove. Adjust privateBuffer, which points to the previous 
    // move, to take CC into account, then make privateBuffer point to nextMove.
    // We may need to add a "bridge" move to connect these two moves.
    
    // Before adjusting the first move (in privateBuffer), note where the move
    // ends up in user coordinates. We can't change userX/Y/Z yet because
    // performOffset needs the unchanged values.
    double preX = userX;
    double preY = userY;
    double preZ = userZ;
    if (privateBuffer.type == Statement.MOVE)
      {
        DataMove theMove = (DataMove) privateBuffer.data;
        if (theMove.xDefined) preX = theMove.xValue;
        if (theMove.yDefined) preY = theMove.yValue;
        if (theMove.zDefined) preZ = theMove.zValue;
      }
    else
      {
        // Must be an arc move.
        DataCircular theMove = (DataCircular) privateBuffer.data;
        if (theMove.xDefined) preX = theMove.X;
        if (theMove.yDefined) preY = theMove.Y;
        if (theMove.zDefined) preZ = theMove.Z;
      }
    
    // This function will adjust privateBuffer, and it may produce a
    // bridging move in spareStatement.
    try {
      performOffset(nextMove);
    } catch (Exception e) {
      // One of the two moves, privateBuffer or nextMove, is an arc that
      // is geometrically bogus. Generally, it will be nextMove, but go ahead
      // and check if only one of the two is an arc.
      if (nextMove.type == Statement.MOVE)
        // Not sure how, but it must be privateBuffer that's the problem.
        changeToError(privateBuffer,e.getMessage());
      else
        changeToError(nextMove,e.getMessage());
        
      // It should be OK to continue with the rest of this method.
    }
    
    // Note changes in tool position due to the first move in privateBuffer.
    // This modifies machine coordinates, not user coordinates.
    noteMachCoords(privateBuffer);
    
    // And change the user coordinates to the values we saved.
    userX = preX;
    userY = preY;
    userZ = preZ;
    
    if (spareStatement.type != Statement.NIL)
      {
        // Yes, there is a bridging move. Add it to the buffer.
        // This does not go immediately after the first move, but that's
        // OK becuase any intervening statements don't affect the tool position.
        // As long as it goes before the nextMove, that's fine.
        //publicBuffer.enqueue(&spareStatement);
        publicBuffer.add(spareStatement);
        
        // Note any changes to the tool position in machX/Y/Z too.
        // This doesn't affect user coordinates.
        noteMachCoords(spareStatement);
      }
    
    // All done. Move nextMove into the privateBuffer so that it's
    // ready for the next time around.
    privateBuffer = nextMove;
  }

  public Statement nextStatement() throws Exception {
    
    // Read and translate statements to be passed up to the next layer.
    
    //  For the record, the only statement types we should see here are:
    //  Statement::SError
    //  Statement::EndOfFile
    //  Statement::MOVE
    //  Statement::G00
    //  Statement::G01
    //  Statement::G02
    //  Statement::G03
    //  Statement::G17
    //  Statement::G18
    //  Statement::G19
    //  Statement::G40
    //  Statement::G41
    //  Statement::G42
    //  Statement::M03
    //  Statement::M04
    //  Statement::M05
    //  Statement::M06
    
    // Make sure that there's something in the publicBuffer to return.
    if (publicBuffer.isEmpty() == true)
      populateBuffer();
    
    // We now know that there's at least one statement ready in
    // the buffer.
    //const Statement* answer = publicBuffer.dequeue();
    Statement answer = publicBuffer.remove();
    return answer;
  }

  public void reset() {
    
    // Not sure when this would be necessary, but just to be safe, empty the buffer.
    while (publicBuffer.size() > 0)
      {
        //(void) publicBuffer.dequeue();
        Statement trash = publicBuffer.remove();
      }
    
    machX = X0;
    machY = Y0;
    machZ = Z0;
    
    feedRate = -1.0;
    MachineState.curAxis = AxisChoice.XY;
    spareStatement.type = Statement.NIL;
    privateBuffer = null;
    cutterComp = false;
    ccLeft = true;
    toolRadius = -1.0;
    inOddballVertCase = false;
      
    lowerLayer.reset();
  }
  
  
}



