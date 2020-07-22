package com.portgo.adapter;

import android.content.Context;
import android.media.MediaPlayer;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.R;
import com.portgo.util.SelectableObject;
import com.portgo.view.ViewHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by huacai on 2017/5/24.
 */

public class RecordFilesAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final Context mContext;
    private List<SelectableObject<File>> mFiles;
    private Calendar cal = Calendar.getInstance();

    public RecordFilesAdapter(Context context, ArrayList<SelectableObject<File>> files) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mFiles = files;
    }


    @Override
    public int getCount() {
        if (mFiles != null) {
            return mFiles.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return mFiles.get(i);
    }

    @Override
    public long getItemId(int i) {
        return mFiles.get(i).hashCode();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder.RecordViewHolder holder = null;
        if (view == null) {
            view = mInflater.inflate(R.layout.view_recordfile_item, null);
            holder = new ViewHolder.RecordViewHolder();
            holder.recordFileName = (TextView) view.findViewById(R.id.recordfile_item_name);
            holder.recordFileDSC = (TextView) view.findViewById(R.id.recordfile_item_description);
            holder.recordFileLen = (TextView) view.findViewById(R.id.recordfile_item_len);
            holder.recordSelector= (CheckBox) view.findViewById(R.id.recordfile_item_radiobox);
            view.setTag(holder);
        } else {
            holder = (ViewHolder.RecordViewHolder) view.getTag();
        }

        SelectableObject<File> selectFile = mFiles.get(i);
        File file = selectFile.getObject();
        if (((ListView)viewGroup).getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE) {
            holder.recordSelector.setVisibility(View.VISIBLE);
            holder.recordSelector.setChecked(selectFile.isChecked());
        } else {
            holder.recordSelector.setVisibility(View.GONE);
        }

        holder.recordFileName.setText(file.getName());
        cal.setTimeInMillis(file.lastModified());
        holder.recordFileDSC.setText(cal.getTime().toLocaleString());
        holder.recordFileLen.setText(getFilePlayTime(file));
        return view;
    }

    String getFilePlayTime(File file) {
        Calendar calendar= Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
        String strTime = mContext.getString(R.string.unconnect);
        calendar.setTimeInMillis(0);
        MediaPlayer player = new MediaPlayer();
        try {
            if(file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                player.setDataSource(fis.getFD());
                player.prepare();
                calendar.setTimeInMillis(player.getDuration());
            }
        } catch (IOException e)
        {
//            Toast.makeText(mContext,R.string.can_not_openfile, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        player.release();
        strTime = ""+DateFormat.format("HH:mm:ss",calendar);

        return strTime;
    }


}