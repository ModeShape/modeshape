/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.teiid;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Test;
import org.modeshape.sequencer.teiid.VdbDataRole.Permission;
import org.modeshape.sequencer.teiid.VdbModel.Severity;
import org.modeshape.sequencer.teiid.VdbModel.ValidationMarker;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;

/**
 * 
 */
public class VdbManifestTest {

    @Test
    public void shouldReadVdbManifestFromBooksVDB() throws Exception {
        VdbManifest manifest = VdbManifest.read(streamFor("/model/books/BooksVDB_vdb.xml"), null/*, context*/);
        assertThat(manifest.getName(), is("BooksVDB"));
        assertThat(manifest.getVersion(), is(2));
        assertThat(manifest.getDescription(), is("This is a VDB description"));
        assertThat(manifest.getProperties().get("query-timeout"), is("10000"));
        assertThat(manifest.getProperties().get("preview"), is("true"));

        { // models
            List<VdbModel> models = manifest.getModels();
            assertThat(models.size(), is(3));

            { // model 1
                VdbModel model1 = models.get(0);
                assertThat(model1.getType(), is(CoreLexicon.ModelType.VIRTUAL));
                assertThat(model1.isVisible(), is(false));
                assertThat(model1.getName(), is("BooksProcedures"));
                assertThat(model1.getPathInVdb(), is("TestRESTWarGen/BooksProcedures.xmi"));
                assertThat(model1.getDescription(), is("This is a model description"));
                assertThat(model1.getChecksum(), is(1855484649L));
                assertThat(model1.isBuiltIn(), is(false));

                { // properties
                    Map<String, String> props = model1.getProperties();
                    assertThat(props.size(), is(2));
                    assertThat(props.get("modelClass"), is("Relational"));
                    assertThat(props.get("indexName"), is("1159106455.INDEX"));
                }
            }

            { // model 2
                VdbModel model2 = models.get(1);
                assertThat(model2.getType(), is(CoreLexicon.ModelType.PHYSICAL));
                assertThat(model2.isVisible(), is(true));
                assertThat(model2.getName(), is("MyBooks"));
                assertThat(model2.getPathInVdb(), is("TestRESTWarGen/MyBooks.xmi"));
                assertThat(model2.getDescription(), is(""));
                assertThat(model2.getChecksum(), is(2550610907L));
                assertThat(model2.isBuiltIn(), is(false));

                { // properties
                    Map<String, String> props = model2.getProperties();
                    assertThat(props.size(), is(2));
                    assertThat(props.get("modelClass"), is("Relational"));
                    assertThat(props.get("indexName"), is("718925066.INDEX"));
                }

                { // source
                    assertThat(model2.getSourceTranslator(), is("MyBooks_mysql5"));
                    assertThat(model2.getSourceJndiName(), is("MyBooks"));
                    assertThat(model2.getSourceName(), is("MyBooks"));
                }
            }

            { // model 3
                VdbModel model3 = models.get(2);
                assertThat(model3.getType(), is(CoreLexicon.ModelType.VIRTUAL));
                assertThat(model3.isVisible(), is(true));
                assertThat(model3.getName(), is("MyBooksView"));
                assertThat(model3.getPathInVdb(), is("TestRESTWarGen/MyBooksView.xmi"));
                assertThat(model3.getDescription(), is(""));
                assertThat(model3.getChecksum(), is(825941341L));
                assertThat(model3.isBuiltIn(), is(false));

                { // properties
                    Map<String, String> props = model3.getProperties();
                    assertThat(props.size(), is(2));
                    assertThat(props.get("modelClass"), is("Relational"));
                    assertThat(props.get("indexName"), is("2173178531.INDEX"));
                }

                { // imports
                    assertThat(model3.getImports().size(), is(1));
                    assertThat(model3.getImports().iterator().next(), is("/TestRESTWarGen/MyBooks.xmi"));
                }

                { // validation errors
                    List<ValidationMarker> problems = model3.getProblems();
                    assertThat(problems.size(), is(3));

                    { // problem 1
                        ValidationMarker problem1 = problems.get(0);
                        assertThat(problem1.getSeverity(), is(Severity.ERROR));
                        assertThat(problem1.getPath(), is("BOOKS"));
                        assertThat(problem1.getMessage(),
                                   is("The name BOOKS is the same (ignoring case) as 1 other object(s) under the same parent"));
                    }

                    { // problem 2
                        ValidationMarker problem2 = problems.get(1);
                        assertThat(problem2.getSeverity(), is(Severity.ERROR));
                        assertThat(problem2.getPath(), is("BOOKS"));
                        assertThat(problem2.getMessage(), is("Group does not exist: MyBooksView.BOOKS"));
                    }

                    { // problem 3
                        ValidationMarker problem3 = problems.get(2);
                        assertThat(problem3.getSeverity(), is(Severity.ERROR));
                        assertThat(problem3.getPath(), is("BOOKS"));
                        assertThat(problem3.getMessage(),
                                   is("The name BOOKS is the same (ignoring case) as 1 other object(s) under the same parent"));
                    }
                }
            }
        }

        { // translators
            List<VdbTranslator> translators = manifest.getTranslators();
            assertThat(translators.size(), is(1));
            VdbTranslator translator = manifest.getTranslators().get(0);
            assertThat(translator.getDescription(), is("This is a translator description"));
            assertThat(translator.getType(), is("mysql5"));
            assertThat(translator.getName(), is("MyBooks_mysql5"));
            assertThat(translator.getProperties().size(), is(2));
            assertThat(translator.getProperties().get("nameInSource"), is("bogusName"));
            assertThat(translator.getProperties().get("supportsUpdate"), is("true"));
        }

        { // data roles
            List<VdbDataRole> dataRoles = manifest.getDataRoles();
            assertThat(dataRoles.size(), is(1));
            VdbDataRole dataRole = dataRoles.get(0);
            assertThat(dataRole.getName(), is("My Data Role"));
            assertThat(dataRole.isAllowCreateTempTables(), is(true));
            assertThat(dataRole.isAnyAuthenticated(), is(true));

            // mapped role names
            assertThat(dataRole.getMappedRoleNames().size(), is(2));
            assertTrue(dataRole.getMappedRoleNames().contains("Sledge"));
            assertTrue(dataRole.getMappedRoleNames().contains("Hammer"));

            // permissions
            assertThat(dataRole.getPermissions().size(), is(3));

            { // permission 1
                Permission perm1 = dataRole.getPermissions().get(0);
                assertThat(perm1.getResourceName(), is("BooksProcedures"));
                assertFalse(perm1.canCreate());
                assertTrue(perm1.canRead());
                assertTrue(perm1.canUpdate());
                assertTrue(perm1.canDelete());
                assertFalse(perm1.canExecute());
                assertFalse(perm1.canAlter());
            }

            { // permission 2
                Permission perm2 = dataRole.getPermissions().get(1);
                assertThat(perm2.getResourceName(), is("sysadmin"));
                assertFalse(perm2.canCreate());
                assertTrue(perm2.canRead());
                assertFalse(perm2.canUpdate());
                assertFalse(perm2.canDelete());
                assertFalse(perm2.canExecute());
                assertFalse(perm2.canAlter());
            }

            { // permission 3
                Permission perm3 = dataRole.getPermissions().get(2);
                assertThat(perm3.getResourceName(), is("MyBooks"));
                assertFalse(perm3.canCreate());
                assertTrue(perm3.canRead());
                assertTrue(perm3.canUpdate());
                assertTrue(perm3.canDelete());
                assertFalse(perm3.canExecute());
                assertFalse(perm3.canAlter());
            }
        }

        { // entries
            List<VdbEntry> entries = manifest.getEntries();
            assertThat(entries.size(), is(2));

            { // entry 1
                VdbEntry entry1 = entries.get(0);
                assertThat(entry1.getPath(), is("path1"));
                assertThat(entry1.getDescription(), is("This is entry 1 description"));
                assertThat(entry1.getProperties().size(), is(2));
                assertThat(entry1.getProperties().get("drummer"), is("Ringo"));
                assertThat(entry1.getProperties().get("guitar"), is("John"));
            }

            { // entry 2
                VdbEntry entry2 = entries.get(1);
                assertThat(entry2.getPath(), is("path2"));
                assertThat(entry2.getDescription(), is("This is entry 2 description"));
                assertThat(entry2.getProperties().size(), is(2));
                assertThat(entry2.getProperties().get("bass"), is("Paul"));
                assertThat(entry2.getProperties().get("leadGuitar"), is("George"));
            }
        }
    }

