package tk.commonnotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;


public class NotesList extends AppCompatActivity {

    /**
    public or private
     */
    private String listType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        Intent intent = getIntent();

        if (intent != null) {
            Bundle extras = intent.getExtras();

            if (extras != null) {
                listType = (String) extras.get("listType");
            }
        }

        LinearLayout layout = findViewById(R.id.listLayout);

        for (int i = 0; i < 20; i++) {
            View v = LayoutInflater.from(this).inflate(R.layout.note_card, layout);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(NotesList.this, EditNote.class);
                    // TODO put which note is selected
                    intent.putExtra("", "");
                    startActivity(intent);
                }
            });
        }
    }
}
