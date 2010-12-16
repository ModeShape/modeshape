/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.test.integration.performance;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.property.Path;
import org.modeshape.test.integration.AbstractAdHocModeShapeTest;

public class JcrRepositoryPerformanceTest extends AbstractAdHocModeShapeTest {

    private static final int NUMBER_OF_COPIES = 150;
    private static boolean USE_SEPARATE_SESSIONS = true;
    private boolean printDetail = false;

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        printDetail = false;
    }

    @Ignore( "Removed from automatic builds. Can be run manually." )
    @Test
    public void shouldSimulateGuvnorUsageAgainstRepositoryWithInMemoryStore() throws Exception {
        print = true;
        startEngine("config/configRepositoryForDroolsInMemoryPerformance.xml", "Repo");
        assertNode("/", "mode:root");
        // import the file ...
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml");
        session().refresh(false);

        // Verify the file was imported ...
        withSession(new VerifyContent());

        simulateGuvnorUsage();
    }

    @Ignore( "Removed from automatic builds. Can be run manually." )
    @Test
    public void shouldSimulateGuvnorUsageAgainstRepositoryWithJpaStore() throws Exception {
        print = true;
        startEngine("config/configRepositoryForDroolsJpaPerformance.xml", "Repo");
        assertNode("/", "mode:root");
        // import the file ...
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml");
        session().refresh(false);

        // Verify the file was imported ...
        withSession(new VerifyContent());

        simulateGuvnorUsage();
    }

    protected void simulateGuvnorUsage() throws Exception {

        // for (int i = 0; i != 30; ++i) {
        // // Create a snapshot ...
        // String snapshotName = i <= NUMBER_OF_COPIES ? "TEST" + i : "TEST15";
        // withSession(new CreatePackageSnapshot("mortgages", snapshotName, "My TEST snapshot"));
        // }

        Stopwatch sw = new Stopwatch(false, "Iteration");
        Stopwatch total = new Stopwatch(true, "Total usage");
        Stopwatch sw15 = new Stopwatch(true, "First " + NUMBER_OF_COPIES);
        Stopwatch swRest = new Stopwatch(true, "Remaining");
        for (int i = 0; i != 30; ++i) {
            sw.start();
            total.start();
            if (i <= NUMBER_OF_COPIES) sw15.start();
            else swRest.start();

            // Navigate (with separate sessions for each step) the "ApplicantDsl" technical asset ...
            browseTo("/drools:repository/drools:package_area/mortgages/assets/ApplicantDsl");

            // Now modify the asset a number of times ...
            repeatedlyWithSession(1, new ModifyAsset("/drools:repository/drools:package_area/mortgages/assets/ApplicantDsl"));

            // Open the "mortgages" package ...
            browseTo("/drools:repository/drools:package_area/mortgages");

            // View the source ...
            ViewContent viewContent = new ViewContent("/drools:repository/drools:package_area/mortgages");
            withSession(viewContent);
            printDetail(viewContent.getContent());

            // Save and validate ...

            // Build the package ...
            withSession(new BuildPackage("mortgages"));

            // Create a snapshot ...
            String snapshotName = i <= NUMBER_OF_COPIES ? "TEST" + i : "TEST15";
            withSession(new CreatePackageSnapshot("mortgages", snapshotName, "My TEST snapshot"));
            withSession(new LoadPackageSnapshot("mortgages", snapshotName));

            // Package p = guvnor.openPackage("mortgages");
            // p.viewSource();
            // p.saveAndValidate();
            // p.build();
            // p.createSnapshot("TEST", null, "My TEST Snapshot");

            sw.stop();
            total.stop();
            if (i <= NUMBER_OF_COPIES) sw15.stop();
            else swRest.stop();
            print(StringUtil.justifyRight("" + i, 3, ' ') + " " + sw);
            sw.reset();

            // withSession(new CountNodes());
        }
        if (sw15.getCount() != total.getCount()) print(sw15);
        if (swRest.getCount() > 0) print(swRest);
        print(total);
        // withSession(new PrintNodes());
    }

    protected void repeatedlyWithSession( int times,
                                          Operation operation ) throws Exception {
        for (int i = 0; i != times; ++i) {
            double time = withSession(operation);
            printDetail("Time to execute \"" + operation.getClass().getSimpleName() + "\": " + time + " ms");
        }
    }

    protected void browseTo( String path ) throws Exception {
        double time = 0.0d;
        for (Iterator<Path> iterator = path(path).pathsFromRoot(); iterator.hasNext();) {
            Path p = iterator.next();
            time += withSession(new BrowseContent(string(p)));
        }
        printDetail("Time to browse down to \"" + path + "\": " + time + " ms");
    }

    protected Path path( String path ) {
        return engine.getExecutionContext().getValueFactories().getPathFactory().create(path);
    }

    protected String string( Object obj ) {
        return engine.getExecutionContext().getValueFactories().getStringFactory().create(obj);
    }

    protected void print( Object msg ) {
        if (print && msg != null) {
            System.out.println(msg.toString());
        }
    }

    protected void printDetail( Object msg ) {
        if (printDetail && msg != null) {
            System.out.println(msg.toString());
        }
    }

    protected double withSession( Operation operation ) throws Exception {
        long startTime = System.nanoTime();
        Session oldSession = session();
        Session session = USE_SEPARATE_SESSIONS ? repository.login() : oldSession;
        try {
            operation.run(session);
        } finally {
            if (oldSession != null) setSession(oldSession);
            if (oldSession != session) session.logout();
        }
        return TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }

    protected interface Operation {
        public void run( Session session ) throws Exception;
    }

    protected abstract class BasicOperation implements Operation {
        protected Node assertNode( Session session,
                                   String path,
                                   String primaryType,
                                   String... mixinTypes ) throws RepositoryException {
            Node node = session.getNode(path);
            assertThat(node.getPrimaryNodeType().getName(), is(primaryType));
            Set<String> expectedMixinTypes = new HashSet<String>(Arrays.asList(mixinTypes));
            Set<String> actualMixinTypes = new HashSet<String>();
            for (NodeType mixin : node.getMixinNodeTypes()) {
                actualMixinTypes.add(mixin.getName());
            }
            assertThat("Mixin types do not match", actualMixinTypes, is(expectedMixinTypes));
            return node;
        }
    }

    protected class VerifyContent extends BasicOperation {
        public void run( Session s ) throws Exception {
            // Verify the file was imported ...
            assertNode(s, "/drools:repository", "nt:folder");
            assertNode(s, "/drools:repository/drools:package_area", "nt:folder");
            assertNode(s, "/drools:repository/drools:package_area/mortgages", "drools:packageNodeType");
            assertNode(s, "/drools:repository/drools:package_area/mortgages/assets", "drools:versionableAssetFolder");
        }
    }

    protected class BrowseContent extends DroolsOperation {
        private String path;

        public BrowseContent( String path ) {
            this.path = path;
        }

        public void run( Session s ) throws RepositoryException {
            // Verify the file was imported ...
            Node node = s.getNode(path);
            assertThat(node, is(notNullValue()));
        }

    }

    protected class CountNodes extends DroolsOperation {
        public void run( Session s ) throws RepositoryException {
            // Count the nodes below the root, excluding the '/jcr:system' branch ...
            String queryStr = "SELECT [jcr:primaryType] FROM [nt:base]";
            Query query = s.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
            long numNonSystemNodes = query.execute().getRows().getSize();
            print("  # nodes NOT in '/jcr:system' branch: " + numNonSystemNodes);
        }
    }

    protected class PrintNodes extends DroolsOperation {
        public void run( Session s ) throws RepositoryException {
            // Count the nodes below the root, excluding the '/jcr:system' branch ...
            String queryStr = "SELECT [jcr:path] FROM [nt:base] ORDER BY [jcr:path]";
            Query query = s.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
            print(query.execute());
        }
    }

    protected class ViewContent extends DroolsOperation {
        private String path;
        private String content;

        public ViewContent( String path ) {
            this.path = path;
        }

        public void run( Session s ) throws Exception {
            // Verify the file was imported ...
            Node node = s.getNode(path);
            assertThat(node, is(notNullValue()));
            content = readBinaryContentAttachment(node);
        }

        public String getContent() {
            return content;
        }
    }

    protected class ModifyAsset extends DroolsOperation {
        private String path;

        public ModifyAsset( String path ) {
            this.path = path;
        }

        public void run( Session s ) throws RepositoryException {
            // Verify the file was imported ...
            Node assetNode = s.getNode(path);
            checkout(assetNode);
            updateDescription(assetNode, "This is the new description");
            checkin(assetNode, "First change");
        }
    }

    protected class CreatePackageSnapshot extends DroolsOperation {
        private String packageName;
        private String snapshotName;
        private String comment;

        public CreatePackageSnapshot( String packageName,
                                      String snapshotName,
                                      String comment ) {
            this.packageName = packageName;
            this.snapshotName = snapshotName;
            this.comment = comment;
        }

        public void run( Session s ) throws RepositoryException {
            createPackageSnapshot(s, packageName, snapshotName);
            Node pkgItem = loadPackageSnapshot(s, packageName, snapshotName);
            if (comment != null) {
                updateCheckinComment(pkgItem, comment);
            }
            s.save(); // same as RulesRepository.save()
        }
    }

    protected class LoadPackageSnapshot extends DroolsOperation {
        private String packageName;
        private String snapshotName;

        public LoadPackageSnapshot( String packageName,
                                    String snapshotName ) {
            this.packageName = packageName;
            this.snapshotName = snapshotName;
        }

        public void run( Session s ) throws RepositoryException {
            loadPackageSnapshot(s, packageName, snapshotName);
        }
    }

    protected class BuildPackage extends DroolsOperation {
        private String packageName;

        public BuildPackage( String packageName ) {
            this.packageName = packageName;
        }

        public void run( Session s ) throws RepositoryException, IOException {
            buildPackage(s, packageName);
            getPackageAssets(s, packageName);
        }
    }

    public abstract class DroolsOperation extends BasicOperation {

        /**
         * Property names for this node type.
         */
        public static final String TITLE_PROPERTY_NAME = "drools:title";
        public static final String DESCRIPTION_PROPERTY_NAME = "drools:description";
        public static final String LAST_MODIFIED_PROPERTY_NAME = "drools:lastModified";
        public static final String FORMAT_PROPERTY_NAME = "drools:format";
        public static final String CHECKIN_COMMENT = "drools:checkinComment";
        public static final String VERSION_NUMBER_PROPERTY_NAME = "drools:versionNumber";
        public static final String CONTENT_PROPERTY_ARCHIVE_FLAG = "drools:archive";
        public static final String LAST_CONTRIBUTOR_PROPERTY_NAME = "drools:lastContributor";
        public static final String CONTENT_PROPERTY_NAME = "drools:content";
        public static final String CONTENT_PROPERTY_BINARY_NAME = "drools:binaryContent";
        public static final String CONTENT_PROPERTY_ATTACHMENT_FILENAME = "drools:attachmentFileName";
        public static final String PACKAGE_AREA = "drools:package_area";
        public static final String PACKAGE_SNAPSHOT_AREA = "drools:packagesnapshot_area";
        public static final String ASSET_FOLDER_NAME = "assets";

        public VersionManager versionMgr( Node versionable ) throws RepositoryException {
            return versionable.getSession().getWorkspace().getVersionManager();
        }

        public void checkout( Node versionable ) throws RepositoryException {
            versionMgr(versionable).checkout(versionable.getPath());
        }

        public void checkin( Node versionable,
                             String comment ) throws RepositoryException {
            versionable.setProperty(LAST_MODIFIED_PROPERTY_NAME, Calendar.getInstance());
            updateCheckinComment(versionable, comment);
            versionable.setProperty(LAST_CONTRIBUTOR_PROPERTY_NAME, versionable.getSession().getUserID());
            long nextVersion = versionNumber(versionable) + 1;
            versionable.setProperty(VERSION_NUMBER_PROPERTY_NAME, nextVersion);
            versionable.getSession().save();

            versionMgr(versionable).checkin(versionable.getPath());
        }

        public void updateCheckinComment( Node versionable,
                                          String comment ) throws RepositoryException {
            versionable.setProperty(CHECKIN_COMMENT, comment);
        }

        public Calendar lastModified( Node versionable ) throws RepositoryException {
            if (versionable.hasProperty(LAST_MODIFIED_PROPERTY_NAME)) {
                Property lastModifiedProperty = versionable.getProperty(LAST_MODIFIED_PROPERTY_NAME);
                return lastModifiedProperty.getDate();
            }
            return null;
        }

        public long versionNumber( Node versionable ) throws RepositoryException {
            return longProperty(versionable, VERSION_NUMBER_PROPERTY_NAME);
        }

        public long longProperty( Node theNode,
                                  String propertyName ) throws RepositoryException {
            if (theNode.hasProperty(propertyName)) {
                Property data = theNode.getProperty(propertyName);
                return data.getValue().getLong();
            }
            return 0;
        }

        public void updateDescription( Node versionable,
                                       String newDescriptionContent ) throws RepositoryException {
            checkout(versionable);
            versionable.setProperty(DESCRIPTION_PROPERTY_NAME, newDescriptionContent);
            Calendar lastModified = Calendar.getInstance();
            versionable.setProperty(LAST_MODIFIED_PROPERTY_NAME, lastModified);
        }

        public boolean isBinary( Node node ) throws RepositoryException {
            return node.hasProperty(CONTENT_PROPERTY_BINARY_NAME);
        }

        public boolean isArchived( Node node ) throws RepositoryException {
            return node.hasProperty(CONTENT_PROPERTY_ARCHIVE_FLAG);
        }

        public String getAssetFormat( Node node ) throws RepositoryException {
            return node.hasProperty(FORMAT_PROPERTY_NAME) ? node.getProperty(FORMAT_PROPERTY_NAME).getString() : null;
        }

        public String readBinaryContentAttachment( Node assetNode ) throws RepositoryException, IOException {
            if (assetNode.hasProperty(CONTENT_PROPERTY_BINARY_NAME)) {
                Property data = assetNode.getProperty(CONTENT_PROPERTY_BINARY_NAME);
                return IoUtil.read(data.getBinary().getStream());
            }
            if (assetNode.hasProperty(CONTENT_PROPERTY_NAME)) {
                Property data = assetNode.getProperty(CONTENT_PROPERTY_NAME);
                return IoUtil.read(data.getBinary().getStream());
            }
            return null;
        }

        public Node area( Session session,
                          String areaName ) throws RepositoryException {
            return session.getRootNode().getNode("drools:repository").getNode(areaName);
        }

        public void removePackageSnapshot( Session session,
                                           String packageName,
                                           String snapshotName ) throws RepositoryException {
            Node snapshotArea = area(session, PACKAGE_SNAPSHOT_AREA);
            Node snapshotPackage = null;
            if (snapshotArea.hasNode(packageName)) {
                snapshotPackage = snapshotArea.getNode(packageName);
            } else {
                snapshotPackage = snapshotArea.addNode(packageName, "nt:folder");
                session.save();
            }
            if (snapshotPackage.hasNode(snapshotName)) {
                // remove the existing node ...
                snapshotPackage.getNode(snapshotName).remove();
                session.save();
            }
        }

        public void createPackageSnapshot( Session session,
                                           String packageName,
                                           String snapshotName ) throws RepositoryException {
            Node packageNode = area(session, PACKAGE_AREA).getNode(packageName);
            Node snapshotArea = area(session, PACKAGE_SNAPSHOT_AREA);
            Node snapshotPackage = null;
            if (snapshotArea.hasNode(packageName)) {
                snapshotPackage = snapshotArea.getNode(packageName);
            } else {
                snapshotPackage = snapshotArea.addNode(packageName, "nt:folder");
                session.save();
            }
            if (snapshotPackage.hasNode(snapshotName)) {
                // remove the existing node ...
                snapshotPackage.getNode(snapshotName).remove();
                session.save();
            }
            // Make the snapshot ...
            String newName = snapshotPackage.getPath() + "/" + snapshotName;
            long start = System.currentTimeMillis();
            session.getWorkspace().copy(packageNode.getPath(), newName);
            printDetail("Time taken for snap: " + (System.currentTimeMillis() - start));
        }

        public Node loadPackageSnapshot( Session session,
                                         String packageName,
                                         String snapshotName ) throws RepositoryException {
            Node snapshotArea = area(session, PACKAGE_SNAPSHOT_AREA);
            return snapshotArea.getNode(packageName).getNode(snapshotName);
        }

        public void buildPackage( Session session,
                                  String packageName ) throws RepositoryException, IOException {
            List<Node> assets = listAssets(session, packageName, "function");
            long time = System.currentTimeMillis();
            for (Node assetNode : assets) {
                if (isBinary(assetNode)) {
                    readBinaryContentAttachment(assetNode);
                } else {
                    if (assetNode.hasProperty(CONTENT_PROPERTY_NAME)) {
                        Property data = assetNode.getProperty(CONTENT_PROPERTY_NAME);
                        data.getValue().getString();
                    }
                }
            }

            List<Node> drls = listAssets(session, packageName, "drl");
            for (Node drlNode : drls) {
                if (!isArchived(drlNode)) {
                    // build asset, which appears to be just processing the content ...
                    readBinaryContentAttachment(drlNode);
                }
            }
            List<Node> allAssets = getPackageAssets(session, packageName);
            for (Node nonDrlNode : allAssets) {
                if (!"drl".equals(getAssetFormat(nonDrlNode)) && !isArchived(nonDrlNode)) {
                    // build asset, which appears to be just processing the content ...
                    readBinaryContentAttachment(nonDrlNode);
                }
            }

            long taken = System.currentTimeMillis() - time;
            printDetail("Package build time is: " + taken);
        }

        @SuppressWarnings( "deprecation" )
        public List<Node> listAssets( Session session,
                                      String packageName,
                                      String format ) throws RepositoryException {
            Node packageNode = area(session, PACKAGE_AREA).getNode(packageName);
            String packagePath = packageNode.getPath();
            String sql = "SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '" + packagePath + "/" + ASSET_FOLDER_NAME
                         + "[%]/%'" + " AND drools:format = '" + format + "' AND drools:archive = 'false' ORDER BY drools:title";

            Query q = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
            long time = System.currentTimeMillis();
            QueryResult res = q.execute();

            NodeIterator it = res.getNodes();
            long taken = System.currentTimeMillis() - time;

            printDetail("Query execution time is: " + taken);
            return nodesFrom(it);
        }

        public List<Node> getPackageAssets( Session session,
                                            String packageName ) throws RepositoryException {
            Node packageNode = area(session, PACKAGE_AREA).getNode(packageName);
            NodeIterator iter = packageNode.getNode(ASSET_FOLDER_NAME).getNodes();
            return nodesFrom(iter);
        }

        protected List<Node> nodesFrom( NodeIterator iter ) {
            List<Node> result = new ArrayList<Node>();
            while (iter.hasNext()) {
                result.add(iter.nextNode());
            }
            return result;
        }
    }

}
