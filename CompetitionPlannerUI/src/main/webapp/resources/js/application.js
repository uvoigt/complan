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
	timeOnlyTitle: "Nur Zeit",
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
function login1() {
	$.ajax({
		method: "POST",
		url: "j_security_check",
			data: "j_username=" + dlg.jq.find("input[type=text]").val() + "&j_password=" + dlg.jq.find('input[type=password]').val()
	}).done(function(data) {
		if (typeof data == "string" && data.indexOf(dlg.jq.find("#loginDialog_title").text()) != -1)
			return;
		dlg.hide();
		var ext;
		PrimeFaces.ajax.AjaxRequest(dlg.cfg, ext);
//		$.ajax({
//			method: dlg.cfg.type,
//			url: dlg.cfg.url,
//			data: dlg.cfg.data
////		}).beforeSend(function(xhr, settings) {
////			dlg.xhr.beforeSend(xhr, settings);
//		}).error(function(xhr, textStatus, error) {
//			dlg.xhr.error(xhr, textStatus, error);
//		}).success(function(data, textStatus, xhr) {
//			dlg.xhr.success(data, textStatus, xhr);
//		}).complete(function(xhr, textStatus) {
//			dlg.xhr.complete(xhr, textStatus);
//		});
	}).fail(function() {
		dlg.jq.find(".loginError").show();
	}).always(function() {
		dlg.jq.find(".ui-button").attr("disabled", false);
	});
}
function emptyResponse() {
	return "<?xml version='1.0' encoding='UTF-8'?><partial-response />";
}