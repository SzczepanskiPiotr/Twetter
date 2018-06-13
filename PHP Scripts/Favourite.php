<?php
 // 1- connect to db
require("DBInfo.php");

 // define quesry 
if($_GET['op']==1) //op=1 add, op=2 delete
// add folllowing
{$query="insert into favourited(user_id,tweet_id) values (" . $_GET['user_id']. ","  . $_GET['tweet_id'] . ")";  // $usename=$_GET['username'];
}
else{ // remove following
$query="delete from favourited where user_id=" . $_GET['user_id']. " and tweet_id="  . $_GET['tweet_id']; 	
}
$result=  mysqli_query($connect, $query);

$query1 = "select count(tweet_id) from favourited where tweet_id =". $_GET['tweet_id'];
$result1=  mysqli_query($connect, $query1);

if($result1)
{
    $count  = mysqli_fetch_assoc($result1);
}

if(! $result)
{$output ="{'msg':'fail'}";
}
else {

$output ="{'msg':'favourite is updated'".",'info':'". json_encode($count) ."'}";
}
 
print( $output);// this will print the output in json
 
//5- close connection
mysqli_close($connect);
?>