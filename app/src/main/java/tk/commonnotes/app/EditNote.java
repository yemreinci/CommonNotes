package tk.commonnotes.app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import tk.commonnotes.R;
import tk.commonnotes.ot.Message;
import tk.commonnotes.ot.Replace;


public class EditNote extends AppCompatActivity {
    private EditText text;
    private Handler handler = new Handler();
    private BlockingQueue<Message> messages;
    private boolean disableWatcher = false;
    private LinkedList<Replace> operations;
    private int numAcknowledged = 0;
    private Socket sock;
    private int numExecuted = 0;
    int noteId;
    private boolean isDeleted = false;
    private HashMap<Character, Character> charToBullet, bulletToChar;
    private EditNoteBackground background = null;

    /**
     * focus on edittext view and open keyboard
     */
    private void focusOnEditText() {
        if(findViewById(R.id.editText).requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    /**
     * initialize bullet point conversion tables
     */
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
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }

    public void exit() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setEnabled(false);
            }
        });

        if (background != null) {
            background.stop();
        }

        finish();
    }

    void setInitialText(final String initialText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                disableWatcher = true;
                text.append(initialText);
                disableWatcher = false;
                text.setEnabled(true);
                focusOnEditText();
            }
        });
    }

    void handleDelete() {
        if (!isDeleted) {
            isDeleted = true;
            text.setEnabled(false);
            findViewById(R.id.scrollView).setVisibility(View.GONE);
            findViewById(R.id.deletedLayout).setVisibility(View.VISIBLE);
            background.stop();
        }
    }

    /**
     * handle message from server
     */
    void handleMessage(final Message message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("receive", "received: " + message.getOperation());

                if(message.getOperation().delete) {
                    handleDelete();
                    return;
                }

                // discard acknowledged operations
                while (numAcknowledged < message.getNumExecuted()) {
                    operations.removeFirst();
                    numAcknowledged++;
                }

                Replace transformed = (Replace) message.getOperation();
                // transform concurrent operations
                for (ListIterator<Replace> iter = operations.listIterator(); iter.hasNext(); ) {
                    Replace op = iter.next();

                    Replace t1 = (Replace) transformed.transform(op, true);
                    Replace t2 = (Replace) op.transform(transformed, false);

                    transformed = t1;
                    iter.set(t2);
                }

                // apply the transformed operation
                disableWatcher = true;
                text.getText().replace(transformed.bi, transformed.ei, transformed.str);
                numExecuted++;
                disableWatcher = false;

                // add a highligh span for UI effect
                final BackgroundColorSpan span = new BackgroundColorSpan(Color.rgb(212, 232, 255));
                text.getText().setSpan(span,
                        transformed.bi, transformed.bi+transformed.str.length(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        text.getText().removeSpan(span);
                    }
                }, 600);
            }
        });
    }

    /**
     * handles bullet point logic
     */
    private void handleBulletPoints(CharSequence s, int start, int count) {
        if (count == 1 && s.charAt(start) == ' ') { // bullet list start
            // after a space is entered if the current editor looks like:
            //     * |
            // than transform the bullet character

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
        else if (count == 1 && s.charAt(start) == '\n') { // new line
            // when a new line is created, check if the line above is a bullet line,
            // insert bullet if it is
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_note_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_delete) {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            messages.add(new Message(new Replace(true), numExecuted));
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure you want to delete this note?")
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();

            return true;
        }
        else if (id == android.R.id.home) {
            exit();
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

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
                if (disableWatcher) { // ignore
                    return;
                }

                // create a replace operation
                Replace operation = new Replace(start, start+before, s.subSequence(start, start+count).toString());
                operations.add(operation);

                // append a message with the operation to be sent by the network thread
                messages.add(new Message(operation, numExecuted));

                handleBulletPoints(s, start, count);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        background = new EditNoteBackground(this, messages);

        background.start();
    }
}