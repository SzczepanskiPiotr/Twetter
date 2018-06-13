<?php
 // 1- connect to db
require("DBInfo.php");

 $query="select * from favourited where user_id=" . $_GET['user_id']. 
 " and tweet_id="  . $_GET['tweet_id'] ;  // $usename=$_GET['username'];
 
$result=  mysqli_query($connect, $query);
if(! $result)
{ die("Error in query");}
 //get data from database
$output=array();
while($row=  mysqli_fetch_assoc($result))
{
 $output[]=$row;  //$row['id']
}
 
$query1 = "select tweet_id, count(tweet_id) from favourited where tweet_id =". $_GET['tweet_id'];
$result1=  mysqli_query($connect, $query1);

if($result1)
{
    $count  = mysqli_fetch_assoc($result1);
}
 
 if ($output) {
print("{'msg':'is favourite'".",'info':'". json_encode($output) ."','count':'". json_encode($count) ."'}");

}
else {
print("{'msg':'is not favourite'".",'info':'". json_encode($output) ."','count':'". json_encode($count) ."'}");
}
 
//5- close connection
mysqli_close($connect);
?>