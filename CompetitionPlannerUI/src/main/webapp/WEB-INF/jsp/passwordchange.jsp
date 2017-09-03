<% response.setCharacterEncoding("UTF8");
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="f"
%><!DOCTYPE html>
<f:bundle basename="MessagesBundle">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<title><f:message key="passchangeTitle"/></title>
	<link href="favicon.ico" rel="icon" type="image/x-icon"/>
	<link rel="stylesheet" type="text/css" href="resources/css/login.css"/>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<script type="text/javascript">
	function checkpw() {
		document.passform.change.disabled = document.passform.p1.value != document.passform.p2.value || document.passform.p1.value == "";
	}
	</script>
</head>
<body onload="document.passform.p1.focus();">
	<form method="post" action="passwordchange" name="passform">
		<input type="hidden" name="t" value="<%= request.getParameter("t") %>" />
		<div id="main">
			<table>
				<tbody>
					<tr>
						<td><input type="password" name="p1" placeholder="<f:message key="loginPass"/>" onkeyup="checkpw()" onblur="checkpw()" /></td>
					</tr>
					<tr>
						<td><input type="password" name="p2" placeholder="<f:message key="loginPass"/>" onkeyup="checkpw()" onblur="checkpw()" /></td>
					</tr>
					<tr>
						<td colspan="2"><input class="anmelden" style="width: unset;" type="submit" name="change" disabled="disabled" value="<f:message key="passchangeSubmit"/>" /></td>
					</tr>
				</tbody>
			</table>
		</div>
	</form>
</body>
</html>
</f:bundle>