    @Test
    public void shouldReadVdbManifestFromQuickEmployees() throws Exception {
        VdbManifest manifest = VdbManifest.read(streamFor("/model/QuickEmployees/vdb.xml"), null/*, context*/);
        assertThat(manifest.getName(), is("qe"));
        assertThat(manifest.getVersion(), is(1));
        assertThat(manifest.getDescription(), is("This VDB is for testing Recursive XML documents and Text Sources"));
        // assertThat(manifest.isPreview(), is(false));

        // check models
        assertThat(manifest.getModels().size(), is(5));
        VdbModel model = null;
        Set<String> imports = null;
        List<ValidationMarker> problems = null;
        Map<String, String> props = null;

        // -------
        // model 1
        // -------
        model = manifest.getModels().get(0);
        assertThat(model.getType(), is(CoreLexicon.ModelType.VIRTUAL));
        assertThat(model.isVisible(), is(true));
        assertThat(model.getName(), is("EmpV"));
        assertThat(model.getPathInVdb(), is("QuickEmployees/EmpV.xmi"));
        assertThat(model.getChecksum(), is(2273245105L));
        assertThat(model.isBuiltIn(), is(false));

        // model 1 properties
        props = model.getProperties();
        assertThat(props.size(), is(1));
        assertThat(props.get("indexName"), is("1646901791.INDEX"));

        // model 1 imports
        imports = model.getImports();
        assertThat(imports.size(), is(2));

        // model 1 problems
        problems = model.getProblems();
        assertThat(problems.size(), is(3));
        assertThat(problems.get(0).getSeverity(), is(Severity.WARNING));
        assertThat(problems.get(0).getPath(), is("EmpTable/annualSalary"));
        assertThat(problems.get(0).getMessage(),
                   is("Missing or invalid Precision on column with a numeric datatype (See validation Preferences)"));
    }

