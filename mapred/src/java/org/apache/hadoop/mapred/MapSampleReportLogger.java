package org.apache.hadoop.mapred;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

import java.util.HashMap;

@InterfaceAudience.Private
@InterfaceStability.Unstable
public class MapSampleReportLogger {
    public static final Log LOG = LogFactory.getLog(MapSampleReportLogger.class);

    HashMap<String, MapSampleReport> sampleReports = new HashMap<String, MapSampleReport>();

    public void resetSampleReportForJob(String jobId){
        sampleReports.put(jobId, null);
        LOG.info("0. SampleReport cleared: " + jobId);
    }

    public boolean logNetworkCopyDurationForTheFirstTime(String jobId, long duration, String reduceTracker){
        if(duration<0)
            return false;
        MapSampleReport report = sampleReports.get(jobId);
        if(report==null || !(report.getNetworkWriteDurationMilliSec()<0) || report.getTrackerName().equals(reduceTracker))
            return false;
        if(report.getNetworkWriteBytes()<0)
            return false;

        return true;
    }

    public void logNetworkCopyDurationAndReduceTracker (String jobId, long duration, String reduceTracker){
        LOG.info("5. logNetworkCopyDurationAndReduceTracker" + jobId);

        MapSampleReport report = sampleReports.get(jobId);

        if(report.getNetworkWriteBytes()<0){
            LOG.error("logNetworkCopyDurationAndReduceTracker invoked before logLocalReducesPercentage!");
            return;
        }
        
        report.setReduceTrackerName(reduceTracker);
        report.setNetworkWriteDurationMilliSec(duration);
        report.taskNetworkIORate = calculateTaskNetworkIORate(report);

        LOG.info(jobId + " map sample report: " + report.toString());
    }

    public void logLocalReducesPercentage(TaskInProgress tip, float localReducesPercentage){
        LOG.info("4. logLocalReducesPercentage: " + tip.getJob().getJobID().toString());

        MapSampleReport report = getReport(tip, null);
        if(report.getDiskWriteBytes()<0){
            LOG.error("logLocalReducesPercentage invoked before logStatsUponMapSampleTaskComplete!");
            return;
        }
        report.localReducesPercentage = localReducesPercentage;

//        long estimatedNetworkWriteBites = (long)((1-report.localReducesPercentage)*report.getDiskWriteBytes());
        long estimatedNetworkWriteBites = (long)(1.0*report.getDiskWriteBytes()/tip.getJob().numReduceTasks); //divided by total number of partitions
        report.setNetworkWriteBytes(estimatedNetworkWriteBites);
    }

    public void logStatsUponMapSampleTaskComplete(TaskInProgress tip){
        LOG.info("3. logStatsUponMapSampleTaskComplete:" + tip.getJob().getJobID().toString());

        TaskStatus taskStatus = tip.getTaskStatus(tip.getSampleTaskId());
        SampleTaskStatus sampleStatus = taskStatus.getSampleStatus();
        MapSampleReport report = getReport(tip, null);
        LOG.info("taskID: " + tip.getSampleTaskId());
        report.setSampleMapTaskId(tip.getSampleTaskId());    //log again in case the first sample task fails.

//        long mapDuration = tip.getExecFinishTime() - tip.getLastDispatchTime();
        long mapDuration = sampleStatus.getWriteOutputDoneTime() - sampleStatus.getReadInputStartTime();
        report.setMapDurationMilliSec(mapDuration);
        report.setReadDuration(sampleStatus.getReadInputDoneTime() - sampleStatus.getReadInputStartTime());
        report.setDiskWriteDurationMilliSec(sampleStatus.getWriteOutputDoneTime() - sampleStatus.getWriteOutputStartTime());
        report.setAdditionalSpillDurationMilliSec(sampleStatus.getAdditionalSpillDurationMilliSec());

        report.setDiskWriteBytes(taskStatus.getOutputSize());
        report.setReadSize(tip.getMapInputSize());
        report.setAdditionalSpillBytes(sampleStatus.getAdditionalSpillSize());

        report.taskDiskIORate = calculateTaskDiskIORate(report);

    }

    public void logTrackerStatus(TaskInProgress tip, TaskTrackerStatus trackerStatus){
        LOG.info("2. logTrackerStatus:" + tip.getJob().getJobID().toString());

        MapSampleReport report = getReport(tip, null);
        TaskTrackerStatus.ResourceStatus resourceStatus = trackerStatus.getResourceStatus();
        LOG.info(resourceStatus.toString());

        report.setTrackerCPUScore(resourceStatus.calculateCPUScore());
        report.setTrackerDiskReadScore(resourceStatus.getDiskReadScore());
        report.setTrackerDiskWriteScore(resourceStatus.getDiskWriteScore());
        report.setTrackerNetworkIOScore(resourceStatus.getNetworkScore());
    }

