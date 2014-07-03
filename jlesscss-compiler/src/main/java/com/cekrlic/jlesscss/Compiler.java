/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cekrlic.jlesscss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Compiles LESS input into CSS, using less.js on Mozilla Rhino.
 */
public abstract class Compiler {
	private static final Logger log = LoggerFactory.getLogger(Compiler.class);

	/**
	 * Create a new LESS compiler.
	 *
	 * @throws LessCSSException If there was a problem initializing the compiler.
	 */
	protected Compiler() throws LessCSSException {
	}

	/**
	 * Compile stuff.
	 * @param source The LESS source; imported is deducted from the source.
	 * @return The compiled CSS.
	 * @throws LessCSSException
	 */
	public String compile(Source source) throws LessCSSException {
		return compile(source, source.getImporter());
	}

	/**
	 * Compile stuff.
	 * @param source The LESS source; imported is deducted from the source.
	 * @param importer The importer class which is called when coming across <tt>@import</tt> statements. May be <tt>null</tt>
	 *   if you don't expect to see such statements.
	 * @return The compiled CSS.
	 * @throws LessCSSException
	 */
	public abstract String compile(Source source, Importer importer) throws LessCSSException;


	protected String getName(String s) {
		return s.substring(s.lastIndexOf('/') + 1);
	}


	public static Compiler getCompiler() throws LessCSSException {

		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine;

		// See https://github.com/pose/jav8
		engine = factory.getEngineByName("jav8");
		if(engine != null) {
			return new JavaXScriptCompiler(engine);
		}
		log.info("V8 engine not found. Will try Rhino next.");

		try {
			Class.forName("org.mozilla.javascript.Context");
			return new RhinoCompiler();
		} catch (ClassNotFoundException e) {
			// ignore
		}

		log.info("Rhino not found in class path. Will try Nashorn. Please note that tests have shown Nashorn to be slower than Rhino.");

		engine = factory.getEngineByName("nashorn");
		if(engine != null) {
			return new JavaXScriptCompiler(engine);
		}

		throw new LessCSSException("No JavaScript engine found in classpath! Either install Java 8 or include Rhino 1.7.R4 / jav8 in your classpath!");
	}

	/**
	 * Callback is called directly from the script when parsing is complete.
	 */
	public static class Callback {
		private static final Logger log = LoggerFactory.getLogger(Callback.class);
		protected final String filename;
		protected final AtomicBoolean complete = new AtomicBoolean(false);

		public Callback(String filename) {
			this.filename = filename;
		}

		public boolean isComplete() {
			synchronized (complete) {
				return complete.get();
			}
		}

		protected void callback() {
			synchronized (complete) {
				complete.set(true);
			}

			log.debug("Parsing complete for {}", filename);
			synchronized (this) {
				this.notifyAll();
			}
		}


	}

}