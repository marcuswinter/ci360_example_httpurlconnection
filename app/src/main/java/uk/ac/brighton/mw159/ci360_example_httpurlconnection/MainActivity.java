package uk.ac.brighton.mw159.ci360_example_httpurlconnection;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;


public class MainActivity extends ListActivity
{
    private static final String TAG = MainActivity.class.getSimpleName();

    private String mQuery;
    private ArrayList<Item> mItemList;
    private ItemListAdapter mItemListAdapter;
    private ListView mListView;
    private Bitmap mDefaultThumb;
    private ProgressDialog mProgress;

    // handler and messages for async tasks
    private static final int MSG_ITEMLIST_QUERY_COMPLETE = 1;
    private static final int MSG_ITEMLIST_THUMB_AVAILABLE = 2;
    private Handler mHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case MSG_ITEMLIST_QUERY_COMPLETE:
                    if((mProgress != null) && (mProgress.isShowing())) mProgress.dismiss();
                    mItemListAdapter.notifyDataSetChanged();
                    mListView.smoothScrollToPosition(0);
                    updateThumbs();
                    break;


                case MSG_ITEMLIST_THUMB_AVAILABLE: // called for each thumb!
                    mItemListAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };





    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mItemList = new ArrayList<Item>();
        mItemListAdapter = new ItemListAdapter(this, R.layout.list_item, mItemList, false);
        setListAdapter(mItemListAdapter);

        mListView = (ListView) findViewById(android.R.id.list);
        mDefaultThumb = ((BitmapDrawable)(getDrawable(R.drawable.image_default_thumb))).getBitmap();

        ((Button) findViewById(R.id.btn_search)).setOnClickListener(new OnClickListener()
        {
            public void onClick(View view)
            {
                View focused = getCurrentFocus();
                if (focused != null) {
                    focused.clearFocus();
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
                }
                search();
            }
        });
    }




    private final void search()
    {
        mQuery = ((EditText) findViewById(R.id.txt_search)).getText().toString().trim();

        if(mQuery.length() == 0) return;  // sanity check

        mProgress = ProgressDialog.show(this, null, getString(R.string.loading));

        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    String url = "http://www.vam.ac.uk/api/json/museumobject/search?images=1&q="
                               + URLEncoder.encode(mQuery, StandardCharsets.UTF_8.name());
                    String response = getRequestText(url);
                    Log.i(TAG, response);

                    mItemList.clear();

                    JSONObject json = new JSONObject(response);
                    JSONArray records = json.getJSONArray("records");
                    for(int i=0; i<records.length(); i++)
                    {
                        JSONObject obj = records.getJSONObject(i);
                        JSONObject fields = obj.getJSONObject("fields");
                        Item item = new Item(fields.getString("artist"),
                                             fields.optString("date_text", "Unknown"),
                                             fields.getString("primary_image_id"));
                        mItemList.add(item);
                    }
                }
                catch(Exception e)
                {
                    Log.e(TAG, "search()", e);
                }

                mHandler.sendEmptyMessage(MSG_ITEMLIST_QUERY_COMPLETE);

            }
        }).start();
    }



    private final void updateThumbs()
    {
        new Thread(new Runnable()
        {
            public void run()
            {
                ListIterator<Item> it = mItemList.listIterator();
                while(it.hasNext())
                {
                    Item item = it.next();
                    if(item.image_url.length() > 0)
                    {
                        Bitmap thumb = getRequestImage(item.image_url);
                        if(thumb != null)   item.image_thumb = thumb;
                        else                item.image_thumb = mDefaultThumb;
                        mHandler.sendEmptyMessage(MSG_ITEMLIST_THUMB_AVAILABLE);
                    }
                }
            }
        }).start();
    }


    private final String getRequestText(String url)
    {
        String result = null;
        InputStream is = null;
        HttpURLConnection connection = null;

        try
        {
            URL _url = new URL(url);
            connection = (HttpURLConnection) _url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(30 * 1000);
            is = connection.getInputStream();
            InputStreamReader in = new InputStreamReader(is, StandardCharsets.UTF_8);

            StringBuilder mResponseBuffer = new StringBuilder(512);
            char[] mReadBuffer = new char[512];
            int chars_read;
            while((chars_read = in.read(mReadBuffer)) > 0)
            {
                mResponseBuffer.append(mReadBuffer, 0, chars_read);
            }

            result = mResponseBuffer.toString();
        }
        catch (SocketTimeoutException ste)  { /* handle timeout */}
        catch (IOException ioe)             { /* handle IO exception */}
        catch (Exception e)                 { /* handle any other error */
                                              Log.e(TAG, "getRequestText()", e);}
        finally
        {
            if(is != null){try {is.close();} catch (Exception e) {}}
            if(connection != null){connection.disconnect();}
        }

        return result;
    }


    private final Bitmap getRequestImage(String url)
    {
        Bitmap result = null;
        BufferedInputStream bis = null;
        HttpURLConnection connection = null;

        try
        {
            URL _url = new URL(url);
            connection = (HttpURLConnection) _url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(30 * 1000);
            bis = new BufferedInputStream(connection.getInputStream());
            result = BitmapFactory.decodeStream(bis);
        }
        catch (SocketTimeoutException ste)  { /* handle timeout */}
        catch (IOException ioe)             { /* handle IO exception */}
        catch (Exception e)                 { /* handle any other error */
                                              Log.e(TAG, "getRequestText()", e);}
        finally
        {
            if(bis != null){try {bis.close();} catch (Exception e) {}}
            if(connection != null){connection.disconnect();}
        }

        return result;
    }


    //
    // not used here, just for completeness...
    //
    private final String postRequestText(String url, Map<String, String> params)
    {
        String result = null;
        InputStream is = null;
        HttpURLConnection connection = null;

        try
        {
            // build post data
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String,String> param : params.entrySet())
            {
                if (sb.length() != 0) sb.append('&');
                sb.append(param.getKey());
                sb.append('=');
                sb.append(URLEncoder.encode(String.valueOf(param.getValue()), StandardCharsets.UTF_8.name()));
            }
            byte[] postData = sb.toString().getBytes(StandardCharsets.UTF_8);

            // open connection and write data
            URL _url = new URL(url);
            connection = (HttpURLConnection) _url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(30 * 1000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(postData.length));
            connection.setDoOutput(true);
            connection.getOutputStream().write(postData);

            // read response
            is = connection.getInputStream();
            InputStreamReader in = new InputStreamReader(is, StandardCharsets.UTF_8);

            StringBuilder mResponseBuffer = new StringBuilder(512);
            char[] mReadBuffer = new char[512];
            int chars_read;
            while((chars_read = in.read(mReadBuffer)) > 0)
            {
                mResponseBuffer.append(mReadBuffer, 0, chars_read);
            }

            result = mResponseBuffer.toString();
        }
        catch (SocketTimeoutException ste)  { /* handle timeout */}
        catch (IOException ioe)             { /* handle IO exception */}
        catch (Exception e)                 { /* handle any other error */
            Log.e(TAG, "getRequestText()", e);}
        finally
        {
            if(is != null){try {is.close();} catch (Exception e) {}}
            if(connection != null){connection.disconnect();}
        }

        return result;
    }

}
