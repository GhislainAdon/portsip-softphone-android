package com.portgo.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;

import com.portgo.R;
import com.portgo.adapter.CallRuleAdapter;
import com.portgo.database.DBHelperBase.CallRuleColumns;
import com.portgo.manager.CursorHelper;
import com.portgo.util.CallRule;
import com.portgo.view.DragListView;

import java.util.ArrayList;

public class PortActivityCallRules extends PortGoBaseActivity implements View.OnClickListener, DragListView.OnDragMode, AdapterView.OnItemClickListener {
    public final static String ACCOUNT_ID = "rule_account_id";
    ArrayList<CallRule> rules = new ArrayList<>();
    CallRuleAdapter adapter = null;
    private int mAccountId = -1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_setting_fragment_account_callrule);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
		if(toolbar!=null) {
			toolbar.setBackgroundColor(getResources().getColor(R.color.portgo_color_toobar_gray));
			toolbar.setTitle(R.string.callrules_tilte);
			toolbar.setTitleTextAppearance(this,R.style.ToolBarTextAppearance);
			toolbar.setNavigationIcon(R.drawable.nav_back_ico);
			toolbar.setTitleMarginStart(0);
			setSupportActionBar(toolbar);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

        mAccountId = getIntent().getIntExtra(ACCOUNT_ID,-1);
        adapter = new CallRuleAdapter(this,rules,this);
        DragListView lvRules  = (DragListView) findViewById(R.id.lv_fragment_callrules);
        lvRules.setOnItemClickListener(this);
        lvRules.setAdapter(adapter);
        lvRules.setDragModeJudgeMent(this);
        if(mAccountId>0){
            String selection = CallRuleColumns.CALL_RULE_ACCOUNT_ID +"=?";
            Cursor ruleCursor = CursorHelper.resolverQuery(getContentResolver(),CallRuleColumns.CONTENT_URI,null,selection,new String[]{""+mAccountId},CallRuleColumns.DEFAULT_ORDER);
            rules.clear();
            while (CursorHelper.moveCursorToNext(ruleCursor)){
                CallRule rule = CallRule.callRuleFromCursor(ruleCursor);
                rules.add(rule);
            }
            CursorHelper.closeCursor(ruleCursor);
        }
        adapter.notifyDataSetChanged();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode==Activity.RESULT_OK&&mAccountId>0){
            String selection = CallRuleColumns.CALL_RULE_ACCOUNT_ID +"=?";
            rules.clear();
            Cursor ruleCursor = CursorHelper.resolverQuery(getContentResolver(),CallRuleColumns.CONTENT_URI,null,selection,new String[]{""+mAccountId},CallRuleColumns.DEFAULT_ORDER);
            while (CursorHelper.moveCursorToNext(ruleCursor)){
                CallRule rule = CallRule.callRuleFromCursor(ruleCursor);
                rules.add(rule);
            }
            CursorHelper.closeCursor(ruleCursor);
            adapter.notifyDataSetChanged();
        }
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_callrule,menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case android.R.id.home:
				setResult(Activity.RESULT_CANCELED);
				this.finish();
				break;
			case R.id.menu_add:
                Intent intent = new Intent(this,PortActivityAddCallRule.class);
                intent.putExtra(PortActivityAddCallRule.ACCOUNT_ID,mAccountId);
				startActivityForResult(intent,00);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

    @Override
    synchronized public boolean startDragMode(View view, int positon,int posRawX, int posRawY) {//
        if(view==null||positon== AdapterView.INVALID_POSITION)
            return false;
        ImageView mover = (ImageView) view.findViewById(R.id.callrule_item_mover);
        return inRangeOfView(mover, posRawX, posRawY);
    }

    @Override
    public void stopDragMode() {
        ContentResolver resolver = getContentResolver();
        ContentValues contentValues = new ContentValues(1);
        int size = rules.size();
        for(int position = 0;position<size;position++){
            CallRule rule = rules.get(position);
            if(rule.getPriority()!=(size-position)){
                contentValues.clear();
                contentValues.put(CallRuleColumns.CALL_RULE_PRIORITY,size-position);
                resolver.update(ContentUris.withAppendedId(CallRuleColumns.CONTENT_URI,rule.getRuleId()),contentValues,null,null);
            }
        }
    }

    private boolean inRangeOfView(View view, int posRawX, int posRawY ){
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int width = view.getWidth();
        int height = view.getHeight();
        return new Rect(x,y,x+width,y+height).contains(posRawX,posRawY);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.callrule_item_del:
                Object object = view.getTag();
                if(object!=null&&object instanceof Integer){
                    int ruleId =(Integer) object;
                    for(CallRule rule:rules){
                        if(rule.getRuleId() == ruleId){
                            rules.remove(rule);
                            break;
                        }
                    }
                    Uri ruleUri = ContentUris.withAppendedId(CallRuleColumns.CONTENT_URI,ruleId);
                    getContentResolver().delete(ruleUri,null,null);
                    adapter.notifyDataSetChanged();
                }
                break;

        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(this,PortActivityAddCallRule.class);
        intent.putExtra(PortActivityAddCallRule.ACCOUNT_ID,mAccountId);
        intent.putExtra(PortActivityAddCallRule.RULE_ID,(int)l);

        startActivityForResult(intent,00);
    }
}
