package testoftwitterapi;

import java.io.BufferedReader;  
import java.io.InputStreamReader;  
import java.net.URL;  
//import org.json.JSONArray;  
import JSON.*; 
  
/** 
 * 
 * @author Fareez Ahamed 
 * @email kn_fareez@ymail.com 
 */  
public class Main {  
  
    /** 
     * @param args the command line arguments 
     */  
    public static void main(String[] args) throws Exception{  
        BufferedReader in = new BufferedReader(  
                new InputStreamReader(System.in));  
        String urlstr = "http://search.twitter.com/search.json?q=";  
        StringBuffer buff = new StringBuffer();  
  
        System.out.print("Search for : ");  
        urlstr += in.readLine();  
  
        URL url = new URL(urlstr);  
        BufferedReader br = new BufferedReader(  
                                    new InputStreamReader(  
                                    url.openConnection().getInputStream()));  
        int c;  
        while((c=br.read())!=-1)  
        {  
            buff.append((char)c);  
        }  
        br.close();  
  
        JSONObject js = new JSONObject(buff.toString());  
        JSONArray tweets = js.getJSONArray("results");  
        JSONObject tweet;  
        for(int i=0;i<tweets.length();i++) {  
            tweet = tweets.getJSONObject(i);  
            System.out.println((i+1)+") "+tweet.getString("from_user")+" at "+tweet.getString("created_at"));  
            System.out.println(tweets.getJSONObject(i).getString("text")+"\n");  
        }  
    }  
}  