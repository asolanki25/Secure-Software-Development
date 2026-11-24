<?php
// Deliberately vulnerable sample, used only to show the scanner working.
$pdo = new PDO('pgsql:host=127.0.0.1;dbname=demo', 'app', 'Summer2024!');
$name = $_GET['name'];
$rows = $pdo->query("SELECT * FROM customers WHERE full_name = '" . $name . "'");
echo "<p>Results for " . $_GET['name'] . "</p>";
