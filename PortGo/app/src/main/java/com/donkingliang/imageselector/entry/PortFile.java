package com.donkingliang.imageselector.entry;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *图片实体类
 */
public class PortFile implements Parcelable {

    private String path;
    private boolean directory;
    private long time;
    private long filesize;
    private String name;
    private String extName;
    private String disName;
    private PortFile parent;
    ArrayList<PortFile> childs = null;
    public PortFile(PortFile parent,String path, long time,long filesize, String name,String extName,boolean directory) {
        this(parent, path, time,filesize, name,null,extName,directory);
    }

    public PortFile(PortFile parent,String path, long time,long filesize, String name, String disName,String extName,boolean directory) {
        this.path = path;
        this.time = time;
        this.name = name;
        this.extName = extName;
        this.directory = directory;
        this.filesize = filesize;
        this.parent = parent;
        this.disName = disName;
        if(TextUtils.isEmpty(disName)){
            this.disName = name;
        }

    }

    public long getFileSize() {
        return  this.filesize;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisName() {
        return disName;
    }

    public void setDisName(String disName) {
        this.disName = disName;
    }

    public String getextName() {
        return extName;
    }

    public void setextName(String extName) {
        this.extName = extName;
    }

    public boolean isGif(){
        return "image/gif".equals(extName);
    }

    public PortFile getParent(){
        return parent;
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.path);
        dest.writeLong(this.time);
        dest.writeLong(this.filesize);
        dest.writeString(this.name);
        dest.writeString(this.extName);
        dest.writeInt(this.directory?1:0);
    }

    protected PortFile(Parcel in) {
        this.path = in.readString();
        this.time = in.readLong();
        this.filesize = in.readLong();
        this.name = in.readString();
        this.extName = in.readString();
        this.directory = in.readInt()>0?true:false;
    }

    public boolean isDirectory(){
        return this.directory;
    }

    public static PortFile loadPortFile(PortFile parent,String path,String disName){
        PortFile portFile = null;
        File file = new File(path);
        if(!file.isHidden()) {
            boolean isDir = file.isDirectory();
            String name = file.getName();
            String abPath = file.getAbsolutePath();
            long modifyTime = file.lastModified();
            long totalSize = file.length();
            int indexExt = name.lastIndexOf(".");
            String extersion = null;
            if(indexExt!=-1&&indexExt<name.length()){
                extersion = name.substring(indexExt);
            }
            portFile = new PortFile(parent,abPath, modifyTime, totalSize, name,disName, extersion, isDir);
        }
        return portFile;
    }
    public static PortFile loadPortFile(PortFile parent,String path){
        return  loadPortFile(parent,path,null);
    }

    public static PortFile loadPortFile(PortFile parent,@NonNull File file){
        PortFile portFile = null;
        if(!file.isHidden()) {
            boolean isDir = file.isDirectory();
            String name = file.getName();
            String abPath = file.getAbsolutePath();
            long modifyTime = file.lastModified();
            long totalSize = file.length();
            String extersion = name.substring(name.lastIndexOf(".") + 1);
            portFile = new PortFile(parent,abPath, modifyTime, totalSize, name, extersion, isDir);
        }
        return portFile;
    }

    public List<PortFile> getChild(){
        if(childs ==null){
            childs = new ArrayList<>();
            File file = new File(path);
            if(file.exists()&&file.isDirectory()) {
                File[] children = file.listFiles();
                if(children!=null){
                    for (File child:children){
                        PortFile portFile =loadPortFile(this,child);
                        if(portFile!=null){
                            childs.add(portFile);
                        }
                    }
                    Collections.sort(childs, new Comparator<PortFile>() {
                        @Override
                        public int compare(PortFile file1, PortFile file2) {
                            if(file1!=null&&file2!=null&&file1.getDisName()!=null){
                                return file1.getDisName().compareToIgnoreCase(file2.getDisName());
                            }
                            return 0;
                        }
                    });
                }
            }
        }
        return childs;
    }

    public static final Creator<PortFile> CREATOR = new Creator<PortFile>() {
        @Override
        public PortFile createFromParcel(Parcel source) {
            return new PortFile(source);
        }

        @Override
        public PortFile[] newArray(int size) {
            return new PortFile[size];
        }
    };
}
