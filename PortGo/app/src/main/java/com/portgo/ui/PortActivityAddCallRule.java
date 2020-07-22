package com.portgo.ui;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.portgo.R;
import com.portgo.database.DBHelperBase;
import com.portgo.manager.CursorHelper;
import com.portgo.ui.PortGoBaseActivity;
import com.portgo.util.CallRule;
import com.portgo.view.CursorEndEditTextView;

public class PortActivityAddCallRule extends PortGoBaseActivity{
    public static final String ACCOUNT_ID = "account_id";
    public static final String RULE_ID = "rule_id";
    int mAccountID = -1,ruleId = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_setting_fragment_account_add_callrule);

        mAccountID = getIntent().getIntExtra(ACCOUNT_ID,-1);
        ruleId = getIntent().getIntExtra(RULE_ID,-1);
        if(mAccountID<0){
            this.finish();
        }

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
		if(toolbar!=null) {
			toolbar.setBackgroundColor(getResources().getColor(R.color.portgo_color_toobar_gray));
			toolbar.setTitle(R.string.add_callrule_tilte);
			toolbar.setTitleTextAppearance(this,R.style.ToolBarTextAppearance);
			toolbar.setNavigationIcon(R.drawable.nav_back_ico);
			toolbar.setTitleMarginStart(0);
			setSupportActionBar(toolbar);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
        initView();
	}

	void initView(){
        if(ruleId>=0){
            String selection = DBHelperBase.CallRuleColumns._ID+"=?";
            Cursor ruleCursor = CursorHelper.resolverQuery(getContentResolver(),DBHelperBase.CallRuleColumns.CONTENT_URI,null,selection,new String[]{""+ruleId},null);
            if(CursorHelper.moveCursorToFirst(ruleCursor)){
                CallRule rule = CallRule.callRuleFromCursor(ruleCursor);
                CursorEndEditTextView editText = (CursorEndEditTextView) findViewById(R.id.activity_main_fragment_callrule_name_editor);
                if(!TextUtils.isEmpty(rule.getName())){
                    editText.setTextCursorEnd(rule.getName());
                }

                editText = (CursorEndEditTextView) findViewById(R.id.activity_main_fragment_callrule_match_editor);
                if(!TextUtils.isEmpty(rule.getMatcher())){
                    editText.setTextCursorEnd(rule.getMatcher());
                }

                editText = (CursorEndEditTextView) findViewById(R.id.activity_main_fragment_callrule_add_editor);
                if(!TextUtils.isEmpty(rule.getAddPrefix())){
                    editText.setTextCursorEnd(rule.getAddPrefix());
                }

                editText = (CursorEndEditTextView) findViewById(R.id.activity_main_fragment_callrule_remove_editor);
                if(!TextUtils.isEmpty(rule.getRemoveFrefix())){
                    editText.setTextCursorEnd(rule.getRemoveFrefix());
                }
            }
            CursorHelper.closeCursor(ruleCursor);
        }

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_add_callrule,menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case android.R.id.home:
				setResult(Activity.RESULT_CANCELED);
				this.finish();
				break;
			case R.id.menu_save:
                int rule = saveCallRule();
                if(rule>0) {
                    setResult(Activity.RESULT_OK);
                    this.finish();
                }
                break;
		}
		return super.onOptionsItemSelected(item);
	}

	private int saveCallRule(){
        int result = -1;
        String name,matchPrefix,addPrefix,removePrefix;
		EditText editText = (EditText) findViewById(R.id.activity_main_fragment_callrule_name_editor);
        name = editText.getText().toString();
		editText = (EditText) findViewById(R.id.activity_main_fragment_callrule_match_editor);
        matchPrefix = editText.getText().toString();
		editText = (EditText) findViewById(R.id.activity_main_fragment_callrule_add_editor);
        addPrefix = editText.getText().toString();
		editText = (EditText) findViewById(R.id.activity_main_fragment_callrule_remove_editor);
        removePrefix = editText.getText().toString();

        if(TextUtils.isEmpty(name)){
            Toast.makeText(this,R.string.null_name,Toast.LENGTH_SHORT).show();
            return result;
        }

        if(TextUtils.isEmpty(addPrefix)&&TextUtils.isEmpty(removePrefix)){
            Toast.makeText(this,R.string.null_add_removeprefix,Toast.LENGTH_SHORT).show();
            return result;
        }

        ContentValues values = new ContentValues();
        values.put(DBHelperBase.CallRuleColumns.CALL_RULE_ACCOUNT_ID,mAccountID);
        values.put(DBHelperBase.CallRuleColumns.CALL_RULE_NAME,name);
        if(TextUtils.isEmpty(matchPrefix)) {
            matchPrefix="";
        }
        values.put(DBHelperBase.CallRuleColumns.CALL_RULE_MATCHER, matchPrefix);
        if(TextUtils.isEmpty(addPrefix)) {
            addPrefix="";
        }
        values.put(DBHelperBase.CallRuleColumns.CALL_RULE_ADDPREFIX, addPrefix);

        if(TextUtils.isEmpty(removePrefix)) {
            removePrefix="";
        }
        values.put(DBHelperBase.CallRuleColumns.CALL_RULE_REMOVEPREFIX, removePrefix);
        values.put(DBHelperBase.CallRuleColumns.CALL_RULE_ENABLE, 1);
        if(ruleId>=0) {
            Uri contentUri = ContentUris.withAppendedId(DBHelperBase.CallRuleColumns.CONTENT_URI,ruleId);
            return getContentResolver().update(contentUri, values,null,null);
        }else{
            Uri uri = getContentResolver().insert(DBHelperBase.CallRuleColumns.CONTENT_URI, values);
            return (int) ContentUris.parseId(uri);

        }
	}
}
