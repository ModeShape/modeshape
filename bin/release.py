#!/usr/bin/python
import re
import sys
import os
import subprocess
import shutil
from markdown2 import *
from datetime import *
from multiprocessing import Process
from utils import *
from jira import *
from docbook import *

try:
  from xml.etree.ElementTree import ElementTree
except:
  prettyprint('''
        Welcome to the ModeShape Release Script.
        This release script requires that you use at least Python 2.5.0.  It appears
        that you do not thave the ElementTree XML APIs available, which are available
        by default in Python 2.5.0.
        ''', Levels.FATAL)
  sys.exit(1)

modules = []
docbooks = []
uploader = None
git = None
jira = None

def get_modules(directory):
    '''Analyses the pom.xml file and extracts declared modules'''
    tree = ElementTree()
    f = directory + "/pom.xml"
    if settings['verbose']:
      print "Parsing %s to get a list of modules in project" % f
    tree.parse(f)        
    mods = tree.findall(".//{%s}module" % maven_pom_xml_namespace)
    for m in mods:
        modules.append(m.text)

def help_and_exit():
    prettyprint('''
    
%s  ModeShape Release Script%s

    This script automates much of the work of releasing a new version of the ModeShape project, and includes 
    the following tasks: 
      - create a local branch for the new release; 
      - change the project-related versions in the POM files and documentation; 
      - commit those changes locally;
      - create a tag for the release;
      - generate the release notes in multiple formats;
      - generate emails to all the people who have filed, commented on, or worked on issues 
        fixed in this release;
      - run a full assembly build of the software to product all artifacts and documentation;
      - place a copy of all artifacts and documentation in the '../archive' folder;
      - deploy all artifacts to the JBoss.org Maven repository in a staging area (authorization required)
      - upload all artifacts and documentation to JBoss.org (authorization required); and
      - push the commit and tag to the official Git repository (authorization required)
    
    Note that the last three steps are not performed during a dry run.
    
    Before this script is executed, be sure to update and commit the 'release_notes.md' file. It also ensures 
    that the local Git repository is a writable clone of the official ModeShape repository on GitHub.

%s  Usage:%s
        $ bin/release.py [options] <version> [<branch>]
    where:
      <version>           The name of the new version (e.g., '2.4.0.Final' but without quotes), which must 
                          comply with the format '<major>.<minor>.<patch>.<qualifier>', where the qualifier 
                          must be one of 'Final', 'Alpha', 'Beta', or 'CR'.
      branch              The name of the existing branch from which the release should be made. This defaults
                          to 'master'.
    and where the options include:
      --verbose           Show more detailed logging and messages
      --dry-run           Used for trial runs of the release process. This leaves a temporary branch in the local 
                          git repository that contains the committed changes, but does NOT push to the official 
                          Git repository and it does NOT publish artifacts to JBoss.org.
      --skip-tests        Do not run the unit or integration tests when building the software
      --single-threaded   Perform all operations sequentially without using multiple threads
      --multi-threaded    Perform some operations in parallel to reduce the overall run time.
                          This option is not available with '--dry-run'
      --help|?            Display this usage message
            
%s  Examples:%s
    
    $ bin/release.py 3.0.0.Final
         This will release '3.0.0.Final' based off of 'master'
    
    $ bin/release.py 2.8.1.Final 2.x
         This will release '2.8.1.Final' based off of the existing '2.x' branch
    
''' % (Colors.yellow(), Colors.end_color(), Colors.yellow(), Colors.end_color(), Colors.yellow(), Colors.end_color()), Levels.INFO)
    sys.exit(0)

def validate_version(version):  
  version_pattern = get_version_pattern()
  if version_pattern.match(version):
    return version.strip()
  else:
    prettyprint("Invalid version '"+version+"'!\n", Levels.FATAL)
    help_and_exit()

def tag_release(version, branch):
  if git.remote_branch_exists():
    git.switch_to_branch()
    git.create_tag_branch()
  else:
    prettyprint("Branch %s cannot be found on upstream repository.  Aborting!" % branch, Levels.FATAL)
    sys.exit(100)

def get_project_version_tag(tree):
  return tree.find("./{%s}version" % (maven_pom_xml_namespace))

def get_parent_version_tag(tree):
  return tree.find("./{%s}parent/{%s}version" % (maven_pom_xml_namespace, maven_pom_xml_namespace))

