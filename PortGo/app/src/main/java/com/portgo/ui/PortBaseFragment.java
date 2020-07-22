package com.portgo.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.Instrumentation;
import android.app.LoaderManager;
import android.os.Bundle;
import androidx.annotation.Nullable;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.portgo.R;
import com.portgo.manager.UserAccount;
import com.portgo.util.NgnStringUtils;

import java.util.Map;
import java.util.Random;
import java.util.Set;

public class PortBaseFragment extends Fragment implements PortActivityMain.OnKeyBackListener, SearchView.OnQueryTextListener,SearchView.OnCloseListener {
    protected boolean mNeedRemoveFormList = true;
    protected Integer mFragmentId = new Random().nextInt();
    protected Integer mFragmentResId = -1;
    protected int mBackFragmentId = -1;
    static final String BACK_FRAGMENT_ID = "BACK_FRAGMENT_ID";
    static final String EXTRA_ARGS = "EXTRA_ARGS";
    static final String FRAGMENT_RES_ID = "FRAGMENT_RES_ID";
    static final String FRAGMENT_ID = "FRAGMENT_ID";
    static final String REMOVE_FRAGMENT_WHENBACK = "REMOVE_FRAGMENT_WHENBACK";
    PortGoBaseActivity baseActivity = null;
    int indexSelect  = -1;
    String mTitle = null;
    LoaderManager loadMgr;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        baseActivity = (PortGoBaseActivity) getActivity();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadMgr = getLoaderManager();
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case android.R.id.home:
                baseActivity.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    protected void showToolBar(View view,String title) {
        mTitle = title;
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolBar);

        if(toolbar!=null) {
            toolbar.setBackgroundResource(R.color.portgo_color_toobar_gray);
            toolbar.setTitleMarginStart(0);
            toolbar.setTitleTextAppearance(getActivity(),R.style.ToolBarTextAppearance);
            if(!NgnStringUtils.isNullOrEmpty(title)) {
                toolbar.setTitle(title);
            }
        }
    }

    protected void showToolBarAsActionBar(View view,String title,boolean homeup) {
        mTitle = title;
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolBar);

        if(toolbar!=null) {
            toolbar.setBackgroundResource(R.color.portgo_color_toobar_gray);
            setHasOptionsMenu(true);
            toolbar.setTitleTextAppearance(getActivity(),R.style.ToolBarTextAppearance);
            if(homeup) {
                toolbar.setNavigationIcon(R.drawable.nav_back_ico);
            }
            toolbar.setTitleMarginStart(0);
            if(!NgnStringUtils.isNullOrEmpty(title)) {
                toolbar.setTitle(title);
            }
            baseActivity.setSupportActionBar(toolbar);
            if(homeup){
                baseActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    void showMenuItem(Toolbar toolbar , int itemId, boolean show){
        int size  = toolbar.getMenu().size();
        for(int index=0;index<size;index++){
            MenuItem item= toolbar.getMenu().getItem(index);
            if(item!=null&&item.getItemId() == itemId){
                item.setVisible(show);
            }
        }
    }
    @Override
    public boolean onKeyBackPressed(FragmentManager manager,Map<Integer,Fragment> fragments,Bundle result) {

        return false;
	}

    protected  void goback(){
        new Thread () {
            public void run () {
                try {
                    Instrumentation inst= new Instrumentation();
                    inst.sendKeyDownUpSync(KeyEvent. KEYCODE_BACK);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    void showFramegment(Activity activity ,FragmentManager manager,Map<Integer, Fragment> fragments, int resID, PortBaseFragment framegment){
        if(framegment==null||fragments==null)
            return;
        fragments.put(framegment.getFragmentId(),framegment);

        //hide others
        Set<Integer> mainFragmentkey =  fragments.keySet();
        for (Integer key:mainFragmentkey){
            Fragment fg = fragments.get(key);
            if(fg!=null&&fg.isAdded()&&framegment!=fg){
                manager.beginTransaction().hide(fg).commit();
            }
        }

        //show self
        if(framegment.isAdded()){
            manager.beginTransaction().show(framegment).commit();
        }else{
            if(manager!=null)
                manager.beginTransaction().add(resID,framegment).commit();
        }

        if(activity instanceof PortActivityMain){
            ((PortActivityMain)activity).setOnKeyBackListener(framegment);
        }
    }

    public  int getFragmentId(){
        return  mFragmentId;
    }
	@Override
	public void setArguments(Bundle args) {
        try {
            super.setArguments(args);
        }catch (IllegalStateException e){
            Bundle old = getArguments();
            if(old!=null) {
                old.putAll(args);
            }
        }

		mBackFragmentId = args.getInt(BACK_FRAGMENT_ID,mBackFragmentId);
        mFragmentResId =  args.getInt(FRAGMENT_RES_ID,mFragmentResId);
        mFragmentId = args.getInt(FRAGMENT_ID,mFragmentId);
        mNeedRemoveFormList = args.getBoolean(REMOVE_FRAGMENT_WHENBACK,mNeedRemoveFormList);
	}

    public void setArguments(int backFragmentId,int fragmentResId ,int fragmentId,boolean removeRecordWhenBack){
        Bundle bundle = new Bundle();
        bundle.putInt(BACK_FRAGMENT_ID,backFragmentId);
        bundle.putInt(FRAGMENT_RES_ID,fragmentResId);
        bundle.putInt(FRAGMENT_ID,fragmentId);
        bundle.putBoolean(REMOVE_FRAGMENT_WHENBACK,removeRecordWhenBack);
        setArguments(bundle);
    }
    public void setArguments(int backFragmentId,int fragmentResId ,int fragmentId,boolean removeRecordWhenBack,Bundle extra){
        Bundle bundle = new Bundle();
        bundle.putInt(BACK_FRAGMENT_ID,backFragmentId);
        bundle.putInt(FRAGMENT_RES_ID,fragmentResId);
        bundle.putInt(FRAGMENT_ID,fragmentId);
        bundle.putBoolean(REMOVE_FRAGMENT_WHENBACK,removeRecordWhenBack);
        bundle.putBundle("EXTRA_ARGS",extra);
        setArguments(bundle);
    }

    boolean checkCallCondition(String number,int LoginState){
        if (NgnStringUtils.isNullOrEmpty(number)) {
            Toast.makeText(getActivity(),
                    R.string.input_number_tips,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        switch (LoginState) {
            case UserAccount.STATE_LOGIN:
                Toast.makeText(getActivity(),
                        R.string.inlongin_tips,
                        Toast.LENGTH_SHORT).show();
                return false;
            case UserAccount.STATE_NOTONLINE:
                Toast.makeText(getActivity(), R.string.please_login_tips,
                        Toast.LENGTH_SHORT).show();
                return false;
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }


    protected void setViewClickListener(int viewId,View.OnClickListener listener){
        getView().findViewById(viewId).setOnClickListener(listener);
    }

    @Override
    public boolean onClose() {
        return false;
    }
}
