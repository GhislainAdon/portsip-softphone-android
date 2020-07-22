package com.portgo.manager;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;

import com.portgo.util.NgnObservableObject;
import com.portgo.util.NgnStringUtils;

import java.io.InputStream;

/**
 * Contact class defining an entity from the native address book or XCAP server.
 */
public class SimpleContact extends NgnObservableObject{

	private final int mId;
	private String mDisplayName;
	private int mPhotoId;
	protected Bitmap mPhoto;
	private String sortName;
    private int STARRED;

	/**
	 * Creates new address book
	 * @param id a unique id defining this contact
	 * @param displayName the contact's display name
	 */
	public SimpleContact(int id, String displayName){
		super();
		mId = id;
		mDisplayName = displayName;
		sortName = NgnStringUtils.getAllFirstLetter(displayName);
	}
	
	/**
	 * Gets the id of the contact
	 * @return a unique id defining this contact
	 */
	public int getId(){
		return mId;
	}
	


	public void setSortName(String sortName) {
		this.sortName = sortName;
	}

	public String getSortName() {
		return sortName;
	}

	/**
	 * Sets the contact's display name value
	 * @param displayName the new display name to assign to the contact
	 */
	public void setDisplayName(String displayName){
		mDisplayName = displayName;
		sortName = NgnStringUtils.getAllFirstLetter(displayName);
		super.setChangedAndNotifyObservers(displayName);
	}
	
	/**
	 * Gets the contact's display name
	 * @return the contact's display name
	 */
	public String getDisplayName(){
		return NgnStringUtils.isNullOrEmpty(mDisplayName) ? NgnStringUtils.nullValue() : mDisplayName;
	}
	
	public void setPhotoId(int photoId){
		mPhotoId = photoId;
	}
	
	/**
	 * Gets the photo associated to this contact
	 * @param context
	 * @param preferHighres true 原图,false 缩略图
	 * @return a bitmap representing the contact's photo
	 */
	public Bitmap getPhoto(Context context,boolean preferHighres ){
		if(mPhotoId != 0 && mPhoto == null){
			try{
				Uri contactPhotoUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, mId);
			    InputStream photoDataStream = Contacts.openContactPhotoInputStream(context.getContentResolver(), contactPhotoUri,preferHighres);
			    if(photoDataStream != null){
			    	mPhoto = BitmapFactory.decodeStream(photoDataStream);
			    	photoDataStream.close();
			    }
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		return mPhoto;
	}

	public void setStarrred(int starrred) {
		this.STARRED = starrred;
	}

	public int getSTARRED() {
		return STARRED;
	}
}
