// UNUSED

//package projections.gui.count;
//
//import java.awt.*;
//import java.util.Hashtable;
//import projections.gui.Analysis;
//
//public class Counter {
//  public String counterCode = null;
//  public String description = null;
//  public String fileName = null;
//  public Color  color = null;
//
//  private static final int NUM_COUNTERS = 32;
//  private static final Color[] COLOR_LIST = 
//    Analysis.createColorMap(NUM_COUNTERS);
//  private static Hashtable CODE_TO_INDEX = null;
//
//  public Counter(String code, String desc, String fileName) throws Exception { 
//    if (CODE_TO_INDEX == null) { initHash(); }
//    counterCode = code; 
//    description = desc;
//    this.fileName = fileName;
//    Integer index = (Integer) CODE_TO_INDEX.get(code);
//    if (index == null) { throw new Exception("Counter "+code+" unknown"); }
//    color = COLOR_LIST[index.intValue()%NUM_COUNTERS];  // shouldn't get to mod
//  }
//
//  private void initHash() {
//    CODE_TO_INDEX = new Hashtable();
//    CODE_TO_INDEX.put("CYCLES0",      new Integer(0)); 
//    CODE_TO_INDEX.put("INSTR",        new Integer(1));       
//    CODE_TO_INDEX.put("LOAD",         new Integer(2));        
//    CODE_TO_INDEX.put("STORE",        new Integer(3));      
//    CODE_TO_INDEX.put("STORE_COND",   new Integer(4)); 
//    CODE_TO_INDEX.put("FAIL_COND",    new Integer(5));  
//    CODE_TO_INDEX.put("DECODE_BR",    new Integer(6));  
//    CODE_TO_INDEX.put("QUADWORDS2",   new Integer(7)); 
//    CODE_TO_INDEX.put("CACHE_ER2",    new Integer(8));  
//    CODE_TO_INDEX.put("L1_IMISS",     new Integer(9));   
//    CODE_TO_INDEX.put("L2_IMISS",     new Integer(10));   
//    CODE_TO_INDEX.put("INSTRMISPR",   new Integer(11)); 
//    CODE_TO_INDEX.put("EXT_INTERV",   new Integer(12)); 
//    CODE_TO_INDEX.put("EXT_INVAL",    new Integer(13));  
//    CODE_TO_INDEX.put("VIRT_COHER",   new Integer(14)); 
//    CODE_TO_INDEX.put("GR_INSTR15",   new Integer(15)); 
//    CODE_TO_INDEX.put("CYCLES16",     new Integer(16));   
//    CODE_TO_INDEX.put("GR_INSTR17",   new Integer(17)); 
//    CODE_TO_INDEX.put("GR_LOAD",      new Integer(18));    
//    CODE_TO_INDEX.put("GR_STORE",     new Integer(19));   
//    CODE_TO_INDEX.put("GR_ST_COND",   new Integer(20)); 
//    CODE_TO_INDEX.put("GR_FLOPS",     new Integer(21));   
//    CODE_TO_INDEX.put("QUADWORDS1",   new Integer(22)); 
//    CODE_TO_INDEX.put("TLB_MISS",     new Integer(23));   
//    CODE_TO_INDEX.put("MIS_BR",       new Integer(24));     
//    CODE_TO_INDEX.put("L1_DMISS",     new Integer(25));   
//    CODE_TO_INDEX.put("L2_DMISS",     new Integer(26));   
//    CODE_TO_INDEX.put("DATA_MIS",     new Integer(27));   
//    CODE_TO_INDEX.put("EXT_INTERV2",  new Integer(28));
//    CODE_TO_INDEX.put("EXT_INVAL2",   new Integer(29)); 
//    CODE_TO_INDEX.put("CLEAN_ST_PRE", new Integer(30));
//    CODE_TO_INDEX.put("SHARE_ST_PRE", new Integer(31));
//  }
//};
//
