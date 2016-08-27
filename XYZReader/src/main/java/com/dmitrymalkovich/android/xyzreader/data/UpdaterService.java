package com.dmitrymalkovich.android.xyzreader.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

import com.dmitrymalkovich.android.xyzreader.remote.RemoteEndpointUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * todo 6 make a started service
 */
public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";

    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.dmitrymalkovich.android.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING
            = "com.dmitrymalkovich.android.xyzreader.intent.extra.REFRESHING";

    public UpdaterService() {
        super(TAG);
    }

    /**
     * todo 6b - call this method, this will run in the new thread
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Time time = new Time();

        // check network connection
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Log.w(TAG, "Not online, not refreshing.");
            return;
        }

        // if there is an active network, send broadcast
        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, true));

        // Don't even inspect the intent, we only do one thing, and that's fetch content.
        /**
         * ContentProviderOperation: is used to create, update, delete a set of data in a batch
         * @see <a href="http://www.grokkingandroid.com/better-performance-with-contentprovideroperation/"></a>
         */
        ArrayList<ContentProviderOperation> cpo = new ArrayList<ContentProviderOperation>();

        Uri dirUri = ItemsContract.Items.buildDirUri();

        try {
            JSONArray array = RemoteEndpointUtil.fetchJsonArray();
            if (array == null) {
                throw new JSONException("Invalid parsed item array");
            }

            for (int i = 0; i < array.length(); i++) {
                ContentValues values = new ContentValues();
                JSONObject object = array.getJSONObject(i);
                values.put(ItemsContract.Items.SERVER_ID, object.getString("id"));
                values.put(ItemsContract.Items.AUTHOR, object.getString("author"));
                values.put(ItemsContract.Items.TITLE, object.getString("title"));
                values.put(ItemsContract.Items.BODY, object.getString("body"));
                values.put(ItemsContract.Items.THUMB_URL, object.getString("thumb"));
                values.put(ItemsContract.Items.PHOTO_URL, object.getString("photo"));
                values.put(ItemsContract.Items.ASPECT_RATIO, object.getString("aspect_ratio"));
                time.parse3339(object.getString("published_date"));
                values.put(ItemsContract.Items.PUBLISHED_DATE, time.toMillis(false));

                // get the data from content provider
                Long id = Long.valueOf(object.getString("id"));
                Cursor cursor = getContentResolver().query(ItemsContract.Items.buildDirUri(),
                        ArticleLoader.Query.PROJECTION, ItemsContract.Items.SERVER_ID + "=" + id,
                        null, null);

                // insert in data
                if (cursor == null || !cursor.moveToFirst()) {
                    cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
                }

                // close the cursor
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }

            // start doing bath all data
            getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);

        } catch (JSONException | RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Error updating content.", e);
        }

        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, false));
    }
}
