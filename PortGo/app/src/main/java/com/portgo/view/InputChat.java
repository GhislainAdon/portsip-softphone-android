package com.portgo.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.donkingliang.imageselector.FileSelectorActivity;
import com.donkingliang.imageselector.utils.ImageSelector;
import com.donkingliang.imageselector.utils.ImageSelectorUtils;
import com.portgo.R;
import com.portgo.ui.PortActivityRecycleChat;
import com.portgo.ui.PortActivityVideoReorder;
import com.portgo.ui.RecordedActivity;
import com.portgo.util.CropTakePictrueUtils;
import com.portgo.util.UnitConversion;
import com.portgo.view.emotion.EmotionView;
import com.portgo.view.emotion.data.CustomEmoji;
import com.portgo.view.emotion.data.Emoji;
import com.portgo.view.emotion.data.Emoticon;
import com.portgo.view.emotion.data.EmotionData;
import com.portgo.view.emotion.data.EmotionDataManager;
import com.portgo.view.emotion.data.UniqueEmoji;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/11/11.
 */
public class InputChat extends ChatInputBaseFragment {
    private static final int REQUEST_CODE_TAKE_PHOTO = 1;
    private static final int REQUEST_CODE_PICK_PHOTO = 2;
    private static final int REQUEST_CODE_CHOOSE_LOCATION = 3;
    private static final int REQUEST_CODE_VIDEO_RECORD = 4;
    private static final int REQUEST_CODE_FILE_SELECT = 5;
    private static final int REQUEST_CODE_IMAGE_SELECT = 6;


    protected LinearLayout rootView;
    protected LinearLayout buttonView;
    private OnlyView onlyView4;
    private EditText editText;
    private OnlyView onlyView3;
    private Button sendBtn;
    private Button send2toolBtn;
    private Button send2toolBtnOn;
    private OnlyView onlyView1;
    private Button voiceBtn;
    private Button voice2chatBtn;
    private OnlyView onlyView2;
    private Button emojiBtn;
    private Button emoji2chatBtn;
    protected OnlyView boxView;
    protected ChatToolBox toolBox;
    protected VoicePress voicePress;
    protected View voiceView;

    private EmotionView emotionView;

    protected InputChatListener inputChatListener = null;
    private List<ChatToolBox.ChatToolItem> items = new ArrayList<ChatToolBox.ChatToolItem>();

    private boolean keyBoardShow = false;

    private int boxViewHeight;

    public void clearInput() {
        editText.setText("");
    }

    public interface InputChatListener {
        boolean onSendMessage(String msg);
        boolean canStartRecord();
        boolean canStartSession();
        void onSendVoiceMessage(String path, int duration, String description);

        void onPickPhotoMessage(Intent data);

        void onTakePhotoMessage(Intent data);

        void onPickLocMessage(Intent data);
        void onSendFileMessage(Intent data);

        void onRecordVideoMessage(Intent data);

        void onAudioCall();
        void onVideoCall();
    }

    //this will be called at first, so we must make initial data there to avoid initial data being reset. !!!
    public InputChat() {
        inputChatListener = null;
        items = new ArrayList<>();
        items.add(new ToolPhoto());
        items.add(new ToolCamera());
        items.add(new AudioCall());
        items.add(new VideoCall());
        items.add(new FileSender());
    }

