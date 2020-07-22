package com.portgo.manager;

import com.portgo.util.NgnObservableObject;

/**
 * Contact class defining an entity from the native address book or XCAP server.
 */
public class Group extends NgnObservableObject{

	private final int mGroupId;
	private String mGroupName;


	public Group(int id, String groupName){
		super();
		mGroupId = id;
		mGroupName = groupName;
	}

	public int getId(){
		return mGroupId;
	}

//	public Set getGroupMember(){
//		return mGroupMembers;
//	}
//
//	public void  addGroupMember(int contactId){
//		mGroupMembers.add(contactId);
//	}
//	public void removeGroupMember(int contactId){
//		mGroupMembers.remove(contactId);
//	}

	public String getGroupName(){
		return mGroupName;
	}
}
