package vcnc.tpile.lex;

/*

Holds buffer of Token objects, as generated by the Lexer.

There are two layers to the buffer. The Parser should always work within the
first layer. When the first layer is exhausted, the second layer is moved to 
the first and the second layer is refreshed. Some kind of buffer of tokens is 
needed because the parser needs to look ahead. I did it this way because it 
will be easier to thread if I ever want to -- although it's so fast on a
modern machine, that seems silly.

*/


class TokenBuffer {
  
  // The number of tokens in each buffer. 100 is probably far more than 
  // necessary, but this is plenty for lookahead, and the amount of memory 
  // used is small.
  public static int BufferSize = 100;
  
  public Token[] first = null;
  public Token[] second = null;
  
  // The next token in this.first to be consumed.
  public int index = 0;
}