    boolean onlyText = false;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        if(args!=null){
            onlyText = args.getBoolean("TEXT",false);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void setInputChatListener(InputChatListener listener) {
        this.inputChatListener = listener;
    }

    private int getBoxViewHeight() {
        if (0 == boxViewHeight) {
            SharedPreferences pref = getActivity().getSharedPreferences("sys_variable", Context.MODE_PRIVATE);
            boxViewHeight = pref.getInt("virtual_keyboard_height", 0);
            if (0 == boxViewHeight) {
                if (null != getActivity()) {
                    boxViewHeight = UnitConversion.dp2px(getActivity(), 230);
                } else {
                    boxViewHeight = 2 * 230;
                }
            }
        }
        return boxViewHeight;
    }

    //this will be called when fragment called to show the view, this will be called after creatView().
    @Override
    protected View onInitializeView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (LinearLayout) inflater.inflate(R.layout.inputchat_new, container, false);
        buttonView = (LinearLayout) rootView.findViewById(R.id.chat_button);
        toolBox = (ChatToolBox) rootView.findViewById(R.id.chat_tool_box);
        boxView = (OnlyView) rootView.findViewById(R.id.box_view);

        emotionView = (EmotionView) rootView.findViewById(R.id.emotion_view);

        EmotionDataManager manager = EmotionDataManager.getInstance();
        manager.loadEmotion(getActivity());
        List<EmotionData> emotionList = manager.getEmotionList();
        emotionView.setEmotionDataList(emotionList);
        emotionView.setEmotionClickListener(new EmotionView.EmotionClickListener() {
            @Override
            public void OnEmotionClick(Emoticon emotionData, View v, EmotionData.EmotionCategory category) {
                switch (category) {
                    case emoji:
//                        SpannableString spannableString = emotionData.getSpanelText(getResources(), (int) editText.getTextSize());
                        SpannableString spannableString = emotionData.getSpanelText(editText);
                        int cursor = editText.getSelectionStart();
                        editText.getText().insert(cursor,spannableString );
                        break;
                    case image:
                        Toast.makeText(getActivity(),"path:" + emotionData.getDesc(),
                                Toast.LENGTH_SHORT).show();
                    default:
                }
            }

            @Override
            public void OnUniqueEmotionClick(Emoticon uniqueItem, View v, EmotionData.EmotionCategory category) {
                switch (category) {
                    case emoji:
                        if(R.drawable.bx_emotion_delete==uniqueItem.getResourceId()){
                            int cursorStart = editText.getSelectionStart();
                            int cursorEnd = editText.getSelectionEnd();
                            ImageSpan[] spans = editText.getText().getSpans(cursorStart,cursorEnd,ImageSpan.class);
                            int removeStart =cursorStart,removeEnd=cursorEnd;
                            for(ImageSpan span :spans){
                                int spanStart = editText.getText().getSpanStart(span);
                                int spanEnd = editText.getText().getSpanEnd(span);
                                if(cursorEnd==spanEnd){
                                    removeStart = cursorStart<spanStart?cursorStart:spanStart;
                                    removeEnd = spanEnd>cursorEnd?spanEnd:cursorEnd;
                                }
                            }
                            if(removeStart>=0) {
                                if(removeEnd<=removeStart) {
                                    removeStart = removeEnd>0?removeEnd-1:removeEnd;
                                }
                                editText.getText().delete(removeStart, removeEnd);
                            }

                        }
                        break;
                    case image:

                        String temp = UnitConversion.getResourceUriString(getActivity(), R.drawable.ic_launcher);
                        List<Emoticon> customList = emotionView.getEmotionDataList().get(1).getEmotionList();
                        customList.add(new CustomEmoji(temp));
                        EmotionData data = new EmotionData(customList,
                                UnitConversion.getResourceUriString(getActivity(), R.drawable.ic_launcher),
                                EmotionData.EmotionCategory.image, new UniqueEmoji(temp), 2, 4);

                        emotionView.modifyEmotionDataList(data, 1);
                    default:
                }
            }
        });

        init();
        return rootView;
    }

