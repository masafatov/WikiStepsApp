package edu.harvard.cs50.wikisteps.fts;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import edu.harvard.cs50.wikisteps.R;

public class DatabaseLanguageCodes {

    private static final String TAG = "WPcodeDatabase";

    //The columns in the WP Code table
    public static final String COL_LANGUAGE = "LANGUAGE";
    public static final String COL_CODE = "CODE";

    private static final String DATABASE_NAME = "WPCodes";
    private static final String FTS_VIRTUAL_TABLE = "FTS";
    private static final int DATABASE_VERSION = 1;

    private final DatabaseOpenHelper databaseOpenHelper;

    public DatabaseLanguageCodes(Context context) {
        databaseOpenHelper = new DatabaseOpenHelper(context);
    }

    private static class DatabaseOpenHelper extends SQLiteOpenHelper {

        private final Context helperContext;
        private SQLiteDatabase mDatabase;

        private static final String FTS_TABLE_CREATE =
                "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
                        " USING fts4 (" +
                        COL_LANGUAGE + ", " +
                        COL_CODE + ")";

        DatabaseOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            helperContext = context;

            // trigger for onCreate/onUpdate methods when creating db object
            getReadableDatabase();

//            Log.d(TAG, "inside DatabaseOpenHelper constructor");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            mDatabase = db;
            mDatabase.execSQL(FTS_TABLE_CREATE);

            loadData();
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }

        // start new thread
        public void loadData() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getJSONObject();
                }
            }).start();
        }

        // read data from json file to JSON object
        // file from https://commons.wikimedia.org/w/api.php?action=sitematrix&smtype=language
        private void getJSONObject()  {
            final Resources resources = helperContext.getResources();
            InputStream inputStream = resources.openRawResource(R.raw.api_wpcode);

            int size;
            try {
                size = inputStream.available();
                byte[] buffer = new byte[size];
                inputStream.read(buffer);
                inputStream.close();
                String myJson = new String(buffer, "UTF-8");
                JSONObject obj = new JSONObject(myJson);
                loadCodes(obj);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }

        // retrieve WP codes from JSONobject
        private void loadCodes(JSONObject obj) {
            try {
                JSONObject matrix = obj.getJSONObject("sitematrix");
                Log.d(TAG, String.valueOf(matrix));

                int count = 0;
                while (matrix.getJSONObject(String.valueOf(count)) != null) {

//                    Log.d(TAG, "inside sitematrix data");

                    JSONObject temp = matrix.getJSONObject(String.valueOf(count));
                    String code = temp.getString("code");
                    String language = temp.getString("localname");

//                    Log.d(TAG, "code is " + code);
//                    Log.d(TAG, "language is " + language);

                    // store to database
                    long id = addLanguage(language, code);
                    if (id < 0) {
                        // Log.e(TAG, "unable to add language: " + language);
                        Log.d(TAG, "unable to add language: " + language);
                    }
                    count++;
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // add codes and languages to db
        private long addLanguage(String language, String code) {

//            Log.d(TAG, "insert values " + language + " " + code);

            ContentValues values = new ContentValues();
            values.put(COL_LANGUAGE, language);
            values.put(COL_CODE, code);

            return mDatabase.insert(FTS_VIRTUAL_TABLE, null, values);
        }
    }

    // add methods for search!
    public Cursor getWordMatches(String query, String[] columns) {
        String selection = COL_LANGUAGE + " MATCH ?";
        // String[] selectionArgs = new String[] {query+"*"};
        String[] selectionArgs = new String[] {"^"+query+"*"};

        return query(selection, selectionArgs, columns);
    }

    private Cursor query(String selection, String[] selectionArgs, String[] columns) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);

        Cursor cursor = builder.query(databaseOpenHelper.getReadableDatabase(),
                columns, selection, selectionArgs, null, null, null);

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }
}