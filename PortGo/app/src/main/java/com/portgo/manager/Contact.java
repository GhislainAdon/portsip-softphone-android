package com.portgo.manager;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.widget.Checkable;

import com.portgo.R;
import com.portgo.util.NgnObservableObject;
import com.portgo.util.NgnPredicate;
import com.portgo.util.NgnStringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.portgo.manager.ContactManager.PORTSIP_IM_PROTOCAL;

/**
 *
 */
public class Contact extends NgnObservableObject implements Checkable{
	private final int mId;
    private int STARRED;

    //    private final HashMap<String,HashMap<Integer ,HashMap<String,Object>>> MimeData;//
    private final HashMap<String,List<ContactDataAdapter>> mContactData;//

    private final HashSet<Integer> mGroup;
    private String mDisplayName;

    private String mSortNameforNinePath;
    private String mPresence_status;
    private int mPresence_resLable = R.string.status_offline;
    private int mPresence_resicon = R.drawable.mid_content_status_offline_ico;
    private int mPresence_mode;
    private int mSystemContactid;

    private String SORT_KEY_PRIMARY;
    private String SORT_KEY_ALT="";
    public static final int INVALIDE_ID = -1;
    public static final int TYPE_CUSTOM = 0;
    private Bitmap mAvatar = null;
    private int mAvatarId = 0;
	/**
	 * Creates new address book
	 * @param id a unique id defining this contact
	 * @param displayName the contact's display name
	 */
	public Contact(int id, String displayName){
		super();
		mId = id;
//        BaseInfo = new HashMap<>();
//        MimeData = new HashMap<>();
        mContactData= new HashMap<>();
        mGroup = new HashSet<>();
//        mSortName = NgnStringUtils.getAllFirstLetter(displayName);
//        SORT_KEY_PRIMARY = mSortName;
        mDisplayName = displayName;
	}

	public Contact(int id){
        this(id,null);
    }

    public  static class ContactDataIm extends ContactDataAdapter {
        int protocal;//DATA.DATA5
        String customProtocal;//DATA.DATA6
        public ContactDataIm(int id, String data1, int data2, String data3){
            super(id,data1,data2,data3);
        }
        public String getImAccount() {
            return mData1;
        }

        public void setProtocaltype(int protocaltype) {
            this.protocal = protocaltype;
        }

        public void setProtocal(String protocalContent){
            this.customProtocal = protocalContent;
        }
        public void setImAccount(String imAccount) {
            mData1 =imAccount;
        }

        public String getProtocal(){
            return customProtocal;
        }

        @Override
        ContentValues getContentValue() {
            ContentValues values = null;
            if(dataAvailable()){
                values = super.getContentValue();
                values.put(ContactsContract.CommonDataKinds.Im.PROTOCOL,protocal);
                if(protocal ==ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM){//==-1
                    values.put(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL,customProtocal);
                }
            }
            return values;
        }

        @Override
        boolean dataAvailable() {
            return super.dataAvailable()|| !(protocal==ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM&&TextUtils.isEmpty(customProtocal));
        }
    }

    abstract public  static class ContactDataNumber extends ContactDataAdapter implements Checkable{
        private boolean checked;
        private int mContactId= INVALIDE_ID;

        public ContactDataNumber(int id, String data1, int data2, String label){
            super(id,data1,data2,label);
        }
        public void setContactId(int contactid){
            mContactId = contactid;
        }

        public int getContactId(){
            return mContactId;
        }
        public void setChecked(boolean var1){
            checked = var1;
        }

        public  boolean isChecked(){
            return checked;
        }

        public void toggle(){
            checked = !checked;
        }

        public String getNumber(){
            return mData1;
        }

        public String getLabel(){
            return mData3;
        }

        public int getType(){
            return mData2;
        }
    }

    public  static class ContactDataPhone extends ContactDataNumber {
        public ContactDataPhone(int id, String data1, int data2 , String label){
            super(id,data1, data2,label);
        }
    }

