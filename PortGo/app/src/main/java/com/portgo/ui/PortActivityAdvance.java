package com.portgo.ui;
//

import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.manager.ConfigurationManager;
import com.portgo.manager.UserAccount;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portgo.util.SpecialCharTrimFilter;

import java.net.URL;

import static com.portsip.PortSipEnumDefine.ENUM_TRANSPORT_PERS_TCP;
import static com.portsip.PortSipEnumDefine.ENUM_TRANSPORT_PERS_UDP;
import static com.portsip.PortSipEnumDefine.ENUM_TRANSPORT_TCP;
import static com.portsip.PortSipEnumDefine.ENUM_TRANSPORT_TLS;
import static com.portsip.PortSipEnumDefine.ENUM_TRANSPORT_UDP;

//
public class PortActivityAdvance extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, View.OnFocusChangeListener {
    UserAccount user;
    EditText tvDispalyname,tvProxy,tvAuthName,tvStunPort,tvStunServer,tvSubRefresh,tvPubRefresh,tvVoiceMail;
    RadioGroup rgTranstype;
    ToggleButton tbEnableLog,tbEnableStun,tbEnableAgent,tbEnableTls;

    int MIN_TIME,MAX_TIME;
    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        MIN_TIME = getResources().getInteger(R.integer.presence_sub_time_min);
        MAX_TIME = getResources().getInteger(R.integer.presence_sub_time_max);
        user = (UserAccount)getIntent().getSerializableExtra("User");
        setContentView(R.layout.activity_advance);
        initView();

        setAccountInfo();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                this.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onAdvanceAboutClick(View view) {
        startActivity(new Intent(this,PortActivityAbout.class));
    }

    private void initView(){
        showToolBar(getString(R.string.activity_login_advance));
        tvDispalyname = (EditText) findViewById(R.id.activity_advance_displayname);
        tvProxy = (EditText)findViewById(R.id.activity_advance_proxy_server);
        tvAuthName= (EditText)findViewById(R.id.activity_advance_proxy_authname);
        rgTranstype = (RadioGroup)findViewById(R.id.activity_advance_transport);
        findViewById(R.id.activity_advance_ll_category_transport).setVisibility(View.GONE);
        findViewById(R.id.activity_advance_ll_category_outbound).setVisibility(View.GONE);

        tvStunPort = (EditText)findViewById(R.id.activity_advance_stun_port);
        tvStunServer =(EditText)findViewById(R.id.activity_advance_stun_server);
        tvPubRefresh = (EditText)findViewById(R.id.activity_advance_pub_refresh);
        tvSubRefresh = (EditText)findViewById(R.id.activity_advance_sub_refresh);

        tvVoiceMail = (EditText)findViewById(R.id.activity_advance_voicemail);
        tvVoiceMail.setFilters(new InputFilter[]{new SpecialCharTrimFilter("\r\n")});

        ToggleButton tg = (ToggleButton)findViewById(R.id.activity_advance_mobile_switch);
        tg.setOnCheckedChangeListener(this);
        tg.setChecked(ConfigurationManager.getInstance().getBooleanValue(this,ConfigurationManager.PRESENCE_VOIP,
                getResources().getBoolean(R.bool.prefrence_voipcall_default)));

        InputFilterMinMax limit = new InputFilterMinMax(MIN_TIME,MAX_TIME);
        tvPubRefresh.addTextChangedListener(limit);
        tvPubRefresh.setOnFocusChangeListener(this);
        tvSubRefresh.addTextChangedListener(limit);
        tvSubRefresh.setOnFocusChangeListener(this);

        tbEnableStun = (ToggleButton) findViewById(R.id.activity_advance_enablestun);
        tbEnableStun.setOnCheckedChangeListener(this);
        tbEnableLog = (ToggleButton) findViewById(R.id.activity_advance_enablelog);

        tbEnableAgent = (ToggleButton) findViewById(R.id.activity_advance_enableagent);
        tbEnableAgent.setOnCheckedChangeListener(this);
        tbEnableTls= (ToggleButton) findViewById(R.id.activity_advance_enabletlscert);
        tbEnableTls.setOnCheckedChangeListener(this);

    }

