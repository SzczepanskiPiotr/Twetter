<?php
 
 
class UserOperation
{	
 
    function __construct()
    {
		require_once 'DbConnect.php';
		require_once 'Constants.php';
    }
 
    //Method to create a new user
    function registerUser($name, $email, $pass, $picture_path)
    {
        if (!$this->isUserExist($email)) {
            $password = md5($pass);
            $stmt = DB::prepare("INSERT INTO users (username, email, password, picture_path) VALUES (?, ?, ?, ?)");
            if ($stmt->execute([$name, $email, $password, $picture_path]))
                return USER_CREATED;
            return USER_CREATION_FAILED;
        }
        return USER_EXIST;
    }
 
    //Method for user login
    function userLogin($username, $pass)
    {
        $password = md5($pass);
        $stmt = DB::prepare("SELECT user_id FROM users WHERE username = ? AND password = ?");
        $stmt->execute([$username, $password]);
        //$stmt->store_result();
        return $stmt->fetch();
    }
 
 /*
    //Method to update profile of user
    function updateProfile($id, $name, $email, $pass, $picture_path)
    {
        $password = md5($pass);
        $stmt = $this->con->prepare("UPDATE users SET name = ?, email = ?, password = ?, picture_path = ? WHERE user_id = ?");
        $stmt->bind_param("ssssi", $name, $email, $password, $picture_path, $id);
        if ($stmt->execute())
            return true;
        return false;
    }
  */
  
	//Method to follow other user
	function followUser($userId, $followUserId, $op)
    {
		if($op == 1)//adding new follow
			$stmt = DB::prepare("INSERT INTO following(user_id,following_user_id) VALUES (?,?)");
		else if($op == 2)//remove existing follow
			$stmt = DB::prepare("DELETE FROM following WHERE user_id=? AND following_user_id=?"); 	
		$stmt->execute([$userId, $followUserId]);
		return $stmt->rowCount()>0;
    }
	
	//Method to check if user is following
	function checkFollowing($userId, $followUserId){
		$stmt = DB::prepare("SELECT * FROM following WHERE user_id=? AND following_user_id=?");
        $stmt->execute([$userId, $followUserId]);
		return $stmt->fetch();
	}
	
	//Method to get user by email
    function getUserByEmail($email)
    {
        $stmt = DB::prepare("SELECT user_id, username, email, picture_path FROM users WHERE email = ?");
        $stmt->execute([$email]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        return $user;
    }
	
	//Method to get user by email
    function getUserByUsername($username)
    {
        $stmt = DB::prepare("SELECT user_id, username, email, picture_path FROM users WHERE username = ?");
        $stmt->execute([$username]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        return $user;
    }
 
 /*
    //Method to get all users
    function getAllUsers(){
        $stmt = $this->con->prepare("SELECT user_id, username, email, picture_path FROM users");
        $stmt->execute();
        $stmt->bind_result($user_id, $username, $email, $picture_path);
        $users = array();
        while($stmt->fetch()){
            $temp = array();
            $temp['user_id'] = $user_id;
            $temp['username'] = $username;
            $temp['email'] = $email;
            $temp['picture_path'] = $picture_path;
            array_push($users, $temp);
        }
        return $users;
    }
*/
 
    //Method to check if email already exist
    function isUserExist($email)
    {
        $stmt = DB::prepare("SELECT user_id FROM users WHERE email =?");
        $stmt->execute([$email]);
        //$stmt->store_result();
        return $stmt->fetch();
    }
}