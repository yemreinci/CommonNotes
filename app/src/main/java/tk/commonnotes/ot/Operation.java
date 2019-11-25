package tk.commonnotes.ot;

import java.io.Serializable;

public abstract class Operation implements Serializable {
    /**
     * Apply the operation on text
     * @param text Text on which the operation is applied
     */
    public abstract void apply(StringBuilder text);

    public abstract Operation transform(Operation oth, boolean priority);
}