    public void logDataLocality(TaskInProgress tip, boolean dataLocal, String taskTracker){
        LOG.info("1. logDataLocality:" + tip.getJob().getJobID().toString());

        String jobId = tip.getJob().getJobID().toString();
        MapSampleReport report = getReport(tip, taskTracker);
        report.dataLocal = dataLocal;
        report.setSampleMapTaskId(tip.getSampleTaskId());

        sampleReports.put(jobId, report);  //first method called in the logging sequence, therefore need to put
    }

    private MapSampleReport getReport(TaskInProgress tip, String taskTracker){
        TaskAttemptID taskId = tip.getSampleTaskId();
        String jobId = taskId.getJobID().toString();
        TaskStatus taskStatus = tip.getTaskStatus(taskId);
        String taskTrackerName = taskStatus == null ? taskTracker : taskStatus.getTaskTracker();
        MapSampleReport report = sampleReports.get(jobId);
        if(report == null)
            report = new MapSampleReport(taskTrackerName);
//        else if(taskStatus != null && taskStatus.getRunState() == TaskStatus.State.SUCCEEDED)
//            report.setTrackerName(taskStatus.getTaskTracker());

        return report;
    }

    public TaskAttemptID getSampleMapTaskId(JobID jobId){
        MapSampleReport report = sampleReports.get(jobId.toString());
        if(report != null)
            return report.getSampleMapTaskId();

        return null;
    }

    public String getSampleMapTracker(JobID jobId){
        MapSampleReport report = sampleReports.get(jobId.toString());
        if(report != null)
            return report.getTrackerName();

        return null;
    }

    private long calculateTaskDiskIORate(MapSampleReport report){
        if(report.getDiskReadBytes()==MapSampleReport.UNAVAILABLE
                || report.getDiskWriteBytes() == MapSampleReport.UNAVAILABLE
                || report.getDiskReadDurationMilliSec() == MapSampleReport.UNAVAILABLE
                || report.getDiskWriteDurationMilliSec() == MapSampleReport.UNAVAILABLE)
            return MapSampleReport.UNAVAILABLE;

        long totalDiskIODurationMilliSec
                = (report.getDiskReadDurationMilliSec() + report.getDiskWriteDurationMilliSec());
        if (totalDiskIODurationMilliSec == 0)
            return MapSampleReport.UNAVAILABLE;

        long bytesPerMillSec = (report.getDiskReadBytes() + report.getDiskWriteBytes()) / totalDiskIODurationMilliSec;

        return (long)(bytesPerMillSec/1.024);  //convert to kB/s
    }

    private long calculateTaskNetworkIORate(MapSampleReport report){
        if(report.getNetworkReadBytes()==MapSampleReport.UNAVAILABLE
                || report.getNetworkWriteBytes() == MapSampleReport.UNAVAILABLE
                || report.getNetworkReadDurationMilliSec() == MapSampleReport.UNAVAILABLE
                || report.getNetworkWriteDurationMilliSec() == MapSampleReport.UNAVAILABLE)
            return MapSampleReport.UNAVAILABLE;

        long totalNetworkIODurationMilliSec
                = (report.getNetworkReadDurationMilliSec() + report.getNetworkWriteDurationMilliSec());
        if (totalNetworkIODurationMilliSec == 0)
            return MapSampleReport.UNAVAILABLE;

        long bytesPerMillSec = (report.getNetworkReadBytes() + report.getNetworkWriteBytes()) / totalNetworkIODurationMilliSec;

        return (long)(bytesPerMillSec/1.024);  //convert to kB/s
    }

    public Boolean isNaiveIOIntensive(String jobId){
        MapSampleReport report = sampleReports.get(jobId);
        if(report == null || report.getDiskWriteDurationMilliSec()==MapSampleReport.UNAVAILABLE
                || report.getMapDurationMilliSec()==MapSampleReport.UNAVAILABLE)
            return null;

        long diskTime = report.getDiskReadDurationMilliSec() + report.getDiskWriteDurationMilliSec();
        long cpuTime = report.getMapDurationMilliSec()-diskTime;
        
        if(diskTime<0 || cpuTime<0)
            return null;

        return diskTime > cpuTime;
    }
}