    void getAccountInfo(){
        user.setDisplayName(tvDispalyname.getText().toString());
        user.setRealm(tvProxy.getText().toString());
        user.setVoiceMail(tvVoiceMail.getText().toString());
        user.setAuthor(tvAuthName.getText().toString());
        user.setStunPort(NgnStringUtils.parseInt(tvStunPort.getText().toString(),5060));
        user.setStunServer(tvStunServer.getText().toString());
        int transType = ENUM_TRANSPORT_UDP;
        switch (rgTranstype.getCheckedRadioButtonId()){
            case R.id.activity_advance_transport_udp:
                transType = ENUM_TRANSPORT_UDP;
                break;
            case R.id.activity_advance_transport_tls:
                transType = ENUM_TRANSPORT_TLS;
                break;
            case R.id.activity_advance_transport_tcp:
                transType = ENUM_TRANSPORT_TCP;
                break;
            case R.id.activity_advance_transport_pers_udp:
                transType = ENUM_TRANSPORT_PERS_UDP;
                break;
            case R.id.activity_advance_transport_pers_tcp:
                transType = ENUM_TRANSPORT_PERS_TCP;
                break;
        }
        user.setTransType(transType);
        user.setEnableStun(tbEnableStun.isChecked());

    }

    void setAccountInfo(){
        tvDispalyname.setText(user.getDisplayName()==null?"":user.getDisplayName());
        int iPort = user.getPort();
        if (iPort == 0) {
            iPort = 5060;
        }
        String realm = user.getRealm();
        if(!TextUtils.isEmpty(realm)) {
            tvProxy.setText(iPort != 5060 ? realm + ":" + iPort : realm);
        }
        tvAuthName.setText(user.getAuthor());
        tvStunPort.setText(""+user.getStunPort());
        tvStunServer.setText(user.getStunServer());
        tvVoiceMail.setText(user.getVoiceMail());
        int id = R.id.activity_advance_transport_udp;
        switch (user.getTransType()){
            case ENUM_TRANSPORT_UDP:
                id = R.id.activity_advance_transport_udp;
                break;
            case ENUM_TRANSPORT_TLS:
                id = R.id.activity_advance_transport_tls;
                break;
            case ENUM_TRANSPORT_TCP:
                id = R.id.activity_advance_transport_tcp;
                break;
            case ENUM_TRANSPORT_PERS_UDP:
                id = R.id.activity_advance_transport_pers_udp;
                break;
            case ENUM_TRANSPORT_PERS_TCP:
                id = R.id.activity_advance_transport_pers_tcp;
                break;
        }
        rgTranstype.check(id);

        tbEnableLog.setChecked(ConfigurationManager.getInstance().getBooleanValue(this,ConfigurationManager.PRESENCE_DEBUG
                ,getResources().getBoolean(R.bool.debug_default)));

        tbEnableAgent.setChecked(!ConfigurationManager.getInstance().getBooleanValue(this,ConfigurationManager.PRESENCE_AGENT
                ,getResources().getBoolean(R.bool.presence_agent)));
        tbEnableAgent.setChecked(ConfigurationManager.getInstance().getBooleanValue(this,ConfigurationManager.PRESENCE_AGENT
                ,getResources().getBoolean(R.bool.presence_agent)));

        tbEnableStun.setChecked(!user.isStunEnable());
        tbEnableStun.setChecked(user.isStunEnable());

        tbEnableTls.setChecked(ConfigurationManager.getInstance().getBooleanValue(this,ConfigurationManager.PRESENCE_TLS_CERT
                ,getResources().getBoolean(R.bool.tls_cert)));



        tvPubRefresh.setText(""+ConfigurationManager.getInstance().getIntergerValue(this,ConfigurationManager.PRESENCE_PUB_REFRESH
                ,getResources().getInteger(R.integer.presence_pub_time)));
        tvSubRefresh.setText(""+ConfigurationManager.getInstance().getIntergerValue(this,ConfigurationManager.PRESENCE_SUB_REFRESH
                ,getResources().getInteger(R.integer.presence_sub_time)));
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        getAccountInfo();
        intent.putExtra("User",user);
        setResult(RESULT_OK,intent);
        ConfigurationManager.getInstance().setBooleanValue(this,ConfigurationManager.PRESENCE_AGENT,tbEnableAgent.isChecked());

        int time = Integer.parseInt(tvSubRefresh.getText().toString());
        ConfigurationManager.getInstance().setIntegerValue(this,ConfigurationManager.PRESENCE_SUB_REFRESH,time<90?90:time);
        time = Integer.parseInt(tvPubRefresh.getText().toString());
        ConfigurationManager.getInstance().setIntegerValue(this,ConfigurationManager.PRESENCE_PUB_REFRESH,time<90?90:time);

        ConfigurationManager.getInstance().setBooleanValue(this,ConfigurationManager.PRESENCE_DEBUG,tbEnableLog.isChecked());

        ConfigurationManager.getInstance().setBooleanValue(this,ConfigurationManager.PRESENCE_TLS_CERT,tbEnableTls.isChecked());

        super.onBackPressed();
    }

