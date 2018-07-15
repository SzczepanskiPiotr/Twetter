<?php
 
class TweetOperation
{
    private $con;
 
    function __construct()
    {
        require_once dirname(__FILE__) . '/DbConnect.php';
        $db = new DbConnect();
        $this->con = $db->connect();
    }
 
	//Adds new tweet
	function tweetAdd($user_id, $tweet_text, $tweet_picture){
		$stmt = $this->con->prepare("INSERT INTO tweets(user_id,tweet_text,tweet_picture) VALUES (?, ?, ?)";  // $usename=$_GET['username'];
		$stmt->bind_param("iss", $user_id, $tweet_text, $tweet_picture);
        if($stmt->execute())
			return true;
	}
}