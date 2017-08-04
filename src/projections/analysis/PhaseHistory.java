package projections.analysis;

import projections.gui.U;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class PhaseHistory {

    public static final int MAX_ENTRIES = 10;

    private String filename;
    private int numEntries;
    private List<List<Pair>> phaseSet;
    private List<String> phaseName;
    private List<String> phaseProcs;

    private List<String> historyStringVector;

    public PhaseHistory(String logDirectory) {
        filename = logDirectory + "phases.hst";
        if(!(new File(filename)).exists()) {
            phaseSet = new ArrayList<List<Pair>>();
            phaseName = new ArrayList<String>();
            phaseProcs = new ArrayList<String>();
            historyStringVector = new ArrayList<String>();
        } else {
            try {
                loadPhases();
                historyStringVector = new ArrayList<String>();
                for(int i = 0; i < phaseSet.size(); i++)
                    historyStringVector.add(getPhaseConfigString(i));
            } catch (IOException e) {
                System.err.println("Error: " + e.toString());
            }
        }
    }

    private void loadPhases() throws IOException {
        phaseSet = new ArrayList<List<Pair>>();
        phaseName = new ArrayList<String>();
        phaseProcs = new ArrayList<String>();
        numEntries = 0;

        String line;
        StringTokenizer st;
        BufferedReader reader = new BufferedReader(new FileReader(filename));

        while((line = reader.readLine()) != null) {
            st = new StringTokenizer(line);
            String s1 = st.nextToken();
            if(s1.equals("ENTRY")) {
                if(numEntries >= MAX_ENTRIES)
                    throw new IOException("Phase history overflow!");
                List<Pair> currList = new ArrayList<Pair>();
                while(st.hasMoreTokens())
                    currList.add(new Pair(Long.valueOf(st.nextToken()), Long.valueOf(st.nextToken())));
                phaseSet.add(currList);
                numEntries++;
            } else if(s1.equals("NAMEENTRY")) {
                if(st.hasMoreTokens())
                    phaseName.add(st.nextToken());
                else
                    // creates a fake name, cancel, to preserve alignment and prevent issues
                    phaseName.add("cancel");
            } else if(s1.equals("PROCENTRY")) {
                StringBuilder procs = new StringBuilder();
                while(st.hasMoreTokens()) {
                    procs.append(st.nextToken());
                }
                phaseProcs.add(procs.toString());
            }
        }
        reader.close();
    }

    public void save() throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(filename), true);

        for(int i = 0; i < numEntries; i++) {
            writer.print("ENTRY ");
            for(Pair o : phaseSet.get(i)) {
                writer.print(o.getStart());
                writer.print(" ");
                writer.print(o.getEnd());
                writer.print(" ");
            }
            writer.println();
            writer.print("NAMEENTRY ");
            if(phaseName.size() > i)
                writer.println(phaseName.get(i));
            else
                writer.println("cancel");
            writer.print("PROCENTRY ");
            if(phaseProcs.size() > i)
                writer.println(phaseProcs.get(i));
        }
    }

    public void add(List<Pair> list, String name, String procs) {
        if(numEntries == MAX_ENTRIES) {
            phaseSet.remove(MAX_ENTRIES - 1);
            if(phaseName.size() == MAX_ENTRIES)
                phaseName.remove(MAX_ENTRIES - 1);
            if(phaseProcs.size() == MAX_ENTRIES)
                phaseProcs.remove(MAX_ENTRIES - 1);
            numEntries--;
        }

        if(phaseName == null)
            phaseName = new ArrayList<String>();
        phaseName.add(0, name);
        phaseSet.add(0, clonePairList(list));
        phaseProcs.add(0, procs);
        numEntries++;
    }

    public void update(int index, List<Pair> list, String name, String procs) {
        if (index < 0 || index >= numEntries) {
            System.err.println("Internal Error: Attempt to update " +
                    "invalid index " +
                    index + ". Max number of " +
                    "histories is " + numEntries +
                    ". Please report to developers!");
            System.exit(-1);
        }
        phaseSet.remove(index);
        phaseSet.add(index, clonePairList(list));

        if(phaseProcs != null && phaseProcs.size() > index) {
            phaseProcs.remove(index);
            phaseProcs.add(index, procs);
        }

        if(name != null && phaseName != null && phaseName.size() > index) {
            phaseName.remove(index);
            phaseName.add(index, name);
        }
    }

    public void remove(int index) {
        if (index < 0 || index >= numEntries) {
            System.err.println("Internal Error: Attempt to remove " +
                    "invalid index " +
                    index + ". Max number of " +
                    "histories is " + numEntries +
                    ". Please report to developers!");
            System.exit(-1);
        }

        phaseSet.remove(index);
        if(phaseName != null && phaseName.size() > index)
            phaseName.remove(index);
        if(phaseProcs != null && phaseProcs.size() > index)
            phaseProcs.remove(index);
        numEntries--;
    }

    private List<Pair> clonePairList(List<Pair> list) {
        List<Pair> newList = new ArrayList<Pair>(list.size());
        newList.addAll(list);
        return newList;
    }

    public List<String> getHistoryStrings() {
        return historyStringVector;
    }

    public String getProcRange(int index) {
        if(phaseProcs == null || phaseProcs.size() <= index || index < 0)
            return null;
        return phaseProcs.get(index);
    }

    public String getPhaseConfigName(int index) {
        if(phaseName == null || phaseName.size() <= index || index < 0)
            return null;
        return phaseName.get(index);
    }

    private String getPhaseConfigString(int index) {
        if(index < 0 || index >= numEntries) {
            System.err.println("Internal Error: Requested phase config string for history index " +
                    index + " is invalid. Max number of " +
                    "histories is " + numEntries +
                    ". Please report to developers!");
            System.exit(-1);
        }

        List<Pair> list = phaseSet.get(index);
        StringBuilder historyString = new StringBuilder();
        String minVal = U.humanReadableString(getStartValue(index)), maxVal = U.humanReadableString(getEndValue(index));
        historyString.append(list.size()).append(" Phase(s) ");
        if(minVal.indexOf('.') == -1 || minVal.length() - minVal.indexOf('.') <= 2)
            historyString.append(minVal);
        else if(minVal.length() >= 5)
            historyString.append(minVal.substring(0, Math.max(5, minVal.indexOf('.') + 2)));
        else
            historyString.append(minVal.substring(0, minVal.indexOf('.') + 2));
        historyString.append(" to ");
        if(maxVal.indexOf('.') == -1 || maxVal.length() - minVal.indexOf('.') <= 2)
            historyString.append(maxVal);
        else if(maxVal.length() >= 5)
            historyString.append(maxVal.substring(0, Math.max(5, maxVal.indexOf('.') + 2)));
        else
            historyString.append(maxVal.substring(0, maxVal.indexOf('.') + 2));
        if(phaseProcs != null && phaseProcs.size() != 0 && phaseProcs.size() > index) {
            String temp = phaseProcs.get(index);
            if(temp.length() > 10)
                historyString
                        .append(" Proc(s): ")
                        .append(temp.substring(0, 10))
                        .append("...");
            else
                historyString
                        .append(" Proc(s): ")
                        .append(temp);
        }
        if(phaseName != null && phaseName.size() != 0 && phaseName.get(index) != null && !phaseName.get(index).equals("cancel")) {
            String temp = phaseName.get(index);
            if(temp.length() > 10)
                historyString
                        .append(" (")
                        .append(temp.substring(0, 10))
                        .append("...)");
            else
                historyString
                        .append(" (")
                        .append(temp)
                        .append(")");
        }
        return historyString.toString();
    }

    public String getPhaseString(int index, int phaseIndex) {
        if(index < 0 || phaseIndex < 0 || index >= numEntries || phaseIndex >= phaseSet.get(index).size()) {
            System.err.println("Internal Error: Requested phase string for history index " +
                    index + " and phase index " +
                    phaseIndex + " is invalid. Max number of " +
                    "histories is " + numEntries +
                    ". Please report to developers!");
            System.exit(-1);
        }

        Pair curr = phaseSet.get(index).get(phaseIndex);
        StringBuilder phaseString = new StringBuilder();
        phaseString
                .append(U.humanReadableString(curr.getStart()))
                .append(" to ")
                .append(U.humanReadableString(curr.getEnd()));
        if(phaseProcs != null && phaseProcs.size() != 0 && phaseProcs.size() > index) {
            String temp = phaseProcs.get(index);
            if(temp.length() > 10)
                phaseString
                        .append(" Proc(s):")
                        .append(temp.substring(0, 10))
                        .append("...");
            else
                phaseString.append(" Proc(s):").append(temp);
        }

        return phaseString.toString();
    }

    public long getStartOfPhase(int index, int phaseIndex) {
        if(index < 0 || phaseIndex < 0 || index >= numEntries || phaseIndex >= phaseSet.get(index).size()) {
            System.err.println("Internal Error: Requested start of phase for history index " +
                    index + " and phase index " +
                    phaseIndex + " is invalid. Max number of " +
                    "histories is " + numEntries +
                    ". Please report to developers!");
            System.exit(-1);
        }
        return phaseSet.get(index).get(phaseIndex).getStart();
    }

    public long getEndOfPhase(int index, int phaseIndex) {
        if(index < 0 || phaseIndex < 0 || index >= numEntries || phaseIndex >= phaseSet.get(index).size()) {
            System.err.println("Internal Error: Requested end of phase for history index " +
                    index + " and phase index " +
                    phaseIndex + " is invalid. Max number of " +
                    "histories is " + numEntries +
                    ". Please report to developers!");
            System.exit(-1);
        }
        return phaseSet.get(index).get(phaseIndex).getEnd();
    }

    public int getNumPhases(int index) {
        if(index < 0 || index >= numEntries) {
            System.err.println("Internal Error: Requested number of phases for history index " +
                    index + " is invalid. Max number of " +
                    "histories is " + numEntries +
                    ". Please report to developers!");
            System.exit(-1);
        }
        return phaseSet.get(index).size();
    }

    public long getStartValue(int index) {
        if(index < 0 || index >= numEntries) {
            System.err.println("Internal Error: Requested start value of history index " +
                    index + " is invalid. Max number of " +
                    "histories is " + numEntries +
                    ". Please report to developers!");
            System.exit(-1);
        }
        List<Pair> list = phaseSet.get(index);
        long minVal = Long.MAX_VALUE;
        for(Pair curr : list) {
            if(curr.getStart() < minVal)
                minVal = curr.getStart();
        }
        return minVal;
    }

    public long getEndValue(int index) {
        if(index < 0 || index >= numEntries) {
            System.err.println("Internal Error: Requested end value of history index " +
                    index + " is invalid. Max number of " +
                    "histories is " + numEntries +
                    ". Please report to developers!");
            System.exit(-1);
        }
        List<Pair> list = phaseSet.get(index);
        long maxVal = Long.MIN_VALUE;
        for(Pair curr : list) {
            if(curr.getEnd() > maxVal)
                maxVal = curr.getEnd();
        }
        return maxVal;
    }
}
