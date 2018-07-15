<?php

use \Psr\Http\Message\ServerRequestInterface as Request;
use \Psr\Http\Message\ResponseInterface as Response;

require '../vendor/autoload.php';
require_once '../includes/UserOperation.php';
require_once '../includes/TweetOperation.php';

//Creating a new app with the config to show errors
$app = new \Slim\App([
    'settings' => [
        'displayErrorDetails' => true
    ]
]);

//---UserOperation.php---//

//registering a new user
$app->post('/register', function (Request $request, Response $response) {
    if (isTheseParametersAvailable(array('name', 'email', 'password', 'picture_path'))) {
        $requestData = $request->getParsedBody();
        $name = $requestData['name'];
        $email = $requestData['email'];
        $password = $requestData['password'];
        $picture_path = $requestData['picture_path'];
        $db = new DbOperation();
        $responseData = array();

        $result = $db->registerUser($name, $email, $password, $picture_path);

        if ($result == USER_CREATED) {
            $responseData['error'] = false;
            $responseData['message'] = 'Registered successfully';
            $responseData['user'] = $db->getUserByEmail($email);
        } elseif ($result == USER_CREATION_FAILED) {
            $responseData['error'] = true;
            $responseData['message'] = 'Some error occurred';
        } elseif ($result == USER_EXIST) {
            $responseData['error'] = true;
            $responseData['message'] = 'This email already exist, please login';
        }

        $response->getBody()->write(json_encode($responseData));
    }
});

//user login route
$app->post('/login', function (Request $request, Response $response) {
    if (isTheseParametersAvailable(array('username', 'password'))) {
        $requestData = $request->getParsedBody();
        $username = $requestData['username'];
        $password = $requestData['password'];

        $db = new DbOperation();

        $responseData = array();

        if ($db->userLogin($username, $password)) {
            $responseData['error'] = false;
            $responseData['user'] = $db->getUserByUsername($username);
        } else {
            $responseData['error'] = true;
            $responseData['message'] = 'Invalid username or password';
        }

        $response->getBody()->write(json_encode($responseData));
    }
});

//getting all users
$app->get('/users', function (Request $request, Response $response) {
    $db = new DbOperation();
    $users = $db->getAllUsers();
    $response->getBody()->write(json_encode(array("users" => $users)));
});

//updating a user
$app->post('/update/{id}', function (Request $request, Response $response) {
    if (isTheseParametersAvailable(array('name', 'email', 'password', 'picture_path'))) {
        $id = $request->getAttribute('id');

        $requestData = $request->getParsedBody();

        $name = $requestData['name'];
        $email = $requestData['email'];
        $password = $requestData['password'];
        $picture_path = $requestData['picture_path'];


        $db = new DbOperation();

        $responseData = array();

        if ($db->updateProfile($id, $name, $email, $password, $picture_path)) {
            $responseData['error'] = false;
            $responseData['message'] = 'Updated successfully';
            $responseData['user'] = $db->getUserByEmail($email);
        } else {
            $responseData['error'] = true;
            $responseData['message'] = 'Not updated';
        }

        $response->getBody()->write(json_encode($responseData));
    }
});

//sending message to user
$app->post('/followuser', function (Request $request, Response $response) {
    if (isTheseParametersAvailable(array('userId', 'followUserId', 'op'))) {
        $requestData = $request->getParsedBody();
        $userId = $requestData['userId'];
        $followUserId = $requestData['followUserId'];
        $op = $requestData['op'];

        $db = new DbOperation();

        $responseData = array();

        if ($db->followuser($userId, $followUserId, $op)) {
            $responseData['error'] = false;
            $responseData['message'] = 'Following is updated.';
        } else {
            $responseData['error'] = true;
            $responseData['message'] = 'Following coult not be updated.';
        }

        $response->getBody()->write(json_encode($responseData));
    }
});

//---TweetOperation.php---//

//registering a new user
$app->post('/tweetadd', function (Request $request, Response $response) {
    if (isTheseParametersAvailable(array('user_id', 'tweet_text', 'tweet_picture'))) {
        $requestData = $request->getParsedBody();
        $user_id = $requestData['user_id'];
        $tweet_text = $requestData['tweet_text'];
        $tweet_picture = $requestData['tweet_picture'];
        $db = new DbOperation();
        $responseData = array();

        if ($db->tweetAdd($user_id, $tweet_text, $tweet_picture)) {
            $responseData['error'] = false;
            $responseData['message'] = 'Tweet is added.';		
		} else {
			$responseData['error'] = true;
            $responseData['message'] = 'Tweet could not be added.';
		}

        $response->getBody()->write(json_encode($responseData));
    }
});

//function to check parameters
function isTheseParametersAvailable($required_fields)
{
    $error = false;
    $error_fields = "";
    $request_params = $_REQUEST;

    foreach ($required_fields as $field) {
        if (!isset($request_params[$field]) || strlen(trim($request_params[$field])) <= 0) {
            $error = true;
            $error_fields .= $field . ', ';
        }
    }

    if ($error) {
        $response = array();
        $response["error"] = true;
        $response["message"] = 'Required field(s) ' . substr($error_fields, 0, -2) . ' is missing or empty';
        echo json_encode($response);
        return false;
    }
    return true;
}


$app->run();