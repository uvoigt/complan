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
		if (document.closer) {
			$(document).unbind("keydown", document.closer);
			delete document.closer;
		}
	} else {
		var isSmallScreen = helpUI.width() == $(document).width(); 
		helpUI.animate({ "left": isSmallScreen ? "0" : "30%" }, 200);
		var closer = function(evt) {
			if (evt.which == 27)
				toggleHelp(false);
		};
		$(document).keydown(closer);
		document.closer = closer;
	}
}
function sendLogin(formId) {
	var dlg = PF("loginDlg");
	var uname = dlg.jq.find("input[type=text]");
	var upass = dlg.jq.find("input[type=password]");
	var req = createXMLHttpRequest();
	req.open("POST", "j_security_check");
	req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
	req.onreadystatechange = function() {
		if (req.readyState == 4) {
			dlg.jq.find("input,button").attr("disabled", false).attr("readonly", false).removeClass("ui-state-disabled");
			if (req.status == 200 && req.responseText.indexOf("<title>" + dlg.jq.find("#loginDlg_title").text() + "</title>") == -1) {
				message("");
				dlg.cfg.onHide = function() {
					uname.val("");
					upass.val("");
				};
				dlg.hide();
				var prevId = $("#userimg").attr("class");
				if (prevId != req.responseText)
					PrimeFaces.ab( { s: formId, f: formId, p: "@none", u: "currentUser leftMenu theme mainContent", ps: true });
			} else {
				message("{msg:loginError}");
			}
		}
	}
	dlg.jq.find("input,button").attr("disabled", true).attr("readonly", true).addClass("ui-state-disabled");
	req.send("j_username=" + encodeURIComponent(uname.val()) + "&j_password=" + encodeURIComponent(upass.val()));
}
function sendRecovery() {
	var dlg = PF("loginDlg");
	var user = dlg.jq.find("input[type=text]").val();
	if (user == "") {
		message("{msg:loginUserMsg}");
	} else {
		var req = createXMLHttpRequest();
		req.open("POST", "passwordreset?user=" + user);
		req.onreadystatechange = function() {
			if (req.readyState == 4) {
				if (req.status == 200)
					message(req.responseXML.firstChild.textContent);
				else
					message("{msg:loginUserError}");
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
function checkEmpty(countLabel) {
	if ($(".ui-datatable-empty-message").length > 0)
		updateResultCount(countLabel, 0);
}
function updateResultCount(countLabel, count) {
	if (count == undefined)
		count = PF("searchTable").tbody.children().length;
	$("[id$=resultCountLabel]").text(countLabel.replace(/xxx/, count));
}
function updateColumnWidth(buttonCount) {
	$("[id$=aktionenColumn]").css("width", "calc(2.5em * " + buttonCount + " + 10px)");
}
function rowDoubleClicked(table, rowId) {
	var button = table.jq.find("[data-rk=" + rowId + "]>:last-child>.editButton");
	if (button.length > 0) {
		if (!button.attr("disabled"))
			button.trigger("click");
	} else {
		var btnName = table.jq.find("[data-rk=" + rowId + "]>:last-child>.ui-menubutton>button").attr("name");
		if (btnName) {
			btnName = btnName.replace(/:/g, "\\:").replace(/_button/, "_menu");
			button = $(document).find("#" + btnName + ">>.ui-menuitem>:first-child").eq(0);
			if (!button.attr("disabled"))
				button.trigger("click");
		}
	}
}
function attachSubmitHandler() {
	if (!main.handlerAttached) {
		$(main).submit(function(e) {
			setTimeout(function() {
				$(main).find(".ui-submit-param").remove();
			}, 10);
		});
		main.handlerAttached = true;
	}
}
var announcementEdit = {
	init: function() {
		attachSubmitHandler();
		return this;
	}
};
var racesEdit = {
	enableButtons: function() {
		var racesTable = PF("racesTable");
		if (!racesTable || racesTable.getSelectedRowsCount() == 0)
			PF("btnRaceDelete").disable();
		else
			PF("btnRaceDelete").enable();
	}
}
var registrationEdit = {
	enableButtons: function() {
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
		return this;
	},
	checkEmpty: function() {
		if ($("[id$=athletesTable] .ui-datatable-empty-message").length > 0)
			this.updateAthletesCount(0);
		return this;
	},
	updateAthletesCount: function(count) {
		if (count == undefined)
			count = PF("athletesTable").tbody.children().length;
		$("[id$=athletesCountLabel]").text("{msg:registrations.athletesCount, xxx}".replace(/xxx/, count));
		return this;
	},
	updateResultSelected: function() {
		var count = PF("athletesTable").selection.length;
		$("[id$=resultSelectedLabel]").text("{msg:registrations.selected, xxx}".replace(/xxx/, count));
		return this;
	},
	copyFilters: function(fromSelection) {
		var racesTable = PF("racesTable");
		var athletesTable = PF("athletesTable");
		var copyToRegistrationTable = true; // TODO
		if (fromSelection) {
			var selectedId = racesTable.jq.find("[id$=racesTable_selection]").val();
			var row = racesTable.jq.find("tr[data-rk=" + selectedId + "]");
			var ageType = row.children().eq(2).text();
			var gender = row.children().eq(3).text();
			if (gender == "mixed")
				gender = "";
			var raceNumber = row.children().eq(0).text();
		} else {
			var ageType = racesTable.jq.find("[name$=ageType\\:filter]").val();
			var gender = racesTable.jq.find("[name$=gender\\:filter]").val();
		}
		athletesTable.jq.find("[name$=ageType\\:filter]").val(ageType);
		athletesTable.jq.find("[name$=gender\\:filter]").val(gender);
		athletesTable.filter();
		if (copyToRegistrationTable && fromSelection) {
			var registrationTable = PF("registrationTable");
			var raceFilterPattern = $("#raceFilterPattern").text();
			registrationTable.jq.find("[name$=raceNumber\\:filter]").val(raceFilterPattern.replace(/\$/, raceNumber));
			registrationTable.filter();
		}
	},
	toggleColumn: function(table, index) {
		var columnHeader = table.thead.children("tr").find("th:nth-child(" + index + ")");
		columnHeader.toggleClass("ui-helper-hidden");
		table.tbody.children("tr").find("td:nth-child(" + index + ")").toggleClass("ui-helper-hidden");
	},
	updateRegistrationsCount: function(count) {
		if (count == undefined)
			count = PF("registrationTable").tbody.children().length;
		$("[id$=registrationsCountLabel]").text("{msg:registrations.athletesCount, xxx}".replace(/xxx/, count));
	}
};
var programEdit = {
	initExpr: function() {
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
	},
	evalExpr: function() {
		if (window.evaluateExpression) {
			var result = evaluateExpression(PF("expr").jq.val());
			PF("exprStatus").jq.val(result);
		}
	},
	toggleRaceSelection: function(checkbox) {
		var pt = PF("programTable");
		try {
			checkbox.prop("checked") ? pt.selectRowWithCheckbox(checkbox, true) : pt.unselectRowWithCheckbox(checkbox, true);
		} catch (ex) {}
		var numChecked = pt.tbody.find("> tr > td >* :checkbox.ui-state-active").length;
		if (numChecked > 2) {
			checkbox.prop("checked", false);
			numChecked--;
			try {
				pt.unselectRowWithCheckbox(checkbox, true);
			} catch (ex) {}
		}
		this.enableSwap(numChecked == 2);
	},
	enableSwap: function(enable) {
		var swap = PF("swapRaces");
		if (swap)
			enable ? swap.enable() : swap.disable();
	},
	gotoRace: function(id) {
		var rowTop = PF("programTable").jq.find("[data-rk=" + id + "]").offset().top;
		var mainTop = $("#mainContent").offset().top;
		$(".ui-layout-pane-center>.ui-layout-unit-content").animate({scrollTop: rowTop - mainTop}, "fast");
		return false;
	},
	confirmResult: function(message, args) {
		var dlg = PF("confirmResult");
		dlg.message.text(message);
		dlg.args = args;
		dlg.show();
	},
	argsToParams: function(args) {
		var b = [];
		for (var a in args) {
			var c = {name: a};
			c.value = args[a];
			b.push(c);
		}
		return b;
	},
	cellEditInit: function(options) {
		options.global = true;
		var oldOncomplete = options.oncomplete;
		options.oncomplete = function(xhr, status, args) {
			oldOncomplete(xhr, status, args);
			programEdit.enableResultExtra(false);
			var placement = PF("placement");
			placement.items.each(function() {
				var item = $(this);
				var split = item.attr("data-item-value").split(";");
				if (split.length > 1) {
					item.attr("data-item-value", split[0]);
					var resultExtra = PF("resultExtra");
					var text = resultExtra.buttons.children("input[value=" + split[1] +"]").parent().text();
					item.find(".extra").text(text);
				}
			});
		};
		PrimeFaces.ajax.Request.handle(options);
	},
	applyCellEdit: function() {
		var table = PF("programTable");
		if (table.currentCell)
			table.saveCell(table.currentCell);
	},
	cancelCellEdit: function() {
		var table = PF("programTable");
		if (table.currentCell)
			table.currentCell.blur();
		table.jq.parent().focus();
		table.jq.click();
	},
	enableResultExtra: function(enable) {
		var resultExtra = PF("resultExtra");
		enable ? resultExtra.enable() : resultExtra.disable();
		if (enable) {
			var placement = PF("placement");
			var text = placement.items.filter(".ui-state-highlight").find(".extra").text();
			var change = resultExtra.cfg.change;
			resultExtra.cfg.change = null;
			resultExtra.unselect(resultExtra.buttons);
			resultExtra.buttons.blur();
			if (text) {
				var btn = resultExtra.buttons.find(":contains(" + text + ")").parent();
				resultExtra.select(btn);
			}
			resultExtra.cfg.change = change;
		}
	},
	updateResultWithExtra: function(resultExtra) {
		var selected = resultExtra.buttons.filter(".ui-state-active");
		var text = selected.text();
		var extra = selected.children("input").val();
		var placement = PF("placement");
		var highlight = placement.items.filter(".ui-state-highlight");
		highlight.find(".extra").text(text);
		var data = highlight.attr("data-item-value");
		var value = data + ";" + extra;
		if (selected.length == 0)
			value = data;
		placement.jq.find(".ui-helper-hidden>").eq(highlight.index()).attr("value", value).text(value);
	}
};
function initLoginDialog() {
	$.ajaxSetup({
		dataFilter: function(data) {
			if (data.indexOf("{msg:loginTitle}") != -1) {
				PrimeFaces.debug("Detected unauthenticated request");
				PF("loginDlg").show();
				data = "<?xml version='1.0' encoding='UTF-8'?><partial-response />";
			}
			return data;
		}
	});
}
function showStatusDialog() {
	statusTimeout = setTimeout(function() {
		statusTimeout = null;
		PF("statusDialog").show();
	}, 500);
}
function hideStatusDialog() {
	if (statusTimeout)
		clearTimeout(statusTimeout);
	else
		PF("statusDialog").hide();
}
function updateConfirmDlg(title, msg) {
	var dlg = PF("confirmDlg");
	dlg.title.text(title);
	dlg.message.text(msg);
	dlg.icon.remove();
}
function initErrorDialog() {
	PF("errorDetails").legend.click(function() {
		if (copyToClipboard(PF("errorDetails").jq.children().get(1)))
			$(".copiedMessage").show().fadeOut(3000);
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
