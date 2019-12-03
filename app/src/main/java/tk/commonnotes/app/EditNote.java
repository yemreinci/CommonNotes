package tk.commonnotes.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import tk.commonnotes.R;
import tk.commonnotes.common.Replace;
import tk.commonnotes.common.message.Message;

public class EditNote extends AppCompatActivity {
    private EditText text;
    private Handler handler = new Handler();
    private BlockingQueue<Message> messages;
    private boolean disableWatcher = false;
    private LinkedList<Replace> operations;
    private int numAcknowledged = 0;
    private int numExecuted = 0;
    private ArrayList<Replace> outgoing = new ArrayList<Replace>();
    private int noteId;
    private HashMap<Character, Character> charToBullet, bulletToChar;

    private void focusOnEditText() {
        // focus on edit and open keyboard
        if(findViewById(R.id.editText).requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    private void initializeBulletTables() {
        charToBullet = new HashMap<>();
        bulletToChar = new HashMap<>();

        charToBullet.put('*', '•');
        charToBullet.put('-', '◦');

        for(HashMap.Entry<Character, Character> entry: charToBullet.entrySet()) {
            bulletToChar.put(entry.getValue(), entry.getKey());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        initializeBulletTables();

        noteId = (int) getIntent().getExtras().get("noteId");

        messages = new BlockingQueue<Message>();

        text = findViewById(R.id.editText);
        operations = new LinkedList<>();

        text.setEnabled(false);

        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("ontextchange", String.format("%s %d %d %d", s, start, before, count) + " " + s.subSequence(start, start+count).toString());

                if (disableWatcher) {
                    Log.d("ontextchange", "disabled");
                    return;
                }

                Replace operation = new Replace(start, start+before, s.subSequence(start, start+count).toString());
                operations.add(operation);

                messages.add(new Message(operation, numExecuted));

                if (count == 1 && s.charAt(start) == ' ') {
                    if (start > 0 && charToBullet.containsKey(s.charAt(start-1))) {
                        boolean allBlank = true;
                        for (int i = start-2; i >= 0 && s.charAt(i) != '\n'; i--) {
                            if (s.charAt(i) != ' ') {
                                allBlank = false;
                            }
                        }

                        if (allBlank) {
                            String bullet = charToBullet.get(s.charAt(start-1)).toString();
                            text.getText().replace(start-1, start, bullet);
                        }
                    }
                }
                else if (count == 1 && s.charAt(start) == '\n') {
                    boolean bulletSeen = false;
                    char bullet = 0;
                    StringBuilder indent = new StringBuilder();
                    for (int i = start-1; i >= 0 && s.charAt(i) != '\n'; i--) {
                        if (bulletToChar.containsKey(s.charAt(i))) {
                            bullet = s.charAt(i);
                            indent.setLength(0);
                        }
                        else if (bullet != 0) {
                            if (s.charAt(i) == ' ') {
                                indent.append(' ');
                            }
                            else {
                                bullet = 0;
                            }
                        }
                    }

                    if (bullet != 0) {
                        text.getText().insert(start+1, indent.toString() + bullet + " ");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        Runnable network = new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("tcpsocket", "running");

                    Socket sock = new Socket("52.174.25.75", 8001);
                    Log.d("tcpsocket", "socket opened");

                    HashMap<String, Object> request = new HashMap<>();
                    request.put("type", "connectNote");
                    request.put("noteId", noteId);

                    final ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());

                    out.writeObject(request);
                    out.flush();

                    Runnable sender = new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                Message message = messages.pop();

                                try {
                                    out.writeObject(message);
                                    out.flush();
                                    outgoing.add(message.getOperation());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };

                    (new Thread(sender)).start();

                    ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

                    final String initialText = (String) in.readObject();
                    Log.d("yunus", "initial text: " + initialText);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            disableWatcher = true;
                            text.append(initialText);
                            disableWatcher = false;
                            text.setEnabled(true);
                            focusOnEditText();
                        }
                    });

                    while (true) {
                        final Message message = (Message) in.readObject();
                        Log.d("receive", "msg pri: " + message.hasPriority());

                        if (message == null) {
                            System.out.println("E - unexpected null message");
                            break;
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("receive", "received: " + message.getOperation());
                                Replace transformed = (Replace) message.getOperation();

                                while (numAcknowledged < message.getNumExecuted()) {
                                    operations.removeFirst();
                                    numAcknowledged++;
                                }

                                for (ListIterator<Replace> iter = operations.listIterator(); iter.hasNext(); ) {
                                    Replace op = iter.next();

                                    Replace t1 = (Replace) transformed.transform(op, true);
                                    Replace t2 = (Replace) op.transform(transformed, false);

                                    transformed = t1;
                                    iter.set(t2);
                                }

                                Log.d("receive", "executing: " + transformed);
                                disableWatcher = true;
                                text.getText().replace(transformed.bi, transformed.ei, transformed.str);
                                numExecuted++;
                                disableWatcher = false;

                                final BackgroundColorSpan span = new BackgroundColorSpan(Color.rgb(212, 232, 255));

                                text.getText().setSpan(span,
                                        transformed.bi, transformed.bi+transformed.str.length(),
                                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        text.getText().removeSpan(span);
                                    }
                                }, 500);
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.d("tcpsocket", e.toString());
                    e.printStackTrace();
                }
            }
        };

         (new Thread(network)).start();
    }
}