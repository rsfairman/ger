package vcnc.transpile;

// Use this to create a buffered interface to the contents of a JTextArea.
// It gets a block of bytes, and holds them so that they can be given out
// one at a time with getc() and/or peekc().

import javax.swing.JTextArea;


public class TextBuffer {
  
  private TextGetter theGetter = null;
  
  // The user can't jump around in the buffer, but this keeps track of which
  // character has most recently been read so that the value can be reported.
  private int mark = 0;
  
  // These are the buffers. There are two to allow look-ahead.
  private char[] bufOne = null;
  private char[] bufTwo = null;
  
  // Index pointing to the next character of bufOne that should be returned
  // by getc().
  private int bufIndex = 0;
  
  // The number of characters to read at a time. The only obvious external affect
  // of this value is the number characters that the user is allowed to look
  // ahead with peekc(). It should also be large enough to avoid having to
  // refresh the buffers too often.
  private int biteSize = 1000;
  
  
  public TextBuffer() {
    // Do nothing
  }
  
  public TextBuffer(JTextArea theText) {
    this.theGetter = new TextGetter(theText);
    this.mark = 0;
    this.bufTwo = theGetter.read(this.biteSize);
    bufferUpdate();
  }
  
  public TextBuffer(String theText) {
    this.theGetter = new TextGetter(theText);
    this.mark = 0;
    this.bufTwo = theGetter.read(this.biteSize);
    bufferUpdate();
  }
  
  public TextBuffer spinOff(int n) {
    
    // Create a new TextBuffer based on the same underlying TextGetter, and
    // make it start on the n-th character.
    TextBuffer answer = new TextBuffer();
    answer.theGetter = theGetter.spinOff(n);
    answer.mark = n;
    this.bufTwo = theGetter.read(this.biteSize);
    bufferUpdate();
    
    return answer;
  }
  
  public void reset() {
    
    // Make the buffer start over at character zero.
    reset(0);
  }
  
  public void reset(int n) {
    
    // Make the buffer start over at character n.
    theGetter.setMark(n);
    this.mark = n;
    this.bufTwo = theGetter.read(this.biteSize);
    bufferUpdate();
  }
  
  private void bufferUpdate() {
    this.bufOne = this.bufTwo;
    this.bufTwo = theGetter.read(this.biteSize);
    this.bufIndex = 0;
  }
  
  public char getc() throws Exception {
    
    // Return the next character from the buffer. Throw Exception when there are
    // no more characters.
    if (bufOne == null)
      throw new Exception();
    
    char answer = bufOne[bufIndex];
    ++bufIndex;
    ++mark;
    
    if (bufIndex >= bufOne.length)
      bufferUpdate();
    
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
    
    // Return the next character, but don't update bufIndex. As above.
    if (bufOne == null)
      throw new Exception();
    return bufOne[bufIndex];
  }
  
  public char peekc(int n) throws Exception {
    
    // Return the n-th character after the most recently read character.
    // So, peekc(0) is the same as peekc(). This is a tad more complicated 
    // since it may be necessary to look at bufTwo.
    if (bufOne == null)
      throw new Exception();
    
    // See if n is large enough to require that we look at bufTwo.
    if (n + bufIndex >= bufOne.length)
      {
        // Must look at bufTwo.
        if (bufTwo == null)
          throw new Exception();
        
        if (n + bufIndex - biteSize >= bufTwo.length)
          // n is too large. We can't read that far ahead.
          throw new Exception();
        
        return bufTwo[n+bufIndex-biteSize];
      }
    else
      // Answer in bufOne.
      return bufOne[n+bufIndex];
  }
  
}



