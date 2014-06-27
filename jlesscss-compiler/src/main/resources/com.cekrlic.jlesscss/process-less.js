less.async = true;
less.fileAsync = true;

function Objectify(func) {
	this.callback = function() {
		func.apply(this, arguments);
	}
	return this;
}

var processLess = function(importer, name, content, callback, env) {
	// Without this, the thread will simply block!
	content = String(content);


	less.Parser.importer = function (filePath, currentFileInfo, call, localEnv) {
		var thisFilePath = (currentFileInfo.entryPath && currentFileInfo.entryPath != "") ? currentFileInfo.entryPath : localEnv.rootpath;
		var includeFilePath = thisFilePath + filePath;

		// Call the Java importer that does request.include
		var fileData = importer.importFile(includeFilePath).content;
		processLess(importer, includeFilePath, fileData, new Objectify(call), localEnv);
	};

	if (env == null || typeof(env) == 'undefined') {
		env = {
			filename: name,
			syncImport: false
		}
	} else {
		env = new window.less.tree.parseEnv(env);
	}

	env.syncImport = false;
	env.useFileCache = false;
	env.contents = String(content);
	var p = new (less.Parser)(env);
	p.parse(content, function (e, root) {
		callback.callback(e, root, content);
	});
};