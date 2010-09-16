package projections.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class TachyonShifts {

	TreeMap<Integer, Long> tachyonAdjustmentsForEachPe = new TreeMap();

	String filename;

	public TachyonShifts(String logDirectory){
		filename = logDirectory + File.separator +  "tachyonShifts.dat";

		// Check if the file exists
		File f = new File(filename);
		if(f.exists()){
			
			JLabel message = new JLabel("<html><body>A tachyon correction file was located:<br>" + filename +  "<br>Should the times from each PE's logs be adjusted <br>by the amounts specified in this file?</body></html>");
						
			int result = JOptionPane.showConfirmDialog(null, message, "Tachyon Correction?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			
			if(result == JOptionPane.YES_OPTION){
				readTachyonShiftMap();		
			}
		}
	
	}

	/** Accumulate an offset for specified pe */
	public void accumulateTachyonShifts(long shift, Integer pe){
		if(	tachyonAdjustmentsForEachPe.containsKey(pe)){
			Long oldShift = tachyonAdjustmentsForEachPe.get(pe);
			tachyonAdjustmentsForEachPe.put(pe, shift+oldShift);
		} else {
			tachyonAdjustmentsForEachPe.put(pe, shift);
		}
	}

	public void writeTachyonShiftMap() {
		try {
			ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(filename));
			outputStream.writeObject(tachyonAdjustmentsForEachPe);
		} catch (FileNotFoundException e) {
			System.err.println("ERROR: Couldn't write out time corrections (for removing tachyons) to " + filename + " (FileNotFoundException)");
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't write out time corrections (for removing tachyons) to " + filename + " (IOException)");
		} finally {
			System.out.println("Just wrote out Tachyon adjusting shift amounts for " + tachyonAdjustmentsForEachPe.size() + " PEs");
		}
	}

	public void readTachyonShiftMap() {
		try {
			ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(filename));
			tachyonAdjustmentsForEachPe = (TreeMap<Integer, Long>) inputStream.readObject();
		} catch (FileNotFoundException e) {
			//			System.err.println("ERROR: Couldn't read time corrections (for removing tachyons) from " + filename + " (FileNotFoundException)");
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't read time corrections (for removing tachyons) from " + filename + " (IOException)");
		} catch (ClassNotFoundException e) {
			System.err.println("ERROR: Couldn't read time corrections (for removing tachyons) from " + filename + " because the file is not in the correct format (ClassNotFoundException)");
		} finally {
			System.out.println("Just read in Tachyon adjusting shift amounts for " + tachyonAdjustmentsForEachPe.size() + " PEs");
		}

	}

	public long getShiftAmount(int pe) {
		if(	tachyonAdjustmentsForEachPe.containsKey(pe))
			return tachyonAdjustmentsForEachPe.get(pe);
		else
			return 0;
	}


}
