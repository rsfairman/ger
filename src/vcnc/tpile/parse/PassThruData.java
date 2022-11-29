package vcnc.tpile.parse;

/*

Used for G/M-code statements that Ger doesn't know what to do with.
Some of these are clearly unsuitable for interpreting, like M08/M09 for
coolant on/off. In other cases, like G84.1 for drilling, the user may want
to give this a meaning.

*/

import java.util.ArrayList;

import vcnc.tpile.lex.Token;


public class PassThruData extends StateData {
  
  // Because we don't know what to do with these, they're held as nothing
  // more than a sequence of Tokens. The 0-th token on this list is the
  // opening G or M code of the statement.
  public ArrayList<Token> ts = new ArrayList<>(); 

}
