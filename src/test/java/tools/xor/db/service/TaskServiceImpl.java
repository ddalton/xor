/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations 
 * under the License.
 */

package tools.xor.db.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.db.dao.TaskDao;
import tools.xor.db.pm.Task;

@Service("taskService")
public class TaskServiceImpl implements TaskService {
	private static final Logger logger = LogManager.getLogger(new Exception()
	.getStackTrace()[0].getClassName());

	@Autowired private TaskDao taskDao;	

	@Override
	@Transactional
	public Task createTask(Task task) {
		logger.debug("Entering method createTask(Task task).");

		task = taskDao.saveOrUpdate(task); 

		return task;
	}

	@Override
	@Transactional
	public Task getTask(String taskId) {
		logger.debug("Entering method getTask(String taskId).");
		Task task = taskDao.findById(taskId);

		return task;	
	}
}
