package com.portgo.view;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by huacai on 2017/4/26.
 */
public class ViewHolder {
    static  public class ContactViewHolder {
        public CheckBox contacts_item_radiobox;
        public TextView contacts_item_textView_displayname,contacts_item_txavatar;
        public RoundedImageView contacts_item_avatar;
    }

    static  public class RecordViewHolder {
        public TextView recordFileName,recordFileDSC;
        public TextView recordFileLen;
        public CheckBox recordSelector;
    }

    static  public class FriendViewHolder {
        public CheckBox friend_item_radiobox;
        public TextView friend_item_textView_displayname,friend_item_txavatar,friend_item_status,friend_item_presence;
        public RoundedImageView friend_item_avatar;
    }
    static public class GroupViewHolder{
        public TextView view_list_header_title;
    }

    static public class HistroyViewHolder{
        public View convertView;
        public TextView tvCallName ;
        public TextView tvCallNumber;
        public TextView tvStartTime;
        public RoundedImageView imgAvatar;
        public TextView tvAvatar;
        public View moreDetails;
        public CheckBox history_item_radiobox;
    }
    static public class HistroyDetailCallHolder{
        public TextView tvStartTime ;
        public ImageView ivType;
        public ImageView ivRecord;
        public TextView tvInOut;
        public TextView tvConnectTime;
    }

    static public class ContactNumberViewHolder{
        public TextView tvContactName ;
        public TextView tvPhoneNumber ;
        public TextView tvPhoneType ;
    }
    static public class PhoneNumberViewHolder{
        public CheckBox tvCheck ;
        public TextView tvPhoneNumber ;
    }

    static public class MessageViewHolder{
        public TextView tvRemote;
        public TextView tvDate;
        public TextView tvContent;
//        public TextView tvUnSeen;
        public TextView tvAvatar;
        public ImageView imgAvatar;
        public CheckBox message_item_radiobox;
        public int viewType;
    }

    static public class SubViewHolder{
        public TextView tvAvatar;
        public ImageView imgAvatar;
        public CheckBox checkBox;
        public TextView tvRemote;
        public TextView tvDate;
        public TextView tvContent;

        public TextView tvReject;
        public TextView tvAccept;
    }

    static public class LoginViewHolder{
        public TextView user_name;
        public ImageView user_del;
        public View user_item;
    }

    static public class DetaiPhoneHolder{
        public TextView phoneNumber,phoneType;
        public ImageView audio,video,message;
    }

    static public class CallRuleViewHolder{
        public ImageView ruleDel;
        public TextView ruleName;
        public ImageView ruleMover;
    }

}
