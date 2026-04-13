<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

include_once '../config/db.php';
include_once '../libs/jwt.php';

// Note: In a real project, use Composer to install openai-php/client
// For this script, we'll use a direct cURL call for simplicity.

$openai_api_key = getenv('OPENAI_API_KEY') ?: ''; // Set via environment variable, never hardcode!

$database = new Database();
$db = $database->getConnection();

$headers = getallheaders();
$jwt = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;
$user = JWT::decode($jwt);

if (!$user) {
    http_response_code(401);
    echo json_encode(["message" => "Unauthorized"]);
    exit();
}

// 1. Get user order history (last 5 items)
$query = "SELECT p.name FROM order_details od JOIN orders o ON od.order_id = o.id JOIN products p ON od.product_id = p.id WHERE o.user_id = ? ORDER BY o.created_at DESC LIMIT 5";
$stmt = $db->prepare($query);
$stmt->execute([$user->id]);
$history = $stmt->fetchAll(PDO::FETCH_COLUMN);
$history_str = implode(", ", $history);

// 2. Mock Weather (In real app, call OpenWeatherMap)
$weather = "Sunny, 30°C";
$time_of_day = date("H:i");

// 3. Prepare Prompt
$prompt = "User has ordered: $history_str. Current weather is $weather and time is $time_of_day.
Recommend 3 dishes from a Vietnamese restaurant and give a short reason for each. Return only JSON format: [{\"name\": \"...\", \"reason\": \"...\"}]";

// 4. Call OpenAI API
$ch = curl_init('https://api.openai.com/v1/chat/completions');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode([
    'model' => 'gpt-3.5-turbo',
    'messages' => [['role' => 'user', 'content' => $prompt]],
    'temperature' => 0.7
]));
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Authorization: Bearer ' . $openai_api_key
]);

$response = curl_exec($ch);
curl_close($ch);

$result = json_decode($response);
if (isset($result->choices[0]->message->content)) {
    echo $result->choices[0]->message->content;
} else {
    echo json_encode(["message" => "Could not get recommendation."]);
}
?>
