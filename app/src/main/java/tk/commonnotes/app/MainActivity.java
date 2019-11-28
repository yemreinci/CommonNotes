package tk.commonnotes.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import tk.commonnotes.Main2Activity;
import tk.commonnotes.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(MainActivity.this, NotesList.class);
        startActivity(intent);

        findViewById(R.id.publicNotesText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NotesList.class);
                intent.putExtra("listType", "public");
                startActivity(intent);
            }
        });

        findViewById(R.id.privateNotesText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NotesList.class);
                intent.putExtra("listType", "private");
                startActivity(intent);
            }
        });
    }
}
