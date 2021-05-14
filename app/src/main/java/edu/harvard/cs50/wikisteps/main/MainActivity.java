package edu.harvard.cs50.wikisteps.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Field;
import java.util.ArrayList;

import edu.harvard.cs50.wikisteps.R;
import edu.harvard.cs50.wikisteps.fts.DatabaseLanguageCodes;
import edu.harvard.cs50.wikisteps.prefs.Preferences;
import edu.harvard.cs50.wikisteps.search.SearchActivity;

public class MainActivity extends AppCompatActivity {

    private MenuItem languageItem;
    private AutoCompleteTextView startArticle;
    private AutoCompleteTextView goalArticle;
    private Button button;

    // adapter for Autocomplete
    private ArrayAdapter<String> adapter;

    // for check inputs from autocomplete before start algorithm
    private boolean selectedStart;
    private boolean selectedGoal;
    private String lastCheckedStart;
    private String lastCheckedGoal;

    // instantiate Volley library
    private RequestQueue requestQueue;

    // openSearch Wikiedia API: parse only 1 suggestion
    private static final String API = ".wikipedia.org/w/api.php?action=opensearch&format=json&namespace=0&limit=1&search=";
    private static final String HTTP = "https://";

    // Wikipedia language code
    private String wpcode;

    DatabaseLanguageCodes db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        button = findViewById(R.id.button);
        startArticle = findViewById(R.id.actv_startArticle);
        goalArticle = findViewById(R.id.actv_goalArticle);

        db = new DatabaseLanguageCodes(MainActivity.this);

        // first start
        boolean firstStart = Preferences.getFirstStart("firstStart", this);

        if (firstStart) {
            showStartDialog();
        }

        // get wp code from Preferences
        wpcode = Preferences.getLanguageCode("WPcode", MainActivity.this);

        // autocomplete stuff for startArticle with TextWatcher inside
        startArticle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (!startArticle.isPerformingCompletion()) {
                    // for retyping inputs
                    selectedStart = false;

                    String currentInput = startArticle.getText().toString();
//                    Log.d("test", currentInput);
                    String url = HTTP + wpcode + API + currentInput;
//                    Log.d("test", url);
                    fillAutoComplete(url, true);
                }

