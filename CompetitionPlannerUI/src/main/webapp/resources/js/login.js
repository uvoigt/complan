function init() {
	document.loginform.j_username.focus();
	var index = location.href.indexOf("/passwordreset");
	if (index != -1)
		location.replace(location.href.substring(0, index));
}
function message(msg) {
	document.getElementById("message").innerText = msg;
}
function enableFields(form, enable) {
	for (var i = 0; i < form.elements.length; i++) {
		form.elements[i].disabled = !enable;
	}
}
function request(url, form, errorMsg) {
	var req = createXMLHttpRequest();
	req.open("POST", url);
	req.onreadystatechange = function() {
		if (req.readyState == 4) {
			if (req.status == 200)
				message(req.responseXML.firstChild.textContent);
			else
				message(errorMsg);
			enableFields(form, true);
		}
	}
	message("");
	enableFields(form, false);
	req.send();
}
function sendRecovery(loginMsg, errorMsg) {
	var user = document.loginform.j_username.value;
	if (user == "")
		message(loginMsg);
	else
		request("passwordreset?user=" + user, document.loginform, errorMsg);
}
function sendRegister(loginMsg, errorMsg) {
	var email = document.registerform.email.value;
	if (email == "")
		message(loginMsg);
	else
		request("register?email=" + email, document.registerform, errorMsg);
}
function register(msg) {
	document.loginform.style.display = "none";
	document.registerform.style.display = "block";
	message(msg);
}
function sendLogin(title, msgError) {
	var req = createXMLHttpRequest();
	req.open("POST", "j_security_check");
	req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
	if (document.loginform.stayLoggedIn.checked)
		req.setRequestHeader("Stay-Logged-In", "true");
	req.onreadystatechange = function() {
		if (req.readyState == 4) {
			if (req.status == 200 && req.responseText.indexOf(title) == -1)
				location.reload();
			else {
				message(msgError);
				enableFields(document.loginform, true);
			}
		}
	}
	enableFields(document.loginform, false);
	req.send("j_username=" + document.loginform.j_username.value + "&j_password=" + document.loginform.j_password.value);
}
function createXMLHttpRequest() { 
	if (typeof XMLHttpRequest != "undefined")
		return new XMLHttpRequest();
	else if (typeof ActiveXObject != "undefined")
		return new ActiveXObject("Microsoft.XMLHTTP");
	else
		throw new Error("XMLHttpRequest not supported");
}
