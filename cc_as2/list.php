<?php

require_once 'sdk.class.php';
require_once 'video.class.php';

$list = video::getVideoList();

foreach($list as $v){
    echo '<li><a href="view.php?id='.$v[0].'">'.$v[1].'</a></li>';
}

print_r($response);
?>
