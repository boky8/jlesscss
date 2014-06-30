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

	protected abstract String compile(Source source, Importer importer) throws LessCSSException;


	protected String getName(String s) {
		return s.substring(s.lastIndexOf('/') + 1);
	}


	public static Compiler getCompiler() throws LessCSSException {
		// TODO: Check if V8 compiler is available and use that one
		return new RhinoCompiler();
	}

}