package com.dianping.puma.admin.web;

import com.dianping.puma.admin.service.DBInstanceConfigService;
import com.dianping.puma.admin.service.ReplicationTaskService;
import com.dianping.puma.admin.service.ServerConfigService;
import com.dianping.puma.admin.util.GsonUtil;
import com.dianping.puma.core.replicate.model.config.DBInstanceConfig;
import com.dianping.puma.core.replicate.model.config.FileSenderConfig;
import com.dianping.puma.core.replicate.model.config.ServerConfig;
import com.dianping.puma.core.replicate.model.task.ReplicationTask;
import com.dianping.puma.core.replicate.model.BinlogInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.*;

@Controller
public class ReplicationTaskController {

	private static final Logger LOG = LoggerFactory.getLogger(DBInstanceConfigController.class);

	@Autowired
	ReplicationTaskService replicationTaskService;

	@Autowired
	DBInstanceConfigService dbInstanceConfigService;

	@Autowired
	ServerConfigService serverConfigService;

	@RequestMapping(value = { "/replicationTask" })
	public ModelAndView view(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> map = new HashMap<String, Object>();

		List<ReplicationTask> replicationTasks = replicationTaskService.findAll();

		map.put("replicationTasks", replicationTasks);
		map.put("path", "replicationTask");
		return new ModelAndView("main/container", map);
	}

	@RequestMapping(value = { "/replicationTask/create" }, method = RequestMethod.GET)
	public ModelAndView create(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> map = new HashMap<String, Object>();

		List<DBInstanceConfig> dbInstanceConfigs = dbInstanceConfigService.findAll();
		List<ServerConfig> serverConfigs = serverConfigService.findAll();

		map.put("dbInstanceConfigs", dbInstanceConfigs);
		map.put("serverConfigs", serverConfigs);
		map.put("path", "replicationTask");
		map.put("subPath", "create");
		return new ModelAndView("main/container", map);
	}

	@RequestMapping(value = { "/replicationTask/create" }, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public String createPost(String dbInstanceName, String serverName, String binlogFile, String binlogPosition) {

		Map<String, Object> map = new HashMap<String, Object>();

		DBInstanceConfig dbInstanceConfig = dbInstanceConfigService.find(dbInstanceName);
		ServerConfig serverConfig = serverConfigService.find(serverName);

		BinlogInfo binlogInfo = new BinlogInfo();
		binlogInfo.setBinlogFile(binlogFile);
		binlogInfo.setBinlogPosition(Long.parseLong(binlogPosition));

		Timestamp timestamp = new Timestamp((new Date()).getTime());
		long taskId = timestamp.getTime();
		String taskName = timestamp.toString();

		List<FileSenderConfig> fileSenderConfigs = new ArrayList<FileSenderConfig>();
		FileSenderConfig fileSenderConfig = new FileSenderConfig();
		fileSenderConfig.setMasterBucketFilePrefix("bucket-");
		fileSenderConfig.setMaxMasterBucketLengthMB(1000);
		fileSenderConfig.setStorageMasterBaseDir("/data/appdatas/puma/storage/slave/" + taskName);
		fileSenderConfig.setSlaveBucketFilePrefix("bucket-");
		fileSenderConfig.setMaxSlaveBucketLengthMB(1000);
		fileSenderConfig.setStorageSlaveBaseDir("/data/appdatas/puma/storage/slave/" + taskName);
		fileSenderConfig.setFileSenderName("storage-" + taskName);
		fileSenderConfig.setStorageName("storage-" + taskName);
		fileSenderConfig.setPreservedDay(2);
		fileSenderConfig.setMaxMasterFileCount(50);
		fileSenderConfigs.add(fileSenderConfig);

		ReplicationTask replicationTask = new ReplicationTask();

		// @TODO
		replicationTask.setTaskId(taskId);
		replicationTask.setTaskName(taskName);

		replicationTask.setDispatchName("dispatch-" + taskName);
		replicationTask.setFileSenderConfigs(fileSenderConfigs);

		replicationTask.setDbInstanceConfig(dbInstanceConfig);
		replicationTask.setServerConfig(serverConfig);
		replicationTask.setBinlogInfo(binlogInfo);

		try {
			this.replicationTaskService.save(replicationTask);
		}
		catch(Exception e) {
			map.put("success", false);
		}

		map.put("success", true);

		return GsonUtil.toJson(map);
	}
}