    private void showToolBar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        if(toolbar!=null) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.portgo_color_toobar_gray));
            toolbar.setTitleTextAppearance(this,R.style.ToolBarTextAppearance);
            if(!NgnStringUtils.isNullOrEmpty(title)) {
                toolbar.setTitle("");
            }
            toolbar.setNavigationIcon(R.drawable.nav_back_ico);
            toolbar.setTitle(title);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            return ;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()){
            case R.id.activity_advance_enablestun:
                tvStunPort.setEnabled(b);
                tvStunPort.setClickable(b);
                tvStunPort.setFocusable(b);
                tvStunPort.setFocusableInTouchMode(b);
                tvStunServer.setEnabled(b);
                tvStunServer.setClickable(b);
                tvStunServer.setFocusable(b);
                tvStunServer.setFocusableInTouchMode(b);
                if(!b){
                    tvStunServer.clearFocus();
                    tvStunPort.clearFocus();

                }
                break;
            case R.id.activity_advance_enableagent:
                tvPubRefresh.setEnabled(b);
                tvPubRefresh.setClickable(b);
                tvPubRefresh.setFocusable(b);
                tvPubRefresh.setFocusableInTouchMode(b);
                tvSubRefresh.setEnabled(b);
                tvSubRefresh.setClickable(b);
                tvSubRefresh.setFocusable(b);
                tvSubRefresh.setFocusableInTouchMode(b);
                if(!b){
                    tvPubRefresh.clearFocus();
                    tvSubRefresh.clearFocus();
                }
                break;
            case R.id.activity_advance_mobile_switch:
                ConfigurationManager.getInstance().setBooleanValue(this,ConfigurationManager.PRESENCE_VOIP,b);
                if(!b) { }
                break;
            default:
                break;
        }
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        TextView text;
        String input;
        int num;
        if(!b) {
            switch (view.getId()) {
                case R.id.activity_advance_sub_refresh:
                    text = (TextView)view;
                    input = text.getText().toString();
                    num = NgnStringUtils.parseInt(input,0);
                    if(num<MIN_TIME){
                        text.setText(""+MIN_TIME);
                    }
                    break;
                case R.id.activity_advance_pub_refresh:
                    text = (TextView)view;
                    input = text.getText().toString();
                    num = NgnStringUtils.parseInt(input,0);
                    if(num<MIN_TIME){
                        text.setText(""+MIN_TIME);
                    }
                    break;
            }
        }

    }


    public class InputFilterMinMax implements TextWatcher {
        String nums = null;
        final int mMin,mMax;
        InputFilterMinMax(int min,int max){
            mMin = min;
            mMax=max;
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s != null && !s.equals("")) {
                if (mMin != -1 && mMax != -1) {
                    int a = 0;
                    try {
                        a = Integer.parseInt(s.toString());
                    } catch (NumberFormatException e) {
                        // TODO Auto-generated catch block
                        a = 0;
                    }

                    if (a > mMax) {
                        s.replace(0,s.length(),""+mMax) ;
                    }

                    return;
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
        int after) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
        int count) {
//            if (start > 1) {
//                if (mMin != -1 && mMax != -1) {
//                    int num = Integer.parseInt(s.toString());
//                    if (num > mMax) {
//                        s = ""+mMax;
//                    }
//                    if(num<mMin){
//                        s = ""+mMin;
//                    }
//                    return;
//                }
//            }
        }
    }


}