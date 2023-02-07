package vcnc.tpile.parse;

/*

More complex statement objects require additional data. This is the super-class
from which these all come. This is just a place-holder so that the type-checker 
of the Java compiler doesn't choke.

*/

abstract public class StatementData {
  
  // I am not happy about this. but a deep copy of Statements is needed
  // when dealing with sub-programs.
  abstract public StatementData deepCopy();

}
