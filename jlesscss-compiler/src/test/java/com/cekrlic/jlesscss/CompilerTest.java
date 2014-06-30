package com.cekrlic.jlesscss;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author boky
 * @created 30.6.2014 12:28
 */
@Test
public class CompilerTest {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CompilerTest.class);

	private ClassLoader cl;
	private List<Compiler> compilers;

	@BeforeClass
	public void findClassloader() {
		cl = this.getClass().getClassLoader();
	}

	@BeforeClass
	public void findCompilers() {
		compilers = new ArrayList<>();

		compilers.add(new RhinoCompiler());

		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine;

		// See https://github.com/pose/jav8
		engine = factory.getEngineByName("jav8");
		if(engine != null) {
			compilers.add(new JavaXScriptCompiler(engine));
		}

		engine = factory.getEngineByName("nashorn");
		if(engine != null) {
			compilers.add(new JavaXScriptCompiler(engine));
		}
	}

	@DataProvider(name = "sources")
	public Object[][] getSources() throws IOException {
		return new Object[][]{
				{new UriSource(cl.getResource("simple.less"))},
				{new UriSource(cl.getResource("variables.less"))},
				{new UriSource(cl.getResource("functions.less"))}
		};
	}

	@Test(dataProvider = "sources")
	public void compileTest(Source source) {
		for (Compiler compiler : compilers) {
			compiler.compile(source);
		}


	}


}
