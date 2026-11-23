<?php
// Kept outside the web root so it can never be downloaded as a page.
declare(strict_types=1);

function db(): PDO
{
    static $pdo = null;
    if ($pdo instanceof PDO) {
        return $pdo;
    }
    // Credentials come from the environment, never from source code.
    $dsn  = sprintf('pgsql:host=%s;port=%s;dbname=%s',
                    getenv('DB_HOST') ?: '127.0.0.1',
                    getenv('DB_PORT') ?: '5432',
                    getenv('DB_NAME') ?: 'customer_portal');
    $pdo = new PDO($dsn, getenv('DB_USER') ?: 'customer_app',
                   getenv('DB_PASSWORD') ?: '', [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_EMULATE_PREPARES   => false,   // real server-side prepares
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    return $pdo;
}
