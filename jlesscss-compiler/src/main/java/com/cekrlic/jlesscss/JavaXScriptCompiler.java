package com.cekrlic.jlesscss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.io.*;

/**
 * Please note. THIS COMPILER IS NOT YET IN WORKING CONDITION!
 *
 * @author boky
 * @created 5.5.2014 12:00
 */
public class JavaXScriptCompiler extends Compiler {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JavaXScriptCompiler.class);
	private static final org.slf4j.Logger log_js = org.slf4j.LoggerFactory.getLogger(RhinoCompiler.class.getName() + ".js");

	final ScriptEngine engine;
	final Invocable invocable;

	protected JavaXScriptCompiler(ScriptEngine engine) throws LessCSSException {
		super();

		if (engine == null) {
			throw new NullPointerException("You need to provide a JavaScript-compatible scripting engine!");
		}

		this.engine = engine;
		engine.put("log", log_js);

		try {
			ClassLoader classLoader = this.getClass().getClassLoader();


			/*if (engine instanceof Compilable) {
				InputStream is = null;
				try {
					for (String s : new String[]{
							"com/cekrlic/jlesscss/less-rhino.js",
							"com/cekrlic/jlesscss/process-less.js",
							"com/cekrlic/jlesscss/object.js"
					}) {
						InputStream i2 = classLoader.getResourceAsStream(s);
						log.info("Loading {}...", getName(s));
						is = is == null ? i2 : new SequenceInputStream(is, i2);
					}
					Compilable compilable = (Compilable) engine;
					log.info("Compiling...");
					CompiledScript cs = compilable.compile(new InputStreamReader(is, "UTF-8"));
					compiledScript = cs.eval();
					invocable = (Invocable) cs.getEngine();
				} finally {
					if(is != null) {
						is.close();
					}
				}
			} else {*/
				invocable = (Invocable) engine;
				// compiledScript = null;
				for (String s : new String[]{
						"com/cekrlic/jlesscss/less-rhino.js",
						"com/cekrlic/jlesscss/process-less.js"
				}) {
					try (Reader r = new InputStreamReader(classLoader.getResourceAsStream(s), "UTF-8")) {
						log.info("Loading {}", getName(s));
						engine.eval(r);
					}
				}
			/*}*/


		} catch (ScriptException | IOException ex) {
			log.error(ex.toString(), ex);

			throw new LessCSSException("Unable to initialize LESS compiler.", ex);
		}
	}

	@Override
	public String compile(Source source, Importer importer) throws LessCSSException {
		try {
			long start, end;

			final Callback callback = new Callback(source.getFileName());

			// log.info("Calling processLess for {}", source.getFileName());
			start = System.nanoTime();
			invocable.invokeFunction("processLess",
					importer,
					source.getFileName(),
					source.getContent(),
					callback
			);

			if (!callback.isComplete()) {
				log.debug("Waiting for less to finish processing for {}", source.getFileName());
				synchronized (callback) {
					callback.wait(15000);
				}
			}

			if (!callback.isComplete()) {
				throw new LessCSSException("Timed out while waiting for compilation to complete for " + source.getFileName());
			}

			end = System.nanoTime();
			log.info("Processing for {}: {}s", source.getFileName(), String.format("%.3f", (double) (end - start) / 1000000000.0d));
			start = end;

			return callback.toString();

		} catch (InterruptedException | ScriptException | NoSuchMethodException ex) {
			log.error("Error: " + ex.toString(), ex);
			throw new LessCSSException("Unable to process LESS input from " + source.getFileName(), ex);
		}
	}

	public static class Result {
		String result;

		protected Result() {
		}

		public String getResult() {
			return result;
		}

		public void setResult(String result) {
			this.result = result;
		}

		@Override
		public String toString() {
			return getResult();
		}
	}

	public class Callback extends Compiler.Callback {
		private final Logger log = LoggerFactory.getLogger(Callback.class);

		private Object err = null;
		private Object tree = null;

		public Callback(String filename) {
			super(filename);
		}

		public void callback(Object err, Object tree) {
			callback(err, tree, null);
		}

		public void callback(Object err, Object tree, String str) {
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
				try {

					log.debug("Converting to CSS... {}", filename);
					Result result = new Result();
					start = System.nanoTime();
					invocable.invokeFunction("toCss", tree, result);
					end = System.nanoTime();
					log.trace("toCSS('{}'): {}s", filename, String.format("%.3f", (double) (end - start) / 1000000000.0d));
					return result.toString();
				} catch (NoSuchMethodException | ScriptException ex) {
					throw new LessCSSException(ex.toString(), ex);
				}
			}
		}

	}

}
