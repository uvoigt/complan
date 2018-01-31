<% response.setCharacterEncoding("UTF-8");
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="f"
%><!DOCTYPE html>
<f:bundle basename="MessagesBundle">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<title><f:message key="passchangeTitle"/></title>
	<link href="favicon.ico" rel="icon" type="image/x-icon"/>
	<link rel="stylesheet" type="text/css" href="resources/css/login.css"/>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<script type="text/javascript">
	function checkpw() {
		document.passform.change.disabled = document.passform.p1.value != document.passform.p2.value || document.passform.p1.value == "";
	}
	</script>
</head>
<body onload="document.passform.p1.focus();">
	<div class="vertical">
		<div class="horizontal main">
			<noscript>
				<div class="modal"></div>
				<div class="noscript"><f:message key="noscript"/></div>
			</noscript>

			<form method="post" action="passwordchange?${pageContext.request.queryString}" name="passform">
				<table>
					<tbody>
						<tr>
							<td><input type="password" name="p1" placeholder="<f:message key="loginPass"/>" onkeyup="checkpw()" onblur="checkpw()" /></td>
						</tr>
						<tr>
							<td><input type="password" name="p2" placeholder="<f:message key="loginPass"/>" onkeyup="checkpw()" onblur="checkpw()" /></td>
						</tr>
						<tr>
							<td colspan="2"><input class="submit" style="width: unset;" type="submit" name="change" disabled="disabled" value="<f:message key="passchangeSubmit"/>" /></td>
						</tr>
					</tbody>
				</table>
			</form>
		</div>
	</div>
</body>
</html>
</f:bundle>