<?php
class JWT {
    private static $key = "your_secret_key_restaurant_123";

    public static function encode($payload) {
        $header = json_encode(['typ' => 'JWT', 'alg' => 'HS256']);
        $base64UrlHeader = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($header));
        $base64UrlPayload = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode(json_encode($payload)));
        $signature = hash_hmac('sha256', $base64UrlHeader . "." . $base64UrlPayload, self::$key, true);
        $base64UrlSignature = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($signature));
        return $base64UrlHeader . "." . $base64UrlPayload . "." . $base64UrlSignature;
    }

    public static function decode($jwt) {
        $tks = explode('.', $jwt);
        if (count($tks) != 3) return null;
        list($headb64, $bodyb64, $cryptob64) = $tks;
        $header = json_decode(base64_decode($headb64));
        $payload = json_decode(base64_decode($bodyb64));
        $sig = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode(hash_hmac('sha256', $headb64 . "." . $bodyb64, self::$key, true)));
        if ($sig !== $cryptob64) return null;
        return $payload;
    }
}
?>
