package ger.tpile;

/*

The various codes known to the program.

*/


public enum SType {
  
  // Housekeeping codes.
    UNKNOWN // Occasionally used when allocating 
  , ERROR   // Problem to be reported to user
  
  // These are filtered out very early when sub-programs are dealt with,
  // at the Translator.ThruSubProgs stage.
  //
  // BUG: I can imagine (?) wizards wanting a "terminate program" option.
  // M30 is off the table, but we could provide something else -- sort
  // of a "chop the WIP off here in the translator." Basically, call
  // LList.truncateAt() since "chopping off" is how the program terminates.
  , EOF  // Used at an early stage for end-of-file 
  , PROG // An O-code, like O1234
  , M98  // Call subprogram
  , M99  // Return from subprogram
  , M30  // End of program
  
  // Dealt with at the Translator.ThruWizards stage.
  , WIZARD
  
  // Dealt with at the Translator.ThruUnits stage.
  , G20  // Inches
  , G21  // Millimeters
  
  // Dealt with at the Translator.ThruWorkOffsets stage.
  , G52  // Temporary change in PRZ
  , G54  // Work offsets
  , G55  
  , G56  
  , G57
  , G58
  , G59
  , G92  // another way to change PRZ
  
  // Dealt with at the Translator.ThruPolar stage.  
  , G15  // Polar coordinates off
  , G16  // Polar coordinates on
  
  // Dealt with at the Translator.ThruIncremental stage.
  , G90  // Absolute mode
  , G91  // Incremental mode
  
  // These pass all the way through, perhaps with modification, to the fully
  // translated result.
  // Remember: G00 and G01 merely change the travel mode; MOVE moves the cutter.
  // Also, I keep wanting to filter away G17/18/19, but they matter when it
  // comes time to interpret G02 and G03.
  , MOVE  // tool move
  , G00   // rapid mode 
  , G01   // normal mode
  , G02   // CW circular interpolation
  , G03   // CCW circular interpolation
  , G17   // Choose plane for interpolation
  , G18   //   influences the meaning of G02/03
  , G19
  
  // These also pass through. They have no effect on translation, but
  // could be important on a physical machine or in simulation.
  // 
  // Note that M06 (tool change) is a special case since it *does* affect
  // translation, but only due to cutter comp (and rendering). Also, the TLO 
  // commands G43/44/49 are modified to use machine units if they are explicit 
  // rather than using a register. TLO commands don't "matter," but converting
  // this way is being nice to the user.
  // 
  // The meaning of M00 and M01 seems to vary from machine to machine. They
  // are treated here as "temporary pause." On a real machine, you could put
  // one of these in your code, the program would pause, then you hit some
  // button to continue. Unlike M02, which some people might expect to act
  // like M30, these can be ignored by the translator so they don't hurt to
  // allow in the stream.
  // 
  // The TLO (tool length offset) commands (G43, 44, 49) don't really do 
  // anything in the context of the program; they simply pass through unchanged.
  // They're intended to take into account the physical arrangement of a 
  // physical machine, but it would be too difficult to take that stuff into 
  // account here. In theory, it would be possible for the tool table to specify 
  // various "offsets" that the user's G-code would be expected to compensate
  // for, but that seems like an unnecessary complication. The whole purpose of 
  // TLO is to compensate for something that's irritating about physical
  // machines and the value of a simulation is that you can abstract away 
  // real-world irritations. That said, I can imagine a user who wants the 
  // simulation to take these things into account so that he can "check his 
  // work" before going turning to a physical machine. Allowing for that would 
  // only frustrate a larger number of users.

  , M00   // pause
  , M01   // pause
  , M03   // spindle on, CW 
  , M04   // spindle on, CCW
  , M05   // spindle off
  , M06   // Tool change
  , M07   // Coolant on
  , M08   // Coolant on
  , M09   // Coolant off
  , M40   // Spindle high
  , M41   // Spindle low
  , M48   // Enable feed & speed overrides
  , M49   // Disable overrides
  
  , G43   // TLO, positive.
  , G44   // TLO, negative.
  , G49   // Cancel TLO.
  
  
  
  
  

  
  // Yet to handle...
  

  
  
  , G41   // Cutter comp left.
  , G42   // Cutter comp right.
  , G40   // Cancel cutter comp.
  
  ;
  

}
