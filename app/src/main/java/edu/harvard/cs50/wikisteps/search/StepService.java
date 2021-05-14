package edu.harvard.cs50.wikisteps.search;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;

import edu.harvard.cs50.wikisteps.prefs.Preferences;

public class StepService {

    private Context context;

    private static final String HTTP = "https://";

    // Returns all links from the given pages from Wikipedia API
    private static final String LINKS_URL = ".wikipedia.org/w/api.php?action=query&prop=links&pllimit=max&format=json&plnamespace=0&titles=";

    // Find all pages that link to the given pages from
    private static final String LINKS_HERE_URL = ".wikipedia.org/w/api.php?action=query&prop=linkshere&lhlimit=max&format=json&lhnamespace=0&titles=";

    // default value to retrieve first results fro title article
    private static final String PARAM_EMPTY = "";

    // Wikipedia Code for API
    private final String wpcode;

    // target flags, used in proper methods
    private boolean goal;
    private boolean start;

    private final String inputStart;
    private final String inputGoal;

    //collect all checked title as objects, every object has adjacent title (previous or next)
    HashMap<String, WikiPage> allPages = new HashMap<>();

    // trace the interconnect page(s) for bfs algorithm as objects with adjacent title
    ArrayList<WikiPage> matchPages = new ArrayList<>();

    // count visited Pages
    private int count = 0;

    public StepService(Context context, String inputStart, String inputGoal) {
        this.context = context;
        this.inputStart = inputStart;
        this.inputGoal = inputGoal;
        wpcode = Preferences.getLanguageCode("WPcode", context);
    }

    /*
    callback to call Volley response
    collect output from parser methods:
        Queue next: next portion titles to parse
        HashSet Result: parsing titles from input queue
        boolean: found start/goal or interconnection page(s)
        int visited: number of parsing pages
     */
    public interface VolleyResponseListener {
        void onError(String message);

        void onResponse(Queue<String> next, HashSet<String> result, boolean found, int visited);
    }

    /*
    Parse while match FROM START or check all titles from the input Queue
    compare with goal and already parsed titles from goal
    inputs:
        String titleInput: Wikipedia title to parse
        String paramContinue: param "plcontinue", using when more results then "max" are available
        default is "", using paramEmpty for recall method
        Queue<String> input: pages for parse
        Queue<String> output: empty, store new pages for parse
        HashSet<String> resultFromStart: store all pages titles founded from Start article
        HashSet<String> toCompare: store all pages titles founded from Goal article
        VolleyResponseListener: callback to get data from async volley library

    For retrieving path:
    links from parsing page stored as a new objects,
    where parsing page stored as the adjacent PREVIOUS page.
     */
    public void parseWhileFromStart(String titleInput,
                                    String paramContinue,
                                    Queue<String> input,
                                    Queue<String> output,
                                    HashSet<String> resultFromStart,
                                    HashSet<String> toCompare,
                                    VolleyResponseListener volleyResponseListener) {

        // encode title for URL to avoid ampersand issues etc.
        String urlParse = null;
        try {
            urlParse = HTTP + wpcode + LINKS_URL + URLEncoder.encode(titleInput, StandardCharsets.UTF_8.toString()) + paramContinue;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                urlParse,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // create each time new variables to check if max parse 500 links
                            String lContinue = "";
                            boolean batchcomplete = false;

                            // get first keys from JSON object
                            // and check for "continue" / "batchcomplete"
                            // if so - update lcontinue for new url
                            Iterator<String> root = response.keys();
                            while (root.hasNext()) {
                                String key = root.next();
                                if (key.equals("batchcomplete")) {
                                    batchcomplete = true;
                                }
                                else if (key.equals("continue")) {
                                    JSONObject cont = response.getJSONObject(key);
                                    String addToUrl = cont.getString("plcontinue");
                                    lContinue = "&plcontinue=" + addToUrl;
                                }
                            }

                            // parse JSON
                            JSONObject query = response.getJSONObject("query");
                            JSONObject pages = query.getJSONObject("pages");
                            String pageid = "";

                            Iterator<String> keys = pages.keys();
                            while (keys.hasNext()) {
                                pageid = keys.next();
                            }
                            JSONObject id = pages.getJSONObject(pageid);

                            // current parsing page is "previous" in path
                            // use this param to avoid async storing
                            String previous = id.getString("title");

                            // read every title page and compare
                            if (id.has("links")) {
                                JSONArray jsonArray = id.getJSONArray("links");
                                for (int k = 0; k < jsonArray.length(); k++) {
                                    JSONObject link = jsonArray.getJSONObject(k);
                                    String title = link.getString("title");

                                    // check if goal is matched (or connect page parsing from goal)
                                    if (title.equals(inputGoal) || toCompare.contains(title)) {
                                        goal = true;
//                                        Log.d("test", "checked title: " + title);

                                        // add interconnection page to result map as object with adjacent title
                                        WikiPage connectWP = new WikiPage(title);
                                        connectWP.setPreviousPage(previous);
                                        matchPages.add(connectWP);
                                    }

                                    // store all visited pages
                                    // add to results only unvisited pages
                                    if (!allPages.containsKey(title)) {

                                        WikiPage currentWP = new WikiPage(title);
                                        currentWP.setPreviousPage(previous);

                                        // store every page as object with adjacent title
                                        allPages.put(title, currentWP);

                                        // update output values
                                        resultFromStart.add(title);
                                        output.add(title);
                                    }
                                }
                            }

                            // parse again when more results are available for this title and there are no matches
                            if (!batchcomplete && !goal) {
                                // use param lContinue, storing before from API for next part of titles from current page
                                parseWhileFromStart(previous, lContinue, input, output, resultFromStart, toCompare, volleyResponseListener);
                            }

                            else {
                                // count only pages for stats!
                                count++;

                                // check if goal found and input Queue is not empty
                                // if so retrieve the output: queue with next part of titles, stored titles and boolean with result
                                if (goal || input.isEmpty()) {
                                    volleyResponseListener.onResponse(output, resultFromStart, goal, count);
                                }

                                // create new response
                                else {
                                    String current = input.remove();
                                    parseWhileFromStart(current, PARAM_EMPTY, input, output, resultFromStart, toCompare, volleyResponseListener);
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyResponseListener.onError(error.toString());
                error.printStackTrace();
            }
        });

        // set up RequestQueue
        // https://developer.android.com/training/volley/requestqueue.html#singleton
        MySingleton.getInstance(context).addToRequestQueue(request);
    }

