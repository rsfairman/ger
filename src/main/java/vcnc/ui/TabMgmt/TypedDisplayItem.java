package vcnc.ui.TabMgmt;

/*
Every tab that appears can be one of several types, like G-code or
2.5D rendering or whatever. By default, these tabs are all in the main
window, but the user can pull them out to their own window. Every "displayed
thing" should implement this to make it easier to determine the type of
the thing being displayed. 

I am *really* not happy about this, but see no better way.

*/

public interface TypedDisplayItem  {
  
  // If Java had true multiple inheritance, I would do this:
  // public TabbedType type = TabbedType.UNKNOWN;
  // but that's not permitted. The type variable ends up being static.
  //
  // So, do this as way to remind the user that he must declare the variable
  // explicitly in the class that implements this interface.
  public TabbedType type();
  
}
