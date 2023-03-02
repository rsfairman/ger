package ger.graphics;

/*

Used for cartesian coordinates after quantizing based on some scale.

BUG: There's also the CartCoord class. Maybe I should use a template.

*/

public class IntCoord {

  public int x = 0;
  public int y = 0;
  public int z = 0;
  
  public IntCoord(int x,int y,int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
}
