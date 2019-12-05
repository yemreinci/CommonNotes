package tk.commonnotes.ot.operation;

import java.util.Locale;


public class Replace extends Operation {
    private String type = "replace";
    /**
     * Begin index of substring to be replaced
     */
    public int bi;

    /**
     * End index of substring to be replaced
     */
    public int ei;

    /**
     * String to replace with
     */
    public String str;

    /**
     * Return NOOP Replace
     */
    public Replace() {
        this.bi = this.ei = 0;
        this.str = "";
    }

    public Replace(int bi, int ei, String str) {
        this.bi = bi;
        this.ei = ei;
        this.str = str;
    }

    @Override
    public String getType() {
        return "replace";
    }

    public void apply(StringBuilder text) {
        text.replace(bi, ei, str);
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "replace(%d, %d, '%s')", bi, ei, str);
    }

    /**
     * Transform concurrent replace operations
     * @param oth already applied operation
     * @param priority true if oth has higher priority
     * @return
     */
    public Replace transform(Replace oth, boolean priority) {
        // positional difference caused by oth
        int d = oth.str.length() - (oth.ei - oth.bi);

        if (bi == oth.bi && ei == oth.ei) {
            if (str.equals(oth.str)) {
                return new Replace();
            }
            else if (priority) {
                return new Replace(oth.ei+d, oth.ei+d, str);
            }
            else {
                return new Replace(bi, bi, str);
            }
        }
        else if (bi <= oth.bi && oth.ei <= ei) {
            if (bi == oth.bi) {
                return new Replace(oth.ei+d, ei+d, str);
            }
            else if (ei == oth.ei) {
                return new Replace(bi, oth.bi, str);
            }
            else {
                return new Replace(bi, ei + d, str);
            }
        }
        else if (oth.bi <= bi && ei <= oth.ei) {
            if (bi == oth.bi) {
                return new Replace(oth.bi, oth.bi, str);
            }
            else if (ei == oth.ei) {
                return new Replace(oth.ei+d, oth.ei+d, str);
            }
            else {
                return new Replace();
            }
        }
        else if (ei <= oth.bi) {
            return new Replace(bi, ei, str);
        }
        else if (bi >= oth.ei) {
            return new Replace(bi+d, ei+d, str);
        }
        else if (bi < oth.bi) {
            return new Replace(bi, oth.bi, str);
        }
        else {
            return new Replace(oth.ei+d, ei+d, str);
        }
    }
}
