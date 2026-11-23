<?php
declare(strict_types=1);
require __DIR__ . '/../src/db.php';

// ---- Session and response hardening ----
session_set_cookie_params([
    'httponly' => true,                       // JavaScript cannot read it
    'secure'   => !empty($_SERVER['HTTPS']),  // HTTPS-only in production
    'samesite' => 'Strict',                   // not sent on cross-site requests
]);
session_start();
header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header("Content-Security-Policy: default-src 'none'; frame-ancestors 'none'");
header('Cache-Control: no-store');

function respond(int $status, array $body): never
{
    http_response_code($status);
    echo json_encode($body, JSON_HEX_TAG | JSON_HEX_AMP | JSON_HEX_APOS
                          | JSON_HEX_QUOT | JSON_THROW_ON_ERROR);
    exit;
}

// ---- Server-side validation: length first, then an allow-list ----
function clean_customer(array $dirty): array
{
    $limits = ['full_name' => 100, 'email' => 254, 'city' => 60];
    $clean  = [];
    foreach ($limits as $field => $max) {
        $value = $dirty[$field] ?? '';
        if (!is_string($value) || mb_strlen($value) > $max) {
            respond(422, ['message' => 'Please check the form and try again.']);
        }
        $clean[$field] = trim($value);
    }
    $ok = preg_match("/^\p{L}[\p{L} .'-]{0,99}$/u", $clean['full_name'])
       && filter_var($clean['email'], FILTER_VALIDATE_EMAIL) !== false
       && preg_match("/^[A-Za-z][A-Za-z .'-]{0,59}$/", $clean['city']);
    if (!$ok) {
        respond(422, ['message' => 'Please check the form and try again.']);
    }
    return $clean;
}

try {
    $action = $_GET['action'] ?? '';

    if ($action === 'csrf' && $_SERVER['REQUEST_METHOD'] === 'GET') {
        $_SESSION['csrf'] ??= bin2hex(random_bytes(32));
        respond(200, ['token' => $_SESSION['csrf']]);
    }

    if ($action === 'add' && $_SERVER['REQUEST_METHOD'] === 'POST') {
        $sent = $_SERVER['HTTP_X_CSRF_TOKEN'] ?? '';
        if (empty($_SESSION['csrf']) || !hash_equals($_SESSION['csrf'], $sent)) {
            respond(403, ['message' => 'The request could not be verified.']);
        }
        $dirty = json_decode(file_get_contents('php://input'), true);
        if (!is_array($dirty)) {
            respond(400, ['message' => 'Invalid request.']);
        }
        $c = clean_customer($dirty);
        $stmt = db()->prepare(
            'INSERT INTO customers (full_name, email, city)
             VALUES (:full_name, :email, :city) RETURNING customer_id');
        $stmt->execute([':full_name' => $c['full_name'],
                        ':email'     => $c['email'],
                        ':city'      => $c['city']]);
        respond(201, ['message' => 'Customer added.']);
    }

    if ($action === 'search' && $_SERVER['REQUEST_METHOD'] === 'GET') {
        $q = $_GET['q'] ?? '';
        if (!is_string($q) || mb_strlen($q) > 50) {
            respond(422, ['message' => 'Search text is too long.']);
        }
        $pattern = '%' . addcslashes(trim($q), '%_\\') . '%';  // literal % and _
        $stmt = db()->prepare(
            "SELECT full_name, city, to_char(created_at, 'YYYY-MM-DD') AS created_at
               FROM customers
              WHERE full_name ILIKE :pattern
              ORDER BY full_name
              LIMIT 25");
        $stmt->execute([':pattern' => $pattern]);
        respond(200, ['results' => $stmt->fetchAll()]);
    }

    respond(404, ['message' => 'Not found.']);
} catch (PDOException $e) {
    error_log('DB error ' . $e->getCode() . ': ' . $e->getMessage()); // log only
    [$status, $msg] = match ($e->getCode()) {
        '23505' => [409, 'That email address is already registered.'],
        '23514' => [422, 'Please check the form and try again.'],
        default => [500, 'The request could not be completed.'],
    };
    respond($status, ['message' => $msg]);
}
