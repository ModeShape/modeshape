package org.modeshape.util;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.modeshape.connector.store.jpa.JpaSource;
import org.modeshape.connector.store.jpa.Model;

/**
 * Main class to generate DDL that can be used to build the schema for the {@link JpaSource JPA connector}. The class is intended
 * to be bundled into an executable jar file and invoked with the following syntax:
 * 
 * <pre>
 * java -jar &lt;jar_name&gt; -dialect &lt;dialect name&gt; -model &lt;model_name&gt; [-out &lt;path to output directory&gt;]
 *     Example: java -jar dna-jpa-ddl-gen-0.7-jar-with-dependencies.jar -dialect HSQL -model Basic -out /tmp
 * </pre>
 */
public class SchemaGen {

    public static final String CREATE_FILE_NAME = "create.dna-jpa-connector.ddl";
    public static final String DROP_FILE_NAME = "drop.dna-jpa-connector.ddl";

    private final Dialect dialect;
    private final Model model;
    private final File outputPath;

    public SchemaGen( String dialect,
                      String model,
                      File outputPath ) {

        this.dialect = dialectFor(dialect);
        this.model = JpaSource.Models.getModel(model);
        this.outputPath = outputPath;
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
        // cfg.setProperties(properties);
        SchemaExport export = new SchemaExport(configurator.getHibernateConfiguration());
        export.setOutputFile(new File(outputPath, CREATE_FILE_NAME).getCanonicalPath());
        export.create(false, false);

        export.setOutputFile(new File(outputPath, DROP_FILE_NAME).getCanonicalPath());
        export.drop(false, false);
    }

    public static final String USAGE = "java -jar <jar_name> -dialect <dialect name> -model <model_name> [-out <path to output directory>]\n"
                                       + "\tExample: java -jar dna-jpa-ddl-gen-0.7-jar-with-dependencies.jar -dialect HSQL -model Basic -out /tmp";

    public static void main( String[] args ) throws IOException {
        String modelName = null;
        String dialectName = null;
        File outputFile = new File(".");

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
            } else if ("-help".equals(args[i]) || "-?".equals(args[i])) {
                printUsage();
            }
            i++;
        }

        if (modelName == null || dialectName == null) printUsage();

        SchemaGen main = new SchemaGen(dialectName, modelName, outputFile);
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
