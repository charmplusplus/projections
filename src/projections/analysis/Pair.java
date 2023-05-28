package projections.analysis;

public class Pair {
    private long start, end;

    public Pair(long x, long y) {
        start = x;
        end = y;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public long getStart() {
        return start;
    }
}
