package ger.tpile;

/*

Use this to create a buffered interface to a String. The built-in StringBuffer 
class sounds like the thing to use, making this redundant, but StringBuffer
has a *mutable* sequence of characters. The built-in StringReader class is 
almost what I want, but that class has no peek() method for looking at the 
next character without moving the mark. In theory, StringReader could be used 
by messing with the mark, but the class below is just as easy.

Note: an earlier version of this used a pair of character buffers between
the String and the getting/peeking. That's complicated, so I changed to
using String.charAt(), which is much simpler. The downside is that it may
be that charAt() is slower than getting larger blocks of characters from
the String. It depends on how Java defines Strings internally
and how charAt() is implemented. Realistically, this shouldn't make a big
difference either way, so go with the shorter/simpler code.

NOTE: I'm not keen on this class. There should be a better way.

*/


public class CodeBuffer {
  
  private String theText = null;
  
  // The user can't jump around in the buffer, but this keeps track of which
  // character has most recently been read so that the value can be reported.
  private int mark = 0;
  
  
  public CodeBuffer(String theText) {
    
    this.theText = theText;
    this.mark = 0;
  }
  
  public void reset() {
    
    // Make the buffer start over at character zero.
    reset(0);
  }
  
  public void reset(int n) {
    
    // Make the buffer start over at character n.
    this.mark = n;
  }
  
  public char getc() throws Exception {
    
    // Return the next character from the buffer. Throw Exception when there are
    // no more characters.
    char answer = theText.charAt(mark);
    ++mark;
    
    return answer;
  }
  
  public int getLastCharIndex() {
    
    // Return the count to the most recently read character. This is to the
    // most recent character that was *read*; peeking doesn't count.
    // Put another way, this returns the index (counting from 0) of the next
    // character to be read.
    return mark;
  }
  
  public char peekc() throws Exception {
    
    // Return the next character, but don't update the mark. As above.
    return theText.charAt(mark);
  }
  
  public char peekc(int n) throws Exception {
    
    // Return the n-th character after the most recently read character.
    // So, peekc(0) is the same as peekc(). 
    return theText.charAt(mark + n);
  }
  
}



