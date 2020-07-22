package com.donkingliang.imageselector;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.donkingliang.imageselector.adapter.FileAdapter;
import com.donkingliang.imageselector.adapter.TopDirectoryAdapter;
import com.donkingliang.imageselector.entry.PortFile;
import com.donkingliang.imageselector.utils.ImageSelector;
import com.portgo.R;
import com.portgo.manager.ConfigurationManager;

import java.util.ArrayList;
import java.util.List;

public class FileSelectorActivity extends AppCompatActivity {
    private TextView tvFolderName,tvEmpty,tvTitle;
    private TextView tvConfirm,tvSelect;
    private FrameLayout btnConfirm;
    private RecyclerView rvFiles;
    private RecyclerView rvFolder;
    private View masking;

    private FileAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;

    private ArrayList<PortFile> mTopDirs = new ArrayList<>(2);
    private boolean applyLoadImage = false;
    private static final int PERMISSION_WRITE_EXTERNAL_REQUEST_CODE = 0x00000011;
    private static final int PERMISSION_CAMERA_REQUEST_CODE = 0x00000012;

    private boolean isOpenFolder;
    private boolean isInitFolder;
    private boolean isSingle;

    private int mMaxCount;


    //用于接收从外面传进来的已选择的图片列表。当用户原来已经有选择过图片，现在重新打开选择器，允许用
    // 户把先前选过的图片传进来，并把这些图片默认为选中状态。
    private ArrayList<String> mSelectedImages;

    /**
     * 启动图片选择器
     *
     * @param activity
     * @param requestCode
     * @param isSingle       是否单选
     * @param isViewImage    是否点击放大图片查看
     * @param useCamera      是否使用拍照功能
     * @param maxSelectCount 图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
     * @param selected       接收从外面传进来的已选择的图片列表。当用户原来已经有选择过图片，现在重新打开
     *                       选择器，允许用户把先前选过的图片传进来，并把这些图片默认为选中状态。
     */
    public static void openActivity(Activity activity, int requestCode,
                                    boolean isSingle, boolean isViewImage, boolean useCamera,
                                    int maxSelectCount, ArrayList<String> selected) {
        Intent intent = new Intent(activity, FileSelectorActivity.class);
        intent.putExtras(dataPackages(isSingle, isViewImage, useCamera, maxSelectCount, selected));
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 启动图片选择器
     *
     * @param fragment
     * @param requestCode
     * @param isSingle       是否单选
     * @param isViewImage    是否点击放大图片查看
     * @param useCamera      是否使用拍照功能
     * @param maxSelectCount 图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
     * @param selected       接收从外面传进来的已选择的图片列表。当用户原来已经有选择过图片，现在重新打开
     *                       选择器，允许用户把先前选过的图片传进来，并把这些图片默认为选中状态。
     */
    public static void openActivity(Fragment fragment, int requestCode,
                                    boolean isSingle, boolean isViewImage, boolean useCamera,
                                    int maxSelectCount, ArrayList<String> selected) {
        Intent intent = new Intent(fragment.getContext(), FileSelectorActivity.class);
        intent.putExtras(dataPackages(isSingle, isViewImage, useCamera, maxSelectCount, selected));
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 启动图片选择器
     *
     * @param fragment
     * @param requestCode
     * @param isSingle       是否单选
     * @param isViewImage    是否点击放大图片查看
     * @param useCamera      是否使用拍照功能
     * @param maxSelectCount 图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
     * @param selected       接收从外面传进来的已选择的图片列表。当用户原来已经有选择过图片，现在重新打开
     *                       选择器，允许用户把先前选过的图片传进来，并把这些图片默认为选中状态。
     */
    public static void openActivity(android.app.Fragment fragment, int requestCode,
                                    boolean isSingle, boolean isViewImage, boolean useCamera,
                                    int maxSelectCount, ArrayList<String> selected) {
        Intent intent = new Intent(fragment.getActivity(), FileSelectorActivity.class);
        intent.putExtras(dataPackages(isSingle, isViewImage, useCamera, maxSelectCount, selected));
        fragment.startActivityForResult(intent, requestCode);
    }

    public static Bundle dataPackages(boolean isSingle, boolean isViewImage, boolean useCamera,
                                      int maxSelectCount, ArrayList<String> selected) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ImageSelector.IS_SINGLE, isSingle);
        bundle.putBoolean(ImageSelector.IS_VIEW_IMAGE, isViewImage);
        bundle.putBoolean(ImageSelector.USE_CAMERA, useCamera);
        bundle.putInt(ImageSelector.MAX_SELECT_COUNT, maxSelectCount);
        bundle.putStringArrayList(ImageSelector.SELECTED, selected);
        return bundle;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_select);

