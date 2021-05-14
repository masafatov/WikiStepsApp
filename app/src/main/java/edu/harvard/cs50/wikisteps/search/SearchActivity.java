package edu.harvard.cs50.wikisteps.search;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import edu.harvard.cs50.wikisteps.path.Path;
import edu.harvard.cs50.wikisteps.path.PathActivity;
import edu.harvard.cs50.wikisteps.R;
import edu.harvard.cs50.wikisteps.path.Step;

public class SearchActivity extends AppCompatActivity {

    // Start and Goal from Intent from previous Activity
    private String inputStart;
    private String inputGoal;

    // parsing from MediaWiki API using Volley Library
    StepService stepService;

    // empty as a default parameter for parsing
    private static final String PARAM_EMPTY = "";

    // two-end bfs algorithm
    private Queue<String> qFromStart = new LinkedList<>();
    private Queue<String> qFromGoal = new LinkedList<>();
    private HashSet<String> sResultFromStart = new HashSet<>();
    private HashSet<String> sResultFromGoal = new HashSet<>();
    private boolean goal;

    // display the result of search
    private TextView showDescription;
    private TextView showCheckedTitles;

    // show checked titles in parallel thread
    private BlockingQueue<String> blockingQueue = new LinkedBlockingDeque<>();

    // define listPath
    private ArrayList<Path> listPath = new ArrayList<>();

    // stats values
    private long start;
    private int visitedPages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        showDescription = findViewById(R.id.textView_search);
        showCheckedTitles = findViewById(R.id.textView_showTitles);
        showDescription.setText("looking for steps by checking articles");

        inputStart = getIntent().getStringExtra("start");
        inputGoal = getIntent().getStringExtra("goal");

        stepService = new StepService(SearchActivity.this, inputStart, inputGoal);

        // instantiate with input values
        qFromStart.add(inputStart);
        qFromGoal.add(inputGoal);

        start = System.currentTimeMillis();

        // two-end BFS algorithm to find path
        check();