    @Test
    public void shouldReadVdbManifestWithModelMetadata() throws Exception {
        VdbManifest manifest = VdbManifest.read(streamFor("/vdb/declarativeModels-vdb.xml"), null);
        assertThat(manifest.getName(), is("twitter"));
        assertThat(manifest.getVersion(), is(1));
        assertThat(manifest.getDescription(), is("Shows how to call Web Services"));

        // properties
        Map<String, String> props = manifest.getProperties();
        assertThat(props.size(), is(1));
        Entry<String, String> prop = props.entrySet().iterator().next();
        assertThat(prop.getKey(), is("UseConnectorMetadata"));
        assertThat(prop.getValue(), is("cached"));

        // models
        boolean foundSource = false;
        boolean foundVirtual = false;
        List<VdbModel> models = manifest.getModels();
        assertThat(models.size(), is(2));

        for (VdbModel model : models) {
            if (!foundSource && "twitter".equals(model.getName())) {
                assertThat(model.getType(), is(CoreLexicon.ModelType.PHYSICAL));
                assertThat(model.getSourceTranslator(), is("rest"));
                assertThat(model.getSourceJndiName(), is("java:/twitterDS"));
                assertThat(model.getSourceName(), is("twitter"));
                foundSource = true;
            } else if (!foundVirtual && "twitterview".equals(model.getName())) {
                assertThat(model.getType(), is(CoreLexicon.ModelType.VIRTUAL));
                assertThat(model.getMetadataType(), is(VdbModel.DEFAULT_METADATA_TYPE));

                final String metadata = "CREATE VIRTUAL PROCEDURE getTweets(query varchar) RETURNS (created_on varchar(25),"
                                        + " from_user varchar(25), to_user varchar(25),"
                                        + " profile_image_url varchar(25), source varchar(25), text varchar(140)) AS"
                                        + " select tweet.* from"
                                        + " (call twitter.invokeHTTP(action => 'GET', endpoint =>querystring('',query as \"q\"))) w,"
                                        + " XMLTABLE('results' passing JSONTOXML('myxml', w.result) columns"
                                        + " created_on string PATH 'created_at'," + " from_user string PATH 'from_user',"
                                        + " to_user string PATH 'to_user',"
                                        + " profile_image_url string PATH 'profile_image_url'," + " source string PATH 'source',"
                                        + " text string PATH 'text') tweet;"
                                        + " CREATE VIEW Tweet AS select * FROM twitterview.getTweets;";
                assertThat(model.getModelDefinition(), is(metadata));
                foundVirtual = true;
            } else {
                fail();
            }
        }
    }
    
    private InputStream streamFor( String resourcePath ) throws Exception {
        InputStream istream = getClass().getResourceAsStream(resourcePath);
        assertThat(istream, is(notNullValue()));
        return istream;
    }
}
