package projections.gui.count;

import java.awt.*;

public class Counter {
  public String counterCode = null;
  public String description = null;
  public String fileName = null;
  public Color  color = null;
  private static final Color[] COLOR_LIST = 
    { Color.blue, Color.cyan, Color.darkGray, Color.gray, Color.green, 
      Color.lightGray, Color.magenta, Color.orange, Color.pink, Color.red, 
      Color.white, Color.yellow };
  private static int colorLoc = 0;
  
  public Counter(String code, String desc, String fileName) { 
    counterCode = code; 
    description = desc;
    this.fileName = fileName;
    calcColor();
  }

  private void calcColor() {
    String code = counterCode;
    if      (code.equals("CYCLES0"))      { color = Color.yellow;       } // 0
    else if (code.equals("INSTR"))        { color = Color.blue;         } // 1
    else if (code.equals("LOAD"))         { color = Color.orange;       } // 2
    else if (code.equals("STORE"))        { color = Color.magenta;      } // 3
    else if (code.equals("STORE_COND"))   { color = new Color(90,90,0); } // 4
    else if (code.equals("FAIL_COND"))    { color = new Color(90,90,0); } // 5
    else if (code.equals("DECODE_BR"))    { color = new Color(90,90,0); } // 6
    else if (code.equals("QUADWORDS2"))   { color = new Color(90,90,0); } // 7
    else if (code.equals("CACHE_ER2"))    { color = new Color(90,90,0); } // 8
    else if (code.equals("L1_IMISS"))     { color = new Color(90,90,0); } // 9
    else if (code.equals("L2_IMISS"))     { color = new Color(90,90,0); } // 10
    else if (code.equals("INSTRMISPR"))   { color = new Color(90,90,0); } // 11
    else if (code.equals("EXT_INTERV"))   { color = new Color(90,90,0); } // 12
    else if (code.equals("EXT_INVAL"))    { color = new Color(90,90,0); } // 13
    else if (code.equals("VIRT_COHER"))   { color = new Color(90,90,0); } // 14
    else if (code.equals("GR_INSTR15"))   { color = new Color(90,90,0); } // 15
    else if (code.equals("CYCLES16"))     { color = new Color(90,90,0); } // 16
    else if (code.equals("GR_INSTR17"))   { color = new Color(90,90,0); } // 17
    else if (code.equals("GR_LOAD"))      { color = new Color(90,90,0); } // 18
    else if (code.equals("GR_STORE"))     { color = new Color(90,90,0); } // 19
    else if (code.equals("GR_ST_COND"))   { color = new Color(90,90,0); } // 20
    else if (code.equals("GR_FLOPS"))     { color = Color.red;          } // 21
    else if (code.equals("QUADWORDS1"))   { color = new Color(90,90,0); } // 22
    else if (code.equals("TLB_MISS"))     { color = Color.pink;         } // 23
    else if (code.equals("MIS_BR"))       { color = new Color(90,90,0); } // 24
    else if (code.equals("L1_DMISS"))     { color = Color.cyan;         } // 25
    else if (code.equals("L2_DMISS"))     { color = Color.green;        } // 26
    else if (code.equals("DATA_MIS"))     { color = new Color(90,90,0); } // 27
    else if (code.equals("EXT_INTERV2"))  { color = new Color(90,90,0); } // 28
    else if (code.equals("EXT_INVAL2"))   { color = new Color(90,90,0); } // 29
    else if (code.equals("CLEAN_ST_PRE")) { color = new Color(90,90,0); } // 30
    else if (code.equals("SHARE_ST_PRE")) { color = new Color(90,90,0); } // 31
    else {
      color = COLOR_LIST[colorLoc % COLOR_LIST.length];
      colorLoc++;
    }
  }
};

