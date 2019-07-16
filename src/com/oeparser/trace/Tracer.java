package com.oeparser.trace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.prorefactor.core.JPNode;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.treeparser.ParseUnit;

import com.joanju.proparse.NodeTypes;

public class Tracer {
	private File   basedir;
	private String input;
	private File   output;
	
	private String propath;
	private File   schema;
	
	private BufferedWriter writer;
	private List<String>   trace;
	private Map<String,Map<Integer,List<JPNode>>> sources;
	
	private static final String   PROJECT_NAME   = "default";
	private static final String[] EXTENSIONS     = new String[] { "p", "py", "w", "cls" };
	private static final int[]    NON_EXECUTABLE = new int[] { NodeTypes.USING, NodeTypes.DEFINE, 
			NodeTypes.DO, NodeTypes.PROCEDURE, NodeTypes.PERIOD, NodeTypes.REPEAT, NodeTypes.METHOD, 
			NodeTypes.FUNCTION, NodeTypes.CLASS, NodeTypes.CONSTRUCTOR, NodeTypes.INTERFACE,
			NodeTypes.CATCH, NodeTypes.FINALLY, NodeTypes.DESTRUCTOR, NodeTypes.BLOCKLEVEL};
	
	private int errors  = 0;
	private int success = 0;
	
	public Tracer (File basedir, String input, File output, String propath, File schema) throws Exception {
		this.basedir = basedir;
		this.input   = input;
		this.output  = output;
		
		this.propath = propath;
		this.schema  = schema;
		
		this.trace   = new ArrayList<String>();
		this.sources = new HashMap<String,Map<Integer,List<JPNode>>>();
	}
	
	public void generateProjectFiles() throws IOException {
		new File("./prorefactor/projects/" + Tracer.PROJECT_NAME + "/").mkdirs();
		new File("./prorefactor/").deleteOnExit();
		
		BufferedWriter progress = new BufferedWriter(new FileWriter(new File("./prorefactor/projects/" + Tracer.PROJECT_NAME + "/progress.properties")));
		String schemaPath = schema.getAbsolutePath();
		
		propath = propath.replaceAll("\\\\\\\\", "//");
		propath = propath.replaceAll("\\\\", "/");
		
		schemaPath = schemaPath.replaceAll("\\\\\\\\", "//");
		schemaPath = schemaPath.replaceAll("\\\\", "/");
		
		progress.write("batch_mode=false\n");
		progress.write("opsys=WIN32\n");
		progress.write("proversion=11.6\n");
		progress.write("window_system=MS-WINXP\n");
		progress.write("propath=" + propath + "\n");
		progress.write("database_aliases=\n");
		progress.close();

		BufferedWriter proparse = new BufferedWriter(new FileWriter(new File("./prorefactor/projects/" + Tracer.PROJECT_NAME + "/proparse.properties")));
		proparse.write("schema_file=" + schemaPath + "\n");
		proparse.close();
	}
	
	public void removeProjectFiles() {
		this.removeFile(new File("./prorefactor"));
	}
	
	private void removeFile(File file) {
		if (file.isDirectory()) {
			String[] subfiles = file.list();
        
        	if (subfiles != null) {
        		for (String subfile : subfiles)
        			this.removeFile(new File(file.getAbsolutePath() + File.separator  + subfile));
        	}
        }
        
        file.delete();
	}
	
