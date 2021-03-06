package com.cekrlic.jlesscss;

import org.mozilla.javascript.*;
import org.mozilla.javascript.tools.shell.Global;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * @author boky
 * @created 5.5.2014 12:00
 */
public class RhinoCompiler extends Compiler {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RhinoCompiler.class);
	private static final org.slf4j.Logger log_js = org.slf4j.LoggerFactory.getLogger(RhinoCompiler.class.getName() + ".js");

	private final Scriptable scope;
	private final ContextFactory contextFactory;

	public RhinoCompiler() throws LessCSSException {
		super();

		contextFactory = ContextFactory.getGlobal();
		try {
			Context context = contextFactory.enterContext();
			context.setLanguageVersion(Context.VERSION_1_8);
			context.setOptimizationLevel(9);
			Global global = new Global();
			global.init(context);
			scope = context.initStandardObjects(global);
			ScriptableObject.putProperty(scope, "log", Context.javaToJS(log_js, scope));
			// scope.put("log", scope, Context.toObject(log, scope));

			ClassLoader[] classLoaders = new ClassLoader[] {
					Thread.currentThread().getContextClassLoader(),
					this.getClass().getClassLoader()
			};
			// Files need to bi split otherwise you're going to hit the Rhino/JVM 64k limit
			for (String s : new String[]{
					"com/cekrlic/jlesscss/less-rhino.js",
					"com/cekrlic/jlesscss/process-less.js"
			}) {
				java.io.InputStream is = null;
				for(ClassLoader cl: classLoaders) {
					is = cl.getResourceAsStream(s);
					if(is != null) { break; }
				}

				if(is == null) {
					throw new LessCSSException("Could not load file " + s + "!");
				}

				try (Reader r = new InputStreamReader(is, "UTF-8")) {
					log.info("Compiling {}", getName(s));
					Script script = context.compileReader(r, getName(s), 1, null);
					script.exec(context, scope);
				}
			}
		} catch (IOException ex) {
			throw new LessCSSException("Unable to initialize LESS compiler.", ex);
		} finally {
			Context.exit();
		}
	}

	/**
	 * Compile the LESS input into CSS.
	 *
	 * @param source The LESS file being compiled.
	 * @return Compiled LESS input.
	 * @throws LessCSSException
	 */
	public String compile(Source source, Importer importer) throws LessCSSException {

		try {
			long start, end;

			final Context context = contextFactory.enterContext();
			context.setLanguageVersion(Context.VERSION_1_8);

			final Callback callback = new Callback(context, scope, source.getFileName());

			// log.info("Calling processLess for {}", source.getFileName());
			Function processLess = (Function) ScriptableObject.getProperty(scope, "processLess");
			processLess.call(context, scope, scope, new Object[]{
					Context.toObject(importer, scope),
					Context.toObject(source.getFileName(), scope),
					Context.toObject(source.getContent(), scope),
					Context.toObject(callback, scope)
			});

			start = System.nanoTime();
			if(!callback.isComplete()) {
				log.debug("Waiting for less to finish processing for {}", source.getFileName());
				synchronized (callback) {
					callback.wait(15000);
				}
			}

			if(!callback.isComplete()) {
				throw new LessCSSException("Timed out while waiting for compilation to complete for " + source.getFileName());
			}

			end = System.nanoTime();
			log.info("Processing for {}: {}s", source.getFileName(), String.format("%.3f", (double) (end - start) / 1000000000.0d));

			return callback.toString();

		} catch (InterruptedException | JavaScriptException ex) {
			throw new LessCSSException("Unable to process LESS input from " + source.getFileName(), ex);
		} finally {
			Context.exit();
		}
	}

	public static class Callback extends Compiler.Callback {
		private static final Logger log = LoggerFactory.getLogger(Callback.class);
		private final Context context;
		private final Scriptable scope;

		private ScriptableObject err = null;
		private ScriptableObject tree = null;

		public Callback(Context context, Scriptable scope, String filename) {
			super(filename);
			this.context = context;
			this.scope = scope;
		}

		public void callback(ScriptableObject err, ScriptableObject tree) {
			callback(err, tree, null);
		}

		public void callback(ScriptableObject err, ScriptableObject tree, String content) {
			this.err = err;
			this.tree = tree;
			super.callback();
		}

		@Override
		public String toString() {
			if (err != null) {
				throw new LessCSSException(err.toString());
			} else {
				long start, end;

				log.debug("Converting to CSS... {}", filename);
				Function f = (Function) tree.get("toCSS");
				start = System.nanoTime();
				Object result = f.call(context, scope, tree, new Object[]{});
				end = System.nanoTime();
				log.trace("toCSS('{}'): {}s", filename, String.format("%.3f", (double) (end - start) / 1000000000.0d));
				return result.toString();
			}
		}
	}

}
