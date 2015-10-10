package barqsoft.footballscores.service;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.ScoresAppWidgetProvider;
import barqsoft.footballscores.ScoresDBHelper;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.scoresAdapter;

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class WidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private ScoresDBHelper mOpenHelper;
        private static final int mCount = 10;
        private ContentValues[] mItems;
        private Context mContext;
        private int mAppWidgetId;
        private String[] mFragmentDate = new String[1];
        private Cursor mCursor;


        public ListRemoteViewsFactory(Context applicationContext, Intent intent) {
            mContext = applicationContext;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            mItems = (ContentValues[]) intent.getParcelableArrayExtra(ScoresAppWidgetProvider.KEY_DATA);
            SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
            mFragmentDate[0] = mformat.format(new Date(System.currentTimeMillis()));
        }

        public void onCreate() {
            mOpenHelper = new ScoresDBHelper(getApplicationContext());
        }

        @Override
        public void onDataSetChanged() {
            Log.e("WIDGETSERVICE", "OnDataSetChanged triggered");
            if (mCursor != null) {
                mCursor.close();
            }
            mCursor = mOpenHelper.getReadableDatabase().query(
                    DatabaseContract.SCORES_TABLE,
                    null, DatabaseContract.scores_table.DATE_COL + " LIKE ?", mFragmentDate, null, null, null);
        }

        @Override
        public void onDestroy() {
            // In onDestroy() you should tear down anything that was setup for your data source,
            // eg. cursors, connections, etc.
            if(mCursor != null) mCursor.close();
        }

        @Override
        public int getCount() {
            if(mCursor != null) return mCursor.getCount();
            return 0;
        }

        public RemoteViews getViewAt(int position) {
            // Construct a RemoteViews item based on the app widget item XML file, and set the
            // text based on the position.
            RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.scores_list_item);

            mCursor.moveToPosition(position);

            rv.setTextViewText(R.id.away_name, mCursor.getString(scoresAdapter.COL_AWAY));
            rv.setTextViewText(R.id.home_name, mCursor.getString(scoresAdapter.COL_HOME));
            rv.setTextViewText(R.id.date_textview, mCursor.getString(scoresAdapter.COL_MATCHTIME));
            String score = Utilies.getScores(
                    mCursor.getInt(scoresAdapter.COL_HOME_GOALS),
                    mCursor.getInt(scoresAdapter.COL_AWAY_GOALS)
            );
            rv.setTextViewText(R.id.score_textview, score);
            rv.setImageViewResource(R.id.home_crest, Utilies.getTeamCrestByTeamName(
                    mCursor.getString(scoresAdapter.COL_HOME)));
            rv.setImageViewResource(R.id.away_crest, Utilies.getTeamCrestByTeamName(
                    mCursor.getString(scoresAdapter.COL_AWAY)));



            // Next, we set a fill-intent which will be used to fill-in the pending intent template
            // which is set on the collection view in StackWidgetProvider.
            Bundle extras = new Bundle();
            extras.putInt(ScoresAppWidgetProvider.EXTRA_ITEM, position);
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            rv.setOnClickFillInIntent(R.id.root_layout, fillInIntent);


            // Return the RemoteViews object.
            return rv;
        }

        public RemoteViews getLoadingView() {
            // You can create a custom loading view (for instance when getViewAt() is slow.) If you
            // return null here, you will get the default loading view.
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }


    }



}