	private void log(String msg) {
		BufferedWriter log;
		try {
			log = new BufferedWriter(new FileWriter(new File("./prorefactor/oeTrace.log"), true));
			log.write(msg + "\n");
			log.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run() throws Exception {
		errors  = 0;
		success = 0;
		
		trace.clear();
		
		if(output != null){
			writer = new BufferedWriter(new FileWriter(output));
			writer.write("<coverage version=\"1\">\n");
		}
		
		this.generateProjectFiles();
		
		RefactorSession session = RefactorSession.getInstance();
		session.loadProject(Tracer.PROJECT_NAME);
		
		String[] split = input.split(",");
		
		for (int i = 0; i < split.length; i++) {
			File source = new File(basedir.getCanonicalPath() + File.separator + split[i]);
			System.out.println("Reading source " + source.getCanonicalPath()); 
			
			try {
				this.parseSource(source);
			} catch(Exception e) {
				e.printStackTrace();
				errors++;
	    	}
		}
		
		this.removeProjectFiles();
		
		if(output != null){
			writer.write("</coverage>\n");
			writer.close();
		}
		
		success = trace.size();
	}
	
	public void parseSource(File file) throws Exception {
		if (file.getName().endsWith("menuProgramData.p") ||
			file.getName().endsWith("btb929zb.p")) {
			return;
		}
		
		if (!file.isFile()) {
			String[] subfiles = file.list();
            
            if (subfiles != null) {
                for (String subfile : subfiles){
                	try {
                		this.parseSource(new File(file.getAbsolutePath() + File.separator  + subfile));
                	} catch(Exception e) {
               			e.printStackTrace();
                		errors++;
                	}
                }
            }
		} else {
			String ext = "";

			int i = file.getPath().lastIndexOf('.');
			if (i > 0) ext = file.getPath().substring(i + 1).toLowerCase();
			
			boolean v = false;
			
			for (int j = 0; j < Tracer.EXTENSIONS.length && !v; j++)
				if(ext.matches(Tracer.EXTENSIONS[j] + "[0-9]*")) v = true;
			
			if (!v) return;
			
			System.out.println("Parsing file " + file.getName());
			
			ParseUnit parser = new ParseUnit(file.getCanonicalFile());
			parser.loadOrBuildPUB();
			parser.parse();
			
			sources.clear();
			
			JPNode root = parser.getTopNode();
			read(root, file.getName());
			
			for (String source : sources.keySet()) {
				Map<Integer,List<JPNode>> map = sources.get(source);
				String path = source.replaceAll(basedir.getCanonicalPath().replaceAll("\\\\","\\\\\\\\"),"").substring(1);
				
				writer.write("  <file path=\"" + path + "\">\n");
				
				for (Integer line : map.keySet()) {
					List<JPNode> commands = map.get(line);
				
					JPNode node = commands.get(0);
					int    type = node.getType();
						
					if (this.containsType(type)) continue;
						
					writer.write("    <lineToCover lineNumber=\"" + line + "\" covered=\"false\"/>\n");
				}
				
				writer.write("  </file>\n");
				
				trace.add(source);
			}
		}
	}
	
	public int getErrorsCount() {
		return errors;
	}

	public int getSuccessCount() {
		return success;
	}

	public void read(JPNode node, String name) throws IOException {
		String source = new File(node.getFilename()).getCanonicalPath();
		if (!source.startsWith(basedir.getCanonicalPath())) return;
		
		if (trace.contains(source)) return;
		
		Map<Integer,List<JPNode>> map = sources.get(source); 
		
		if (map == null) {
			map = new TreeMap<Integer,List<JPNode>>();
			sources.put(source, map);
		}

		if (node.isNatural()) {
			if (node.getStatement() != null) {
				if (node.getLine() > 0) {
					int line = node.getLine();
					
					if (node.getStatement().getFileIndex() == node.getFileIndex() && 
						node.getStatement().getLine() != 0) {
						line = node.getStatement().getLine();
					}

					List<JPNode> commands = map.get(line);

					if (commands == null) {
						commands = new ArrayList<JPNode>();
						map.put(line, commands);
					}

					commands.add(node);
				}
			}
		}

		for (JPNode child : node.getDirectChildren())
			read(child, name);
	}
	
	public boolean containsType(int type) {
		for (int i = 0; i < NON_EXECUTABLE.length; i++) {
			if (NON_EXECUTABLE[i] == type) return true;
		}
		
		return false;
	}
}