    public  static class ContactDataSipAddress extends ContactDataNumber {
        public String mNumberFor9Path;

        public ContactDataSipAddress(int id, String data1, int data2, String label){
            super(id,data1,data2,label);
            mNumberFor9Path = NgnStringUtils.letter2NumberIn9Path(data1);
        }
    }

    public  static class ContactDataNickName extends ContactDataAdapter {
        String lable;

        public ContactDataNickName(int id, String data1, int data2, String data3){
            super(id,data1,data2,data3);
        }
    }

    public  static class ContactDataPhoto extends ContactDataAdapter {
        public Bitmap bitmap;
        ContactDataPhoto(int id, String data1, int data2, String data3){
            super(id,data1,data2,data3);
        }
    }

    public  static class ContactDataEmail extends ContactDataAdapter {
        ContactDataEmail(int id, String data1, int data2, String data3){
            super(id,data1,data2,data3);
        }
    }

    public  static class ContactDataEvent extends ContactDataAdapter {
        ContactDataEvent(int id, String data1, int data2, String data3){
            super(id,data1,data2,data3);
        }
    }

    public  static class ContactDataStructuredPostal extends ContactDataAdapter {
        ContactDataStructuredPostal(int id, String data1, int data2, String data3){
            super(id,data1,data2,data3);
        }

    }


    public  static class ContactDataNote extends ContactDataAdapter {
        ContactDataNote(int id, String data1, int data2, String data3){
            super(id,data1,data2,data3);
        }
    }

    public  static class ContactDataWebsite extends ContactDataAdapter {
        ContactDataWebsite(int id, String data1, int data2, String data3){
            super(id,data1,data2,data3);
        }

    }
    public  static class ContactDataStructuredName extends ContactDataAdapter {
        public String mSortName;
        public String mSortNameforNinePath;
        public String sortKey;
        public int fullnamestyle;


        public String givenName;

        public String middleName;//
        public String prefix;//
        public String suffix;//
        public String phoneticName;//
        public String phoneticFamilyName;//
        public String phoneticMiddleName;//
        public String phoneticGivenName;

        public ContactDataStructuredName(int id, String data1, int data2, String data3){
            super(id,data1,data2,data3);
        }