                else {
                    selectedStart = true;
                    hideKeyboard();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // autocomplete stuff for goalArticle with TextWatcher inside
        goalArticle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!goalArticle.isPerformingCompletion()) {
                    // for retyping inputs
                    selectedGoal = false;

                    String currentInput = goalArticle.getText().toString();
//                    Log.d("test", currentInput);
                    String url = HTTP + wpcode + API + currentInput;
//                    Log.d("test", url);
                    fillAutoComplete(url, false);
                }
                else {
                    selectedGoal = true;
                    hideKeyboard();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // check inputs and go to SearchActivity to start algorithm
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // check if articles are founded from API:
                // by autocomplete inputs or by last checked inputs
                if (selectedStart && selectedGoal
                        || startArticle.getText().toString().equals(lastCheckedStart)
                        && goalArticle.getText().toString().equals(lastCheckedGoal)) {
                    Intent intent = new Intent(MainActivity.this, SearchActivity.class);

                    intent.putExtra("start", startArticle.getText().toString());
                    intent.putExtra("goal", goalArticle.getText().toString());
                    startActivity(intent);
                }
                else {
                    if (!selectedStart && !startArticle.getText().toString().equals(lastCheckedStart)) {
                        Toast.makeText(MainActivity.this, "Start article is incorrect!", Toast.LENGTH_LONG).show();
                    }

                    else if (!selectedGoal && !goalArticle.getText().toString().equals(lastCheckedGoal)) {
                        Toast.makeText(MainActivity.this, "Goal article is incorrect!", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(MainActivity.this, "You need to enter existing articles!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    // helper method for parse Opensearch Wikipedia API
    // called from autocomplete TextChangedListener
    // boolean: true for start, false to goal
    private void fillAutoComplete(String url, boolean inputFromStart) {

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {

                ArrayList<String> arrList=new ArrayList<String>();
                try {
                    JSONArray result = response.getJSONArray(1);
//                    Log.d("test", String.valueOf(result));

                    // store the suggestion
                    if (result.length() > 0) {
                        String temp = result.getString(0);
                        arrList.add(temp);
//                    Log.d("test", String.valueOf(arrList));

                        // pass to autocomplete adapter suggestion
                        adapter = new ArrayAdapter<String>(
                                getApplicationContext(),
                                android.R.layout.simple_list_item_1,
                                arrList);

                        if (inputFromStart) {
                            startArticle.setAdapter(adapter);
                            startArticle.showDropDown();
                            lastCheckedStart = temp;
                        }
                        else {
                            goalArticle.setAdapter(adapter);
                            goalArticle.showDropDown();
                            lastCheckedGoal = temp;
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("test", String.valueOf(error));
            }
        });

        requestQueue.add(request);
    }

    // helper method to hide keybord after choose autocomplete
    private void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

     // add language menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // load menu layout
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.language_menu, menu);

        languageItem = menu.findItem(R.id.item_l);

        // set up current value for code menu title
        languageItem.setTitle(wpcode);

        return true;
    }

    // switch language
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_search:
                SearchView searchView = (SearchView) item.getActionView();

                SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

                // define white theme for suggestions
                SearchView.SearchAutoComplete autoCompleteTextView = (SearchView.SearchAutoComplete) searchView.findViewById(R.id.search_src_text);
                if (autoCompleteTextView != null) {
                    autoCompleteTextView.setDropDownBackgroundDrawable(getResources().getDrawable(R.drawable.abc_popup_background_mtrl_mult));
                }

                // add custom hint
                searchView.setQueryHint("Search language here");

                // change color for cursor in SearchView
                AutoCompleteTextView searchTextView = searchView.findViewById(R.id.search_src_text);
                try {
                    Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
                    mCursorDrawableRes.setAccessible(true);
                    mCursorDrawableRes.set(searchTextView, R.drawable.cursor); //This sets the cursor resource ID to 0 or @null which will make it visible on white background
                } catch (Exception e) {
                }

                // create lists for items from db
                ArrayList<String> suggestions = new ArrayList<>();
                ArrayList<String> codes = new ArrayList<>();

                // define CursorAdapter for bind data with view
                CursorAdapter suggestionAdapter = new SimpleCursorAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        null,
                        new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1},  // new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1},
                        new int[]{android.R.id.text1},
                        0);

                searchView.setSuggestionsAdapter(suggestionAdapter);

                searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                    @Override
                    public boolean onSuggestionSelect(int position) {
                        return false;
                    }

                    @Override
                    public boolean onSuggestionClick(int position) {
                        // set up values from db
                        String current = suggestions.get(position);
                        String code = codes.get(position);

                        Preferences.setLanguageCode("WPcode", code, MainActivity.this);
                        wpcode = Preferences.getLanguageCode("WPcode", MainActivity.this);
                        languageItem.setTitle(wpcode);

                        Toast.makeText(MainActivity.this, "You chose " + current + " language!", Toast.LENGTH_SHORT).show();

                        searchView.clearFocus();
                        return true;
                    }
                });

                // for set up input from keyboard
                searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {

                        // clear items from suggestions lists
                        suggestions.clear();
                        codes.clear();

                        // get data from db
                        Cursor test = db.getWordMatches(newText, null);

                        // debugging cursor
                        // Log.d("WPcodeDatabase", "from Main Activity " + String.valueOf(DatabaseUtils.dumpCursorToString(test) ));

                        // process cursor to add items to suggestions and cursor list
                        if (test != null) {
                            if (test.moveToFirst()) {
                                do {
                                    String suggestion = test.getString(test.getColumnIndex("LANGUAGE"));
                                    suggestions.add(suggestion);
                                    // Log.d("WPcodeDatabase", "suggestion is: " + suggestion);
                                    String code = test.getString((test.getColumnIndex("CODE")));
                                    codes.add(code);
                                    // Log.d("WPcodeDatabase", "code is: " + code);
                                } while (test.moveToNext());
                            }
                        }

                        // create MatrixCursor for CursorAdapter
                        String[] columns = { BaseColumns._ID,
                                SearchManager.SUGGEST_COLUMN_TEXT_1,
                                SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                        };

                        MatrixCursor cursor = new MatrixCursor(columns);

                        for (int i = 0; i < suggestions.size(); i++) {
                            String[] tmp = {Integer.toString(i),suggestions.get(i),suggestions.get(i)};
                            cursor.addRow(tmp);
                        }

                        // add MatrixCursor to adapter
                        suggestionAdapter.swapCursor(cursor);

                        return true;
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // first start dialog for language preferences
    private void showStartDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Default search is on English!")
                .setMessage("Choose another wikipedia language by click on lang code above.")
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create().show();
        Preferences.setLanguageCode("WPcode", "en", this);
        Preferences.setFirstStart("firstStart",false, this);
    }
}