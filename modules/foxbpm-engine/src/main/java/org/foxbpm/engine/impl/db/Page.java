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
 * @author kenshin
 */
package org.foxbpm.engine.impl.db;


/**
 * 
 * 
 * @author kenshin
 */
public class Page {
  
  protected int pageIndex;
  protected int pageSize;
  
  public Page(int pageIndex, int pageSize) {
    this.pageIndex = pageIndex;
    this.pageSize = pageSize;
  }
  
  public int getPageIndex() {
	return pageIndex;
  }
  public int getPageSize() {
	return pageSize;	
  }
}
