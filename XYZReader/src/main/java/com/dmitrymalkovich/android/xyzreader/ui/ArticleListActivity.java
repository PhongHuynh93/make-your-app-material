package com.dmitrymalkovich.android.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;

import com.dmitrymalkovich.android.xyzreader.R;
import com.dmitrymalkovich.android.xyzreader.data.ArticleLoader;
import com.dmitrymalkovich.android.xyzreader.data.UpdaterService;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
//todo 4
public class ArticleListActivity extends AppCompatActivity
        implements SwipeRefreshLayout.OnRefreshListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recycler_view) RecyclerView mRecyclerView;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    private ArticleListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        ButterKnife.bind(this);

//      todo 5  listen when swipe
        mSwipeRefreshLayout.setOnRefreshListener(this);

        // show a toolbar
        setSupportActionBar(mToolbar);

//        but not showing the title in toolbar
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        int columnCount = getResources().getInteger(R.integer.grid_column_count);

        // create a recyclerview with 2 columns
        StaggeredGridLayoutManager staggeredGridLayoutManager =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(staggeredGridLayoutManager);

        mAdapter = new ArticleListAdapter(null);
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);

        // call the loader to load the data from content provider to supply to the recyclerview
        getSupportLoaderManager().initLoader(0, null, this);

        // if the first time openning the app, connect to service to download datas and save it to db
        if (savedInstanceState == null) {
            startService(new Intent(this, UpdaterService.class));
        }
    }

    /**
     * fixme - start refresh the data everytime the app is restart
     *
     * todo - tại sao register receiver rồi mà lại ko thấy sendBroadcast.
     */
    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    /**
     * fixme - because in onStart, I register Receiver, so in onStop I must unregister it
     * cause when the app is not see anymore, it doesn't need to update the UI.
     */
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                boolean isRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                mSwipeRefreshLayout.setRefreshing(isRefreshing);
            }
        }
    };

    /**
     * create the cursor loader by create the constructor of cursor loader
     * @param i
     * @param bundle
     * @return
     */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    /**
     *
     * @param cursorLoader
     * @param cursor
     */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

//    todo 5b - when swipe, call this method
    @Override
    public void onRefresh() {
        startService(new Intent(this, UpdaterService.class));
    }
}
