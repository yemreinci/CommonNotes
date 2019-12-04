package tk.commonnotes.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

import tk.commonnotes.R;

public class NotesList extends AppCompatActivity {

    /**
    public or private
     */
    private String listType;
    private LinearLayout layout;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            loadNotes();
            return true;
        }

        if (id == R.id.action_add) {
            Thread job = new Thread() {
                @Override
                public void run() {
                    Socket sock = null;

                    try {
                        sock = new Socket(Config.serverAddress, Config.serverPort);

                        final ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                        final ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

                        HashMap<String, Object> request = new HashMap<>();

                        request.put("type", "newNote");

                        out.writeObject(request);
                        out.flush();

                        HashMap<String, Object> response = (HashMap<String, Object>) in.readObject();

                        in.close();
                        out.close();

                        final int noteId = (int) response.get("noteId");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(NotesList.this, EditNote.class);
                                intent.putExtra("noteId", noteId);
                                startActivity(intent);
                            }
                        });
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        "check your connection!",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    } finally {
                        if (sock != null) {
                            try {
                                sock.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            };

            job.start();
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadNotes() {
        layout.removeAllViews();
        // TODO add a spinner for better ui

        Thread job = new Thread() {
            List<HashMap<String, Object>> notes = null;

            @Override
            public void run() {
                Socket sock = null;
                try {
                    sock = new Socket(Config.serverAddress, Config.serverPort);

                    final ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                    final ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

                    HashMap<String, Object> request = new HashMap<>();

                    request.put("type", "listNotes");

                    out.writeObject(request);
                    out.flush();

                    notes = (List) in.readObject();

                    in.close();
                    out.close();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "loading notes failed!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    return;
                } finally {
                    if (sock != null) {
                        try {
                            sock.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (HashMap<String, Object> note: notes) {
                            View v = LayoutInflater.from(NotesList.this).inflate(R.layout.note_card, layout, false);

                            final Integer noteId = (int) note.get("noteId");
                            String text = (String) note.get("text");

                            v.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(NotesList.this, EditNote.class);
                                    intent.putExtra("noteId", noteId);
                                    startActivity(intent);
                                }
                            });

                            text = text.trim();

                            int lineBreak = text.indexOf("\n");
                            if (lineBreak == -1) {
                                lineBreak = text.length();
                            }

                            TextView textView = v.findViewById(R.id.text);

                            Spannable spannableText = new SpannableString(text);
                            spannableText.setSpan(
                                    new RelativeSizeSpan(1.5f),
                                    0,
                                    lineBreak,
                                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                            textView.setText(spannableText);

                            layout.addView(v);
                        }
                    }
                });
            }
        };

        job.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadNotes();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();

        if (intent != null) {
            Bundle extras = intent.getExtras();

            if (extras != null) {
                listType = (String) extras.get("listType");
            }
        }

        layout = findViewById(R.id.listLayout);
    }
}
