package tk.commonnotes.app;

import java.util.LinkedList;

public class BlockingQueue<T> {
    public LinkedList<T> elements;

    public BlockingQueue() {
        elements = new LinkedList<T>();
    }

    public synchronized void add(T element) {
        elements.add(element);
        notify();
    }

    public synchronized T pop() {
        while (elements.isEmpty()) {
            try {
                wait();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        return elements.pop();
    }
}
