package org.kahina.core.profiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class KahinaLogger
{
    //a stack of starting times is stored for each Thread
    private Map<String,List<Long>> timeStacks;
    
    //cached writers for the log files generated by each thread
    private Map<String, FileWriter> fileStreams;
    private List<String> openFileStreams;
    private final int FILESTREAM_CACHE_SIZE = 20;
    
    private boolean loggingEnabled = false;
    
    private String logDir;
    
    public KahinaLogger(String logDir)
    {
        timeStacks = new HashMap<String,List<Long>>();
        fileStreams = new HashMap<String,FileWriter>();
        openFileStreams = new LinkedList<String>();
        this.logDir = logDir;
        File dir = new File(logDir);
        if (!dir.exists())
        {
            if (!dir.mkdirs())
            {
                System.err.println("WARNING: KahinaLogger failed to create directory " + logDir);
                System.err.println("         Logging had to be disabled!");
                disableLogging();
            }
        }
        else
        {
            //clear logging files in log directory     
            String[] logFiles;      
            if(dir.isDirectory())
            {  
                logFiles = dir.list();  
                for (int i = 0; i < logFiles.length; i++) 
                {  
                    File logFile = new File(dir, logFiles[i]);
                    if (logFile.getName().endsWith(".log"))
                    {
                        logFile.delete();  
                    }
                }  
             }  
        }
    }
    
    public synchronized void startMeasuring()
    {
        if (loggingEnabled)
        {
            String threadID = Thread.currentThread().getName();
            pushToStack(threadID,System.currentTimeMillis());
        }
    }
    
    public synchronized void endMeasuring(String message)
    {
        if (loggingEnabled)
        {
            long time = System.currentTimeMillis();
            String threadID = Thread.currentThread().getName();
            long startTime = popFromStack(threadID);
            int stackDepth = stackDepth(threadID);
            try
            {
                FileWriter out = getFileStream(threadID);
                if (out != null)
                {
                    for (int i = 0; i < stackDepth; i++)
                    {
                        out.append(' ');
                        out.append(' ');
                    }
                    out.append((time-startTime) + " ms " + message + "\n");
                    out.flush();
                }
            }
            catch (IOException e)
            {
                System.err.println("WARNING: IOException in KahinaLogger during file output!");
            }
        }
    }
    
    public synchronized FileWriter getFileStream(String threadID)
    {
        FileWriter newOut = fileStreams.get(threadID);
        if (newOut == null)
        {
            String logFileName = logDir + "/" + threadID + ".log";
            try
            {
                if (openFileStreams.size() >= FILESTREAM_CACHE_SIZE)
                {
                    String closedThreadID = openFileStreams.remove(0);
                    fileStreams.get(closedThreadID).close();
                    fileStreams.remove(closedThreadID);
                }
                newOut = new FileWriter(new File(logFileName), true);
                fileStreams.put(threadID, newOut);
                openFileStreams.add(threadID);
            }
            catch (IOException e)
            {
                System.err.println("WARNING: IOException in KahinaLogger while creating " + logFileName);
                System.err.println("         Logging had to be disabled!");
                disableLogging();
            }
        }
        else
        {
            //bring file stream to the end of the cache, it was recently used
            openFileStreams.remove(threadID);
            openFileStreams.add(threadID);
        }
        return newOut;
    }
    
    public void enableLogging()
    {
        loggingEnabled = true;
        timeStacks = new HashMap<String,List<Long>>();
    }
    
    public void disableLogging()
    {
        loggingEnabled = false;
        timeStacks = null;
    }
    
    private void pushToStack(String threadID, long startTime)
    {
        List<Long> timeStack = timeStacks.get(threadID);
        if (timeStack == null)
        {
            timeStack = new LinkedList<Long>();
            timeStacks.put(threadID, timeStack);
        }
        timeStack.add(0,startTime);
    }
    
    private long popFromStack(String threadID)
    {
        List<Long> timeStack = timeStacks.get(threadID);
        if (timeStack == null) return System.currentTimeMillis();
        return timeStack.remove(0);
    }
    
    private int stackDepth(String threadID)
    {
        List<Long> timeStack = timeStacks.get(threadID);
        if (timeStack == null) return 0;
        return timeStack.size();
    }
}
