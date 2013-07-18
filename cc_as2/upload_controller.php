<?php

require_once 'sdk.class.php';
require_once 'video.class.php';

$s3 = new AmazonS3();
$bucket = "youtube_assignment";

if ($_FILES["file"]["error"] > 0)
  {
  echo "Error: " . $_FILES["file"]["error"] . "<br>";
  }
else
{
  echo "Upload: " . $_FILES["file"]["name"] . "<br>";
  echo "Type: " . $_FILES["file"]["type"] . "<br>";
  echo "Size: " . ($_FILES["file"]["size"] / 1024) . " kB<br>";
  echo "Stored in: " . $_FILES["file"]["tmp_name"]."<br>";
  
  $video_name = $_POST['vname'];
  $video_desp = $_POST['vdesp'];
  $new = "/var/www/html/uploads/" .$_FILES["file"]["name"] ;
  
  
  if(move_uploaded_file($_FILES["file"]["tmp_name"], $new)){
      $policy_video = array(
      'fileUpload' => $new,
      'acl' => AmazonS3::ACL_PUBLIC,
      'storage' => AmazonS3::STORAGE_REDUCED,
      );
      $thumbnail=getThumbnail($new);
      //Upload Video
      $s3->create_object($bucket, $_FILES["file"]["name"], $policy_video);
      
      $policy_pic = array(
      'fileUpload' => '/var/www/html/pics/'.$thumbnail,
      'acl' => AmazonS3::ACL_PUBLIC,
      'storage' => AmazonS3::STORAGE_REDUCED,
      );
      //upload thumbnail
      $s3->create_object($bucket, 'pics/'.$thumbnail, $policy_pic);
      video::insertVideo($video_name, $video_desp, $_FILES["file"]["name"], $thumbnail);
  }
  
  
}


function getThumbnail($file){
    $cmd = "/home/ec2-user/ffmpeg/ffmpeg";
    $name = time().".jpg";
    $cmd.= ' -i '.$file.' -vcodec mjpeg -vframes 1 -an -f rawvideo -s 260x210 /var/www/html/pics/'.$name;
    echo $cmd;
    exec($cmd);
    
    return $name;
}

function getExtension($str) 
{
         $i = strrpos($str,".");
         if (!$i) { return ""; } 

         $l = strlen($str) - $i;
         $ext = substr($str,$i+1,$l);
         return $ext;
}



?>
