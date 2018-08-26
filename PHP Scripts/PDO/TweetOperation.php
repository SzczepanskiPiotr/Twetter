<?php
 
 
class TweetOperation
{

    function __construct()
    {
		require_once 'DbConnect.php';
		require_once 'Constants.php';
    }
 
	//Method to add new tweet
	function tweetAdd($userId, $tweet_text, $tweet_picture){
		$stmt = DB::prepare("INSERT INTO tweets(user_id,tweet_text,tweet_picture) VALUES (?, ?, ?)"); 
        $stmt->execute([$userId, $tweet_text, $tweet_picture]);
		return $stmt->rowCount()>0;
	}
	
	//Method to show tweets
	function tweetList($userId, $startFrom, $query, $op, $check_user_id){
		if($op == 1){//myFollowingSearch
		
			/*$inStmt = DB::prepare("SELECT following_user_id FROM following WHERE user_id=?");
			$inStmt->execute([$userId]);
			$arr = ($inStmt->fetchAll(PDO::FETCH_NUM));
			$array = array();
			foreach ($arr as $element ) {
				$array = array_merge($array, $element);
			}
			$in  = str_repeat('?,', count($array)) . '?';

			$params = array_merge($array,[$userId]);
			$sql = "SELECT * FROM user_tweets WHERE user_id IN ($in) ORDER BY tweet_date DESC LIMIT 20 OFFSET ?";
			$stmt = DB::prepare($sql);	
			for ($x = 0; $x <= count($params) - 1; $x++) {
				$param = $params[$x];
				echo " ID:" .($x+1) . " PARAM:". $param;
				$stmt->bindParam(($x+1),$param);
			} 
			$param = (int)$startFrom;
			echo " ID:" .(count($params)+1) . " PARAM:". $param;
			$stmt->bindParam((count($params)+1),$param,PDO::PARAM_INT);
			$stmt->execute();
			return $sql;
			*/
			$stmt = DB::prepare("SELECT t.* FROM following f RIGHT JOIN user_tweets t ON t.user_id = f.following_user_id WHERE f.user_id=? OR t.user_id=? ORDER  BY t.tweet_date DESC LIMIT 20 OFFSET ?");
			$stmt->bindParam(1,$userId);
			$stmt->bindParam(2,$userId);
			$startFrom = (int)$startFrom;
			$stmt->bindParam(3,$startFrom,PDO::PARAM_INT);
			$stmt->execute();
			$tweets = $stmt->fetchAll(PDO::FETCH_ASSOC);
			//echo $tweets[0]['tweet_text'];
			//echo "COUNT" .$tweets[1]['username'];
			//echo "\n" .count($tweets);
			for($x = 0; $x <= count($tweets) - 1; $x++){
				
				$localUserId = $tweets[$x]['user_id'] ;
				$localTweetId = $tweets[$x]['tweet_id'];
				//echo "NEW:" .$localUserId, $localTweetId;
				if($this->checkFavourite($userId,$localTweetId)){
					$temp = array('isFavourite' => 'true');
					//echo $temp['isFavourite'];
					$tweets[$x] = array_merge($tweets[$x],$temp);
				}else{
					$temp = array('isFavourite' => 'false');
					//echo $temp['isFavourite'];
					$tweets[$x] = array_merge($tweets[$x],$temp);
				}
				
				//echo "\n" .$x;
			}
			return $tweets;			
		}
		else if($op == 2){//searchSpecificPersonPosts
			$stmt = DB::prepare("SELECT * FROM user_tweets WHERE user_id=? ORDER BY tweet_date DESC". 
			" LIMIT 20 OFFSET ?");
			$stmt->bindParam(1,$check_user_id);
			$startFrom = (int)$startFrom;
			$stmt->bindParam(2,$startFrom,PDO::PARAM_INT);
			$stmt->execute();	
			$tweets = $stmt->fetchAll(PDO::FETCH_ASSOC);
			//echo $tweets[0]['tweet_text'];
			//echo "COUNT" .$tweets[1]['username'];
			//echo "\n" .count($tweets);
			for($x = 0; $x <= count($tweets) - 1; $x++){
				
				$localUserId = $tweets[$x]['user_id'] ;
				$localTweetId = $tweets[$x]['tweet_id'];
				//echo "NEW:" .$localUserId, $localTweetId;
				if($this->checkFavourite($userId,$localTweetId)){
					$temp = array('isFavourite' => 'true');
					//echo $temp['isFavourite'];
					$tweets[$x] = array_merge($tweets[$x],$temp);
				}else{
					$temp = array('isFavourite' => 'false');
					//echo $temp['isFavourite'];
					$tweets[$x] = array_merge($tweets[$x],$temp);
				}
				
				//echo "\n" .$x;
			}
			return $tweets;		
		}
		else if($op == 3){//searchByTweetText
			$stmt = DB::prepare("SELECT * FROM user_tweets WHERE tweet_text LIKE ? LIMIT 20 OFFSET ?");	
			$search = "%$query%";
			$stmt->bindParam(1,$search);
			$startFrom = (int)$startFrom;
			$stmt->bindParam(2,$startFrom,PDO::PARAM_INT);
			$stmt->execute();	
			$tweets = $stmt->fetchAll(PDO::FETCH_ASSOC);
			//echo $tweets[0]['tweet_text'];
			//echo "COUNT" .$tweets[1]['username'];
			//echo "\n" .count($tweets);
			for($x = 0; $x <= count($tweets) - 1; $x++){
				
				$localUserId = $tweets[$x]['user_id'] ;
				$localTweetId = $tweets[$x]['tweet_id'];
				//echo "NEW:" .$localUserId, $localTweetId;
				if($this->checkFavourite($userId,$localTweetId)){
					$temp = array('isFavourite' => 'true');
					//echo $temp['isFavourite'];
					$tweets[$x] = array_merge($tweets[$x],$temp);
				}else{
					$temp = array('isFavourite' => 'false');
					//echo $temp['isFavourite'];
					$tweets[$x] = array_merge($tweets[$x],$temp);
				}
				
				//echo "\n" .$x;
			}
			return $tweets;		
		}
	}
	