    //we should replace the view with fragment after views created.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        voicePress = new VoicePress();
        voicePress.setListener(new VoicePress.voiceMessageListener() {
            @Override
            public void onSendVoiceMessage(String path, int duration, String description) {
                if (null != inputChatListener) {
                    inputChatListener.onSendVoiceMessage(path, duration, description);
                }
            }

            @Override
            public boolean canStart() {
                if (null != inputChatListener) {
                    return inputChatListener.canStartRecord()&&inputChatListener.canStartSession();
                }
                return false;
            }
        });
        ft.replace(R.id.voice_press_view, voicePress);
        ft.commit();
        voiceView = rootView.findViewById(R.id.voice_press_view);
    }

    public void init() {
        initInputLayout();
        initToolBox();
    }

    private void initToolBox() {
        toolBox.setData(items);
    }

    private void initInputLayout() {
        onlyView1 = (OnlyView) buttonView.findViewById(R.id.button_1);
        voiceBtn = (Button) buttonView.findViewById(R.id.voice_button);
        voice2chatBtn = (Button) buttonView.findViewById(R.id.voice2chat_button);
        onlyView2 = (OnlyView) buttonView.findViewById(R.id.button_2);
        emojiBtn = (Button) buttonView.findViewById(R.id.emoji_button);
        emoji2chatBtn = (Button) buttonView.findViewById(R.id.emoji2chat_button);
        onlyView3 = (OnlyView) buttonView.findViewById(R.id.button_3);
        sendBtn = (Button) buttonView.findViewById(R.id.send_button);
        send2toolBtn = (Button) buttonView.findViewById(R.id.send2tool_button);
        send2toolBtnOn = (Button) buttonView.findViewById(R.id.send2toolOn_button);
        onlyView4 = (OnlyView) buttonView.findViewById(R.id.voice_view);
        editText = (EditText) buttonView.findViewById(R.id.id_edit);
        editText.setCursorVisible(true);

        if(onlyText){
            onlyView1.setVisibility(View.GONE);
            onlyView3.setChildView(sendBtn);
        }
        initButtonListener();
    }

    private void initButtonListener() {
        voiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideAll();
                onlyView1.setChildView(voice2chatBtn);
                onlyView2.setChildView(emojiBtn);
                onlyView3.setChildView(send2toolBtn);
                onlyView4.setChildView(voiceView);
            }
        });
        voice2chatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSoftInput();
                onlyView1.setChildView(voiceBtn);
                onlyView2.setChildView(emojiBtn);
                onlyView4.setChildView(editText);
                editText.requestFocus();
            }
        });
        emojiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftInput(new Runnable() {
                    @Override
                    public void run() {
                        showBoxView();
                    }
                });
                onlyView1.setChildView(voiceBtn);
                onlyView2.setChildView(emoji2chatBtn);
                if (send2toolBtnOn.getVisibility() == View.VISIBLE) {
                    onlyView3.setChildView(send2toolBtn);
                }
                onlyView4.setChildView(editText);
                boxView.setChildView(emotionView);
            }
        });
        emoji2chatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSoftInput();
                onlyView1.setChildView(voiceBtn);
                onlyView2.setChildView(emojiBtn);
                onlyView4.setChildView(editText);
                editText.requestFocus();
            }
        });
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (null != inputChatListener) {
                    if(inputChatListener.onSendMessage(editText.getEditableText().toString())){
                        clearInput();
                    }
                }

            }
        });
        send2toolBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftInput(new Runnable() {
                    @Override
                    public void run() {
                        showBoxView();
                    }
                });
                onlyView1.setChildView(voiceBtn);
                onlyView2.setChildView(emojiBtn);
                onlyView3.setChildView(send2toolBtnOn);
                onlyView4.setChildView(editText);
                boxView.setChildView(toolBox);
            }
        });
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                EmotionDataManager.getInstance().getSpanelText(s.toString(),editText);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(onlyText){
                    onlyView3.setChildView(sendBtn);
                    if (TextUtils.isEmpty(s.toString())) {
                        onlyView3.setChildView(sendBtn);
                        sendBtn.setEnabled(false);
                    }else{
                        sendBtn.setEnabled(true);
                        onlyView3.setChildView(sendBtn);
                    }
                }else {
                    if (TextUtils.isEmpty(s.toString())) {
                        onlyView3.setChildView(send2toolBtn);
                    } else {
                        onlyView3.setChildView(sendBtn);
                    }
                }
                buttonView.postInvalidate();
            }
        });
    }

    private void showBoxView() {
        if (null == boxView) {
            return;
        }
        if (isBoxViewShow()) {
            return;
        }
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) boxView.getLayoutParams();
        params.height = getBoxViewHeight();
        boxView.setLayoutParams(params);
        boxView.setVisibility(View.VISIBLE);
    }

    public void addToolBoxData(ChatToolBox.ChatToolItem item) {
        this.items.add(item);
        if (null != toolBox) {
            toolBox.setData(items);
        }
    }

    public void addToolBoxData(List<ChatToolBox.ChatToolItem> items) {
        this.items.addAll(items);
        if (null != toolBox) {
            toolBox.setData(items);
        }
    }

    Runnable runOnKeyboardDismiss;

    protected boolean hideSoftInput(Runnable runnable) {
        if (keyBoardShow) {
            runOnKeyboardDismiss = runnable;
            super.hideSoftKeyboard();
            return true;
        }
        if (null != runnable) {
            runnable.run();
        }
        runOnKeyboardDismiss = null;
        return false;
    }

    public void onKeyboardShow(int height) {
        SharedPreferences pref = getActivity().getSharedPreferences("sys_variable", Context.MODE_PRIVATE);
        pref.edit().putInt("virtual_keyboard_height", height).apply();
        boxViewHeight = height;
        keyBoardShow = true;
        hideBoxView();
        onlyView1.setChildView(voiceBtn);
        onlyView2.setChildView(emojiBtn);
        if(onlyText){
            onlyView3.setChildView(sendBtn);
        }else {
            if (!editText.getText().toString().equals("")) {
                onlyView3.setChildView(sendBtn);
            } else {
                onlyView3.setChildView(send2toolBtn);
            }
        }
    }

    public void onKeyboardDismiss() {
        keyBoardShow = false;
        if (null != runOnKeyboardDismiss) {
            runOnKeyboardDismiss.run();
            runOnKeyboardDismiss = null;
        }
    }

    public void hideBoxViewMode() {
        if (null == rootView) {
            return;
        }
        this.hideAll();
        onlyView1.setChildView(voiceBtn);
        onlyView2.setChildView(emojiBtn);
        if(onlyText){
            onlyView3.setChildView(sendBtn);
        }else {
            onlyView3.setChildView(send2toolBtn);
        }
        onlyView4.setChildView(editText);
    }

    @Override
    public boolean handleBack() {
        return hideAll();
    }

    public boolean hideAll() {
        return hideBoxView() || hideSoftInput(null);
    }

    public boolean hideBoxView() {
        if (isBoxViewShow()) {
            boxView.setVisibility(View.GONE);
            return true;
        } else {
            return false;
        }
    }

    private void showSoftInput() {
        if (keyBoardShow) {
            return;
        }
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.toggleSoftInput(0, 0);
    }

    private boolean isBoxViewShow() {
        if (null == boxView) {
            return false;
        }
        return View.VISIBLE == boxView.getVisibility();
    }

    public boolean isBoxShow() {
        return isBoxViewShow() || keyBoardShow;
    }

    private class ToolPhoto implements ChatToolBox.ChatToolItem {
        @Override
        public int getIcon() {
            return R.drawable.app_panel_gallery_selector;//app_panel_pic_icon
        }

        @Override
        public String getName() {
            return getString(R.string.string_abulmn);
        }

        @Override
        public void onItemSelected() {
            if(inputChatListener!=null&&inputChatListener.canStartSession()) {
                startPhotoPicker();
            }
        }
    }

    private class ToolCamera implements ChatToolBox.ChatToolItem {
        @Override
        public int getIcon() {
            return R.drawable.app_panel_camera_selector;
        }

        @Override
        public String getName() {
            return getString(R.string.string_camera);
        }

        @Override
        public void onItemSelected() {
            if(inputChatListener!=null&&inputChatListener.canStartRecord()&&inputChatListener.canStartSession()) {
                startCameraDlg();
            }
        }
    }

    private class VideoCall implements ChatToolBox.ChatToolItem {
        @Override
        public int getIcon() {
            return R.drawable.app_panel_video_selector;
        }

        @Override
        public String getName() {
            return getString(R.string.string_Video);
        }

        @Override
        public void onItemSelected() {
            if(inputChatListener!=null&&inputChatListener.canStartSession()) {
                inputChatListener.onVideoCall();
            }
        }
    }
    private class FileSender implements ChatToolBox.ChatToolItem {
        @Override
        public int getIcon() {
            return R.drawable.app_panel_file_selector;
        }

        @Override
        public String getName() {
            return getString(R.string.string_file);
        }

        @Override
        public void onItemSelected() {
            if(inputChatListener!=null&&inputChatListener.canStartSession()) {
                startFilePicker();
            }
        }
    }
    private class ToolAd implements ChatToolBox.ChatToolItem {
        @Override
        public int getIcon() {
            return R.drawable.app_panel_ads_selector;
        }

        @Override
        public String getName() {
            return getString(R.string.string_Video);
        }

        @Override
        public void onItemSelected() {
        }
    }
    private class AudioCall implements ChatToolBox.ChatToolItem {
        @Override
        public int getIcon() {
            return R.drawable.app_panel_audio_selector;
        }

        @Override
        public String getName() {
            return getString(R.string.string_audio);
        }

        @Override
        public void onItemSelected() {
            if(null != inputChatListener&&inputChatListener.canStartSession()) {
                inputChatListener.onAudioCall();
            }
        }
    }


    protected void startCameraDlg() {
        Intent cameraItent =  new Intent(getActivity(), RecordedActivity.class);
        cameraItent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivityForResult(cameraItent, REQUEST_CODE_VIDEO_RECORD);

    }

    protected void startPhotoPicker() {

        ImageSelector.builder()
                .useCamera(false)
                .setSingle(true)
                .setViewImage(true)
                .start(this, REQUEST_CODE_IMAGE_SELECT);

    }

    protected void startFilePicker() {
        FileSelectorActivity.openActivity(this, REQUEST_CODE_FILE_SELECT, false, false,
                false, 0, null);

    }

    protected void startLocationChooser() {

    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK && null != data && null != inputChatListener) {
            switch (requestCode) {
                case REQUEST_CODE_FILE_SELECT:
                    inputChatListener.onSendFileMessage(data);
                    break;
                case REQUEST_CODE_IMAGE_SELECT:
                    inputChatListener.onPickPhotoMessage(data);
                    break;
                case REQUEST_CODE_TAKE_PHOTO:
                    inputChatListener.onTakePhotoMessage(data);
                    break;
//                case REQUEST_CODE_PICK_PHOTO:
//                    inputChatListener.onPickPhotoMessage(data);
//                    break;
                case CropTakePictrueUtils.CHOOSE_PHOTO_KITKAT://>==7.0
                case CropTakePictrueUtils.CHOOSE_PHOTO:
                    inputChatListener.onPickPhotoMessage(data);
                    break;
                case REQUEST_CODE_CHOOSE_LOCATION:
                    inputChatListener.onPickLocMessage(data);
                    break;
                case REQUEST_CODE_VIDEO_RECORD:
                    inputChatListener.onRecordVideoMessage(data);
                    break;
            }
        }
        this.hideBoxViewMode();
    }
}
