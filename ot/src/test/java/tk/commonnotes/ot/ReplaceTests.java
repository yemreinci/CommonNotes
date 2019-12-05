package tk.commonnotes.ot;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import static org.junit.Assert.assertEquals;

import tk.commonnotes.ot.operation.Replace;

public class ReplaceTests {
    @Test
    public void testApply() {
        Replace op = new Replace(3, 7, "foo");

        StringBuilder str = new StringBuilder("barbarbar");

        op.apply(str);

        assertEquals(str.toString(), "barfooar");
    }

    @Test
    public void testSerialize() throws Exception {
        Replace op = new Replace(3, 7, "foo");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(out);

        objectOut.writeObject(op);
        objectOut.flush();
        objectOut.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream objectIn = new ObjectInputStream(in);

        Replace op2 = (Replace) objectIn.readObject();

        assertEquals(op.toString(), op2.toString());
    }

    @Test
    public void testToString() {
        Replace replace = new Replace(12, 19, "bar");

        assertEquals("replace(12, 19, 'bar')", replace.toString());
    }

    @Test
    public void testTransform() {
        Random rand = new Random(1337);

        for (int i = 0; i < 5000; i++) {
            int len = rand.nextInt(10);

            String s = "";
            for (int j = 0; j < len; j++) {
                s += ("abcd".charAt(rand.nextInt(4)));
            }

            int bi1, ei1, bi2, ei2;
            bi1 = rand.nextInt(len+1);
            ei1 = bi1 + rand.nextInt(len+1-bi1);
            bi2 = rand.nextInt(len+1);
            ei2 = bi2 + rand.nextInt(len+1-bi2);

            String[] choices = {
                    "", "a", "b", "c", "d", "ab", "ac", "abc", "def"
            };

            Replace r1 = new Replace(bi1, ei1, choices[rand.nextInt(choices.length)]);
            Replace r2 = new Replace(bi2, ei2, choices[rand.nextInt(choices.length)]);

//            System.out.println(r1);
//            System.out.println(r2);

            StringBuilder s1 = new StringBuilder(s);
            StringBuilder s2 = new StringBuilder(s);

            Replace r1p = r1.transform(r2, true);
            Replace r2p = r2.transform(r1, false);

//            System.out.println(r1p);
//            System.out.println(r2p);

            r2.apply(s1);
            r1p.apply(s1);

            r1.apply(s2);
            r2p.apply(s2);

            assertEquals(s1.toString(), s2.toString());
        }
    }
}