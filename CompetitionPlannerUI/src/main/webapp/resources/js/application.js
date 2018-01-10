function selectSidebarLink(link) {
	$("#leftMenu").find(".ui-state-active").removeClass("ui-state-active");
	if (link) {
		$(link).addClass("ui-state-active");
	}
	hideHelp();
}
function navItemSelected() {
	$("#leftMenuContainer").css("display", "").addClass("menuHidden");
}
function hideHelp() {
	PF("layout").hide("east");
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
function sendLogin() {
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
				//	var ext;
				//	PrimeFaces.ajax.AjaxRequest(dlg.cfg, ext);
				refresh();							
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
	val = val !== undefined ? "?i=" + encodeURIComponent(val) : location.protocol + "//" + location.host + location.pathname;
	if (history.replaceState)
		history.replaceState("", "", val);
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
function toggleColumn(table, index) {
	var columnHeader = table.thead.children("tr").find("th:nth-child(" + index + ")");
	columnHeader.toggleClass("ui-helper-hidden");
	table.tbody.children("tr").find("td:nth-child(" + index + ")").toggleClass("ui-helper-hidden");
}
function initExprText() {
	var expr = PF("expr");
	if (!expr)
		return;
	expr.jq.attr("spellcheck", false);
	expr.jq.keydown(function(ev) {
		console.log(ev);
		if (ev.key == '') {
			var popup = document.createElement("div")
			document.body.append(p);
			var pjq = $(popup);
			pjq.css("background-color", "#faebd7");
			pjq.width(144);
			pjq.height(136);
			pjq.css("position", "absolute");
			pjq.css("top", "");
			pjq.css("left", "");
			pjq.css("border-radius", "3px");
			pjq.css("border", "solid 1px");
		}
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
