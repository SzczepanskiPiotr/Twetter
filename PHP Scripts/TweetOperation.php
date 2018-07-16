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
 
	//Method to add new tweet
	function tweetAdd($userId, $tweet_text, $tweet_picture){
		$stmt = $this->con->prepare("INSERT INTO tweets(user_id,tweet_text,tweet_picture) VALUES (?, ?, ?)"); 
		$stmt->bind_param("iss", $userId, $tweet_text, $tweet_picture);
        if($stmt->execute())
			return true;
		return false;
	}
	
	//Method to show tweets
	function tweetList($userId, $startFrom, $query, $op){
		if($op == 1){//myFollowingSearch
			$stmt = $this->con->prepare("SELECT * FROM user_tweets WHERE user_id IN 
			(SELECT following_user_id FROM following WHERE user_id=?) OR user_id=? ORDER BY tweet_date DESC". 
			" LIMIT 20 OFFSET ?");
			$stmt->bind_param("iii", $userId, $userId, $startFrom);
		}
		else if($op == 2){//searchSpecificPersonPosts
			$stmt = $this->con->prepare("SELECT * FROM user_tweets WHERE user_id=? ORDER BY tweet_date DESC". 
			" LIMIT 20 OFFSET ?");
			$stmt->bind_param("ii", $userId, $startFrom);		
		}
		else if($op == 3){//searchByTweetText
			$stmt = $this->con->prepare("SELECT * FROM user_tweets WHERE tweet_text LIKE '%?%' LIMIT 20 OFFSET ?");	
			$stmt->bind_param("si", $query, $startFrom);		
		}
		if($stmt->execute()){
			$stmt->bind_result($tweet_id, $tweet_text, $tweet_picture, $tweet_date, $user_id, $username, $picture_path);
			$tweets = array();
			while($stmt->fetch()){
				$temp = array();
				$temp['tweet_id'] = $tweet_id;
				$temp['tweet_text'] = $tweet_text;
				$temp['tweet_picture'] = $tweet_picture;
				$temp['tweet_date'] = $tweet_date;
				$temp['user_id'] = $user_id;
				$temp['username'] = $username;
				$temp['picture_path'] = $picture_path;
				if (!$this->checkFavourite($user_id,$tweet_id)) 
					$temp['isFavourite'] = false;
				else 
					$temp['isFavourite'] = true;
				$temp['favouriteCount'] = countFavourites($tweet_id);

				array_push($tweets, $temp);
			}
			return $tweets;
        }	
		return false;
	}
	
	//Method to favourite tweet
	function favourite($userId, $tweetId, $op){
		if($op == 1)//favourite tweet
			$stmt = $this->con->prepare("INSTERT INTO favourited(user_id,tweet_id) VALUES (?, ?)");	
		else if($op == 2)//un-favourite tweet
			$stmt = $this->con->prepare("DELETE FROM favourited WHERE user_id=? AND tweet_id=?");
		$stmt->bind_param("ii", $userId, $tweetId);		
		if($stmt->execute())
			return true;
		return false;
	}
	
	//Method that checks if selected tweet is favourite by specific user
	function checkFavourite($userId, $tweetId){
		$stmt = $this->con->prepare("SELECT * FROM favourited WHERE user_id=? AND tweet_id=?");	
		$stmt->bind_param("ii", $userId, $tweetId);		
		        $stmt->execute();
        $stmt->store_result();
        return $stmt->num_rows > 0;
	}

	//Method to count favourites amount
	function countFavourites($tweetId){
		$stmt = $this->con->prepare("select count(tweet_id) from favourited where tweet_id=?"); 
		$stmt->bind_param("i", $tweetId);
        $stmt->execute();
		return $stms->fetch();
	}
	
}