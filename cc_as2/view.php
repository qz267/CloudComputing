<?php require_once 'video.class.php';

if(!isset( $_GET['id'])){
    header("Location:list.php");
}



$video_info = video::getVideoInfo((int)$_GET['id'])

?>
<body style="overflow: hidden">
<script type="text/javascript" src="/jwplayer/jwplayer.js" ></script>
<script type="text/javascript" src="/jwplayer/jwplayer.html5.js" ></script>
<div id='mediaplayer'>This text will be replaced</div>
</body>
<script type="text/javascript">
   jwplayer("mediaplayer").setup({
       file: "mp4:<?php echo video::parseFileName($video_info[1])?>",
        height: 270,
        modes: [{
                    type: "flash",
                    src: "/jwplayer/jwplayer.flash.swf"
              
        },{
            type: "html5",
            config: {
        file: "http://d1px79e7r2jd26.cloudfront.net/<?php echo $video_info[1]?>",
                provider: "video"
            }
        }],
        provider: "rtmp",
        streamer: "rtmp://s3tejfjjfrkydq.cloudfront.net/cfx/st",
        width: 480
    });
</script>