        // parallel thread to show checked titles articles
        Thread show = new Thread() {
            @Override
            public void run() {

                while(!goal) {
                    if (!blockingQueue.isEmpty()) {
                        String s = blockingQueue.remove();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showCheckedTitles.setText(s);
                            }
                        });
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showCheckedTitles.setText("Waiting for response from MediaWiki API");
                            }
                        });
                    }
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        show.start();
    }

    /*
    Find the shortest path between the start and the goal using Bidirectional search algorithm
    (efficient Two-End Breadth First Search).

    Efficient means that BFS from start to goal and from goal to start are doing step-by-step.
    At every step queues are comparing to choose smaller for next iteration.

    Short description:
    recursion method while found goal from start or start from goal
    or interconnected pages between

    Algorithm has "two" parts:
        one here to compare queues and activate parsing for each iteration until find any match
        second in StepService: compare titles with previous when store new titles from API

        As a result:
            without match: recall check()
            with match: call showResult and go to Path Activity
     */
    private void check() {

        // check if both inputs have internal links, if links 0 - back to mainActivity and try another inputs
        // !! It is checking at the second iteration for each input, after parsing links for each page
        if (qFromStart.size() == 0 ) {
            Toast.makeText(SearchActivity.this, "There are no links from page " + inputStart + " to other Wikipedia articles! Try another start article!", Toast.LENGTH_LONG).show();
            finish();
        }

        else if (qFromGoal.size() == 0) {
            Toast.makeText(SearchActivity.this, "There are no links from page " + inputGoal + " to other Wikipedia articles! Try another goal article!", Toast.LENGTH_LONG).show();
            finish();
        }

        // if inputs have internal links or it's first iteration for inputs
        // here two-end BFS
        else {

            // instantiate new empty Queue at each itearation
            Queue<String> qEmpty = new LinkedList<>();

//             Log.d("test", "qA: " + qFromStart.size());
//             Log.d("test", "qB: " + qFromGoal.size());

            /*
            Compare two Queues, and choose smallest queue to parse
            as adding inputs in onCreate method to both Queues at very first iteration go from start article
             */
            if (qFromStart.size() <= qFromGoal.size()) {

                /*
                Parse every page in Queue, using:
                    current: start with first title, and if Queue store more titles using them inside StepService
                    paramEmpty: default empty String for continue parameter (can be changed in StepService if page have more links than max - 500)
                    Queue From Start as input: loop until any match or empty in StepService
                    Empty Queue for store new part of titles for next iteration
                    sResultFromStart: HashSet that store all checked pages from Start article
                    sResultFromGoal: store all checked pages from Goal article for match checking
                    VolleyResponseListener(): callback to wait the results

                recall check() inside if no match
                or call showResult if it is
                 */

                // get first title from Queue to check
                String current = qFromStart.remove();

                stepService.parseWhileFromStart(current, PARAM_EMPTY, qFromStart, qEmpty, sResultFromStart, sResultFromGoal, new StepService.VolleyResponseListener() {

                    @Override
                    public void onError(String message) {
                        Log.e("volleyError", message);
                    }

                    @Override
                    public void onResponse(Queue<String> next, HashSet<String> result, boolean found, int visited) {

                        // add current result to display on screen
                        blockingQueue.addAll(result);

                        // update input Queue
                        qFromStart = next;
                        // Log.d("test", "Size of output q is: " + qFromStart.size());

                        // collect output result in one HashSet
                        sResultFromStart.addAll(result);
                        // Log.d("test", "Size of pages to compare from root: " + sResultFromStart.size());

                        // update boolean if found goal article or intersection article(s)
                        goal = found;
                        // Log.d("test", "Result: " + goal);

                        // if not found repeat search with updated Queue
                        if (!goal) {
                            check();
                        }

                        // match found!
                        else {
                            // store patsing pages
                            visitedPages = visited;

                            // prepare results and go to new activity
                            matchFound();
                        }
                    }
                });
            }

            // Looking for path from Goal article to start
            // the same algorithm as from Start but call reflect method to parse in backward direction
            else {

                /*
                Parse every page in Queue, using:
                    current: start with first title, and if Queue store more titles using them inside StepService
                    paramEmpty: default empty String for continue parameter (can be changed in StepService if page have more links than max - 500)
                    Queue From Goal as input: loop until any match or empty in StepService
                    Empty Queue for store new part of titles for next iteration
                    sResultFromGoal: HashSet that store all checked pages from Goal article
                    sResultFromStart: store all checked pages from Start article for match checking
                    VolleyResponseListener(): callback to wait the results

                recall check() inside if no match
                or call showResult if it is
                 */

                // get first title from Queue to check
                String current = qFromGoal.remove();

                stepService.parseWhileFromGoal(current, PARAM_EMPTY, qFromGoal, qEmpty, sResultFromGoal, sResultFromStart, new StepService.VolleyResponseListener() {
                    @Override
                    public void onError(String message) {
                        Log.e("volleyError", message);
                    }

                    @Override
                    public void onResponse(Queue<String> next, HashSet<String> result, boolean found, int visited) {

                        // add current result to display on screen
                        blockingQueue.addAll(result);

                        // update input Queue
                        qFromGoal = next;
                        // Log.d("test", "Size of output q is: " + qFromGoal.size());

                        // collect output result in one HashSet
                        sResultFromGoal.addAll(result);
                        // Log.d("test", "Size of pages to compare from goal: " + sResultFromGoal.size());

                        // update boolean if found goal article or intersection article(s)
                        goal = found;
                        // Log.d("test", "Result: " + goal);

                        // if not found repeat search with updated Queue
                        if (!goal) {
                            check();
                        }

                        // match found!
                        else {
                            // store patsing pages
                            visitedPages = visited;

                            // prepare results and go to new activity
                            matchFound();
                        }
                    }
                });
            }
        }
    }

    // This method executed only if boolean goal is true, match found!
    private void matchFound() {

        // prepare path as object
        retrievePath(stepService.matchPages, stepService.allPages);

        // store time for search
        long elapsedTime = System.currentTimeMillis() - start;
        float timeSearch =  elapsedTime / 1000F;

        // go to PathActivity
        // send Path, size of checked pages and time search for next activity
        Intent intent = new Intent(SearchActivity.this, PathActivity.class);
        intent.putExtra("listPath", listPath);
        intent.putExtra("pages", stepService.allPages.size());
        intent.putExtra("time", timeSearch);
        intent.putExtra("visited", visitedPages);
        startActivity(intent);
    }

    /*
    For nested Recycler View in Path Activity it is necessary to create new objects: Steps and Paths.
    This objects will be created by retrieving data from list of interconnection pages and
    comparing this pages with the all storing pages for restore the full path.

    Match pages may include goal or start article!
    */
    private void retrievePath(ArrayList<WikiPage> matched, HashMap<String, WikiPage> all) {

        // instantiated objects for retrieving path
        WikiPage current;
        WikiPage previous;
        WikiPage next;

        // check where interconnect page was found
        // if adjacent has previous - from start search
        // if next - from goal search
        if (matched.get(0).getPreviousPage() != null) {

            // first check if it is one step - direct connection!
            if (matched.get(0).getTitle().equals(inputGoal) && matched.get(0).getPreviousPage().equals(inputStart)) {
                // create single path with single step
                ArrayList<Step> singlePath = new ArrayList<>();
                singlePath.add(new Step(inputStart));
                singlePath.add(new Step(inputGoal));
                listPath.add(new Path(singlePath));
            }

            // retrieve path
            else {

                // retrieve single path for every interconnected page
                for (WikiPage wp: matched) {

                    ArrayList<Step> singlePath = new ArrayList<>();

                    current = all.get(wp.getTitle());

                    // go to previous page from interconnected page until start article
                    if (!wp.getPreviousPage().equals(inputStart)) {
                        previous = all.get(wp.getPreviousPage());
                        singlePath.add(new Step(previous.getTitle()));
                        while(!previous.getPreviousPage().equals(inputStart)) {
                            previous = all.get(previous.getPreviousPage());
                            singlePath.add(new Step(previous.getTitle()));
                        }
                    }

                    // add start article
                    singlePath.add(new Step(inputStart));

                    // reverse to get path from userInput to interconnected page
                    Collections.reverse(singlePath);

                    // add interconnected page
                    singlePath.add(new Step(current.getTitle()));

                    // go to next page until goal article
                    if(!current.getNextPage().equals(inputGoal)) {
                        Log.d("test", String.valueOf(all.get(current.getNextPage())));
                        next = all.get(current.getNextPage());
                        singlePath.add(new Step(next.getTitle()));

                        while (!next.getNextPage().equals(inputGoal)) {
                            next = all.get((next.getNextPage()));
                            singlePath.add(new Step(next.getTitle()));
                        }
                    }

                    // finally add goal article
                    singlePath.add(new Step(inputGoal));

                    // and store the path
                    Path path = new Path(singlePath);
                    listPath.add(path);
                }
            }
        }

        // adjacent has next - found from goal
        else {

            for (WikiPage wp : matched) {

                ArrayList<Step> singlePath = new ArrayList<>();

                //recognise connector page and nextPage
                current = all.get(wp.getTitle());

                // go to previous pages from all pages until find start article
                if (!current.getPreviousPage().equals(inputStart)) {
                    previous = all.get(current.getPreviousPage());

                    singlePath.add(new Step(previous.getTitle()));


                    while(!previous.getPreviousPage().equals(inputStart)) {
                        previous = all.get(previous.getPreviousPage());
                        singlePath.add(new Step(previous.getTitle()));
                    }
                }

                singlePath.add(new Step(inputStart));

                // reverse to get  path from userInput to connector
                Collections.reverse(singlePath);

                // add connector and next page
                singlePath.add(new Step(current.getTitle()));

                if(!wp.getNextPage().equals(inputGoal)) {
                    next = all.get(wp.getNextPage());
                    singlePath.add(new Step(next.getTitle()));
                    while (!next.getNextPage().equals(inputGoal)) {
                        next = all.get((next.getNextPage()));
                        singlePath.add(new Step(next.getTitle()));
                    }
                }

                singlePath.add(new Step(inputGoal));

                Path path = new Path(singlePath);
                listPath.add(path);
            }
        }
    }
}


