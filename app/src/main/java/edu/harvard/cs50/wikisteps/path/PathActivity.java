package edu.harvard.cs50.wikisteps.path;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;

import edu.harvard.cs50.wikisteps.R;

public class PathActivity extends AppCompatActivity {

    // fields for instantiate recyclerView
    private RecyclerView recyclerView;
    private OuterRecyclerViewAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private ArrayList<Path> listPath;

    private TextView textViewPages;
    private TextView textViewTime;
    private TextView textViewSteps;
    private TextView textViewVisited;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path);

        listPath = (ArrayList<Path>) getIntent().getSerializableExtra("listPath");
        int checkedPages = getIntent().getIntExtra("pages", 0);
        float time = getIntent().getFloatExtra("time", 0);
        int visitedPages = getIntent().getIntExtra("visited", 0);

        textViewPages = findViewById(R.id.textView_pages);
        textViewTime = findViewById(R.id.textView_time);
        textViewSteps = findViewById(R.id.textView_steps);
        textViewVisited = findViewById(R.id.textView_parse);
        textViewSteps.setText("Steps: " + (listPath.get(0).getStepList().size() - 1));
        textViewTime.setText("Time, sec: " + time);
        textViewVisited.setText("Visited pages: " + visitedPages);
        textViewPages.setText("Checked pages: " + checkedPages);

        // instantiate recyclerView
        recyclerView = findViewById(R.id.recycler_view);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new OuterRecyclerViewAdapter(listPath, PathActivity.this);
        recyclerView.setAdapter(adapter);
    }

    // start new Main Activity like up button
    @Override
    public void onBackPressed()
    {
        NavUtils.navigateUpFromSameTask(this);
        super.onBackPressed();
    }
}