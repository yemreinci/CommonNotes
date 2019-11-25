package tk.commonnotes.ot;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;

public class MessageTests {
    @Test
    public void testSerialize() throws Exception {
        Message rq1 = new Message(new Replace(3, 7, "foo"), 10);
        Message rq2 = new Message(new Replace(1, 2, "bar"), 15);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(out);

        objectOut.writeObject(rq1);
        objectOut.writeObject(rq2);
        objectOut.flush();
        objectOut.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream objectIn = new ObjectInputStream(in);

        Message rq1Clone = (Message) objectIn.readObject();
        Message rq2Clone = (Message) objectIn.readObject();

        assertEquals(rq1.getOperation(), rq1Clone.getOperation());
        assertEquals(rq1.getNumExecuted(), rq1Clone.getNumExecuted());
        assertEquals(rq2.getOperation(), rq2Clone.getOperation());
        assertEquals(rq2.getNumExecuted(), rq2Clone.getNumExecuted());
    }
}
