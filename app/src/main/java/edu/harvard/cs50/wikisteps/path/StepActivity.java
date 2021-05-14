 package edu.harvard.cs50.wikisteps.path;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.Iterator;

import edu.harvard.cs50.wikisteps.R;
import edu.harvard.cs50.wikisteps.prefs.Preferences;

public class StepActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textViewTitle;
    private TextView textViewArticle;
    private Button button;

    private static final String HTTP = "https://";

    // get image with 800px max size
    public static final String IMAGE_API = ".wikipedia.org/w/api.php?action=query&prop=pageimages&pithumbsize=800&format=json&titles=";

    // get short description of article
    public static final String ARTICLE_API = ".wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro&explaintext&redirects=1&format=json&titles=";

    // classic Wikipedia page
    public static final String WIKI_URL = ".wikipedia.org/wiki/";

    private String wpcode;

    private String title;

    // construct url for volley
    private String imageUrl;
    private String articleUrl;

    private RequestQueue requestQueue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step);

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        // add up button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        imageView = findViewById(R.id.imageView);
        textViewTitle = findViewById(R.id.textView_title);
        textViewArticle = findViewById(R.id.textView_article);
        button = findViewById(R.id.button_wa);

        title = getIntent().getStringExtra("title");
        textViewTitle.setText(title);

        wpcode = Preferences.getLanguageCode("WPcode", this);

        // construct url for volley
        imageUrl = HTTP + wpcode + IMAGE_API + title;
        articleUrl = HTTP + wpcode + ARTICLE_API + title;

        load();

        // open full article in browser or wiki app
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String openURL = HTTP + wpcode + WIKI_URL + title;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(openURL));
                startActivity(browserIntent);
            }
        });
    }

    // load main image and short description of article using volley
    public void load() {

        JsonObjectRequest requestImg = new JsonObjectRequest(Request.Method.GET, imageUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {
                    JSONObject query = response.getJSONObject("query");
                    JSONObject pages = query.getJSONObject("pages");
                    String pageid = "";
                    Iterator<String> keys = pages.keys();
                    while (keys.hasNext()) {
                        pageid = keys.next();
                    }
                    JSONObject id = pages.getJSONObject(pageid);

                    // if article with image change size of image
                    if (id.has("thumbnail")) {

                        // change imageView here for correct work constraintDimensionRatio
                        ViewGroup.LayoutParams params = imageView.getLayoutParams();
                        params.height = 0;
                        imageView.requestLayout();

                        JSONObject imgContainer = id.getJSONObject("thumbnail");
                        String img = imgContainer.getString("source");

                        // load image with Picasso library
                        Picasso.get().load(img).into(imageView);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("test", String.valueOf(error));
            }
        });

        // load description
        JsonObjectRequest requestShort = new JsonObjectRequest(Request.Method.GET, articleUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject query = response.getJSONObject("query");
                    JSONObject pages = query.getJSONObject("pages");
                    String pageid = "";
                    Iterator<String> keys = pages.keys();
                    while (keys.hasNext()) {
                        pageid = keys.next();
                    }
                    JSONObject id = pages.getJSONObject(pageid);
                    Log.d("test", String.valueOf(id));

                    if (id.has("extract")) {
                        String description = id.getString("extract");

                        // update articles for view without empty spaces
                        if (description.endsWith("\n\n")) {
                            description = description.replaceAll("\n\n","");
                        }

                        // create lines between paragraphs
                        description = description.replaceAll("\n","\n\n");

                        // set text to TextView
                        textViewArticle.setText(description);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        // add both requestes to volley requestQueue
        requestQueue.add(requestImg);
        requestQueue.add(requestShort);
    }

    // go to previous activity if click up button
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }
}
