package com.portgo.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.portgo.R;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ActivityMainContactFavoriteFragment extends PortBaseFragment implements TabLayout.OnTabSelectedListener
       {
    Toolbar mToolbar;
    Map<Integer,PortBaseFragment> contactFragments = new HashMap<Integer,PortBaseFragment>();
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
        contactFragments.put(0,new ActivityMainContactsFragment());
        contactFragments.put(1,new ActivityMainFriendsFragment());
        contactFragments.put(2,new ActivityMainFavoriteFragment());
        return inflater.inflate(R.layout.activity_main_contactfavorite_fragment, null);
	}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {}

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        indexSelect = tab.getPosition();
        showSelectFragment(indexSelect);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab){
    }

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mToolbar = (Toolbar) view.findViewById(R.id.toolBar);
        initView(view);

        mToolbar.inflateMenu(R.menu.menu_contact);
        showToolBar(view,getString(R.string.portgo_title_contact));
        initSearchView(mToolbar.getMenu().findItem(R.id.menu_contact_search));
    }

    TabLayout tabLayout;
    private  void initView(View view) {
        tabLayout = (TabLayout) view.findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.contact_tab_all));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.contact_tab_friend));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.contact_tab_favorite));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setOnTabSelectedListener(this);
        onTabSelected(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()));

    }

    @Override
    public boolean onKeyBackPressed(FragmentManager manager, Map<Integer,Fragment> fragments, Bundle result) {
        indexSelect = tabLayout.getSelectedTabPosition();
        PortBaseFragment fragment = contactFragments.get(indexSelect);
        return fragment.onKeyBackPressed(manager, fragments, result);
    }
    
    void showSelectFragment(int select){

        Set<Integer> keys = contactFragments.keySet();
        for(Integer index:keys){
            PortBaseFragment  fragment= contactFragments.get(select);
            if(select == index){
                MenuItem searchItem = mToolbar.getMenu().findItem(R.id.menu_contact_search);

                getChildFragmentManager().beginTransaction().replace(R.id.contacts_content,fragment).show(fragment).commit();
                if(searchItem!=null) {
                    mToolbar.collapseActionView();
                    SearchView searchView = (SearchView) searchItem.getActionView();
                    searchView.setOnQueryTextListener(fragment);
                    searchView.setOnCloseListener(fragment);
                }
            }else {
            }
        }
    }

    private void initSearchView(MenuItem menuSearch){
        if(menuSearch!=null) {
            SearchManager searchManager = (SearchManager) baseActivity.getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menuSearch.getActionView();

            setCursorIcon(searchView);

            SearchableInfo info = searchManager.getSearchableInfo(baseActivity.getComponentName());
            searchView.setSearchableInfo(info);
            if(tabLayout!=null) {
                int select = tabLayout.getSelectedTabPosition();
                PortBaseFragment fragment = contactFragments.get(select);
                searchView.setOnQueryTextListener(fragment);
                searchView.setOnCloseListener(fragment);
            }
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
}
