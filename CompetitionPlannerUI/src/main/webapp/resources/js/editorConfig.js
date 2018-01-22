CKEDITOR.editorConfig = function(config) {
	config.entities = false;
	config.tabSpaces = 8;
	config.enterMode = CKEDITOR.ENTER_BR;
	config.extraPlugins = "SelectImage";
	config.language = "de";
	config.toolbar = [["Source", "-", "NewPage", "Cut", "Copy", "Paste", "PasteText", "PasteFromWord", "-", "Undo", "Redo",
		"Find", "Replace", "-" , "SelectAll", "-", "Scayt", "-",
		"Bold", "Italic", "Underline", "Strike", "Subscript", "Superscript", "-", "RemoveFormat",
		"NumberedList", "BulletedList", "-", "Outdent", "Indent", "-",
		"JustifyLeft", "JustifyCenter", "JustifyRight", "JustifyBlock", "-",
		"Link", "Unlink", "Anchor", "Image", "Table", "CreateDiv", "HorizontalRule", "Smiley", "SpecialChar", "PageBreak"],
		"/",
		["Styles", "Format", "Font", "FontSize", "TextColor", "BGColor", "Maximize", "ShowBlocks"]];
};
CKEDITOR.plugins.add("SelectImage", {
	init: function(editor) {
		editor.addCommand("SelectImage", {
			exec: selectImage
		});
		editor.ui.addButton("Image", {
			label: "Bild einfügen",
			command: "SelectImage"
		});
	}
});
function selectImage(editor) {
	var form = document.getElementById("fileUploadForm");
	if (!form) {
		var form = document.createElement("form");
		form.setAttribute("id", "fileUploadForm");
		var input = document.createElement("input");
		input.setAttribute("name", "fileSelect");
		input.setAttribute("type", "file");
		input.setAttribute("accept", "image/*");
		form.appendChild(input);
		document.body.appendChild(form);
		input.onchange = function() {
			fileSelected(form, editor);
		}
	}
	form.fileSelect.value = null;
	form.fileSelect.click();
}
function fileSelected(form, editor) {
	if (form.fileSelect.files.length != 1)
		return;
	var file = form.fileSelect.files.item(0);
	if (window.FileReader) {
		var fr = new FileReader();
		fr.onload = function () {
			editor.insertHtml("<img src=\"" + fr.result + "\" />");
		};
		fr.readAsDataURL(file);
	}
}
