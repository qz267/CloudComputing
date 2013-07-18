package com.qz.PageRanking;  
  
import org.apache.hadoop.conf.Configurable;  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.io.*;  
import org.apache.hadoop.mapreduce.Partitioner;  
  
public class RangePartitioner extends Partitioner<Text, Text> implements Configurable  
{  
  
    private int nodeCnt = 0;  
    private Configuration conf;  
  
    public RangePartitioner() {}  
    @Override  
    public Configuration getConf()  
    {  
        return conf;  
    }  
  
    @Override  
    public void setConf(Configuration arg0)  
    {  
        this.conf = arg0;  
        configure();  
    }  
  
    @Override  
    public int getPartition(Text arg0, Text arg1, int arg2)  
    {  
        return (int) ((float)(Integer.parseInt(arg0.toString()) / (float) nodeCnt) * arg2) % arg2;  
    }  
    private void configure()   //?????????????????????  
    {  
        nodeCnt = conf.getInt("NodeCount", 0);  
    }  
  
}  