
package com.portgo.manager;

import com.portgo.util.NgnObservableList;
import com.portgo.util.NgnPredicate;

import java.util.Collection;
import java.util.List;

public class UserAccountList {
    private final NgnObservableList<UserAccount> mAccounts;
	
    public UserAccountList(){
    	mAccounts = new NgnObservableList<UserAccount>(true);
    }
    
	public NgnObservableList<UserAccount> getList(){
		return mAccounts;
	}
	
	public void setList(List<UserAccount> uses){
		mAccounts.add(uses);
	}	
	
	public void addAccount(UserAccount user){
		mAccounts.add(0, user);
	}
	
	public void removeAccount(UserAccount user){
		if(mAccounts != null){
			mAccounts.remove(user);
		}
	}
	
	public void removeAccounts(Collection<UserAccount> events){
		if(mAccounts != null){
			mAccounts.removeAll(events);
		}
	}
	
	public void removeAccounts(NgnPredicate<UserAccount> predicate){
		if(mAccounts != null){
			final List<UserAccount> eventsToRemove = mAccounts.filter(predicate);
			mAccounts.removeAll(eventsToRemove);
		}
	}
	
	public void removeAccount(int location){
		if(mAccounts != null){
			mAccounts.remove(location);
		}
	}
	
	public void clear(){
		if(mAccounts != null){
			mAccounts.clear();
		}
	}
	
	
}
