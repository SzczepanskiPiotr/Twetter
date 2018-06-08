<?php
 // 1- connect to db
$host="localhost";
$user="id5553486_sdp_db_admin";
$password="tw33t666r";
$database="id5553486_sdp_db";
$connect=  mysqli_connect($host, $user, $password, $database);
if(mysqli_connect_errno())
{ die("cannot connect to database field:". mysqli_connect_error());   }
?>