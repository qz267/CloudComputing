package com.qz.PageRanking;  

import java.io.IOException;  
  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.FSDataOutputStream;  
import org.apache.hadoop.fs.FileSystem;  
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.Text;  
import org.apache.hadoop.mapreduce.Mapper;  
import org.apache.hadoop.mapreduce.Reducer;  
  
public class RankJob  
{  
    public static final double d = 0.85;  
    private static final double nodecount = 10;  
    private static final double threshold=0.01;//Neighbor count 
      
    public static enum MidNodes  
    {  
            Map, Reduce  
    };  
  
    public static class PageRankMaper extends Mapper<Object, Text, Text, Text>  
    {  
        @Override  
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException  
        {  
            PageNode node = PageNode.InstanceFormString(value.toString());  
            node.setOldPR(node.getNewPR());  
            context.write(new Text(node.getId()), new Text(PageNode.toStringWithOutID(node)));  
  
            for (String str : node.getDestNodes())  
            {  
                String outPR = new Double(node.getNewPR() / (double)node.getNumDest()).toString();  
                context.write(new Text(str), new Text(outPR));  
            }  
        }  
    }  
  
    public static class PageRankJobReducer extends Reducer<Text, Text, Text, Text>  
    {  
        private double totalMass = Double.NEGATIVE_INFINITY; //template store PR values of all key  
        private double missMass=Double.NEGATIVE_INFINITY;  
  
        @Override  
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException  
        {  
            PageNode currentNode = new PageNode(key.toString());  
            double inPR = 0.0;  
  
            for (Text val : values)  
            {  
                String[] temp = val.toString().trim().split("\\s+");  
                if (temp.length == 1) // output PR value
                {  
                    inPR += Double.valueOf(temp[0]);  
                } else if (temp.length >= 4)  
                {//output neighbor info  
                    currentNode = PageNode.InstanceFormString(key.toString() + "\t" + val.toString());  
                } else if (temp.length == 3)  
                { 
                    context.getCounter("PageRankJobReducer", "errornode").increment(1);  
                    currentNode=PageNode.InstanceFormString(key.toString() + "\t" + val.toString());  
                }  
            }  
            if (currentNode.getNumDest()>=1)  
            {  
                double newPRofD = (1 - RankJob.d) /(double) RankJob.nodecount + RankJob.d * inPR;  
                currentNode.setNewPR(newPRofD);  
                context.write(new Text(currentNode.getId()), new Text(PageNode.toStringWithOutID(currentNode)));  
            }else if (currentNode.getNumDest()==0) {  
                  
                missMass=currentNode.getOldPR();
            }  
              
            totalMass += inPR;  
            double partPR=(currentNode.getNewPR()-currentNode.getOldPR())*(currentNode.getNewPR()-currentNode.getOldPR());  
            if (partPR<=threshold)  
            {  
                context.getCounter(MidNodes.Reduce).increment(1);  
            }  
        }  
  
        @Override  
        public void cleanup(Context context) throws IOException, InterruptedException  
        {  
            Configuration conf = context.getConfiguration();  
            String taskId = conf.get("mapred.task.id");  
            String path = conf.get("PageRankingPath");// set the path
              
            if (missMass==Double.NEGATIVE_INFINITY)  
            {  
                return;  
            }  
            FileSystem fs = FileSystem.get(context.getConfiguration());  
            FSDataOutputStream out = fs.create(new Path(path + "/"+"missMass"), false);  
            out.writeDouble(missMass);  
            out.close();  
        }  
    }  
}  
