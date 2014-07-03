/*
 This code is needed to make javax.scripting work with compiled scripts.
 Compiled scripts in javax.scripting can only be executed, you cannot call
 specific functions inside the compiled script. Hence this nifty little script


 Expected input parameters:
 - importer: com.cekrlic.jlesscss.Importer -- the external file importer
 - name: String -- the name of the file we're compiling
 - content: String -- the LESS content
 - callback: The callback
 - result: com.cekrlic.jlesscss.JavaXScriptCompiler$Result -- the class that will receive the end response
*/

processLess(importer, name, content, {
	callback: function (e, root, content) {
		if(e==null) {
			result.result = root.toCSS();
		} else {
			result.result = String(e);
		}
		callback.callback(e, root, content);
	}
});