def patch_poms(working_dir, version):
  patched_poms = list()
  walker = GlobDirectoryWalker(working_dir, "pom.xml")
  for pom_file in walker:
    tree = ElementTree()
    tree.parse(pom_file)
    # The current version of the POM is what we're looking for ...
    current_version_elem = get_project_version_tag(tree)
    if current_version_elem == None:
      # There is no version for the POM, so get it from the parent ...
      current_version_elem = get_parent_version_tag(tree)
    current_version = current_version_elem.text
    if walker.replace_all_in(pom_file,"<version>%s</version>" % current_version,"<version>%s</version>" % version):
      patched_poms.append(pom_file)
  return patched_poms

def generate_release_notes(markdown_file,version,output_dir):
  f = open(markdown_file)
  readme_md = f.read()

  # Replace the version entity with the actual version ...
  readme_md = re.sub('&version;',version,readme_md)

  # Append the JIRA-generated release notes
  issues_md = jira.get_release_notes_in_markdown()
  readme_md = readme_md + "\n\n" + issues_md

  # Convert the lines to HTML using Markdown ...
  readme_html = Markdown().convert(readme_md);

  # Convert the lines to text by removing the Markdown patterns ...
  readme_text = unmarkdown(readme_md)

  # Write out the two files in the desired location ...
  os.makedirs(output_dir)
  path = os.path.join(output_dir,"release.html")
  mdf = open(path,'w')
  mdf.write(readme_html)
  mdf.close()

  path = os.path.join(output_dir,"release.txt")
  mdt = open(path,'w')
  mdt.write(readme_text)
  mdt.close()

def generate_contribution_emails(output_dir,bcc_address):
  '''Generates an HTML page in the output directory containing mailto links that can be used to create the contribution emails in your mail application'''
  html_content = jira.get_contribution_html(bcc_address)
  file_path = os.path.join(output_dir,"contributions.html")
  f = open(file_path,'w')
  f.write(html_content)
  f.close()

def copy_artifacts_to_archive_location(archive_path,version):
  try:
    os.makedirs(archive_path)
  except:
    pass
    
  # Copy the 'modeshape-distribution' artifacts ...
  from_files = ['dist.zip', 'source.zip']
  to_files = ['dist.zip', 'source.zip']
  for fsuffix,tsuffix in zip(from_files,to_files):
    shutil.copy("modeshape-distribution/target/modeshape-%s-%s" % (version,fsuffix), "%s/modeshape-%s-%s" % (archive_path,version,tsuffix))

  # Copy the 'deploy/jbossas' artifact(s) ...
  from_path = os.path.join('deploy','jbossas','target','distribution','modeshape-%s-jbossas-7-dist.zip' % (version))
  to_path = os.path.join(archive_path,'modeshape-%s-jbossas-7-dist.zip' % (version))
  shutil.copy(from_path,to_path)
  
  # Make an area for the documentation ...
  docs_path = os.path.join(archive_path,version)
  if not os.path.exists(docs_path):
    os.makedirs(docs_path)

  # Copy the Full JavaDoc ...
  from_path = os.path.join('modeshape-distribution','target','api-full')
  copy_folder(from_path,os.path.join(docs_path,'api-full'))

  ## Copy the API JavaDoc ...
  #from_path = os.path.join('modeshape-distribution','target','api')
  #copy_folder(from_path,os.path.join(docs_path,'api'))
  #
  ## Copy the XRef ...
  #from_path = os.path.join('modeshape-distribution','target','xref')
  #if os.path.exists(from_path):
  #  copy_folder(from_path,os.path.join(docs_path,'xref'))

  # Copy the release notes into the archive area...
  for readme in ['release.html','release.txt']:
    from_path = os.path.join('target',readme)
    shutil.copy(from_path,os.path.join(docs_path,readme))
    shutil.copy(from_path,os.path.join(archive_path,readme))


def copy_release_notes_to_archive_location(archive_path,version):
  try:
    os.makedirs(archive_path)
  except:
    pass
    
  # Copy the release notes into the archive area...
  for readme in ['release.html','release.txt']:
    from_path = os.path.join('target',readme)
    shutil.copy(from_path,os.path.join(archive_path,readme))
  

def copy_folder( from_path, to_path ):
  if os.path.exists(to_path):
    shutil.rmtree(to_path)
  shutil.copytree(from_path,to_path)  

def update_versions(version):
  modified_files = []

  ## Update versions in the POM files ...
  for pom in patch_poms('.',version):
    modified_files.append(pom)

  # Now make sure this goes back into the repository.
  git.commit(modified_files)

def get_module_name(pom_file):
  tree = ElementTree()
  tree.parse(pom_file)
  return tree.findtext("./{%s}artifactId" % maven_pom_xml_namespace)


