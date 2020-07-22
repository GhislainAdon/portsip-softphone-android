/* Copyright (C) 2010-2011, Mamadou Diop.
*  Copyright (C) 2011, Doubango Telecom.
*
* Contact: Mamadou Diop <diopmamadou(at)doubango(dot)org>
*	
* This file is part of imsdroid Project (http://code.google.com/p/imsdroid)
*
* imsdroid is free software: you can redistribute it and/or modify it under the terms of 
* the GNU General Public License as published by the Free Software Foundation, either version 3 
* of the License, or (at your option) any later version.
*	
* imsdroid is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*	
* You should have received a copy of the GNU General Public License along 
* with this program; if not, write to the Free Software Foundation, Inc., 
* 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package com.portgo.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

public class NgnObservableHashMap<K, V>  extends NgnObservableObject implements Observer{
	private final LinkedHashMap<K, V> mHashMap;
	private final boolean mWatchValueChanged;
	
	public NgnObservableHashMap(boolean watchValueChanged){
		super();
		mHashMap = new LinkedHashMap<K, V>();
		if((mWatchValueChanged = watchValueChanged)){
			
		}
	}
	
	public V put(K key, V value){
		if(value == null){
			return null;
		}
		V result = mHashMap.put(key, value);
		if(mWatchValueChanged && value instanceof Observable){
			((Observable)value).addObserver(this);
		}
		super.setChangedAndNotifyObservers(result);
		return result;
	}
	
	public V get(K key){
		return mHashMap.get(key);
	}
	
	public V getAt(int position){
		int index = 0;
		for(Entry<K, V> entry : mHashMap.entrySet()) {
			if(index == position){
				return entry.getValue();
			}
		    index++;
		}
		return null;
	}
	
	public Collection<V> values(){
		return this.mHashMap.values();
	}
	
	public V remove(K key){
		V result = this.mHashMap.remove(key);
		if(this.mWatchValueChanged && result != null && result instanceof Observable){
			((Observable)result).deleteObserver(this);
		}
		super.setChangedAndNotifyObservers(result);
		return result;
	}


    public boolean addAll(HashMap<K, V> map) {

        if(map!=null&&map.size()>0)
        {
            Iterator iterator = map.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry entry = (Map.Entry) iterator.next();
                try {
                    K key = (K) entry.getKey();
                    V value = (V) entry.getValue();
                    if(mWatchValueChanged && value instanceof Observable){
                        ((Observable)value).addObserver(this);
                    }
                    mHashMap.put(key,value);
                }catch (Exception e){
                    return false;
                }
            }
        }

        NotifyData notify = new NotifyData(this,map,NotifyData.Action.ACTIONT_ADD_CONNECTION);
        super.setChangedAndNotifyObservers(notify);
        return true;
    }

	public boolean clear() {
		if (mHashMap.size() > 0) {
			if (this.mWatchValueChanged) {
				Iterator iterator = mHashMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry entry = (Map.Entry) iterator.next();
					V object = (V) entry.getValue();
					if (object != null && object instanceof Observable) {
						((Observable) object).deleteObserver(this);
					}

				}
			}
			mHashMap.clear();

			NotifyData notify = new NotifyData(this, null, NotifyData.Action.ACTIONT_CLEAR);
			super.setChangedAndNotifyObservers(notify);
		}
		return true;
	}
	public boolean isEmpty(){
		return this.mHashMap.isEmpty();
	}
	
	public boolean containsKey(K key){
		return this.mHashMap.containsKey(key);
	}
	
	public Set<Entry<K, V>> entrySet(){
		return this.mHashMap.entrySet();
	}
	
	public int size(){
		return this.mHashMap.size();
	}
	
	@Override
	public void update(Observable observable, Object data) {
		super.setChangedAndNotifyObservers(data);
	}
}
