package com.oeparser.trace;

import java.io.File;
import java.util.Date;

public class MainApp {
	public static void main (String [] args) {
		if (args.length != 5) {
			System.out.println("Usage java -jar tracer.jar <base dir> <source.p>|<source dir> <propath> <schema> <output.xml>");
			System.exit(0);
		}
		
		try {
			Date start   = new Date();
			File basedir = new File(args[0]);
			
			String source  = args[1];
			String propath = args[2].replaceAll(";", ",");
			
			File schema = new File(args[3]);
			File output = new File(args[4]);
			
			MainApp.run(basedir, source, propath, schema, output);
			
			System.out.println("Trace code generated in " + ((new Date().getTime() - start.getTime()) / 1000) + "s");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public static void run(File basedir, String source, String propath, File schema, File output) throws Exception {
        Tracer compiler = new Tracer(basedir, source, output, propath, schema);
        compiler.run();
        
        System.out.println("Files with errors: " + compiler.getErrorsCount());
        System.out.println("Files with success: " + compiler.getSuccessCount());
    }
}