def upload_artifacts(base_dir, version):
  """Downloadable artifacts get rsync'ed to filemgmt.jboss.org, in the downloads_htdocs/modeshape directory"""

  # Create an area under 'target' where we can move all the files/folders that we need to upload ...
  os.chdir("%s/target/" % (base_dir))
  os.makedirs("downloads/%s" % version)

  # Copy the 'modeshape-distribution' artifacts ...
  from_files = ['dist.zip', 'source.zip']
  to_files = ['dist.zip', 'source.zip']
  for fsuffix,tsuffix in zip(from_files,to_files):
    shutil.copy("%s/modeshape-distribution/target/modeshape-%s-%s" % (base_dir,version,fsuffix), "downloads/%s/modeshape-%s-%s" % (version,version,tsuffix))

  # Copy the 'deploy/jbossas' artifact(s) ...
  from_path = "%s/deploy/jbossas/target/distribution/modeshape-%s-jbossas-7-dist.zip" % (base_dir,version)
  shutil.copy(from_path, "downloads/%s/modeshape-%s-jbossas-7-dist.zip" % (version,version))
  
  # Copy the readme files ...
  for readme in ['release.html','release.txt']:
    from_path = os.path.join(base_dir,'target',readme)
    to_path = os.path.join('downloads',version,readme)
    shutil.copy(from_path,to_path)

  # rsync this stuff to filemgmt.jboss.org
  os.chdir("%s/target/downloads" % (base_dir))
  uploader.upload_rsync(version, "modeshape@filemgmt.jboss.org:/downloads_htdocs/modeshape", flags = ['-rv', '--protocol=28'])
  
  # We're done, so go back to where we were ...
  os.chdir(base_dir)

def upload_documentation(base_dir, version):
  """Javadocs get rsync'ed to filemgmt.jboss.org, in the docs_htdocs/modeshape directory"""

  # Create an area under 'target' where we can move all the files/folders that we need to upload ...
  os.chdir("%s/target/" % (base_dir))
  os.makedirs("docs/%s" % version)

  # Move the 'api' and 'api-full' folders into the 'docs/<version>/' folder so we can rsync that '<version>' folder
  #os.rename("%s/modeshape-distribution/target/api" % base_dir, "docs/%s/api" % version)
  os.rename("%s/modeshape-distribution/target/api-full" % base_dir, "docs/%s/api-full" % version)

  # Copy the readme files ...
  for readme in ['release.html','release.txt']:
    from_path = os.path.join(base_dir,'target',readme)
    to_path = os.path.join('docs',version,readme)
    shutil.copy(from_path,to_path)

  # rsync this stuff to filemgmt.jboss.org
  os.chdir("%s/target/docs" % (base_dir))
  uploader.upload_rsync(version, "modeshape@filemgmt.jboss.org:/docs_htdocs/modeshape", flags = ['-rv', '--protocol=28'])
  
  # We're done, so go back to where we were ...
  os.chdir(base_dir)

def do_task(target, args, async_processes):
  if settings['multi_threaded']:
    async_processes.append(Process(target = target, args = args))  
  else:
    target(*args)

