package com.taosdata.jdbc.utils;

import java.io.File;

import javax.swing.tree.ExpandVetoException;

public class TDNode {
    
    private int index;
    private int running;
    private int deployed;
    private boolean testCluster;
    private int valgrind;
    private String path;    
    private String cfgDir;
    private String dataDir;
    private String logDir;
    private String cfgPath;

    public TDNode(int index) {
        this.index = index;
        running = 0;
        deployed = 0;
        testCluster = false;
        valgrind = 0;        
    }


            // for root, dirs, files in os.walk(projPath):
            //     if ("taosd" in files):
            //         rootRealPath = os.path.dirname(os.path.realpath(root))
            //         if ("packaging" not in rootRealPath):
            //             binPath = os.path.join(root, "taosd")
            //             break
        // else:
        //     projPath = selfPath + "/../../../"
        //     for root, dirs, files in os.walk(projPath):
        //         if ("taosd" in files):
        //             rootRealPath = os.path.dirname(os.path.realpath(root))
        //             if ("packaging" not in rootRealPath):
        //                 binPath = os.path.join(root, "taosd")
        //                 break

        // if (binPath == ""):
        //     tdLog.exit("taosd not found!")
        // else:
        //     tdLog.info("taosd found in %s" % rootRealPath)

    public void start() {
        String selfPath = System.getProperty("user.dir");
        String binPath = "";
        String projDir = selfPath + "../../../";

        File dir = new File(projDir);
                
        File[] fileList = dir.listFiles();
        if(fileList == null || fileList.length == 0) {
            System.out.println("The project path doens't exist");
            return;
        }

        for(File file : fileList) {
            if(file.getName().equals("taosd") && !file.getAbsolutePath().contains("packing")) {
                binPath = file.getAbsolutePath();
                break;
            }
        }

        if(binPath.equals("")) {
            System.out.println("taosd not found");
            return;
        } else {
            System.out.println("taosd found in " + binPath);
        }

        if(this.deployed == 0) {
            System.out.println("dnode" + index + "is not deployed");
            return;
        }

        String cmd = "";
        if(this.valgrind == 0) {
            cmd = "nohup " + binPath + " -c "  + this.cfgDir + " > /dev/null 2>&1 & "; 
        } else {
            String valgrindCmdline = "valgrind --tool=memcheck --leak-check=full --show-reachable=no --track-origins=yes --show-leak-kinds=all -v --workaround-gcc296-bugs=yes";
            cmd = "nohup " + valgrindCmdline + " " + binPath + " -c "  + this.cfgDir + " 2>&1 & ";                                
        }

        try{
            if(Runtime.getRuntime().exec(cmd).waitFor() != 0) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        this.running = 1;        
    }

    public void stop() {
        String toBeKilled = "";
        if (this.valgrind == 0) {
            toBeKilled = "taosd";
        } else {
            toBeKilled = "valgrind.bin";
        }

        if (this.running != 0) {
            String psCmd = "ps -ef|grep -w %s| grep -v grep | awk '{print " + toBeKilled + "}'";
            
        }
    }

    public void startIP() {
        try{
            String cmd = "sudo ifconfig lo:" + index + "192.168.0." + index + " up";
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void stopIP() {
        try{
            String cmd = "sudo ifconfig lo:" + index + "192.168.0." + index + " down";
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCfgConfig(String option, String value) {
        try{
            String cmd = "echo " + option + " " + value + " >> " + this.cfgPath;
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDnodeRootDir() {
        String dnodeRootDir = this.path + "/sim/psim/dnode" + this.index;
        return dnodeRootDir;
    }

    public String getDnodesRootDir() {
        String dnodesRootDir = this.path + "/sim/psim" + this.index;
        return dnodesRootDir;
    }

    public void deploy() {
        this.logDir = this.path + "/sim/dnode" + this.index + "/log";
        this.dataDir = this.path + "/sim/dnode" + this.index + "/data";
        this.cfgDir = this.path + "/sim/dnode" + this.index + "/cfg";
        this.cfgPath = this.path + "/sim/dnode" + this.index + "/cfg/taos.cfg";

        try {        
            String cmd = "rm -rf " + this.logDir;
            Runtime.getRuntime().exec(cmd).waitFor();
            
            cmd = "rm -rf " + this.cfgDir;
            Runtime.getRuntime().exec(cmd).waitFor();

            cmd = "rm -rf " + this.dataDir;
            Runtime.getRuntime().exec(cmd).waitFor();

            cmd = "mkdir -p " + this.logDir;
            Runtime.getRuntime().exec(cmd).waitFor();

            cmd = "mkdir -p " + this.cfgDir;
            Runtime.getRuntime().exec(cmd).waitFor();

            cmd = "mkdir -p " + this.dataDir;
            Runtime.getRuntime().exec(cmd).waitFor();

            cmd = "touch " + this.cfgPath;
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }        

        if(this.testCluster) {
            startIP();
            setCfgConfig("masterIp", "192.168.0.1");
            setCfgConfig("secondIp", "192.168.0.2");
            setCfgConfig("publicIp", "192.168.0." + this.index);
            setCfgConfig("internalIp", "192.168.0." + this.index);
            setCfgConfig("privateIp", "192.168.0." + this.index);
        }
        setCfgConfig("dataDir", this.dataDir);
        setCfgConfig("logDir", this.logDir);
        setCfgConfig("numOfLogLines", "100000000");
        setCfgConfig("mnodeEqualVnodeNum", "0");
        setCfgConfig("walLevel", "1");
        setCfgConfig("statusInterval", "1");
        setCfgConfig("numOfTotalVnodes", "64");
        setCfgConfig("numOfMnodes", "3");
        setCfgConfig("numOfThreadsPerCore", "2.0");
        setCfgConfig("monitor", "0");
        setCfgConfig("maxVnodeConnections", "30000");
        setCfgConfig("maxMgmtConnections", "30000");
        setCfgConfig("maxMeterConnections", "30000");
        setCfgConfig("maxShellConns", "30000");
        setCfgConfig("locale", "en_US.UTF-8");
        setCfgConfig("charset", "UTF-8");
        setCfgConfig("asyncLog", "0");
        setCfgConfig("anyIp", "0");
        setCfgConfig("dDebugFlag", "135");
        setCfgConfig("mDebugFlag", "135");
        setCfgConfig("sdbDebugFlag", "135");
        setCfgConfig("rpcDebugFlag", "135");
        setCfgConfig("tmrDebugFlag", "131");
        setCfgConfig("cDebugFlag", "135");
        setCfgConfig("httpDebugFlag", "135");
        setCfgConfig("monitorDebugFlag", "135");
        setCfgConfig("udebugFlag", "135");
        setCfgConfig("jnidebugFlag", "135");
        setCfgConfig("qdebugFlag", "135"); 
        this.deployed = 1;
    }


    public static void main(String args[]){
        String currPath = System.getProperty("user.dir");
        String projectRoot = currPath + "../../../";
        String cmd = "cd " + projectRoot;
        try {
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

}