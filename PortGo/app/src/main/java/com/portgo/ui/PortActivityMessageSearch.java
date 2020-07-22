package com.portgo.ui;
//

import android.app.LoaderManager;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;


import com.portgo.R;
import com.portgo.adapter.MessageSearchAdapter;
import com.portgo.database.DBHelperBase;
import com.portgo.manager.AccountManager;
import com.portgo.manager.ChatSession;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.MessageEvent;
import com.portgo.manager.PortLoaderManager;
import com.portgo.manager.UserAccount;
import com.portgo.ui.PortGoBaseActivity;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
//
public class PortActivityMessageSearch extends PortGoBaseActivity implements View.OnClickListener,
        SearchView.OnQueryTextListener, LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {
    private final int LOADER_SMS_MESSAGE = 0x3324;
    MessageSearchAdapter mAdapter;
    List<MessageEvent> messages =new ArrayList<>();
    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_message_search);
        ListView listview = (ListView) findViewById(R.id.messages_listView);
        mAdapter = new MessageSearchAdapter(this,messages);
        listview.setAdapter(mAdapter);
        listview.setOnItemClickListener(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        if(toolbar!=null) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.portgo_color_toobar_gray));
            toolbar.setTitle("");
            toolbar.setTitleTextAppearance(this,R.style.ToolBarTextAppearance);
            toolbar.setNavigationIcon(R.drawable.nav_back_ico);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        doSearchQuery(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_message_search,menu);
        initSearchView(menu.findItem(R.id.menu_message_search));
        return true;
    }

    @Override
    public void onClick(View view) {

    }
    private void initSearchView(MenuItem menuSearch) {
        if (menuSearch != null) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menuSearch.getActionView();
            setCursorIcon(searchView);
            ComponentName cn =getComponentName();
            SearchableInfo info = searchManager.getSearchableInfo(cn);
            if (info != null) {
                searchView.setSearchableInfo(info);
            }

            searchView.setOnQueryTextListener(this);

            searchView.setIconifiedByDefault(true);
            searchView.setFocusable(true);
            searchView.setIconified(false);
            searchView.requestFocusFromTouch();

        }
    }

    private void setCursorIcon(SearchView searchView){
        try {

            Class cls = Class.forName("androidx.appcompat.widget.SearchView");
            Field field = cls.getDeclaredField("mSearchSrcTextView");
            field.setAccessible(true);
            TextView tv  = (TextView) field.get(searchView);

            Class[] clses = cls.getDeclaredClasses();
            for(Class cls_ : clses)
            {
                if(cls_.toString().endsWith("androidx.appcompat.widget.SearchView$SearchAutoComplete"))
                {
                    Class targetCls = cls_.getSuperclass().getSuperclass().getSuperclass().getSuperclass();
                    Field cuosorIconField = targetCls.getDeclaredField("mCursorDrawableRes");
                    cuosorIconField.setAccessible(true);
                    cuosorIconField.set(tv, R.drawable.port_edittext_cursor);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doSearchQuery(Intent intent){
        if(intent == null)
            return;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    final String QUERY_STRING ="QUERY_STRING";
    @Override
    public boolean onQueryTextChange(String newText) {
        findViewById(R.id.messages_empty).setVisibility(View.INVISIBLE);
        if(TextUtils.isEmpty(newText)){
            messages.clear();
            mAdapter.notifyDataSetChanged();
        }else {
            Bundle bundle = new Bundle();
            bundle.putString(QUERY_STRING, newText);
            PortLoaderManager.restartLoader(this,loadMgr,LOADER_SMS_MESSAGE, bundle, this);

        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                this.finish();
                break;
            default:
                super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case LOADER_SMS_MESSAGE: {
                String [] args;
                String where = DBHelperBase.MessageColumns.MESSAGE_CONTENT + " LIKE ? OR "+DBHelperBase.MessageColumns.MESSAGE_CONTENT + " LIKE ?",keyword = null;
                if (bundle != null) {
                    keyword = bundle.getString(QUERY_STRING);
                }

                if (!TextUtils.isEmpty(keyword)) {
                    args = new String[]{"%\""+MessageEvent.KEY_TEXT_CONTENT+"\""+":"+"\"" +"%"+keyword+"%",
                            "%\""+MessageEvent.KEY_FILE_NAME+"\""+":"+"\"" +"%"+keyword+"%\",\"mime"};
                    return new CursorLoader(this, DBHelperBase.MessageColumns.CONTENT_URI,
                            null, where, args, DBHelperBase.MessageColumns.DEFAULT_ORDER);
                }
            }

        }
        return null;
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        UserAccount userAccount = AccountManager.getDefaultUser(this);
        String local = userAccount.getFullAccountReamName();
        switch (loader.getId()) {
            case LOADER_SMS_MESSAGE:
                List<MessageEvent> events = new ArrayList<>();
                while (CursorHelper.moveCursorToNext(cursor)) {
                    MessageEvent message = MessageEvent.messageFromCursor(cursor);

                    int sessionid = message.getSessionid();
                    ChatSession session = ChatSession.ChatSessionById(this,sessionid);
                    if(session!=null&&session.getLocalUri().equals(local)) {
                        events.add(message);
                    }
                }
                CursorHelper.closeCursor(cursor);
                messages.clear();
                messages.addAll(events);
                if(mAdapter!=null){
                    if(events.isEmpty()){
                        findViewById(R.id.messages_empty).setVisibility(View.VISIBLE);
                    }else{
                        findViewById(R.id.messages_empty).setVisibility(View.INVISIBLE);
                    }
                    mAdapter.notifyDataSetChanged();
                }
                break;


        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()){
            case LOADER_SMS_MESSAGE:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        MessageEvent event = (MessageEvent) mAdapter.getItem(i);
        if(event!=null){
            int sessionId = event.getSessionid();

            PortActivityRecycleChat.beginChat(this, sessionId,event.getId());
            this.finish();
        }
    }
}