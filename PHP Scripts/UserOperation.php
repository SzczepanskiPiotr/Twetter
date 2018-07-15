<?php
 
class UserOperation
{
    private $con;
	
	define('USER_CREATED', 101);
	define('USER_EXIST', 102);
	define('USER_CREATION_FAILED', 103);
 
    function __construct()
    {
        require_once dirname(__FILE__) . '/DbConnect.php';
        $db = new DbConnect();
        $this->con = $db->connect();
    }
 
    //Method to create a new user
    function registerUser($name, $email, $pass, $picture_path)
    {
        if (!$this->isUserExist($email)) {
            $password = md5($pass);
            $stmt = $this->con->prepare("INSERT INTO users (username, email, password, picture_path) VALUES (?, ?, ?, ?)");
            $stmt->bind_param("ssss", $name, $email, $password, $picture_path);
            if ($stmt->execute())
                return USER_CREATED;
            return USER_CREATION_FAILED;
        }
        return USER_EXIST;
    }
 
    //Method for user login
    function userLogin($username, $pass)
    {
        $password = md5($pass);
        $stmt = $this->con->prepare("SELECT id FROM users WHERE username = ? AND password = ?");
        $stmt->bind_param("ss", $username, $password);
        $stmt->execute();
        $stmt->store_result();
        return $stmt->num_rows > 0;
    }
 
    //Method to update profile of user
    function updateProfile($id, $name, $email, $pass, $picture_path)
    {
        $password = md5($pass);
        $stmt = $this->con->prepare("UPDATE users SET name = ?, email = ?, password = ?, picture_path = ? WHERE id = ?");
        $stmt->bind_param("ssssi", $name, $email, $password, $picture_path, $id);
        if ($stmt->execute())
            return true;
        return false;
    }
 
    //Method to get user by email
    function getUserByEmail($email)
    {
        $stmt = $this->con->prepare("SELECT id, name, email, gender FROM users WHERE email = ?");
        $stmt->bind_param("s", $email);
        $stmt->execute();
        $stmt->bind_result($id, $name, $email, $gender);
        $stmt->fetch();
        $user = array();
        $user['id'] = $id;
        $user['name'] = $name;
        $user['email'] = $email;
        $user['picture_path'] = $picture_path;
        return $user;
    }
	
	    //Method to get user by email
    function getUserByUsername($username)
    {
        $stmt = $this->con->prepare("SELECT id, name, email, gender FROM users WHERE username = ?");
        $stmt->bind_param("s", $username);
        $stmt->execute();
        $stmt->bind_result($id, $name, $email, $picture_path);
        $stmt->fetch();
        $user = array();
        $user['id'] = $id;
        $user['name'] = $name;
        $user['email'] = $email;
        $user['picture_path'] = $picture_path;
        return $user;
    }
 
    //Method to get all users
    function getAllUsers(){
        $stmt = $this->con->prepare("SELECT id, name, email, picture_path FROM users");
        $stmt->execute();
        $stmt->bind_result($id, $name, $email, $picture_path);
        $users = array();
        while($stmt->fetch()){
            $temp = array();
            $temp['id'] = $id;
            $temp['name'] = $name;
            $temp['email'] = $email;
            $temp['picture_path'] = $picture_path;
            array_push($users, $temp);
        }
        return $users;
    }
 
    //Method to check if email already exist
    function isUserExist($email)
    {
        $stmt = $this->con->prepare("SELECT id FROM users WHERE email = ?");
        $stmt->bind_param("s", $email);
        $stmt->execute();
        $stmt->store_result();
        return $stmt->num_rows > 0;
    }
	
	//Method to follow other user
	function followUser($userId, $followUserId, $op)
    {
		if($op == 1)
			$stmt = $this->con->prepare("INSERT INTO following(user_id,following_user_id) VALUES (?,?)");
		else if($op == 2)
			$stmt = $this->con->prepare("DELETE FROM following WHERE user_id=? AND following_user_id=?"); 	
        $stmt->bind_param("ii", $userId, $followUserId);
        if($stmt->execute())
			return true;
		return false;
    }
}