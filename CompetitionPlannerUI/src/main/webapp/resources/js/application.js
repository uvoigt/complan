function selectSidebarLink(link) {
	$("#leftMenu").find(".ui-state-active").removeClass("ui-state-active");
	if (link) {
		$(link).addClass("ui-state-active");
	}
}
function navItemSelected() {
	$("#leftMenuContainer").css("display", "").addClass("menuHidden");
}
function toggleHelp(show) {
	var helpUI = $("#helpUI");
	if (show === false) {
		helpUI.animate({ "left": "100%" }, 200);
	} else {
		var isSmallScreen = helpUI.width() == $(document).width(); 
		helpUI.animate({ "left": isSmallScreen ? "0" : "30%" }, 200);
	}
}
function sendLogin(formId) {
	var dlg = PF("loginDlg");
	var req = createXMLHttpRequest();
	req.open("POST", "j_security_check");
	req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
	req.onreadystatechange = function() {
		if (req.readyState == 4) {
			dlg.jq.find("input,button").attr("disabled", false).attr("readonly", false);
			if (req.status == 200 && req.responseText.indexOf("<title>" + dlg.jq.find("#loginDialog_title").text() + "</title>") == -1) {
				message("");
				dlg.hide();
				var prevId = PrimeFaces.identity;
				PrimeFaces.ab({ s: formId, f: formId, u: "leftMenu theme currentUser currentUserMenu", onco: function() {
					if (prevId != PrimeFaces.identity)
						PrimeFaces.ab( { s: formId, f: formId, u: "mainContent" });
				}});
			} else {
				message("{msg:loginError}");
			}
		}
	}
	dlg.jq.find("input,button").attr("disabled", true).attr("readonly", true);
	req.send("j_username=" + dlg.jq.find("input[type=text]").val() + String.fromCharCode(38) + "j_password=" + dlg.jq.find('input[type=password]').val());
}
function sendRecovery(userMsg, errorMsg) {
	var dlg = PF("loginDlg");
	var user = dlg.jq.find("input[type=text]").val();
	if (user == "") {
		message(userMsg);
	} else {
		var req = createXMLHttpRequest();
		req.open("POST", "passwordreset?user=" + user);
		req.onreadystatechange = function() {
			if (req.readyState == 4) {
				if (req.status == 200)
					message(req.responseXML.firstChild.textContent);
				else
					message(errorMsg);
			}
		}
		req.send();
	}
}
function createXMLHttpRequest() { 
	if (typeof XMLHttpRequest != "undefined")
		return new XMLHttpRequest();
	else if (typeof ActiveXObject != "undefined")
		return new ActiveXObject("Microsoft.XMLHTTP");
	else
		throw new Error("XMLHttpRequest not supported");
}
function message(msg) {
	$(".loginMessage").html(msg);
}
function setUrlParam(val) {
	val = val ? "?" + encodeURIComponent(val) : location.protocol + "//" + location.host + location.pathname;
	if (history.replaceState)
		history.replaceState("", "", val);
}
function announcementEdit_enableStatusButtons(status) {
	var btnAnnounce = PF("btnAnnounce");
	var btnRevoke = PF("btnRevoke");
	if (!btnAnnounce || !btnRevoke)
		return;
	btnAnnounce.disable();
	btnRevoke.disable();
	if (status == 0)
		btnAnnounce.enable();
	else if (status == 1)
		btnRevoke.enable();
}
function registrationEdit_enableButtons() {
	var racesTable = PF("racesTable");
	var athletesTable = PF("athletesTable");
	var registrationTable = PF("registrationTable");
	var registrationSelected = registrationTable && registrationTable.getSelectedRowsCount() > 0;
	if (athletesTable && athletesTable.getSelectedRowsCount() > 0 &&
			(racesTable && racesTable.getSelectedRowsCount() > 0 || registrationSelected))
		PF("btnAddAthlete").enable();
	else
		PF("btnAddAthlete").disable();
	if (registrationSelected)
		PF("btnAddRequest").enable();
	else
		PF("btnAddRequest").disable();
}
function copyFilters() {
	var racesTable = PF("racesTable");
	var athletesTable = PF("athletesTable");
	var ageType = racesTable.jq.find("[name$=ageType\\:filter]").val();
	athletesTable.jq.find("[name$=ageType\\:filter]").val(ageType);
	var gender = racesTable.jq.find("[name$=gender\\:filter]").val();
	athletesTable.jq.find("[name$=gender\\:filter]").val(gender);
	athletesTable.filter();
}
function toggleColumn(table, index) {
	var columnHeader = table.thead.children("tr").find("th:nth-child(" + index + ")");
	columnHeader.toggleClass("ui-helper-hidden");
	table.tbody.children("tr").find("td:nth-child(" + index + ")").toggleClass("ui-helper-hidden");
}
function programEdit_initExpr() {
	var expr = PF("expr");
	if (!expr)
		return;
	expr.jq.attr("spellcheck", false);
	if (expr.jq.initialized)
		return;
	expr.jq.initialized = true;
	expr.jq.keydown(function(evt) {
		if (evt.keyCode == 9) {
			var start = expr.jq.getSelection().start;
			var text = expr.jq.val();
			expr.jq.val(text.substring(0, start) + "\t" + text.substring(start, text.length));
			expr.jq.setSelection(start + 1);
			evt.preventDefault();
			evt.stopPropagation();
		}
	});
	expr.jq.keypress(function(evt) {
		if (evt.charCode != 32 || !evt.ctrlKey)
			return;
		var text = expr.jq.val();
		text = text.substring(0, expr.jq.getCursorPosition());
		expr.search(text);
		expr.query = "";
	});
}
function setupAjax() {
	$.ajaxSetup({
		dataFilter: function(data) {
			$(document).off("keydown");
			if (data.indexOf("{msg:loginTitle}") != -1) {
				PrimeFaces.debug("Detected unauthenticated request");
				var dlg = PF("loginDlg");
				dlg.jq.find("input[type=text]").val("");
				dlg.jq.find('input[type=password]').val("");
				dlg.show();
				data = "<?xml version='1.0' encoding='UTF-8'?><partial-response />";
			}
			return data;
		}
	});
}
function copyToClipboard(element) {
	if (document.selection) { 
		var range = document.body.createTextRange();
		range.moveToElementText(element);
		range.select().createTextRange();
		var result = document.execCommand("copy"); 
		range.empty();
		return result;
	} else if (window.getSelection) {
		var range = document.createRange();
		range.selectNode(element);
		window.getSelection().addRange(range);
		var result = document.execCommand("copy"); 
		window.getSelection().removeAllRanges();
		return result;
	}
}
$(window).resize(function() {
	$("#leftMenuContainer").css("display", "");
});
PrimeFaces.locales["de"] = {
	closeText: "Schließen",
	prevText: "Zurück",
	nextText: "Weiter",
	monthNames: ["Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember"],
	monthNamesShort: ["Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"],
	dayNames: ["Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag"],
	dayNamesShort: ["Son", "Mon", "Die", "Mit", "Don", "Fre", "Sam"],
	dayNamesMin: ["S", "M", "D", "M ", "D", "F ", "S"],
	weekHeader: "Woche",
	firstDay: 1,
	isRTL: false,
	showMonthAfterYear: false,
	yearSuffix: "",
	timeOnlyTitle: "",
	timeText: "Zeit",
	hourText: "Stunde",
	minuteText: "Minute",
	secondText: "Sekunde",
	currentText: "Aktuelles Datum",
	ampm: false,
	month: "Monat",
	week: "Woche",
	day: "Tag",
	allDayText: "Ganzer Tag"
};
