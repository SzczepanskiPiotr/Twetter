<?php
 
class DbConnect
{
    //Variable to store database link
    private $con;
 
    //Class constructor
    function __construct()
    {
 
    }
 
    //This method will connect to the database
    function connect()
    {
		
		$host="localhost";
		$user="id5553486_sdp_db_admin";
		$password="tw33t666r";
		$database="id5553486_sdp_db";
		
        //Including the constants.php file to get the database constants 
		include_once dirname(__FILE__) . '/Constants.php';
        //connecting to mysql database
        $this->con = new mysqli($host, $user, $password, $database);
 
        //Checking if any error occured while connecting
        if (mysqli_connect_errno()) {
            echo "Failed to connect to MySQL: " . mysqli_connect_error();
            return null;
        }
 
        //finally returning the connection link
        return $this->con;
    }
 
}