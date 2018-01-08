<% response.setCharacterEncoding("UTF-8");
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="f"
%><!DOCTYPE html>
<f:setLocale value="${pageContext.request.locale}"/>
<f:setBundle basename="MessagesBundle"/>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<title><f:message key="loginTitle"/></title>
	<link rel="icon" type="image/x-icon" href="favicon.ico">
	<link rel="stylesheet" type="text/css" href="resources/css/login.css"/>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<script type="text/javascript" src="resources/js/login.js"></script>
</head>
<body onload="document.loginform.j_username.focus();">
	<div class="vertical">
		<div class="horizontal main">
			<noscript>
				<div class="modal"></div>
				<div class="noscript"><f:message key="noscript"/></div>
			</noscript>
			<div class="message">
				<div id="message"></div>
			</div>
			<form method="post" action="" name="loginform" onsubmit="sendLogin('<title><f:message key="loginTitle"/></title>', '<f:message key="loginError"/>'); return false;">
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
								<div class="recovery">
									<a href="" onclick="sendRecovery('<f:message key="loginUserMsg"/>', '<f:message key="loginUserError"/>'); return false;">
										<f:message key="loginRecovery"/>
									</a>
									<br />
									<a href="" class="register" onclick="register('<f:message key="loginRegisterMsg"/>'); return false;">
										<f:message key="loginRegister"/>
									</a>
								</div>
							</td>
						</tr>
						<tr>
							<td colspan="2"><input id="submit" type="submit" class="submit" value="<f:message key="loginSubmit"/>" /></td>
						</tr>
					</tbody>
				</table>
			</form>
			<form method="post" action="" name="registerform" onsubmit="sendRegister('<f:message key="loginRegisterMsg"/>', '<f:message key="loginUserError"/>'); return false;" style="display: none;">
				<table>
					<tbody>
						<tr>
							<td><input type="text" name="email" placeholder="<f:message key="loginEmail"/>" autocomplete="off"/></td>
						</tr>
						<tr>
							<td colspan="2"><input id="submit" type="submit" class="submit" value="<f:message key="loginSubmitRegister"/>" /></td>
						</tr>
					</tbody>
				</table>
			</form>
		</div>
	</div>
</body>
</html>