package vcnc;

/*
Holds values that are global to the machine. 

Some of these are static(ish), like the work offsets table, and some change
as the simulation proceeds, like the tool location.

*/

import vcnc.workoffsets.WorkOffsets;

public class MachineGlobals {

  public static WorkOffsets workOffsets = new WorkOffsets();
  
}
