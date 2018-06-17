<?php
 // 1- connect to db
require("DBInfo.php");
 
 // define quesry 
$query="select * from users where username= '". $_GET['username'] . "'";  // $usename=$_GET['username'];
$result=  mysqli_query($connect, $query);

if(! $result)
{ die("Error in query");}
 //get data from database
$output=array();

	while($row=  mysqli_fetch_assoc($result))
	{
		$output[]=$row;  //$row['id']
		break;
	}
	if ($output) 
	{
		$query1 = "select password from users where username= '". $_GET['username'] ."'";
		$result1 = mysqli_query($connect, $query1);
		$row1  = mysqli_fetch_row($result1);
		$hashedPassword1 = $row1[0];
		
		if (password_verify($_GET['password'],$hashedPassword1)) {
			print( "{'msg':'Pass Login'". ",'info':'". json_encode($output) ."'}");// this will print the output in json
		}else {
		print("{'msg':'invalid password'}");
		}
	}
	else{
		print("{'msg':' cannot login'}");
	}

// 4 clear
mysqli_free_result($result);
//5- close connection
mysqli_close($connect);
?>