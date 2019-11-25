package tk.commonnotes.ot;

import java.util.ArrayList;
import java.util.Objects;

public class Replace extends Operation {
    /**
     * Begin index of substring to be replaced
     */
    public int bi;

    /**
     * End index of substring to be replaced
     */
    public int ei;

    /**
     * String to be replaced with
     */
    public String str;

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
    public void apply(StringBuilder text) {
        text.replace(bi, ei, str);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Replace replace = (Replace) o;
        return bi == replace.bi &&
                ei == replace.ei &&
                str.equals(replace.str);
    }

    @Override
    public String toString() {
        return String.format("replace(%d, %d, '%s')", bi, ei, str);
    }

    @Override
    public Operation transform(Operation operation, boolean priority) {
        Replace replaceOp = (Replace) operation;

        int d = replaceOp.str.length() - (replaceOp.ei - replaceOp.bi);

        if (bi == replaceOp.bi && ei == replaceOp.ei) {
            if (str.equals(replaceOp.str)) {
                return new Replace();
            }
            else if (priority) {
                return new Replace(replaceOp.ei+d, replaceOp.ei+d, str);
            }
            else {
                return new Replace(bi, bi, str);
            }
        }
        else if (bi <= replaceOp.bi && replaceOp.ei <= ei) {
            return new Replace(bi, ei+d, str);
        }
        else if (replaceOp.bi <= bi && ei <= replaceOp.ei) {
            return new Replace();
        }
        else if (ei <= replaceOp.bi) {
            return new Replace(bi, ei, str);
        }
        else if (bi >= replaceOp.ei) {
            return new Replace(bi+d, ei+d, str);
        }
        else if (bi < replaceOp.bi) {
            return new Replace(bi, replaceOp.bi, str);
        }
        else {
            return new Replace(replaceOp.ei+d, ei+d, str);
        }
    }

}
