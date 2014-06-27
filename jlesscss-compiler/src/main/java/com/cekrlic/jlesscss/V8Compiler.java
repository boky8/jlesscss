package com.cekrlic.jlesscss;

import org.mozilla.javascript.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Please note. THIS COMPILER IS NOT YET IN WORKING CONDITION!
 *
 * @author boky
 * @created 5.5.2014 12:00
 */
public class V8Compiler extends Compiler {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(V8Compiler.class);

	final ScriptEngine engine;

	protected V8Compiler() throws LessCSSException {
		super();

		ScriptEngineManager factory = new ScriptEngineManager();
		engine = factory.getEngineByName("jav8");

		try {
			ClassLoader classLoader = this.getClass().getClassLoader();
			for (String s : new String[]{
					"com/cekrlic/jlesscss/less-rhino.js",
					"com/cekrlic/jlesscss/process-less.js"
			}) {
				try (Reader r = new InputStreamReader(classLoader.getResourceAsStream(s), "UTF-8")) {
					log.info("Loading {}", getName(s));
					engine.eval(r);
				}
			}
		} catch (ScriptException | IOException ex) {
			throw new LessCSSException("Unable to initialize LESS compiler.", ex);
		}
	}

	@Override
	protected String compile(Source source, Importer importer) throws LessCSSException {
		try {
			long start, end;

			final Callback callback = new Callback();

			log.info("Calling processLess for {}", source.getFileName());
			start = System.nanoTime();
			Invocable inv = (Invocable) engine;
			inv.invokeFunction("processLess",
					importer,
					source.getFileName(),
					source.getContent(),
					callback
			);

			synchronized (callback) {
				callback.wait(15000);
			}
			end = System.nanoTime();
			log.info("Processing for {}: {}s", source.getFileName(), String.format("%.3f", (double) (end - start) / 1000000000.0d));
			start = end;

			return callback.toString();

		} catch (InterruptedException | ScriptException | NoSuchMethodException ex) {
			throw new LessCSSException("Unable to process LESS input from " + source.getFileName(), ex);
		} finally {
			Context.exit();
		}
	}

	public static class Callback {
		private static final Logger log = LoggerFactory.getLogger(Callback.class);

		private Object err = null;
		private Object tree = null;

		public Callback() {
		}

		public void callback(Object err, Object tree) {
			callback(err, tree, null);
		}

		public void callback(Object err, Object tree, String str) {
			this.err = err;
			this.tree = tree;

			synchronized (this) {
				this.notify();
			}
		}

		@Override
		public String toString() {
			if (err != null) {
				throw new LessCSSException(err.toString());
			} else {
				long start, end;
				try {

					Invocable invocable = (Invocable) tree;
					start = System.nanoTime();
					Object result = invocable.invokeFunction("toSS");
					end = System.nanoTime();
					log.info("toCSS(): {}s", String.format("%.3f", (double) (end - start) / 1000000000.0d));
					return result.toString();
				} catch (NoSuchMethodException | ScriptException ex) {
					throw new LessCSSException(ex.toString(), ex);
				}
			}
		}

	}

}
