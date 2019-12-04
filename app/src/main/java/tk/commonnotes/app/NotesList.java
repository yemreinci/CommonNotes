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
    private boolean refreshing = false;
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
            refresh();
            return true;
        }

        if (id == R.id.action_add) {
            handleNewNote();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleNewNote() {
        Thread job = new Thread() {
            @Override
            public void run() {
                Socket sock = null;

                try {
                    sock = new Socket(Config.serverAddress, Config.serverPort);

                    // open streams
                    final ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                    final ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

                    // prepare request
                    HashMap<String, Object> request = new HashMap<>();
                    request.put("type", "newNote");

                    // send request
                    out.writeObject(request);
                    out.flush();

                    // get response
                    HashMap<String, Object> response = (HashMap<String, Object>) in.readObject();

                    in.close();
                    out.close();

                    // start the edit activity for new note
                    final int noteId = (int) response.get("noteId");
                    Intent intent = new Intent(NotesList.this, EditNote.class);
                    intent.putExtra("noteId", noteId);
                    startActivity(intent);

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Add note failed! Check your connection and try again.",
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

    private void updateNotesList(List<HashMap<String, Object>> notes) {
        layout.removeAllViews();

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

            // find the first line break
            int lineBreak = text.indexOf("\n");
            if (lineBreak == -1) {
                lineBreak = text.length();
            }

            TextView textView = v.findViewById(R.id.text);

            // first line will be in a larger font
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

    /**
     * Remove the notes list and reload from server
     */
    private void refresh() {
        if (refreshing) // refresh thread is already running
            return;

        Thread refreshJob = new Thread() {
            List<HashMap<String, Object>> notes = null;

            @Override
            public void run() {
                Socket sock = null;

                try {
                    sock = new Socket(Config.serverAddress, Config.serverPort);

                    // open streams
                    final ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                    final ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

                    // prepare request object
                    HashMap<String, Object> request = new HashMap<>();
                    request.put("type", "listNotes");

                    // send request
                    out.writeObject(request);
                    out.flush();

                    // retrieve response
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
                    // always try to close the socket
                    if (sock != null) {
                        try {
                            sock.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // update ui
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateNotesList(notes);
                        refreshing = false;
                    }
                });
            }
        };

        refreshing = true;
        refreshJob.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        refresh();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        layout = findViewById(R.id.listLayout);
    }
}
