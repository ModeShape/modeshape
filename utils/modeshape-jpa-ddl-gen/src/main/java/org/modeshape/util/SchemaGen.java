package org.modeshape.util;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.modeshape.connector.store.jpa.JpaSource;
import org.modeshape.connector.store.jpa.Model;
import org.modeshape.connector.store.jpa.util.StoreOptionEntity;

/**
 * Main class to generate DDL that can be used to build the schema for the {@link JpaSource JPA connector}. The class is intended
 * to be bundled with scripts that are to be invoked with the following syntax:
 * 
 * <pre>
 * ddl-gen.sh -dialect &lt;dialect name&gt; -model &lt;model_name&gt; [-out &lt;path to output directory&gt;] [-delimiter &lt;delim&gt;] [-newline]
 *     Example: ddl-gen.sh -dialect HSQL -model Simple -out /tmp
 * </pre>
 */
public class SchemaGen {

	protected static Logger logger = Logger.getLogger("org.modeshape.util"); //$NON-NLS-1$
	
    public static final String CREATE_FILE_NAME = "create.modeshape-jpa-connector.ddl";
    public static final String DROP_FILE_NAME = "drop.modeshape-jpa-connector.ddl";
    
    private final Dialect dialect;
    private final Model model;
    private final File outputPath;
    // delimiter is what the statement separater should be.  Default is what SchemaExport uses"
    private final String delimiter;
    // newLine indicates that a new line should occur after the statement, and before the delimiter.
    private boolean newLineAfterStatement = false;
    
	public SchemaGen(String dialect, String model, File outputPath) {

		this.dialect = dialectFor(dialect);
		this.delimiter = null;

		this.model = JpaSource.Models.getModel(model);
		if (this.model == null) {
			throw new RuntimeException(JpaDdlGenI18n.invalidModel.text());
		}
		this.outputPath = outputPath;

		if (this.outputPath != null && !this.outputPath.exists()) {
			this.outputPath.mkdirs();

			String logMsg = JpaDdlGenI18n.directoryLocationCreated
					.text(this.outputPath.getAbsolutePath()); //$NON-NLS-1$
			logger.log(Level.INFO, logMsg);

		}
	}

    public SchemaGen( String dialect,
                      String model,
                      File outputPath,
                      String delimiter) {

        this.dialect = dialectFor(dialect);
        this.delimiter = delimiter;

        this.model = JpaSource.Models.getModel(model);
		if (this.model == null) {
			throw new RuntimeException(JpaDdlGenI18n.invalidModel.text());
		}
        this.outputPath = outputPath;

        if (this.outputPath != null && !this.outputPath.exists()) {
        	this.outputPath.mkdirs();
        	
            String logMsg = JpaDdlGenI18n.directoryLocationCreated.text(this.outputPath.getAbsolutePath()); //$NON-NLS-1$
            logger.log(Level.INFO, logMsg);

        }
    }
    
    public void setNewLineAfterStatement(boolean newLine) {
    	this.newLineAfterStatement = newLine;
    }

    /**
     * Returns the {@link Dialect Hibernate dialect} that corresponds to the provided name.
     * <p>
     * This method will tolerate certain kinds of abbreviations for the dialect. Since all Hibernate dialects have a name equal to
     * their fully-qualified name, all valid dialect names start with &quot;org.hibernate.dialect.&quot;. If the given {@code
     * dialectName} does not begin with this string, this string will be prepended.
     * </p>
     * <p>
     * Additionally, all Hibernate dialect names end in &quot;Dialect&quot;. If the given dialect name does not end with
     * &quot;Dialect&quot;, this will be appended to the dialect name before the Hibernate dialect is looked up.
     * </p>
     * <p>
     * The net effect of these rules is that callers wishing to retrieve a dialect (e.g., org.hibernate.dialect.HSQLDialect) can
     * specify the dialect as "org.hibernate.dialect.HSQLDialect", "HSQLDialect", "org.hibernate.dialect.HSQL", or just "HSQL".
     * </p>
     * 
     * @param dialectName the name of the {@code Dialect} to return
     * @return the {@link Dialect Hibernate dialect} that corresponds to the provided name.
     */
    private Dialect dialectFor( String dialectName ) {
        Properties props = new Properties();

        if (!dialectName.endsWith("Dialect")) dialectName += "Dialect";
        if (!dialectName.startsWith("org.hibernate.dialect.")) dialectName = "org.hibernate.dialect." + dialectName;

        props.put(Environment.DIALECT, dialectName);

        return Dialect.getDialect(props);
    }

    /**
     * Writes {@link SchemaExport#create(boolean, boolean) create} and {@link SchemaExport#drop(boolean, boolean) drop} DDL files
     * to the given {@link #outputPath output directory} or the current directory if no output directory was provided.
     * 
     * @throws IOException
     */
    void generate() throws IOException {
        Ejb3Configuration configurator = new Ejb3Configuration();
        configurator.setProperty(Environment.DIALECT, dialect.toString());
        model.configure(configurator);
        configurator.addAnnotatedClass(StoreOptionEntity.class);

        // cfg.setProperties(properties);
        SchemaExport export = new SchemaExport(configurator.getHibernateConfiguration());
        export.setOutputFile(new File(outputPath, CREATE_FILE_NAME).getCanonicalPath());
        if (this.delimiter != null ) {
        	export.setDelimiter( (this.newLineAfterStatement ? ("\r" + delimiter) : delimiter) );
        } else if (this.newLineAfterStatement) {
        	export.setDelimiter("\r");
        }
        export.execute(false, false, false, true);

        export.setOutputFile(new File(outputPath, DROP_FILE_NAME).getCanonicalPath());
        export.drop(false, false);
    }

    public static final String USAGE = "./ddl-gen.sh -dialect <dialect name> -model <model_name> [-out <path to output directory>] [-delimiter <delim>] [-newline]\n"
                                       + "\tExample: ./ddl-gen.sh -dialect HSQL -model Simple -out /tmp  -delimiter % -newline ";

    public static void main( String[] args ) throws IOException {
        String modelName = null;
        String dialectName = null;
        File outputFile = new File(".");
        String delim = null;
        boolean newline = false;

        int i = 0;
        while (i < args.length) {
            if ("-dialect".equals(args[i])) {
                if (i == args.length - 1) printUsage();
                dialectName = args[++i];
            } else if ("-model".equals(args[i])) {
                if (i == args.length - 1) printUsage();
                modelName = args[++i];
            } else if ("-out".equals(args[i])) {
                if (i == args.length - 1) printUsage();
                outputFile = new File(args[++i]);
            } else if ("-delimiter".equals(args[i])) {
                if (i == args.length - 1) printUsage();                
                delim =args[++i];
            } else if ("-newline".equals(args[i])) {
            	newline = true;
            } else if ("-help".equals(args[i]) || "-?".equals(args[i])) {
                printUsage();
            }
            i++;
        }

        if (modelName == null || dialectName == null) printUsage();

        SchemaGen main = new SchemaGen(dialectName, modelName, outputFile, delim);
        main.setNewLineAfterStatement(newline);
        main.generate();
    }

    /**
     * Print the usage message and exit with a non-zero return code.
     */
    private static void printUsage() {
        System.err.println(USAGE);
        System.exit(1);
    }

}
