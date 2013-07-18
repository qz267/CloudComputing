package com.qz.PageRanking;  
  
  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.FSDataInputStream;  
import org.apache.hadoop.fs.FileStatus;  
import org.apache.hadoop.fs.FileSystem;  
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.Text;  
import org.apache.hadoop.mapreduce.Counters;  
import org.apache.hadoop.mapreduce.Job;  
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;  
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;  
  
  
public class Runner  
{  
    public static final int numNodes=5; 
    public static final int maxiter=10; 
    public static void main(String[] args) throws Exception  
    {  
        long count=0;
        int it=1;  
        int num=1;  
        String input="/Graph/input/";  
        String output="/Graph/output1";  
        do{  
            Job job=getPageRankJob(input, output);  
            job.waitForCompletion(true);  
              
            Counters counter = job.getCounters();  
            count = counter.findCounter(RankJob.MidNodes.Reduce).getValue();  
              
              
            input="/Graph/output"+it;  
            it++;  
            output="/Graph/output"+it;  
              
            Job job1=getDistrbuteJob(input,output);  
            job1.waitForCompletion(true);  
              
            input="/Graph/output"+it;  
            it++;  
            output="/Graph/output"+it;  
              
            if(num<maxiter)  
            System.out.println("it:"+it+" "+count);  
            num++;  
        }while(count!=numNodes);  
  
    }  
      
    public static Job getPageRankJob(String inPath,String outPath) throws Exception  
    {  
        Configuration conf = new Configuration();  
        Job job=new Job(conf,"PageRank job");  
          
        job.getConfiguration().setInt("NodeCount", numNodes);  
        job.getConfiguration().setBoolean("mapred.map.tasks.speculative.execution", false);  
        job.getConfiguration().setBoolean("mapred.reduce.tasks.speculative.execution", false);  
          
        job.getConfiguration().set("PageRankMassPath", "/mass");  
          
  
        job.setJarByClass(Runner.class);  
          
        job.setNumReduceTasks(5);  
  
        job.setMapperClass(RankJob.PageRankMaper.class);  
        job.setReducerClass(RankJob.PageRankJobReducer.class);  
        job.setPartitionerClass(RangePartitioner.class);  
          
        job.setMapOutputKeyClass(Text.class);  
        job.setMapOutputValueClass(Text.class);  
        job.setOutputKeyClass(Text.class);  
        job.setOutputValueClass(Text.class);  
          
          
        FileInputFormat.addInputPath(job, new Path(inPath));  
        FileOutputFormat.setOutputPath(job, new Path(outPath));  
          
        FileSystem.get(job.getConfiguration()).delete(new Path(outPath), true);//???????????????????????????  
          
        return job;   
    }  
  
    public static Job getDistrbuteJob(String inPath,String outPath) throws Exception  
    {  
        Configuration conf = new Configuration();  
        Job job=new Job(conf,"Ditribute job");  
          
        double mass = Double.NEGATIVE_INFINITY;                //???????????????dangling?????????PR?????????????????????????????????  
        FileSystem fs = FileSystem.get(conf);  
        for (FileStatus f : fs.listStatus(new Path("/mass/missMass")))  
        {  
          FSDataInputStream fin = fs.open(f.getPath());  
          mass = fin.readDouble();  
          fin.close();  
        }  
        job.getConfiguration().setFloat("MissingMass",(float)mass);  
        job.getConfiguration().setInt("NodeCount", numNodes);  
        job.getConfiguration().setInt("NodeCount", numNodes);  
        job.getConfiguration().setBoolean("mapred.map.tasks.speculative.execution", false);  
        job.getConfiguration().setBoolean("mapred.reduce.tasks.speculative.execution", false);  
          
        job.getConfiguration().set("PageRankMassPath", "/mass");  
          
  
        job.setJarByClass(Runner.class);  
          
        job.setNumReduceTasks(5);  
  
        job.setMapperClass(RankJob.PageRankMaper.class);  
        job.setReducerClass(RankJob.PageRankJobReducer.class);  
        job.setPartitionerClass(RangePartitioner.class);  
          
        job.setMapOutputKeyClass(Text.class);  
        job.setMapOutputValueClass(Text.class);  
        job.setOutputKeyClass(Text.class);  
        job.setOutputValueClass(Text.class);  
          
          
        FileInputFormat.addInputPath(job, new Path(inPath));  
        FileOutputFormat.setOutputPath(job, new Path(outPath));  
          
        FileSystem.get(job.getConfiguration()).delete(new Path(outPath), true);//???????????????????????????  
          
        return job;   
    }  
}  