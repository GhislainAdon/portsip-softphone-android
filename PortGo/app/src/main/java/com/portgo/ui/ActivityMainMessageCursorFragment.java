package com.portgo.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
//import android.database.CursorJoiner;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.R;
import com.portgo.adapter.MessageAdapter;
import com.portgo.database.DBHelperBase;

import com.portgo.database.DataBaseManager;
import com.portgo.manager.AccountManager;
import com.portgo.manager.ChatSessionForShow;
import com.portgo.manager.Contact;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.MessageEvent;
import com.portgo.manager.ChatSession;
import com.portgo.manager.NotificationUtils;
import com.portgo.manager.PortLoaderManager;
import com.portgo.manager.PortSipCall;
import com.portgo.manager.UserAccount;
import com.portgo.view.IconTipsMenu;
import com.portgo.view.SlideMenu;
import com.portgo.view.SlideMenuCreator;
import com.portgo.view.SlideMenuItem;
import com.portgo.view.SlideMenuListView;
import com.portgo.view.emotion.data.EmotionDataManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import static com.portgo.BuildConfig.ENABLEVIDEO;
import static com.portgo.BuildConfig.HASVIDEO;

public class ActivityMainMessageCursorFragment extends PortBaseFragment implements View.OnClickListener,
		Toolbar.OnMenuItemClickListener,LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, SearchView.OnQueryTextListener, Observer {
	private SlideMenuListView mListView;

    private MessageAdapter mAdapter;
	private Toolbar toolbar;
    private final int LOADER_SMS_SESSION= 0x3324;
    private final int LOADER_PRESENCE_MESSAGE = 0x44;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PortLoaderManager.initLoader(baseActivity,loadMgr,LOADER_SMS_SESSION,null,this);
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
        EmotionDataManager manager = EmotionDataManager.getInstance();
        manager.loadEmotion(getActivity());
        return inflater.inflate(R.layout.activity_main_message_fragment, null);
	}

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        baseActivity.mContactMgr.getObservableContacts().addObserver(this);
        initView(view);
		toolbar = (Toolbar) view.findViewById(R.id.toolBar);
		showToolBar(view,getString(R.string.portgo_title_message));

		toolbar.inflateMenu(R.menu.menu_message);
        toolbar.setOnMenuItemClickListener(this);
    }

    @Override
    public void onDestroyView() {
        baseActivity.mContactMgr.getObservableContacts().deleteObserver(this);
        super.onDestroyView();
    }

    List<ChatSessionForShow> sessions= new ArrayList<>();
	void initView(View view) {
		mAdapter = new MessageAdapter(baseActivity,sessions);
		mListView = (SlideMenuListView) view.findViewById(R.id.messages_listView);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        SlideMenuCreator creator = new SlideMenuCreator() {

            @Override
            public void create(SlideMenu menu) {
                if(HASVIDEO) {
                    SlideMenuItem videoItem = new SlideMenuItem.Builder(baseActivity).setWidth((int) baseActivity.getResources().getDimension(R.dimen.slide_item_width)).
                            setBackground(baseActivity.getResources().getDrawable(R.color.portgo_color_lightgray))
                            .setTitle(getString(R.string.string_Video_Call)).build();
                    menu.addMenuItem(videoItem);
                }
                    SlideMenuItem audioItem = new SlideMenuItem.Builder(baseActivity).setWidth((int) baseActivity.getResources().getDimension(R.dimen.slide_item_width)).
                            setBackground(baseActivity.getResources().getDrawable(R.color.portgo_color_blue))
                            .setTitle(getString(R.string.string_Audio_Call)).build();
                    menu.addMenuItem(audioItem);

                SlideMenuItem deleteItem = new SlideMenuItem.Builder(baseActivity).setWidth((int) baseActivity.getResources().getDimension(R.dimen.slide_item_width)).
                        setBackground(baseActivity.getResources().getDrawable(R.color.portgo_color_red)).
                        setTitle(getString(R.string.string_Delete)).build();


                menu.addMenuItem(deleteItem);
            }
        };

        mListView.setMenuCreator(creator);
        mListView.setOnMenuItemClickListener(new SlideMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SlideMenu menu, int index) {
                ChatSessionForShow sessionForShow = (ChatSessionForShow) mAdapter.getItem(position);
                ChatSession session = sessionForShow.getSession();
                if(!HASVIDEO) {
                    index+=1;//
                }
                switch (index) {
                    case 0:
                        if(ENABLEVIDEO) {
                            baseActivity.makeCall(session.getRemoteId(), session.getRemoteUri(), PortSipCall.MEDIATYPE_AUDIOVIDEO);
                        }
                        break;
					case 1:
					    baseActivity.makeCall(session.getRemoteId(), session.getRemoteUri(), PortSipCall.MEDIATYPE_AUDIO);
						break;
					case 2:
					    DataBaseManager.deleteSession(getActivity(),session.getId());
                        break;
                }
                return false;
            }
        });
	}

    @Override
    public boolean onKeyBackPressed(FragmentManager manager, Map<Integer, Fragment> fragments, Bundle result) {
        if(mListView.getChoiceMode()==AbsListView.CHOICE_MODE_MULTIPLE){
            exitSelectStatus();
            return true;
        }else {
            return super.onKeyBackPressed(manager, fragments, result);
        }
    }

    @Override
	public void onResume() {
		super.onResume();
//		if(mAdapter!=null){
//			mAdapter.notifyDataSetChanged();
//		}

	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()){
            case R.id.bottombar_left:
                {
                    ArrayList<Long> items = mAdapter.getSelectItems();
                    for (long itemId : items) {
                        Cursor cursor = CursorHelper.resolverQuery(getActivity().getContentResolver(),DBHelperBase.MessageColumns.CONTENT_URI,null,
                                DBHelperBase.MessageColumns._ID+"='"+itemId+"'",null,null);
                        if(CursorHelper.moveCursorToFirst(cursor)) {
                            MessageEvent event = MessageEvent.messageFromCursor(cursor);
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(DBHelperBase.MessageColumns.MESSAGE_SEEN, 0);
                            String selection = DBHelperBase.MessageColumns.MESSAGE_SESSION_ID + "=?";
                            getActivity().getContentResolver().update(DBHelperBase.MessageColumns.CONTENT_URI, contentValues, selection,
                                    new String[]{""+itemId});
                        }
                        CursorHelper.closeCursor(cursor);
                    }
                    exitSelectStatus();
                }

                break;
            case R.id.bottombar_right:
                {
                    ArrayList<Long> items = mAdapter.getSelectItems();
                    Long[]deleteSessions = (Long[]) items.toArray(new Long[items.size()]);
                    DataBaseManager.deleteSessions(getActivity(),deleteSessions);
                    exitSelectStatus();
                }
                break;
        }
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()){
            case R.id.menu_search:
                Intent intent = new Intent(getActivity(),PortActivityMessageSearch.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                getActivity().startActivity(intent);
                break;
			case R.id.menu_message_new:
                PortActivityRecycleChat.beginChat(baseActivity, null,0,null);
				break;
            case R.id.menu_message_select:
                mAdapter.setSelectAll();
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.menu_message_clear:
                mAdapter.setSelectNone();
                mAdapter.notifyDataSetChanged();
                break;
        }
        updateSelectStatus();
        return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        UserAccount userAccount = AccountManager.getDefaultUser(baseActivity);
        String [] args;
        if(userAccount == null) {
            return null;
        }
        switch (i) {
            case LOADER_SMS_SESSION: {
                    String local = userAccount.getFullAccountReamName();
                    String where = "("+DBHelperBase.ChatSessionColumns.SESSION_LOCAL_URI+"=?) AND (("+DBHelperBase.ChatSessionColumns.SESSION_DELETE+"<>1) OR " +
                            "("+DBHelperBase.ChatSessionColumns.SESSION_DELETE +" is null))";                 
                    args = new String[]{local};
                    return new CursorLoader(baseActivity, DBHelperBase.ViewChatSessionColumns.CONTENT_URI,
                            null, where, args, DBHelperBase.ChatSessionColumns.DEFAULT_ORDER);
            }

            case LOADER_PRESENCE_MESSAGE:{
                String where = DBHelperBase.SubscribeColumns.SUBSCRIB_ACCTION + " =? AND "+
                        DBHelperBase.SubscribeColumns.SUBSCRIB_SEEN+"=?";
                return new CursorLoader(getActivity(), DBHelperBase.SubscribeColumns.CONTENT_URI,
                        null, where, new String[]{"" + DBHelperBase.SubscribeColumns.ACTION_NONE,"" +0}, null);// DBHelperBase.SubscribeColumns.DEFAULT_ORDER);
            }

        }
        return null;
	}

    int presenceSize = 0;
    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
			case LOADER_SMS_SESSION:
			    List<ChatSessionForShow> events = new ArrayList<>();
                while (CursorHelper.moveCursorToNext(cursor)) {
                    ChatSession session = ChatSession.ChatSessionFormViewCursor(cursor);
                    ChatSessionForShow sessionForShow = new ChatSessionForShow(session);
                    if (session.getContactid() > 0) {
                        Contact contact = baseActivity.mContactMgr.getObservableContacts().get(session.getContactid());
                        sessionForShow.setAttachContact(contact);
                    }
                    events.add(sessionForShow);

                }
                synchronized (sessions) {
                    sessions.clear();
                    sessions.addAll(events);
                }
                if(mAdapter!=null){
                    mAdapter.notifyDataSetChanged();
                }
				break;
            case LOADER_PRESENCE_MESSAGE:
                MenuItem item = toolbar.getMenu().findItem(R.id.menu_message_presence);
                IconTipsMenu actionProvider = (IconTipsMenu) MenuItemCompat.getActionProvider(item);
                presenceSize = cursor.getCount();
                if(cursor.getCount()>0){
                    actionProvider.setTipsVisable(View.VISIBLE);
                    actionProvider.setNumTips(presenceSize);
                }else{
                    actionProvider.setTipsVisable(View.INVISIBLE);
                }
                break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()){
            case LOADER_SMS_SESSION:
                break;
        }
	}

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if(((ListView)parent).getChoiceMode()== AbsListView.CHOICE_MODE_MULTIPLE) {
            mAdapter.setItemCheck(view,id,!mAdapter.isItemChecked(id));
            updateSelectStatus();
        }else {
            if (AccountManager.getInstance().getLoginState() != UserAccount.STATE_ONLINE) {
                Toast.makeText(baseActivity, R.string.please_login_tips, Toast.LENGTH_SHORT).show();
                return;
            }

            PortActivityRecycleChat.beginChat(baseActivity, (int) id,-1);
        }
    }

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
		enterSelectStatus();
		if(mListView.getChoiceMode()== AbsListView.CHOICE_MODE_MULTIPLE) {
            mAdapter.setItemCheck(view,id,!mAdapter.isItemChecked(id));
            updateSelectStatus();
		}
		return true;
	}


	private void enterSelectStatus(){
		View  bottomView = LayoutInflater.from(baseActivity).inflate(R.layout.view_bottombar,null);
		TextView txread = (TextView) bottomView.findViewById(R.id.bottombar_left);
        TextView txdelete = (TextView) bottomView.findViewById(R.id.bottombar_right);
        txread.setOnClickListener(this);
        txdelete.setOnClickListener(this);
        txread.setText(R.string.already_read);

		FrameLayout bottomBar = (FrameLayout) baseActivity.findViewById(R.id.activity_main_bottombar);
		bottomBar.findViewById(R.id.activity_main_tabs).setVisibility(View.INVISIBLE);
		bottomBar.addView(bottomView);
		Toolbar toolbar = (Toolbar) getView().findViewById(R.id.toolBar);

		if(toolbar!=null) {
            toolbar.setNavigationIcon(R.drawable.nav_back_ico);
			toolbar.setNavigationOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					exitSelectStatus();
				}
			});
			toolbar.getMenu().setGroupVisible(R.id.group_message_normal,false);
			toolbar.getMenu().setGroupVisible(R.id.group_message_select,true);
			toolbar.invalidate();
			toolbar.setTitle(R.string.please_select);

		}

		mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
		if(mAdapter!=null)
			mAdapter.notifyDataSetChanged();
	}

	private void exitSelectStatus(){
		FrameLayout bottomBar = (FrameLayout) baseActivity.findViewById(R.id.activity_main_bottombar);
		View bottomView = bottomBar.findViewById(R.id.bottom_toolbar);
		if(bottomView!=null){
			bottomBar.removeView(bottomView);
		}
		bottomBar.findViewById(R.id.activity_main_tabs).setVisibility(View.VISIBLE);
		Toolbar toolbar = (Toolbar) getView().findViewById(R.id.toolBar);

		if(toolbar!=null) {
			toolbar.setNavigationIcon(null);
			toolbar.setNavigationOnClickListener(null);
            toolbar.getMenu().setGroupVisible(R.id.group_message_normal,true);
            toolbar.getMenu().setGroupVisible(R.id.group_message_select,false);
			toolbar.invalidate();
			toolbar.setTitle(R.string.portgo_title_message);
		}

		mListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
		if(mAdapter!=null)
			mAdapter.notifyDataSetChanged();
	}

	private void updateSelectStatus(){
        if(mListView.getChoiceMode()!=AbsListView.CHOICE_MODE_MULTIPLE)
            return;
        if(mAdapter!=null&&!mAdapter.isAllSelect()){
            toolbar.getMenu().findItem(R.id.menu_message_select).setVisible(true);
            toolbar.getMenu().findItem(R.id.menu_message_clear).setVisible(false);

        }else{
            toolbar.getMenu().findItem(R.id.menu_message_clear).setVisible(true);
            toolbar.getMenu().findItem(R.id.menu_message_select).setVisible(false);
        }
        int selectSize = mAdapter.getSelectItems().size();

        FrameLayout bottomBar = (FrameLayout) baseActivity.findViewById(R.id.activity_main_bottombar);
        View bottomView = bottomBar.findViewById(R.id.bottom_toolbar);
        TextView txread = (TextView) bottomView.findViewById(R.id.bottombar_left);
        TextView txdelete = (TextView) bottomView.findViewById(R.id.bottombar_right);
        txread.setEnabled(selectSize>0);
        txdelete.setEnabled(selectSize>0);
        toolbar.setNavigationIcon(R.drawable.nav_back_ico);

        if(selectSize>0) {
            toolbar.setTitle(String.format(getString(R.string.select_sum), selectSize));
        }else {
            toolbar.setTitle(R.string.please_select);
        }
    }

	private void initSearchView(MenuItem menuSearch){
		if(menuSearch!=null) {
			SearchManager searchManager = (SearchManager) baseActivity.getSystemService(Context.SEARCH_SERVICE);
			SearchView searchView = (SearchView) menuSearch.getActionView();
            setCursorIcon(searchView);

            ComponentName cn = new ComponentName(baseActivity,PortActivityMessageSearch.class);

			SearchableInfo info = searchManager.getSearchableInfo(cn);
            if(info!=null) {
                searchView.setSearchableInfo(info);
            }
			searchView.setOnQueryTextListener(this);
            searchView.setOnCloseListener(this);
			searchView.setIconifiedByDefault(false);

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

    @Override
    public void update(Observable observable, Object o) {
        synchronized (sessions){
            for(ChatSessionForShow sessionForShow:sessions){
                ChatSession session= sessionForShow.getSession();
                if(session.getContactid()>0){
                    sessionForShow.setAttachContact(baseActivity.mContactMgr.getObservableContacts().get(session.getContactid()));
                }
            }
        }
    }

    public boolean onClose() {
        return true;
    }
}
