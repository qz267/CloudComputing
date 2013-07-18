import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import JSON.*;


public class as3 {
	
	
	/*************************************************************************************
	Making a call to the twitter API with requested query:
	*************************************************************************************/

	String query = req.getParameter("firstname");
	query = query.replace(" ","%20");

	URL Twitter = new URL("http://search.twitter.com/search.json?q="+query);
	URLConnection yc = Twitter.openConnection();
	BufferedReader in = new BufferedReader(
	new InputStreamReader(yc.getInputStream()));
	String inputLine;
	StringBuffer output=new StringBuffer("");
	while ((inputLine = in.readLine()) != null)
	output.append((inputLine));
	in.close();
	String json = output.toString();

	/*************************************************************************************
	Parsing the JSON response from twitter:
	(If you’re not able to find library files, unzip the attached JSONParsing file and add it to your code.)
	*************************************************************************************/

	JSONObject myjson = new JSONObject(json);
	JSONArray the_json_array = myjson.getJSONArray("results");


	int size = the_json_array.length();
	ArrayList<JSONObject> arrays = new ArrayList<JSONObject>();

	for (int i = 0; i < size; i++)
	{
	JSONObject another_json_object = the_json_array
	.getJSONObject(i);
	tweet_id = another_json_object.get("id_str").toString();
	text = another_json_object.get("text").toString();

	from_user = another_json_object.get("from_user").toString();
	from_user_name = another_json_object.get("from_user_name").toString();
	created_at = another_json_object.get("created_at").toString();
	profile_image_url = another_json_object.get("profile_image_url").toString();

	tb = new TwitterBean(tweet_id, text, from_user,from_user_name, created_at, profile_image_url);
	listOfTweets.add(tb);
	
	
	public static String analysis(List<TwitterBean>tws, String key, String searchKey){
		if (tws.size()=0){
			
		}else{
			
		}
	}

}
