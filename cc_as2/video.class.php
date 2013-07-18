<?php

require_once 'database.class.php';
/**
 * Description of video
 *
 * @author edwardlzk
 */
class video {
    
    public static function insertVideo($name,$desp, $loc, $pic){
        $db = Database::getInstance();
        
        
        $sql = "INSERT INTO video(vname,vdesp, vloc_s3, vpic_loc, vupload)
            VALUES('$name','$desp', '$loc', '$pic', now())";
        $db->query($sql);
    }
    
    public static function getVideoList(){
        $db = Database::getInstance();
        $sql = "SELECT vid, vname, vloc_s3, vpic_loc FROM video ORDER BY vupload";
        return $db->queryArray($sql);
    }
    
    public static function getVideoInfo($id){
        $db = Database::getInstance();
        $sql = "SELECT vname, vloc_s3, vpic_loc,vupload FROM video WHERE vid = $id";
        $db->query($sql);
        $db->singleRecord();
        return $db->Record;
    }
    
    public static function parseFileName($name)
    {
         $i = strrpos($name,".");
         if (!$i) { return ""; } 

         $name = substr($name,0,$i);
         return $name;
    }
}

?>
