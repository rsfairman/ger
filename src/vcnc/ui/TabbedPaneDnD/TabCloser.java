package vcnc.ui.TabbedPaneDnD;

/*

Used like an ActionEvent, but for closing tabs. The idea is that a tab
may need to know when it's going to be closed. In fact, "tab" is too limited
a word; it could by anything that wants to know. 

BUG: Get rid of...doesn't work.

*/

public interface TabCloser {

  public void doClose();
}
