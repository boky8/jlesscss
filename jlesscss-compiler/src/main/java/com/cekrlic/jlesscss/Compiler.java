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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Compiles LESS input into CSS, using less.js on Mozilla Rhino.
 */
public abstract class Compiler {

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
		/*

		Until we get CompiledScripts to work properly, Rhino beats nashorn
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine;

		// See https://github.com/pose/jav8
		engine = factory.getEngineByName("jav8");
		if(engine != null) {
			return new JavaXScriptCompiler(engine);
		}

		engine = factory.getEngineByName("nashorn");
		if(engine != null) {
			return new JavaXScriptCompiler(engine);
		}
		 */


		return new RhinoCompiler();
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