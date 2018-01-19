CKEDITOR.editorConfig = function( config ) {
	config.entities = false;
	config.language = "de";
	config.toolbar = [["Source", "-", "NewPage", "Cut", "Copy", "Paste", "PasteText", "PasteFromWord", "-", "Undo", "Redo",
		"Find", "Replace", "-" , "SelectAll", "-", "Scayt", "-",
		"Bold", "Italic", "Underline", "Strike", "Subscript", "Superscript", "-", "RemoveFormat",
		"NumberedList", "BulletedList", "-", "Outdent", "Indent", "-",
		"JustifyLeft", "JustifyCenter", "JustifyRight", "JustifyBlock", "-",
		"Link", "Unlink", "Anchor", "Image", "Table", "HorizontalRule", "Smiley", "SpecialChar", "PageBreak"],
		"/",
		["Styles", "Format", "Font", "FontSize", "TextColor", "BGColor", "Maximize", "ShowBlocks"]];
};