        @Override
        public ContentValues getContentValue(){
            ContentValues values = null;
            if(dataAvailable()) {
                values = new ContentValues();

//                values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,data1);
                if(!TextUtils.isEmpty(givenName)) {
                    values.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName);//first name
                }else{
                    values.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, "");//first name
                }
                if(!TextUtils.isEmpty(mData3)) {
                    values.put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, mData3);//last name
                }
                else{
                    values.put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, "");//first name
                }
                if(!TextUtils.isEmpty(middleName))
                values.put(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,middleName);
                if(!TextUtils.isEmpty(prefix))
                values.put(ContactsContract.CommonDataKinds.StructuredName.PREFIX,prefix);
                if(!TextUtils.isEmpty(suffix))
                values.put(ContactsContract.CommonDataKinds.StructuredName.SUFFIX,suffix);
                if(!TextUtils.isEmpty(phoneticName))
                values.put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME,phoneticName);
                if(!TextUtils.isEmpty(phoneticFamilyName))
                values.put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME,phoneticFamilyName);
                if(!TextUtils.isEmpty(phoneticMiddleName))
                values.put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME,phoneticMiddleName);
                if(!TextUtils.isEmpty(phoneticGivenName))
                values.put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME,phoneticGivenName);
            }
            return values;
        }

        @Override
        public boolean dataAvailable() {
            return !( TextUtils.isEmpty(givenName)&&
                    TextUtils.isEmpty(mData3)&& TextUtils.isEmpty(middleName)&& TextUtils.isEmpty(prefix)&&
                    TextUtils.isEmpty(suffix)&&TextUtils.isEmpty(phoneticName)&&TextUtils.isEmpty(phoneticFamilyName)&&TextUtils.isEmpty(phoneticMiddleName)&&
                    TextUtils.isEmpty(phoneticGivenName));
        }

        public String getDisplayName(){
            return mData1;
        }

        public String getGivenName() {
            return givenName;
        }

        public String getFamilyName() {
            return mData3;
        }

        public void setGivenName(String givenName) {
            this.givenName = givenName;
        }

        public void setFamilyName(String familyName) {
            this.mData3 = familyName;
        }
    }

    public  static class ContactDataOrgnization extends ContactDataAdapter {
        private String title = "";
        private String department = "";
        private String location = "";

        public ContactDataOrgnization(int id, String company, int data2, String data3, String department, String title){
            super(id,company,data2,data3);
            this.department=department;
            this.title=title;
        }

        @Override
        public ContentValues getContentValue(){
            ContentValues values = new ContentValues();
            if(dataAvailable()){
                values = super.getContentValue();
                //
                if (!NgnStringUtils.isNullOrEmpty(department)) {
                    values.put(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, department);
                }else{
                    values.put(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, "");
                }
                //
                if (!NgnStringUtils.isNullOrEmpty(title)) {
                    values.put(ContactsContract.CommonDataKinds.Organization.TITLE, title);
                }else{
                    values.put(ContactsContract.CommonDataKinds.Organization.TITLE, "");
                }
            }

            return values;
        }

        @Override
        public boolean dataAvailable() {
            return !(TextUtils.isEmpty(location)&&TextUtils.isEmpty(mData1)&&TextUtils.isEmpty(title) && TextUtils.isEmpty(department));
        }

        public String getCompony(){
            return  mData1;
        }

        public String getTitle(){
            return  title;
        }

        public String getDepartment(){
            return department;
        }

        public  void setCompany(String company){
            mData1 = company;
        }
        public  void setTitle(String title){
            this.title = title;
        }
        public  void setDepartment(String department){
            this.department= department;
        }
        public  void setLocation(String location){
            this.location= location;
        }
    }

    public  void addStructName(ContactDataStructuredName name){
        final String mime = ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(name);
    }

    public  void addNickName(ContactDataNickName nickName){
        final String mime = ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(nickName);
    }

    public  void addPhone(ContactDataPhone phone){
        final String mime = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(phone);
    }

    public  void addSipAddress(ContactDataSipAddress sipAddress){
        final String mime = ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(sipAddress);
    }

    public  void addIm(ContactDataIm im){
        final String mime = ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(im);
    }

    public void addOgnization(ContactDataOrgnization org){
        final String mime = ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(org);
    }

    public void addEvent(ContactDataEvent event){
        final String mime = ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(event);
    }

    public void addStructuredPostal(ContactDataStructuredPostal structuredPostal){
        final String mime = ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(structuredPostal);
    }

    public void addNote(ContactDataNote note){
        final String mime = ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(note);
    }

    public void addWebsite(ContactDataWebsite website){
        final String mime = ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(website);
    }


    public void addPhoto(ContactDataPhoto photo){
        final String mime = ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(photo);
    }

    public  void addEmail(ContactDataEmail email){
        final String mime = ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE;
        List<ContactDataAdapter> list = mContactData.get(mime);
        if(list==null){
            list = new ArrayList<>();
            mContactData.put(mime,list);
        }
        list.add(email);
    }


    public void setPresence_mode(int mPresence_mode) {
        this.mPresence_mode = mPresence_mode;
    }

    public void setPresence_resicon(int mPresence_resicon) {
        this.mPresence_resicon = mPresence_resicon;
    }

    public void setPresence_resLable(int mPresence_resLable) {
        this.mPresence_resLable = mPresence_resLable;
    }

    public void setPresence_status(String mPresence_status) {
        this.mPresence_status = mPresence_status;
    }

    public int getPresence_mode() {
        return mPresence_mode;
    }

    public int getPresence_resicon() {
        return mPresence_resicon;
    }

    public int getPresence_resLable() {
        return mPresence_resLable;
    }

    public String getPresence_status() {
        return mPresence_status;
    }

    public void setSystemContactid(int contactid){
        mSystemContactid = contactid;
    }

	/**
	 * Gets the id of the contact
	 * @return a unique id defining this contact
	 */
	public int getId(){
		return mId;
	}


    public void addGroup(int groupId){
        mGroup.add(groupId);
    }

    /**
     * 判断当前联系人是否属于群组
     * @param groupId
     * @return
     */
	public boolean inGroup(int groupId){
        return mGroup.contains(groupId);
    }
	/**
	 * Gets the contact's display name
	 * @return the contact's display name
	 */
	public final  String getDisplayName(){
        if(mDisplayName==null)//如果为空，付空字符串值
        {
           List names = getContactStructuredNames();
            if(names.size()>0){
                ContactDataStructuredName name = (ContactDataStructuredName) names.get(0);
                if(name!=null){
                    mDisplayName = name.getDisplayName();
                }
            }
        }
		return mDisplayName;
	}

    public String getAvatarText(){
        String avatarText;
        String disName = getDisplayName();
        if(disName==null) {
            disName= "";
        }
        if (disName.length() > 1) {
            avatarText = disName.substring(0, 2);
            if (avatarText.getBytes().length > 2) {
                avatarText = avatarText.substring(0, 1);
            }
        } else {
            avatarText = disName;
        }

        return avatarText.toLowerCase();
    }
    public Bitmap getAvatar(){
        return mAvatar;
    }

