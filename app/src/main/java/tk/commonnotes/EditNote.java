package tk.commonnotes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import tk.commonnotes.ot.Operation;
import tk.commonnotes.ot.Replace;
import tk.commonnotes.ot.Message;

//                        if (reader.ready()) {
//                            command = reader.readLine().replace("<br>", "\n");
//
//                            String[] splitted = command.split(" ", 3);
//                            final int from = Integer.parseInt(splitted[0]);
//                            final int to = Integer.parseInt(splitted[1]);
//                            final String with = splitted.length < 3 ? "": splitted[2];
//
//                            handler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    int start = text.getSelectionStart();
//                                    int end = text.getSelectionEnd();
//
//                                    disable = true;
//                                    text.getText().replace(from, to, with);
//                                    disable = false;
//
//                                    if (to <= start) {
//                                        int d = with.length() - (to - from);
//                                        text.setSelection(start + d, end + d);
//                                    } else if (end <= from) {
//                                        text.setSelection(start, end);
//                                    }
//
//                                    final BackgroundColorSpan span = new BackgroundColorSpan(Color.YELLOW);
//
//                                    text.getText().setSpan(span,
//                                            from, from + with.length(),
//                                            Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
//
//                                    handler.postDelayed(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            text.getText().removeSpan(span);
//                                        }
//                                    }, 800);
//
//                                }
//                            });
//                        }
//String now = s.subSequence(start, start + count).toString();
//
//
//        if (lastAdded.equals("*") && now.equals(" ") &&
//        s.subSequence(start-1, start+1).toString().equals("* ")) {
//        text.getText().replace(start-1, start+1, "• ");
//        inList = true;
//        }
//        else if (now.equals("\n") && inList) {
//        text.getText().insert(start+1,"• ");
//        }
//
//        lastAdded = now;

public class EditNote extends AppCompatActivity {
    private EditText text;
    private Handler handler = new Handler();
    private BlockingQueue<Message> messages;
    private boolean disable = false;
    private ArrayList<Operation> operations;
    private int numExecuted = 0;

    private void focusOnEditText() {
        if(findViewById(R.id.editText).requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        focusOnEditText();

        messages = new BlockingQueue<Message>();

        text = findViewById(R.id.editText);
        operations = new ArrayList<Operation>();

        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d("beforetextchange", String.format("%s %d %d %d", s, start, count, after));
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("ontextchange", String.format("%s %d %d %d", s, start, before, count) + " " + s.subSequence(start, start+count).toString());

                if (disable) {
                    Log.d("ontextchange", "disabled");
                    return;
                }

                Operation operation = new Replace(start, start+before, s.subSequence(start, start+count).toString());
                operations.add(operation);

                messages.add(new Message(operation, numExecuted));
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d("aftertextchange", s.toString());
            }
        });


        Runnable network = new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("tcpsocket", "running");

                    Socket sock = new Socket("52.174.25.75", 8001);
                    Log.d("tcpsocket", "socket opened");

                    final ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());

                    Runnable sender = new Runnable() {
                        int cursor = 0;

                        @Override
                        public void run() {
                            while (true) {
                                Message message = messages.pop();

                                try {
                                    out.writeObject(message);
                                    out.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };

                    (new Thread(sender)).start();

                    ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

                    while (true) {
                        final Message message = (Message) in.readObject();
                        Log.d("receive", "msg pri: " + message.hasPriority());

                        if (message == null) {
                            continue;
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("receive", "received: " + message.getOperation());
                                Replace transformed = (Replace) message.getOperation();
                                for (int i = message.getNumExecuted(); i < operations.size(); i++) {
                                    Replace t1 = (Replace) transformed.transform(operations.get(i), true);
                                    Replace t2 = (Replace) operations.get(i).transform(transformed, false);

                                    transformed = t1;
                                    operations.set(i, t2);
                                }

                                Log.d("receive", "executing: " + transformed);
                                disable = true;
                                text.getText().replace(transformed.bi, transformed.ei, transformed.str);
                                numExecuted++;
                                disable = false;
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
