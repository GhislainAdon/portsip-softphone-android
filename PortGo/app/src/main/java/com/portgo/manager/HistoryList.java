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
package com.portgo.manager;

import com.portgo.util.NgnObservableList;
import com.portgo.util.NgnPredicate;

import java.util.Collection;
import java.util.List;

public class HistoryList {
    private final NgnObservableList<HistoryEvent> mEvents;
    
    @SuppressWarnings("unused")
	private List<HistoryEvent> mSerializableEvents;
	
    public HistoryList(){
    	mEvents = new NgnObservableList<HistoryEvent>(true);
    	mSerializableEvents = mEvents.getList();
    }
    
	public NgnObservableList<HistoryEvent> getList(){
		return mEvents;
	}
	
	public void setList(List<HistoryEvent> list){
		mEvents.add(list);
	}	
	
	public void addEvent(HistoryEvent e){
		mEvents.add(0, e);
	}
	
	public void removeEvent(HistoryEvent e){
		if(mEvents != null){
			mEvents.remove(e);
		}
	}
	
	public void removeEvents(Collection<HistoryEvent> events){
		if(mEvents != null){
			mEvents.removeAll(events);
		}
	}
	
	public void removeEvents(NgnPredicate<HistoryEvent> predicate){
		if(mEvents != null){
			final List<HistoryEvent> eventsToRemove = mEvents.filter(predicate);
			mEvents.removeAll(eventsToRemove);
		}
	}
	
	public void removeEvent(int location){
		if(mEvents != null){
			mEvents.remove(location);
		}
	}
	
	public void clear(){
		if(mEvents != null){
			mEvents.clear();
		}
	}	
	
}
