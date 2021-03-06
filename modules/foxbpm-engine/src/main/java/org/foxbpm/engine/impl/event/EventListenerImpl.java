/**
 * Copyright 1996-2014 FoxBPM ORG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author ych
 */
package org.foxbpm.engine.impl.event;

import org.foxbpm.engine.event.EventListener;

public class EventListenerImpl implements EventListener {
	
	private String id;
	private String eventType;
	private String listenerClass;
	private int priority;
	
	
	public EventListenerImpl(){
		
	}
	
	public EventListenerImpl(String id,String eventType,String listenerClass){
		this.id = id;
		this.listenerClass = listenerClass;
		this.eventType = eventType;
	}
	
	public EventListenerImpl(String id,String eventType,String listenerClass,int priority){
		this.id = id;
		this.listenerClass = listenerClass;
		this.eventType = eventType;
		this.priority = priority ;
	}
	
	public String getEventType() {
		return eventType;
	}
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}
	public String getListenerClass() {
		return listenerClass;
	}
	
	public void setListenerClass(String listenerClass) {
		this.listenerClass = listenerClass;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	
}
