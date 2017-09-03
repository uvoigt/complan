<% response.setCharacterEncoding("UTF8");
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="f"
%><!DOCTYPE html>
<f:bundle basename="MessagesBundle">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<title><f:message key="loginTitle"/></title>
	<link rel="icon" type="image/x-icon" href="favicon.ico">
	<link rel="stylesheet" type="text/css" href="resources/css/login.css"/>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<script type="text/javascript">
	function message(msg) {
		msgElm.innerText = msg;
	}
	function enableFields(form, enable) {
		for (var i = 0; i < form.elements.length; i++) {
			form.elements[i].disabled = !enable;
		}
	}
	function sendLogin() {
		var req = new XMLHttpRequest();
		req.open("POST", "j_security_check");
		req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
		req.onreadystatechange = function() {
			if (req.readyState == 4) {
				if (req.status == 200 && req.responseText.indexOf('<title><f:message key="loginTitle"/></title>') == -1)
					location.reload();
				else {
					message('<f:message key="loginError"/>');
					enableFields(document.loginform, true);
				}
			}
		}
		enableFields(document.loginform, false);
		req.send("j_username=" + document.loginform.j_username.value + "&j_password=" + document.loginform.j_password.value);
	}
	function request(url, form) {
		var req = new XMLHttpRequest();
		req.open("POST", url);
		req.onreadystatechange = function() {
			if (req.readyState == 4) {
				if (req.status == 200)
					message(req.responseXML.firstChild.textContent);
				else
					message('<f:message key="loginUserError"/>');
				enableFields(form, true);
			}
		}
		message("");
		enableFields(form, false);
		req.send();
	}
	function sendRecovery() {
		var user = document.loginform.j_username.value;
		if (user == "")
			message('<f:message key="loginUserMsg"/>');
		else
			request("passwordreset?user=" + user, document.loginform);
	}
	function sendRegister() {
		var email = document.registerform.email.value;
		if (email == "")
			message('<f:message key="loginRegisterMsg"/>');
		else
			request("register?email=" + email, document.registerform);
	}
	function register() {
		document.loginform.style.display = "none";
		document.registerform.style.display = "block";
		msgElm = document.getElementById("message1")
		message('<f:message key="loginRegisterMsg"/>');
	}
	</script>
</head>
<body onload="document.loginform.j_username.focus(); msgElm = document.getElementById('message');">
	<form action="" name="loginform" onsubmit="sendLogin(); return false;">
		<div id="main">
			<div id="message" class="message"><%
				if (Boolean.TRUE.equals(request.getAttribute("login.error"))) { %><f:message key="loginError"/><%
				}  %></div>
			<table>
				<tbody>
					<tr>
						<td><input type="text" name="j_username" placeholder="<f:message key="loginName"/>" autocomplete="off"/></td>
					</tr>
					<tr>
						<td><input type="password" name="j_password" placeholder="<f:message key="loginPass"/>" /></td>
					</tr>
					<tr>
						<td colspan="2" align="right">
							<div style="height: 20px">
								<a href="" class="recovery" onclick="sendRecovery(); return false;"><f:message key="loginRecovery"/></a>
								<br />
								<a href="" class="recovery register" onclick="register(); return false;"><f:message key="loginRegister"/></a>
							</div>
						</td>
					</tr>
					<tr>
						<td colspan="2"><input id="submit" type="submit" class="anmelden" value="<f:message key="loginSubmit"/>" /></td>
					</tr>
				</tbody>
			</table>
		</div>
	</form>
	<form action="" name="registerform" onsubmit="sendRegister(); return false;" style="display: none;">
		<div id="main">
			<div id="message1" class="message"></div>
			<table>
				<tbody>
					<tr>
						<td><input type="text" name="email" placeholder="<f:message key="loginEmail"/>" autocomplete="off"/></td>
					</tr>
					<tr>
						<td colspan="2"><input id="submit" type="submit" class="anmelden" value="<f:message key="loginSubmitRegister"/>" /></td>
					</tr>
				</tbody>
			</table>
		</div>
	</form>
</body>
</html>
</f:bundle>