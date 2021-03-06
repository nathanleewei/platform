/**
 * 
 */
package com.taobao.tairtest_touch;

import com.ibm.staf.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author ashu
 * 
 */
public class FailOverBaseCase extends BaseTestCase {
	// Directory
	final static String tair_bin = "/home/admin/tair_bin_mdb/";
	final static String test_bin = "/home/admin/ashu/recovery/";
	// Server Operation
	final static String start = "start";
	final static String stop = "stop";
	// Tool Option
	final static String put = "put";
	final static String get = "get";
	final static String rem = "rem";
	// Key Words for log
	final static String start_migrate = "need migrate,";
	final static String finish_migrate = "migrate all done";
	final static String finish_rebuild = "version changed";
	// Parameters
	final static int down_time = 10;
	final static int ds_down_time = 10;
	final static String put_count = "100000";
	final static float put_count_float = 100000.0f;
	// Server List
	final String csarr[] = new String[] { "10.232.4.14", "10.232.4.15" };
	final String dsarr[] = new String[] { "10.232.4.14", "10.232.4.15", "10.232.4.16", "10.232.4.17", "10.232.4.18" };
	final List csList = Arrays.asList(csarr);
	final List dsList = Arrays.asList(dsarr);

	/**
	 * @param machine
	 * @param opID
	 * @param type
	 *            0:normal 1:force
	 * @return
	 */
	public boolean control_cs(String machine, String opID, int type) {
		log.debug("control cs:" + machine + " " + opID + " type=" + type);
		boolean ret = false;
		String cmd = "cd " + FailOverBaseCase.tair_bin + " && ./tair.sh " + opID + "_cs && sleep 5";
		if (opID.equals(FailOverBaseCase.stop) && type == 1)
			cmd = "killall -9 tair_cfg_svr_mdb && sleep 2";
		executeShell(stafhandle, machine, cmd);
		cmd = "ps -ef|grep tair_cfg_svr_mdb|wc -l";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0) {
			log.debug("cs rc!=0");
			ret = false;
		} else {
			String stdout = getShellOutput(result);
			log.debug("------------cs ps result--------------" + stdout);
			if (opID.equals(FailOverBaseCase.start) && (new Integer(stdout.trim())).intValue() != 3) {
				ret = false;
			} else if (opID.equals(FailOverBaseCase.stop) && (new Integer(stdout.trim())).intValue() != 2) {
				ret = false;
			} else {
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * @param machine
	 * @param opID
	 * @param type
	 *            0:normal 1:force
	 * @return
	 */
	public boolean control_ds(String machine, String opID, int type) {
		log.debug("control ds:" + machine + " " + opID + " type=" + type);
		boolean ret = false;
		String cmd = "cd " + FailOverBaseCase.tair_bin + " && ./tair.sh " + opID + "_ds ";
		int expectNum = 0;
		if (opID.equals(FailOverBaseCase.stop)) {
			expectNum = 2;
		} else if (opID.equals(FailOverBaseCase.start)) {
			expectNum = 3;
		}
		executeShell(stafhandle, machine, cmd);

		if (opID.equals(FailOverBaseCase.stop) && type == 1)
			cmd = "killall -9 tair_server_mdb && sleep 2";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		int waittime = 0;
		cmd = "ps -ef|grep tair_server_mdb|wc -l";
		while (waittime < 110) {
			result = executeShell(stafhandle, machine, cmd);
			if (result.rc != 0) {
				log.debug("ds rc!=0");
				ret = false;
			} else {
				String stdout = getShellOutput(result);
				if ((new Integer(stdout.trim())).intValue() == expectNum) {

					log.debug("------------ds ps result--------------" + stdout);
					ret = true;
					break;
				} else {
					ret = false;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {

					}
					waittime++;
				}
			}
		}
		return ret;
	}

	/**
	 * @param cs_group
	 * @param opID
	 * @param type
	 *            0:normal 1:force
	 * @return
	 */
	public boolean batch_control_cs(List cs_group, String opID, int type) {
		boolean ret = false;
		for (Iterator it = cs_group.iterator(); it.hasNext();) {
			if (!control_cs((String) it.next(), opID, type)) {
				ret = false;
				break;
			} else
				ret = true;
		}
		return ret;
	}

	/**
	 * @param ds_group
	 * @param opID
	 * @param type
	 *            0:normal 1:force
	 * @return
	 */
	public boolean batch_control_ds(List ds_group, String opID, int type) {
		boolean ret = false;
		for (Iterator it = ds_group.iterator(); it.hasNext();) {
			if (!control_ds((String) it.next(), opID, type)) {
				ret = false;
				break;
			} else
				ret = true;
		}
		return ret;
	}

	/**
	 * @param cs_group
	 * @param ds_group
	 * @param opID
	 * @param type
	 *            0:normal 1:force
	 * @return
	 */
	public boolean control_cluster(List cs_group, List ds_group, String opID, int type) {
		boolean ret = false;
		if (!batch_control_ds(ds_group, opID, type) || !batch_control_cs(cs_group, opID, type))
			ret = false;
		else
			ret = true;
		return ret;
	}

	public boolean clean_data(String machine) {
		boolean ret = false;
		String cmd = "cd " + FailOverBaseCase.tair_bin + " && ./tair.sh clean";
		STAFResult rst = executeShell(stafhandle, machine, cmd);
		if (rst.rc != 0)
			ret = false;
		else
			ret = true;
		return ret;
	}

	public boolean clean_tool(String machine) {
		boolean ret = false;
		killall_tool_proc();
		String cmd = "cd " + FailOverBaseCase.test_bin + " && ";
		cmd += "./clean.sh";
		STAFResult rst = executeShell(stafhandle, machine, cmd);
		if (rst.rc != 0)
			ret = false;
		else
			ret = true;
		return ret;
	}

	public boolean execute_data_verify_tool() {
		log.debug("start verify tool,run batchData");
		boolean ret = false;
		String cmd = "cd " + test_bin + " && ";
		cmd += "./batchData.sh";
		STAFResult result = executeShell(stafhandle, "local", cmd);
		if (result.rc != 0)
			ret = false;
		else
			ret = true;
		return ret;
	}

	public boolean execute_stress_tool(int cnt) {
		log.debug("start stress tool");
		boolean ret = false;
		String cmd = "cd " + test_bin + " && ";
		cmd += "./debug.sh " + cnt;
		STAFResult result = executeShell(stafhandle, "local", cmd);
		if (result.rc != 0)
			ret = false;
		else
			ret = true;
		return ret;
	}

	public boolean killall_tool_proc() {
		log.debug("force kill all data tool process");
		boolean ret = false;
		String cmd = "killall -9 mdb_tool";
		STAFResult result = executeShell(stafhandle, "local", cmd);
		if (result.rc != 0)
			ret = false;
		else
			ret = true;
		return ret;
	}

	public boolean batch_clean_data(List machines) {
		boolean ret = true;
		for (Iterator it = machines.iterator(); it.hasNext();) {
			if (!clean_data((String) it.next()))
				ret = false;
		}
		return ret;
	}

	public boolean reset_cluster(List csList, List dsList) {
		boolean ret = false;
		log.debug("stop and clean cluster!");
		if (control_cluster(csList, dsList, FailOverBaseCase.stop, 1) && batch_clean_data(csList) && batch_clean_data(dsList))
			ret = true;
		return ret;
	}
	
	public int check_process(String machine, String prname) {
		log.debug("check process " + prname + " on " + machine);
		int ret = 0;
		String cmd = "ps -ef|grep " + prname + "|wc -l";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = -1;
		else {
			String stdout = getShellOutput(result);
			try {
				ret = (new Integer(stdout.trim())).intValue();
			} catch (Exception e) {
				log.debug("get verify exception: " + stdout);
				ret = -1;
			}
		}
		return ret;
	}

	public int check_keyword(String machine, String keyword, String logfile) {
		int ret = 0;
		String cmd = "grep \"" + keyword + "\" " + logfile + " |wc -l";
		log.debug("check keyword:" + cmd + " on " + machine + " in file:" + logfile);
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = -1;
		else {
			String stdout = getShellOutput(result);
			try {
				ret = (new Integer(stdout.trim())).intValue();
				log.error("time=" + ret);
			} catch (Exception e) {
				log.debug("get verify exception: " + stdout);
				ret = -1;
			}
		}
		return ret;
	}

	public int touch_file(String machine, String file) {
		int ret = 0;
		String cmd = "touch " + file;
		log.debug("touch file:" + file + " on " + machine);
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = -1;
		return ret;
	}

	/**
	 * @param machine
	 *            machine which you want to operate
	 * @param confname
	 *            file which you want to modify , should include full path
	 * @param key
	 *            key which you want to modify
	 * @param value
	 *            value which you want to change
	 * @return
	 */
	public boolean modify_config_file(String machine, String confname, String key, String value) {
		log.debug("change file:" + confname + " on " + machine + " key=" + key + " value=" + value);
		boolean ret = false;
		String cmd = "sed -i \"s/" + key + "=.*$/" + key + "=" + value + "/\" " + confname + " && grep \"" + key + "=.*$\" " + confname + "|awk -F\"=\" \'{print $2}\'";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		else {
			String stdout = getShellOutput(result);
			if (stdout.trim().equals(value))
				ret = true;
			else
				ret = false;
		}
		return ret;
	}
///////////////////////////////////////////////////////////////////////////////////////////////
	public boolean batch_modify(List machines, String confname, String key, String value) {
		boolean ret = true;
		for (Iterator it = machines.iterator(); it.hasNext();) {
			if (!modify_config_file((String) it.next(), confname, key, value)) {
				ret = false;
				break;
			}
		}
		return ret;
	}
	
	public void shutoff_net(String machine1, String machine2) {
		log.debug("shut off net between " + machine1 + " and " + machine2);
		boolean ret1 = false;
		boolean ret2 = false;
		String cmd = "ssh root@localhost " + "\"/sbin/iptables -A OUTPUT -d " + machine2 +" -j DROP\"";
		STAFResult result = executeShell(stafhandle, machine1, cmd);
		if (result.rc != 0) {
			ret1 = false;
//			return ret1;
		}
		else {
			cmd = "ssh root@localhost " + "\"/sbin/iptables-save\"";
			result = executeShell(stafhandle, machine1, cmd);
			if (result.rc != 0) {
				ret1 = false;
//				return ret1;
			}
			else {
				ret1 = true;
//				return ret1;
			}
		}
		cmd = "ssh root@localhost " + "\"/sbin/iptables -A OUTPUT -d " + machine1 +" -j DROP\"";
		result = executeShell(stafhandle, machine2, cmd);
		if (result.rc != 0) {
			ret2 = false;
//			return ret2;
		}
		else {
			cmd = "ssh root@localhost " + "\"/sbin/iptables-save\"";
			result = executeShell(stafhandle, machine2, cmd);
			if (result.rc != 0) {
				ret2 = false;
//				return ret2;
			}
			else {
				ret2 = true;
//				return ret2;
			}
		}
		if(!ret1 || !ret2)
			fail("shut off net between " + machine1 + " and " + machine2 + " failure!");
		log.error("shut off net between " + machine1 + " and " + machine2 + " successful!");
	}
	
	public boolean recover_net(String machine) {
		log.debug("recover net on " + machine);
		boolean ret = true;
		String cmd = "ssh root@localhost " + "\"/sbin/iptables -F\"";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		return ret;
	}
	
	public boolean batch_recover_net(List machines) {
		boolean ret = true;
		for (Iterator it = machines.iterator(); it.hasNext();) {
			if (!recover_net((String)it.next())) {
				ret = false;
				break;
			}
		}
		return ret;
	}
	
	public boolean execute_tool_on_mac(String machine, String option) {
		log.debug("start " + option + " on " + machine);
		if(!modify_config_file(machine, FailOverBaseCase.tair_bin+"tools/DataDebug.conf", "actiontype", option))
			fail("modify configure file failed");
		if(!modify_config_file(machine, FailOverBaseCase.tair_bin+"tools/DataDebug.conf", "datasize", FailOverBaseCase.put_count))
			fail("modify configure file failed");
		if(!modify_config_file(machine, FailOverBaseCase.tair_bin+"tools/DataDebug.conf", "filename", "read.kv"))
			fail("modify configure file failed");
		
		boolean ret = true;
		String cmd = "cd " + FailOverBaseCase.tair_bin+ "tools/" + "&& ./batchData.sh";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		return ret;
	}
	
	public void wait_tool_on_mac(String machine)
	{
		log.error("start wait on " + machine);
		int count=0;
		while(check_process(machine, "DataDebug")!=2)
		{
			waitto(16);
			if(++count>150)break;
		}
		if(count>150)fail("wait time out on " + machine + "!");
		log.error("wait success on " + machine);
	}
	
	public boolean execute_clean_on_cs(String machine) {
		log.debug("clean kv and log files on " + machine);
		boolean ret = true;
                String cmd = "cd " + FailOverBaseCase.tair_bin + "tools/" + "&& ./clean.sh";
                STAFResult result = executeShell(stafhandle, machine, cmd);
                if (result.rc != 0)
                        ret = false;
                return ret;
	}
	
	public void wait_keyword_equal(String machine, String keyword, int expect_count)
	{
		int count=0;
		while(check_keyword(machine, keyword, FailOverBaseCase.tair_bin+"logs/config.log")!=expect_count)
		{
			waitto(2);
			if(++count>10)break;
		}
		if(count>10)fail("wish the count of " + keyword + " on " + machine + " not change, but changed!");
		log.error("the count of " + keyword + " on " + machine + " is right!");
	}
	
	public void wait_keyword_change(String machine, String keyword, int base_count)
	{
		int count=0;
		while(check_keyword(machine, keyword, FailOverBaseCase.tair_bin+"logs/config.log")<=base_count)
		{
			waitto(2);
			if(++count>10)break;
		}
		if(count>10)fail("wish the count of " + keyword + " on " + machine + " change, but not changed!");
		log.error("the count of " + keyword + " on " + machine + " is right!");
	}

	protected int getVerifySuccessful(String machine) {
		int ret = 0;
		String verify = "tail -10 " + FailOverBaseCase.tair_bin + "tools/" + "datadbg0.log|grep \"Successful\"|awk -F\" \" \'{print $3}\'";
		log.debug("do verify on " + machine);
		STAFResult result = executeShell(stafhandle, machine, verify);
		if (result.rc != 0)
			ret = -1;
		else {
			String stdout = getShellOutput(result);
			try {
				ret = (new Integer(stdout.trim())).intValue();
			} catch (Exception e) {
				log.debug("get verify exception: " + stdout);
				ret = -1;
			}
		}
		log.error(ret);
		return ret;
	}

	//if(!copy_file(csList.get(0), FailOverBaseCase.table_path + "group_1_server_table", local))fail("copy table file failed!");
	public boolean copy_file(String source_machine, String filename, String target_machine) {
		log.error("Copy " + filename + " from " + source_machine +" to " + target_machine);
		boolean ret = true;
		String cmd = "scp " + filename + " " + target_machine + ":" + FailOverBaseCase.test_bin;
		STAFResult result = executeShell(stafhandle, source_machine, cmd);
		if (result.rc != 0)
			ret = false;
		return ret;
	}
////////////////////////////////////////////////////////////////////////////////////////////////////
	public boolean comment_line(String machine, String file, String keyword, String comment) {
		boolean ret = false;
		String cmd = "sed -i \'s/.*" + keyword + "/" + comment + "&/\' " + file;
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		else {
			// String stdout=getShellOutput(result);
			ret = true;
		}
		return ret;
	}

	public boolean uncomment_line(String machine, String file, String keyword, String comment) {
		boolean ret = false;
		String cmd = "sed -i \'s/" + comment + "*\\(.*" + keyword + ".*$\\)/\\1/\' " + file;
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		else {
			// String stdout=getShellOutput(result);
			ret = true;
		}
		return ret;
	}

	public boolean batch_uncomment(List machines, String confname, List keywords, String comment) {
		boolean ret = true;
		for (Iterator ms = machines.iterator(); ms.hasNext();) {
			String cms = (String) ms.next();
			for (Iterator ks = keywords.iterator(); ks.hasNext();) {
				if (!uncomment_line(cms, confname, (String) ks.next(), comment))
					ret = false;
			}
		}
		return ret;
	}

	/**
	 * @return successful count
	 */
	protected int getVerifySuccessful() {
		int ret = 0;
		String verify = "cd " + FailOverBaseCase.test_bin + " && ";
		verify += " tail -10 datadbg0.log|grep \"Successful\"|awk -F\" \" \'{print $3}\'";
		log.debug("do verify on local");
		STAFResult result = executeShell(stafhandle, "local", verify);
		if (result.rc != 0)
		{
			log.debug("get result "+result );
			ret = -1;
		}
		else {
			String stdout = getShellOutput(result);
			try {
				ret = (new Integer(stdout.trim())).intValue();
                log.error(ret);
			} catch (Exception e) {
				log.debug("get verify exception: " + stdout);
				ret = -1;
			}
		}
		return ret;
	}

	/**
	 * @return successful count
	 */
	protected int getDSFailNum(String dsName) {
		int ret = 0;
		String verify = "cd " + FailOverBaseCase.test_bin + " && ";
		verify += "cat datadbg0.log|grep \"get failure: " + dsName + "\"|wc -l";
		STAFResult result = executeShell(stafhandle, "local", verify);
		if (result.rc != 0)
			ret = -1;
		else {
			String stdout = getShellOutput(result);
			try {
				ret = (new Integer(stdout.trim())).intValue();
			} catch (Exception e) {
				log.debug("get verify exception: " + stdout);
				ret = -1;
			}
		}
		return ret;
	}
}
