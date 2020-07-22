package com.portgo.ui;
//

import android.content.Intent;
import android.os.Bundle;

import com.portgo.R;

//
public class PortActivityHistoryDetail extends PortGoBaseActivity{
    public static final String REMOTE = "detail_remote";
    public static final String COUNT = "detail_count";
    public static final String EVENT_ID= "detail_id";
    public static final String MISS= "detail_mis";

    int remote;
    int count,id;
    boolean getMissed = false;
    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        remote = getIntent().getIntExtra(REMOTE,0);
        count = getIntent().getIntExtra(COUNT,1);
        id= getIntent().getIntExtra(EVENT_ID,0);
        getMissed= getIntent().getBooleanExtra(MISS,false);
        setContentView(R.layout.activity_chat);
        initView();
    }

    ActivityMainHistoryDetailsFragment historyDetailsFragment;
    private  void initView(){
        historyDetailsFragment = new ActivityMainHistoryDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(REMOTE,remote);
        bundle.putInt(COUNT,count);
        bundle.putInt(EVENT_ID,id);
        bundle.putBoolean(MISS,getMissed);

        Bundle extraBundle = new Bundle();
        extraBundle.putBundle(PortBaseFragment.EXTRA_ARGS,bundle);
        historyDetailsFragment.setArguments(extraBundle);
        getFragmentManager().beginTransaction().add(R.id.conten_fragment,historyDetailsFragment).show(historyDetailsFragment).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(historyDetailsFragment!=null){
            historyDetailsFragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}