//	/**
//	 * Gets the photo associated to this contact
//	 * @param context
//	 * @param preferHighres true 原图,false 缩略图
//	 * @return a bitmap representing the contact's photo
//	 */



	public ContactDataPhoto getPhoto(Context context, Uri contentUri, int photoId, boolean preferHighres ){
		Bitmap photo = null;
        ContactDataPhoto contactPhoto =getContactPhoto(getContactAvatarId());

        if (contactPhoto!=null&&contactPhoto.mId>0) {

            try {
                if(mAvatar==null|| preferHighres==true) {//联系人里面只保存头像缩略图。联系人大图，使用实时获取的办法
                    Uri contactPhotoUri = ContentUris.withAppendedId(contentUri, mId);
                    InputStream photoDataStream = Contacts.openContactPhotoInputStream(context.getContentResolver(), contactPhotoUri, preferHighres);
                    if (photoDataStream != null) {
                        photo = BitmapFactory.decodeStream(photoDataStream);
                        photoDataStream.close();
                        contactPhoto.bitmap = photo;
                    }
                    if(preferHighres==false){
                        mAvatar = photo;
                    }
                }else{
                    contactPhoto.bitmap = mAvatar;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
		return contactPhoto;
	}

	public void setStarrred(int starrred) {
		this.STARRED = starrred;
	}

	public int getSTARRED() {
		return STARRED;
	}

    public final HashSet<Integer> getGroupInfo() {
        return mGroup;
    }

    /**
     * @return
     */
    public String getSortKey() {
        return SORT_KEY_PRIMARY;
    }

    /**
     * @param sortKey
     */
    public void setSortKey(String sortKey) {
        if(sortKey==null) {
            this.SORT_KEY_PRIMARY="";
        }else{
            this.SORT_KEY_PRIMARY = sortKey;
        }
    }
    public void setSortKeyAlt(String sortKeyAlt) {
        if(!TextUtils.isEmpty(sortKeyAlt)) {
            String[] sortkeyArray= sortKeyAlt.split(" ");
            if(sortkeyArray.length==1){
                this.SORT_KEY_ALT = sortKeyAlt;
            }else{
                for(int i=0;i<sortkeyArray.length;i++) {
                    if(!TextUtils.isEmpty(sortkeyArray[i])&&sortkeyArray[i].length()>0) {
                        this.SORT_KEY_ALT += sortkeyArray[i].substring(0, 1);
                    }
                    i++;
                }
            }

            mSortNameforNinePath= NgnStringUtils.letter2NumberIn9Path(this.SORT_KEY_ALT);
        }

    }

    public void  setAvatar(Bitmap avatar){
        mAvatar = avatar;
    }
    public void  setAvatarId(int avatarId){
        mAvatarId = avatarId;
    }


    public String getSortKeyAlt() {
        if(SORT_KEY_ALT==null){
            SORT_KEY_ALT ="";
        }
        return this.SORT_KEY_ALT;
    }

    public String getSortNameforNinePath() {
        if(mSortNameforNinePath==null){
            mSortNameforNinePath="";
        }
        return mSortNameforNinePath;
    }

    public static class ContactFilterByAnyPhoneNumber implements NgnPredicate<Contact>{
		private final String mPhoneNumber;
		public ContactFilterByAnyPhoneNumber(String phoneNumber){
			mPhoneNumber = phoneNumber;
		}
		@Override
		public boolean apply(Contact contact) {
//			for(PhoneNumber phoneNumer : contact.getPhoneNumbers()){
//				if(NgnStringUtils.equals(phoneNumer.getNumber(), mPhoneNumber, false)||NgnStringUtils.equals(phoneNumer.getNumber(), "sip:"+mPhoneNumber, false)){
//					return true;
//				}
//			}
			return false;
		}
	}

	public  boolean isEmpty(){
        if(mContactData.size()==0) {
            return true;
        }else{
            Iterator it = mContactData.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry entry = (Map.Entry) it.next();
                Object ob = entry.getValue();
                if(ob!=null){
                    List<ContactDataAdapter> datas = (List<ContactDataAdapter>)ob;
                    for(ContactDataAdapter dataAdapter:datas){
                        if(dataAdapter.dataAvailable()){
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    protected HashMap<String,List<ContactDataAdapter>> getContactData(){
        return mContactData;
    }

    public int getContactAvatarId(){
        return mAvatarId;
    }

    public List<ContactDataAdapter> getContactNumbers(){
        List numbers = new ArrayList<ContactDataAdapter>();
        if(getContactSipNumbers()!=null) {
            numbers.addAll(getContactSipNumbers());
        }
        if(getContactPhones()!=null) {
            numbers.addAll(getContactPhones());
        }
        return numbers;
    }

    public List<ContactDataAdapter> getContactSipNumbers(){
        List adapters = mContactData.get(ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE);
        if(adapters==null){
            adapters = new ArrayList<>();
        }
        return adapters;
    }

    public List<ContactDataAdapter> getContactPhones(){
        List adapters = mContactData.get(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        if(adapters==null){
            adapters = new ArrayList<>();
        }
        return adapters;
    }
    public List<ContactDataAdapter> getOrgnizationInfos(){
         List adapters =mContactData.get(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
        if(adapters==null){
            adapters = new ArrayList<>();
        }
        return adapters;
    }

    ContactDataSipAddress getSipNumber(int id){
        ContactDataSipAddress sipNumber = null;
        List<ContactDataAdapter> list = mContactData.get(ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE);
        if(list!=null) {
            for (Object obj : list) {
                if (obj != null && obj instanceof ContactDataSipAddress) {
                    ContactDataSipAddress sipAddress = (ContactDataSipAddress) obj;
                    if(sipAddress.mId==id){
                        sipNumber = sipAddress;
                        break;
                    }
                }
            }
        }
        return  sipNumber;
    }

    ContactDataPhone getPhoneNumber(int id){
        ContactDataPhone phoneNumber = null;
        List list= mContactData.get(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

        if(list!=null) {
            for (Object obj : list) {
                if (obj != null && obj instanceof ContactDataPhone) {
                    ContactDataPhone phone = (ContactDataPhone) obj;
                    if(phone.mId==id){
                        phoneNumber = phone;
                        break;
                    }
                }
            }
        }
        return  phoneNumber;
    }

    public ContactDataOrgnization getOrgnizationInfo(int id){
        ContactDataOrgnization org = null;
        List list = mContactData.get(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
        if(list!=null) {
            for (Object obj : list) {
                if (obj != null && obj instanceof ContactDataOrgnization) {
                    ContactDataOrgnization orgnization = (ContactDataOrgnization) obj;
                    if(orgnization.mId==id){
                        org = orgnization;
                        break;
                    }
                }
            }
        }
        return org;
    }


    public ContactDataIm getContactIm(int id){
        ContactDataIm im = null;
        List list= mContactData.get(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);
        if(list!=null) {
            for (Object obj : list) {
                if (obj != null && obj instanceof ContactDataIm) {
                    ContactDataIm contactIm = (ContactDataIm) obj;
                    if(contactIm.mId==id){
                        im = contactIm;
                        break;
                    }
                }
            }
        }
        return im;
    }

    public ContactDataIm getContactPortImAddress(){
        ContactDataIm im = null;
        List list= mContactData.get(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);
        if(list!=null) {
            for (Object obj:list) {
                if (obj != null && obj instanceof ContactDataIm) {
                    ContactDataIm temp = (ContactDataIm) obj;
                    if((temp.protocal == ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM)&& PORTSIP_IM_PROTOCAL.equals(temp.customProtocal)){
                        im = temp;
                        break;
                    }
                }
            }
        }
        return im;
    }

    public List<ContactDataAdapter> getContactStructuredNames(){
        List adapters =mContactData.get(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        if(adapters==null){
            adapters = new ArrayList<>();
        }
        return adapters;
    }

    public ContactDataStructuredName getNameInfo(int id){
        ContactDataStructuredName name = null;
        List<ContactDataAdapter> list = mContactData.get(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        if(list!=null) {
            for (Object obj : list) {
                if (obj != null && obj instanceof ContactDataStructuredName) {
                    name = (ContactDataStructuredName) obj;
                }
            }
        }

        return name;
    }

    public ContactDataNickName getNickNameInfo(int id){
        ContactDataNickName name = null;
        List list = mContactData.get(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
        if(list!=null) {
            for (Object obj : list) {
                if (obj != null && obj instanceof ContactDataNickName) {
                    name = (ContactDataNickName) obj;
                }
            }
        }
        return name;
    }

    public ContactDataPhoto getContactPhoto(int id){
        ContactDataPhoto photo = null;
        List<ContactDataAdapter> list = mContactData.get(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
        if(list!=null) {
            for (ContactDataAdapter item : list) {
                if (item != null && item instanceof ContactDataPhoto) {
                    photo = (ContactDataPhoto) item;
                }
            }
        }

        return photo;
    }

    private boolean checked =false;
    public void setChecked(boolean var1){
        checked = var1;
    }

    public  boolean isChecked(){
        return checked;
    }

    public void toggle(){
        checked = !checked;
    }

    public static Contact contactFromCursorForHistoryShow(Cursor cursor,Context context){
        Contact contact = null;

        int id = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        String disName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        int photoId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
        contact = new Contact(id,disName);
        if(photoId>0) {
            contact.getPhoto(context, ContactsContract.Data.CONTENT_URI, photoId, false);
        }
        return contact;
    }

    static public  Object getObjectById(List<ContactDataAdapter> items, int id){
        ContactDataAdapter item = null;
        if(items!=null){

            for(ContactDataAdapter adapter:items){
                if(adapter!=null&&adapter.mId==id) {
                    item = adapter;
                    break;
                }
            }
        }
        return item;
    }
}
