less.async = true;
less.fileAsync = true;

function Objectify(func) {
	this.callback = function() {
		func.apply(this, arguments);
	}
	return this;
}

var processLess = function(importer, name, content, callback, env) {
	log.debug("Processing " + name);
	// Without this, the thread will simply block!
	content = String(content);


	less.Parser.importer = function (filePath, currentFileInfo, call, localEnv) {
		var thisFilePath = (currentFileInfo.entryPath && currentFileInfo.entryPath != "") ? currentFileInfo.entryPath : localEnv.rootpath;
		var includeFilePath = thisFilePath + filePath;

		log.debug("Importing " + includeFilePath);
		// Call the Java importer that does request.include
		var fileData = importer.importFile(includeFilePath).content;
		log.debug("Included " + includeFilePath);
		processLess(importer, includeFilePath, fileData, new Objectify(call), localEnv);
	};

	if (env == null || typeof(env) == 'undefined') {
		env = {
			filename: name,
			syncImport: false
		}
	} else {
		env = new less.tree.parseEnv(env);
	}

	env.syncImport = false;
	env.useFileCache = false;
	env.contents = String(content);
	log.debug("Parsing starts...");
	var p = new (less.Parser)(env);
	p.parse(content, function (e, root) {
		log.debug("Parsing ends, notifying caller...");
		callback.callback(e, root, content);
	});
};

// JAVAX SCRIPTING has a difficulty returning data from scripts
var toCss = function(tree, result) {
	result.result = tree.toCSS();
};
