<?php
 // 1- connect to db
require("DBInfo.php");
 // define quesry 
$hashedPassword = password_hash($_GET['password'], PASSWORD_DEFAULT);
$query="insert into users(username,email,password,picture_path) values ('" . $_GET['username']. "','"  . $_GET['email'] . "','"  .$hashedPassword . "','"  . $_GET['picture_path'] . "')";  // $usename=$_GET['username'];
$result=  mysqli_query($connect, $query);
if(! $result)
{$output ="{'msg':'fail'}";
}
else {
$output ="{'msg':'user is added'}";
}
 
print( $output);// this will print the output in json
 
//5- close connection
mysqli_close($connect);
?>