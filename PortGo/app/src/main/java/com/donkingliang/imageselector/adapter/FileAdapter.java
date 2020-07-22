package com.donkingliang.imageselector.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.donkingliang.imageselector.entry.Image;
import com.donkingliang.imageselector.entry.PortFile;
import com.portgo.R;
import com.portgo.adapter.ChatRecyclerCursoAdapter;
import com.portgo.util.NgnStringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private Context mContext;
    private List<PortFile> mFiles;
    private LayoutInflater mInflater;

    //保存选中的图片
    private ArrayList<PortFile> mSelectFiles = new ArrayList<>();
    private OnFileSelectListener mSelectListener;
    private OnItemClickListener mItemClickListener;
    private int mMaxCount;
    private boolean isSingle;

    final  int MAX_SENDFILE_SIZE= 50*10241024;
    private final SimpleDateFormat sDurationTimerFormat;
    /**
     * @param maxCount    图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
     * @param isSingle    是否单选
     */
    public FileAdapter(Context context, int maxCount, boolean isSingle) {
        mContext = context;
        this.mInflater = LayoutInflater.from(mContext);
        mMaxCount = maxCount;
        this.isSingle = isSingle;
        sDurationTimerFormat = new SimpleDateFormat(mContext.getString(R.string.timeformat_md));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = mInflater.inflate(R.layout.adapter_files_item, parent, false);
        return new ViewHolder(view);

    }

    void setFileDrawable(@NonNull ViewHolder holder, @NonNull PortFile file){
        int result = R.drawable.file_unknow;
        String key = null;
        String extName = file.getextName();
        if(file.isDirectory()){
            result = R.drawable.file_folder;
        }else{
            if(TextUtils.isEmpty(extName)){
                result = R.drawable.file_unknow;
            }else if(extName.equalsIgnoreCase("MP3")||extName.equalsIgnoreCase("arm")||extName.equalsIgnoreCase("wav")){
                result = R.drawable.file_music;
            }else if(extName.equalsIgnoreCase("MP4")){
                result = R.drawable.file_movi;
            }else if(extName.equalsIgnoreCase("jpg")||extName.equalsIgnoreCase("jpeg")||extName.equalsIgnoreCase("png")){
                result = R.drawable.file_image;
//                Glide.with(mContext).load(new File(file.getPath()))
//                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)).apply(new RequestOptions().centerCrop())
//                        .into(holder.ivIcon);
//                return;
            }else if(extName.equalsIgnoreCase("ppt")){
                result = R.drawable.file_ppt;
            }else if(extName.equalsIgnoreCase("pdf")){
                result = R.drawable.file_pdf;
            }else if(extName.equalsIgnoreCase("txt")||extName.equalsIgnoreCase("html")
                    ||extName.equalsIgnoreCase("log")||extName.equalsIgnoreCase("ini")){
                result = R.drawable.file_txt;
            }
        }

        Glide.with(mContext).load(result).into(holder.ivIcon);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

            final PortFile file = mFiles.get(position);
            holder.tvName.setText(file.getName());
            setFileDrawable(holder,file);

            if(file.isDirectory()){
                List<PortFile> files = file.getChild();
                int childrenCount = 0;
                if(files!=null){
                    childrenCount = files.size();
                }
                holder.tvDesc.setText(mContext.getText(R.string.string_file)+": "+childrenCount);

                holder.ivSelectIcon.setVisibility(View.INVISIBLE);
                holder.vSplit.setVisibility(View.INVISIBLE);

            }else{
                long filesize = file.getFileSize();
                long fileTime = file.getTime();
                Date date = new Date(fileTime);
                String size = NgnStringUtils.getReadAbleSize(filesize);
                holder.tvDesc.setText(size+ sDurationTimerFormat.format(date));

                holder.ivSelectIcon.setVisibility(View.VISIBLE);
                holder.vSplit.setVisibility(View.VISIBLE);
            }

            setItemSelect(holder, mSelectFiles.contains(file));
            //点击选中/取消选中图片
            holder.ivSelectIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkedFile(holder, file);
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(file.isDirectory()) {
                        if (mItemClickListener != null) {
                            int p = holder.getAdapterPosition();
                            mItemClickListener.OnItemClick(file, p);
                        }
                    }
                     else {
                        checkedFile(holder, file);
                    }
                }
            });
    }

    private void checkedFile(ViewHolder holder, PortFile file) {
        if(file.getFileSize()<1){
            Toast.makeText(mContext,R.string.string_empty,Toast.LENGTH_SHORT).show();
            setItemSelect(holder, false);
            return;
        }else if (file.getFileSize()>MAX_SENDFILE_SIZE){
            Toast.makeText(mContext,R.string.string_huge,Toast.LENGTH_SHORT).show();
            setItemSelect(holder, false);
            return;
        }
        if (mSelectFiles.contains(file)) {
            //如果图片已经选中，就取消选中
            unSelectImage(file);
            setItemSelect(holder, false);
        } else if (isSingle) {
            //如果是单选，就先清空已经选中的图片，再选中当前图片
            clearImageSelect();
            selectImage(file);
            setItemSelect(holder, true);
        } else if (mMaxCount <= 0 || mSelectFiles.size() < mMaxCount) {
            //如果不限制图片的选中数量，或者图片的选中数量
            // 还没有达到最大限制，就直接选中当前图片。
            selectImage(file);
            setItemSelect(holder, true);
        }
    }

    /**
     * 选中图片
     *
     * @param file
     */
    private void selectImage(PortFile file) {
        mSelectFiles.add(file);
        if (mSelectListener != null) {
            mSelectListener.OnFileSelect(file, true, mSelectFiles.size());
        }
    }

    /**
     * 取消选中图片
     *
     * @param file
     */
    private void unSelectImage(PortFile file) {
        mSelectFiles.remove(file);
        if (mSelectListener != null) {
            mSelectListener.OnFileSelect(file, false, mSelectFiles.size());
        }
    }


    @Override
    public int getItemCount() {
        return getImageCount();
    }

    private int getImageCount() {
        return mFiles == null ? 0 : mFiles.size();
    }

    public List<PortFile> getData() {
        return mFiles;
    }

    public void refresh(List<PortFile> data) {
        mFiles = data;
        notifyDataSetChanged();
    }

    /**
     * 设置图片选中和未选中的效果
     */
    private void setItemSelect(ViewHolder holder, boolean isSelect) {
        if (isSelect) {
            holder.ivSelectIcon.setImageResource(R.drawable.icon_image_select);
//            holder.ivMasking.setAlpha(0.5f);
        } else {
            holder.ivSelectIcon.setImageResource(R.drawable.icon_image_un_select);
//            holder.ivMasking.setAlpha(0.2f);
        }
    }

    private void clearImageSelect() {
        if (mFiles != null && mSelectFiles.size() == 1) {
            int index = mFiles.indexOf(mSelectFiles.get(0));
            mSelectFiles.clear();
            if (index != -1) {
                notifyItemChanged(index);
            }
        }
    }

    public void setSelectedImages(ArrayList<String> selected) {
        if (mFiles != null && selected != null) {
            for (String path : selected) {
                if (isFull()) {
                    return;
                }
                for (PortFile file : mFiles) {
                    if (path.equals(file.getPath())) {
                        if (!mSelectFiles.contains(file)) {
                            mSelectFiles.add(file);
                        }
                        break;
                    }
                }
            }
            notifyDataSetChanged();
        }
    }


    private boolean isFull() {
        if (isSingle && mSelectFiles.size() == 1) {
            return true;
        } else if (mMaxCount > 0 && mSelectFiles.size() == mMaxCount) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<PortFile> getSelectImages() {
        return mSelectFiles;
    }

    public void setOnImageSelectListener(OnFileSelectListener listener) {
        this.mSelectListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivIcon;
        ImageView ivSelectIcon;
        ImageView ivMasking;
        TextView tvName;
        TextView tvDesc;
        View vSplit;

        public ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_fileicon);
            ivSelectIcon = itemView.findViewById(R.id.iv_select);
            ivMasking = itemView.findViewById(R.id.iv_masking);
            tvName = itemView.findViewById(R.id.tv_filename);
            tvDesc = itemView.findViewById(R.id.tv_filedesc);
            vSplit = itemView.findViewById(R.id.v_split);
        }
    }

    public interface OnFileSelectListener {
        void OnFileSelect(PortFile image, boolean isSelect, int selectCount);
    }

    public interface OnItemClickListener {
        void OnItemClick(PortFile file, int position);
    }
}
