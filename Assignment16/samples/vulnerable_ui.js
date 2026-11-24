// Deliberately vulnerable sample, used only to show the scanner working.
function show(result) {
  document.getElementById('out').innerHTML = result.name;
  return eval('(' + result.json + ')');
}
const AWS_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY";
