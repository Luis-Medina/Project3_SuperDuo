package it.jaschke.alexandria.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import it.jaschke.alexandria.MainActivity;
import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.models.Book;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class BookService extends IntentService {

    private final String LOG_TAG = BookService.class.getSimpleName();

    public static final String FETCH_BOOK = "it.jaschke.alexandria.services.action.FETCH_BOOK";
    public static final String DELETE_BOOK = "it.jaschke.alexandria.services.action.DELETE_BOOK";
    public static final String SAVE_BOOK = "it.jaschke.alexandria.services.action.SAVE_BOOK";

    public static final String EAN = "it.jaschke.alexandria.services.extra.EAN";

    public BookService() {
        super("Alexandria");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (FETCH_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                fetchBook(ean);
            } else if (DELETE_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                deleteBook(ean);
            } else if(SAVE_BOOK.equals(action)){
                final Book book = (Book) intent.getSerializableExtra(EAN);
                saveBook(book);
            }
        }
    }

    private void saveBook(Book book){
        writeBackBook(book);

        if(book.getAuthors().size() > 0) {
            writeBackAuthors(book.getEan(), book.getAuthors());
        }
        if(book.getCategories().size() > 0){
            writeBackCategories(book.getEan(),book.getCategories());
        }

        sendMessage(MainActivity.ACTION_SHOW_MESSAGE, getString(R.string.book_saved), null);
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void deleteBook(String ean) {
        if(ean!=null) {
            getContentResolver().delete(AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)), null, null);
            sendMessage(MainActivity.ACTION_BOOK_DELETED, null, null);
        }
    }

    private void sendMessage(String action, String message, Book bookFetched){
        Intent messageIntent = new Intent();
        messageIntent.setAction(action);
        messageIntent.putExtra(MainActivity.EXTRA_MESSAGE, message);
        messageIntent.putExtra(MainActivity.EXTRA_BOOK, bookFetched);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
    }

    /**
     * Handle action fetchBook in the provided background thread with the provided
     * parameters.
     */
    private void fetchBook(String ean) {

        if(ean == null || ean.length()!=13){
            return;
        }

        Cursor bookEntry = getContentResolver().query(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        if(bookEntry.getCount()>0){
            bookEntry.close();
            sendMessage(MainActivity.ACTION_SHOW_MESSAGE, getString(R.string.book_exists), null);
            return;
        }

        bookEntry.close();

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String bookJsonString = null;

        try {
            final String FORECAST_BASE_URL = "https://www.googleapis.com/books/v1/volumes?";
            final String QUERY_PARAM = "q";

            final String ISBN_PARAM = "isbn:" + ean;

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                    .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }

            if (buffer.length() == 0) {
                return;
            }
            bookJsonString = buffer.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }

        }

        final String ITEMS = "items";

        final String VOLUME_INFO = "volumeInfo";

        final String TITLE = "title";
        final String SUBTITLE = "subtitle";
        final String AUTHORS = "authors";
        final String DESC = "description";
        final String CATEGORIES = "categories";
        final String IMG_URL_PATH = "imageLinks";
        final String IMG_URL = "thumbnail";

        if(bookJsonString == null || bookJsonString.isEmpty()){
            sendMessage(MainActivity.ACTION_SHOW_MESSAGE, getResources().getString(R.string.no_json), null);
            return;
        }

        try {
            JSONObject bookJson = new JSONObject(bookJsonString);
            JSONArray bookArray;
            if(bookJson.has(ITEMS)){
                bookArray = bookJson.getJSONArray(ITEMS);
            }else{
                sendMessage(MainActivity.ACTION_SHOW_MESSAGE, getResources().getString(R.string.not_found), null);
                return;
            }

            JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(VOLUME_INFO);

            String title = bookInfo.getString(TITLE);

            String subtitle = "";
            if(bookInfo.has(SUBTITLE)) {
                subtitle = bookInfo.getString(SUBTITLE);
            }

            String desc="";
            if(bookInfo.has(DESC)){
                desc = bookInfo.getString(DESC);
            }

            String imgUrl = "";
            if(bookInfo.has(IMG_URL_PATH) && bookInfo.getJSONObject(IMG_URL_PATH).has(IMG_URL)) {
                imgUrl = bookInfo.getJSONObject(IMG_URL_PATH).getString(IMG_URL);
            }

            Book book = new Book(ean, title, subtitle, desc, imgUrl);
            if(bookInfo.has(AUTHORS)) {
                for (int i = 0; i < bookInfo.getJSONArray(AUTHORS).length(); i++) {
                    book.getAuthors().add(bookInfo.getJSONArray(AUTHORS).getString(i));
                }
            }
            if(bookInfo.has(CATEGORIES)){
                for (int i = 0; i < bookInfo.getJSONArray(CATEGORIES).length(); i++) {
                    book.getCategories().add(bookInfo.getJSONArray(CATEGORIES).getString(i));
                }
            }

            sendMessage(MainActivity.ACTION_BOOK_FETCHED, null, book);

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error ", e);
        }
    }

    private void writeBackBook(Book book) {
        ContentValues values= new ContentValues();
        values.put(AlexandriaContract.BookEntry._ID, book.getEan());
        values.put(AlexandriaContract.BookEntry.TITLE, book.getTitle());
        values.put(AlexandriaContract.BookEntry.IMAGE_URL, book.getImgUrl());
        values.put(AlexandriaContract.BookEntry.SUBTITLE, book.getSubtitle());
        values.put(AlexandriaContract.BookEntry.DESC, book.getDesc());
        getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI,values);
    }

    private void writeBackAuthors(String ean, ArrayList<String> authors)  {
        ContentValues values= new ContentValues();
        for (int i = 0; i < authors.size(); i++) {
            values.put(AlexandriaContract.AuthorEntry._ID, ean);
            values.put(AlexandriaContract.AuthorEntry.AUTHOR, authors.get(i));
            getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);
            values= new ContentValues();
        }
    }

    private void writeBackCategories(String ean, ArrayList<String> categories){
        ContentValues values= new ContentValues();
        for (int i = 0; i < categories.size(); i++) {
            values.put(AlexandriaContract.CategoryEntry._ID, ean);
            values.put(AlexandriaContract.CategoryEntry.CATEGORY, categories.get(i));
            getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
            values= new ContentValues();
        }
    }
 }