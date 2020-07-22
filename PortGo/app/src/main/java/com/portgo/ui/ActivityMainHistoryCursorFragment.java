package com.portgo.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ContentProviderOperation;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.portgo.R;
import com.portgo.adapter.HistoryAVCallAdapter;
import com.portgo.database.DBHelperBase;
import com.portgo.database.DataBaseManager;
import com.portgo.database.UriMactherHepler;
import com.portgo.manager.AccountManager;
import com.portgo.manager.Contact;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.HistoryAVCallEvent;
import com.portgo.manager.HistoryAVCallEventForList;
import com.portgo.manager.PortLoaderManager;
import com.portgo.manager.UserAccount;
import com.portgo.view.SlideMenu;
import com.portgo.view.SlideMenuCreator;
import com.portgo.view.SlideMenuItem;
import com.portgo.view.SlideMenuListView;

import java.util.ArrayList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import static com.portgo.BuildConfig.ENABLEIM;
import static com.portgo.BuildConfig.HASIM;


public class ActivityMainHistoryCursorFragment extends PortBaseFragment implements AdapterView.OnItemClickListener
        ,AdapterView.OnItemLongClickListener, TabLayout.OnTabSelectedListener,
        Toolbar.OnMenuItemClickListener,LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener, Observer {
	private static final int LOADER_ID = 0x795;
	private HistoryAVCallAdapter mAdapter;
	private SlideMenuListView mListView;
	private Toolbar toolbar;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
        return  inflater.inflate(R.layout.activity_main_history_fragment, null);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        initView(view);

        baseActivity.mContactMgr.getObservableContacts().addObserver(this);
		toolbar = (Toolbar) view.findViewById(R.id.toolBar);
        toolbar.setTitle(R.string.portgo_title_history);
		toolbar.inflateMenu(R.menu.menu_history);
		toolbar.setOnMenuItemClickListener(this);
		showToolBar(toolbar,null);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);//
	}

    final ArrayList<HistoryAVCallEventForList> events= new ArrayList<>();
    private void initView(View view){
		mListView = (SlideMenuListView) view.findViewById(R.id.screen_tab_history_listView);
        addSlideMenu(mListView);
		mAdapter = new HistoryAVCallAdapter(baseActivity,events,mMoreDetailsClick);

		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_history_all));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_history_unreceived));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setOnTabSelectedListener(this);
        onTabSelected(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()));
    }

	@Override
	public void onTabReselected(TabLayout.Tab tab) {}

	final String CONSTRAIN = "query_constrain";
	final String HISTORY_MISS = DBHelperBase.HistoryColumns.HISTORY_CONNECTED+"=0";
	@Override
	public void onTabSelected(TabLayout.Tab tab) {
		indexSelect = tab.getPosition();
		Bundle bundle = new Bundle();
		switch (indexSelect) {
			case 0:
				bundle =null;
				break;
			case 1:
                bundle = new Bundle();
				bundle.putString(CONSTRAIN,HISTORY_MISS);
				break;
		}

		PortLoaderManager.restartLoader(baseActivity,loadMgr,LOADER_ID,bundle,this);
        initMenu();
	}

	@Override
	public void onTabUnselected(TabLayout.Tab tab){
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(((ListView)parent).getChoiceMode()==AbsListView.CHOICE_MODE_MULTIPLE){
            mAdapter.setItemCheck((int) id,!mAdapter.isItemChecked((int)id));
			mAdapter.notifyDataSetChanged();
			updateSelectStatus();
        }else{
            HistoryAVCallEventForList item = (HistoryAVCallEventForList) mAdapter.getItem(position);
            baseActivity.makeCall(item.getEvent().getRemoteID(),item.getEvent().getRemoteUri(),item.getEvent().getMediaType().ordinal());
        }
	}

	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        enterSelectStatus();
        if(mListView.getChoiceMode()== AbsListView.CHOICE_MODE_MULTIPLE) {
            mAdapter.setItemCheck((int) id,!mAdapter.isItemChecked((int)id));
			updateSelectStatus();
        }
        return true;
	}

	View.OnClickListener mMoreDetailsClick = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
            HistoryAVCallEventForList item = (HistoryAVCallEventForList) view.getTag();
			Intent intent = new Intent();
            String remote = item.getEvent().getRemoteUri();
            int count = item.getCount();
			intent.putExtra(PortActivityHistoryDetail.REMOTE,item.getEvent().getRemoteID());
            intent.putExtra(PortActivityHistoryDetail.COUNT,count);
			intent.putExtra(PortActivityHistoryDetail.EVENT_ID,item.getEvent().getId());
            intent.putExtra(PortActivityHistoryDetail.MISS,indexSelect==1);
			intent.setClass(baseActivity, PortActivityHistoryDetail.class);
			baseActivity.startActivity(intent);
		}
	};

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch(item.getItemId()){
			case R.id.menu_history_selectall:
				if(mAdapter.isAllSelect()){
                	mAdapter.setSelectNone();
				}else{
					mAdapter.setSelectALL();
				}
				mAdapter.notifyDataSetChanged();
				break;
		}
		updateSelectStatus();
		return true;
	}

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
	public void  onDestroyView (){
        baseActivity.mContactMgr.getObservableContacts().deleteObserver(this);
		super.onDestroyView();
	}

	void initMenu(){
        if(toolbar==null)
            return;
		int size  = toolbar.getMenu().size();
		for(int index=0;index<size;index++){
            MenuItem item = toolbar.getMenu().getItem(index);
			updateMenuItem(item);
		}
	}

	private void addSlideMenu(SlideMenuListView listView){
		SlideMenuCreator creator = new SlideMenuCreator() {

			@Override
			public void create(SlideMenu menu) {
				if(HASIM) {
					SlideMenuItem messageItem = new SlideMenuItem.Builder(baseActivity).
							setWidth((int) getResources().getDimension(R.dimen.slide_item_width)).
							setBackground(getResources().getDrawable(R.color.portgo_color_lightgray))
							.setTitleSize(getResources().getInteger(R.integer.slide_item_titlesize))
							.setTitle(getString(R.string.string_Message)).setTitleColor(getResources().getColor(R.color.portgo_color_white)).build();
					menu.addMenuItem(messageItem);
				}
				SlideMenuItem deleteItem = new SlideMenuItem.Builder(baseActivity).
						setWidth((int) getResources().getDimension(R.dimen.slide_item_width)).
						setBackground(getResources().getDrawable(R.color.portgo_color_red)).
						setTitleColor(getResources().getColor(R.color.portgo_color_white))
                        .setTitleSize(getResources().getInteger(R.integer.slide_item_titlesize))
						.setTitle(getString(R.string.string_Delete)).build();

				menu.addMenuItem(deleteItem);
			}
		};
		listView.setMenuCreator(creator);

		listView.setOnMenuItemClickListener(new SlideMenuListView.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(int position, SlideMenu menu, int index) {
                HistoryAVCallEventForList eventForList = null;
                HistoryAVCallEvent event;
				if(!HASIM){
					index+=1;//跳过IM按钮
				}
                switch (index) {
                    case 0:
                    	if(ENABLEIM) {
							eventForList = (HistoryAVCallEventForList) mAdapter.getItem(position);
							event = eventForList.getEvent();
							PortActivityRecycleChat.beginChat(getActivity(), event.getRemoteUri(), event.getContactId(), event.getDisplayName());
						}
                        break;
					case 1:
						eventForList = (HistoryAVCallEventForList) mAdapter.getItem(position);
						event = eventForList.getEvent();
						baseActivity.getContentResolver().delete(DBHelperBase.HistoryColumns.CONTENT_URI,
								DBHelperBase.HistoryColumns.HISTORY_REMOTE_ID+"='"+event.getRemoteID()+"' AND "+
								DBHelperBase.HistoryColumns.HISTORY_CONNECTED+"='"+(event.getConnect()?1:0)+"' AND "+
								DBHelperBase.HistoryColumns.HISTORY_CALLOUT+"='"+(event.getCallOut()?1:0)+"' AND "+
								DBHelperBase.HistoryColumns.HISTORY_MIDIATYPE +"="+event.getMediaType().ordinal(),null);
						mAdapter.notifyDataSetChanged();
						break;
				}
				return false;
			}
		});
	}

	void updateMenuItem(MenuItem item){
        if(item==null)
            return;
		switch (item.getItemId()){
			case R.id.menu_history_selectall:
				break;
			default:
				break;
		}
	}
	int limit = 100;
	int totalSize  =100;
	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		UserAccount userAccount = AccountManager.getDefaultUser(baseActivity);
		if(userAccount!=null) {
			String local = userAccount.getFullAccountReamName();
			String constrain = "";
			if (bundle != null) {
				constrain = bundle.getString(CONSTRAIN);
			}
			if (!TextUtils.isEmpty(constrain)) {
				constrain += " AND ";
			}

			constrain += DBHelperBase.HistoryColumns.HISTORY_LOCAL + "=?";

            String[] pro = new String[]{
                    DBHelperBase.HistoryColumns._ID,
                    "COUNT(*) as size",
                     DBHelperBase.HistoryColumns.HISTORY_MIDIATYPE,
                     DBHelperBase.HistoryColumns.HISTORY_STARTTIME ,
                    DBHelperBase.HistoryColumns.HISTORY_ENDTIME ,
                     DBHelperBase.HistoryColumns.HISTORY_INCALLTIME ,
                    DBHelperBase.HistoryColumns.HISTORY_LOCAL,
                     DBHelperBase.HistoryColumns.HISTORY_REMOTE_ID ,
                    DBHelperBase.HistoryColumns.HISTORY_DISPLAYNAME,
                     DBHelperBase.HistoryColumns.HISTORY_HASRECORD,
                    DBHelperBase.HistoryColumns.HISTORY_GROUP,
                     DBHelperBase.HistoryColumns.HISTORY_CALLID ,
                    DBHelperBase.HistoryColumns.HISTORY_CALLOUT,
                     DBHelperBase.HistoryColumns.HISTORY_SEEN ,
                    DBHelperBase.HistoryColumns.HISTORY_CONNECTED ,
                    DBHelperBase.RemoteColumns.REMOTE_CONTACT_ID,
                    DBHelperBase.RemoteColumns.REMOTE_CONTACT_TYPE,
                    DBHelperBase.RemoteColumns.REMOTE_URI,
                    DBHelperBase.RemoteColumns.REMOTE_DISPPLAY_NAME
            };
			return new CursorLoader(baseActivity, DBHelperBase.ViewHistoryColumns.CONTENT_URI,
                    pro, constrain, new String[]{local}, DBHelperBase.ViewHistoryColumns.DEFAULT_ORDER);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
			case LOADER_ID:
                events.clear();
			    while (CursorHelper.moveCursorToNext(cursor)){
                    int indexSize = cursor.getColumnIndex("size");
                    if(indexSize>=0){
                        indexSize = cursor.getInt(indexSize);
                    }
                    HistoryAVCallEvent event = HistoryAVCallEvent.historyAVCallEventFromViewCursor(getActivity(),cursor);
                    HistoryAVCallEventForList item = new HistoryAVCallEventForList(event);
                    item.setCount(indexSize);
                    item.setAttachContact(baseActivity.mContactMgr.getObservableContacts().get(event.getContactId()));
                    events.add(item);
                }

                mAdapter.notifyDataSetChanged();
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
        events.clear();
	}

    private void enterSelectStatus(){
        View  bottomView = LayoutInflater.from(baseActivity).inflate(R.layout.view_bottombar,null);
        bottomView.findViewById(R.id.bottombar_right).setOnClickListener(this);
		bottomView.findViewById(R.id.bottombar_left).setVisibility(View.GONE);
        FrameLayout bottomBar = (FrameLayout) baseActivity.findViewById(R.id.activity_main_bottombar);
        bottomBar.findViewById(R.id.activity_main_tabs).setVisibility(View.INVISIBLE);
        bottomBar.addView(bottomView);

        if(toolbar!=null) {
			toolbar.setNavigationIcon(R.drawable.nav_back_ico);
			toolbar.setNavigationOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					exitSelectStatus();
				}
			});
			toolbar.setTitle(R.string.portgo_title_history);
			toolbar.getMenu().findItem(R.id.menu_history_selectall).setVisible(true);
        }

        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        if(mAdapter!=null)
            mAdapter.notifyDataSetChanged();
        TabLayout tabLayout = (TabLayout) getView().findViewById(R.id.tabLayout);
        LinearLayout tabStrip = (LinearLayout) tabLayout.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            View tabView = tabStrip.getChildAt(i);
            if (tabView != null) {
                tabView.setClickable(false);
            }
        }
    }

    private void exitSelectStatus(){
        FrameLayout bottomBar = (FrameLayout) baseActivity.findViewById(R.id.activity_main_bottombar);
        View bottomView = bottomBar.findViewById(R.id.bottom_toolbar);
        if(bottomView!=null){
            bottomBar.removeView(bottomView);
        }
        bottomBar.findViewById(R.id.activity_main_tabs).setVisibility(View.VISIBLE);

        if(toolbar!=null) {
			toolbar.setTitle(R.string.portgo_title_history);
            toolbar.setNavigationIcon(null);
			toolbar.setNavigationOnClickListener(null);
			toolbar.getMenu().findItem(R.id.menu_history_selectall).setVisible(false);
        }

        TabLayout tabLayout = (TabLayout) getView().findViewById(R.id.tabLayout);
        LinearLayout tabStrip = (LinearLayout) tabLayout.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            View tabView = tabStrip.getChildAt(i);
            if (tabView != null) {
                tabView.setClickable(true);
            }
        }

        mListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        if(mAdapter!=null) {
			mAdapter.setSelectNone();
			mAdapter.notifyDataSetChanged();
		}
    }

    private void updateSelectStatus(){
        if(mListView.getChoiceMode()!=AbsListView.CHOICE_MODE_MULTIPLE)
            return;
		int selectSize = mAdapter.getSelectItems().size();
		if(selectSize==0){
			toolbar.setTitle(R.string.please_select);
		}else{
			toolbar.setTitle(String.format(getString(R.string.select_sum), selectSize));
		}
		if(!mAdapter.isAllSelect()){
			toolbar.getMenu().findItem(R.id.menu_history_selectall).setTitle(R.string.string_all);
		}else{
			toolbar.getMenu().findItem(R.id.menu_history_selectall).setTitle(R.string.string_clear);
		}

		FrameLayout bottomBar = (FrameLayout) baseActivity.findViewById(R.id.activity_main_bottombar);
		View bottomView = bottomBar.findViewById(R.id.bottom_toolbar);
		bottomView.findViewById(R.id.bottombar_right).setEnabled(selectSize>0);
	}
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bottombar_right:
				ArrayList<Integer> selectItems = mAdapter.getSelectItems();
				ArrayList<HistoryAVCallEventForList> deleteEvents = new ArrayList<>();
				for (HistoryAVCallEventForList eventForList:events){
					if(selectItems.contains(eventForList.getEvent().getId())){
						deleteEvents.add(eventForList);
					}
				}
				ArrayList<ContentProviderOperation>ops = new ArrayList<>();
				for (HistoryAVCallEventForList event :deleteEvents) {

					String selection= DBHelperBase.HistoryColumns.HISTORY_GROUP+ "=?" ;
					String[] selectionArgs = new String[]{""+event.getEvent().getGroup()};
                    ContentProviderOperation op = ContentProviderOperation.newDelete(DBHelperBase.HistoryColumns.CONTENT_URI).
                            withSelection(selection,selectionArgs).build();
					ops.add(op);
				}
				if(ops.size()>0) {
					try {
						baseActivity.getContentResolver().applyBatch(UriMactherHepler.AUTHORITY, ops);
					}catch (RemoteException e) {
						e.printStackTrace();
					} catch (OperationApplicationException e) {
						e.printStackTrace();
					}
				}
                exitSelectStatus();
				break;
			default:
				break;
        }
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
    public void update(Observable observable, Object o) {
        getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (HistoryAVCallEventForList eventForList : events) {
					Contact contact = baseActivity.mContactMgr.getObservableContacts().get(eventForList.getEvent().getContactId());
					eventForList.setAttachContact(contact);
				}
			}
		});
    }

}