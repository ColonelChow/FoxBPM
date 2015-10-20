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
 * @author ych
 */
package org.foxbpm.engine.impl.cmd;

import java.util.ArrayList;
import java.util.List;

import org.foxbpm.engine.identity.UserDefinition;
import org.foxbpm.engine.impl.db.Page;
import org.foxbpm.engine.impl.entity.UserEntity;
import org.foxbpm.engine.impl.interceptor.Command;
import org.foxbpm.engine.impl.interceptor.CommandContext;

/**
 * 根据userId 或者userName 模糊匹配 参数之间or关系
 * 
 * @author Administrator
 * 
 */
public class FindUsersCmd implements Command<List<UserEntity>> {

	/**
	 * 示例：%20080101%
	 */
	private String idLike;
	/**
	 * 示例：%张三%
	 */
	private String nameLike;
	private Page page;

	public FindUsersCmd(String idLike, String nameLike, Page page) {
		this.idLike = idLike;
		this.nameLike = nameLike;
		this.page = page;
	}

	 
	public List<UserEntity> execute(CommandContext commandContext) {
		UserDefinition userDefinition = commandContext.getUserEntityManager();
		List<UserEntity> userEntityList = null;
		if (null != page) {
			userEntityList = userDefinition.findUsers(idLike, nameLike, page.getPageIndex(), page.getPageSize());
		} else {
			userEntityList = userDefinition.findUsers(idLike, nameLike);
		}
		List<UserEntity> userList =new ArrayList<UserEntity>();
		if (null != userEntityList && !userEntityList.isEmpty()) {
			for (UserEntity userEntity : userEntityList) {
				userList.add(userEntity);
			}
		}
		return userList;
	}
}