    /*
    The same principle as for parseWhileFromStart

    Parse while found FROM GOAL or check all titles from the input Queue
    compare with start and already parsed titles from start
    input:
        String titleInput: Wikipedia title to parse
        String paramContinue: param "lhcontinue", using when more results then "max" are available
        default is "", using paramEmpty for recall method
        Queue<String> input: pages for parse
        Queue<String> output: empty, store new pages for parse
        HashSet<String> resultFromGoal: store all pages titles founded from Goal article
        HashSet<String> toCompare: store all pages titles founded from Start article
        VolleyResponseListener: callback to get data from async volley library

    For retrieving path:
    links from parsing page stored as a new objects,
    where parsing page stored as the adjacent NEXT page.
    */
    public void parseWhileFromGoal(String titleInput,
                                   String paramContinue,
                                   Queue<String> input,
                                   Queue<String> output,
                                   HashSet<String> resultFromGoal,
                                   HashSet<String> toCompare,
                                   VolleyResponseListener volleyResponseListener) {

        String urlParse = null;
        try {
            urlParse = HTTP + wpcode + LINKS_HERE_URL + URLEncoder.encode(titleInput, StandardCharsets.UTF_8.toString()) + paramContinue;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                urlParse,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // create each time new variables to check if max parse 500 links
                            String lContinue = "";
                            boolean batchcomplete = false;

                            // get first keys from JSON object
                            // and check for "continue" / "batchcomplete"
                            // if so - update lcontinue for new url
                            Iterator<String> root = response.keys();
                            while (root.hasNext()) {
                                String key = root.next();
                                if (key.equals("batchcomplete")) {
                                    batchcomplete = true;
                                }
                                else if (key.equals("continue")) {
                                    JSONObject cont = response.getJSONObject(key);
                                    String addToUrl = cont.getString("lhcontinue");
                                    lContinue = "&lhcontinue=" + addToUrl;
                                }
                            }
                            // parse JSON
                            JSONObject query = response.getJSONObject("query");
                            JSONObject pages = query.getJSONObject("pages");
                            String pageid = "";
                            Iterator<String> keys = pages.keys();
                            while (keys.hasNext()) {
                                pageid = keys.next();
                            }
                            JSONObject id = pages.getJSONObject(pageid);

                            // current parsing page is "next" in path
                            // use this param to avoid async storing
                            String next = id.getString("title");

                            // read every title page and compare
                            if (id.has("linkshere")) {

                                JSONArray jsonArray = id.getJSONArray("linkshere");
                                for (int k = 0; k < jsonArray.length(); k++) {
                                    JSONObject link = jsonArray.getJSONObject(k);

                                    String title = link.getString("title");

                                    // check if start is matched (or connect page parsing from start)
                                    if (title.equals(inputStart) || toCompare.contains(title)) {
                                        start = true;
//                                        Log.d("test", "Checked title " + title);

                                        WikiPage connectWP = new WikiPage(title);
                                        connectWP.setNextPage(next);
                                        matchPages.add(connectWP);
                                    }

                                    // store all visited pages
                                    // add to results only unvisited pages
                                    if (!allPages.containsKey(title)) {
                                        WikiPage currentWP = new WikiPage(title);
                                        // store next page for retrieving path
                                        currentWP.setNextPage(next);

                                        allPages.put(title, currentWP);
                                        resultFromGoal.add(title);
                                        output.add(title);
                                    }
                                }
                            }

                            // parse again if nessesary
                            if (!batchcomplete && !start) {
                                // use param lContinue, storing before from API for next part of titles from current page
                                parseWhileFromGoal(next, lContinue, input, output, resultFromGoal, toCompare, volleyResponseListener);
                            }
                            else {
                                // count only pages for stats!
                                count++;

                                // check if goal found and input Queue is not empty
                                // if so retrieve the output: queue with next part of titles, stored titles and boolean with result
                                if (start || input.isEmpty()) {
                                    volleyResponseListener.onResponse(output, resultFromGoal, start, count);
                                }

                                // if not: parse new page
                                else {
                                    String current = input.remove();
                                    parseWhileFromGoal(current, PARAM_EMPTY, input, output, resultFromGoal, toCompare, volleyResponseListener);
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyResponseListener.onError(error.toString());
                error.printStackTrace();
            }
        });

        // set up RequestQueue
        MySingleton.getInstance(context).addToRequestQueue(request);
    }
}