        Intent intent = getIntent();
        mMaxCount = intent.getIntExtra(ImageSelector.MAX_SELECT_COUNT, 0);
        isSingle = intent.getBooleanExtra(ImageSelector.IS_SINGLE, false);
        mSelectedImages = intent.getStringArrayListExtra(ImageSelector.SELECTED);

        setStatusBarColor();
        initView();
        initListener();
        initFolderList();
        initImageList();
        checkPermissionAndLoadImages();
        hideFolderList();
        setSelectImageCount(0);

    }

    private void loadTopDirs(){
        PortFile file = PortFile.loadPortFile(null,Environment.getExternalStorageDirectory().getAbsolutePath(),getString(R.string.string_sdcard));
        if(file!=null) {
            mTopDirs.add(file);
        }

        String filePath = ConfigurationManager.getInstance().getStringValue(this,ConfigurationManager.PRESENCE_FILE_PATH,
                getExternalFilesDir(null).getAbsolutePath()+ConfigurationManager.PRESENCE_FILE_DEFALUT_SUBPATH);
        file = PortFile.loadPortFile(null,filePath,getString(R.string.string_portfile));
        if(file!=null) {
            mTopDirs.add(file);
        }
    }
    /**
     * 修改状态栏颜色
     */
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#373c3d"));
        }
    }

    private void initView() {
        rvFiles = (RecyclerView) findViewById(R.id.rv_files);
        rvFolder = (RecyclerView) findViewById(R.id.rv_folder);
        tvConfirm = (TextView) findViewById(R.id.tv_confirm);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);
        btnConfirm = (FrameLayout) findViewById(R.id.btn_confirm);
        tvSelect = findViewById(R.id.tv_select);
        findViewById(R.id.btn_preview).setVisibility(View.INVISIBLE);
        tvFolderName = (TextView) findViewById(R.id.tv_folder_name);
        masking = findViewById(R.id.masking);
        tvTitle =findViewById(R.id.title);
    }


    private void initListener() {
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm();
            }
        });

        findViewById(R.id.btn_folder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInitFolder) {
                    if (isOpenFolder) {
                        closeFolder();
                    } else {
                        openFolder();
                    }
                }
            }
        });

        masking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFolder();
            }
        });
    }

    /**
     * 初始化图片列表
     */
    private void initImageList() {
        // 判断屏幕方向
        mLayoutManager = new LinearLayoutManager(this);
        rvFiles.setLayoutManager(mLayoutManager);
//        rvFiles.addItemDecoration();

        mAdapter = new FileAdapter(this, mMaxCount, isSingle);
        rvFiles.setAdapter(mAdapter);
        ((SimpleItemAnimator) rvFiles.getItemAnimator()).setSupportsChangeAnimations(false);
        if (mTopDirs != null && !mTopDirs.isEmpty()) {
            PortFile folder = mTopDirs.get(0);
            tvTitle.setText(folder.getDisName());
            tvFolderName.setText(folder.getDisName());
            setFolder(folder);
        }

        mAdapter.setOnImageSelectListener(new FileAdapter.OnFileSelectListener() {
                                              @Override
                                              public void OnFileSelect(PortFile file, boolean isSelect, int selectCount) {
                                                  tvSelect.setText(getString(R.string.string_select)+selectCount);
                                                  setSelectImageCount(selectCount);
                                              }});
        mAdapter.setOnItemClickListener(new FileAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(PortFile file, int position) {
                setFolder(file);
            }
        });
    }

    PortFile currPortFile = null;
    /**
     * 初始化图片文件夹列表
     */
    private void initFolderList() {
        loadTopDirs();
        if (mTopDirs != null && !mTopDirs.isEmpty()) {
            isInitFolder = true;
            rvFolder.setLayoutManager(new LinearLayoutManager(FileSelectorActivity.this));
            TopDirectoryAdapter adapter = new TopDirectoryAdapter(FileSelectorActivity.this, mTopDirs);
            adapter.setOnFolderSelectListener(new TopDirectoryAdapter.OnFolderSelectListener() {
                @Override
                public void OnFolderSelect(PortFile folder) {
                    tvFolderName.setText(folder.getDisName());
                    tvTitle.setText(folder.getDisName());
                    setFolder(folder);
                    closeFolder();
                }
            });
            rvFolder.setAdapter(adapter);
        }
    }

    /**
     * 刚开始的时候文件夹列表默认是隐藏的
     */
    private void hideFolderList() {
        rvFolder.post(new Runnable() {
            @Override
            public void run() {
                rvFolder.setTranslationY(rvFolder.getHeight());
                rvFolder.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 设置选中的文件夹，同时刷新图片列表
     *
     * @param folder
     */
    private void setFolder(PortFile folder) {
        if (folder != null && mAdapter != null) {
            currPortFile = folder;

            rvFiles.scrollToPosition(0);
            List<PortFile> files = folder.getChild();
            if(files==null||((List) files).size()==0){
                tvEmpty.setVisibility(View.VISIBLE);
            }else{
                tvEmpty.setVisibility(View.GONE);
            }
            mAdapter.refresh(files);
        }
    }

    private void setSelectImageCount(int count) {
        String sure = getString(R.string.string_sure);
        if (count == 0) {
            btnConfirm.setEnabled(false);
            tvConfirm.setText(sure);

        } else {
            btnConfirm.setEnabled(true);
            if (isSingle) {
                tvConfirm.setText(sure);
            } else if (mMaxCount > 0) {
                tvConfirm.setText(sure+"(" + count + "/" + mMaxCount + ")");
            } else {
                tvConfirm.setText(sure+"(" + count + ")");
            }
        }
    }

    /**
     * 弹出文件夹列表
     */
    private void openFolder() {
        if (!isOpenFolder) {
            masking.setVisibility(View.VISIBLE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(rvFolder, "translationY",
                    rvFolder.getHeight(), 0).setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    rvFolder.setVisibility(View.VISIBLE);
                }
            });
            animator.start();
            isOpenFolder = true;
        }
    }

    /**
     * 收起文件夹列表
     */
    private void closeFolder() {
        if (isOpenFolder) {
            masking.setVisibility(View.GONE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(rvFolder, "translationY",
                    0, rvFolder.getHeight()).setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    rvFolder.setVisibility(View.GONE);
                }
            });
            animator.start();
            isOpenFolder = false;
        }
    }

    private int getFirstVisibleItem() {
        return mLayoutManager.findFirstVisibleItemPosition();
    }

    private void confirm() {
        if (mAdapter == null) {
            return;
        }
        //因为图片的实体类是Image，而我们返回的是String数组，所以要进行转换。
        ArrayList<PortFile> selectFiles = mAdapter.getSelectImages();
        ArrayList<String> files = new ArrayList<>();
        for (PortFile image : selectFiles) {
            files.add(image.getPath());
        }

        //点击确定，把选中的图片通过Intent传给上一个Activity。
        setResult(files,false);
        finish();
    }

    private void setResult(ArrayList<String> images,boolean isCameraImage) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(ImageSelector.SELECT_RESULT, images);
        intent.putExtra(ImageSelector.IS_CAMERA_IMAGE,isCameraImage);
        setResult(RESULT_OK, intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (applyLoadImage) {
            applyLoadImage = false;
            checkPermissionAndLoadImages();
        }
    }

    /**
     * 处理图片预览页返回的结果
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ImageSelector.RESULT_CODE) {
            if (data != null && data.getBooleanExtra(ImageSelector.IS_CONFIRM, false)) {
                //如果用户在预览页点击了确定，就直接把用户选中的图片返回给用户。
                confirm();
            } else {
                //否则，就刷新当前页面。
                mAdapter.notifyDataSetChanged();
                setSelectImageCount(mAdapter.getSelectImages().size());
            }
        }
    }

    /**
     * 检查权限并加载SD卡里的图片。
     */
    private void checkPermissionAndLoadImages() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            Toast.makeText(this, "没有图片", Toast.LENGTH_LONG).show();
            return;
        }
        int hasWriteExternalPermission = ContextCompat.checkSelfPermission(getApplication(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteExternalPermission == PackageManager.PERMISSION_GRANTED) {
            //有权限，加载图片。
        } else {
            //没有权限，申请权限。
            ActivityCompat.requestPermissions(FileSelectorActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_REQUEST_CODE);
        }
    }

    /**
     * 处理权限申请的回调。
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_WRITE_EXTERNAL_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //允许权限，加载图片。
            } else {
                //拒绝权限，弹出提示框。
                showExceptionDialog(true);
            }
        } else if (requestCode == PERMISSION_CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //允许权限，有调起相机拍照。
            } else {
                //拒绝权限，弹出提示框。
                showExceptionDialog(false);
            }
        }
    }

    /**
     * 发生没有权限等异常时，显示一个提示dialog.
     */
    private void showExceptionDialog(final boolean applyLoad) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("提示")
                .setMessage("该相册需要赋予访问存储和拍照的权限，请到“设置”>“应用”>“权限”中配置权限。")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                startAppSettings();
                if (applyLoad) {
                    applyLoadImage = true;
                }
            }
        }).show();
    }


    /**
     * 启动应用的设置
     */
    private void startAppSettings() {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN && isOpenFolder) {
            closeFolder();
            return true;
        }else{
            if(mTopDirs!=null&&!mTopDirs.contains(currPortFile)){
                setFolder(currPortFile.getParent());
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
