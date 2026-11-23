<?php
// Router for PHP's built-in development server only. In production, Apache or
// Nginx sends these headers and serves only the files in public/.
$path  = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$files = ['/' => ['index.html', 'text/html'],
          '/index.html' => ['index.html', 'text/html'],
          '/app.js'     => ['app.js', 'text/javascript'],
          '/style.css'  => ['style.css', 'text/css']];
if ($path === '/api.php') {
    return false;                           // api.php sends its own headers
}
if (!isset($files[$path])) {                // anything else does not exist
    http_response_code(404);
    exit('Not found');
}
[$file, $type] = $files[$path];
header("Content-Type: $type; charset=utf-8");
header("Content-Security-Policy: default-src 'self'; script-src 'self'; "
     . "style-src 'self'; object-src 'none'; base-uri 'none'; "
     . "form-action 'self'; frame-ancestors 'none'");
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: no-referrer');
readfile(__DIR__ . '/public/' . $file);
