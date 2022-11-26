package vcnc.util;

// This exception is given by certain methods in FileIOUtil. For instance,
// when trying to read a csv file, if the entry being read is empty, this
// exception is thrown. For example, if the file is
// A,B,,D
// so that the 'C' column is empty, if you call FileIOUtil.readCharTo(bin,',')
// on the third column, this exception will be thrown.
// Another strategy would be to redefine readCharTo() to take a third parameter
// that would be the char value to return if this field is empty.

public class EmptyReadException extends Exception {
  
  public EmptyReadException(String msg) {
    super(msg);
  }
  
}
