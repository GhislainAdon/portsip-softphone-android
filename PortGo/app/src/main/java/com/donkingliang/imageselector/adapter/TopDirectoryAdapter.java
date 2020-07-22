package com.donkingliang.imageselector.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.donkingliang.imageselector.entry.Folder;
import com.donkingliang.imageselector.entry.Image;
import com.donkingliang.imageselector.entry.PortFile;
import com.portgo.R;

import java.io.File;
import java.util.ArrayList;

public class TopDirectoryAdapter extends RecyclerView.Adapter<TopDirectoryAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<PortFile> mDirs;
    private LayoutInflater mInflater;
    private OnFolderSelectListener mListener;

    public TopDirectoryAdapter(Context context, ArrayList<PortFile> dirs) {
        mContext = context;
        mDirs = dirs;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.adapter_topdirctory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final PortFile folder = mDirs.get(position);

        holder.tvFolderName.setText(folder.getDisName());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyDataSetChanged();
                if (mListener != null) {
                    mListener.OnFolderSelect(folder);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDirs == null ? 0 : mDirs.size();
    }

    public void setOnFolderSelectListener(OnFolderSelectListener listener) {
        this.mListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFolderName;

        public ViewHolder(View itemView) {
            super(itemView);
            tvFolderName = itemView.findViewById(R.id.tv_folder_name);
        }
    }

    public interface OnFolderSelectListener {
        void OnFolderSelect(PortFile folder);
    }

}
