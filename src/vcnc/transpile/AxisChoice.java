package vcnc.transpile;

// Use this to keep track of whether G17, G18 or G19 is in force.
//
// I use ZX instead of XZ because it conforms better to the implicit order 
// when you choose the ZX plane in polar coordinate move.

enum AxisChoice { XY, ZX, YZ }
