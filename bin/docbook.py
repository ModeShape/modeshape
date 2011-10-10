#!/usr/bin/python
import re
import os
from utils import *

class DocBook(object):  
  '''Encapsulates JIRA access used when getting issues related to ModeShape release'''
 
  def __init__(self, version):
    self.version = version
    self.modified_files = set()
    try:
      if settings['verbose']:
        self.verbose = True
    except:
      self.verbose = False

  def patch_docbooks_under(self,working_dir):
    '''Replace all occurrences of the version in all documents' source, returning the list of files that were modified '''
    for docbook in self.get_docbook_dirs(working_dir):
      docbook_path = os.path.join(working_dir,docbook)
      self.patch_docbook(docbook_path)
    return self.modified_files
  
  def get_docbook_dirs(self,working_dir):
    dirname = working_dir #os.path.join(working_dir,'docs')
    if self.verbose:
      print "Looking for DocBook modules in project under %s" % dirname
    return os.listdir(dirname)

  def patch_docbook(self,doc_dir):
    '''Replace all occurrences of the version in the document source'''
    # Find the prior version ...
    prior_version = self.get_docbook_prior_version(doc_dir)
    if prior_version != None:
      self.replace_all_occurrences(doc_dir,'*.xml',prior_version)
      self.replace_all_occurrences(doc_dir,'*.dtd',prior_version)
    return
  
  def get_docbook_prior_version(self,doc_dir):
    dtd_path = os.path.join(doc_dir,'src','main','docbook','en-US','custom.dtd')
    if not os.path.exists(dtd_path):
      return None
    f = open(dtd_path)
    content = f.read();
    f.close()
    # Find the version number ...
    m = re.search(r"versionNumber\s+\"(.*?)\"",content)
    if m:
      return m.group(1)
    return None
  
  def replace_all_occurrences(self,doc_dir,pattern,prior_version):
    walker = GlobDirectoryWalker(doc_dir, pattern)
    for a_file in walker:
      if walker.replace_all_in(a_file,prior_version,self.version):
        self.modified_files.add(a_file)

### This is the starting place for this script.
def main():
  
  print "Working DocBook ..."
  version = '2.5.2.GA1'
  docbook = DocBook(version)
  for modified_file in docbook.patch_docbooks_under('docs'):
    print "Modified: %s" % (modified_file)

if __name__ == "__main__":
  main()  