### This is the starting place for this script.
def release():
  global settings
  global uploader
  global git
  global jira
  assert_python_minimum_version(2, 5)
  base_dir = os.getcwd()
    
  # Process the arguments ...
  version = None
  branch = 'master'
  if len(sys.argv) > 1:
    for arg in sys.argv[1:len(sys.argv)]:
      if arg == '--verbose':
        settings['verbose'] = True
      elif arg == '--dry-run':
        settings['dry_run'] = True
      elif arg == '--multi-threaded':
        settings['multi_threaded'] = True
      elif arg == '--single-threaded':
        settings['multi_threaded'] = False
      elif arg == '--help' or arg == '?':
        help_and_exit()
      else:
        if version == None:
          # The first non-option is the version
          print "validating version '%s'" % arg
          version = validate_version(arg)
        else:
          branch = arg
  
  ## Set up network interactive tools
  if settings['dry_run']:
    # Use stubs
    prettyprint("***", Levels.DEBUG)
    prettyprint("*** This is a DRY RUN.  No changes will be committed and no files will be published.  Used to test this release script only. ***", Levels.DEBUG)
    prettyprint("***", Levels.DEBUG)
    prettyprint("Your settings are %s" % settings, Levels.DEBUG)
    uploader = DryRunUploader()
  else:
    uploader = Uploader()
  
  # Make sure they want to continue ...    
  sure = input_with_default("\nDid you update and commit the 'release_notes.md' file?", "N")
  if not sure.upper().startswith("Y"):
    prettyprint("... Please do this now and rerun this script.", Levels.WARNING)
    print ""
    sys.exit(1)

  prettyprint("", Levels.INFO)
  prettyprint("Releasing ModeShape version %s from branch '%s'" % (version, branch), Levels.INFO)
  sure = input_with_default("Are you sure you want to continue?", "N")
  if not sure.upper().startswith("Y"):
    prettyprint("... User Abort!", Levels.WARNING)
    sys.exit(1)
  prettyprint("OK, releasing! Please stand by ...", Levels.INFO)
  
  tag_name = "modeshape-%s" % version
  git = Git(branch, tag_name)
  if not git.is_upstream_clone():
    proceed = input_with_default('This is not a clone of an %supstream%s ModeShape repository! Are you sure you want to proceed?' % (Colors.UNDERLINE, Colors.END), 'N')
    if not proceed.upper().startswith('Y'):
      prettyprint("... User Abort!", Levels.WARNING)
      sys.exit(1)
      
  # Haven't yet done anything ...
      
  ## Release order:
  # Step 1: Tag in Git
  prettyprint("Step 1: Tagging %s in git as %s" % (branch, version), Levels.INFO)
  tag_release(version, branch)
  prettyprint("Step 1: Complete", Levels.INFO)
  
  # Step 2: Update version in tagged files
  prettyprint("Step 2: Updating version number in source files", Levels.INFO)
  update_versions(version)
  prettyprint("Step 2: Complete", Levels.INFO)
  
  # Step 3: Build and test in Maven2
  prettyprint("Step 3: Build and test in Maven3", Levels.INFO)
  maven_build_distribution(version)
  prettyprint("Step 3: Complete", Levels.INFO)
  
  # Step 4: Generate release notes and place into the 'target' folder
  jira_url = "https://issues.jboss.org/"
  project_key = 'MODE'
  project_name = 'ModeShape'
  project_id = '12310930'
  prettyprint("Step 4: Generating release notes using JIRA and placing in './target'", Levels.INFO)
  jira = Jira(jira_url,project_key,project_id,project_name,version)
  generate_release_notes('release_notes.md',version,"target")
  prettyprint("Step 4: Complete", Levels.INFO)

  # # Step 5: Copy files into archive
  archive_path = os.path.join("..","archive",version);
  if not os.path.exists(archive_path):
    os.makedirs(archive_path)
  print "archive_path = '%s'" % archive_path
  prettyprint("Step 5: Copying build artifacts and documentation to archive '%s'" % (archive_path), Levels.INFO)
  copy_artifacts_to_archive_location(archive_path,version)
  copy_release_notes_to_archive_location(archive_path,version);
  prettyprint("Step 5: Complete", Levels.INFO)

  # Step 6: Generate contribution emails 
  prettyprint("Step 6: Generating contribution emails using JIRA and placing in '%s'" % (archive_path), Levels.INFO)
  generate_contribution_emails(archive_path,'rhauch@redhat.com')
  prettyprint("Step 6: Complete", Levels.INFO)

  # Nothing else should modify any files locally ...
    
  ## Clean up in git
  prettyprint("Step 7: Committing changes to Git, creating release tag, and pushing to 'origin'", Levels.INFO)
  git.tag_for_release()
  if not settings['dry_run']:
    git.push_to_origin()
    git.cleanup()
  else:
    prettyprint("In dry-run mode.  Not pushing tag to remote origin and not removing temp release branch '%s'." % git.working_branch, Levels.DEBUG)
  prettyprint("Step 7: Complete", Levels.INFO)

  async_processes = []

  # Step 8: Upload javadocs to JBoss.org
  prettyprint("Step 8: Uploading documentation to JBoss.org", Levels.INFO)
  do_task(upload_documentation, [base_dir, version], async_processes)
  prettyprint("Step 8: Complete", Levels.INFO)
  
  # Step 9: Upload downloads to JBoss.org
  prettyprint("Step 9: Uploading downloads to JBoss.org", Levels.INFO)
  do_task(upload_artifacts, [base_dir, version], async_processes)    
  prettyprint("Step 9: Complete", Levels.INFO)
  
  ## Wait for processes to finish
  for p in async_processes:
    p.start()
  
  for p in async_processes:
    p.join()
  
  prettyprint("\n\n\nDone!  Now all you need to do is the remaining post-release tasks as outlined in https://docspace.corp.redhat.com/docs/DOC-28594", Levels.INFO)

if __name__ == "__main__":
  release()