	//Method to favourite tweet
	function favourite($userId, $tweetId){
		
		$checkStmt = DB::prepare("SELECT * FROM `favourited` WHERE user_id=? AND tweet_id=?");
		$checkStmt->execute([$userId,$tweetId]);
		if($checkStmt->rowCount()>0){
			$stmt = DB::prepare("DELETE FROM favourited WHERE user_id=? AND tweet_id=?");
			$stmt1 = DB::prepare("UPDATE tweets SET favoruiteCount = favoruiteCount - 1 WHERE tweet_id =?");		
		}else{
			$stmt = DB::prepare("INSERT INTO favourited(user_id,tweet_id) VALUES (?, ?)");
			$stmt1 = DB::prepare("UPDATE tweets SET favoruiteCount = favoruiteCount + 1 WHERE tweet_id =?");
		}	
		$stmt->execute([$userId, $tweetId]);
		$stmt1->execute([$tweetId]);
		return $checkStmt->rowCount()>0;
	}
	
	//Method that checks if selected tweet is favourite by specific user
	function checkFavourite($userId, $tweetId){
		$stmt = DB::prepare("SELECT user_id FROM favourited WHERE user_id=? AND tweet_id=?");	
		$stmt->execute([$userId, $tweetId]);
		return $stmt->fetch();
	}

	//Method to count favourites amount
	function countFavourites($tweetId){
		$stmt = $this->con->prepare("select count(tweet_id) from favourited where tweet_id=?"); 
		$stmt->bind_param("i", $tweetId);
        $stmt->execute();
		return $stmt->rowCount()>0;
	}
	
}