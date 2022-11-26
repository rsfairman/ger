package vcnc.transpile;

/*
Lookup table that maps program numbers to lines (character counts, actually) 
of the the source file. When a program is loaded, the first thing that the
interpreter does is generate this dictionary in case any of the programs call
a subprogram.
*/

import java.util.Hashtable;


public class ProgramDict {
	
	// The keys here are the program numbers that appear with an O-statement.
	// The value consists of two integers. First is the character number where 
  // the "O" of O[X] appears. Second is the line number on which it appears.
  // The line number is needed for error reporting.
	Hashtable<Integer,int[]> dict = new Hashtable<Integer,int[]>();
}
